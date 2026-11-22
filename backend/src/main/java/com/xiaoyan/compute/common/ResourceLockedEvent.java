package com.xiaoyan.compute.common;

public record ResourceLockedEvent(
        String eventId,
        Long orderId,
        Long skuId,
        String startAt,
        int hours,
        int quantity,
        String lockNo,
        boolean locked,
        String reason
) { }
