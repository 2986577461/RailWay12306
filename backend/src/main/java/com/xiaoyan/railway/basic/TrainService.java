package com.xiaoyan.railway.basic;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.basic.dto.TrainCommand;
import com.xiaoyan.railway.basic.dto.TrainDetailVO;
import com.xiaoyan.railway.common.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TrainService {
    private final TrainMapper trainMapper;
    private final TrainStopMapper trainStopMapper;
    private final StationMapper stationMapper;

    public TrainService(TrainMapper trainMapper, TrainStopMapper trainStopMapper, StationMapper stationMapper) {
        this.trainMapper = trainMapper;
        this.trainStopMapper = trainStopMapper;
        this.stationMapper = stationMapper;
    }

    public List<Train> list() {
        return trainMapper.selectList(Wrappers.<Train>lambdaQuery().orderByAsc(Train::getTrainNo));
    }

    @Transactional
    public Long create(TrainCommand command) {
        List<TrainCommand.StopItem> stops = sortedStops(command.stops());
        Train train = Train.builder()
                .trainNo(command.trainNo())
                .trainType(command.trainType())
                .startStationId(stops.getFirst().stationId())
                .endStationId(stops.getLast().stationId())
                .startTime(stops.getFirst().departTime())
                .endTime(stops.getLast().arriveTime())
                .status(1)
                .build();
        trainMapper.insert(train);
        insertStops(train.getId(), stops);
        return train.getId();
    }

    public TrainDetailVO detail(Long id) {
        Train train = trainMapper.selectById(id);
        if (train == null) {
            throw new BizException("车次不存在");
        }
        List<TrainStop> stops = trainStopMapper.selectList(Wrappers.<TrainStop>lambdaQuery()
                .eq(TrainStop::getTrainId, id)
                .orderByAsc(TrainStop::getStopSeq));
        Map<Long, String> stationNames = loadStationNames(stops);
        List<TrainDetailVO.StopVO> stopVOs = stops.stream()
                .map(s -> new TrainDetailVO.StopVO(
                        s.getStationId(), stationNames.get(s.getStationId()), s.getStopSeq(),
                        s.getArriveTime(), s.getDepartTime(), s.getStopMinutes()))
                .toList();
        return new TrainDetailVO(train.getId(), train.getTrainNo(), train.getTrainType(),
                train.getStartTime(), train.getEndTime(), stopVOs);
    }

    @Transactional
    public void update(Long id, TrainCommand command) {
        Train train = trainMapper.selectById(id);
        if (train == null) {
            throw new BizException("车次不存在");
        }
        List<TrainCommand.StopItem> stops = sortedStops(command.stops());
        train.setTrainNo(command.trainNo());
        train.setTrainType(command.trainType());
        train.setStartStationId(stops.get(0).stationId());
        train.setEndStationId(stops.get(stops.size() - 1).stationId());
        train.setStartTime(stops.get(0).departTime());
        train.setEndTime(stops.get(stops.size() - 1).arriveTime());
        trainMapper.updateById(train);
        trainStopMapper.delete(Wrappers.<TrainStop>lambdaQuery().eq(TrainStop::getTrainId, id));
        insertStops(id, stops);
    }

    @Transactional
    public void delete(Long id) {
        trainStopMapper.delete(Wrappers.<TrainStop>lambdaQuery().eq(TrainStop::getTrainId, id));
        trainMapper.deleteById(id);
    }

    private List<TrainCommand.StopItem> sortedStops(List<TrainCommand.StopItem> stops) {
        if (stops == null || stops.isEmpty()) {
            throw new BizException("经停站不能为空");
        }
        List<TrainCommand.StopItem> sorted = new ArrayList<>(stops);
        sorted.sort(Comparator.comparingInt(TrainCommand.StopItem::stopSeq));
        return sorted;
    }

    private void insertStops(Long trainId, List<TrainCommand.StopItem> stops) {
        for (TrainCommand.StopItem s : stops) {
            trainStopMapper.insert(TrainStop.builder()
                    .trainId(trainId)
                    .stationId(s.stationId())
                    .stopSeq(s.stopSeq())
                    .arriveTime(s.arriveTime())
                    .departTime(s.departTime())
                    .stopMinutes(s.stopMinutes())
                    .build());
        }
    }

    private Map<Long, String> loadStationNames(List<TrainStop> stops) {
        List<Long> stationIds = stops.stream().map(TrainStop::getStationId).distinct().toList();
        if (stationIds.isEmpty()) {
            return Map.of();
        }
        return stationMapper.selectBatchIds(stationIds).stream()
                .collect(Collectors.toMap(Station::getId, Station::getStationName));
    }
}