package com.xiaoyan.railway.common;

import java.util.List;

public record TicketRequestEvent(
        String eventId,
        String requestId,
        Long orderId,
        Long userId,
        Long trainRunId,
        Long seatTypeId,
        int fromSeq,
        int toSeq,
        int quantity,
        List<Long> passengerIds
) { }
