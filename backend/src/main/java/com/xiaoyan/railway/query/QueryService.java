package com.xiaoyan.railway.query;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.basic.Fare;
import com.xiaoyan.railway.basic.FareMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
public class QueryService {
    private final QueryMapper queryMapper;
    private final FareMapper fareMapper;
    private final StringRedisTemplate redis;
    private final TrainSearchCache cache;

    public QueryService(QueryMapper queryMapper, FareMapper fareMapper,
                        StringRedisTemplate redis, TrainSearchCache cache) {
        this.queryMapper = queryMapper;
        this.fareMapper = fareMapper;
        this.redis = redis;
        this.cache = cache;
    }

    public Map<String, Object> search(String from, String to, String date) {
        String key = cache.cacheKey(from, to, date);
        return cache.getOrRebuild(key, () -> doSearch(from, to, date));
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
            int total = ((Number) row.get("total")).intValue();
            Map<String, Object> seat = new LinkedHashMap<>(row);
            seat.put("available", available(trainRunId, seatTypeId, fromSeq, toSeq, total));
            seat.put("price", findPrice(trainId, seatTypeId, fromStationId, toStationId));
            seats.add(seat);
        }
        return seats;
    }

    /** Live remaining seats = min over the Redis segment keys spanning [fromSeq, toSeq). */
    private int available(Long trainRunId, Long seatTypeId, int fromSeq, int toSeq, int total) {
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
        return found ? Math.max(min, 0) : total;
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
}
