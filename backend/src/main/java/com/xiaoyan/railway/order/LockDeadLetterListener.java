package com.xiaoyan.railway.order;

import com.xiaoyan.railway.common.TicketRequestEvent;
import com.xiaoyan.railway.inventory.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * A lock request that exhausted its RocketMQ retries (inventory service down for long enough) would
 * otherwise leave the order PROCESSING until the payment window lapses. Marking it FAILED tells the
 * user to order again right away. Also releases any stock that might have been deducted: if the lock
 * succeeded but the DB write failed repeatedly, inventory-cancel.lua will return the stock; if the
 * lock never ran, it writes a RELEASED tombstone so a late retry deducts nothing.
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "%DLQ%inventory-service", consumerGroup = "order-lock-dlq")
public class LockDeadLetterListener implements RocketMQListener<TicketRequestEvent> {
    private static final String BUSY_REASON = "系统繁忙，请重新下单";

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public LockDeadLetterListener(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    public void onMessage(TicketRequestEvent event) {
        boolean applied = orderRepository.markLockStatus(event.orderId(), LockStatus.FAILED.getCode(), BUSY_REASON);
        log.error("Lock retries exhausted: orderId={} marked FAILED={}", event.orderId(), applied);

        // Release any locked inventory: inventory-cancel.lua is idempotent and safe whether the lock ran or not
        Order order = orderRepository.findById(event.orderId());
        if (order != null) {
            try {
                inventoryService.cancelUnpaidOrder(order);
                log.info("Released inventory for FAILED order: orderId={}", event.orderId());
            } catch (Exception e) {
                log.error("Failed to release inventory for orderId={}: {}", event.orderId(), e.getMessage(), e);
            }
        } else {
            log.warn("Order not found when releasing inventory: orderId={}", event.orderId());
        }
    }
}
