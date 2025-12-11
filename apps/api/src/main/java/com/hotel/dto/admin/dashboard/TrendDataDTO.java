package com.hotel.dto.admin.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 趋势数据DTO
 * 用于展示系统历史趋势分析
 */
@Data
@Schema(description = "仪表板趋势数据")
public class TrendDataDTO {

    @Schema(description = "统计周期", example = "DAILY")
    private String period;

    @Schema(description = "日期列表")
    private List<String> dates;

    @Schema(description = "订单数量列表")
    private List<Integer> orderCounts;

    @Schema(description = "收入列表")
    private List<BigDecimal> revenues;

    @Schema(description = "入住率列表")
    private List<Double> occupancies;

    @Schema(description = "新增用户数列表")
    private List<Integer> newUsersCounts;

    @Schema(description = "平均评分列表")
    private List<Double> averageRatings;

    @Schema(description = "总订单数", example = "1250")
    private Integer totalOrders;

    @Schema(description = "总收入", example = "286500.00")
    private BigDecimal totalRevenue;

    @Schema(description = "平均入住率", example = "72.5")
    private Double averageOccupancy;

    @Schema(description = "平均日订单数", example = "41.67")
    private Double averageDailyOrders;

    @Schema(description = "平均日收入", example = "9550.00")
    private BigDecimal averageDailyRevenue;

    @Schema(description = "同比增长率（订单数）", example = "15.3")
    private Double ordersGrowthRate;

    @Schema(description = "同比增长率（收入）", example = "12.8")
    private Double revenueGrowthRate;

    @Schema(description = "同比增长率（入住率）", example = "5.2")
    private Double occupancyGrowthRate;
}