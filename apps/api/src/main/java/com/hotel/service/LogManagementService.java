package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.dto.log.LogExportRequest;
import com.hotel.dto.log.LogSearchRequest;
import com.hotel.entity.log.ErrorLog;
import com.hotel.entity.log.LoginLog;
import com.hotel.entity.log.OperationLog;
import com.hotel.repository.log.ErrorLogRepository;
import com.hotel.repository.log.LoginLogRepository;
import com.hotel.repository.log.OperationLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日志管理服务类
 */
@Service
@Slf4j
public class LogManagementService {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private LoginLogRepository loginLogRepository;

    @Autowired
    private ErrorLogRepository errorLogRepository;

    /**
     * 获取操作日志分页数据
     */
    public IPage<OperationLog> getOperationLogs(LogSearchRequest request) {
        log.debug("查询操作日志，参数: {}", request);

        Page<OperationLog> page = new Page<>(request.getPage(), request.getSize());

        IPage<OperationLog> result = operationLogRepository.searchLogs(
                page,
                request.getUsername(),
                request.getOperation(),
                request.getStartTime(),
                request.getEndTime(),
                request.getIp(),
                request.getStatus(),
                request.getSortField(),
                request.getSortDirection()
        );

        log.debug("查询操作日志成功，共 {} 条记录", result.getTotal());
        return result;
    }

    /**
     * 获取登录日志分页数据
     */
    public IPage<LoginLog> getLoginLogs(LogSearchRequest request) {
        log.debug("查询登录日志，参数: {}", request);

        Page<LoginLog> page = new Page<>(request.getPage(), request.getSize());

        IPage<LoginLog> result = loginLogRepository.searchLogs(
                page,
                request.getUsername(),
                request.getLoginType(),
                request.getStartTime(),
                request.getEndTime(),
                request.getIp(),
                request.getStatus(),
                request.getSortField(),
                request.getSortDirection()
        );

        log.debug("查询登录日志成功，共 {} 条记录", result.getTotal());
        return result;
    }

    /**
     * 获取错误日志分页数据
     */
    public IPage<ErrorLog> getErrorLogs(LogSearchRequest request) {
        log.debug("查询错误日志，参数: {}", request);

        Page<ErrorLog> page = new Page<>(request.getPage(), request.getSize());

        IPage<ErrorLog> result = errorLogRepository.searchLogs(
                page,
                request.getUsername(),
                request.getLevel(),
                request.getModule(),
                request.getStartTime(),
                request.getEndTime(),
                request.getIp(),
                request.getSortField(),
                request.getSortDirection()
        );

        log.debug("查询错误日志成功，共 {} 条记录", result.getTotal());
        return result;
    }

    /**
     * 导出日志数据
     */
    @Transactional(readOnly = true)
    public byte[] exportLogs(LogExportRequest request) throws IOException {
        log.info("导出日志数据，类型: {}, 格式: {}", request.getLogType(), request.getExportFormat());

        LogSearchRequest searchRequest = convertToSearchRequest(request);

        switch (request.getLogType().toLowerCase()) {
            case "operation":
                return exportOperationLogs(searchRequest, request.getExportFormat(), request.isIncludeSensitiveInfo());
            case "login":
                return exportLoginLogs(searchRequest, request.getExportFormat(), request.isIncludeSensitiveInfo());
            case "error":
                return exportErrorLogs(searchRequest, request.getExportFormat(), request.isIncludeSensitiveInfo());
            default:
                throw new IllegalArgumentException("不支持的日志类型: " + request.getLogType());
        }
    }

    /**
     * 导出操作日志
     */
    private byte[] exportOperationLogs(LogSearchRequest request, String format, boolean includeSensitive) throws IOException {
        // 设置大的页面大小以获取所有数据
        request.setSize(request.getMaxRecords());
        request.setPage(0);

        IPage<OperationLog> logs = getOperationLogs(request);

        if ("csv".equalsIgnoreCase(format)) {
            return exportOperationLogsToCSV(logs.getRecords(), includeSensitive);
        } else {
            return exportOperationLogsToExcel(logs.getRecords(), includeSensitive);
        }
    }

    /**
     * 导出登录日志
     */
    private byte[] exportLoginLogs(LogSearchRequest request, String format, boolean includeSensitive) throws IOException {
        request.setSize(request.getMaxRecords());
        request.setPage(0);

        IPage<LoginLog> logs = getLoginLogs(request);

        if ("csv".equalsIgnoreCase(format)) {
            return exportLoginLogsToCSV(logs.getRecords(), includeSensitive);
        } else {
            return exportLoginLogsToExcel(logs.getRecords(), includeSensitive);
        }
    }

