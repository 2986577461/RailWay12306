package com.xiaoyan.railway.order;

import com.xiaoyan.railway.common.TicketRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * A lock request that exhausted its RocketMQ retries (inventory service down for long enough) would
 * otherwise leave the order PROCESSING until the payment window lapses. Marking it FAILED tells the
 * user to order again right away. Any stock it had deducted comes back at timeout cancel, because the
 * lock record is still '1'.
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "%DLQ%inventory-service", consumerGroup = "order-lock-dlq")
public class LockDeadLetterListener implements RocketMQListener<TicketRequestEvent> {
    private static final String BUSY_REASON = "系统繁忙，请重新下单";

    private final OrderRepository orderRepository;

    public LockDeadLetterListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void onMessage(TicketRequestEvent event) {
        boolean applied = orderRepository.markLockStatus(event.orderId(), LockStatus.FAILED.getCode(), BUSY_REASON);
        log.error("Lock retries exhausted: orderId={} marked FAILED={}", event.orderId(), applied);
    }
}
