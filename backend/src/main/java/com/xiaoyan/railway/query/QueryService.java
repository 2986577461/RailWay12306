package com.xiaoyan.railway.query;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.basic.Fare;
import com.xiaoyan.railway.basic.FareMapper;
import com.xiaoyan.railway.common.BizException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Service
public class QueryService {
    private static final Duration STALE_LIMIT = Duration.ofMinutes(5);
    private static final int STALE_CAPACITY = 2000;

    private final QueryMapper queryMapper;
    private final FareMapper fareMapper;
    private final StringRedisTemplate redis;
    private final TrainSearchCache cache;
    private final CircuitBreaker queryBreaker;
    private final Map<String, Snapshot> lastGood = new ConcurrentHashMap<>();

    public QueryService(QueryMapper queryMapper, FareMapper fareMapper,
                        StringRedisTemplate redis, TrainSearchCache cache,
                        @Qualifier("redisQueryBreaker") CircuitBreaker queryBreaker) {
        this.queryMapper = queryMapper;
        this.fareMapper = fareMapper;
        this.redis = redis;
        this.cache = cache;
        this.queryBreaker = queryBreaker;
    }

    /**
     * Searches through the query breaker. While Redis or MySQL is failing, serves the last good result
     * for up to {@link #STALE_LIMIT}, marked {@code stale}; otherwise reports availability as temporarily unknown.
     */
    public Map<String, Object> search(String from, String to, String date) {
        String key = cache.cacheKey(from, to, date);
        try {
            Map<String, Object> result = queryBreaker.executeSupplier(
                    () -> cache.getOrRebuild(key, () -> doSearch(from, to, date)));
            if (lastGood.size() < STALE_CAPACITY || lastGood.containsKey(key)) {
                lastGood.put(key, new Snapshot(result, Instant.now()));
            }
            return result;
        } catch (CallNotPermittedException | DataAccessException e) {
            return staleOrUnavailable(key);
        }
    }

    private Map<String, Object> staleOrUnavailable(String key) {
        Snapshot snapshot = lastGood.get(key);
        if (snapshot == null || snapshot.at().plus(STALE_LIMIT).isBefore(Instant.now())) {
            throw new BizException("余票暂不可用，请稍后再试");
        }
        Map<String, Object> stale = new LinkedHashMap<>(snapshot.value());
        stale.put("stale", true);
        return stale;
    }

    private Map<String, Object> doSearch(String from, String to, String date) {
        List<Map<String, Object>> trains = queryMapper.searchTrains(from, to, date);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> train : trains) {
            Map<String, Object> item = new LinkedHashMap<>(train);
            item.put("duration", durationText((String) train.get("departTime"), (String) train.get("arriveTime")));
            Long trainRunId = ((Number) train.get("trainRunId")).longValue();
            Long trainId = ((Number) train.get("trainId")).longValue();
            Long fromStationId = ((Number) train.get("fromStationId")).longValue();
            Long toStationId = ((Number) train.get("toStationId")).longValue();
            int fromSeq = ((Number) train.get("fromSeq")).intValue();
            int toSeq = ((Number) train.get("toSeq")).intValue();
            item.put("seats", listSeats(trainRunId, trainId, fromStationId, toStationId, fromSeq, toSeq));
            result.add(item);
        }
        return Map.of("from", from, "to", to, "date", date, "trains", result);
    }

    private List<Map<String, Object>> listSeats(Long trainRunId, Long trainId, Long fromStationId,
                                                Long toStationId, int fromSeq, int toSeq) {
        List<Map<String, Object>> rows = queryMapper.listSeatTypesForRun(trainRunId);
        List<Map<String, Object>> seats = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long seatTypeId = ((Number) row.get("seatTypeId")).longValue();
            Map<String, Object> seat = new LinkedHashMap<>(row);
            seat.put("available", available(trainRunId, seatTypeId, fromSeq, toSeq));
            seat.put("price", findPrice(trainId, seatTypeId, fromStationId, toStationId));
            seats.add(seat);
        }
        return seats;
    }

    /**
     * Live remaining seats = min over the Redis segment keys spanning [fromSeq, toSeq).
     * Missing keys mean the run was never seeded or Redis lost it, so report 0 rather than the planned total.
     */
    private int available(Long trainRunId, Long seatTypeId, int fromSeq, int toSeq) {
        List<String> keys = IntStream.range(fromSeq, toSeq)
                .mapToObj(seg -> "inventory:" + trainRunId + ":" + seatTypeId + ":" + seg)
                .toList();
        List<String> values = redis.opsForValue().multiGet(keys);
        int min = Integer.MAX_VALUE;
        boolean found = false;
        for (String v : values) {
            if (v == null) {
                continue;
            }
            found = true;
            min = Math.min(min, Integer.parseInt(v));
        }
        return found ? Math.max(min, 0) : 0;
    }

    private BigDecimal findPrice(Long trainId, Long seatTypeId, Long fromStationId, Long toStationId) {
        Fare fare = fareMapper.selectOne(Wrappers.<Fare>lambdaQuery()
                .eq(Fare::getTrainId, trainId)
                .eq(Fare::getSeatTypeId, seatTypeId)
                .eq(Fare::getFromStationId, fromStationId)
                .eq(Fare::getToStationId, toStationId));
        return fare == null ? null : fare.getPrice();
    }

    private String durationText(String depart, String arrive) {
        if (depart == null || arrive == null) {
            return "--";
        }
        try {
            LocalTime start = LocalTime.parse(depart);
            LocalTime end = LocalTime.parse(arrive);
            Duration d = Duration.between(start, end);
            if (d.isNegative()) {
                d = d.plusDays(1);
            }
            return d.toHours() + "小时" + d.toMinutesPart() + "分";
        } catch (DateTimeParseException e) {
            return "--";
        }
    }

    private record Snapshot(Map<String, Object> value, Instant at) { }
}
