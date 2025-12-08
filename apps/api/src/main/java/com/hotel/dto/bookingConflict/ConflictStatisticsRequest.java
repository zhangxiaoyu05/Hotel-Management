package com.hotel.dto.bookingConflict;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class ConflictStatisticsRequest {

    private Long roomId;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String groupBy; // day, week, month, conflictType, roomId
}