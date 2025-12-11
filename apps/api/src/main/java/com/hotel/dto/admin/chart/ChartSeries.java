package com.hotel.dto.admin.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 图表系列数据
 */
@Data
@Schema(description = "图表系列数据")
public class ChartSeries {

    @Schema(description = "系列名称", example = "订单数量")
    private String name;

    @Schema(description = "系列数据", example = "[100, 200, 150]")
    private List<Double> data;

    @Schema(description = "系列颜色", example = "#1890ff")
    private String color;

    @Schema(description = "图表类型", example = "line")
    private String type;

    @Schema(description = "是否堆叠", example = "false")
    private Boolean stacked = false;

    @Schema(description = "Y轴索引", example = "0")
    private Integer yAxis = 0;

    @Schema(description = "是否平滑曲线", example = "true")
    private Boolean smooth = true;
}