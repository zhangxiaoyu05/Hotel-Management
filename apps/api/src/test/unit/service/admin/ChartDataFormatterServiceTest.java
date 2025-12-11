package com.hotel.service.admin;

import com.hotel.dto.admin.chart.OrderTrendChartDTO;
import com.hotel.dto.admin.chart.RevenueChartDTO;
import com.hotel.dto.admin.chart.OccupancyChartDTO;
import com.hotel.dto.admin.chart.PieChartDTO;
import com.hotel.service.ChartDataFormatterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 图表数据格式化服务测试
 */
@ExtendWith(MockitoExtension.class)
class ChartDataFormatterServiceTest {

    @Mock
    private TrendAnalysisService trendAnalysisService;

    @Mock
    private RevenueAnalyticsService revenueAnalyticsService;

    @Mock
    private OccupancyAnalyticsService occupancyAnalyticsService;

    @InjectMocks
    private ChartDataFormatterService chartDataFormatterService;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
    }

    @Test
    void testFormatOrderTrendsChart() {
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 7);
        String period = "daily";

        // 模拟数据
        java.util.Map<String, Object> mockData = java.util.Map.of(
            "dates", java.util.List.of("2025-12-01", "2025-12-02"),
            "orderCounts", java.util.List.of(10, 15),
            "revenues", java.util.List.of(java.math.BigDecimal.valueOf(1000), java.math.BigDecimal.valueOf(1500))
        );

        when(trendAnalysisService.getOrderTrendsDaily(any(), any())).thenReturn(mockData);

        OrderTrendChartDTO result = chartDataFormatterService.formatOrderTrendsChart(startDate, endDate, period);

        assertNotNull(result);
        assertNotNull(result.getTitle());
        assertNotNull(result.getCategories());
        assertNotNull(result.getSeries());
        assertFalse(result.getSeries().isEmpty());
    }

    @Test
    void testFormatRevenueAnalysisChart() {
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 7);
        String type = "trends";

        // 模拟数据
        java.util.Map<String, Object> mockData = java.util.Map.of(
            "dates", java.util.List.of("2025-12-01", "2025-12-02"),
            "revenues", java.util.List.of(java.math.BigDecimal.valueOf(1000), java.math.BigDecimal.valueOf(1500))
        );

        when(revenueAnalyticsService.getRevenueTrendsDaily(any(), any())).thenReturn(mockData);

        RevenueChartDTO result = chartDataFormatterService.formatRevenueAnalysisChart(startDate, endDate, type);

        assertNotNull(result);
        assertNotNull(result.getTitle());
        assertNotNull(result.getCategories());
        assertNotNull(result.getSeries());
    }

    @Test
    void testFormatOccupancyChart() {
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 7);
        String type = "trends";

        // 模拟数据
        java.util.Map<String, Object> mockData = java.util.Map.of(
            "dates", java.util.List.of("2025-12-01", "2025-12-02"),
            "occupancyRates", java.util.List.of(75.5, 80.0)
        );

        when(occupancyAnalyticsService.getOccupancyTrendsDaily(any(), any())).thenReturn(mockData);

        OccupancyChartDTO result = chartDataFormatterService.formatOccupancyChart(startDate, endDate, type);

        assertNotNull(result);
        assertNotNull(result.getTitle());
        assertNotNull(result.getCategories());
        assertNotNull(result.getSeries());
    }

    @Test
    void testFormatPieChartData() {
        String dataType = "room_status";
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 7);

        PieChartDTO result = chartDataFormatterService.formatPieChartData(dataType, startDate, endDate);

        assertNotNull(result);
        assertNotNull(result.getTitle());
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty());
    }

    @Test
    void testFormatDashboardCharts() {
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 7);

        // 模拟数据
        java.util.Map<String, Object> mockOrderData = java.util.Map.of(
            "dates", java.util.List.of("2025-12-01"),
            "orderCounts", java.util.List.of(10)
        );

        java.util.Map<String, Object> mockRevenueData = java.util.Map.of(
            "dates", java.util.List.of("2025-12-01"),
            "revenues", java.util.List.of(java.math.BigDecimal.valueOf(1000))
        );

        java.util.Map<String, Object> mockOccupancyData = java.util.Map.of(
            "dates", java.util.List.of("2025-12-01"),
            "occupancyRates", java.util.List.of(75.5)
        );

        when(trendAnalysisService.getOrderTrendsDaily(any(), any())).thenReturn(mockOrderData);
        when(revenueAnalyticsService.getRevenueTrendsDaily(any(), any())).thenReturn(mockRevenueData);
        when(occupancyAnalyticsService.getOccupancyTrendsDaily(any(), any())).thenReturn(mockOccupancyData);

        var result = chartDataFormatterService.formatDashboardCharts(startDate, endDate);

        assertNotNull(result);
        assertNotNull(result.getTitle());
        assertNotNull(result.getOrderTrendChart());
        assertNotNull(result.getRevenueChart());
        assertNotNull(result.getOccupancyChart());
        assertNotNull(result.getRoomStatusPieChart());
        assertNotNull(result.getOrderStatusPieChart());
    }
}