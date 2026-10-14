package com.xiaoyan.railway.inventory;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs inventory scripts behind the inventory circuit breaker. Script result -1 means the ledger
 * rebuild holds the gate; both that and an open breaker surface as {@link InventoryUnavailableException}.
 */
@Component
public class InventoryRedis {
    private static final long REBUILD_GATE_RESULT = -1L;

    private final StringRedisTemplate redis;
    private final CircuitBreaker breaker;

    public InventoryRedis(StringRedisTemplate redis, @Qualifier("redisInventoryBreaker") CircuitBreaker breaker) {
        this.redis = redis;
        this.breaker = breaker;
    }

    public long run(RedisScript<Long> script, List<String> keys, String... args) {
        long result;
        try {
            result = breaker.executeSupplier(() -> redis.execute(script, keys, (Object[]) args));
        } catch (CallNotPermittedException e) {
            throw new InventoryUnavailableException();
        }
        if (result == REBUILD_GATE_RESULT) {
            throw new InventoryUnavailableException();
        }
        return result;
    }

    public boolean isUnavailable() {
        return breaker.getState() == CircuitBreaker.State.OPEN;
    }
}