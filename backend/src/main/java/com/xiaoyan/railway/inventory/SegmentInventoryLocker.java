package com.xiaoyan.railway.inventory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Atomically decrements every segment key in {@code [fromSeq, toSeq)} for a train run & seat type.
 * Idempotent per order: {@code inventory:locked:{orderId}} records the first successful lock, so
 * duplicate events (double submit, MQ redelivery) are no-ops instead of deducting inventory twice.
 */
@Service
public class SegmentInventoryLocker {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> lockScript;

    /**
     * How long the per-order lock record survives. Must be >= the order payment window
     * (railway.order.expire-minutes) so a late duplicate event still sees the marker.
     */
    public static final int LOCK_RECORD_TTL_SECONDS = 24 * 60 * 60;

    public SegmentInventoryLocker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.lockScript = new DefaultRedisScript<>();
        this.lockScript.setLocation(new ClassPathResource("inventory-lock.lua"));
        this.lockScript.setResultType(Long.class);
    }

    /**
     * Try to lock {@code quantity} seats across every segment in {@code [fromSeq, toSeq)}.
     *
     * @return true when locked — either newly, or already locked by this order (idempotent);
     *         false only when stock is insufficient.
     */
    public boolean lock(Long orderId, Long trainRunId, Long seatTypeId, int fromSeq, int toSeq, int quantity) {
        List<String> keys = new ArrayList<>();
        keys.add("inventory:locked:" + orderId);
        IntStream.range(fromSeq, toSeq)
                .mapToObj(segment -> "inventory:" + trainRunId + ':' + seatTypeId + ':' + segment)
                .forEach(keys::add);
        Long result = redisTemplate.execute(lockScript, keys,
                Integer.toString(quantity), Integer.toString(LOCK_RECORD_TTL_SECONDS));
        return result > 0; // 1 = newly locked, 2 = already locked；null(Redis 异常)按失败处理
    }
}