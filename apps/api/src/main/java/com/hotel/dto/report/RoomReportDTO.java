package com.hotel.dto.report;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 房间报表DTO
 * 用于返回房间统计报表数据
 */
@Data
public class RoomReportDTO {

    /**
     * 查询时间范围
     */
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * 总房间数
     */
    private Long totalRooms;

    /**
     * 整体入住率
     */
    private Double occupancyRate;

    /**
     * 平均房价
     */
    private BigDecimal averageRoomRate;

    /**
     * 按房型统计入住率
     */
    private Map<String, Double> roomUtilization;

    /**
     * 按房型统计收入
     */
    private Map<String, BigDecimal> revenueByRoomType;

    /**
     * 表现最好的房间排行
     */
    private List<RoomPerformance> topPerformingRooms;

    /**
     * 维护中房间数量
     */
    private Long maintenanceRooms;

    /**
     * 可用房间数量
     */
    private Long availableRooms;

    /**
     * 房间使用趋势
     */
    private List<RoomUtilizationTrend> roomUtilizationTrends;

    /**
     * 房型绩效对比
     */
    private List<RoomTypePerformance> roomTypePerformances;

    /**
     * 房间维护统计
     */
    private RoomMaintenanceStats maintenanceStats;

    /**
     * 房间绩效数据
     */
    @Data
    public static class RoomPerformance {
        private Long roomId;
        private String roomNumber;
        private String roomTypeName;
        private Long totalOrders;
        private BigDecimal totalRevenue;
        private BigDecimal averageRevenuePerOrder;
        private Double occupancyRate;
        private Integer totalNights;
        private BigDecimal revenuePerNight;
    }

    /**
     * 房间使用趋势数据
     */
    @Data
    public static class RoomUtilizationTrend {
        private String date;
        private Long totalRooms;
        private Long occupiedRooms;
        private Long availableRooms;
        private Double occupancyRate;
        private BigDecimal dailyRevenue;
    }

    /**
     * 房型绩效数据
     */
    @Data
    public static class RoomTypePerformance {
        private String roomTypeName;
        private Long totalRooms;
        private Long totalOrders;
        private BigDecimal totalRevenue;
        private Double occupancyRate;
        private BigDecimal averageDailyRate;
        private BigDecimal revenuePerAvailableRoom;
        private Double revenueContribution;
    }

    /**
     * 房间维护统计数据
     */
    @Data
    public static class RoomMaintenanceStats {
        private Long currentlyUnderMaintenance;
        private Long totalMaintenanceDays;
        private Map<String, Long> maintenanceReasons;
        private List<MaintenanceRecord> recentMaintenanceRecords;
    }

    /**
     * 维护记录
     */
    @Data
    public static class MaintenanceRecord {
        private Long roomId;
        private String roomNumber;
        private String reason;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer durationDays;
    }
}