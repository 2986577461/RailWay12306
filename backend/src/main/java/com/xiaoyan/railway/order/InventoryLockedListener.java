package com.xiaoyan.railway.order;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.common.InventoryLockedEvent;
import com.xiaoyan.railway.common.RocketTopics;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Persists the async inventory-lock outcome onto the order ({@code lock_status}),
 * the missing link that lets the frontend learn whether its ticket was actually locked
 * (locked → payable; failed with a reason such as 余票不足). On success it also writes the
 * per-passenger {@code order_passenger} rows — this is the only place that link is created.
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = RocketTopics.INVENTORY_LOCKED, consumerGroup = "order-service")
public class InventoryLockedListener implements RocketMQListener<InventoryLockedEvent> {
    private final OrderRepository orderRepository;
    private final OrderPassengerMapper orderPassengerMapper;

    public InventoryLockedListener(OrderRepository orderRepository, OrderPassengerMapper orderPassengerMapper) {
        this.orderRepository = orderRepository;
        this.orderPassengerMapper = orderPassengerMapper;
    }

    @Override
    public void onMessage(InventoryLockedEvent event) {
        int lockStatus = event.locked() ? LockStatus.LOCKED.getCode() : LockStatus.FAILED.getCode();
        boolean applied = orderRepository.markLockStatus(event.orderId(), lockStatus, event.reason());
        log.info("Inventory lock result orderId={} locked={} applied={} reason={}",
                event.orderId(), event.locked(), applied, event.reason());
        if (event.locked() && applied) {
            writePassengers(event);
        }
    }

    private void writePassengers(InventoryLockedEvent event) {
        if (orderPassengerMapper.exists(Wrappers.<OrderPassenger>lambdaQuery()
                .eq(OrderPassenger::getOrderId, event.orderId()))) {
            return;
        }
        Order order = orderRepository.findById(event.orderId());
        if (order == null) {
            log.warn("Order {} not found when writing order_passenger rows", event.orderId());
            return;
        }
        BigDecimal ticketPrice = order.getTotalAmount()
                .divide(BigDecimal.valueOf(order.getQuantity()), 2, RoundingMode.HALF_UP);
        for (Long passengerId : event.passengerIds()) {
            OrderPassenger orderPassenger = OrderPassenger.builder()
                    .orderId(event.orderId())
                    .passengerId(passengerId)
                    .seatTypeId(event.seatTypeId())
                    .ticketPrice(ticketPrice)
                    .ticketStatus(1)
                    .build();
            orderPassengerMapper.insert(orderPassenger);
        }
    }
}
