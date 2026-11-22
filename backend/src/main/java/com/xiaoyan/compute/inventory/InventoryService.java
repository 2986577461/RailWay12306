package com.xiaoyan.compute.inventory;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.compute.catalog.GpuInventorySlot;
import com.xiaoyan.compute.catalog.GpuInventorySlotMapper;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryService {
    private final StringRedisTemplate redis;
    private final GpuInventorySlotMapper slotMapper;

    public InventoryService(StringRedisTemplate redis, GpuInventorySlotMapper slotMapper) {
        this.redis = redis;
        this.slotMapper = slotMapper;
    }

    public void reloadAll() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        List<GpuInventorySlot> slots = slotMapper.selectList(Wrappers.<GpuInventorySlot>lambdaQuery()
                .ge(GpuInventorySlot::getSlotStart, now)
                .lt(GpuInventorySlot::getSlotStart, now.plusHours(72)));
        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (GpuInventorySlot slot : slots) {
            keys.add(InventoryKeys.slot(slot.getSkuId(), slot.getSlotStart()));
            values.add(Integer.toString(Math.max(0,
                    slot.getTotalCount() - slot.getLockedCount() - slot.getSoldCount())));
        }
        if (keys.isEmpty()) {
            return;
        }
        redis.executePipelined((RedisCallback<Object>) connection -> {
            for (int i = 0; i < keys.size(); i++) {
                connection.stringCommands().set(
                        keys.get(i).getBytes(StandardCharsets.UTF_8),
                        values.get(i).getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
    }

    public void release(Long orderId, Long skuId, LocalDateTime startAt, int hours, int quantity) {
        String marker = InventoryKeys.marker(skuId, orderId);
        if (!Boolean.TRUE.equals(redis.hasKey(marker))) {
            return;
        }
        for (String key : InventoryKeys.slots(skuId, startAt, hours)) {
            redis.opsForValue().increment(key, quantity);
        }
        // marker is intentionally kept until TTL expiry so a late MQ redelivery cannot deduct twice.
    }
}
