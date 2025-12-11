package com.hotel.dto.historical;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 历史数据聚合结果DTO
 */
@Data
@Schema(description = "历史数据聚合结果")
public class HistoricalDataAggregationDTO {

    @Schema(description = "总订单数", example = "1000")
    private Integer totalOrders;

    @Schema(description = "总收入", example = "158000.50")
    private BigDecimal totalRevenue;

    @Schema(description = "总房间数", example = "100")
    private Integer totalRooms;

    @Schema(description = "总用户数", example = "500")
    private Integer totalUsers;

    @Schema(description = "订单状态分布", example = "{\"PENDING\": 100, \"COMPLETED\": 800}")
    private Map<String, Long> orderStatusDistribution;

    @Schema(description = "房间状态分布", example = "{\"AVAILABLE\": 30, \"OCCUPIED\": 65}")
    private Map<String, Long> roomStatusDistribution;

    @Schema(description = "用户角色分布", example = "{\"USER\": 480, \"ADMIN\": 20}")
    private Map<String, Long> userRoleDistribution;

    @Schema(description = "用户状态分布", example = "{\"ACTIVE\": 490, \"INACTIVE\": 10}")
    private Map<String, Long> userStatusDistribution;

    @Schema(description = "订单金额分布", example = "{\"0-500\": 200, \"500-1000\": 500}")
    private Map<String, Long> orderAmountDistribution;

    @Schema(description = "平均订单金额", example = "158.00")
    private BigDecimal averageOrderAmount;

    @Schema(description = "平均入住率", example = "75.5")
    private Double averageOccupancyRate;

    @Schema(description = "统计时间范围")
    private String timeRange;

    @Schema(description = "数据更新时间")
    private String lastUpdateTime;
}