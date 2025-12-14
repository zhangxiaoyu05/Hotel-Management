package com.hotel.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotel.controller.BaseController;
import com.hotel.dto.report.OrderReportDTO;
import com.hotel.dto.report.ReportExportRequest;
import com.hotel.dto.report.RevenueReportDTO;
import com.hotel.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 报表控制器测试
 *
 * @author Hotel System
 * @version 1.0
 */
@WebMvcTest(ReportController.class)
@DisplayName("报表控制器测试")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("获取订单报表 - 成功")
    void getOrderReport_Success() throws Exception {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        OrderReportDTO mockReport = createMockOrderReport(startDate, endDate);
        when(reportService.generateOrderReport(eq(startDate), eq(endDate), isNull(), isNull()))
            .thenReturn(mockReport);

        // When & Then
        mockMvc.perform(get("/v1/admin/reports/orders")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(100))
                .andExpect(jsonPath("$.data.totalRevenue").value(10000))
                .andExpect(jsonPath("$.message").value("订单报表生成成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("获取收入报表 - 成功")
    void getRevenueReport_Success() throws Exception {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        RevenueReportDTO mockReport = createMockRevenueReport(startDate, endDate);
        when(reportService.generateRevenueReport(eq(startDate), eq(endDate), isNull()))
            .thenReturn(mockReport);

        // When & Then
        mockMvc.perform(get("/v1/admin/reports/revenue")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").value(15000))
                .andExpect(jsonPath("$.data.occupancyRate").value(75.5));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("导出报表 - 成功")
    void exportReport_Success() throws Exception {
        // Given
        ReportExportRequest request = new ReportExportRequest();
        request.setReportType(ReportExportRequest.ReportType.ORDER);
        request.setExportFormat("EXCEL");
        request.setStartDate(LocalDate.now().minusDays(30));
        request.setEndDate(LocalDate.now());

        String fileUrl = "/exports/reports/ORDER_20241214.xlsx";
        when(reportService.exportReport(any(ReportExportRequest.class)))
            .thenReturn(fileUrl);

        // When & Then
        mockMvc.perform(post("/v1/admin/reports/export")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(fileUrl));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("获取报表概览 - 成功")
    void getReportOverview_Success() throws Exception {
        // Given
        when(reportService.getReportOverview())
            .thenReturn(createMockReportOverview());

        // When & Then
        mockMvc.perform(get("/v1/admin/reports/overview")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.todayOrders").value(25))
                .andExpect(jsonPath("$.data.currentOccupancyRate").value(85.0));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("访问报表接口 - 权限不足")
    void getReport_InsufficientPermission() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/admin/reports/orders")
                .param("startDate", "2024-11-14")
                .param("endDate", "2024-12-14")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("访问报表接口 - 未认证")
    void getReport_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/admin/reports/orders")
                .param("startDate", "2024-11-14")
                .param("endDate", "2024-12-14")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ================= 辅助方法 =================

    private OrderReportDTO createMockOrderReport(LocalDate startDate, LocalDate endDate) {
        OrderReportDTO report = new OrderReportDTO();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setTotalOrders(100L);
        report.setTotalRevenue(new BigDecimal("10000"));
        report.setAverageOrderValue(new BigDecimal("100"));
        report.setCompletionRate(85.0);
        report.setCancellationRate(15.0);

        Map<String, Long> ordersByStatus = new HashMap<>();
        ordersByStatus.put("COMPLETED", 85L);
        ordersByStatus.put("CANCELLED", 15L);
        report.setOrdersByStatus(ordersByStatus);

        Map<String, Long> ordersByRoomType = new HashMap<>();
        ordersByRoomType.put("标准间", 60L);
        ordersByRoomType.put("豪华间", 40L);
        report.setOrdersByRoomType(ordersByRoomType);

        return report;
    }

    private RevenueReportDTO createMockRevenueReport(LocalDate startDate, LocalDate endDate) {
        RevenueReportDTO report = new RevenueReportDTO();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setTotalRevenue(new BigDecimal("15000"));
        report.setAverageDailyRate(new BigDecimal("200"));
        report.setRevenuePerAvailableRoom(new BigDecimal("150"));
        report.setOccupancyRate(75.5);
        report.setRevenueGrowthRate(10.2);

        Map<String, BigDecimal> revenueByRoomType = new HashMap<>();
        revenueByRoomType.put("标准间", new BigDecimal("9000"));
        revenueByRoomType.put("豪华间", new BigDecimal("6000"));
        report.setRevenueByRoomType(revenueByRoomType);

        return report;
    }

    private Object createMockReportOverview() {
        // 这里应该创建一个 ReportOverviewDTO 对象
        // 由于没有看到完整的 DTO 定义，这里返回一个模拟对象
        Map<String, Object> overview = new HashMap<>();
        overview.put("todayOrders", 25);
        overview.put("todayRevenue", 2500);
        overview.put("currentOccupancyRate", 85.0);
        overview.put("monthlyNewUsers", 15);
        overview.put("availableRooms", 80);
        overview.put("maintenanceRooms", 5);
        overview.put("activeUsers", 150);
        return overview;
    }
}