package com.hotel.service;

import com.hotel.dto.OrderSummaryDTO;
import com.hotel.dto.admin.dashboard.*;
import com.hotel.entity.Order;
import com.hotel.entity.Review;
import com.hotel.entity.Room;
import com.hotel.entity.User;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.ReviewRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表板服务
 * 提供仪表板数据查询和统计功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 获取仪表板核心指标
     */
    @Cacheable(value = "dashboard:metrics", key = "'today'", unless = "#result == null")
    public DashboardMetricsDTO getDashboardMetrics() {
        log.info("开始获取仪表板核心指标");

        DashboardMetricsDTO metrics = new DashboardMetricsDTO();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = YearMonth.now().atDay(1);

        try {
            // 今日订单数
            int todayOrdersCount = orderRepository.countOrdersByDate(today);
            metrics.setTodayOrdersCount(todayOrdersCount);

            // 今日收入
            BigDecimal todayRevenue = orderRepository.calculateRevenueByDate(today);
            metrics.setTodayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO);

            // 本月订单数和收入
            int monthlyOrdersCount = orderRepository.countOrdersByDateRange(monthStart, today);
            BigDecimal monthlyRevenue = orderRepository.calculateRevenueByDateRange(monthStart, today);
            metrics.setMonthlyOrdersCount(monthlyOrdersCount);
            metrics.setMonthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO);

            // 房间统计
            List<Room> allRooms = roomRepository.selectList(null);
            int totalRooms = allRooms.size();
            long availableRooms = allRooms.stream()
                .filter(room -> "AVAILABLE".equals(room.getStatus()))
                .count();
            long reservedRooms = allRooms.stream()
                .filter(room -> "OCCUPIED".equals(room.getStatus()))
                .count();
            long maintenanceRooms = allRooms.stream()
                .filter(room -> "MAINTENANCE".equals(room.getStatus()))
                .count();

            metrics.setTotalRooms(totalRooms);
            metrics.setAvailableRooms((int) availableRooms);
            metrics.setReservedRooms((int) reservedRooms);
            metrics.setMaintenanceRooms((int) maintenanceRooms);

            // 入住率计算
            double occupancyRate = totalRooms > 0 ? (double) reservedRooms / totalRooms * 100 : 0.0;
            metrics.setOccupancyRate(roundToTwoDecimal(occupancyRate));

            // 活跃用户统计（近7天有订单的用户）
            LocalDate sevenDaysAgo = today.minusDays(7);
            int activeUsersCount = userRepository.countActiveUsersByOrders(sevenDaysAgo);
            metrics.setTotalActiveUsers(activeUsersCount);

            // 今日新用户数
            int todayNewUsers = userRepository.countUsersByDate(today);
            metrics.setTodayNewUsers(todayNewUsers);

            // 评价统计
            List<Review> approvedReviews = reviewRepository.selectList(
                reviewRepository.getQueryWrapper().eq("status", "APPROVED")
            );

            // 平均评分
            double averageRating = approvedReviews.stream()
                .filter(review -> review.getOverallRating() != null)
                .mapToInt(Review::getOverallRating)
                .average()
                .orElse(0.0);
            metrics.setAverageRating(roundToTwoDecimal(averageRating));

            // 待审核评价数
            int pendingReviewsCount = reviewRepository.countByStatus("PENDING");
            metrics.setPendingReviewsCount(pendingReviewsCount);

            log.info("仪表板核心指标获取完成");
            return metrics;

        } catch (Exception e) {
            log.error("获取仪表板核心指标失败", e);
            throw new RuntimeException("获取仪表板核心指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时数据
     */
    @Cacheable(value = "dashboard:realtime", key = "'current'", unless = "#result == null")
    public RealTimeDataDTO getRealTimeData() {
        log.info("开始获取实时数据");

        RealTimeDataDTO realTimeData = new RealTimeDataDTO();
        LocalDate today = LocalDate.now();

        try {
            // 最新订单列表（最近10条）
            List<Order> recentOrders = orderRepository.findRecentOrders(10);
            List<OrderSummaryDTO> orderSummaries = recentOrders.stream()
                .map(this::convertToOrderSummary)
                .collect(Collectors.toList());
            realTimeData.setRecentOrders(orderSummaries);

            // 房间状态统计
            List<Room> allRooms = roomRepository.selectList(null);
            Map<String, Integer> roomStatusCounts = allRooms.stream()
                .collect(Collectors.groupingBy(
                    Room::getStatus,
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
            realTimeData.setRoomStatusCounts(roomStatusCounts);

            // 订单状态统计
            List<Order> allOrders = orderRepository.selectList(null);
            int pendingOrdersCount = (int) allOrders.stream()
                .filter(order -> "PENDING".equals(order.getStatus()))
                .count();
            int activeOrdersCount = (int) allOrders.stream()
                .filter(order -> Arrays.asList("CONFIRMED", "COMPLETED").contains(order.getStatus()))
                .count();

            // 待入住和待退房统计
            LocalDate tomorrow = today.plusDays(1);
            int pendingCheckInCount = orderRepository.countOrdersByCheckInDate(today);
            int pendingCheckOutCount = orderRepository.countOrdersByCheckOutDate(today);

            realTimeData.setPendingOrdersCount(pendingOrdersCount);
            realTimeData.setActiveOrdersCount(activeOrdersCount);
            realTimeData.setPendingCheckInCount(pendingCheckInCount);
            realTimeData.setPendingCheckOutCount(pendingCheckOutCount);

            // 用户统计
            LocalDate sevenDaysAgo = today.minusDays(7);
            int activeUsersCount = userRepository.countActiveUsersByOrders(sevenDaysAgo);
            int todayNewUsersCount = userRepository.countUsersByDate(today);

            realTimeData.setActiveUsersCount(activeUsersCount);
            realTimeData.setTodayNewUsersCount(todayNewUsersCount);
            realTimeData.setOnlineUsersCount(activeUsersCount / 2); // 假设一半活跃用户在线

            // 系统状态
            realTimeData.setSystemStatus("NORMAL");
            realTimeData.setDatabaseStatus("ACTIVE");
            realTimeData.setCacheStatus("ACTIVE");
            realTimeData.setLastUpdateTime(LocalDateTime.now());

            log.info("实时数据获取完成");
            return realTimeData;

        } catch (Exception e) {
            log.error("获取实时数据失败", e);
            throw new RuntimeException("获取实时数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取趋势数据
     */
    @Cacheable(value = "dashboard:trends", key = "#period + '_' + #days", unless = "#result == null")
    public TrendDataDTO getTrendData(String period, int days) {
        log.info("开始获取{}趋势数据，最近{}天", period, days);

        TrendDataDTO trendData = new TrendDataDTO();
        trendData.setPeriod(period.toUpperCase());

        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);

            List<String> dates = new ArrayList<>();
            List<Integer> orderCounts = new ArrayList<>();
            List<BigDecimal> revenues = new ArrayList<>();
            List<Double> occupancies = new ArrayList<>();
            List<Integer> newUsersCounts = new ArrayList<>();

            int totalOrders = 0;
            BigDecimal totalRevenue = BigDecimal.ZERO;
            double totalOccupancy = 0.0;

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                dates.add(date.format(DateTimeFormatter.ISO_DATE));

                // 订单数量
                int dailyOrderCount = orderRepository.countOrdersByDate(date);
                orderCounts.add(dailyOrderCount);
                totalOrders += dailyOrderCount;

                // 收入
                BigDecimal dailyRevenue = orderRepository.calculateRevenueByDate(date);
                revenues.add(dailyRevenue != null ? dailyRevenue : BigDecimal.ZERO);
                if (dailyRevenue != null) {
                    totalRevenue = totalRevenue.add(dailyRevenue);
                }

                // 入住率
                double dailyOccupancy = calculateOccupancyRate(date);
                occupancies.add(dailyOccupancy);
                totalOccupancy += dailyOccupancy;

                // 新用户数
                int dailyNewUsers = userRepository.countUsersByDate(date);
                newUsersCounts.add(dailyNewUsers);
            }

            // 计算平均值
            double averageOccupancy = days > 0 ? totalOccupancy / days : 0.0;
            double averageDailyOrders = days > 0 ? (double) totalOrders / days : 0.0;
            BigDecimal averageDailyRevenue = days > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            // 设置数据
            trendData.setDates(dates);
            trendData.setOrderCounts(orderCounts);
            trendData.setRevenues(revenues);
            trendData.setOccupancies(occupancies);
            trendData.setNewUsersCounts(newUsersCounts);

            trendData.setTotalOrders(totalOrders);
            trendData.setTotalRevenue(totalRevenue);
            trendData.setAverageOccupancy(roundToTwoDecimal(averageOccupancy));
            trendData.setAverageDailyOrders(roundToTwoDecimal(averageDailyOrders));
            trendData.setAverageDailyRevenue(averageDailyRevenue);

            // TODO: 计算同比增长率
            trendData.setOrdersGrowthRate(0.0);
            trendData.setRevenueGrowthRate(0.0);
            trendData.setOccupancyGrowthRate(0.0);

            log.info("趋势数据获取完成");
            return trendData;

        } catch (Exception e) {
            log.error("获取趋势数据失败", e);
            throw new RuntimeException("获取趋势数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取收入统计
     */
    @Cacheable(value = "dashboard:revenue", key = "'statistics'", unless = "#result == null")
    public RevenueStatisticsDTO getRevenueStatistics() {
        log.info("开始获取收入统计");

        RevenueStatisticsDTO revenueStats = new RevenueStatisticsDTO();
        LocalDate today = LocalDate.now();

        try {
            // 各时间段收入
            BigDecimal todayRevenue = orderRepository.calculateRevenueByDate(today);
            LocalDate yesterday = today.minusDays(1);
            BigDecimal yesterdayRevenue = orderRepository.calculateRevenueByDate(yesterday);

            LocalDate weekStart = today.minusDays(6);
            BigDecimal weeklyRevenue = orderRepository.calculateRevenueByDateRange(weekStart, today);
            LocalDate lastWeekStart = weekStart.minusDays(7);
            BigDecimal lastWeekRevenue = orderRepository.calculateRevenueByDateRange(lastWeekStart, weekStart.minusDays(1));

            LocalDate monthStart = YearMonth.now().atDay(1);
            BigDecimal monthlyRevenue = orderRepository.calculateRevenueByDateRange(monthStart, today);
            LocalDate lastMonthStart = monthStart.minusMonths(1);
            LocalDate lastMonthEnd = monthStart.minusDays(1);
            BigDecimal lastMonthRevenue = orderRepository.calculateRevenueByDateRange(lastMonthStart, lastMonthEnd);

            LocalDate yearStart = today.withDayOfYear(1);
            BigDecimal yearlyRevenue = orderRepository.calculateRevenueByDateRange(yearStart, today);
            LocalDate lastYearStart = yearStart.minusYears(1);
            LocalDate lastYearEnd = yearStart.minusDays(1);
            BigDecimal lastYearRevenue = orderRepository.calculateRevenueByDateRange(lastYearStart, lastYearEnd);

            // 设置收入数据
            revenueStats.setTodayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO);
            revenueStats.setYesterdayRevenue(yesterdayRevenue != null ? yesterdayRevenue : BigDecimal.ZERO);
            revenueStats.setWeeklyRevenue(weeklyRevenue != null ? weeklyRevenue : BigDecimal.ZERO);
            revenueStats.setLastWeekRevenue(lastWeekRevenue != null ? lastWeekRevenue : BigDecimal.ZERO);
            revenueStats.setMonthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO);
            revenueStats.setLastMonthRevenue(lastMonthRevenue != null ? lastMonthRevenue : BigDecimal.ZERO);
            revenueStats.setYearlyRevenue(yearlyRevenue != null ? yearlyRevenue : BigDecimal.ZERO);
            revenueStats.setLastYearRevenue(lastYearRevenue != null ? lastYearRevenue : BigDecimal.ZERO);

            revenueStats.setTotalRevenue(yearlyRevenue != null ? yearlyRevenue : BigDecimal.ZERO);

            // 计算增长率
            revenueStats.setDailyGrowthRate(calculateGrowthRate(todayRevenue, yesterdayRevenue));
            revenueStats.setWeeklyGrowthRate(calculateGrowthRate(weeklyRevenue, lastWeekRevenue));
            revenueStats.setMonthlyGrowthRate(calculateGrowthRate(monthlyRevenue, lastMonthRevenue));
            revenueStats.setYearlyGrowthRate(calculateGrowthRate(yearlyRevenue, lastYearRevenue));

            // 平均订单金额
            int totalOrders = orderRepository.countOrdersByDateRange(yearStart, today);
            BigDecimal averageOrderValue = totalOrders > 0 && yearlyRevenue != null ?
                yearlyRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            revenueStats.setAverageOrderValue(averageOrderValue);

            // 预测收入
            if (monthlyRevenue != null) {
                BigDecimal projectedMonthlyRevenue = monthlyRevenue.multiply(
                    BigDecimal.valueOf(YearMonth.now().lengthOfMonth() / (double) today.getDayOfMonth())
                ).setScale(2, RoundingMode.HALF_UP);
                revenueStats.setProjectedMonthlyRevenue(projectedMonthlyRevenue);

                BigDecimal projectedYearlyRevenue = monthlyRevenue.multiply(BigDecimal.valueOf(12));
                revenueStats.setProjectedYearlyRevenue(projectedYearlyRevenue.setScale(2, RoundingMode.HALF_UP));
            }

            // TODO: 按房型、支付方式等维度统计收入
            revenueStats.setRevenueByRoomType(new HashMap<>());
            revenueStats.setRevenueByPaymentMethod(new HashMap<>());
            revenueStats.setRevenueBySource(new HashMap<>());

            log.info("收入统计获取完成");
            return revenueStats;

        } catch (Exception e) {
            log.error("获取收入统计失败", e);
            throw new RuntimeException("获取收入统计失败: " + e.getMessage());
        }
    }

    /**
     * 清除仪表板缓存
     */
    @CacheEvict(value = {"dashboard:metrics", "dashboard:realtime", "dashboard:trends", "dashboard:revenue"}, allEntries = true)
    public void clearDashboardCache() {
        log.info("清除仪表板缓存完成");
    }

    /**
     * 转换订单为摘要DTO
     */
    private OrderSummaryDTO convertToOrderSummary(Order order) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUserId());
        dto.setRoomId(order.getRoomId());
        dto.setCheckInDate(order.getCheckInDate().format(DateTimeFormatter.ISO_DATE));
        dto.setCheckOutDate(order.getCheckOutDate().format(DateTimeFormatter.ISO_DATE));
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus());
        dto.setGuestCount(order.getGuestCount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // TODO: 关联查询用户、房间、酒店信息
        dto.setUsername("用户" + order.getUserId());
        dto.setRoomNumber("房间" + order.getRoomId());
        dto.setRoomTypeName("标准房");
        dto.setHotelName("酒店");
        dto.setHotelId(1L);

        return dto;
    }

    /**
     * 计算指定日期的入住率
     */
    private double calculateOccupancyRate(LocalDate date) {
        try {
            List<Room> allRooms = roomRepository.selectList(null);
            if (allRooms.isEmpty()) return 0.0;

            long occupiedRooms = orderRepository.findConflictingOrders(
                null, date, date, Arrays.asList("CONFIRMED", "COMPLETED")
            ).stream().map(Order::getRoomId).distinct().count();

            return (double) occupiedRooms / allRooms.size() * 100;
        } catch (Exception e) {
            log.warn("计算入住率失败，日期: {}", date, e);
            return 0.0;
        }
    }

    /**
     * 计算增长率
     */
    private double calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        if (current == null) {
            current = BigDecimal.ZERO;
        }

        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();
    }

    /**
     * 保留两位小数
     */
    private double roundToTwoDecimal(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}