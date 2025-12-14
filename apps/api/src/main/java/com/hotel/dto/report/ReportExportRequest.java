package com.hotel.dto.report;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * 报表导出请求DTO
 */
@Data
public class ReportExportRequest {

    /**
     * 报表类型
     */
    @NotNull(message = "报表类型不能为空")
    private ReportType reportType;

    /**
     * 导出格式
     */
    @NotNull(message = "导出格式不能为空")
    @Pattern(regexp = "^(EXCEL|PDF)$", message = "导出格式只支持EXCEL或PDF")
    private String exportFormat;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 房型ID（可选）
     */
    private Long roomTypeId;

    /**
     * 订单状态（可选，仅用于订单报表）
     */
    private String orderStatus;

    /**
     * 是否包含图表
     */
    private Boolean includeCharts = false;

    /**
     * 是否包含详细数据
     */
    private Boolean includeDetailedData = true;

    /**
     * 报表类型枚举
     */
    public enum ReportType {
        ORDER("订单报表"),
        REVENUE("收入报表"),
        USER("用户报表"),
        ROOM("房间报表");

        private final String description;

        ReportType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}