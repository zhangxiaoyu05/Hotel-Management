package com.hotel.controller.admin;

import com.hotel.controller.BaseController;
import com.hotel.dto.report.*;
import com.hotel.service.ReportService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * 报表控制器
 * 提供各种业务报表的生成和查看功能
 *
 * @author Hotel System
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/admin/reports")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "报表管理", description = "报表生成和查看相关接口")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController extends BaseController {

    private final ReportService reportService;

    @Qualifier("reportQueryRateLimiter")
    private final RateLimiter reportQueryRateLimiter;

    @Qualifier("reportExportRateLimiter")
    private final RateLimiter reportExportRateLimiter;

    /**
     * 应用速率限制的装饰器方法
     */
    private <T> Supplier<T> withRateLimit(Supplier<T> supplier, RateLimiter rateLimiter) {
        return RateLimiter.decorateSupplier(rateLimiter, supplier);
    }

    /**
     * 获取订单报表
     */
    @GetMapping("/orders")
    @Operation(summary = "获取订单报表", description = "按时间范围和其他筛选条件获取订单统计报表")
    public ResponseEntity<ApiResponse<OrderReportDTO>> getOrderReport(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,

            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,

            @Parameter(description = "房型ID（可选）")
            @RequestParam(required = false) Long roomTypeId,

            @Parameter(description = "订单状态（可选）")
            @RequestParam(required = false) String orderStatus) {

        log.info("获取订单报表，时间范围：{} - {}, 房型ID：{}, 订单状态：{}",
                startDate, endDate, roomTypeId, orderStatus);

        try {
            Supplier<OrderReportDTO> reportSupplier = () ->
                reportService.generateOrderReport(startDate, endDate, roomTypeId, orderStatus);

            OrderReportDTO report = withRateLimit(reportSupplier, reportQueryRateLimiter).get();
            return ResponseEntity.ok(success(report, "订单报表生成成功"));
        } catch (Exception e) {
            log.error("生成订单报表失败", e);

            // 检查是否是速率限制异常
            if (e.getMessage() != null && e.getMessage().contains("RateLimiter")) {
                return ResponseEntity.status(429).body(failed("请求过于频繁，请稍后再试"));
            }

            return ResponseEntity.badRequest().body(failed("生成订单报表失败：" + e.getMessage()));
        }
    }

    /**
     * 获取收入报表
     */
    @GetMapping("/revenue")
    @Operation(summary = "获取收入报表", description = "获取收入明细和趋势分析")
    public ResponseEntity<ApiResponse<RevenueReportDTO>> getRevenueReport(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,

            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,

            @Parameter(description = "房型ID（可选）")
            @RequestParam(required = false) Long roomTypeId) {

        log.info("获取收入报表，时间范围：{} - {}, 房型ID：{}", startDate, endDate, roomTypeId);

        try {
            RevenueReportDTO report = reportService.generateRevenueReport(startDate, endDate, roomTypeId);
            return ResponseEntity.ok(success(report, "收入报表生成成功"));
        } catch (Exception e) {
            log.error("生成收入报表失败", e);
            return ResponseEntity.badRequest().body(failed("生成收入报表失败：" + e.getMessage()));
        }
    }

    /**
     * 获取用户报表
     */
    @GetMapping("/users")
    @Operation(summary = "获取用户报表", description = "获取用户增长、活跃度、留存率分析")
    public ResponseEntity<ApiResponse<UserReportDTO>> getUserReport(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,

            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        log.info("获取用户报表，时间范围：{} - {}", startDate, endDate);

        try {
            UserReportDTO report = reportService.generateUserReport(startDate, endDate);
            return ResponseEntity.ok(success(report, "用户报表生成成功"));
        } catch (Exception e) {
            log.error("生成用户报表失败", e);
            return ResponseEntity.badRequest().body(failed("生成用户报表失败：" + e.getMessage()));
        }
    }

    /**
     * 获取房间报表
     */
    @GetMapping("/rooms")
    @Operation(summary = "获取房间报表", description = "获取房间使用率、收入贡献分析")
    public ResponseEntity<ApiResponse<RoomReportDTO>> getRoomReport(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,

            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,

            @Parameter(description = "房型ID（可选）")
            @RequestParam(required = false) Long roomTypeId) {

        log.info("获取房间报表，时间范围：{} - {}, 房型ID：{}", startDate, endDate, roomTypeId);

        try {
            RoomReportDTO report = reportService.generateRoomReport(startDate, endDate, roomTypeId);
            return ResponseEntity.ok(success(report, "房间报表生成成功"));
        } catch (Exception e) {
            log.error("生成房间报表失败", e);
            return ResponseEntity.badRequest().body(failed("生成房间报表失败：" + e.getMessage()));
        }
    }

    /**
     * 导出报表
     */
    @PostMapping("/export")
    @Operation(summary = "导出报表", description = "导出指定格式的报表文件")
    public ResponseEntity<ApiResponse<String>> exportReport(
            @Parameter(description = "导出请求参数", required = true)
            @Valid @RequestBody ReportExportRequest request) {

        log.info("导出报表，类型：{}, 格式：{}, 时间范围：{} - {}",
                request.getReportType(), request.getExportFormat(),
                request.getStartDate(), request.getEndDate());

        try {
            Supplier<String> exportSupplier = () -> reportService.exportReport(request);

            String fileUrl = withRateLimit(exportSupplier, reportExportRateLimiter).get();
            return ResponseEntity.ok(success(fileUrl, "报表导出成功"));
        } catch (Exception e) {
            log.error("导出报表失败", e);

            // 检查是否是速率限制异常
            if (e.getMessage() != null && e.getMessage().contains("RateLimiter")) {
                return ResponseEntity.status(429).body(failed("导出请求过于频繁，请稍后再试"));
            }

            return ResponseEntity.badRequest().body(failed("导出报表失败：" + e.getMessage()));
        }
    }

    /**
     * 获取报表概览数据
     */
    @GetMapping("/overview")
    @Operation(summary = "获取报表概览", description = "获取所有报表的概览数据")
    public ResponseEntity<ApiResponse<ReportOverviewDTO>> getReportOverview() {

        log.info("获取报表概览数据");

        try {
            ReportOverviewDTO overview = reportService.getReportOverview();
            return ResponseEntity.ok(success(overview, "报表概览获取成功"));
        } catch (Exception e) {
            log.error("获取报表概览失败", e);
            return ResponseEntity.badRequest().body(failed("获取报表概览失败：" + e.getMessage()));
        }
    }

    /**
     * 刷新报表缓存
     */
    @PostMapping("/refresh-cache")
    @Operation(summary = "刷新报表缓存", description = "手动刷新报表数据缓存")
    public ResponseEntity<ApiResponse<String>> refreshReportCache() {

        log.info("刷新报表缓存");

        try {
            reportService.refreshReportCache();
            return ResponseEntity.ok(success("报表缓存刷新成功"));
        } catch (Exception e) {
            log.error("刷新报表缓存失败", e);
            return ResponseEntity.badRequest().body(failed("刷新报表缓存失败：" + e.getMessage()));
        }
    }
}