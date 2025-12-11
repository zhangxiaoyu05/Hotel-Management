package com.hotel.service.admin;

import com.hotel.dto.admin.dashboard.DashboardMetricsDTO;
import com.hotel.dto.admin.dashboard.RealTimeDataDTO;
import com.hotel.dto.admin.dashboard.TrendDataDTO;
import com.hotel.dto.admin.dashboard.RevenueStatisticsDTO;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.ReviewRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;
import com.hotel.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 仪表板服务测试
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
    }

    @Test
    void testGetDashboardMetrics() {
        // 准备测试数据
        when(orderRepository.countOrdersByDate(any(LocalDate.class))).thenReturn(25);
        when(orderRepository.calculateRevenueByDate(any(LocalDate.class))).thenReturn(new BigDecimal("5880.50"));
        when(orderRepository.countOrdersByDateRange(any(LocalDate.class), any(LocalDate.class))).thenReturn(120);
        when(orderRepository.calculateRevenueByDateRange(any(LocalDate.class), any(LocalDate.class))).thenReturn(new BigDecimal("156800.00"));

        when(roomRepository.selectList(any())).thenReturn(Arrays.asList(
            createRoom(1L, "AVAILABLE"),
            createRoom(2L, "OCCUPIED"),
            createRoom(3L, "MAINTENANCE")
        ));

        when(userRepository.countActiveUsersByOrders(any(LocalDate.class))).thenReturn(128);
        when(userRepository.countUsersByDate(any(LocalDate.class))).thenReturn(12);

        when(reviewRepository.selectList(any())).thenReturn(Arrays.asList(
            createReview(5),
            createReview(4),
            createReview(5)
        ));
        when(reviewRepository.countByStatus("PENDING")).thenReturn(8);

        // 执行测试
        DashboardMetricsDTO result = dashboardService.getDashboardMetrics();

        // 验证结果
        assertNotNull(result);
        assertEquals(25, result.getTodayOrdersCount());
        assertEquals(new BigDecimal("5880.50"), result.getTodayRevenue());
        assertEquals(120, result.getMonthlyOrdersCount());
        assertEquals(new BigDecimal("156800.00"), result.getMonthlyRevenue());
        assertEquals(3, result.getTotalRooms());
        assertEquals(128, result.getTotalActiveUsers());
        assertEquals(12, result.getTodayNewUsers());
        assertEquals(8, result.getPendingReviewsCount());
        assertTrue(result.getOccupancyRate() >= 0);
        assertTrue(result.getAverageRating() >= 0);

        // 验证方法调用
        verify(orderRepository).countOrdersByDate(any(LocalDate.class));
        verify(orderRepository).calculateRevenueByDate(any(LocalDate.class));
        verify(roomRepository).selectList(any());
        verify(userRepository).countActiveUsersByOrders(any(LocalDate.class));
        verify(reviewRepository).countByStatus("PENDING");
    }

    @Test
    void testGetRealTimeData() {
        // 准备测试数据
        when(orderRepository.findRecentOrders(10)).thenReturn(Arrays.asList(
            createOrder(1L, "ORD-001", new BigDecimal("1000.00")),
            createOrder(2L, "ORD-002", new BigDecimal("1500.00"))
        ));

        when(roomRepository.selectList(any())).thenReturn(Arrays.asList(
            createRoom(1L, "AVAILABLE"),
            createRoom(2L, "OCCUPIED")
        ));

        when(orderRepository.selectList(any())).thenReturn(Arrays.asList(
            createOrder(1L, "ORD-001", new BigDecimal("1000.00")),
            createOrder(2L, "ORD-002", new BigDecimal("1500.00"))
        ));

        when(userRepository.countActiveUsersByOrders(any(LocalDate.class))).thenReturn(128);
        when(userRepository.countUsersByDate(any(LocalDate.class))).thenReturn(12);

        // 执行测试
        RealTimeDataDTO result = dashboardService.getRealTimeData();

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getRecentOrders());
        assertNotNull(result.getRoomStatusCounts());
        assertTrue(result.getRecentOrders().size() <= 10);
        assertEquals(128, result.getActiveUsersCount());
        assertEquals(12, result.getTodayNewUsersCount());
        assertNotNull(result.getOnlineUsersCount());
        assertNotNull(result.getSystemStatus());
        assertNotNull(result.getLastUpdateTime());

        // 验证方法调用
        verify(orderRepository).findRecentOrders(10);
        verify(roomRepository).selectList(any());
    }

    @Test
    void testGetTrendData() {
        // 准备测试数据
        String period = "daily";
        int days = 7;

        when(orderRepository.countOrdersByDate(any(LocalDate.class))).thenReturn(10);
        when(orderRepository.calculateRevenueByDate(any(LocalDate.class))).thenReturn(new BigDecimal("1000.00"));
        when(userRepository.countUsersByDate(any(LocalDate.class))).thenReturn(5);

        // 执行测试
        TrendDataDTO result = dashboardService.getTrendData(period, days);

        // 验证结果
        assertNotNull(result);
        assertEquals("DAILY", result.getPeriod());
        assertNotNull(result.getDates());
        assertNotNull(result.getOrderCounts());
        assertNotNull(result.getRevenues());
        assertNotNull(result.getOccupancies());
        assertNotNull(result.getNewUsersCounts());
        assertEquals(days, result.getDates().size());
        assertEquals(days, result.getOrderCounts().size());
        assertEquals(days, result.getRevenues().size());

        // 验证聚合数据
        assertTrue(result.getTotalOrders() >= 0);
        assertTrue(result.getTotalRevenue().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getAverageOccupancy() >= 0);
        assertTrue(result.getAverageDailyOrders() >= 0);
        assertTrue(result.getAverageDailyRevenue().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void testGetRevenueStatistics() {
        // 准备测试数据
        when(orderRepository.calculateRevenueByDate(any(LocalDate.class))).thenReturn(new BigDecimal("5000.00"));
        when(orderRepository.calculateRevenueByDateRange(any(LocalDate.class), any(LocalDate.class))).thenReturn(new BigDecimal("35000.00"));
        when(orderRepository.countOrdersByDateRange(any(LocalDate.class), any(LocalDate.class))).thenReturn(50);

        // 执行测试
        RevenueStatisticsDTO result = dashboardService.getRevenueStatistics();

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getTodayRevenue());
        assertNotNull(result.getWeeklyRevenue());
        assertNotNull(result.getMonthlyRevenue());
        assertNotNull(result.getYearlyRevenue());
        assertNotNull(result.getAverageOrderValue());
        assertTrue(result.getTodayRevenue().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getWeeklyRevenue().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getMonthlyRevenue().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getYearlyRevenue().compareTo(BigDecimal.ZERO) >= 0);

        // 验证增长率计算
        assertTrue(result.getDailyGrowthRate() >= -100);
        assertTrue(result.getWeeklyGrowthRate() >= -100);
        assertTrue(result.getMonthlyGrowthRate() >= -100);
        assertTrue(result.getYearlyGrowthRate() >= -100);
    }

    @Test
    void testClearDashboardCache() {
        // 执行测试
        assertDoesNotThrow(() -> dashboardService.clearDashboardCache());
    }

    // 辅助方法创建测试数据
    private com.hotel.entity.Room createRoom(Long id, String status) {
        com.hotel.entity.Room room = new com.hotel.entity.Room();
        room.setId(id);
        room.setStatus(status);
        room.setPrice(new BigDecimal("1000.00"));
        return room;
    }

    private com.hotel.entity.Order createOrder(Long id, String orderNumber, BigDecimal price) {
        com.hotel.entity.Order order = new com.hotel.entity.Order();
        order.setId(id);
        order.setOrderNumber(orderNumber);
        order.setTotalPrice(price);
        order.setStatus("CONFIRMED");
        order.setUserId(1L);
        order.setRoomId(1L);
        order.setCreatedAt(java.time.LocalDateTime.now());
        return order;
    }

    private com.hotel.entity.Review createReview(int rating) {
        com.hotel.entity.Review review = new com.hotel.entity.Review();
        review.setId(1L);
        review.setOverallRating(rating);
        review.setStatus("APPROVED");
        review.setCreatedAt(java.time.LocalDateTime.now());
        return review;
    }
}