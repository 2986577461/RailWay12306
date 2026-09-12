package com.xiaoyan.railway.inventory;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.basic.SeatType;
import com.xiaoyan.railway.basic.SeatTypeMapper;
import com.xiaoyan.railway.basic.TrainRun;
import com.xiaoyan.railway.basic.TrainRunMapper;
import com.xiaoyan.railway.query.TrainSearchCache;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seeds Redis segment-inventory keys from the authoritative {@code seat_inventory} table.
 * Key scheme: {@code inventory:{trainRunId}:{seatTypeId}:{segment}} — the same keys that
 * {@code SegmentInventoryLocker} decrements atomically, so query / lock / release share one source.
 */
@Service
public class InventoryService {
    private final StringRedisTemplate redis;
    private final TrainRunMapper trainRunMapper;
    private final InventoryMapper inventoryMapper;
    private final SeatTypeMapper seatTypeMapper;
    private final TrainSearchCache trainSearchCache;

    public InventoryService(StringRedisTemplate redis, TrainRunMapper trainRunMapper,
                            InventoryMapper inventoryMapper, SeatTypeMapper seatTypeMapper,
                            TrainSearchCache trainSearchCache) {
        this.redis = redis;
        this.trainRunMapper = trainRunMapper;
        this.inventoryMapper = inventoryMapper;
        this.seatTypeMapper = seatTypeMapper;
        this.trainSearchCache = trainSearchCache;
    }

    /**
     * Seed all active runs. Batch DB reads + pipelined Redis writes so this stays fast
     * even for hundreds of runs (the admin reload endpoint and startup both call it).
     */
    public void reloadAll(boolean force) {
        List<TrainRun> runs = trainRunMapper.selectList(Wrappers.<TrainRun>lambdaQuery().eq(TrainRun::getStatus, 1));
        if (runs.isEmpty()) {
            return;
        }
        for (TrainRun run : runs) {
            ensureSeatInventory(run.getId());
        }
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
                                String.valueOf(m.get("total"))), Collectors.toList())));

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (TrainRun run : runs) {
            Integer segments = segmentCountByTrain.get(run.getTrainId());
            List<SeatTotal> totals = totalsByRun.get(run.getId());
            if (segments == null || segments < 2 || totals == null) {
                continue;
            }
            for (SeatTotal st : totals) {
                for (int seg = 1; seg < segments; seg++) {
                    keys.add("inventory:" + run.getId() + ":" + st.seatTypeId() + ":" + seg);
                    values.add(st.total());
                }
            }
        }
        writePipelined(keys, values, force);
        trainSearchCache.evictAll();
    }

    /** Seed one run's segment inventory (newly created run). */
    public void seedRun(TrainRun run, boolean force) {
        Integer segments = inventoryMapper.segmentCount(run.getTrainId());
        if (segments == null || segments < 2) {
            return;
        }
        ensureSeatInventory(run.getId());
        List<Map<String, Object>> seatTotals = inventoryMapper.seatTotals(run.getId());
        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (Map<String, Object> st : seatTotals) {
            Long seatTypeId = ((Number) st.get("seatTypeId")).longValue();
            String total = String.valueOf(st.get("total"));
            for (int seg = 1; seg < segments; seg++) {
                keys.add("inventory:" + run.getId() + ":" + seatTypeId + ":" + seg);
                values.add(total);
            }
        }
        writePipelined(keys, values, force);
        trainSearchCache.evictAll();
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

    /** force=true resets existing counts (开售/调库存); force=false only fills missing keys (startup). */
    private void writePipelined(List<String> keys, List<String> values, boolean force) {
        if (keys.isEmpty()) {
            return;
        }
        redis.executePipelined((RedisCallback<Object>) connection -> {
            for (int i = 0; i < keys.size(); i++) {
                byte[] k = keys.get(i).getBytes(StandardCharsets.UTF_8);
                byte[] v = values.get(i).getBytes(StandardCharsets.UTF_8);
                if (force) {
                    connection.stringCommands().set(k, v);
                } else {
                    connection.stringCommands().setNX(k, v);
                }
            }
            return null;
        });
    }

    private record SeatTotal(Long seatTypeId, String total) { }
}
