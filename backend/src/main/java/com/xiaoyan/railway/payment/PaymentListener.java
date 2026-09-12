package com.xiaoyan.railway.payment;

import com.xiaoyan.railway.common.OrderPaidEvent;
import com.xiaoyan.railway.common.RocketTopics;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = RocketTopics.ORDER_PAID, consumerGroup = "payment-service")
public class PaymentListener implements RocketMQListener<OrderPaidEvent> {
    @Override
    public void onMessage(OrderPaidEvent event) {
        // 支付回调已在前置 mock-notify 中验签+幂等处理；此处预留出票/后续补偿逻辑。
        log.info("Order paid event received: orderId={}, paymentNo={}", event.orderId(), event.paymentNo());
    }
}
