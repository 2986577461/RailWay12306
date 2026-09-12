package com.xiaoyan.railway.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDetailVO(
        String orderNo,
        int status,
        String statusText,
        BigDecimal totalAmount,
        LocalDateTime expireAt,
        LocalDateTime createdAt,
        String fromStationName,
        String toStationName,
        String trainNo
) { }