    /**
     * 导出错误日志
     */
    private byte[] exportErrorLogs(LogSearchRequest request, String format, boolean includeSensitive) throws IOException {
        request.setSize(request.getMaxRecords());
        request.setPage(0);

        IPage<ErrorLog> logs = getErrorLogs(request);

        if ("csv".equalsIgnoreCase(format)) {
            return exportErrorLogsToCSV(logs.getRecords(), includeSensitive);
        } else {
            return exportErrorLogsToExcel(logs.getRecords(), includeSensitive);
        }
    }

    /**
     * 操作日志导出为Excel
     */
    private byte[] exportOperationLogsToExcel(List<OperationLog> logs, boolean includeSensitive) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("操作日志");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "用户名", "操作", "方法", "IP", "执行时长(ms)", "状态", "创建时间"};
            if (includeSensitive) {
                headers = new String[]{"ID", "用户名", "操作", "方法", "请求参数", "IP", "用户代理", "执行时长(ms)", "状态", "错误信息", "创建时间"};
            }

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 填充数据
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < logs.size(); i++) {
                Row row = sheet.createRow(i + 1);
                OperationLog log = logs.get(i);

                int colIndex = 0;
                row.createCell(colIndex++).setCellValue(log.getId() != null ? log.getId() : 0);
                row.createCell(colIndex++).setCellValue(log.getUsername() != null ? log.getUsername() : "");
                row.createCell(colIndex++).setCellValue(log.getOperation() != null ? log.getOperation() : "");
                row.createCell(colIndex++).setCellValue(log.getMethod() != null ? log.getMethod() : "");

                if (includeSensitive) {
                    row.createCell(colIndex++).setCellValue(log.getParams() != null ? log.getParams() : "");
                    row.createCell(colIndex++).setCellValue(log.getIp() != null ? log.getIp() : "");
                    row.createCell(colIndex++).setCellValue(log.getUserAgent() != null ? log.getUserAgent() : "");
                } else {
                    row.createCell(colIndex++).setCellValue(log.getIp() != null ? log.getIp() : "");
                }

                row.createCell(colIndex++).setCellValue(log.getTime() != null ? log.getTime() : 0);
                row.createCell(colIndex++).setCellValue(log.getStatus() != null ? log.getStatus() : "");

                if (includeSensitive) {
                    row.createCell(colIndex++).setCellValue(log.getErrorMessage() != null ? log.getErrorMessage() : "");
                }

                row.createCell(colIndex++).setCellValue(log.getCreateTime() != null ?
                    log.getCreateTime().format(formatter) : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 登录日志导出为Excel
     */
    private byte[] exportLoginLogsToExcel(List<LoginLog> logs, boolean includeSensitive) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("登录日志");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "用户名", "登录类型", "IP", "地理位置", "浏览器", "状态", "消息", "创建时间"};
            if (includeSensitive) {
                headers = new String[]{"ID", "用户名", "登录类型", "IP", "地理位置", "浏览器", "操作系统", "状态", "消息", "用户代理", "会话ID", "创建时间"};
            }

            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < logs.size(); i++) {
                Row row = sheet.createRow(i + 1);
                LoginLog log = logs.get(i);

                int colIndex = 0;
                row.createCell(colIndex++).setCellValue(log.getId() != null ? log.getId() : 0);
                row.createCell(colIndex++).setCellValue(log.getUsername() != null ? log.getUsername() : "");
                row.createCell(colIndex++).setCellValue(log.getLoginType() != null ? log.getLoginType() : "");
                row.createCell(colIndex++).setCellValue(log.getIp() != null ? log.getIp() : "");
                row.createCell(colIndex++).setCellValue(log.getLocation() != null ? log.getLocation() : "");
                row.createCell(colIndex++).setCellValue(log.getBrowser() != null ? log.getBrowser() : "");

                if (includeSensitive) {
                    row.createCell(colIndex++).setCellValue(log.getOs() != null ? log.getOs() : "");
                }

                row.createCell(colIndex++).setCellValue(log.getStatus() != null ? log.getStatus() : "");
                row.createCell(colIndex++).setCellValue(log.getMessage() != null ? log.getMessage() : "");

                if (includeSensitive) {
                    row.createCell(colIndex++).setCellValue(log.getUserAgent() != null ? log.getUserAgent() : "");
                    row.createCell(colIndex++).setCellValue(log.getSessionId() != null ? log.getSessionId() : "");
                }

                row.createCell(colIndex++).setCellValue(log.getCreateTime() != null ?
                    log.getCreateTime().format(formatter) : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 错误日志导出为Excel
     */
    private byte[] exportErrorLogsToExcel(List<ErrorLog> logs, boolean includeSensitive) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("错误日志");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "异常类型", "消息", "模块", "级别", "类名", "方法名", "IP", "用户名", "创建时间"};
            if (includeSensitive) {
                headers = new String[]{"ID", "异常类型", "消息", "堆栈追踪", "模块", "级别", "类名", "方法名", "文件名", "行号", "URL", "请求参数", "IP", "用户代理", "用户名", "创建时间"};
            }

            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < logs.size(); i++) {
                Row row = sheet.createRow(i + 1);
                ErrorLog log = logs.get(i);

                int colIndex = 0;
                row.createCell(colIndex++).setCellValue(log.getId() != null ? log.getId() : 0);
                row.createCell(colIndex++).setCellValue(log.getExceptionType() != null ? log.getExceptionType() : "");
                row.createCell(colIndex++).setCellValue(log.getMessage() != null ? log.getMessage() : "");

                if (includeSensitive) {
                    row.createCell(colIndex++).setCellValue(log.getStackTrace() != null ? log.getStackTrace() : "");
                }

                row.createCell(colIndex++).setCellValue(log.getModule() != null ? log.getModule() : "");
                row.createCell(colIndex++).setCellValue(log.getLevel() != null ? log.getLevel() : "");
                row.createCell(colIndex++).setCellValue(log.getClassName() != null ? log.getClassName() : "");
                row.createCell(colIndex++).setCellValue(log.getMethodName() != null ? log.getMethodName() : "");

                if (includeSensitive) {
                    row.createCell(colIndex++).setCellValue(log.getFileName() != null ? log.getFileName() : "");
                    row.createCell(colIndex++).setCellValue(log.getLineNumber() != null ? log.getLineNumber() : 0);
                    row.createCell(colIndex++).setCellValue(log.getUrl() != null ? log.getUrl() : "");
                    row.createCell(colIndex++).setCellValue(log.getParams() != null ? log.getParams() : "");
                    row.createCell(colIndex++).setCellValue(log.getUserAgent() != null ? log.getUserAgent() : "");
                } else {
                    row.createCell(colIndex++).setCellValue(log.getIp() != null ? log.getIp() : "");
                }

                row.createCell(colIndex++).setCellValue(log.getUsername() != null ? log.getUsername() : "");
                row.createCell(colIndex++).setCellValue(log.getCreateTime() != null ?
                    log.getCreateTime().format(formatter) : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 操作日志导出为CSV
     */
    private byte[] exportOperationLogsToCSV(List<OperationLog> logs, boolean includeSensitive) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(out)) {

            // 写入CSV头部
            StringBuilder headers = new StringBuilder("ID,用户名,操作,方法,IP,执行时长(ms),状态,创建时间");
            if (includeSensitive) {
                headers.append(",请求参数,用户代理,错误信息");
            }
            writer.println(headers.toString());

            // 写入数据
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (OperationLog log : logs) {
                StringBuilder row = new StringBuilder();
                row.append(log.getId() != null ? log.getId() : 0).append(",");
                row.append(escapeCsv(log.getUsername() != null ? log.getUsername() : "")).append(",");
                row.append(escapeCsv(log.getOperation() != null ? log.getOperation() : "")).append(",");
                row.append(escapeCsv(log.getMethod() != null ? log.getMethod() : "")).append(",");
                row.append(escapeCsv(log.getIp() != null ? log.getIp() : "")).append(",");
                row.append(log.getTime() != null ? log.getTime() : 0).append(",");
                row.append(escapeCsv(log.getStatus() != null ? log.getStatus() : "")).append(",");
                row.append(escapeCsv(log.getCreateTime() != null ? log.getCreateTime().format(formatter) : ""));

                if (includeSensitive) {
                    row.append(",").append(escapeCsv(log.getParams() != null ? log.getParams() : ""));
                    row.append(",").append(escapeCsv(log.getUserAgent() != null ? log.getUserAgent() : ""));
                    row.append(",").append(escapeCsv(log.getErrorMessage() != null ? log.getErrorMessage() : ""));
                }

                writer.println(row.toString());
            }

            writer.flush();
            return out.toByteArray();
        }
    }

    /**
     * 登录日志导出为CSV
     */
    private byte[] exportLoginLogsToCSV(List<LoginLog> logs, boolean includeSensitive) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(out)) {

            StringBuilder headers = new StringBuilder("ID,用户名,登录类型,IP,地理位置,浏览器,状态,消息,创建时间");
            if (includeSensitive) {
                headers.append(",操作系统,用户代理,会话ID");
            }
            writer.println(headers.toString());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (LoginLog log : logs) {
                StringBuilder row = new StringBuilder();
                row.append(log.getId() != null ? log.getId() : 0).append(",");
                row.append(escapeCsv(log.getUsername() != null ? log.getUsername() : "")).append(",");
                row.append(escapeCsv(log.getLoginType() != null ? log.getLoginType() : "")).append(",");
                row.append(escapeCsv(log.getIp() != null ? log.getIp() : "")).append(",");
                row.append(escapeCsv(log.getLocation() != null ? log.getLocation() : "")).append(",");
                row.append(escapeCsv(log.getBrowser() != null ? log.getBrowser() : "")).append(",");
                row.append(escapeCsv(log.getStatus() != null ? log.getStatus() : "")).append(",");
                row.append(escapeCsv(log.getMessage() != null ? log.getMessage() : "")).append(",");
                row.append(escapeCsv(log.getCreateTime() != null ? log.getCreateTime().format(formatter) : ""));

                if (includeSensitive) {
                    row.append(",").append(escapeCsv(log.getOs() != null ? log.getOs() : ""));
                    row.append(",").append(escapeCsv(log.getUserAgent() != null ? log.getUserAgent() : ""));
                    row.append(",").append(escapeCsv(log.getSessionId() != null ? log.getSessionId() : ""));
                }

                writer.println(row.toString());
            }

            writer.flush();
            return out.toByteArray();
        }
    }

    /**
     * 错误日志导出为CSV
     */
    private byte[] exportErrorLogsToCSV(List<ErrorLog> logs, boolean includeSensitive) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(out)) {

            StringBuilder headers = new StringBuilder("ID,异常类型,消息,模块,级别,类名,方法名,IP,用户名,创建时间");
            if (includeSensitive) {
                headers.append(",堆栈追踪,文件名,行号,URL,请求参数,用户代理");
            }
            writer.println(headers.toString());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (ErrorLog log : logs) {
                StringBuilder row = new StringBuilder();
                row.append(log.getId() != null ? log.getId() : 0).append(",");
                row.append(escapeCsv(log.getExceptionType() != null ? log.getExceptionType() : "")).append(",");
                row.append(escapeCsv(log.getMessage() != null ? log.getMessage() : "")).append(",");

                if (includeSensitive) {
                    row.append(escapeCsv(log.getStackTrace() != null ? log.getStackTrace() : "")).append(",");
                }

                row.append(escapeCsv(log.getModule() != null ? log.getModule() : "")).append(",");
                row.append(escapeCsv(log.getLevel() != null ? log.getLevel() : "")).append(",");
                row.append(escapeCsv(log.getClassName() != null ? log.getClassName() : "")).append(",");
                row.append(escapeCsv(log.getMethodName() != null ? log.getMethodName() : "")).append(",");
                row.append(escapeCsv(log.getIp() != null ? log.getIp() : "")).append(",");
                row.append(escapeCsv(log.getUsername() != null ? log.getUsername() : "")).append(",");
                row.append(escapeCsv(log.getCreateTime() != null ? log.getCreateTime().format(formatter) : ""));

                if (includeSensitive) {
                    row.append(",").append(escapeCsv(log.getFileName() != null ? log.getFileName() : ""));
                    row.append(",").append(log.getLineNumber() != null ? log.getLineNumber() : 0);
                    row.append(",").append(escapeCsv(log.getUrl() != null ? log.getUrl() : ""));
                    row.append(",").append(escapeCsv(log.getParams() != null ? log.getParams() : ""));
                    row.append(",").append(escapeCsv(log.getUserAgent() != null ? log.getUserAgent() : ""));
                }

                writer.println(row.toString());
            }

            writer.flush();
            return out.toByteArray();
        }
    }

    /**
     * 转换导出请求为搜索请求
     */
    private LogSearchRequest convertToSearchRequest(LogExportRequest exportRequest) {
        LogSearchRequest searchRequest = new LogSearchRequest();
        searchRequest.setUsername(exportRequest.getUsername());
        searchRequest.setOperation(exportRequest.getOperation());
        searchRequest.setStartTime(exportRequest.getStartTime());
        searchRequest.setEndTime(exportRequest.getEndTime());
        searchRequest.setIp(exportRequest.getIp());
        searchRequest.setStatus(exportRequest.getStatus());
        searchRequest.setLevel(exportRequest.getLevel());
        searchRequest.setModule(exportRequest.getModule());
        searchRequest.setLoginType(exportRequest.getLoginType());
        return searchRequest;
    }

    /**
     * CSV字段转义
     */
    private String escapeCsv(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}