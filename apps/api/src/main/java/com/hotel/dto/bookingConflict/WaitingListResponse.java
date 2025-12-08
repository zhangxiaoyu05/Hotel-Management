package com.hotel.dto.bookingConflict;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class WaitingListResponse {

    private Long waitingListId;

    private Long roomId;

    private String roomNumber;

    private String roomType;

    private String status;

    private Integer priority;

    private LocalDateTime requestedCheckInDate;

    private LocalDateTime requestedCheckOutDate;

    private Integer guestCount;

    private Long estimatedWaitTime;

    private Integer currentPosition;

    private LocalDateTime notifiedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private String specialRequests;
}