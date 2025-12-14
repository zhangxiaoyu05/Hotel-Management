package com.hotel.dto.report;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 收入报表DTO
 * 用于返回收入统计报表数据
 */
@Data
public class RevenueReportDTO {

    /**
     * 查询时间范围
     */
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * 总收入
     */
    private BigDecimal totalRevenue;

    /**
     * 按月份统计收入
     */
    private Map<String, BigDecimal> monthlyRevenue;

    /**
     * 按房型统计收入
     */
    private Map<String, BigDecimal> revenueByRoomType;

    /**
     * 平均每日房价 (ADR)
     */
    private BigDecimal averageDailyRate;

    /**
     * 每间可售房收入 (RevPAR)
     */
    private BigDecimal revenuePerAvailableRoom;

    /**
     * 入住率
     */
    private Double occupancyRate;

    /**
     * 收入增长率（与上一周期对比）
     */
    private Double revenueGrowthRate;

    /**
     * 日收入趋势
     */
    private List<DailyRevenueData> dailyRevenueTrends;

    /**
     * 房型收入贡献排行
     */
    private List<RoomTypeRevenueContribution> roomTypeRevenueContributions;

    /**
     * 收入预测数据（未来3个月）
     */
    private List<RevenueForecast> revenueForecasts;

    /**
     * 日收入数据项
     */
    @Data
    public static class DailyRevenueData {
        private String date;
        private BigDecimal revenue;
        private Long orderCount;
        private BigDecimal averageOrderValue;
    }

    /**
     * 房型收入贡献数据
     */
    @Data
    public static class RoomTypeRevenueContribution {
        private String roomTypeName;
        private BigDecimal revenue;
        private Double percentage;
        private Long orderCount;
        private BigDecimal averageOrderValue;
    }

    /**
     * 收入预测数据
     */
    @Data
    public static class RevenueForecast {
        private String period;
        private BigDecimal predictedRevenue;
        private BigDecimal confidenceLevel;
        private Double growthRate;
    }
}