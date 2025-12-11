package com.hotel.service;

import com.hotel.dto.admin.dashboard.TrendDataDTO;
import com.hotel.entity.Order;
import com.hotel.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 趋势分析服务
 * 提供订单、收入、入住率等趋势数据分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrendAnalysisService {

    private final OrderRepository orderRepository;
    private final DashboardStatisticsService dashboardStatisticsService;

    /**
     * 获取订单趋势数据（按日统计）
     */
    @Cacheable(value = "trends:orders:daily", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public Map<String, Object> getOrderTrendsDaily(LocalDate startDate, LocalDate endDate) {
        log.info("获取订单趋势数据（按日），时间段: {} 至 {}", startDate, endDate);

        try {
            List<String> dates = new ArrayList<>();
            List<Integer> orderCounts = new ArrayList<>();
            List<BigDecimal> revenues = new ArrayList<>();
            List<Integer> pendingOrders = new ArrayList<>();
            List<Integer> completedOrders = new ArrayList<>();
            List<Integer> cancelledOrders = new ArrayList<>();

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                dates.add(currentDate.format(DateTimeFormatter.ISO_DATE));

                // 当日订单总数
                int totalOrders = orderRepository.countOrdersByDate(currentDate);
                orderCounts.add(totalOrders);

                // 当日收入
                BigDecimal dailyRevenue = orderRepository.calculateRevenueByDate(currentDate);
                revenues.add(dailyRevenue != null ? dailyRevenue : BigDecimal.ZERO);

                // 按状态统计订单
                int pendingCount = countOrdersByStatusAndDate(currentDate, "PENDING");
                int completedCount = countOrdersByStatusAndDate(currentDate, "COMPLETED");
                int cancelledCount = countOrdersByStatusAndDate(currentDate, "CANCELLED");

                pendingOrders.add(pendingCount);
                completedOrders.add(completedCount);
                cancelledOrders.add(cancelledCount);

                currentDate = currentDate.plusDays(1);
            }

            // 计算趋势指标
            Map<String, Object> trends = new HashMap<>();
            trends.put("dates", dates);
            trends.put("orderCounts", orderCounts);
            trends.put("revenues", revenues);
            trends.put("pendingOrders", pendingOrders);
            trends.put("completedOrders", completedOrders);
            trends.put("cancelledOrders", cancelledOrders);
            trends.put("totalOrders", orderCounts.stream().mapToInt(Integer::intValue).sum());
            trends.put("totalRevenue", revenues.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            trends.put("averageDailyOrders", calculateAverage(orderCounts));
            trends.put("averageDailyRevenue", calculateAverageDecimal(revenues));

            log.info("订单趋势数据（按日）获取完成");
            return trends;

        } catch (Exception e) {
            log.error("获取订单趋势数据（按日）失败", e);
            throw new RuntimeException("获取订单趋势数据（按日）失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单趋势数据（按周统计）
     */
    @Cacheable(value = "trends:orders:weekly", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public Map<String, Object> getOrderTrendsWeekly(LocalDate startDate, LocalDate endDate) {
        log.info("获取订单趋势数据（按周），时间段: {} 至 {}", startDate, endDate);

        try {
            Map<String, Object> trends = new HashMap<>();
            List<String> weekLabels = new ArrayList<>();
            List<Integer> orderCounts = new ArrayList<>();
            List<BigDecimal> revenues = new ArrayList<>();

            // 按周分组统计
            LocalDate currentWeekStart = startDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

            while (currentWeekStart.isBefore(endDate)) {
                LocalDate currentWeekEnd = currentWeekStart.plusDays(6);
                if (currentWeekEnd.isAfter(endDate)) {
                    currentWeekEnd = endDate;
                }

                String weekLabel = String.format("%s~%s",
                    currentWeekStart.format(DateTimeFormatter.ofPattern("MM/dd")),
                    currentWeekEnd.format(DateTimeFormatter.ofPattern("MM/dd")));
                weekLabels.add(weekLabel);

                // 周订单统计
                int weekOrders = orderRepository.countOrdersByDateRange(currentWeekStart, currentWeekEnd);
                orderCounts.add(weekOrders);

                // 周收入统计
                BigDecimal weekRevenue = orderRepository.calculateRevenueByDateRange(currentWeekStart, currentWeekEnd);
                revenues.add(weekRevenue != null ? weekRevenue : BigDecimal.ZERO);

                currentWeekStart = currentWeekStart.plusWeeks(1);
            }

            trends.put("weekLabels", weekLabels);
            trends.put("orderCounts", orderCounts);
            trends.put("revenues", revenues);
            trends.put("totalOrders", orderCounts.stream().mapToInt(Integer::intValue).sum());
            trends.put("totalRevenue", revenues.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            trends.put("averageWeeklyOrders", calculateAverage(orderCounts));
            trends.put("averageWeeklyRevenue", calculateAverageDecimal(revenues));

            log.info("订单趋势数据（按周）获取完成");
            return trends;

        } catch (Exception e) {
            log.error("获取订单趋势数据（按周）失败", e);
            throw new RuntimeException("获取订单趋势数据（按周）失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单趋势数据（按月统计）
     */
    @Cacheable(value = "trends:orders:monthly", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public Map<String, Object> getOrderTrendsMonthly(LocalDate startDate, LocalDate endDate) {
        log.info("获取订单趋势数据（按月），时间段: {} 至 {}", startDate, endDate);

        try {
            Map<String, Object> trends = new HashMap<>();
            List<String> monthLabels = new ArrayList<>();
            List<Integer> orderCounts = new ArrayList<>();
            List<BigDecimal> revenues = new ArrayList<>();

            // 按月分组统计
            YearMonth currentMonth = YearMonth.from(startDate);
            YearMonth endMonth = YearMonth.from(endDate);

            while (!currentMonth.isAfter(endMonth)) {
                String monthLabel = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                monthLabels.add(monthLabel);

                LocalDate monthStart = currentMonth.atDay(1);
                LocalDate monthEnd = currentMonth.atEndOfMonth();
                if (monthEnd.isAfter(endDate)) {
                    monthEnd = endDate;
                }

                // 月订单统计
                int monthOrders = orderRepository.countOrdersByDateRange(monthStart, monthEnd);
                orderCounts.add(monthOrders);

                // 月收入统计
                BigDecimal monthRevenue = orderRepository.calculateRevenueByDateRange(monthStart, monthEnd);
                revenues.add(monthRevenue != null ? monthRevenue : BigDecimal.ZERO);

                currentMonth = currentMonth.plusMonths(1);
            }

            trends.put("monthLabels", monthLabels);
            trends.put("orderCounts", orderCounts);
            trends.put("revenues", revenues);
            trends.put("totalOrders", orderCounts.stream().mapToInt(Integer::intValue).sum());
            trends.put("totalRevenue", revenues.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            trends.put("averageMonthlyOrders", calculateAverage(orderCounts));
            trends.put("averageMonthlyRevenue", calculateAverageDecimal(revenues));

            log.info("订单趋势数据（按月）获取完成");
            return trends;

        } catch (Exception e) {
            log.error("获取订单趋势数据（按月）失败", e);
            throw new RuntimeException("获取订单趋势数据（按月）失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单状态分布趋势
     */
    @Cacheable(value = "trends:orders:status", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public Map<String, Object> getOrderStatusTrends(LocalDate startDate, LocalDate endDate) {
        log.info("获取订单状态分布趋势，时间段: {} 至 {}", startDate, endDate);

        try {
            Map<String, Object> trends = new HashMap<>();
            Map<String, List<Integer>> statusTrends = new HashMap<>();

            // 初始化各状态的数组
            String[] statuses = {"PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"};
            for (String status : statuses) {
                statusTrends.put(status, new ArrayList<>());
            }

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                for (String status : statuses) {
                    int count = countOrdersByStatusAndDate(currentDate, status);
                    statusTrends.get(status).add(count);
                }
                currentDate = currentDate.plusDays(1);
            }

            trends.put("statusTrends", statusTrends);
            trends.put("statusDistribution", calculateStatusDistribution(startDate, endDate));

            log.info("订单状态分布趋势获取完成");
            return trends;

        } catch (Exception e) {
            log.error("获取订单状态分布趋势失败", e);
            throw new RuntimeException("获取订单状态分布趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取节假日订单对比分析
     */
    @Cacheable(value = "trends:orders:holidays", key = "#year", unless = "#result == null")
    public Map<String, Object> getHolidayOrderAnalysis(int year) {
        log.info("获取节假日订单对比分析，年份: {}", year);

        try {
            Map<String, Object> analysis = new HashMap<>();
            Map<String, Integer> holidayOrders = new HashMap<>();
            Map<String, Integer> normalDayOrders = new HashMap<>();

            // 主要节假日（示例数据，实际应该从配置或外部API获取）
            Map<String, LocalDate[]> holidays = getMajorHolidays(year);

            for (Map.Entry<String, LocalDate[]> entry : holidays.entrySet()) {
                String holidayName = entry.getKey();
                LocalDate[] dates = entry.getValue();

                // 节假日前后的订单对比
                LocalDate holidayStart = dates[0];
                LocalDate holidayEnd = dates[1];

                int holidayOrdersCount = orderRepository.countOrdersByDateRange(holidayStart, holidayEnd);

                // 节假日前一周的订单数
                LocalDate weekBeforeStart = holidayStart.minusDays(7);
                LocalDate weekBeforeEnd = holidayStart.minusDays(1);
                int normalWeekOrdersCount = orderRepository.countOrdersByDateRange(weekBeforeStart, weekBeforeEnd);

                holidayOrders.put(holidayName, holidayOrdersCount);
                normalDayOrders.put(holidayName, normalWeekOrdersCount);
            }

            analysis.put("holidayOrders", holidayOrders);
            analysis.put("normalDayOrders", normalDayOrders);
            analysis.put("growthRates", calculateGrowthRates(holidayOrders, normalDayOrders));

            log.info("节假日订单对比分析完成");
            return analysis;

        } catch (Exception e) {
            log.error("获取节假日订单对比分析失败", e);
            throw new RuntimeException("获取节假日订单对比分析失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 统计指定日期和状态的订单数量
     */
    private int countOrdersByStatusAndDate(LocalDate date, String status) {
        try {
            // 这里应该使用专门的SQL查询
            // 为了简化，使用现有方法进行近似计算
            List<Order> orders = orderRepository.selectList(null);
            return (int) orders.stream()
                .filter(order -> status.equals(order.getStatus())
                    && order.getCreatedAt() != null
                    && order.getCreatedAt().toLocalDate().equals(date))
                .count();
        } catch (Exception e) {
            log.warn("统计订单状态失败，日期: {}, 状态: {}", date, status, e);
            return 0;
        }
    }

    /**
     * 计算平均值
     */
    private double calculateAverage(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    /**
     * 计算BigDecimal平均值
     */
    private BigDecimal calculateAverageDecimal(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算状态分布
     */
    private Map<String, Integer> calculateStatusDistribution(LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> distribution = new HashMap<>();
        String[] statuses = {"PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"};

        for (String status : statuses) {
            int count = 0;
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                count += countOrdersByStatusAndDate(currentDate, status);
                currentDate = currentDate.plusDays(1);
            }
            distribution.put(status, count);
        }

        return distribution;
    }

    /**
     * 获取主要节假日
     */
    private Map<String, LocalDate[]> getMajorHolidays(int year) {
        Map<String, LocalDate[]> holidays = new HashMap<>();

        // 这里使用示例日期，实际应该从配置或API获取
        holidays.put("春节", new LocalDate[]{
            LocalDate.of(year, 2, 10),
            LocalDate.of(year, 2, 17)
        });
        holidays.put("国庆", new LocalDate[]{
            LocalDate.of(year, 10, 1),
            LocalDate.of(year, 10, 7)
        });
        holidays.put("五一", new LocalDate[]{
            LocalDate.of(year, 5, 1),
            LocalDate.of(year, 5, 3)
        });

        return holidays;
    }

    /**
     * 计算增长率
     */
    private Map<String, Double> calculateGrowthRates(Map<String, Integer> holidayOrders, Map<String, Integer> normalDayOrders) {
        Map<String, Double> growthRates = new HashMap<>();

        for (String holiday : holidayOrders.keySet()) {
            int holidayCount = holidayOrders.get(holiday);
            int normalCount = normalDayOrders.get(holiday);

            if (normalCount == 0) {
                growthRates.put(holiday, holidayCount > 0 ? 100.0 : 0.0);
            } else {
                double growthRate = ((double) (holidayCount - normalCount) / normalCount) * 100;
                growthRates.put(holiday, Math.round(growthRate * 100.0) / 100.0);
            }
        }

        return growthRates;
    }
}