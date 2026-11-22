package com.xiaoyan.compute.payment;

import com.xiaoyan.compute.common.OrderPaidEvent;
import com.xiaoyan.compute.common.RocketTopics;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Component
public class OrderPaidPublisher {
    private final RocketMQTemplate rocketMQTemplate;

    public OrderPaidPublisher(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void publishAfterCommit(Payment payment) {
        OrderPaidEvent event = new OrderPaidEvent(UUID.randomUUID().toString(), payment.getOrderId(), payment.getPaymentNo());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rocketMQTemplate.convertAndSend(RocketTopics.ORDER_PAID, event);
                }
            });
        } else {
            rocketMQTemplate.convertAndSend(RocketTopics.ORDER_PAID, event);
        }
    }
}
