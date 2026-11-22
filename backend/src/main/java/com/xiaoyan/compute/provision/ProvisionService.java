package com.xiaoyan.compute.provision;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.compute.catalog.GpuSku;
import com.xiaoyan.compute.catalog.GpuSkuMapper;
import com.xiaoyan.compute.common.OrderPaidEvent;
import com.xiaoyan.compute.common.ProvisionRequestEvent;
import com.xiaoyan.compute.common.RocketTopics;
import com.xiaoyan.compute.instance.GpuInstance;
import com.xiaoyan.compute.instance.InstanceService;
import com.xiaoyan.compute.inventory.InventoryService;
import com.xiaoyan.compute.order.OrderRepository;
import com.xiaoyan.compute.order.OrderStatus;
import com.xiaoyan.compute.order.RentalOrder;
import com.xiaoyan.compute.payment.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class ProvisionService {
    private final OrderRepository orderRepository;
    private final GpuSkuMapper skuMapper;
    private final ProvisionTaskMapper taskMapper;
    private final CloudProviderAdapter providerAdapter;
    private final InstanceService instanceService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final RocketMQTemplate rocketMQTemplate;
    private final int maxRetries;
    private final int retryDelaySeconds;

    public ProvisionService(OrderRepository orderRepository, GpuSkuMapper skuMapper,
                            ProvisionTaskMapper taskMapper, CloudProviderAdapter providerAdapter,
                            InstanceService instanceService, PaymentService paymentService,
                            InventoryService inventoryService, RocketMQTemplate rocketMQTemplate,
                            @Value("${compute.provision.max-retries:3}") int maxRetries,
                            @Value("${compute.provision.retry-delay-seconds:5}") int retryDelaySeconds) {
        this.orderRepository = orderRepository;
        this.skuMapper = skuMapper;
        this.taskMapper = taskMapper;
        this.providerAdapter = providerAdapter;
        this.instanceService = instanceService;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.rocketMQTemplate = rocketMQTemplate;
        this.maxRetries = maxRetries;
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public void startProvision(OrderPaidEvent event) {
        RentalOrder order = orderRepository.findById(event.orderId());
        if (order == null) return;
        if (order.getStatus() == OrderStatus.PAID.getCode()) {
            orderRepository.markProvisioning(order.getId());
        }
        order = orderRepository.findById(order.getId());
        if (order == null || order.getStatus() != OrderStatus.PROVISIONING.getCode()) return;
        ensureTask(order.getId());
        rocketMQTemplate.convertAndSend(RocketTopics.PROVISION_REQUEST,
                new ProvisionRequestEvent(UUID.randomUUID().toString(), order.getId()));
    }

    public void provision(ProvisionRequestEvent event) {
        RentalOrder order = orderRepository.findById(event.orderId());
        if (order == null || order.getStatus() != OrderStatus.PROVISIONING.getCode()) return;
        ProvisionTask task = ensureTask(order.getId());
        if (task.getStatus() == ProvisionTaskStatus.SUCCEEDED.getCode()) return;
        taskMapper.update(null, Wrappers.<ProvisionTask>lambdaUpdate()
                .set(ProvisionTask::getStatus, ProvisionTaskStatus.RUNNING.getCode())
                .set(ProvisionTask::getUpdatedAt, LocalDateTime.now())
                .eq(ProvisionTask::getId, task.getId()));
        GpuSku sku = skuMapper.selectById(order.getSkuId());
        if (sku == null) {
            fail(order, task, "GPU 规格不存在");
            return;
        }
        try {
            ProvisionResult result = providerAdapter.provision(order, sku);
            GpuInstance instance = instanceService.createForOrder(order, sku, result.instanceNo(),
                    result.privateIp(), result.endpoint(), result.sshCommand());
            taskMapper.update(null, Wrappers.<ProvisionTask>lambdaUpdate()
                    .set(ProvisionTask::getStatus, ProvisionTaskStatus.SUCCEEDED.getCode())
                    .set(ProvisionTask::getProviderInstanceId, result.instanceNo())
                    .set(ProvisionTask::getUpdatedAt, LocalDateTime.now())
                    .eq(ProvisionTask::getId, task.getId()));
            orderRepository.markRunning(order.getId(), instance.getId());
            log.info("Provision succeeded orderNo={} instance={}", order.getOrderNo(), result.instanceNo());
        } catch (Exception e) {
            retryOrFail(order, task, e.getMessage());
        }
    }

    public void retry(ProvisionTask task) {
        int updated = taskMapper.update(null, Wrappers.<ProvisionTask>lambdaUpdate()
                .set(ProvisionTask::getStatus, ProvisionTaskStatus.RUNNING.getCode())
                .set(ProvisionTask::getUpdatedAt, LocalDateTime.now())
                .eq(ProvisionTask::getId, task.getId())
                .eq(ProvisionTask::getStatus, ProvisionTaskStatus.RETRYING.getCode()));
        if (updated > 0) {
            rocketMQTemplate.convertAndSend(RocketTopics.PROVISION_REQUEST,
                    new ProvisionRequestEvent(UUID.randomUUID().toString(), task.getOrderId()));
        }
    }

    private ProvisionTask ensureTask(Long orderId) {
        ProvisionTask task = taskMapper.selectOne(Wrappers.<ProvisionTask>lambdaQuery()
                .eq(ProvisionTask::getOrderId, orderId));
        if (task != null) return task;
        LocalDateTime now = LocalDateTime.now();
        task = ProvisionTask.builder()
                .id(IdWorker.getId())
                .orderId(orderId)
                .status(ProvisionTaskStatus.PENDING.getCode())
                .attempt(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            taskMapper.insert(task);
            return task;
        } catch (Exception ignored) {
            return taskMapper.selectOne(Wrappers.<ProvisionTask>lambdaQuery()
                    .eq(ProvisionTask::getOrderId, orderId));
        }
    }

    private void retryOrFail(RentalOrder order, ProvisionTask task, String error) {
        int attempt = (task.getAttempt() == null ? 0 : task.getAttempt()) + 1;
        orderRepository.incrementProvisionRetry(order.getId());
        if (attempt >= maxRetries) {
            fail(order, task, error);
            return;
        }
        taskMapper.update(null, Wrappers.<ProvisionTask>lambdaUpdate()
                .set(ProvisionTask::getStatus, ProvisionTaskStatus.RETRYING.getCode())
                .set(ProvisionTask::getAttempt, attempt)
                .set(ProvisionTask::getLastError, error)
                .set(ProvisionTask::getNextRetryAt, LocalDateTime.now().plusSeconds(retryDelaySeconds))
                .set(ProvisionTask::getUpdatedAt, LocalDateTime.now())
                .eq(ProvisionTask::getId, task.getId()));
        log.warn("Provision retry scheduled orderNo={} attempt={} error={}", order.getOrderNo(), attempt, error);
    }

    private void fail(RentalOrder order, ProvisionTask task, String error) {
        taskMapper.update(null, Wrappers.<ProvisionTask>lambdaUpdate()
                .set(ProvisionTask::getStatus, ProvisionTaskStatus.FAILED.getCode())
                .set(ProvisionTask::getLastError, error)
                .set(ProvisionTask::getUpdatedAt, LocalDateTime.now())
                .eq(ProvisionTask::getId, task.getId()));
        orderRepository.markProvisionFailed(order.getId(), error);
        orderRepository.markRefunding(order.getId());
        paymentService.refund(order);
        orderRepository.markRefunded(order.getId());
        inventoryService.release(order.getId(), order.getSkuId(), order.getStartAt(),
                order.getHours(), order.getQuantity());
        log.error("Provision failed and refunded orderNo={} error={}", order.getOrderNo(), error);
    }
}
