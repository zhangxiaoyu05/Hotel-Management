package com.hotel.dto.admin.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单趋势图表数据
 */
@Data
@Schema(description = "订单趋势图表数据")
public class OrderTrendChartDTO {

    @Schema(description = "图表标题", example = "订单趋势分析")
    private String title;

    @Schema(description = "图表副标题", example = "2025-12-01 至 2025-12-31")
    private String subtitle;

    @Schema(description = "X轴标签")
    private List<String> categories;

    @Schema(description = "图例数据")
    private List<String> legend;

    @Schema(description = "图表系列数据")
    private List<ChartSeries> series = new ArrayList<>();

    @Schema(description = "是否显示工具提示", example = "true")
    private Boolean tooltip = true;

    @Schema(description = "是否显示图例", example = "true")
    private Boolean showLegend = true;

    @Schema(description = "图表主题", example = "default")
    private String theme = "default";
}