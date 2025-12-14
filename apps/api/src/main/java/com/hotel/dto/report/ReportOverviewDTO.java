package com.hotel.dto.report;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报表概览DTO
 * 用于返回所有报表的核心指标概览
 */
@Data
public class ReportOverviewDTO {

    /**
     * 数据更新时间
     */
    private LocalDate lastUpdated;

    /**
     * 今日订单数
     */
    private Long todayOrders;

    /**
     * 今日收入
     */
    private BigDecimal todayRevenue;

    /**
     * 当前入住率
     */
    private Double currentOccupancyRate;

    /**
     * 本月新增用户数
     */
    private Long monthlyNewUsers;

    /**
     * 本月总收入
     */
    private BigDecimal monthlyRevenue;

    /**
     * 可用房间数
     */
    private Long availableRooms;

    /**
     * 维护中房间数
     */
    private Long maintenanceRooms;

    /**
     * 活跃用户数（近30天）
     */
    private Long activeUsers;

    /**
     * 本月订单完成率
     */
    private Double monthlyCompletionRate;

    /**
     * 平均每日房价
     */
    private BigDecimal averageDailyRate;
}