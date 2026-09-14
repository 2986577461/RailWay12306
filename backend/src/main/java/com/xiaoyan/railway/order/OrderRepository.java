package com.xiaoyan.railway.order;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.basic.Fare;
import com.xiaoyan.railway.basic.FareMapper;
import com.xiaoyan.railway.basic.TrainRun;
import com.xiaoyan.railway.basic.TrainRunMapper;
import com.xiaoyan.railway.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {
    private final OrderMapper orderMapper;
    private final TrainRunMapper trainRunMapper;
    private final FareMapper fareMapper;
    private final int expireMinutes;

    public OrderRepository(OrderMapper orderMapper, TrainRunMapper trainRunMapper, FareMapper fareMapper,
                           @Value("${railway.order.expire-minutes:15}") int expireMinutes) {
        this.orderMapper = orderMapper;
        this.trainRunMapper = trainRunMapper;
        this.fareMapper = fareMapper;
        this.expireMinutes = expireMinutes;
    }

    public Optional<OrderSummary> findByIdempotencyKey(Long userId, String key) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getIdempotencyKey, key));
        return Optional.ofNullable(order).map(o -> new OrderSummary(o.getId(), o.getOrderNo(), o.getLockStatus()));
    }

    public OrderSummary create(long id, Long userId, TicketRequestCommand command, String idempotencyKey) {
        String orderNo = "O" + id;
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .id(id)
                .orderNo(orderNo)
                .userId(userId)
                .trainRunId(command.trainRunId())
                .fromStationId(command.fromStationId())
                .toStationId(command.toStationId())
                .seatTypeId(command.seatTypeId())
                .fromSeq(command.fromSeq())
                .toSeq(command.toSeq())
                .quantity(command.passengerIds().size())
                .orderStatus(OrderStatus.PENDING.getCode())
                .totalAmount(resolveAmount(command))
                .expireAt(now.plusMinutes(expireMinutes))
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            orderMapper.insert(order);
            return new OrderSummary(id, orderNo, LockStatus.PROCESSING.getCode());
        } catch (DuplicateKeyException ignored) {
            return findByIdempotencyKey(userId, idempotencyKey).orElseThrow();
        }
    }

    /** Order owned by {@code userId}, or null. */
    public Order findOrder(Long userId, String orderNo) {
        return orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
    }

    public List<Order> listByUser(Long userId, int limit) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt)
                .last("LIMIT " + limit));
    }

    /** Pending orders whose payment window has passed — candidates for timeout cancellation. */
    public List<Order> findExpiredPending(LocalDateTime now, int limit) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderStatus, OrderStatus.PENDING.getCode())
                .lt(Order::getExpireAt, now)
                .last("LIMIT " + limit));
    }

    /**
     * Persist the async inventory-lock result. Only applies while the order is still PENDING,
     * so a stale lock result (arriving after timeout-cancel, say) is ignored.
     */
    public boolean markLockStatus(Long orderId, int lockStatus, String failReason) {
        LambdaUpdateWrapper<Order> update = Wrappers.<Order>lambdaUpdate()
                .set(Order::getLockStatus, lockStatus)
                .set(Order::getUpdatedAt, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getOrderStatus, OrderStatus.PENDING.getCode());
        if (failReason != null) {
            update.set(Order::getLockFailReason, failReason);
        }
        return orderMapper.update(null, update) > 0;
    }

    /** Optimistic PENDING → PAID transition; returns false if already transitioned. */
    public boolean markPaid(Long orderId) {
        return orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getOrderStatus, OrderStatus.PAID.getCode())
                .set(Order::getUpdatedAt, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getOrderStatus, OrderStatus.PENDING.getCode())) > 0;
    }

    /** Optimistic PENDING → CANCELLED (timeout); returns false if no longer pending. */
    public boolean cancelPending(Long orderId) {
        return orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getOrderStatus, OrderStatus.CANCELLED.getCode())
                .set(Order::getUpdatedAt, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getOrderStatus, OrderStatus.PENDING.getCode())) > 0;
    }

    /** Optimistic PAID → REFUNDED; returns false if not in PAID state. */
    public boolean markRefunded(Long orderId) {
        return orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getOrderStatus, OrderStatus.REFUNDED.getCode())
                .set(Order::getUpdatedAt, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getOrderStatus, OrderStatus.PAID.getCode())) > 0;
    }

    /** total = fare(train, seatType, from, to).price × passenger count. */
    private BigDecimal resolveAmount(TicketRequestCommand command) {
        TrainRun run = trainRunMapper.selectById(command.trainRunId());
        if (run == null) {
            throw new BizException("车次运行不存在");
        }
        Fare fare = fareMapper.selectOne(Wrappers.<Fare>lambdaQuery()
                .eq(Fare::getTrainId, run.getTrainId())
                .eq(Fare::getSeatTypeId, command.seatTypeId())
                .eq(Fare::getFromStationId, command.fromStationId())
                .eq(Fare::getToStationId, command.toStationId()));
        if (fare == null) {
            throw new BizException("该区间未配置票价");
        }
        return fare.getPrice().multiply(BigDecimal.valueOf(command.passengerIds().size()));
    }

    public record OrderSummary(Long id, String orderNo, Integer lockStatus) { }
}
