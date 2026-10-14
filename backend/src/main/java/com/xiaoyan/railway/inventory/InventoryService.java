package com.xiaoyan.railway.inventory;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.basic.SeatType;
import com.xiaoyan.railway.basic.SeatTypeMapper;
import com.xiaoyan.railway.basic.TrainRun;
import com.xiaoyan.railway.basic.TrainRunMapper;
import com.xiaoyan.railway.common.CacheEvictEvent;
import com.xiaoyan.railway.common.RocketTopics;
import com.xiaoyan.railway.order.Order;
import com.xiaoyan.railway.order.OrderRepository;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Redis segment inventory is derived from the order ledger: remaining = seat total minus the stock
 * held by orders that have not released it. Key scheme: {@code inventory:{trainRunId}:{seatTypeId}:{segment}}.
 */
@Service
public class InventoryService {
    private static final String REBUILD_GATE = "inventory:rebuilding";
    private static final Duration REBUILD_GATE_TTL = Duration.ofSeconds(120);

    private final StringRedisTemplate redis;
    private final TrainRunMapper trainRunMapper;
    private final InventoryMapper inventoryMapper;
    private final SeatTypeMapper seatTypeMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final OrderRepository orderRepository;
    private final InventoryRedis inventoryRedis;
    private final DefaultRedisScript<Long> cancelScript;
    private final DefaultRedisScript<Long> refundScript;

    public InventoryService(StringRedisTemplate redis, TrainRunMapper trainRunMapper,
                            InventoryMapper inventoryMapper, SeatTypeMapper seatTypeMapper,
                            RocketMQTemplate rocketMQTemplate, OrderRepository orderRepository,
                            InventoryRedis inventoryRedis) {
        this.redis = redis;
        this.trainRunMapper = trainRunMapper;
        this.inventoryMapper = inventoryMapper;
        this.seatTypeMapper = seatTypeMapper;
        this.rocketMQTemplate = rocketMQTemplate;
        this.orderRepository = orderRepository;
        this.inventoryRedis = inventoryRedis;
        this.cancelScript = script("inventory-cancel.lua");
        this.refundScript = script("inventory-refund.lua");
    }

