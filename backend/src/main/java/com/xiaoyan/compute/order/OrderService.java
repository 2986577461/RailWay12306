package com.xiaoyan.compute.order;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xiaoyan.compute.catalog.CatalogService;
import com.xiaoyan.compute.catalog.GpuSku;
import com.xiaoyan.compute.catalog.GpuSkuMapper;
import com.xiaoyan.compute.common.BizException;
import com.xiaoyan.compute.common.ResourceLockRequestEvent;
import com.xiaoyan.compute.common.ResourceLockedEvent;
import com.xiaoyan.compute.common.RocketTopics;
import com.xiaoyan.compute.inventory.InventoryService;
import com.xiaoyan.compute.payment.PaymentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final GpuSkuMapper skuMapper;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final RocketMQTemplate rocketMQTemplate;
    private final OrderRateLimiter rateLimiter;
    private final PaymentService paymentService;
    private final int expireMinutes;

    public OrderService(OrderRepository orderRepository, GpuSkuMapper skuMapper,
                        CatalogService catalogService, InventoryService inventoryService,
                        RocketMQTemplate rocketMQTemplate, OrderRateLimiter rateLimiter,
                        PaymentService paymentService,
                        @Value("${compute.order.expire-minutes:15}") int expireMinutes) {
        this.orderRepository = orderRepository;
        this.skuMapper = skuMapper;
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.rocketMQTemplate = rocketMQTemplate;
        this.rateLimiter = rateLimiter;
        this.paymentService = paymentService;
        this.expireMinutes = expireMinutes;
    }

    public OrderDetailView create(Long userId, CreateOrderCommand command, String idempotencyKey) {
        rateLimiter.check(userId);
        GpuSku sku = catalogService.findSku(command.gpuId());
        LocalDateTime nowHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime startAt = catalogService.normalizeStart(command.startAt());
        if (startAt.isBefore(nowHour)) {
            startAt = nowHour;
        }
        if (command.hours() < 1 || command.hours() > 72) {
            throw new BizException("租用时长需在 1 到 72 小时之间");
        }
        if (command.quantity() < 1) {
            throw new BizException("GPU 数量至少为 1");
        }
        CatalogService.AvailabilityView availability = catalogService.availability(command.gpuId(), startAt, command.hours());
        if (availability.available() < command.quantity()) {
            throw new BizException("该租期余量不足，请调整时间或数量");
        }
        String requestId = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString() : idempotencyKey;
        RentalOrder existing = orderRepository.findByIdempotencyKey(userId, requestId).orElse(null);
        RentalOrder order = existing != null ? existing
                : orderRepository.create(IdWorker.getId(), userId, sku,
                command.region() == null || command.region().isBlank() ? sku.getRegion() : command.region(),
                command.quantity(), startAt, command.hours(), requestId, expireMinutes);

        ResourceLockRequestEvent event = new ResourceLockRequestEvent(UUID.randomUUID().toString(), requestId,
                order.getId(), sku.getId(), order.getStartAt().toString(), order.getHours(), order.getQuantity());
        rocketMQTemplate.convertAndSend(RocketTopics.RESOURCE_LOCK_REQUEST, event);
        return detail(userId, order.getOrderNo());
    }

    public List<OrderListItemView> list(Long userId) {
        List<RentalOrder> orders = orderRepository.listByUser(userId, 100);
        if (orders.isEmpty()) return List.of();
        Map<Long, GpuSku> skus = skuMapper.selectBatchIds(
                        orders.stream().map(RentalOrder::getSkuId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(GpuSku::getId, item -> item));
        return orders.stream().map(order -> toListItem(order, skus.get(order.getSkuId()))).toList();
    }

    public OrderDetailView detail(Long userId, String orderNo) {
        RentalOrder order = orderRepository.findOrder(userId, orderNo);
        if (order == null) throw new BizException("订单不存在");
        GpuSku sku = skuMapper.selectById(order.getSkuId());
        return toDetail(order, sku);
    }

    public OrderDetailView cancel(Long userId, String orderNo) {
        RentalOrder order = orderRepository.findOrder(userId, orderNo);
        if (order == null) throw new BizException("订单不存在");
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT.getCode()) {
            throw new BizException("当前订单不可取消");
        }
        if (!orderRepository.markCancelled(order.getId())) {
            throw new BizException("订单状态已变化");
        }
        inventoryService.release(order.getId(), order.getSkuId(), order.getStartAt(),
                order.getHours(), order.getQuantity());
        return detail(userId, orderNo);
    }

    public OrderDetailView refund(Long userId, String orderNo) {
        RentalOrder order = orderRepository.findOrder(userId, orderNo);
        if (order == null) throw new BizException("订单不存在");
        if (order.getStatus() != OrderStatus.PAID.getCode()
                && order.getStatus() != OrderStatus.PROVISIONING.getCode()) {
            throw new BizException("仅已支付或开通中的订单可退款");
        }
        if (!order.getStartAt().isAfter(LocalDateTime.now())) {
            throw new BizException("租期已开始，暂不支持自助退款");
        }
        if (!orderRepository.markRefunding(order.getId())) {
            throw new BizException("订单状态已变化");
        }
        paymentService.refund(order);
        orderRepository.markRefunded(order.getId());
        inventoryService.release(order.getId(), order.getSkuId(), order.getStartAt(),
                order.getHours(), order.getQuantity());
        return detail(userId, orderNo);
    }

    public void applyLockResult(ResourceLockedEvent event) {
        int lockStatus = event.locked() ? LockStatus.LOCKED.getCode() : LockStatus.FAILED.getCode();
        orderRepository.markLockStatus(event.orderId(), lockStatus, event.reason());
    }

    public RentalOrder findOrder(Long userId, String orderNo) {
        RentalOrder order = orderRepository.findOrder(userId, orderNo);
        if (order == null) throw new BizException("订单不存在");
        return order;
    }

    public RentalOrder findById(Long orderId) {
        RentalOrder order = orderRepository.findById(orderId);
        if (order == null) throw new BizException("订单不存在");
        return order;
    }

    private OrderListItemView toListItem(RentalOrder order, GpuSku sku) {
        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        return new OrderListItemView(order.getOrderNo(), sku == null ? "GPU" : sku.getName(),
                order.getRegion(), order.getQuantity(), order.getStartAt(), order.getEndAt(),
                order.getHours(), order.getTotalAmount(), status.name(), status.getLabel(),
                LockStatus.fromCode(order.getLockStatus()).getLabel(),
                ProvisionStatus.fromCode(order.getProvisionStatus()).getLabel(),
                order.getCreatedAt());
    }

    private OrderDetailView toDetail(RentalOrder order, GpuSku sku) {
        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        LockStatus lock = LockStatus.fromCode(order.getLockStatus());
        ProvisionStatus provision = ProvisionStatus.fromCode(order.getProvisionStatus());
        return new OrderDetailView(order.getOrderNo(), sku == null ? null : sku.getCode(),
                sku == null ? "GPU" : sku.getName(), order.getRegion(), order.getQuantity(),
                order.getStartAt(), order.getEndAt(), order.getHours(), order.getUnitPrice(),
                order.getTotalAmount(), status.name(), status.getLabel(), lock.name(), lock.getLabel(),
                provision.name(), provision.getLabel(), order.getLockFailReason(), order.getInstanceId(),
                order.getPaymentNo(), order.getExpireAt(), order.getCreatedAt());
    }

    public record CreateOrderCommand(
            @NotBlank String gpuId,
            String region,
            @Min(1) @Max(32) int quantity,
            @NotNull LocalDateTime startAt,
            @Min(1) @Max(72) int hours
    ) { }

    public record OrderListItemView(
            String orderNo, String gpuName, String region, int quantity,
            LocalDateTime startAt, LocalDateTime endAt, int hours, BigDecimal amount,
            String status, String statusLabel, String lockStatusLabel,
            String provisionStatusLabel, LocalDateTime createdAt
    ) { }

    public record OrderDetailView(
            String orderNo, String gpuId, String gpuName, String region, int quantity,
            LocalDateTime startAt, LocalDateTime endAt, int hours, BigDecimal unitPrice,
            BigDecimal amount, String status, String statusLabel, String lockStatus,
            String lockStatusLabel, String provisionStatus, String provisionStatusLabel,
            String lockFailReason, Long instanceId, String paymentNo,
            LocalDateTime expireAt, LocalDateTime createdAt
    ) { }
}
