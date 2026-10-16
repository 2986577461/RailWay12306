package com.xiaoyan.railway.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Bulk cancel orders matching train run, seat type, segment, and passenger IDs.
 */
public record BulkCancelCommand(
        @NotNull Long trainRunId,
        @NotNull Long seatTypeId,
        @NotNull Long fromStationId,
        @NotNull Long toStationId,
        @NotNull Integer fromSeq,
        @NotNull Integer toSeq,
        @NotEmpty List<Long> passengerIds
) {
}
