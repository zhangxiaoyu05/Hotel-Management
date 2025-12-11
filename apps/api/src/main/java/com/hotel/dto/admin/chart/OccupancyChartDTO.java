package com.hotel.dto.admin.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 入住率图表数据
 */
@Data
@Schema(description = "入住率图表数据")
public class OccupancyChartDTO {

    @Schema(description = "图表标题", example = "入住率分析")
    private String title;

    @Schema(description = "图表副标题", example = "2025-12-01 至 2025-12-31")
    private String subtitle;

    @Schema(description = "X轴标签")
    private List<String> categories;

    @Schema(description = "图例数据")
    private List<String> legend;

    @Schema(description = "图表系列数据")
    private List<ChartSeries> series = new ArrayList<>();

    @Schema(description = "热力图数据", example = "[[1, 1, 75.5], [1, 8, 82.3]]")
    private List<List<Object>> heatmapData;

    @Schema(description = "星期标签", example = "[\"周一\", \"周二\", ...]")
    private List<String> weekDays;

    @Schema(description = "是否显示工具提示", example = "true")
    private Boolean tooltip = true;

    @Schema(description = "是否显示图例", example = "true")
    private Boolean showLegend = true;

    @Schema(description = "Y轴标题")
    private String yAxisTitle = "入住率 (%)";

    @Schema(description = "Y轴最大值", example = "100")
    private Double yAxisMax = 100.0;

    @Schema(description = "数据格式化", example = "{value}%")
    private String dataFormat = "{value}%";
}