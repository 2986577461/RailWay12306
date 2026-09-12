package com.xiaoyan.railway.order;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {
    private final OrderMapper orderMapper;
    public OrderRepository(OrderMapper orderMapper) { this.orderMapper = orderMapper; }

    public Optional<OrderSummary> findByIdempotencyKey(Long userId, String key) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getIdempotencyKey, key));
        return Optional.ofNullable(order).map(o -> new OrderSummary(o.getId(), o.getOrderNo()));
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
                .orderStatus(1)
                .totalAmount(BigDecimal.ZERO)
                .expireAt(now.plusMinutes(15))
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            orderMapper.insert(order);
            return new OrderSummary(id, orderNo);
        } catch (DuplicateKeyException ignored) {
            return findByIdempotencyKey(userId, idempotencyKey).orElseThrow();
        }
    }

    public List<Order> listByUser(Long userId) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt));
    }

    public Optional<Order> findByUserAndOrderNo(Long userId, String orderNo) {
        return Optional.ofNullable(orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getOrderNo, orderNo)));
    }

    public record OrderSummary(Long id, String orderNo) { }
}

