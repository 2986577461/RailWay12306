package com.xiaoyan.railway.basic.dto;

import jakarta.validation.constraints.NotBlank;

public record StationCommand(
        @NotBlank(message = "站码不能为空") String stationCode,
        @NotBlank(message = "站名不能为空") String stationName,
        String cityName,
        String pinyin
) { }
