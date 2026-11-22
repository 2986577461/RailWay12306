package com.xiaoyan.compute.order;

import com.xiaoyan.compute.common.ResourceLockedEvent;
import com.xiaoyan.compute.common.RocketTopics;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = RocketTopics.RESOURCE_LOCKED, consumerGroup = "compute-order-service")
public class OrderLockedListener implements RocketMQListener<ResourceLockedEvent> {
    private final OrderService orderService;

    public OrderLockedListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void onMessage(ResourceLockedEvent event) {
        orderService.applyLockResult(event);
        log.info("Resource lock result orderId={} locked={} reason={}", event.orderId(), event.locked(), event.reason());
    }
}
