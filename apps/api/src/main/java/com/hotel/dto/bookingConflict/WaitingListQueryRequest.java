package com.hotel.dto.bookingConflict;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class WaitingListQueryRequest {

    private Long userId;

    private Long roomId;

    private String status; // WAITING, NOTIFIED, EXPIRED, CONFIRMED

    private Integer page = 1;

    private Integer size = 10;

    private String sortBy = "createdAt";

    private String sortDirection = "desc";
}