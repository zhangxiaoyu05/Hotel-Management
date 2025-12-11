package com.hotel.dto.admin.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 收入统计DTO
 * 用于展示系统收入分析数据
 */
@Data
@Schema(description = "收入统计数据")
public class RevenueStatisticsDTO {

    @Schema(description = "总收入", example = "156800.00")
    private BigDecimal totalRevenue;

    @Schema(description = "今日收入", example = "5880.50")
    private BigDecimal todayRevenue;

    @Schema(description = "本周收入", example = "38200.00")
    private BigDecimal weeklyRevenue;

    @Schema(description = "本月收入", example = "156800.00")
    private BigDecimal monthlyRevenue;

    @Schema(description = "本年收入", example = "1865000.00")
    private BigDecimal yearlyRevenue;

    @Schema(description = "昨日收入", example = "4250.00")
    private BigDecimal yesterdayRevenue;

    @Schema(description = "上周收入", example = "35600.00")
    private BigDecimal lastWeekRevenue;

    @Schema(description = "上月收入", example = "142300.00")
    private BigDecimal lastMonthRevenue;

    @Schema(description = "去年收入", example = "1650000.00")
    private BigDecimal lastYearRevenue;

    @Schema(description = "日收入增长率（百分比）", example = "38.35")
    private Double dailyGrowthRate;

    @Schema(description = "周收入增长率（百分比）", example = "7.30")
    private Double weeklyGrowthRate;

    @Schema(description = "月收入增长率（百分比）", example = "10.18")
    private Double monthlyGrowthRate;

    @Schema(description = "年收入增长率（百分比）", example = "13.03")
    private Double yearlyGrowthRate;

    @Schema(description = "按房型收入统计")
    private Map<String, BigDecimal> revenueByRoomType;

    @Schema(description = "按支付方式收入统计")
    private Map<String, BigDecimal> revenueByPaymentMethod;

    @Schema(description = "按收入来源统计（线上/线下）")
    private Map<String, BigDecimal> revenueBySource;

    @Schema(description = "平均订单金额", example = "1420.00")
    private BigDecimal averageOrderValue;

    @Schema(description = "预计月收入", example = "172000.00")
    private BigDecimal projectedMonthlyRevenue;

    @Schema(description = "预计年收入", example = "2064000.00")
    private BigDecimal projectedYearlyRevenue;

    @Schema(description = "收入目标完成度（百分比）", example = "85.2")
    private Double revenueTargetCompletion;
}