package com.xiaoyan.railway.common;

public record InventoryLockedEvent(
        String eventId,
        String requestId,
        Long orderId,
        Long trainRunId,
        Long seatTypeId,
        int fromSeq,
        int toSeq,
        int quantity,
        String lockNo,
        boolean locked,
        String reason
) { }
