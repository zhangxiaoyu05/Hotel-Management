package com.hotel.service;

import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收入分析服务
 * 提供详细的收入统计、分析和预测功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueAnalyticsService {

    private final OrderRepository orderRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    /**
     * 获取收入趋势分析（按日）
     */
    @Cacheable(value = "revenue:trends:daily", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public Map<String, Object> getRevenueTrendsDaily(LocalDate startDate, LocalDate endDate) {
        log.info("获取收入趋势分析（按日），时间段: {} 至 {}", startDate, endDate);

        try {
            List<String> dates = new ArrayList<>();
            List<BigDecimal> revenues = new ArrayList<>();
            List<Integer> orderCounts = new ArrayList<>();
            List<BigDecimal> averageOrderValues = new ArrayList<>();

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                dates.add(currentDate.format(DateTimeFormatter.ISO_DATE));

                // 当日收入
                BigDecimal dailyRevenue = orderRepository.calculateRevenueByDate(currentDate);
                revenues.add(dailyRevenue != null ? dailyRevenue : BigDecimal.ZERO);

                // 当日订单数
                int dailyOrders = orderRepository.countOrdersByDate(currentDate);
                orderCounts.add(dailyOrders);

                // 平均订单价值
                BigDecimal avgOrderValue = dailyOrders > 0 && dailyRevenue != null ?
                    dailyRevenue.divide(BigDecimal.valueOf(dailyOrders), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                averageOrderValues.add(avgOrderValue);

                currentDate = currentDate.plusDays(1);
            }

            Map<String, Object> trends = new HashMap<>();
            trends.put("dates", dates);
            trends.put("revenues", revenues);
            trends.put("orderCounts", orderCounts);
            trends.put("averageOrderValues", averageOrderValues);
            trends.put("totalRevenue", revenues.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            trends.put("totalOrders", orderCounts.stream().mapToInt(Integer::intValue).sum());
            trends.put("averageDailyRevenue", calculateAverageDecimal(revenues));
            trends.put("averageOrderValue", calculateAverageDecimal(averageOrderValues));

            log.info("收入趋势分析（按日）获取完成");
            return trends;

        } catch (Exception e) {
            log.error("获取收入趋势分析（按日）失败", e);
            throw new RuntimeException("获取收入趋势分析（按日）失败: " + e.getMessage());
        }
    }

    /**
     * 获取收入趋势分析（按月）
     */
    @Cacheable(value = "revenue:trends:monthly", key = "#year", unless = "#result == null")
    public Map<String, Object> getRevenueTrendsMonthly(int year) {
        log.info("获取收入趋势分析（按月），年份: {}", year);

        try {
            List<String> months = new ArrayList<>();
            List<BigDecimal> revenues = new ArrayList<>();
            List<Integer> orderCounts = new ArrayList<>();
            List<Double> growthRates = new ArrayList<>();

            BigDecimal previousMonthRevenue = null;

            for (int month = 1; month <= 12; month++) {
                String monthLabel = String.format("%d-%02d", year, month);
                months.add(monthLabel);

                LocalDate monthStart = LocalDate.of(year, month, 1);
                LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());

                // 月收入统计
                BigDecimal monthRevenue = orderRepository.calculateRevenueByDateRange(monthStart, monthEnd);
                revenues.add(monthRevenue != null ? monthRevenue : BigDecimal.ZERO);

                // 月订单统计
                int monthOrders = orderRepository.countOrdersByDateRange(monthStart, monthEnd);
                orderCounts.add(monthOrders);

                // 计算环比增长率
                if (previousMonthRevenue != null && previousMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    double growthRate = monthRevenue != null ?
                        monthRevenue.subtract(previousMonthRevenue)
                            .divide(previousMonthRevenue, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue() : 0.0;
                    growthRates.add(Math.round(growthRate * 100.0) / 100.0);
                } else {
                    growthRates.add(0.0);
                }

                previousMonthRevenue = monthRevenue != null ? monthRevenue : BigDecimal.ZERO;
            }

            Map<String, Object> trends = new HashMap<>();
            trends.put("months", months);
            trends.put("revenues", revenues);
            trends.put("orderCounts", orderCounts);
            trends.put("growthRates", growthRates);
            trends.put("totalRevenue", revenues.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            trends.put("totalOrders", orderCounts.stream().mapToInt(Integer::intValue).sum());
            trends.put("averageMonthlyRevenue", calculateAverageDecimal(revenues));

            log.info("收入趋势分析（按月）获取完成");
            return trends;

        } catch (Exception e) {
            log.error("获取收入趋势分析（按月）失败", e);
            throw new RuntimeException("获取收入趋势分析（按月）失败: " + e.getMessage());
        }
    }

    /**
     * 按房型收入分析
     */
    @Cacheable(value = "revenue:by-room-type", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public Map<String, Object> getRevenueByRoomType(LocalDate startDate, LocalDate endDate) {
        log.info("获取按房型收入分析，时间段: {} 至 {}", startDate, endDate);

        try {
            // 获取所有房型
            List<RoomType> roomTypes = roomTypeRepository.selectList(null);
            Map<String, BigDecimal> revenuesByRoomType = new HashMap<>();
            Map<String, Integer> orderCountsByRoomType = new HashMap<>();
            Map<String, BigDecimal> averageOrderValuesByRoomType = new HashMap<>();

            for (RoomType roomType : roomTypes) {
                // 查询该房型的订单
                List<Order> roomTypeOrders = getOrdersByRoomType(roomType.getId(), startDate, endDate);

                BigDecimal totalRevenue = roomTypeOrders.stream()
                    .filter(order -> order.getTotalPrice() != null)
                    .map(Order::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                int orderCount = roomTypeOrders.size();
                BigDecimal avgOrderValue = orderCount > 0 ?
                    totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                revenuesByRoomType.put(roomType.getName(), totalRevenue);
                orderCountsByRoomType.put(roomType.getName(), orderCount);
                averageOrderValuesByRoomType.put(roomType.getName(), avgOrderValue);
            }

            // 按收入排序
            List<Map.Entry<String, BigDecimal>> sortedRevenues = revenuesByRoomType.entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());

            Map<String, Object> analysis = new HashMap<>();
            analysis.put("revenuesByRoomType", revenuesByRoomType);
            analysis.put("orderCountsByRoomType", orderCountsByRoomType);
            analysis.put("averageOrderValuesByRoomType", averageOrderValuesByRoomType);
            analysis.put("sortedRoomTypes", sortedRevenues.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()));
            analysis.put("sortedRevenues", sortedRevenues.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()));

            log.info("按房型收入分析获取完成");
            return analysis;

        } catch (Exception e) {
            log.error("获取按房型收入分析失败", e);
            throw new RuntimeException("获取按房型收入分析失败: " + e.getMessage());
        }
    }

    /**
     * 收入预测分析
     */
    @Cacheable(value = "revenue:forecast", key = "#days", unless = "#result == null")
    public Map<String, Object> getRevenueForecast(int days) {
        log.info("获取收入预测分析，预测天数: {}", days);

        try {
            LocalDate today = LocalDate.now();
            LocalDate historicalStart = today.minusDays(90); // 使用过去90天作为历史数据
            LocalDate historicalEnd = today.minusDays(1);

            // 获取历史收入数据
            Map<String, Object> historicalData = getRevenueTrendsDaily(historicalStart, historicalEnd);
            @SuppressWarnings("unchecked")
            List<BigDecimal> historicalRevenues = (List<BigDecimal>) historicalData.get("revenues");

            if (historicalRevenues.size() < 30) {
                throw new RuntimeException("历史数据不足，无法进行预测");
            }

            // 简单的线性预测（基于最近30天的平均值和增长率）
            List<BigDecimal> recentRevenues = historicalRevenues.subList(
                Math.max(0, historicalRevenues.size() - 30), historicalRevenues.size());

            BigDecimal recentAverage = calculateAverageDecimal(recentRevenues);
            BigDecimal recentGrowth = calculateGrowthRate(recentRevenues);

            // 生成预测数据
            List<String> forecastDates = new ArrayList<>();
            List<BigDecimal> forecastRevenues = new ArrayList<>();
            List<BigDecimal> confidenceIntervals = new ArrayList<>();

            BigDecimal baseRevenue = recentAverage;
            for (int i = 1; i <= days; i++) {
                LocalDate forecastDate = today.plusDays(i);
                forecastDates.add(forecastDate.format(DateTimeFormatter.ISO_DATE));

                // 考虑增长率的预测
                BigDecimal forecastRevenue = baseRevenue.multiply(
                    BigDecimal.ONE.add(recentGrowth.multiply(BigDecimal.valueOf(i))));

                // 周末调整（假设周末收入增加20%）
                if (isWeekend(forecastDate)) {
                    forecastRevenue = forecastRevenue.multiply(BigDecimal.valueOf(1.2));
                }

                forecastRevenues.add(forecastRevenue);

                // 置信区间（简单计算，假设误差为±20%）
                BigDecimal confidenceInterval = forecastRevenue.multiply(BigDecimal.valueOf(0.2));
                confidenceIntervals.add(confidenceInterval);
            }

            BigDecimal totalForecastRevenue = forecastRevenues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> forecast = new HashMap<>();
            forecast.put("forecastDates", forecastDates);
            forecast.put("forecastRevenues", forecastRevenues);
            forecast.put("confidenceIntervals", confidenceIntervals);
            forecast.put("totalForecastRevenue", totalForecastRevenue);
            forecast.put("averageDailyForecast", totalForecastRevenue.divide(
                BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP));
            forecast.put("historicalAverage", recentAverage);
            forecast.put("predictedGrowthRate", recentGrowth.multiply(BigDecimal.valueOf(100)));

            log.info("收入预测分析获取完成");
            return forecast;

        } catch (Exception e) {
            log.error("获取收入预测分析失败", e);
            throw new RuntimeException("获取收入预测分析失败: " + e.getMessage());
        }
    }

    /**
     * 收入结构分析
     */
    @Cacheable(value = "revenue:structure", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public Map<String, Object> getRevenueStructureAnalysis(LocalDate startDate, LocalDate endDate) {
        log.info("获取收入结构分析，时间段: {} 至 {}", startDate, endDate);

        try {
            // 获取所有订单
            List<Order> orders = getCompletedOrdersInRange(startDate, endDate);

            // 按价格区间分析
            Map<String, Integer> priceRangeDistribution = analyzePriceRangeDistribution(orders);

            // 按入住天数分析
            Map<String, BigDecimal> revenueByStayDuration = analyzeRevenueByStayDuration(orders);

            // 按预订提前期分析
            Map<String, BigDecimal> revenueByBookingLeadTime = analyzeRevenueByBookingLeadTime(orders);

            // 按支付方式分析（如果有的话）
            Map<String, BigDecimal> revenueByPaymentMethod = analyzeRevenueByPaymentMethod(orders);

            Map<String, Object> structure = new HashMap<>();
            structure.put("priceRangeDistribution", priceRangeDistribution);
            structure.put("revenueByStayDuration", revenueByStayDuration);
            structure.put("revenueByBookingLeadTime", revenueByBookingLeadTime);
            structure.put("revenueByPaymentMethod", revenueByPaymentMethod);
            structure.put("totalOrders", orders.size());
            structure.put("totalRevenue", orders.stream()
                .filter(order -> order.getTotalPrice() != null)
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

            log.info("收入结构分析获取完成");
            return structure;

        } catch (Exception e) {
            log.error("获取收入结构分析失败", e);
            throw new RuntimeException("获取收入结构分析失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 根据房型ID获取订单
     */
    private List<Order> getOrdersByRoomType(Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        List<Room> rooms = roomRepository.selectList(null).stream()
            .filter(room -> roomTypeId.equals(room.getRoomTypeId()))
            .collect(Collectors.toList());

        List<Long> roomIds = rooms.stream().map(Room::getId).collect(Collectors.toList());
        List<Order> allOrders = orderRepository.selectList(null);

        return allOrders.stream()
            .filter(order -> roomIds.contains(order.getRoomId())
                && order.getCreatedAt() != null
                && order.getCreatedAt().toLocalDate().isAfter(startDate.minusDays(1))
                && order.getCreatedAt().toLocalDate().isBefore(endDate.plusDays(1)))
            .collect(Collectors.toList());
    }

    /**
     * 获取指定时间范围内的已完成订单
     */
    private List<Order> getCompletedOrdersInRange(LocalDate startDate, LocalDate endDate) {
        List<Order> allOrders = orderRepository.selectList(null);
        return allOrders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus())
                && order.getCreatedAt() != null
                && order.getCreatedAt().toLocalDate().isAfter(startDate.minusDays(1))
                && order.getCreatedAt().toLocalDate().isBefore(endDate.plusDays(1)))
            .collect(Collectors.toList());
    }

    /**
     * 分析价格区间分布
     */
    private Map<String, Integer> analyzePriceRangeDistribution(List<Order> orders) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("0-500", 0);
        distribution.put("500-1000", 0);
        distribution.put("1000-2000", 0);
        distribution.put("2000-5000", 0);
        distribution.put("5000+", 0);

        for (Order order : orders) {
            if (order.getTotalPrice() == null) continue;

            BigDecimal price = order.getTotalPrice();
            if (price.compareTo(BigDecimal.valueOf(500)) < 0) {
                distribution.merge("0-500", 1, Integer::sum);
            } else if (price.compareTo(BigDecimal.valueOf(1000)) < 0) {
                distribution.merge("500-1000", 1, Integer::sum);
            } else if (price.compareTo(BigDecimal.valueOf(2000)) < 0) {
                distribution.merge("1000-2000", 1, Integer::sum);
            } else if (price.compareTo(BigDecimal.valueOf(5000)) < 0) {
                distribution.merge("2000-5000", 1, Integer::sum);
            } else {
                distribution.merge("5000+", 1, Integer::sum);
            }
        }

        return distribution;
    }

    /**
     * 按入住天数分析收入
     */
    private Map<String, BigDecimal> analyzeRevenueByStayDuration(List<Order> orders) {
        Map<String, BigDecimal> revenueByDuration = new HashMap<>();
        revenueByDuration.put("1天", BigDecimal.ZERO);
        revenueByDuration.put("2-3天", BigDecimal.ZERO);
        revenueByDuration.put("4-7天", BigDecimal.ZERO);
        revenueByDuration.put("8-14天", BigDecimal.ZERO);
        revenueByDuration.put("15天+", BigDecimal.ZERO);

        for (Order order : orders) {
            if (order.getTotalPrice() == null || order.getCheckInDate() == null || order.getCheckOutDate() == null) {
                continue;
            }

            long days = java.time.temporal.ChronoUnit.DAYS.between(order.getCheckInDate(), order.getCheckOutDate());
            String durationKey;

            if (days == 1) {
                durationKey = "1天";
            } else if (days <= 3) {
                durationKey = "2-3天";
            } else if (days <= 7) {
                durationKey = "4-7天";
            } else if (days <= 14) {
                durationKey = "8-14天";
            } else {
                durationKey = "15天+";
            }

            revenueByDuration.merge(durationKey, order.getTotalPrice(), BigDecimal::add);
        }

        return revenueByDuration;
    }

    /**
     * 按预订提前期分析收入
     */
    private Map<String, BigDecimal> analyzeRevenueByBookingLeadTime(List<Order> orders) {
        Map<String, BigDecimal> revenueByLeadTime = new HashMap<>();
        revenueByLeadTime.put("当天", BigDecimal.ZERO);
        revenueByLeadTime.put("1-3天", BigDecimal.ZERO);
        revenueByLeadTime.put("4-7天", BigDecimal.ZERO);
        revenueByLeadTime.put("8-14天", BigDecimal.ZERO);
        revenueByLeadTime.put("15天+", BigDecimal.ZERO);

        for (Order order : orders) {
            if (order.getTotalPrice() == null || order.getCreatedAt() == null || order.getCheckInDate() == null) {
                continue;
            }

            long days = java.time.temporal.ChronoUnit.DAYS.between(
                order.getCreatedAt().toLocalDate(), order.getCheckInDate());
            String leadTimeKey;

            if (days == 0) {
                leadTimeKey = "当天";
            } else if (days <= 3) {
                leadTimeKey = "1-3天";
            } else if (days <= 7) {
                leadTimeKey = "4-7天";
            } else if (days <= 14) {
                leadTimeKey = "8-14天";
            } else {
                leadTimeKey = "15天+";
            }

            revenueByLeadTime.merge(leadTimeKey, order.getTotalPrice(), BigDecimal::add);
        }

        return revenueByLeadTime;
    }

    /**
     * 按支付方式分析收入
     */
    private Map<String, BigDecimal> analyzeRevenueByPaymentMethod(List<Order> orders) {
        Map<String, BigDecimal> revenueByPaymentMethod = new HashMap<>();

        // 模拟数据，实际应该从订单表中获取支付方式
        revenueByPaymentMethod.put("支付宝", BigDecimal.ZERO);
        revenueByPaymentMethod.put("微信支付", BigDecimal.ZERO);
        revenueByPaymentMethod.put("银行卡", BigDecimal.ZERO);
        revenueByPaymentMethod.put("现金", BigDecimal.ZERO);

        // 简单分配（实际应该根据真实数据）
        for (Order order : orders) {
            if (order.getTotalPrice() == null) continue;

            // 模拟分配
            String paymentMethod = switch (order.getId() % 4) {
                case 0 -> "支付宝";
                case 1 -> "微信支付";
                case 2 -> "银行卡";
                default -> "现金";
            };

            revenueByPaymentMethod.merge(paymentMethod, order.getTotalPrice(), BigDecimal::add);
        }

        return revenueByPaymentMethod;
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
     * 计算增长率
     */
    private BigDecimal calculateGrowthRate(List<BigDecimal> values) {
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal first = values.get(0);
        BigDecimal last = values.get(values.size() - 1);

        if (first.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return last.subtract(first).divide(first, 4, RoundingMode.HALF_UP);
    }

    /**
     * 判断是否为周末
     */
    private boolean isWeekend(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }
}