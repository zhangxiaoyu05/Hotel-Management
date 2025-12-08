package com.hotel.dto.bookingConflict;

import lombok.Data;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ConflictStatisticsResponse {

    private int totalConflicts;

    private int timeOverlapConflicts;

    private int doubleBookingConflicts;

    private int concurrentRequestConflicts;

    private int resolvedConflicts;

    private int waitingListSize;

    private double resolutionRate;

    private List<ConflictTrend> conflictTrends;

    private Map<String, Integer> conflictsByRoom;

    private Map<String, Integer> conflictsByType;

    private List<RoomConflictHotspot> roomHotspots;

    @Data
    @Builder
    public static class ConflictTrend {
        private String period;
        private int conflictCount;
        private int resolvedCount;
        private double resolutionRate;
    }

    @Data
    @Builder
    public static class RoomConflictHotspot {
        private Long roomId;
        private String roomNumber;
        private int conflictCount;
        private int waitingListSize;
        private double conflictRate;
    }
}