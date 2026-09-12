package com.xiaoyan.railway.payment;

import com.xiaoyan.railway.common.RocketTopics;
import com.xiaoyan.railway.common.OrderPaidEvent;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RocketMQMessageListener(topic = RocketTopics.ORDER_PAID, consumerGroup = "payment-service")
public class PaymentListener implements RocketMQListener<OrderPaidEvent> {
    @Override public void onMessage(OrderPaidEvent event) {
        // Payment provider callbacks are verified and made idempotent before publishing this event.
    }
}
