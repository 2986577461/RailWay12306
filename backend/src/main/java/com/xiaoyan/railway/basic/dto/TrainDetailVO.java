package com.xiaoyan.railway.basic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.List;

public record TrainDetailVO(
        Long id,
        String trainNo,
        String trainType,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        List<StopVO> stops
) {
    public record StopVO(
            Long stationId,
            String stationName,
            Integer stopSeq,
            @JsonFormat(pattern = "HH:mm") LocalTime arriveTime,
            @JsonFormat(pattern = "HH:mm") LocalTime departTime,
            Integer stopMinutes
    ) { }
}
