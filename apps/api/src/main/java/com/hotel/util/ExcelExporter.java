package com.hotel.util;

import com.hotel.dto.report.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Excel导出工具类
 *
 * @author Hotel System
 * @version 1.0
 */
@Slf4j
@Component
public class ExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 导出订单报表到Excel
     */
    public byte[] exportOrderReport(OrderReportDTO report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 创建概览工作表
            createOrderOverviewSheet(workbook, report);

            // 创建详细数据工作表
            createOrderDetailSheet(workbook, report);

            // 创建趋势数据工作表
            createOrderTrendSheet(workbook, report);

            // 创建房型偏好工作表
            createRoomTypePreferenceSheet(workbook, report);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 导出收入报表到Excel
     */
    public byte[] exportRevenueReport(RevenueReportDTO report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 创建概览工作表
            createRevenueOverviewSheet(workbook, report);

            // 创建月度收入工作表
            createMonthlyRevenueSheet(workbook, report);

            // 创建房型收入贡献工作表
            createRoomTypeRevenueSheet(workbook, report);

            // 创建日收入趋势工作表
            createDailyRevenueTrendSheet(workbook, report);

            // 创建收入预测工作表
            createRevenueForecastSheet(workbook, report);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 导出用户报表到Excel
     */
    public byte[] exportUserReport(UserReportDTO report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 创建用户概览工作表
            createUserOverviewSheet(workbook, report);

            // 创建用户排行工作表
            createUserRankingSheet(workbook, report);

            // 创建用户注册趋势工作表
            createUserRegistrationTrendSheet(workbook, report);

            // 创建用户行为分析工作表
            createUserBehaviorAnalysisSheet(workbook, report);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 导出房间报表到Excel
     */
    public byte[] exportRoomReport(RoomReportDTO report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 创建房间概览工作表
            createRoomOverviewSheet(workbook, report);

            // 创建房间绩效工作表
            createRoomPerformanceSheet(workbook, report);

            // 创建房型对比工作表
            createRoomTypeComparisonSheet(workbook, report);

            // 创建房间使用趋势工作表
            createRoomUtilizationTrendSheet(workbook, report);

            // 创建维护统计工作表
            createMaintenanceStatsSheet(workbook, report);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ================= 订单报表工作表创建方法 =================

    private void createOrderOverviewSheet(XSSFWorkbook workbook, OrderReportDTO report) {
        Sheet sheet = workbook.createSheet("订单概览");

        // 创建标题样式
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 报表标题
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("订单报表概览");
        titleCell.setCellStyle(titleStyle);

        // 时间范围
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("统计时间范围");
        dateRow.createCell(1).setCellValue(
            report.getStartDate().format(DATE_FORMATTER) + " 至 " +
            report.getEndDate().format(DATE_FORMATTER));

        rowNum++; // 空行

        // 基础统计数据
        String[][] overviewData = {
            {"总订单数", String.valueOf(report.getTotalOrders())},
            {"总收入", report.getTotalRevenue().toString()},
            {"平均订单价值", report.getAverageOrderValue().toString()},
            {"订单完成率", String.format("%.2f%%", report.getCompletionRate())},
            {"订单取消率", String.format("%.2f%%", report.getCancellationRate())}
        };

        for (String[] data : overviewData) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data[0]);
            row.createCell(1).setCellValue(data[1]);
        }

        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createOrderDetailSheet(XSSFWorkbook workbook, OrderReportDTO report) {
        Sheet sheet = workbook.createSheet("订单详细统计");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 按状态统计
        rowNum = createStatTable(sheet, rowNum, "按订单状态统计", headerStyle,
            report.getOrdersByStatus());

        rowNum += 2; // 空行

        // 按房型统计
        rowNum = createStatTable(sheet, rowNum, "按房型统计", headerStyle,
            report.getOrdersByRoomType());

        rowNum += 2; // 空行

        // 按月份统计收入
        rowNum = createRevenueTable(sheet, rowNum, "按月份收入统计", headerStyle,
            report.getRevenueByMonth());

        // 自动调整列宽
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createOrderTrendSheet(XSSFWorkbook workbook, OrderReportDTO report) {
        Sheet sheet = workbook.createSheet("订单趋势");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"日期", "订单数量", "收入金额"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (OrderReportDTO.OrderTrendData trend : report.getOrderTrends()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(trend.getDate());
            row.createCell(1).setCellValue(trend.getOrderCount());
            row.createCell(2).setCellValue(trend.getRevenue().doubleValue());
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createRoomTypePreferenceSheet(XSSFWorkbook workbook, OrderReportDTO report) {
        Sheet sheet = workbook.createSheet("房型偏好排行");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"房型", "订单数量", "收入金额", "占比"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (OrderReportDTO.RoomTypePreference preference : report.getRoomTypePreferences()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(preference.getRoomTypeName());
            row.createCell(1).setCellValue(preference.getOrderCount());
            row.createCell(2).setCellValue(preference.getRevenue().doubleValue());
            row.createCell(3).setCellValue(String.format("%.2f%%", preference.getPercentage()));
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ================= 收入报表工作表创建方法 =================

    private void createRevenueOverviewSheet(XSSFWorkbook workbook, RevenueReportDTO report) {
        Sheet sheet = workbook.createSheet("收入概览");
        CellStyle titleStyle = createTitleStyle(workbook);

        int rowNum = 0;

        // 报表标题
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("收入报表概览");
        titleCell.setCellStyle(titleStyle);

        // 时间范围
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("统计时间范围");
        dateRow.createCell(1).setCellValue(
            report.getStartDate().format(DATE_FORMATTER) + " 至 " +
            report.getEndDate().format(DATE_FORMATTER));

        rowNum++; // 空行

        // 核心指标
        String[][] metricsData = {
            {"总收入", report.getTotalRevenue().toString()},
            {"平均每日房价 (ADR)", report.getAverageDailyRate().toString()},
            {"每间可售房收入 (RevPAR)", report.getRevenuePerAvailableRoom().toString()},
            {"入住率", String.format("%.2f%%", report.getOccupancyRate())},
            {"收入增长率", String.format("%.2f%%", report.getRevenueGrowthRate())}
        };

        for (String[] data : metricsData) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data[0]);
            row.createCell(1).setCellValue(data[1]);
        }

        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createMonthlyRevenueSheet(XSSFWorkbook workbook, RevenueReportDTO report) {
        Sheet sheet = workbook.createSheet("月度收入");
        int rowNum = createRevenueTable(sheet, 0, "按月份收入统计", createHeaderStyle(workbook),
            report.getMonthlyRevenue());

        // 自动调整列宽
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createRoomTypeRevenueSheet(XSSFWorkbook workbook, RevenueReportDTO report) {
        Sheet sheet = workbook.createSheet("房型收入贡献");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"房型", "收入金额", "订单数量", "平均订单价值", "收入占比"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (RevenueReportDTO.RoomTypeRevenueContribution contribution :
                report.getRoomTypeRevenueContributions()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(contribution.getRoomTypeName());
            row.createCell(1).setCellValue(contribution.getRevenue().doubleValue());
            row.createCell(2).setCellValue(contribution.getOrderCount());
            row.createCell(3).setCellValue(contribution.getAverageOrderValue().doubleValue());
            row.createCell(4).setCellValue(String.format("%.2f%%", contribution.getPercentage()));
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createDailyRevenueTrendSheet(XSSFWorkbook workbook, RevenueReportDTO report) {
        Sheet sheet = workbook.createSheet("日收入趋势");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"日期", "收入金额", "订单数量", "平均订单价值"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (RevenueReportDTO.DailyRevenueData data : report.getDailyRevenueTrends()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data.getDate());
            row.createCell(1).setCellValue(data.getRevenue().doubleValue());
            row.createCell(2).setCellValue(data.getOrderCount());
            row.createCell(3).setCellValue(data.getAverageOrderValue().doubleValue());
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createRevenueForecastSheet(XSSFWorkbook workbook, RevenueReportDTO report) {
        Sheet sheet = workbook.createSheet("收入预测");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"预测期间", "预测收入", "增长率", "置信度"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (RevenueReportDTO.RevenueForecast forecast : report.getRevenueForecasts()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(forecast.getPeriod());
            row.createCell(1).setCellValue(forecast.getPredictedRevenue().doubleValue());
            row.createCell(2).setCellValue(String.format("%.2f%%", forecast.getGrowthRate()));
            row.createCell(3).setCellValue(String.format("%.0f%%", forecast.getConfidenceLevel().doubleValue() * 100));
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ================= 用户报表工作表创建方法 =================

    private void createUserOverviewSheet(XSSFWorkbook workbook, UserReportDTO report) {
        Sheet sheet = workbook.createSheet("用户概览");
        CellStyle titleStyle = createTitleStyle(workbook);

        int rowNum = 0;

        // 报表标题
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("用户报表概览");
        titleCell.setCellStyle(titleStyle);

        // 时间范围
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("统计时间范围");
        dateRow.createCell(1).setCellValue(
            report.getStartDate().format(DATE_FORMATTER) + " 至 " +
            report.getEndDate().format(DATE_FORMATTER));

        rowNum++; // 空行

        // 基础统计
        String[][] overviewData = {
            {"总用户数", String.valueOf(report.getTotalUsers())},
            {"活跃用户数", String.valueOf(report.getActiveUsers())},
            {"用户留存率", String.format("%.2f%%", report.getUserRetentionRate())},
            {"用户转化率", String.format("%.2f%%", report.getUserConversionRate())}
        };

        for (String[] data : overviewData) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data[0]);
            row.createCell(1).setCellValue(data[1]);
        }

        // 按角色统计
        rowNum += 2;
        rowNum = createStatTable(sheet, rowNum, "按角色用户统计", createHeaderStyle(workbook),
            report.getUsersByRole());

        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createUserRankingSheet(XSSFWorkbook workbook, UserReportDTO report) {
        Sheet sheet = workbook.createSheet("用户排行");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 消费金额排行
        rowNum = createUserSpendingTable(sheet, rowNum, "消费金额排行", headerStyle,
            report.getTopUsersBySpending());

        rowNum += 2;

        // 下单次数排行
        rowNum = createUserOrderTable(sheet, rowNum, "下单次数排行", headerStyle,
            report.getTopUsersByOrders());

        // 自动调整列宽
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createUserRegistrationTrendSheet(XSSFWorkbook workbook, UserReportDTO report) {
        Sheet sheet = workbook.createSheet("用户注册趋势");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"日期", "新增用户", "累计用户"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (UserReportDTO.UserRegistrationTrend trend : report.getUserRegistrationTrends()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(trend.getDate());
            row.createCell(1).setCellValue(trend.getNewUserCount());
            row.createCell(2).setCellValue(trend.getCumulativeUserCount());
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createUserBehaviorAnalysisSheet(XSSFWorkbook workbook, UserReportDTO report) {
        Sheet sheet = workbook.createSheet("用户行为分析");

        int rowNum = 0;

        // 行为分析数据
        if (report.getUserBehaviorAnalysis() != null) {
            UserReportDTO.UserBehaviorAnalysis analysis = report.getUserBehaviorAnalysis();

            String[][] behaviorData = {
                {"平均每用户订单数", String.format("%.2f", analysis.getAverageOrdersPerUser())},
                {"平均每用户消费金额", analysis.getAverageSpendingPerUser().toString()},
                {"重复购买率", String.format("%.2f%%", analysis.getRepeatPurchaseRate())}
            };

            for (String[] data : behaviorData) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(data[0]);
                row.createCell(1).setCellValue(data[1]);
            }

            // 预订时间分布
            if (analysis.getBookingTimeDistribution() != null) {
                rowNum += 2;
                Row headerRow = sheet.createRow(rowNum++);
                headerRow.createCell(0).setCellValue("预订时间分布");

                for (Map.Entry<String, Long> entry : analysis.getBookingTimeDistribution().entrySet()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(entry.getKey());
                    row.createCell(1).setCellValue(entry.getValue() + "次");
                }
            }
        }

        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    // ================= 房间报表工作表创建方法 =================

    private void createRoomOverviewSheet(XSSFWorkbook workbook, RoomReportDTO report) {
        Sheet sheet = workbook.createSheet("房间概览");
        CellStyle titleStyle = createTitleStyle(workbook);

        int rowNum = 0;

        // 报表标题
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("房间报表概览");
        titleCell.setCellStyle(titleStyle);

        // 时间范围
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("统计时间范围");
        dateRow.createCell(1).setCellValue(
            report.getStartDate().format(DATE_FORMATTER) + " 至 " +
            report.getEndDate().format(DATE_FORMATTER));

        rowNum++; // 空行

        // 基础统计
        String[][] overviewData = {
            {"总房间数", String.valueOf(report.getTotalRooms())},
            {"可用房间数", String.valueOf(report.getAvailableRooms())},
            {"维护中房间数", String.valueOf(report.getMaintenanceRooms())},
            {"入住率", String.format("%.2f%%", report.getOccupancyRate())},
            {"平均房价", report.getAverageRoomRate().toString()}
        };

        for (String[] data : overviewData) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data[0]);
            row.createCell(1).setCellValue(data[1]);
        }

        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createRoomPerformanceSheet(XSSFWorkbook workbook, RoomReportDTO report) {
        Sheet sheet = workbook.createSheet("房间绩效排行");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"房间号", "房型", "订单总数", "总收入", "平均订单价值", "入住天数", "日均收入"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (RoomReportDTO.RoomPerformance performance : report.getTopPerformingRooms()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(performance.getRoomNumber());
            row.createCell(1).setCellValue(performance.getRoomTypeName());
            row.createCell(2).setCellValue(performance.getTotalOrders());
            row.createCell(3).setCellValue(performance.getTotalRevenue().doubleValue());
            row.createCell(4).setCellValue(performance.getAverageRevenuePerOrder().doubleValue());
            row.createCell(5).setCellValue(performance.getTotalNights());
            row.createCell(6).setCellValue(performance.getRevenuePerNight().doubleValue());
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createRoomTypeComparisonSheet(XSSFWorkbook workbook, RoomReportDTO report) {
        Sheet sheet = workbook.createSheet("房型绩效对比");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"房型", "房间数量", "订单总数", "总收入", "入住率", "平均房价", "RevPAR", "收入占比"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (RoomReportDTO.RoomTypePerformance performance : report.getRoomTypePerformances()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(performance.getRoomTypeName());
            row.createCell(1).setCellValue(performance.getTotalRooms());
            row.createCell(2).setCellValue(performance.getTotalOrders());
            row.createCell(3).setCellValue(performance.getTotalRevenue().doubleValue());
            row.createCell(4).setCellValue(String.format("%.2f%%", performance.getOccupancyRate()));
            row.createCell(5).setCellValue(performance.getAverageDailyRate().doubleValue());
            row.createCell(6).setCellValue(performance.getRevenuePerAvailableRoom().doubleValue());
            row.createCell(7).setCellValue(String.format("%.2f%%", performance.getRevenueContribution()));
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createRoomUtilizationTrendSheet(XSSFWorkbook workbook, RoomReportDTO report) {
        Sheet sheet = workbook.createSheet("房间使用趋势");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"日期", "总房间数", "占用房间数", "可用房间数", "入住率", "日收入"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (RoomReportDTO.RoomUtilizationTrend trend : report.getRoomUtilizationTrends()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(trend.getDate());
            row.createCell(1).setCellValue(trend.getTotalRooms());
            row.createCell(2).setCellValue(trend.getOccupiedRooms());
            row.createCell(3).setCellValue(trend.getAvailableRooms());
            row.createCell(4).setCellValue(String.format("%.2f%%", trend.getOccupancyRate()));
            row.createCell(5).setCellValue(trend.getDailyRevenue().doubleValue());
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createMaintenanceStatsSheet(XSSFWorkbook workbook, RoomReportDTO report) {
        Sheet sheet = workbook.createSheet("维护统计");

        int rowNum = 0;

        if (report.getMaintenanceStats() != null) {
            RoomReportDTO.RoomMaintenanceStats stats = report.getMaintenanceStats();

            // 基础统计
            String[][] statsData = {
                {"当前维护中房间数", String.valueOf(stats.getCurrentlyUnderMaintenance())},
                {"总维护天数", String.valueOf(stats.getTotalMaintenanceDays())}
            };

            for (String[] data : statsData) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(data[0]);
                row.createCell(1).setCellValue(data[1]);
            }

            // 维护原因统计
            if (stats.getMaintenanceReasons() != null) {
                rowNum += 2;
                Row headerRow = sheet.createRow(rowNum++);
                headerRow.createCell(0).setCellValue("维护原因统计");

                for (Map.Entry<String, Long> entry : stats.getMaintenanceReasons().entrySet()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(entry.getKey());
                    row.createCell(1).setCellValue(entry.getValue() + "次");
                }
            }

            // 最近维护记录
            if (stats.getRecentMaintenanceRecords() != null && !stats.getRecentMaintenanceRecords().isEmpty()) {
                rowNum += 2;
                Row headerRow = sheet.createRow(rowNum++);
                headerRow.createCell(0).setCellValue("最近维护记录");

                String[] headers = {"房间号", "维护原因", "开始日期", "结束日期", "持续天数"};
                Row subHeaderRow = sheet.createRow(rowNum++);
                for (int i = 0; i < headers.length; i++) {
                    subHeaderRow.createCell(i).setCellValue(headers[i]);
                }

                for (RoomReportDTO.MaintenanceRecord record : stats.getRecentMaintenanceRecords()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(record.getRoomNumber());
                    row.createCell(1).setCellValue(record.getReason());
                    row.createCell(2).setCellValue(record.getStartDate().format(DATE_FORMATTER));
                    row.createCell(3).setCellValue(record.getEndDate().format(DATE_FORMATTER));
                    row.createCell(4).setCellValue(record.getDurationDays());
                }
            }
        }

        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    // ================= 辅助方法 =================

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private int createStatTable(Sheet sheet, int startRow, String title, CellStyle headerStyle,
                              Map<String, Long> data) {
        if (data == null || data.isEmpty()) return startRow;

        // 标题行
        Row titleRow = sheet.createRow(startRow++);
        titleRow.createCell(0).setCellValue(title);

        // 表头
        Row headerRow = sheet.createRow(startRow++);
        headerRow.createCell(0).setCellValue("类别");
        headerRow.createCell(1).setCellValue("数量");

        // 设置表头样式
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);

        // 数据行
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            Row row = sheet.createRow(startRow++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        return startRow;
    }

    private int createRevenueTable(Sheet sheet, int startRow, String title, CellStyle headerStyle,
                                Map<String, ?> data) {
        if (data == null || data.isEmpty()) return startRow;

        // 标题行
        Row titleRow = sheet.createRow(startRow++);
        titleRow.createCell(0).setCellValue(title);

        // 表头
        Row headerRow = sheet.createRow(startRow++);
        headerRow.createCell(0).setCellValue("期间");
        headerRow.createCell(1).setCellValue("金额");

        // 设置表头样式
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);

        // 数据行
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            Row row = sheet.createRow(startRow++);
            row.createCell(0).setCellValue(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof BigDecimal) {
                row.createCell(1).setCellValue(((BigDecimal) value).doubleValue());
            } else {
                row.createCell(1).setCellValue(value.toString());
            }
        }

        return startRow;
    }

    private int createUserSpendingTable(Sheet sheet, int startRow, String title, CellStyle headerStyle,
                                      List<UserReportDTO.UserSpendingSummary> data) {
        if (data == null || data.isEmpty()) return startRow;

        // 标题行
        Row titleRow = sheet.createRow(startRow++);
        titleRow.createCell(0).setCellValue(title);

        // 表头
        Row headerRow = sheet.createRow(startRow++);
        String[] headers = {"用户ID", "用户名", "邮箱", "总消费金额", "订单数量", "平均订单价值"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据行
        for (UserReportDTO.UserSpendingSummary summary : data) {
            Row row = sheet.createRow(startRow++);
            row.createCell(0).setCellValue(summary.getUserId());
            row.createCell(1).setCellValue(summary.getUsername());
            row.createCell(2).setCellValue(summary.getEmail());
            row.createCell(3).setCellValue(summary.getTotalSpending().doubleValue());
            row.createCell(4).setCellValue(summary.getOrderCount());
            row.createCell(5).setCellValue(summary.getAverageOrderValue().doubleValue());
        }

        return startRow;
    }

    private int createUserOrderTable(Sheet sheet, int startRow, String title, CellStyle headerStyle,
                                   List<UserReportDTO.UserOrderSummary> data) {
        if (data == null || data.isEmpty()) return startRow;

        // 标题行
        Row titleRow = sheet.createRow(startRow++);
        titleRow.createCell(0).setCellValue(title);

        // 表头
        Row headerRow = sheet.createRow(startRow++);
        String[] headers = {"用户ID", "用户名", "邮箱", "订单数量", "最后下单时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据行
        for (UserReportDTO.UserOrderSummary summary : data) {
            Row row = sheet.createRow(startRow++);
            row.createCell(0).setCellValue(summary.getUserId());
            row.createCell(1).setCellValue(summary.getUsername());
            row.createCell(2).setCellValue(summary.getEmail());
            row.createCell(3).setCellValue(summary.getOrderCount());
            if (summary.getLastOrderDate() != null) {
                row.createCell(4).setCellValue(summary.getLastOrderDate().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                row.createCell(4).setCellValue("");
            }
        }

        return startRow;
    }
}