package com.xiaoyan.compute.order;

import com.xiaoyan.compute.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OrderRateLimiter {
    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final int windowSeconds;
    private final int maxRequests;

    public OrderRateLimiter(StringRedisTemplate redis,
                            @Value("${compute.order.rate-limit.enabled:false}") boolean enabled,
                            @Value("${compute.order.rate-limit.window-seconds:10}") int windowSeconds,
                            @Value("${compute.order.rate-limit.max-requests:3}") int maxRequests) {
        this.redis = redis;
        this.enabled = enabled;
        this.windowSeconds = windowSeconds;
        this.maxRequests = maxRequests;
    }

    public void check(Long userId) {
        if (!enabled) return;
        String key = "compute:order:rate:" + userId;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofSeconds(windowSeconds));
        }
        if (count != null && count > maxRequests) {
            throw new BizException("下单太频繁，请稍后再试");
        }
    }
}
