package com.hotel.dto.report;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 订单报表DTO
 * 用于返回订单统计报表数据
 */
@Data
public class OrderReportDTO {

    /**
     * 查询时间范围
     */
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * 总订单数
     */
    private Long totalOrders;

    /**
     * 总收入
     */
    private BigDecimal totalRevenue;

    /**
     * 平均订单价值
     */
    private BigDecimal averageOrderValue;

    /**
     * 按状态统计订单数量
     */
    private Map<String, Long> ordersByStatus;

    /**
     * 按房型统计订单数量
     */
    private Map<String, Long> ordersByRoomType;

    /**
     * 按日期统计订单数量
     */
    private Map<String, Long> ordersByDate;

    /**
     * 按月份统计收入
     */
    private Map<String, BigDecimal> revenueByMonth;

    /**
     * 按渠道统计订单数量
     */
    private Map<String, Long> ordersByChannel;

    /**
     * 订单完成率
     */
    private Double completionRate;

    /**
     * 取消率
     */
    private Double cancellationRate;

    /**
     * 订单趋势数据（用于图表展示）
     */
    private List<OrderTrendData> orderTrends;

    /**
     * 房型偏好排行
     */
    private List<RoomTypePreference> roomTypePreferences;

    /**
     * 订单趋势数据项
     */
    @Data
    public static class OrderTrendData {
        private String date;
        private Long orderCount;
        private BigDecimal revenue;
    }

    /**
     * 房型偏好数据
     */
    @Data
    public static class RoomTypePreference {
        private String roomTypeName;
        private Long orderCount;
        private BigDecimal revenue;
        private Double percentage;
    }
}