package com.xiaoyan.compute.instance;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.compute.catalog.GpuSku;
import com.xiaoyan.compute.catalog.GpuSkuMapper;
import com.xiaoyan.compute.common.BizException;
import com.xiaoyan.compute.order.OrderRepository;
import com.xiaoyan.compute.order.RentalOrder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InstanceService {
    private final GpuInstanceMapper instanceMapper;
    private final GpuSkuMapper skuMapper;
    private final OrderRepository orderRepository;

    public InstanceService(GpuInstanceMapper instanceMapper, GpuSkuMapper skuMapper,
                           OrderRepository orderRepository) {
        this.instanceMapper = instanceMapper;
        this.skuMapper = skuMapper;
        this.orderRepository = orderRepository;
    }

    public GpuInstance createForOrder(RentalOrder order, GpuSku sku, String instanceNo,
                                      String privateIp, String endpoint, String sshCommand) {
        LocalDateTime now = LocalDateTime.now();
        GpuInstance instance = GpuInstance.builder()
                .id(IdWorker.getId())
                .instanceNo(instanceNo)
                .orderId(order.getId())
                .skuId(order.getSkuId())
                .region(order.getRegion())
                .status(InstanceStatus.RUNNING.getCode())
                .privateIp(privateIp)
                .endpoint(endpoint)
                .sshCommand(sshCommand)
                .utilization(35 + Math.abs(instanceNo.hashCode() % 45))
                .startedAt(now)
                .expiresAt(order.getEndAt())
                .createdAt(now)
                .updatedAt(now)
                .build();
        instanceMapper.insert(instance);
        return instance;
    }

    public List<InstanceView> list(Long userId) {
        List<RentalOrder> orders = orderRepository.listAllByUser(userId);
        if (orders.isEmpty()) return List.of();
        Map<Long, RentalOrder> orderMap = orders.stream().collect(Collectors.toMap(RentalOrder::getId, item -> item));
        List<GpuInstance> instances = instanceMapper.selectList(Wrappers.<GpuInstance>lambdaQuery()
                .in(GpuInstance::getOrderId, orderMap.keySet())
                .orderByDesc(GpuInstance::getStartedAt));
        Set<Long> skuIds = instances.stream().map(GpuInstance::getSkuId).collect(Collectors.toSet());
        Map<Long, GpuSku> skus = skuIds.isEmpty() ? Map.of()
                : skuMapper.selectBatchIds(skuIds).stream().collect(Collectors.toMap(GpuSku::getId, item -> item));
        return instances.stream().map(instance -> toView(instance, orderMap.get(instance.getOrderId()),
                skus.get(instance.getSkuId()))).toList();
    }

    public InstanceView detail(Long userId, String instanceNo) {
        GpuInstance instance = instanceMapper.selectOne(Wrappers.<GpuInstance>lambdaQuery()
                .eq(GpuInstance::getInstanceNo, instanceNo));
        if (instance == null) throw new BizException("实例不存在");
        RentalOrder order = orderRepository.findById(instance.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) throw new BizException("实例不存在");
        return toView(instance, order, skuMapper.selectById(instance.getSkuId()));
    }

    public void completeByOrder(Long orderId) {
        instanceMapper.update(null, Wrappers.<GpuInstance>lambdaUpdate()
                .set(GpuInstance::getStatus, InstanceStatus.TERMINATED.getCode())
                .set(GpuInstance::getTerminatedAt, LocalDateTime.now())
                .set(GpuInstance::getUpdatedAt, LocalDateTime.now())
                .eq(GpuInstance::getOrderId, orderId)
                .eq(GpuInstance::getStatus, InstanceStatus.RUNNING.getCode()));
    }

    private InstanceView toView(GpuInstance instance, RentalOrder order, GpuSku sku) {
        String uptime = "—";
        if (instance.getStartedAt() != null && instance.getStatus() == InstanceStatus.RUNNING.getCode()) {
            Duration duration = Duration.between(instance.getStartedAt(), LocalDateTime.now());
            long seconds = Math.max(0, duration.getSeconds());
            uptime = String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        }
        return new InstanceView(instance.getInstanceNo(), order == null ? null : order.getOrderNo(),
                sku == null ? "GPU" : sku.getName(), instance.getRegion(),
                InstanceStatus.fromCode(instance.getStatus()).getLabel(), instance.getPrivateIp(),
                uptime, instance.getUtilization(), sku == null ? BigDecimal.ZERO : sku.getPricePerHour(),
                instance.getEndpoint(), instance.getSshCommand());
    }

    public record InstanceView(
            String id, String orderId, String gpuName, String region, String status,
            String ip, String uptime, int utilization, BigDecimal pricePerHour,
            String endpoint, String sshCommand
    ) { }
}
