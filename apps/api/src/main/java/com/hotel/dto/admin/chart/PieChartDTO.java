package com.hotel.dto.admin.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 饼图数据
 */
@Data
@Schema(description = "饼图数据")
public class PieChartDTO {

    @Schema(description = "图表标题", example = "状态分布")
    private String title;

    @Schema(description = "图表副标题")
    private String subtitle;

    @Schema(description = "饼图数据项")
    private List<PieDataItem> data = new ArrayList<>();

    @Schema(description = "是否显示标签", example = "true")
    private Boolean showLabels = true;

    @Schema(description = "是否显示图例", example = "true")
    private Boolean showLegend = true;

    @Schema(description = "是否显示工具提示", example = "true")
    private Boolean tooltip = true;

    @Schema(description = "饼图半径", example = "60%")
    private String radius = "60%";

    @Schema(description = "饼图内半径", example = "0%")
    private String innerRadius = "0%";

    @Schema(description = "是否为环形图", example = "false")
    private Boolean donut = false;

    @Schema(description = "标签格式", example = "{name}: {value} ({d}%)")
    private String labelFormat = "{name}: {value} ({d}%)";
}