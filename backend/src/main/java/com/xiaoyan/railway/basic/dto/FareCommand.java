package com.xiaoyan.railway.basic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FareCommand(
        @NotNull(message = "车次不能为空") Long trainId,
        @NotNull(message = "席别不能为空") Long seatTypeId,
        @NotNull(message = "出发站不能为空") Long fromStationId,
        @NotNull(message = "到达站不能为空") Long toStationId,
        @NotNull(message = "票价不能为空") @DecimalMin(value = "0.01", message = "票价必须大于0") BigDecimal price
) { }
