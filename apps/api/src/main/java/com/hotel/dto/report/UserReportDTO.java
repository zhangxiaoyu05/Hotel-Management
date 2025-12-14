package com.hotel.dto.report;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用户报表DTO
 * 用于返回用户统计报表数据
 */
@Data
public class UserReportDTO {

    /**
     * 查询时间范围
     */
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * 总用户数
     */
    private Long totalUsers;

    /**
     * 按月份统计新增用户数
     */
    private Map<String, Long> newUsersByMonth;

    /**
     * 活跃用户数（在指定时间段内有订单的用户）
     */
    private Long activeUsers;

    /**
     * 用户留存率
     */
    private Double userRetentionRate;

    /**
     * 用户转化率（注册后下单的用户比例）
     */
    private Double userConversionRate;

    /**
     * 按角色统计用户数量
     */
    private Map<String, Long> usersByRole;

    /**
     * 下单次数最多的用户排行
     */
    private List<UserOrderSummary> topUsersByOrders;

    /**
     * 消费金额最多的用户排行
     */
    private List<UserSpendingSummary> topUsersBySpending;

    /**
     * 用户注册趋势
     */
    private List<UserRegistrationTrend> userRegistrationTrends;

    /**
     * 用户行为分析数据
     */
    private UserBehaviorAnalysis userBehaviorAnalysis;

    /**
     * 用户订单汇总数据
     */
    @Data
    public static class UserOrderSummary {
        private Long userId;
        private String username;
        private String email;
        private Long orderCount;
        private java.time.LocalDateTime lastOrderDate;
    }

    /**
     * 用户消费汇总数据
     */
    @Data
    public static class UserSpendingSummary {
        private Long userId;
        private String username;
        private String email;
        private java.math.BigDecimal totalSpending;
        private Long orderCount;
        private java.math.BigDecimal averageOrderValue;
    }

    /**
     * 用户注册趋势数据
     */
    @Data
    public static class UserRegistrationTrend {
        private String date;
        private Long newUserCount;
        private Long cumulativeUserCount;
    }

    /**
     * 用户行为分析数据
     */
    @Data
    public static class UserBehaviorAnalysis {
        private Double averageOrdersPerUser;
        private java.math.BigDecimal averageSpendingPerUser;
        private Double repeatPurchaseRate;
        private java.time.LocalDateTime averageBookingTime;
        private Map<String, Long> bookingTimeDistribution;
    }
}