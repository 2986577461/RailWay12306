package com.xiaoyan.railway.basic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record TrainCommand(
        @NotBlank(message = "车次号不能为空") String trainNo,
        String trainType,
        @Valid @NotEmpty(message = "经停站不能为空") List<StopItem> stops
) {
    public record StopItem(
            @NotNull(message = "车站不能为空") Long stationId,
            @NotNull(message = "经停序号不能为空") Integer stopSeq,
            @JsonFormat(pattern = "HH:mm") LocalTime arriveTime,
            @JsonFormat(pattern = "HH:mm") LocalTime departTime,
            Integer stopMinutes
    ) { }
}
