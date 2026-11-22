package com.xiaoyan.compute.order;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.compute.catalog.GpuSku;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {
    private final OrderMapper orderMapper;

    public OrderRepository(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public Optional<RentalOrder> findByIdempotencyKey(Long userId, String key) {
        return Optional.ofNullable(orderMapper.selectOne(Wrappers.<RentalOrder>lambdaQuery()
                .eq(RentalOrder::getUserId, userId)
                .eq(RentalOrder::getIdempotencyKey, key)));
    }

    public RentalOrder create(long id, Long userId, GpuSku sku, String region, int quantity,
                              LocalDateTime startAt, int hours, String idempotencyKey, int expireMinutes) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal total = sku.getPricePerHour()
                .multiply(BigDecimal.valueOf((long) quantity * hours));
        RentalOrder order = RentalOrder.builder()
                .id(id)
                .orderNo("ORD-" + id)
                .userId(userId)
                .skuId(sku.getId())
                .region(region)
                .quantity(quantity)
                .startAt(startAt)
                .endAt(startAt.plusHours(hours))
                .hours(hours)
                .unitPrice(sku.getPricePerHour())
                .totalAmount(total)
                .status(OrderStatus.PENDING_PAYMENT.getCode())
                .lockStatus(LockStatus.PROCESSING.getCode())
                .provisionStatus(ProvisionStatus.NOT_STARTED.getCode())
                .provisionRetryCount(0)
                .expireAt(now.plusMinutes(expireMinutes))
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            orderMapper.insert(order);
            return order;
        } catch (DuplicateKeyException ignored) {
            return findByIdempotencyKey(userId, idempotencyKey).orElseThrow();
        }
    }

    public RentalOrder findOrder(Long userId, String orderNo) {
        return orderMapper.selectOne(Wrappers.<RentalOrder>lambdaQuery()
                .eq(RentalOrder::getOrderNo, orderNo)
                .eq(RentalOrder::getUserId, userId));
    }

    public RentalOrder findById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    public List<RentalOrder> listByUser(Long userId, int limit) {
        return orderMapper.selectList(Wrappers.<RentalOrder>lambdaQuery()
                .eq(RentalOrder::getUserId, userId)
                .orderByDesc(RentalOrder::getCreatedAt)
                .last("LIMIT " + limit));
    }

    public List<RentalOrder> listAllByUser(Long userId) {
        return orderMapper.selectList(Wrappers.<RentalOrder>lambdaQuery()
                .eq(RentalOrder::getUserId, userId)
                .orderByDesc(RentalOrder::getCreatedAt));
    }

    public List<RentalOrder> findExpiredPending(LocalDateTime now, int limit) {
        return orderMapper.selectList(Wrappers.<RentalOrder>lambdaQuery()
                .eq(RentalOrder::getStatus, OrderStatus.PENDING_PAYMENT.getCode())
                .lt(RentalOrder::getExpireAt, now)
                .last("LIMIT " + limit));
    }

    public List<RentalOrder> findExpiredRunning(LocalDateTime now, int limit) {
        return orderMapper.selectList(Wrappers.<RentalOrder>lambdaQuery()
                .eq(RentalOrder::getStatus, OrderStatus.RUNNING.getCode())
                .lt(RentalOrder::getEndAt, now)
                .last("LIMIT " + limit));
    }

    public boolean markLockStatus(Long orderId, int lockStatus, String reason) {
        LambdaUpdateWrapper<RentalOrder> update = Wrappers.<RentalOrder>lambdaUpdate()
                .set(RentalOrder::getLockStatus, lockStatus)
                .set(RentalOrder::getUpdatedAt, LocalDateTime.now())
                .eq(RentalOrder::getId, orderId)
                .eq(RentalOrder::getStatus, OrderStatus.PENDING_PAYMENT.getCode());
        if (reason != null) {
            update.set(RentalOrder::getLockFailReason, reason);
        }
        return orderMapper.update(null, update) > 0;
    }

    public boolean markPaid(Long orderId, String paymentNo) {
        return orderMapper.update(null, Wrappers.<RentalOrder>lambdaUpdate()
                .set(RentalOrder::getStatus, OrderStatus.PAID.getCode())
                .set(RentalOrder::getPaymentNo, paymentNo)
                .set(RentalOrder::getPaidAt, LocalDateTime.now())
                .set(RentalOrder::getUpdatedAt, LocalDateTime.now())
                .eq(RentalOrder::getId, orderId)
                .eq(RentalOrder::getStatus, OrderStatus.PENDING_PAYMENT.getCode())) > 0;
    }

    public boolean markProvisioning(Long orderId) {
        return orderMapper.update(null, Wrappers.<RentalOrder>lambdaUpdate()
                .set(RentalOrder::getStatus, OrderStatus.PROVISIONING.getCode())
                .set(RentalOrder::getProvisionStatus, ProvisionStatus.QUEUED.getCode())
                .set(RentalOrder::getUpdatedAt, LocalDateTime.now())
                .eq(RentalOrder::getId, orderId)
                .eq(RentalOrder::getStatus, OrderStatus.PAID.getCode())) > 0;
    }

    public boolean markProvisionRunning(Long orderId) {
        return orderMapper.update(null, Wrappers.<RentalOrder>lambdaUpdate()
                .set(RentalOrder::getProvisionStatus, ProvisionStatus.RUNNING.getCode())
                .set(RentalOrder::getUpdatedAt, LocalDateTime.now())
                .eq(RentalOrder::getId, orderId)
                .eq(RentalOrder::getStatus, OrderStatus.PROVISIONING.getCode())) > 0;
    }

    public boolean markRunning(Long orderId, Long instanceId) {
        return orderMapper.update(null, Wrappers.<RentalOrder>lambdaUpdate()
                .set(RentalOrder::getStatus, OrderStatus.RUNNING.getCode())
                .set(RentalOrder::getProvisionStatus, ProvisionStatus.SUCCEEDED.getCode())
                .set(RentalOrder::getInstanceId, instanceId)
                .set(RentalOrder::getProvisionedAt, LocalDateTime.now())
                .set(RentalOrder::getUpdatedAt, LocalDateTime.now())
                .eq(RentalOrder::getId, orderId)
                .eq(RentalOrder::getStatus, OrderStatus.PROVISIONING.getCode())) > 0;
    }

    public boolean markProvisionFailed(Long orderId, String error) {
        return orderMapper.update(null, Wrappers.<RentalOrder>lambdaUpdate()
                .set(RentalOrder::getStatus, OrderStatus.PROVISION_FAILED.getCode())
                .set(RentalOrder::getProvisionStatus, ProvisionStatus.FAILED.getCode())
                .set(RentalOrder::getProvisionError, error)
                .set(RentalOrder::getUpdatedAt, LocalDateTime.now())
                .eq(RentalOrder::getId, orderId)
                .eq(RentalOrder::getStatus, OrderStatus.PROVISIONING.getCode())) > 0;
    }

    public boolean markCompleted(Long orderId) {
        return orderMapper.update(null, Wrappers.<RentalOrder>lambdaUpdate()
                .set(RentalOrder::getStatus, OrderStatus.COMPLETED.getCode())
                .set(RentalOrder::getCompletedAt, LocalDateTime.now())
                .set(RentalOrder::getUpdatedAt, LocalDateTime.now())
                .eq(RentalOrder::getId, orderId)
                .eq(RentalOrder::getStatus, OrderStatus.RUNNING.getCode())) > 0;
    }

    public boolean markCancelled(Long orderId) {
        return orderMapper.update(null, Wrappers.<RentalOrder>lambdaUpdate()
                .set(RentalOrder::getStatus, OrderStatus.CANCELLED.getCode())
                .set(RentalOrder::getLockStatus, LockStatus.RELEASED.getCode())
                .set(RentalOrder::getCancelledAt, LocalDateTime.now())
                .set(RentalOrder::getUpdatedAt, LocalDateTime.now())
                .eq(RentalOrder::getId, orderId)
                .eq(RentalOrder::getStatus, OrderStatus.PENDING_PAYMENT.getCode())) > 0;
    }

    public boolean markRefunding(Long orderId) {
        return orderMapper.update(null, Wrappers.<RentalOrder>lambdaUpdate()
                .set(RentalOrder::getStatus, OrderStatus.REFUNDING.getCode())
                .set(RentalOrder::getUpdatedAt, LocalDateTime.now())
                .in(RentalOrder::getStatus, OrderStatus.PAID.getCode(), OrderStatus.PROVISIONING.getCode(), OrderStatus.PROVISION_FAILED.getCode())
                .eq(RentalOrder::getId, orderId)) > 0;
    }

    public boolean markRefunded(Long orderId) {
        return orderMapper.update(null, Wrappers.<RentalOrder>lambdaUpdate()
                .set(RentalOrder::getStatus, OrderStatus.REFUNDED.getCode())
                .set(RentalOrder::getLockStatus, LockStatus.RELEASED.getCode())
                .set(RentalOrder::getUpdatedAt, LocalDateTime.now())
                .eq(RentalOrder::getId, orderId)
                .eq(RentalOrder::getStatus, OrderStatus.REFUNDING.getCode())) > 0;
    }

    public void incrementProvisionRetry(Long orderId) {
        orderMapper.update(null, Wrappers.<RentalOrder>lambdaUpdate()
                .setSql("provision_retry_count = provision_retry_count + 1")
                .eq(RentalOrder::getId, orderId));
    }
}
