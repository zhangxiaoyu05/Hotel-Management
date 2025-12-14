package com.hotel.util;

import com.hotel.dto.report.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * PDF导出工具类
 *
 * @author Hotel System
 * @version 1.0
 */
@Slf4j
@Component
public class PDFExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    /**
     * 导出订单报表到PDF
     */
    public byte[] exportOrderReport(OrderReportDTO report) throws IOException, DocumentException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // 设置中文字体
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font headerFont = new Font(baseFont, 14, Font.BOLD);
            Font normalFont = new Font(baseFont, 12, Font.NORMAL);
            Font smallFont = new Font(baseFont, 10, Font.NORMAL);

            // 报表标题
            Paragraph title = new Paragraph("订单统计报表", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 时间范围
            Paragraph dateRange = new Paragraph(
                "统计时间范围：" + report.getStartDate().format(DATE_FORMATTER) + " 至 " +
                report.getEndDate().format(DATE_FORMATTER),
                normalFont);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);

            // 概览数据
            document.add(new Paragraph("报表概览", headerFont));
            document.add(createOverviewTable(report, normalFont, smallFont));
            document.add(Chunk.NEWLINE);

            // 详细统计表格
            if (report.getOrdersByStatus() != null && !report.getOrdersByStatus().isEmpty()) {
                document.add(new Paragraph("按订单状态统计", headerFont));
                document.add(createStatTable(report.getOrdersByStatus(), normalFont, smallFont));
                document.add(Chunk.NEWLINE);
            }

            if (report.getOrdersByRoomType() != null && !report.getOrdersByRoomType().isEmpty()) {
                document.add(new Paragraph("按房型统计", headerFont));
                document.add(createStatTable(report.getOrdersByRoomType(), normalFont, smallFont));
                document.add(Chunk.NEWLINE);
            }

            // 房型偏好排行
            if (report.getRoomTypePreferences() != null && !report.getRoomTypePreferences().isEmpty()) {
                document.add(new Paragraph("房型偏好排行", headerFont));
                document.add(createRoomTypePreferenceTable(report.getRoomTypePreferences(), normalFont, smallFont));
                document.add(Chunk.NEWLINE);
            }

            document.close();
            return outputStream.toByteArray();
        }
    }

    /**
     * 导出收入报表到PDF
     */
    public byte[] exportRevenueReport(RevenueReportDTO report) throws IOException, DocumentException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // 设置中文字体
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font headerFont = new Font(baseFont, 14, Font.BOLD);
            Font normalFont = new Font(baseFont, 12, Font.NORMAL);
            Font smallFont = new Font(baseFont, 10, Font.NORMAL);

            // 报表标题
            Paragraph title = new Paragraph("收入统计报表", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 时间范围
            Paragraph dateRange = new Paragraph(
                "统计时间范围：" + report.getStartDate().format(DATE_FORMATTER) + " 至 " +
                report.getEndDate().format(DATE_FORMATTER),
                normalFont);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);

            // 核心指标
            document.add(new Paragraph("核心指标", headerFont));
            document.add(createRevenueOverviewTable(report, normalFont, smallFont));
            document.add(Chunk.NEWLINE);

            // 房型收入贡献
            if (report.getRoomTypeRevenueContributions() != null && !report.getRoomTypeRevenueContributions().isEmpty()) {
                document.add(new Paragraph("房型收入贡献", headerFont));
                document.add(createRoomTypeRevenueTable(report.getRoomTypeRevenueContributions(), normalFont, smallFont));
                document.add(Chunk.NEWLINE);
            }

            // 收入预测
            if (report.getRevenueForecasts() != null && !report.getRevenueForecasts().isEmpty()) {
                document.add(new Paragraph("收入预测", headerFont));
                document.add(createRevenueForecastTable(report.getRevenueForecasts(), normalFont, smallFont));
                document.add(Chunk.NEWLINE);
            }

            document.close();
            return outputStream.toByteArray();
        }
    }

    /**
     * 导出用户报表到PDF
     */
    public byte[] exportUserReport(UserReportDTO report) throws IOException, DocumentException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // 设置中文字体
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font headerFont = new Font(baseFont, 14, Font.BOLD);
            Font normalFont = new Font(baseFont, 12, Font.NORMAL);
            Font smallFont = new Font(baseFont, 10, Font.NORMAL);

            // 报表标题
            Paragraph title = new Paragraph("用户统计报表", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 时间范围
            Paragraph dateRange = new Paragraph(
                "统计时间范围：" + report.getStartDate().format(DATE_FORMATTER) + " 至 " +
                report.getEndDate().format(DATE_FORMATTER),
                normalFont);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);

            // 用户概览
            document.add(new Paragraph("用户概览", headerFont));
            document.add(createUserOverviewTable(report, normalFont, smallFont));
            document.add(Chunk.NEWLINE);

            // 消费金额排行
            if (report.getTopUsersBySpending() != null && !report.getTopUsersBySpending().isEmpty()) {
                document.add(new Paragraph("消费金额排行", headerFont));
                document.add(createUserSpendingTable(report.getTopUsersBySpending(), normalFont, smallFont));
                document.add(Chunk.NEWLINE);
            }

            // 用户行为分析
            if (report.getUserBehaviorAnalysis() != null) {
                document.add(new Paragraph("用户行为分析", headerFont));
                document.add(createUserBehaviorAnalysisTable(report.getUserBehaviorAnalysis(), normalFont, smallFont));
                document.add(Chunk.NEWLINE);
            }

            document.close();
            return outputStream.toByteArray();
        }
    }

    /**
     * 导出房间报表到PDF
     */
    public byte[] exportRoomReport(RoomReportDTO report) throws IOException, DocumentException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // 设置中文字体
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font headerFont = new Font(baseFont, 14, Font.BOLD);
            Font normalFont = new Font(baseFont, 12, Font.NORMAL);
            Font smallFont = new Font(baseFont, 10, Font.NORMAL);

            // 报表标题
            Paragraph title = new Paragraph("房间统计报表", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 时间范围
            Paragraph dateRange = new Paragraph(
                "统计时间范围：" + report.getStartDate().format(DATE_FORMATTER) + " 至 " +
                report.getEndDate().format(DATE_FORMATTER),
                normalFont);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);

            // 房间概览
            document.add(new Paragraph("房间概览", headerFont));
            document.add(createRoomOverviewTable(report, normalFont, smallFont));
            document.add(Chunk.NEWLINE);

            // 房型绩效对比
            if (report.getRoomTypePerformances() != null && !report.getRoomTypePerformances().isEmpty()) {
                document.add(new Paragraph("房型绩效对比", headerFont));
                document.add(createRoomTypePerformanceTable(report.getRoomTypePerformances(), normalFont, smallFont));
                document.add(Chunk.NEWLINE);
            }

            // 房间绩效排行
            if (report.getTopPerformingRooms() != null && !report.getTopPerformingRooms().isEmpty()) {
                document.add(new Paragraph("房间绩效排行", headerFont));
                document.add(createRoomPerformanceTable(report.getTopPerformingRooms(), normalFont, smallFont));
                document.add(Chunk.NEWLINE);
            }

            document.close();
            return outputStream.toByteArray();
        }
    }

    // ================= 表格创建方法 =================

    private PdfPTable createOverviewTable(OrderReportDTO report, Font normalFont, Font smallFont) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(80);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "指标", "数值", normalFont);
        addTableRow(table, "总订单数", String.valueOf(report.getTotalOrders()), smallFont);
        addTableRow(table, "总收入", report.getTotalRevenue().toString(), smallFont);
        addTableRow(table, "平均订单价值", report.getAverageOrderValue().toString(), smallFont);
        addTableRow(table, "订单完成率", String.format("%.2f%%", report.getCompletionRate()), smallFont);
        addTableRow(table, "订单取消率", String.format("%.2f%%", report.getCancellationRate()), smallFont);

        return table;
    }

    private PdfPTable createRevenueOverviewTable(RevenueReportDTO report, Font normalFont, Font smallFont) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(80);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "指标", "数值", normalFont);
        addTableRow(table, "总收入", report.getTotalRevenue().toString(), smallFont);
        addTableRow(table, "平均每日房价 (ADR)", report.getAverageDailyRate().toString(), smallFont);
        addTableRow(table, "每间可售房收入 (RevPAR)", report.getRevenuePerAvailableRoom().toString(), smallFont);
        addTableRow(table, "入住率", String.format("%.2f%%", report.getOccupancyRate()), smallFont);
        addTableRow(table, "收入增长率", String.format("%.2f%%", report.getRevenueGrowthRate()), smallFont);

        return table;
    }

    private PdfPTable createUserOverviewTable(UserReportDTO report, Font normalFont, Font smallFont) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(80);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "指标", "数值", normalFont);
        addTableRow(table, "总用户数", String.valueOf(report.getTotalUsers()), smallFont);
        addTableRow(table, "活跃用户数", String.valueOf(report.getActiveUsers()), smallFont);
        addTableRow(table, "用户留存率", String.format("%.2f%%", report.getUserRetentionRate()), smallFont);
        addTableRow(table, "用户转化率", String.format("%.2f%%", report.getUserConversionRate()), smallFont);

        return table;
    }

    private PdfPTable createRoomOverviewTable(RoomReportDTO report, Font normalFont, Font smallFont) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(80);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "指标", "数值", normalFont);
        addTableRow(table, "总房间数", String.valueOf(report.getTotalRooms()), smallFont);
        addTableRow(table, "可用房间数", String.valueOf(report.getAvailableRooms()), smallFont);
        addTableRow(table, "维护中房间数", String.valueOf(report.getMaintenanceRooms()), smallFont);
        addTableRow(table, "入住率", String.format("%.2f%%", report.getOccupancyRate()), smallFont);
        addTableRow(table, "平均房价", report.getAverageRoomRate().toString(), smallFont);

        return table;
    }

    private PdfPTable createStatTable(Map<String, Long> data, Font normalFont, Font smallFont) {
        if (data == null || data.isEmpty()) return new PdfPTable(1);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(80);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "类别", "数量", normalFont);

        for (Map.Entry<String, Long> entry : data.entrySet()) {
            addTableRow(table, entry.getKey(), String.valueOf(entry.getValue()), smallFont);
        }

        return table;
    }

    private PdfPTable createRoomTypePreferenceTable(List<OrderReportDTO.RoomTypePreference> data, Font normalFont, Font smallFont) {
        if (data == null || data.isEmpty()) return new PdfPTable(1);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "房型", "订单数量", "收入金额", "占比", normalFont);

        for (OrderReportDTO.RoomTypePreference preference : data) {
            addTableRow(table, preference.getRoomTypeName(),
                String.valueOf(preference.getOrderCount()),
                preference.getRevenue().toString(),
                String.format("%.2f%%", preference.getPercentage()), smallFont);
        }

        return table;
    }

    private PdfPTable createRoomTypeRevenueTable(List<RevenueReportDTO.RoomTypeRevenueContribution> data, Font normalFont, Font smallFont) {
        if (data == null || data.isEmpty()) return new PdfPTable(1);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "房型", "收入金额", "订单数量", "平均订单价值", "收入占比", normalFont);

        for (RevenueReportDTO.RoomTypeRevenueContribution contribution : data) {
            addTableRow(table, contribution.getRoomTypeName(),
                contribution.getRevenue().toString(),
                String.valueOf(contribution.getOrderCount()),
                contribution.getAverageOrderValue().toString(),
                String.format("%.2f%%", contribution.getPercentage()), smallFont);
        }

        return table;
    }

    private PdfPTable createRevenueForecastTable(List<RevenueReportDTO.RevenueForecast> data, Font normalFont, Font smallFont) {
        if (data == null || data.isEmpty()) return new PdfPTable(1);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(80);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "预测期间", "预测收入", "置信度", normalFont);

        for (RevenueReportDTO.RevenueForecast forecast : data) {
            addTableRow(table, forecast.getPeriod(),
                forecast.getPredictedRevenue().toString(),
                String.format("%.0f%%", forecast.getConfidenceLevel().doubleValue() * 100), smallFont);
        }

        return table;
    }

    private PdfPTable createUserSpendingTable(List<UserReportDTO.UserSpendingSummary> data, Font normalFont, Font smallFont) {
        if (data == null || data.isEmpty()) return new PdfPTable(1);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "用户名", "邮箱", "总消费金额", "订单数量", normalFont);

        for (UserReportDTO.UserSpendingSummary summary : data) {
            addTableRow(table, summary.getUsername(),
                summary.getEmail(),
                summary.getTotalSpending().toString(),
                String.valueOf(summary.getOrderCount()), smallFont);
        }

        return table;
    }

    private PdfPTable createUserBehaviorAnalysisTable(UserReportDTO.UserBehaviorAnalysis analysis, Font normalFont, Font smallFont) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(80);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "指标", "数值", normalFont);

        if (analysis != null) {
            addTableRow(table, "平均每用户订单数", String.format("%.2f", analysis.getAverageOrdersPerUser()), smallFont);
            addTableRow(table, "平均每用户消费金额", analysis.getAverageSpendingPerUser().toString(), smallFont);
            addTableRow(table, "重复购买率", String.format("%.2f%%", analysis.getRepeatPurchaseRate()), smallFont);
        }

        return table;
    }

    private PdfPTable createRoomTypePerformanceTable(List<RoomReportDTO.RoomTypePerformance> data, Font normalFont, Font smallFont) {
        if (data == null || data.isEmpty()) return new PdfPTable(1);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "房型", "房间数量", "订单总数", "总收入", "入住率", "收入占比", normalFont);

        for (RoomReportDTO.RoomTypePerformance performance : data) {
            addTableRow(table, performance.getRoomTypeName(),
                String.valueOf(performance.getTotalRooms()),
                String.valueOf(performance.getTotalOrders()),
                performance.getTotalRevenue().toString(),
                String.format("%.2f%%", performance.getOccupancyRate()),
                String.format("%.2f%%", performance.getRevenueContribution()), smallFont);
        }

        return table;
    }

    private PdfPTable createRoomPerformanceTable(List<RoomReportDTO.RoomPerformance> data, Font normalFont, Font smallFont) {
        if (data == null || data.isEmpty()) return new PdfPTable(1);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addTableHeader(table, "房间号", "房型", "订单总数", "总收入", "平均订单价值", normalFont);

        for (RoomReportDTO.RoomPerformance performance : data) {
            addTableRow(table, performance.getRoomNumber(),
                performance.getRoomTypeName(),
                String.valueOf(performance.getTotalOrders()),
                performance.getTotalRevenue().toString(),
                performance.getAverageRevenuePerOrder().toString(), smallFont);
        }

        return table;
    }

    // ================= 辅助方法 =================

    private void addTableHeader(PdfPTable table, String header1, String header2, Font font) {
        addTableHeader(table, new String[]{header1, header2}, font);
    }

    private void addTableHeader(PdfPTable table, String header1, String header2, String header3, Font font) {
        addTableHeader(table, new String[]{header1, header2, header3}, font);
    }

    private void addTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(new Color(240, 240, 240));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String value1, String value2, Font font) {
        addTableRow(table, new String[]{value1, value2}, font);
    }

    private void addTableRow(PdfPTable table, String value1, String value2, String value3, Font font) {
        addTableRow(table, new String[]{value1, value2, value3}, font);
    }

    private void addTableRow(PdfPTable table, String value1, String value2, String value3, String value4, Font font) {
        addTableRow(table, new String[]{value1, value2, value3, value4}, font);
    }

    private void addTableRow(PdfPTable table, String value1, String value2, String value3, String value4, String value5, Font font) {
        addTableRow(table, new String[]{value1, value2, value3, value4, value5}, font);
    }

    private void addTableRow(PdfPTable table, String[] values, Font font) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(3);
            table.addCell(cell);
        }
    }
}