    private static DefaultRedisScript<Long> script(String location) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(location));
        script.setResultType(Long.class);
        return script;
    }

    /** 发 MQ 让 {@code CacheEvictListener} 可靠地驱逐查询缓存，而不是在本进程直接操作 Redis。 */
    private void evictSearchCache() {
        rocketMQTemplate.convertAndSend(RocketTopics.CACHE_EVICT, new CacheEvictEvent(String.valueOf(IdWorker.getId())));
    }

    /** Rebuild every active run from the order ledger (startup and admin reset). */
    public void reloadAll() {
        rebuild(trainRunMapper.selectList(Wrappers.<TrainRun>lambdaQuery().eq(TrainRun::getStatus, 1)));
    }

    /** Rebuild one newly opened run. */
    public void seedRun(TrainRun run) {
        rebuild(List.of(run));
    }

    private void rebuild(List<TrainRun> runs) {
        if (runs.isEmpty()) {
            return;
        }
        for (TrainRun run : runs) {
            ensureSeatInventory(run.getId());
        }
        // 门闩在读账本之前打开：闸门开启后所有库存脚本拒绝执行，快照之后不会有扣减或返还落进来再被覆盖
        redis.opsForValue().set(REBUILD_GATE, "1", REBUILD_GATE_TTL);
        try {
            writeFromLedger(runs);
        } finally {
            redis.delete(REBUILD_GATE);
        }
        evictSearchCache();
    }

    /**
     * Writes remaining = total minus held for every segment, and restores the lock record of each
     * holding order, so a later release returns exactly what the ledger says is held.
     */
    private void writeFromLedger(List<TrainRun> runs) {
        List<Long> trainIds = runs.stream().map(TrainRun::getTrainId).distinct().toList();
        List<Long> runIds = runs.stream().map(TrainRun::getId).toList();

        Map<Long, Integer> segmentCountByTrain = inventoryMapper.segmentCounts(trainIds).stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("trainId")).longValue(),
                        m -> ((Number) m.get("segmentCount")).intValue()));

        Map<Long, List<SeatTotal>> totalsByRun = inventoryMapper.seatTotalsByRuns(runIds).stream()
                .collect(Collectors.groupingBy(
                        m -> ((Number) m.get("trainRunId")).longValue(),
                        Collectors.mapping(m -> new SeatTotal(
                                ((Number) m.get("seatTypeId")).longValue(),
                                ((Number) m.get("total")).intValue()), Collectors.toList())));

        List<Order> holders = orderRepository.findStockHolders(runIds);
        Map<String, Integer> held = new HashMap<>();
        for (Order order : holders) {
            for (String key : segmentKeys(order)) {
                held.merge(key, order.getQuantity(), Integer::sum);
            }
        }

        Map<String, String> remaining = new LinkedHashMap<>();
        for (TrainRun run : runs) {
            Integer segments = segmentCountByTrain.get(run.getTrainId());
            List<SeatTotal> totals = totalsByRun.get(run.getId());
            if (segments == null || segments < 2 || totals == null) {
                continue;
            }
            for (SeatTotal st : totals) {
                for (int seg = 1; seg < segments; seg++) {
                    String key = segmentKey(run.getId(), st.seatTypeId(), seg);
                    remaining.put(key, String.valueOf(st.total() - held.getOrDefault(key, 0)));
                }
            }
        }

        Map<String, String> lockRecords = new LinkedHashMap<>();
        for (Order order : holders) {
            lockRecords.put(lockRecordKey(order.getId()), "1");
        }
        for (Order order : orderRepository.findCancelledUnreleasedByRuns(runIds)) {
            lockRecords.put(lockRecordKey(order.getId()), "RELEASED");
        }
        writeAtomically(remaining, lockRecords);
    }

    /** MULTI/EXEC so readers and scripts never observe a half-written rebuild. */
    private void writeAtomically(Map<String, String> remaining, Map<String, String> lockRecords) {
        redis.execute((RedisCallback<Object>) connection -> {
            connection.multi();
            remaining.forEach((k, v) -> connection.stringCommands().set(bytes(k), bytes(v)));
            lockRecords.forEach((k, v) -> connection.stringCommands().set(bytes(k), bytes(v),
                    Expiration.seconds(SegmentInventoryLocker.LOCK_RECORD_TTL_SECONDS), SetOption.upsert()));
            connection.exec();
            return null;
        });
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * New trains/runs have no {@code seat_inventory} rows. Query reads that table for {@code seats},
     * so missing rows show up as {@code seats: []}. Fill every seat type with a default total.
     */
    private void ensureSeatInventory(Long trainRunId) {
        List<Map<String, Object>> existing = inventoryMapper.seatTotals(trainRunId);
        Set<Long> have = new HashSet<>();
        for (Map<String, Object> row : existing) {
            have.add(((Number) row.get("seatTypeId")).longValue());
        }
        List<SeatType> types = seatTypeMapper.selectList(null);
        for (SeatType type : types) {
            if (have.contains(type.getId())) {
                continue;
            }
            inventoryMapper.insertSeatInventory(IdWorker.getId(), trainRunId, type.getId(), defaultTotal(type.getCode()));
        }
    }

    private int defaultTotal(String code) {
        if (code == null) {
            return 100;
        }
        return switch (code) {
            case "SECOND_CLASS" -> 600;
            case "FIRST_CLASS" -> 100;
            case "BUSINESS" -> 16;
            default -> 100;
        };
    }

    /**
     * Refund of a PAID order. PAID implies the lock succeeded, so the stock always goes back.
     * The script is idempotent through the RELEASED tombstone, so a retried refund cannot return it twice.
     */
    public void refundOrder(Order order) {
        if (segmentHasNull(order)) {
            return;
        }
        inventoryRedis.run(refundScript, stockKeys(order),
                String.valueOf(order.getQuantity()), String.valueOf(SegmentInventoryLocker.LOCK_RECORD_TTL_SECONDS));
        evictSearchCache();
    }

    /**
     * Timeout cancel of an UNPAID order. Returns stock only if this order's lock actually deducted it;
     * otherwise leaves a tombstone so a lock event arriving later cannot deduct for a cancelled order.
     */
    public void cancelUnpaidOrder(Order order) {
        if (segmentHasNull(order)) {
            return;
        }
        inventoryRedis.run(cancelScript, stockKeys(order),
                String.valueOf(order.getQuantity()), String.valueOf(SegmentInventoryLocker.LOCK_RECORD_TTL_SECONDS));
        evictSearchCache();
    }

    /** Old orders created before the segment columns existed carry null values and are skipped. */
    private static boolean segmentHasNull(Order order) {
        return order.getSeatTypeId() == null || order.getFromSeq() == null
                || order.getToSeq() == null || order.getQuantity() == null;
    }

    private static List<String> stockKeys(Order order) {
        List<String> keys = new ArrayList<>();
        keys.add(lockRecordKey(order.getId()));
        keys.addAll(segmentKeys(order));
        return keys;
    }

    private static List<String> segmentKeys(Order order) {
        return IntStream.range(order.getFromSeq(), order.getToSeq())
                .mapToObj(seg -> segmentKey(order.getTrainRunId(), order.getSeatTypeId(), seg))
                .toList();
    }

    private static String segmentKey(Long trainRunId, Long seatTypeId, int segment) {
        return "inventory:" + trainRunId + ":" + seatTypeId + ":" + segment;
    }

    private static String lockRecordKey(Long orderId) {
        return "inventory:locked:" + orderId;
    }

    private record SeatTotal(Long seatTypeId, int total) { }
}
