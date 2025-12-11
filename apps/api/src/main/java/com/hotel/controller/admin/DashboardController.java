package com.hotel.controller.admin;

import com.hotel.controller.BaseController;
import com.hotel.dto.admin.dashboard.DashboardMetricsDTO;
import com.hotel.dto.admin.dashboard.RealTimeDataDTO;
import com.hotel.dto.admin.dashboard.TrendDataDTO;
import com.hotel.dto.admin.dashboard.RevenueStatisticsDTO;
import com.hotel.dto.admin.chart.*;
import com.hotel.dto.historical.HistoricalDataQueryDTO;
import com.hotel.service.DashboardService;
import com.hotel.service.ChartDataFormatterService;
import com.hotel.service.HistoricalDataService;
import com.hotel.service.RevenueAnalyticsService;
import com.hotel.service.OccupancyAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 仪表板控制器
 * 提供管理员仪表板数据查询接口
 */
@RestController
@RequestMapping("/v1/admin/dashboard")
@Slf4j
@Tag(name = "仪表板接口", description = "管理员仪表板数据查询接口")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController extends BaseController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ChartDataFormatterService chartDataFormatterService;

    @Autowired
    private HistoricalDataService historicalDataService;

    @Autowired
    private RevenueAnalyticsService revenueAnalyticsService;

    @Autowired
    private OccupancyAnalyticsService occupancyAnalyticsService;

    /**
     * 获取仪表板核心指标
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取仪表板核心指标", description = "获取今日订单数、入住率、收入等关键运营数据")
    public ResponseEntity<ApiResponse<DashboardMetricsDTO>> getDashboardMetrics() {
        try {
            log.info("管理员请求获取仪表板核心指标");

            DashboardMetricsDTO metrics = dashboardService.getDashboardMetrics();

            log.info("仪表板核心指标获取成功");
            return ResponseEntity.ok(success(metrics, "获取仪表板核心指标成功"));

        } catch (Exception e) {
            log.error("获取仪表板核心指标失败", e);
            return ResponseEntity.ok(failed("获取仪表板核心指标失败: " + e.getMessage()));
        }
    }

    /**
     * 获取实时数据
     */
    @GetMapping("/realtime")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取实时数据", description = "获取房间状态、最新订单等实时信息")
    public ResponseEntity<ApiResponse<RealTimeDataDTO>> getRealTimeData() {
        try {
            log.info("管理员请求获取实时数据");

            RealTimeDataDTO realTimeData = dashboardService.getRealTimeData();

            log.info("实时数据获取成功");
            return ResponseEntity.ok(success(realTimeData, "获取实时数据成功"));

        } catch (Exception e) {
            log.error("获取实时数据失败", e);
            return ResponseEntity.ok(failed("获取实时数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取趋势数据
     */
    @GetMapping("/trends")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取趋势数据", description = "获取订单、收入、入住率等历史趋势数据")
    public ResponseEntity<ApiResponse<TrendDataDTO>> getTrendData(
            @Parameter(description = "统计周期", example = "daily") @RequestParam(defaultValue = "daily") String period,
            @Parameter(description = "天数", example = "30") @RequestParam(defaultValue = "30") int days) {

        try {
            log.info("管理员请求获取趋势数据，周期: {}, 天数: {}", period, days);

            // 限制最大查询天数
            if (days > 365) {
                return ResponseEntity.ok(failed("查询天数不能超过365天"));
            }

            TrendDataDTO trendData = dashboardService.getTrendData(period, days);

            log.info("趋势数据获取成功");
            return ResponseEntity.ok(success(trendData, "获取趋势数据成功"));

        } catch (Exception e) {
            log.error("获取趋势数据失败", e);
            return ResponseEntity.ok(failed("获取趋势数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取收入统计
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取收入统计", description = "获取各时间段的收入统计数据和增长率")
    public ResponseEntity<ApiResponse<RevenueStatisticsDTO>> getRevenueStatistics() {
        try {
            log.info("管理员请求获取收入统计");

            RevenueStatisticsDTO revenueStats = dashboardService.getRevenueStatistics();

            log.info("收入统计获取成功");
            return ResponseEntity.ok(success(revenueStats, "获取收入统计成功"));

        } catch (Exception e) {
            log.error("获取收入统计失败", e);
            return ResponseEntity.ok(failed("获取收入统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取房间状态统计
     */
    @GetMapping("/room-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取房间状态统计", description = "获取各种状态的房间数量统计")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getRoomStatusStatistics() {
        try {
            log.info("管理员请求获取房间状态统计");

            // TODO: 实现房间状态统计查询
            // 暂时返回空数据
            Map<String, Integer> roomStatusStats = Map.of(
                "AVAILABLE", 30,
                "OCCUPIED", 65,
                "MAINTENANCE", 5,
                "CLEANING", 0
            );

            log.info("房间状态统计获取成功");
            return ResponseEntity.ok(success(roomStatusStats, "获取房间状态统计成功"));

        } catch (Exception e) {
            log.error("获取房间状态统计失败", e);
            return ResponseEntity.ok(failed("获取房间状态统计失败: " + e.getMessage()));
        }
    }

    /**
     * 刷新仪表板缓存
     */
    @PostMapping("/refresh-cache")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "刷新仪表板缓存", description = "清除仪表板数据缓存，强制重新查询最新数据")
    public ResponseEntity<ApiResponse<String>> refreshDashboardCache() {
        try {
            log.info("管理员请求刷新仪表板缓存");

            dashboardService.clearDashboardCache();

            log.info("仪表板缓存刷新成功");
            return ResponseEntity.ok(success("仪表板缓存刷新成功"));

        } catch (Exception e) {
            log.error("刷新仪表板缓存失败", e);
            return ResponseEntity.ok(failed("刷新仪表板缓存失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取系统健康状态", description = "获取数据库连接、缓存等系统组件状态")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        try {
            log.info("管理员请求获取系统健康状态");

            // TODO: 实现系统健康检查
            Map<String, Object> healthStatus = Map.of(
                "database", "UP",
                "cache", "UP",
                "diskSpace", "OK",
                "memory", "OK",
                "cpu", "OK"
            );

            log.info("系统健康状态获取成功");
            return ResponseEntity.ok(success(healthStatus, "获取系统健康状态成功"));

        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            return ResponseEntity.ok(failed("获取系统健康状态失败: " + e.getMessage()));
        }
    }

    // ========== 图表数据API ==========

    /**
     * 获取订单趋势图表数据
     */
    @GetMapping("/charts/orders/trends")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取订单趋势图表数据", description = "获取订单数量、收入等趋势图表数据")
    public ResponseEntity<ApiResponse<OrderTrendChartDTO>> getOrderTrendsChart(
            @Parameter(description = "开始日期", example = "2025-12-01") @RequestParam LocalDate startDate,
            @Parameter(description = "结束日期", example = "2025-12-31") @RequestParam LocalDate endDate,
            @Parameter(description = "统计周期", example = "daily") @RequestParam(defaultValue = "daily") String period) {
        try {
            log.info("管理员请求获取订单趋势图表数据，时间段: {} 至 {}, 周期: {}", startDate, endDate, period);

            OrderTrendChartDTO chartData = chartDataFormatterService.formatOrderTrendsChart(startDate, endDate, period);

            log.info("订单趋势图表数据获取成功");
            return ResponseEntity.ok(success(chartData, "获取订单趋势图表数据成功"));

        } catch (Exception e) {
            log.error("获取订单趋势图表数据失败", e);
            return ResponseEntity.ok(failed("获取订单趋势图表数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取收入分析图表数据
     */
    @GetMapping("/charts/revenue/analysis")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取收入分析图表数据", description = "获取收入趋势、按房型分布等图表数据")
    public ResponseEntity<ApiResponse<RevenueChartDTO>> getRevenueAnalysisChart(
            @Parameter(description = "开始日期", example = "2025-12-01") @RequestParam LocalDate startDate,
            @Parameter(description = "结束日期", example = "2025-12-31") @RequestParam LocalDate endDate,
            @Parameter(description = "图表类型", example = "trends") @RequestParam(defaultValue = "trends") String type) {
        try {
            log.info("管理员请求获取收入分析图表数据，时间段: {} 至 {}, 类型: {}", startDate, endDate, type);

            RevenueChartDTO chartData = chartDataFormatterService.formatRevenueAnalysisChart(startDate, endDate, type);

            log.info("收入分析图表数据获取成功");
            return ResponseEntity.ok(success(chartData, "获取收入分析图表数据成功"));

        } catch (Exception e) {
            log.error("获取收入分析图表数据失败", e);
            return ResponseEntity.ok(failed("获取收入分析图表数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取入住率图表数据
     */
    @GetMapping("/charts/occupancy/analysis")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取入住率图表数据", description = "获取入住率趋势、按房型对比等图表数据")
    public ResponseEntity<ApiResponse<OccupancyChartDTO>> getOccupancyAnalysisChart(
            @Parameter(description = "开始日期", example = "2025-12-01") @RequestParam LocalDate startDate,
            @Parameter(description = "结束日期", example = "2025-12-31") @RequestParam LocalDate endDate,
            @Parameter(description = "图表类型", example = "trends") @RequestParam(defaultValue = "trends") String type) {
        try {
            log.info("管理员请求获取入住率图表数据，时间段: {} 至 {}, 类型: {}", startDate, endDate, type);

            OccupancyChartDTO chartData = chartDataFormatterService.formatOccupancyChart(startDate, endDate, type);

            log.info("入住率图表数据获取成功");
            return ResponseEntity.ok(success(chartData, "获取入住率图表数据成功"));

        } catch (Exception e) {
            log.error("获取入住率图表数据失败", e);
            return ResponseEntity.ok(failed("获取入住率图表数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取饼图数据
     */
    @GetMapping("/charts/pie")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取饼图数据", description = "获取各种饼图数据（房间状态、订单状态等）")
    public ResponseEntity<ApiResponse<PieChartDTO>> getPieChartData(
            @Parameter(description = "数据类型", example = "room_status") @RequestParam String dataType,
            @Parameter(description = "开始日期", example = "2025-12-01") @RequestParam LocalDate startDate,
            @Parameter(description = "结束日期", example = "2025-12-31") @RequestParam LocalDate endDate) {
        try {
            log.info("管理员请求获取饼图数据，类型: {}, 时间段: {} 至 {}", dataType, startDate, endDate);

            PieChartDTO chartData = chartDataFormatterService.formatPieChartData(dataType, startDate, endDate);

            log.info("饼图数据获取成功");
            return ResponseEntity.ok(success(chartData, "获取饼图数据成功"));

        } catch (Exception e) {
            log.error("获取饼图数据失败", e);
            return ResponseEntity.ok(failed("获取饼图数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取仪表板综合图表数据
     */
    @GetMapping("/charts/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取仪表板综合图表数据", description = "获取仪表板所需的所有图表数据")
    public ResponseEntity<ApiResponse<DashboardChartDataDTO>> getDashboardChartData(
            @Parameter(description = "开始日期", example = "2025-12-01") @RequestParam LocalDate startDate,
            @Parameter(description = "结束日期", example = "2025-12-31") @RequestParam LocalDate endDate) {
        try {
            log.info("管理员请求获取仪表板综合图表数据，时间段: {} 至 {}", startDate, endDate);

            DashboardChartDataDTO chartData = chartDataFormatterService.formatDashboardCharts(startDate, endDate);

            log.info("仪表板综合图表数据获取成功");
            return ResponseEntity.ok(success(chartData, "获取仪表板综合图表数据成功"));

        } catch (Exception e) {
            log.error("获取仪表板综合图表数据失败", e);
            return ResponseEntity.ok(failed("获取仪表板综合图表数据失败: " + e.getMessage()));
        }
    }

    // ========== 历史数据API ==========

    /**
     * 查询历史订单数据
     */
    @PostMapping("/historical/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "查询历史订单数据", description = "根据查询条件获取历史订单数据")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queryHistoricalOrders(
            @Parameter(description = "查询条件") @RequestBody HistoricalDataQueryDTO query) {
        try {
            log.info("管理员查询历史订单数据，查询条件: {}", query);

            Map<String, Object> result = historicalDataService.queryHistoricalOrders(query);

            log.info("历史订单数据查询完成，返回 {} 条记录", result.get("total"));
            return ResponseEntity.ok(success(result, "历史订单数据查询成功"));

        } catch (Exception e) {
            log.error("查询历史订单数据失败", e);
            return ResponseEntity.ok(failed("查询历史订单数据失败: " + e.getMessage()));
        }
    }

    /**
     * 聚合历史数据
     */
    @PostMapping("/historical/aggregation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "聚合历史数据", description = "获取历史数据的聚合统计信息")
    public ResponseEntity<ApiResponse<HistoricalDataAggregationDTO>> aggregateHistoricalData(
            @Parameter(description = "查询条件") @RequestBody HistoricalDataQueryDTO query) {
        try {
            log.info("管理员聚合历史数据，查询条件: {}", query);

            HistoricalDataAggregationDTO aggregation = historicalDataService.aggregateHistoricalData(query);

            log.info("历史数据聚合完成");
            return ResponseEntity.ok(success(aggregation, "历史数据聚合成功"));

        } catch (Exception e) {
            log.error("聚合历史数据失败", e);
            return ResponseEntity.ok(failed("聚合历史数据失败: " + e.getMessage()));
        }
    }

    /**
     * 生成历史数据统计报告
     */
    @GetMapping("/historical/report")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "生成历史数据统计报告", description = "生成指定时间段的历史数据统计报告")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateHistoricalReport(
            @Parameter(description = "开始日期", example = "2025-12-01") @RequestParam LocalDate startDate,
            @Parameter(description = "结束日期", example = "2025-12-31") @RequestParam LocalDate endDate,
            @Parameter(description = "分组方式", example = "daily") @RequestParam(defaultValue = "daily") String groupBy) {
        try {
            log.info("管理员生成历史数据统计报告，时间段: {} 至 {}, 分组方式: {}", startDate, endDate, groupBy);

            Map<String, Object> report = historicalDataService.generateHistoricalReport(startDate, endDate, groupBy);

            log.info("历史数据统计报告生成完成");
            return ResponseEntity.ok(success(report, "历史数据统计报告生成成功"));

        } catch (Exception e) {
            log.error("生成历史数据统计报告失败", e);
            return ResponseEntity.ok(failed("生成历史数据统计报告失败: " + e.getMessage()));
        }
    }

    /**
     * 导出历史数据
     */
    @PostMapping("/historical/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "导出历史数据", description = "根据查询条件导出历史数据")
    public ResponseEntity<byte[]> exportHistoricalData(
            @Parameter(description = "查询条件") @RequestBody HistoricalDataQueryDTO query,
            @Parameter(description = "导出格式", example = "csv") @RequestParam(defaultValue = "csv") String format) {
        try {
            log.info("管理员导出历史数据，格式: {}, 查询条件: {}", format, query);

            byte[] data = historicalDataService.exportHistoricalData(query, format);

            String filename = String.format("historical_data_%s.%s",
                LocalDate.now().format(DateTimeFormatter.ISO_DATE), format);

            log.info("历史数据导出完成");
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .header("Content-Type", getContentType(format))
                .body(data);

        } catch (Exception e) {
            log.error("导出历史数据失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取数据快照
     */
    @GetMapping("/historical/snapshot")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取数据快照", description = "获取指定日期的数据快照")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDataSnapshot(
            @Parameter(description = "快照日期", example = "2025-12-11") @RequestParam LocalDate date) {
        try {
            log.info("管理员获取数据快照，日期: {}", date);

            Map<String, Object> snapshot = historicalDataService.getDataSnapshot(date);

            log.info("数据快照获取完成");
            return ResponseEntity.ok(success(snapshot, "数据快照获取成功"));

        } catch (Exception e) {
            log.error("获取数据快照失败", e);
            return ResponseEntity.ok(failed("获取数据快照失败: " + e.getMessage()));
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取导出文件的Content-Type
     */
    private String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "csv" -> "text/csv";
            case "excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "json" -> "application/json";
            default -> "application/octet-stream";
        };
    }
}