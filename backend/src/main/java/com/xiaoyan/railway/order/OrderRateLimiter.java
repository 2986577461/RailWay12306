package com.xiaoyan.railway.order;

import com.xiaoyan.railway.common.BizException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fixed-window limit on {@code POST /api/orders/requests}, counted per user in Redis.
 * Runs before the order row is inserted, so a burst never reaches MySQL.
 * Turn off with {@code railway.order.rate-limit.enabled=false} during load tests.
 */
@Component
public class OrderRateLimiter {
    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final int windowSeconds;
    private final int maxRequests;
    private final DefaultRedisScript<Long> script;

    public OrderRateLimiter(StringRedisTemplate redis,
                            @Value("${railway.order.rate-limit.enabled:true}") boolean enabled,
                            @Value("${railway.order.rate-limit.window-seconds:10}") int windowSeconds,
                            @Value("${railway.order.rate-limit.max-requests:5}") int maxRequests) {
        if (windowSeconds < 1 || maxRequests < 1) {
            throw new IllegalArgumentException("下单限流窗口和次数必须大于 0");
        }
        this.redis = redis;
        this.enabled = enabled;
        this.windowSeconds = windowSeconds;
        this.maxRequests = maxRequests;
        this.script = new DefaultRedisScript<>();
        this.script.setResultType(Long.class);
        // INCR 与 EXPIRE 放在同一段脚本里，避免计数成功后进程退出、键永远不过期。
        this.script.setScriptText("""
                local current = redis.call('INCR', KEYS[1])
                if current == 1 then
                    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
                end
                return current
                """);
    }

    /** No-op when the switch is off. Otherwise rejects once the window count exceeds the max. */
    public void check(Long userId) {
        if (!enabled) {
            return;
        }
        Long current = redis.execute(script, List.of("order:rate:" + userId), Integer.toString(windowSeconds));
        if (current == null || current > maxRequests) {
            throw new BizException("下单太频繁，请稍后再试");
        }
    }
}
