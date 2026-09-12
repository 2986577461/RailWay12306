package com.xiaoyan.railway.inventory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class SegmentInventoryLocker {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> lockScript;

    public SegmentInventoryLocker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.lockScript = new DefaultRedisScript<>();
        this.lockScript.setLocation(new ClassPathResource("inventory-lock.lua"));
        this.lockScript.setResultType(Long.class);
    }

    public boolean lock(Long trainRunId, Long seatTypeId, int fromSeq, int toSeq, int quantity) {
        List<String> keys = IntStream.range(fromSeq, toSeq)
                .mapToObj(segment -> "inventory:" + trainRunId + ':' + seatTypeId + ':' + segment)
                .toList();
        Long result = redisTemplate.execute(lockScript, keys, Integer.toString(quantity));
        return Long.valueOf(1).equals(result);
    }
}
