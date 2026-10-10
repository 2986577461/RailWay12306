package com.xiaoyan.railway.inventory;

import com.xiaoyan.railway.common.CacheEvictEvent;
import com.xiaoyan.railway.common.RocketTopics;
import com.xiaoyan.railway.common.TicketRequestEvent;
import com.xiaoyan.railway.common.InventoryLockedEvent;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;


@Component
@RocketMQMessageListener(topic = RocketTopics.TICKET_REQUEST, consumerGroup = "inventory-service")
public class InventoryListener implements RocketMQListener<TicketRequestEvent> {
    private final RocketMQTemplate rocketMQTemplate;
    private final SegmentInventoryLocker segmentInventoryLocker;

    public InventoryListener(RocketMQTemplate rocketMQTemplate, SegmentInventoryLocker segmentInventoryLocker) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.segmentInventoryLocker = segmentInventoryLocker;
    }

    @Override
    public void onMessage(TicketRequestEvent request) {
        boolean locked = segmentInventoryLocker.lock(request.orderId(), request.trainRunId(), request.seatTypeId(),
                request.fromSeq(), request.toSeq(), request.quantity());
        InventoryLockedEvent event = new InventoryLockedEvent(request.eventId(), request.requestId(), request.orderId(),
                request.trainRunId(), request.seatTypeId(), request.fromSeq(), request.toSeq(), request.quantity(),
                "LOCK-" + request.requestId(), locked, locked ? null : "余票不足", request.passengerIds());
        rocketMQTemplate.convertAndSend(RocketTopics.INVENTORY_LOCKED, event);
        if (locked) {
            rocketMQTemplate.convertAndSend(RocketTopics.CACHE_EVICT,
                    new CacheEvictEvent(String.valueOf(IdWorker.getId())));
        }
    }
}