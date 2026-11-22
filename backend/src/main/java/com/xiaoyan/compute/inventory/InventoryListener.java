package com.xiaoyan.compute.inventory;

import com.xiaoyan.compute.common.ResourceLockRequestEvent;
import com.xiaoyan.compute.common.ResourceLockedEvent;
import com.xiaoyan.compute.common.RocketTopics;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RocketMQMessageListener(topic = RocketTopics.RESOURCE_LOCK_REQUEST, consumerGroup = "compute-inventory-service")
public class InventoryListener implements RocketMQListener<ResourceLockRequestEvent> {
    private final RocketMQTemplate rocketMQTemplate;
    private final ResourceLocker resourceLocker;

    public InventoryListener(RocketMQTemplate rocketMQTemplate, ResourceLocker resourceLocker) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.resourceLocker = resourceLocker;
    }

    @Override
    public void onMessage(ResourceLockRequestEvent request) {
        LocalDateTime startAt = LocalDateTime.parse(request.startAt());
        boolean locked = resourceLocker.lock(request.orderId(), request.skuId(), startAt,
                request.hours(), request.quantity());
        ResourceLockedEvent result = new ResourceLockedEvent(UUID.randomUUID().toString(), request.orderId(),
                request.skuId(), request.startAt(), request.hours(), request.quantity(),
                "LOCK-" + request.orderId(), locked, locked ? null : "该租期余量不足");
        rocketMQTemplate.convertAndSend(RocketTopics.RESOURCE_LOCKED, result);
    }
}
