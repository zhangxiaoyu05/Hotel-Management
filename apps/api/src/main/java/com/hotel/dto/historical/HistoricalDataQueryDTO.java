package com.hotel.dto.historical;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 历史数据查询DTO
 */
@Data
@Schema(description = "历史数据查询条件")
public class HistoricalDataQueryDTO {

    @Schema(description = "开始日期", example = "2025-11-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2025-12-31")
    private LocalDate endDate;

    @Schema(description = "订单状态列表")
    private List<String> statuses;

    @Schema(description = "最小金额")
    private BigDecimal minAmount;

    @Schema(description = "最大金额")
    private BigDecimal maxAmount;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "房间ID")
    private Long roomId;

    @Schema(description = "房型ID")
    private Long roomTypeId;

    @Schema(description = "酒店ID")
    private Long hotelId;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "排序字段", example = "createdAt")
    private String sortBy = "createdAt";

    @Schema(description = "排序方向", example = "desc")
    private String sortOrder = "desc";

    @Schema(description = "是否包含已删除数据", example = "false")
    private Boolean includeDeleted = false;
}