package com.xiaoyan.railway.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Redis cache for train search results, guarding against the three classic cache pitfalls:
 * <ul>
 *   <li>穿透: empty results are cached with a short TTL.</li>
 *   <li>击穿: a SETNX lock lets only one thread rebuild a hot key.</li>
 *   <li>雪崩: TTL is randomized so keys do not expire at the same instant.</li>
 * </ul>
 */
@Service
public class TrainSearchCache {
    private static final Duration BASE_TTL = Duration.ofSeconds(60);
    private static final Duration EMPTY_TTL = Duration.ofSeconds(30);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final int MAX_JITTER_SECONDS = 30;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public TrainSearchCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public String cacheKey(String from, String to, String date) {
        return "query:trains:" + from + ":" + to + ":" + date;
    }

    public void evictAll() {
        Set<String> keys = redis.keys("query:trains:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    public Map<String, Object> getOrRebuild(String key, Supplier<Map<String, Object>> loader) {
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            return readValue(cached);
        }

        // 击穿防护：只有一个线程抢到锁负责重建
        String lockKey = "query:lock:" + key;
        boolean locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL));
        if (locked) {
            try {
                return rebuildAndCache(key, loader);
            } finally {
                redis.delete(lockKey);
            }
        }

        // 未抢到锁：等待重建者写入后重读
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String after = redis.opsForValue().get(key);
        if (after != null) {
            return readValue(after);
        }
        // 兜底：锁已过期仍无写入（极端情况），直接重建
        return rebuildAndCache(key, loader);
    }

    private Map<String, Object> rebuildAndCache(String key, Supplier<Map<String, Object>> loader) {
        Map<String, Object> value = loader.get();
        // 穿透：空结果也缓存（短 TTL）；雪崩：TTL 加随机抖动
        Duration ttl = isEmpty(value)
                ? EMPTY_TTL
                : BASE_TTL.plusSeconds(ThreadLocalRandom.current().nextInt(MAX_JITTER_SECONDS + 1));
        redis.opsForValue().set(key, writeValue(value), ttl);
        return value;
    }

    private boolean isEmpty(Map<String, Object> value) {
        Object trains = value.get("trains");
        return trains instanceof List<?> list && list.isEmpty();
    }

    private String writeValue(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化查询结果失败", e);
        }
    }

    private Map<String, Object> readValue(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("反序列化缓存结果失败", e);
        }
    }
}
