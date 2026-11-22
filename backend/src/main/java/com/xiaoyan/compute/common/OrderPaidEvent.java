package com.xiaoyan.compute.common;

public record OrderPaidEvent(String eventId, Long orderId, String paymentNo) { }
