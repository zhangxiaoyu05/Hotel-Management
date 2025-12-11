package com.hotel.dto.admin.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 仪表板核心指标DTO
 * 用于展示系统关键运营数据
 */
@Data
@Schema(description = "仪表板核心指标")
public class DashboardMetricsDTO {

    @Schema(description = "今日订单数", example = "25")
    private Integer todayOrdersCount;

    @Schema(description = "今日收入", example = "5880.50")
    private BigDecimal todayRevenue;

    @Schema(description = "入住率（百分比）", example = "75.5")
    private Double occupancyRate;

    @Schema(description = "总活跃用户数", example = "128")
    private Integer totalActiveUsers;

    @Schema(description = "待审核评价数", example = "8")
    private Integer pendingReviewsCount;

    @Schema(description = "平均评分", example = "4.3")
    private Double averageRating;

    @Schema(description = "总房间数", example = "100")
    private Integer totalRooms;

    @Schema(description = "已预订房间数", example = "65")
    private Integer reservedRooms;

    @Schema(description = "可用房间数", example = "30")
    private Integer availableRooms;

    @Schema(description = "维护中房间数", example = "5")
    private Integer maintenanceRooms;

    @Schema(description = "本月收入", example = "156800.00")
    private BigDecimal monthlyRevenue;

    @Schema(description = "本月订单数", example = "420")
    private Integer monthlyOrdersCount;

    @Schema(description = "新增用户数（今日）", example = "12")
    private Integer todayNewUsers;
}