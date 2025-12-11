package com.hotel.dto.admin.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 饼图数据项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "饼图数据项")
public class PieDataItem {

    @Schema(description = "数据项名称", example = "已入住")
    private String name;

    @Schema(description = "数据项数值", example = "65")
    private Double value;

    @Schema(description = "数据项颜色", example = "#1890ff")
    private String color;
}