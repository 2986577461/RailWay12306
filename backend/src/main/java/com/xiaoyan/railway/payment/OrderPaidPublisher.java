package com.xiaoyan.railway.payment;

import com.xiaoyan.railway.common.OrderPaidEvent;
import com.xiaoyan.railway.common.RocketTopics;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Publishes {@code railway-order-paid} after the current transaction commits.
 * Extracted from {@link PaymentService} so the payment logic stays unit-testable
 * (the static {@link TransactionSynchronizationManager} is only touched here).
 */
@Component
public class OrderPaidPublisher {
    private final RocketMQTemplate rocketMQTemplate;

    public OrderPaidPublisher(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void publishAfterCommit(Payment payment) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rocketMQTemplate.convertAndSend(RocketTopics.ORDER_PAID,
                        new OrderPaidEvent(UUID.randomUUID().toString(), payment.getOrderId(), payment.getPaymentNo()));
            }
        });
    }
}
