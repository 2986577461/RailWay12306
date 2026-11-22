package com.xiaoyan.compute.inventory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResourceLocker {
    public static final int LOCK_RECORD_TTL_SECONDS = 24 * 60 * 60;
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> lockScript;

    public ResourceLocker(StringRedisTemplate redis) {
        this.redis = redis;
        this.lockScript = new DefaultRedisScript<>();
        this.lockScript.setLocation(new ClassPathResource("resource-lock.lua"));
        this.lockScript.setResultType(Long.class);
    }

    public boolean lock(Long orderId, Long skuId, LocalDateTime startAt, int hours, int quantity) {
        List<String> keys = new ArrayList<>();
        keys.add(InventoryKeys.marker(skuId, orderId));
        keys.addAll(InventoryKeys.slots(skuId, startAt, hours));
        Long result = redis.execute(lockScript, keys,
                Integer.toString(quantity), Integer.toString(LOCK_RECORD_TTL_SECONDS));
        return result != null && result > 0;
    }
}
