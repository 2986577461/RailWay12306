package com.xiaoyan.compute.provision;

import com.xiaoyan.compute.common.OrderPaidEvent;
import com.xiaoyan.compute.common.RocketTopics;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(topic = RocketTopics.ORDER_PAID, consumerGroup = "compute-provision-service")
public class OrderPaidListener implements RocketMQListener<OrderPaidEvent> {
    private final ProvisionService provisionService;

    public OrderPaidListener(ProvisionService provisionService) {
        this.provisionService = provisionService;
    }

    @Override
    public void onMessage(OrderPaidEvent event) {
        provisionService.startProvision(event);
    }
}
