package com.xiaoyan.railway.order;

import com.xiaoyan.railway.common.InventoryLockedEvent;
import com.xiaoyan.railway.common.RocketTopics;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * Persists the async inventory-lock outcome onto the order ({@code lock_status}),
 * the missing link that lets the frontend learn whether its ticket was actually locked
 * (locked → payable; failed with a reason such as 余票不足).
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = RocketTopics.INVENTORY_LOCKED, consumerGroup = "order-service")
public class InventoryLockedListener implements RocketMQListener<InventoryLockedEvent> {
    private final OrderRepository orderRepository;

    public InventoryLockedListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void onMessage(InventoryLockedEvent event) {
        int lockStatus = event.locked() ? LockStatus.LOCKED.getCode() : LockStatus.FAILED.getCode();
        boolean applied = orderRepository.markLockStatus(event.orderId(), lockStatus, event.reason());
        log.info("Inventory lock result orderId={} locked={} applied={} reason={}",
                event.orderId(), event.locked(), applied, event.reason());
    }
}
