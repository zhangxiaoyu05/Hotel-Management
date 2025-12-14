package com.hotel.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hotel.controller.BaseController;
import com.hotel.dto.log.LogExportRequest;
import com.hotel.dto.log.LogSearchRequest;
import com.hotel.entity.log.ErrorLog;
import com.hotel.entity.log.LoginLog;
import com.hotel.entity.log.OperationLog;
import com.hotel.service.LogManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志管理控制器
 * 处理管理员查看和导出系统日志的操作
 */
@RestController
@RequestMapping("/v1/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@Slf4j
@Tag(name = "日志管理接口", description = "管理员专用接口，用于系统日志管理")
@SecurityRequirement(name = "bearerAuth")
public class LogManagementController extends BaseController {

    @Autowired
    private LogManagementService logManagementService;

    /**
     * 获取操作日志
     */
    @GetMapping("/operation")
    @Operation(summary = "获取操作日志", description = "分页查询操作日志")
    public ResponseEntity<ApiResponse<IPage<OperationLog>>> getOperationLogs(
            @Valid LogSearchRequest request) {

        try {
            log.info("管理员 {} 查询操作日志，页码: {}, 每页大小: {}, 用户名: {}, 操作: {}",
                    getCurrentUserId(), request.getPage(), request.getSize(),
                    request.getUsername(), request.getOperation());

            IPage<OperationLog> result = logManagementService.getOperationLogs(request);

            log.info("查询操作日志成功，共 {} 条记录", result.getTotal());
            return ResponseEntity.ok(success(result, "获取操作日志成功"));

        } catch (Exception e) {
            log.error("获取操作日志失败", e);
            return ResponseEntity.ok(failed("获取操作日志失败: " + e.getMessage()));
        }
    }

    /**
     * 获取登录日志
     */
    @GetMapping("/login")
    @Operation(summary = "获取登录日志", description = "分页查询登录日志")
    public ResponseEntity<ApiResponse<IPage<LoginLog>>> getLoginLogs(
            @Valid LogSearchRequest request) {

        try {
            log.info("管理员 {} 查询登录日志，页码: {}, 每页大小: {}, 用户名: {}, 登录类型: {}",
                    getCurrentUserId(), request.getPage(), request.getSize(),
                    request.getUsername(), request.getLoginType());

            IPage<LoginLog> result = logManagementService.getLoginLogs(request);

            log.info("查询登录日志成功，共 {} 条记录", result.getTotal());
            return ResponseEntity.ok(success(result, "获取登录日志成功"));

        } catch (Exception e) {
            log.error("获取登录日志失败", e);
            return ResponseEntity.ok(failed("获取登录日志失败: " + e.getMessage()));
        }
    }

    /**
     * 获取错误日志
     */
    @GetMapping("/error")
    @Operation(summary = "获取错误日志", description = "分页查询错误日志")
    public ResponseEntity<ApiResponse<IPage<ErrorLog>>> getErrorLogs(
            @Valid LogSearchRequest request) {

        try {
            log.info("管理员 {} 查询错误日志，页码: {}, 每页大小: {}, 用户名: {}, 级别: {}, 模块: {}",
                    getCurrentUserId(), request.getPage(), request.getSize(),
                    request.getUsername(), request.getLevel(), request.getModule());

            IPage<ErrorLog> result = logManagementService.getErrorLogs(request);

            log.info("查询错误日志成功，共 {} 条记录", result.getTotal());
            return ResponseEntity.ok(success(result, "获取错误日志成功"));

        } catch (Exception e) {
            log.error("获取错误日志失败", e);
            return ResponseEntity.ok(failed("获取错误日志失败: " + e.getMessage()));
        }
    }

    /**
     * 导出日志
     */
    @PostMapping("/export")
    @Operation(summary = "导出日志", description = "根据条件导出日志数据")
    public ResponseEntity<Resource> exportLogs(
            @Valid @RequestBody LogExportRequest request) {

        try {
            log.info("管理员 {} 导出日志，类型: {}, 格式: {}, 最大记录数: {}",
                    getCurrentUserId(), request.getLogType(), request.getExportFormat(), request.getMaxRecords());

            byte[] data = logManagementService.exportLogs(request);

            String filename = generateFilename(request);
            ByteArrayResource resource = new ByteArrayResource(data);

            MediaType mediaType = "csv".equalsIgnoreCase(request.getExportFormat()) ?
                    MediaType.parseMediaType("text/csv") :
                    MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("导出日志失败", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("导出日志失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取日志统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取日志统计", description = "获取各类日志的统计信息")
    public ResponseEntity<ApiResponse<LogStatisticsResponse>> getLogStatistics() {

        try {
            log.info("管理员 {} 获取日志统计信息", getCurrentUserId());

            // 这里可以添加统计逻辑，例如最近7天、30天的日志统计
            LogStatisticsResponse statistics = new LogStatisticsResponse();
            // TODO: 实现统计逻辑

            return ResponseEntity.ok(success(statistics, "获取日志统计成功"));

        } catch (Exception e) {
            log.error("获取日志统计失败", e);
            return ResponseEntity.ok(failed("获取日志统计失败: " + e.getMessage()));
        }
    }

    /**
     * 生成导出文件名
     */
    private String generateFilename(LogExportRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String logTypeName = getLogTypeName(request.getLogType());
        String extension = "csv".equalsIgnoreCase(request.getExportFormat()) ? ".csv" : ".xlsx";
        return String.format("%s日志_%s%s", logTypeName, timestamp, extension);
    }

    /**
     * 获取日志类型中文名称
     */
    private String getLogTypeName(String logType) {
        switch (logType.toLowerCase()) {
            case "operation":
                return "操作";
            case "login":
                return "登录";
            case "error":
                return "错误";
            default:
                return "日志";
        }
    }

    /**
     * 日志统计响应DTO
     */
    public static class LogStatisticsResponse {
        // TODO: 添加统计字段
        // private Long operationLogCount;
        // private Long loginLogCount;
        // private Long errorLogCount;
        // private List<LogTrend> trends;
    }
}