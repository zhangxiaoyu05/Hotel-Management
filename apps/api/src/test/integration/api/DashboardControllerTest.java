package com.hotel.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 仪表板控制器集成测试
 */
@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetDashboardMetrics_Success() throws Exception {
        // 准备模拟数据
        when(dashboardService.getDashboardMetrics()).thenReturn(createMockMetrics());

        // 执行请求并验证结果
        mockMvc.perform(get("/v1/admin/dashboard/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.todayOrdersCount").value(25))
                .andExpect(jsonPath("$.data.todayRevenue").value(5880.50))
                .andExpect(jsonPath("$.data.occupancyRate").value(75.5))
                .andExpect(jsonPath("$.data.totalActiveUsers").value(128))
                .andExpect(jsonPath("$.message").value("获取仪表板核心指标成功"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetRealTimeData_Success() throws Exception {
        // 准备模拟数据
        when(dashboardService.getRealTimeData()).thenReturn(createMockRealTimeData());

        // 执行请求并验证结果
        mockMvc.perform(get("/v1/admin/dashboard/realtime")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recentOrders").isArray())
                .andExpect(jsonPath("$.data.activeUsersCount").value(128))
                .andExpect(jsonPath("$.data.systemStatus").value("NORMAL"))
                .andExpect(jsonPath("$.message").value("获取实时数据成功"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetTrendData_Success() throws Exception {
        // 准备模拟数据
        when(dashboardService.getTrendData(any(), any())).thenReturn(createMockTrendData());

        // 执行请求并验证结果
        mockMvc.perform(get("/v1/admin/dashboard/trends")
                .param("period", "daily")
                .param("days", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.period").value("DAILY"))
                .andExpect(jsonPath("$.data.dates").isArray())
                .andExpect(jsonPath("$.data.orderCounts").isArray())
                .andExpect(jsonPath("$.data.revenues").isArray())
                .andExpect(jsonPath("$.message").value("获取趋势数据成功"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetTrendData_ExceedsMaxDays() throws Exception {
        // 执行请求并验证结果（超过365天）
        mockMvc.perform(get("/v1/admin/dashboard/trends")
                .param("period", "daily")
                .param("days", "400")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("查询天数不能超过365天"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetRevenueStatistics_Success() throws Exception {
        // 准备模拟数据
        when(dashboardService.getRevenueStatistics()).thenReturn(createMockRevenueStatistics());

        // 执行请求并验证结果
        mockMvc.perform(get("/v1/admin/dashboard/revenue")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.todayRevenue").value(5880.50))
                .andExpect(jsonPath("$.data.monthlyRevenue").value(156800.00))
                .andExpect(jsonPath("$.data.yearlyRevenue").value(1865000.00))
                .andExpect(jsonPath("$.message").value("获取收入统计成功"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetRoomStatusStatistics_Success() throws Exception {
        // 执行请求并验证结果
        mockMvc.perform(get("/v1/admin/dashboard/room-status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.AVAILABLE").value(30))
                .andExpect(jsonPath("$.data.OCCUPIED").value(65))
                .andExpect(jsonPath("$.data.MAINTENANCE").value(5))
                .andExpect(jsonPath("$.message").value("获取房间状态统计成功"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testRefreshDashboardCache_Success() throws Exception {
        // 执行请求并验证结果
        mockMvc.perform(post("/v1/admin/dashboard/refresh-cache")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("仪表板缓存刷新成功"))
                .andExpect(jsonPath("$.message").value("仪表板缓存刷新成功"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetSystemHealth_Success() throws Exception {
        // 执行请求并验证结果
        mockMvc.perform(get("/v1/admin/dashboard/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.database").value("UP"))
                .andExpect(jsonPath("$.data.cache").value("UP"))
                .andExpect(jsonPath("$.data.diskSpace").value("OK"))
                .andExpect(jsonPath("$.message").value("获取系统健康状态成功"));
    }

    @Test
    void testGetDashboardMetrics_WithoutAdminRole_403() throws Exception {
        // 执行请求并验证结果（非管理员用户）
        mockMvc.perform(get("/v1/admin/dashboard/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // 辅助方法创建模拟数据
    private Object createMockMetrics() {
        return new Object() {
            public final int todayOrdersCount = 25;
            public final java.math.BigDecimal todayRevenue = new java.math.BigDecimal("5880.50");
            public final double occupancyRate = 75.5;
            public final int totalActiveUsers = 128;
            public final int pendingReviewsCount = 8;
            public final double averageRating = 4.3;
            public final int totalRooms = 100;
            public final int reservedRooms = 65;
            public final int availableRooms = 30;
            public final int maintenanceRooms = 5;
            public final java.math.BigDecimal monthlyRevenue = new java.math.BigDecimal("156800.00");
            public final int monthlyOrdersCount = 120;
            public final int todayNewUsers = 12;
        };
    }

    private Object createMockRealTimeData() {
        return new Object() {
            public final Object[] recentOrders = new Object[0];
            public final java.util.Map<String, Integer> roomStatusCounts =
                java.util.Map.of("AVAILABLE", 30, "OCCUPIED", 65, "MAINTENANCE", 5);
            public final int activeUsersCount = 128;
            public final int todayNewUsersCount = 12;
            public final int onlineUsersCount = 64;
            public final String systemStatus = "NORMAL";
            public final String databaseStatus = "ACTIVE";
            public final String cacheStatus = "ACTIVE";
            public final String lastUpdateTime = java.time.LocalDateTime.now().toString();
        };
    }

    private Object createMockTrendData() {
        return new Object() {
            public final String period = "DAILY";
            public final String[] dates = {"2025-12-01", "2025-12-02", "2025-12-03"};
            public final Integer[] orderCounts = {20, 25, 30};
            public final java.math.BigDecimal[] revenues = {
                new java.math.BigDecimal("5000.00"),
                new java.math.BigDecimal("6000.00"),
                new java.math.BigDecimal("7000.00")
            };
            public final Double[] occupancies = {70.0, 75.0, 80.0};
            public final Integer[] newUsersCounts = {5, 8, 12};
            public final int totalOrders = 75;
            public final java.math.BigDecimal totalRevenue = new java.math.BigDecimal("18000.00");
            public final double averageOccupancy = 75.0;
            public final double averageDailyOrders = 25.0;
            public final java.math.BigDecimal averageDailyRevenue = new java.math.BigDecimal("6000.00");
        };
    }

    private Object createMockRevenueStatistics() {
        return new Object() {
            public final java.math.BigDecimal todayRevenue = new java.math.BigDecimal("5880.50");
            public final java.math.BigDecimal yesterdayRevenue = new java.math.BigDecimal("4250.00");
            public final java.math.BigDecimal weeklyRevenue = new java.math.BigDecimal("38200.00");
            public final java.math.BigDecimal monthlyRevenue = new java.math.BigDecimal("156800.00");
            public final java.math.BigDecimal yearlyRevenue = new java.math.BigDecimal("1865000.00");
            public final double dailyGrowthRate = 38.35;
            public final double weeklyGrowthRate = 7.30;
            public final double monthlyGrowthRate = 10.18;
            public final double yearlyGrowthRate = 13.03;
            public final java.math.BigDecimal averageOrderValue = new java.math.BigDecimal("1420.00");
            public final java.math.BigDecimal projectedMonthlyRevenue = new java.math.BigDecimal("172000.00");
            public final java.math.BigDecimal projectedYearlyRevenue = new java.math.BigDecimal("2064000.00");
        };
    }
}