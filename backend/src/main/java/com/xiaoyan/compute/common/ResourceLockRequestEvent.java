package com.xiaoyan.compute.common;

public record ResourceLockRequestEvent(
        String eventId,
        String requestId,
        Long orderId,
        Long skuId,
        String startAt,
        int hours,
        int quantity
) { }
