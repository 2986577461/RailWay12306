package com.xiaoyan.railway.common;

public record OrderPaidEvent(String eventId, Long orderId, String paymentNo) { }
