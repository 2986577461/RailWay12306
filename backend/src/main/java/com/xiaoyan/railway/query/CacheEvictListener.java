package com.xiaoyan.railway.query;

import com.xiaoyan.railway.common.CacheEvictEvent;
import com.xiaoyan.railway.common.RocketTopics;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 库存发生变化（锁定/释放/重建）时，通过 MQ 可靠地驱逐查询缓存，
 * 避免库存变化方直接操作 Redis 失败时缓存与库存不一致。
 */
@Component
@RocketMQMessageListener(topic = RocketTopics.CACHE_EVICT, consumerGroup = "query-cache-service")
public class CacheEvictListener implements RocketMQListener<CacheEvictEvent> {
    private final TrainSearchCache trainSearchCache;

    public CacheEvictListener(TrainSearchCache trainSearchCache) {
        this.trainSearchCache = trainSearchCache;
    }

    @Override
    public void onMessage(CacheEvictEvent event) {
        trainSearchCache.evictAll();
    }
}
