package com.hotel.dto.admin.dashboard;

import com.hotel.dto.OrderSummaryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 实时数据DTO
 * 用于展示系统实时状态信息
 */
@Data
@Schema(description = "仪表板实时数据")
public class RealTimeDataDTO {

    @Schema(description = "最新订单列表")
    private List<OrderSummaryDTO> recentOrders;

    @Schema(description = "房间状态统计")
    private Map<String, Integer> roomStatusCounts;

    @Schema(description = "当前在线用户数", example = "45")
    private Integer onlineUsersCount;

    @Schema(description = "活跃用户数（近7天）", example = "128")
    private Integer activeUsersCount;

    @Schema(description = "今日新增用户数", example = "12")
    private Integer todayNewUsersCount;

    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdateTime;

    @Schema(description = "待处理订单数", example = "18")
    private Integer pendingOrdersCount;

    @Schema(description = "待入住订单数", example = "8")
    private Integer pendingCheckInCount;

    @Schema(description = "待退房订单数", example = "5")
    private Integer pendingCheckOutCount;

    @Schema(description = "进行中订单数", example = "65")
    private Integer activeOrdersCount;

    @Schema(description = "系统状态", example = "NORMAL")
    private String systemStatus;

    @Schema(description = "数据库连接状态", example = "ACTIVE")
    private String databaseStatus;

    @Schema(description = "缓存状态", example = "ACTIVE")
    private String cacheStatus;
}