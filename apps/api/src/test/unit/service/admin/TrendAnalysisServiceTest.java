package com.hotel.service.admin;

import com.hotel.service.TrendAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 趋势分析服务测试
 */
@ExtendWith(MockitoExtension.class)
class TrendAnalysisServiceTest {

    @Mock
    private DashboardStatisticsService dashboardStatisticsService;

    @InjectMocks
    private TrendAnalysisService trendAnalysisService;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
    }

    @Test
    void testGetOrderTrendsDaily() {
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 7);

        Map<String, Object> result = trendAnalysisService.getOrderTrendsDaily(startDate, endDate);

        assertNotNull(result);
        assertTrue(result.containsKey("dates"));
        assertTrue(result.containsKey("orderCounts"));
        assertTrue(result.containsKey("revenues"));
        assertTrue(result.containsKey("totalOrders"));
        assertTrue(result.containsKey("totalRevenue"));

        @SuppressWarnings("unchecked")
        java.util.List<String> dates = (java.util.List<String>) result.get("dates");
        assertEquals(7, dates.size());
        assertEquals("2025-12-01", dates.get(0));
    }

    @Test
    void testGetOrderTrendsWeekly() {
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);

        Map<String, Object> result = trendAnalysisService.getOrderTrendsWeekly(startDate, endDate);

        assertNotNull(result);
        assertTrue(result.containsKey("weekLabels"));
        assertTrue(result.containsKey("orderCounts"));
        assertTrue(result.containsKey("revenues"));
        assertTrue(result.containsKey("totalOrders"));
        assertTrue(result.containsKey("totalRevenue"));
    }

    @Test
    void testGetOrderTrendsMonthly() {
        int year = 2025;

        Map<String, Object> result = trendAnalysisService.getOrderTrendsMonthly(year);

        assertNotNull(result);
        assertTrue(result.containsKey("months"));
        assertTrue(result.containsKey("orderCounts"));
        assertTrue(result.containsKey("revenues"));
        assertTrue(result.containsKey("totalOrders"));
        assertTrue(result.containsKey("totalRevenue"));

        @SuppressWarnings("unchecked")
        java.util.List<String> months = (java.util.List<String>) result.get("months");
        assertEquals(12, months.size());
        assertEquals("2025-01", months.get(0));
        assertEquals("2025-12", months.get(11));
    }

    @Test
    void testGetOrderStatusTrends() {
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 7);

        Map<String, Object> result = trendAnalysisService.getOrderStatusTrends(startDate, endDate);

        assertNotNull(result);
        assertTrue(result.containsKey("statusTrends"));
        assertTrue(result.containsKey("statusDistribution"));
    }

    @Test
    void testGetHolidayOrderAnalysis() {
        int year = 2025;

        Map<String, Object> result = trendAnalysisService.getHolidayOrderAnalysis(year);

        assertNotNull(result);
        assertTrue(result.containsKey("holidayOrders"));
        assertTrue(result.containsKey("normalDayOrders"));
        assertTrue(result.containsKey("growthRates"));
    }
}