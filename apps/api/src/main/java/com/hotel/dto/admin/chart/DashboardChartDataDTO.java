package com.hotel.dto.admin.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仪表板图表数据汇总
 */
@Data
@Schema(description = "仪表板图表数据汇总")
public class DashboardChartDataDTO {

    @Schema(description = "图表数据总标题", example = "仪表板图表数据")
    private String title;

    @Schema(description = "数据时间段", example = "2025-12-01 至 2025-12-31")
    private String period;

    @Schema(description = "订单趋势图表数据")
    private OrderTrendChartDTO orderTrendChart;

    @Schema(description = "收入分析图表数据")
    private RevenueChartDTO revenueChart;

    @Schema(description = "入住率图表数据")
    private OccupancyChartDTO occupancyChart;

    @Schema(description = "房间状态饼图数据")
    private PieChartDTO roomStatusPieChart;

    @Schema(description = "订单状态饼图数据")
    private PieChartDTO orderStatusPieChart;

    @Schema(description = "房型收入饼图数据")
    private PieChartDTO revenuePieChart;

    @Schema(description = "数据更新时间")
    private String lastUpdateTime;

    @Schema(description = "数据缓存状态", example = "CACHED")
    private String cacheStatus;
}