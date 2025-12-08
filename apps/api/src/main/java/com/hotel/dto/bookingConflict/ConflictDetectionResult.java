package com.hotel.dto.bookingConflict;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class ConflictDetectionResult {

    private boolean hasConflict;

    private Long conflictId;

    private String conflictType;

    private Long conflictingOrderId;

    private String message;

    private List<AlternativeRoom> alternativeRooms;

    private List<String> suggestions;

    @Data
    @Builder
    public static class AlternativeRoom {
        private Long roomId;
        private String roomNumber;
        private String roomType;
        private String description;
        private java.math.BigDecimal price;
        private java.math.BigDecimal originalPrice;
        private Integer discount;
        private boolean available;
        private String imageUrl;
    }
}