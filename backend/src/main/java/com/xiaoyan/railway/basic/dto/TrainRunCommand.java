package com.xiaoyan.railway.basic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TrainRunCommand(
        @NotNull(message = "车次不能为空") Long trainId,
        @NotNull(message = "运行日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate runDate
) { }
