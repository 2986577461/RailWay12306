package com.xiaoyan.railway.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TicketRequestCommand(
        @NotNull Long trainRunId,
        @NotNull Long seatTypeId,
        @NotNull Long fromStationId,
        @NotNull Long toStationId,
        @Min(1) int fromSeq,
        @Min(2) int toSeq,
        @NotNull List<Long> passengerIds
){}