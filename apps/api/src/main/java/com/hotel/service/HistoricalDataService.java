package com.hotel.service;

import com.hotel.dto.historical.HistoricalDataQueryDTO;
import com.hotel.dto.historical.HistoricalDataAggregationDTO;
import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.entity.User;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 历史数据服务
 * 提供历史数据查询、聚合和分析功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataService {

    private final OrderRepository orderRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    /**
     * 查询历史订单数据
     */
    @Cacheable(value = "historical:orders", key = "#query.hashCode()", unless = "#result == null")
    public Map<String, Object> queryHistoricalOrders(HistoricalDataQueryDTO query) {
        log.info("查询历史订单数据，查询条件: {}", query);

        try {
            List<Order> allOrders = orderRepository.selectList(null);

            // 应用查询过滤条件
            List<Order> filteredOrders = applyOrderFilters(allOrders, query);

            // 分页处理
            int total = filteredOrders.size();
            int startIndex = (query.getPage() - 1) * query.getSize();
            int endIndex = Math.min(startIndex + query.getSize(), total);
            List<Order> pageData = filteredOrders.subList(startIndex, endIndex);

            // 数据聚合
            Map<String, Object> aggregations = calculateOrderAggregations(filteredOrders, query);

            Map<String, Object> result = new HashMap<>();
            result.put("data", pageData);
            result.put("total", total);
            result.put("page", query.getPage());
            result.put("size", query.getSize());
            result.put("aggregations", aggregations);

            log.info("历史订单数据查询完成，返回 {} 条记录", pageData.size());
            return result;

        } catch (Exception e) {
            log.error("查询历史订单数据失败", e);
            throw new RuntimeException("查询历史订单数据失败: " + e.getMessage());
        }
    }

    /**
     * 聚合历史数据
     */
    @Cacheable(value = "historical:aggregation", key = "#query.hashCode()", unless = "#result == null")
    public HistoricalDataAggregationDTO aggregateHistoricalData(HistoricalDataQueryDTO query) {
        log.info("聚合历史数据，查询条件: {}", query);

        try {
            HistoricalDataAggregationDTO aggregation = new HistoricalDataAggregationDTO();

            // 订单数据聚合
            List<Order> orders = applyOrderFilters(orderRepository.selectList(null), query);
            aggregateOrderData(aggregation, orders, query);

            // 房间数据聚合
            List<Room> rooms = roomRepository.selectList(null);
            aggregateRoomData(aggregation, rooms, query);

            // 用户数据聚合
            List<User> users = userRepository.selectList(null);
            aggregateUserData(aggregation, users, query);

            log.info("历史数据聚合完成");
            return aggregation;

        } catch (Exception e) {
            log.error("聚合历史数据失败", e);
            throw new RuntimeException("聚合历史数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取历史数据统计报告
     */
    @Cacheable(value = "historical:report", key = "#startDate + '_' + #endDate + '_' + #groupBy", unless = "#result == null")
    public Map<String, Object> generateHistoricalReport(LocalDate startDate, LocalDate endDate, String groupBy) {
        log.info("生成历史数据统计报告，时间段: {} 至 {}，分组方式: {}", startDate, endDate, groupBy);

        try {
            Map<String, Object> report = new HashMap<>();

            // 基础统计
            List<Order> orders = orderRepository.selectList(null).stream()
                .filter(order -> order.getCreatedAt() != null
                    && !order.getCreatedAt().toLocalDate().isBefore(startDate)
                    && !order.getCreatedAt().toLocalDate().isAfter(endDate))
                .collect(Collectors.toList());

            report.put("totalOrders", orders.size());
            report.put("totalRevenue", orders.stream()
                .filter(order -> order.getTotalPrice() != null)
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
            report.put("averageOrderValue", orders.stream()
                .filter(order -> order.getTotalPrice() != null)
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, orders.size())), 2, RoundingMode.HALF_UP));

            // 按时间分组统计
            switch (groupBy.toLowerCase()) {
                case "daily":
                    report.put("dailyStats", calculateDailyStatistics(orders, startDate, endDate));
                    break;
                case "weekly":
                    report.put("weeklyStats", calculateWeeklyStatistics(orders, startDate, endDate));
                    break;
                case "monthly":
                    report.put("monthlyStats", calculateMonthlyStatistics(orders, startDate, endDate));
                    break;
            }

            // 状态分布
            report.put("statusDistribution", calculateStatusDistribution(orders));

            // 趋势分析
            report.put("trendAnalysis", calculateTrendAnalysis(orders));

            log.info("历史数据统计报告生成完成");
            return report;

        } catch (Exception e) {
            log.error("生成历史数据统计报告失败", e);
            throw new RuntimeException("生成历史数据统计报告失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据导出
     */
    @Cacheable(value = "historical:export", key = "#query.hashCode() + '_' + #format", unless = "#result == null")
    public byte[] exportHistoricalData(HistoricalDataQueryDTO query, String format) {
        log.info("导出历史数据，格式: {}，查询条件: {}", format, query);

        try {
            List<Order> orders = applyOrderFilters(orderRepository.selectList(null), query);

            switch (format.toLowerCase()) {
                case "csv":
                    return exportToCSV(orders);
                case "excel":
                    return exportToExcel(orders);
                case "json":
                    return exportToJSON(orders);
                default:
                    throw new IllegalArgumentException("不支持的导出格式: " + format);
            }

        } catch (Exception e) {
            log.error("导出历史数据失败", e);
            throw new RuntimeException("导出历史数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据快照
     */
    @Cacheable(value = "historical:snapshot", key = "#date", unless = "#result == null")
    public Map<String, Object> getDataSnapshot(LocalDate date) {
        log.info("获取数据快照，日期: {}", date);

        try {
            Map<String, Object> snapshot = new HashMap<>();

            // 当日订单快照
            List<Order> dailyOrders = orderRepository.selectList(null).stream()
                .filter(order -> order.getCreatedAt() != null
                    && order.getCreatedAt().toLocalDate().equals(date))
                .collect(Collectors.toList());

            snapshot.put("dailyOrders", dailyOrders.size());
            snapshot.put("dailyRevenue", dailyOrders.stream()
                .filter(order -> order.getTotalPrice() != null)
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

            // 房间状态快照
            List<Room> rooms = roomRepository.selectList(null);
            Map<String, Long> roomStatusCounts = rooms.stream()
                .collect(Collectors.groupingBy(Room::getStatus, Collectors.counting()));
            snapshot.put("roomStatusCounts", roomStatusCounts);

            // 用户统计快照
            List<User> users = userRepository.selectList(null);
            long activeUsers = users.stream()
                .filter(user -> user.getCreatedAt() != null
                    && user.getCreatedAt().toLocalDate().equals(date))
                .count();
            snapshot.put("newUsers", activeUsers);

            snapshot.put("snapshotTime", date.atStartOfDay());
            snapshot.put("totalUsers", users.size());
            snapshot.put("totalRooms", rooms.size());

            log.info("数据快照获取完成");
            return snapshot;

        } catch (Exception e) {
            log.error("获取数据快照失败", e);
            throw new RuntimeException("获取数据快照失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 应用订单过滤条件
     */
    private List<Order> applyOrderFilters(List<Order> orders, HistoricalDataQueryDTO query) {
        return orders.stream()
            .filter(order -> {
                // 时间范围过滤
                if (order.getCreatedAt() == null) return false;
                if (query.getStartDate() != null && order.getCreatedAt().toLocalDate().isBefore(query.getStartDate())) {
                    return false;
                }
                if (query.getEndDate() != null && order.getCreatedAt().toLocalDate().isAfter(query.getEndDate())) {
                    return false;
                }

                // 状态过滤
                if (query.getStatuses() != null && !query.getStatuses().isEmpty()
                    && !query.getStatuses().contains(order.getStatus())) {
                    return false;
                }

                // 金额范围过滤
                if (order.getTotalPrice() != null) {
                    if (query.getMinAmount() != null && order.getTotalPrice().compareTo(query.getMinAmount()) < 0) {
                        return false;
                    }
                    if (query.getMaxAmount() != null && order.getTotalPrice().compareTo(query.getMaxAmount()) > 0) {
                        return false;
                    }
                }

                return true;
            })
            .sorted((o1, o2) -> {
                // 排序
                if ("createdAt".equals(query.getSortBy())) {
                    int direction = "asc".equalsIgnoreCase(query.getSortOrder()) ? -1 : 1;
                    return direction * o2.getCreatedAt().compareTo(o1.getCreatedAt());
                }
                return 0;
            })
            .collect(Collectors.toList());
    }

    /**
     * 计算订单聚合数据
     */
    private void aggregateOrderData(HistoricalDataAggregationDTO aggregation, List<Order> orders, HistoricalDataQueryDTO query) {
        aggregation.setTotalOrders(orders.size());
        aggregation.setTotalRevenue(orders.stream()
            .filter(order -> order.getTotalPrice() != null)
            .map(Order::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 按状态统计
        Map<String, Long> statusCount = orders.stream()
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        aggregation.setOrderStatusDistribution(statusCount);

        // 按金额区间统计
        Map<String, Long> amountRangeCount = orders.stream()
            .filter(order -> order.getTotalPrice() != null)
            .collect(Collectors.groupingBy(
                order -> getAmountRange(order.getTotalPrice()),
                Collectors.counting()
            ));
        aggregation.setOrderAmountDistribution(amountRangeCount);
    }

    /**
     * 计算房间聚合数据
     */
    private void aggregateRoomData(HistoricalDataAggregationDTO aggregation, List<Room> rooms, HistoricalDataQueryDTO query) {
        aggregation.setTotalRooms(rooms.size());

        Map<String, Long> statusCount = rooms.stream()
            .collect(Collectors.groupingBy(Room::getStatus, Collectors.counting()));
        aggregation.setRoomStatusDistribution(statusCount);
    }

    /**
     * 计算用户聚合数据
     */
    private void aggregateUserData(HistoricalDataAggregationDTO aggregation, List<User> users, HistoricalDataQueryDTO query) {
        aggregation.setTotalUsers(users.size());

        // 按角色统计
        Map<String, Long> roleCount = users.stream()
            .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        aggregation.setUserRoleDistribution(roleCount);

        // 按状态统计
        Map<String, Long> statusCount = users.stream()
            .collect(Collectors.groupingBy(User::getStatus, Collectors.counting()));
        aggregation.setUserStatusDistribution(statusCount);
    }

    /**
     * 计算订单聚合
     */
    private Map<String, Object> calculateOrderAggregations(List<Order> orders, HistoricalDataQueryDTO query) {
        Map<String, Object> aggregations = new HashMap<>();

        aggregations.put("totalOrders", orders.size());
        aggregations.put("totalRevenue", orders.stream()
            .filter(order -> order.getTotalPrice() != null)
            .map(Order::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        return aggregations;
    }

    /**
     * 计算每日统计
     */
    private Map<String, Object> calculateDailyStatistics(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> dailyStats = new HashMap<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<Order> dailyOrders = orders.stream()
                .filter(order -> order.getCreatedAt() != null
                    && order.getCreatedAt().toLocalDate().equals(currentDate))
                .collect(Collectors.toList());

            dailyStats.put(currentDate.format(DateTimeFormatter.ISO_DATE), Map.of(
                "orders", dailyOrders.size(),
                "revenue", dailyOrders.stream()
                    .filter(order -> order.getTotalPrice() != null)
                    .map(Order::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
            ));

            currentDate = currentDate.plusDays(1);
        }

        return dailyStats;
    }

    /**
     * 计算每周统计
     */
    private Map<String, Object> calculateWeeklyStatistics(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        // 简化实现，按周聚合
        Map<String, Object> weeklyStats = new HashMap<>();
        int weekNumber = 1;

        LocalDate weekStart = startDate;
        while (weekStart.isBefore(endDate)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            List<Order> weeklyOrders = orders.stream()
                .filter(order -> order.getCreatedAt() != null
                    && !order.getCreatedAt().toLocalDate().isBefore(weekStart)
                    && !order.getCreatedAt().toLocalDate().isAfter(weekEnd))
                .collect(Collectors.toList());

            weeklyStats.put("第" + weekNumber + "周", Map.of(
                "orders", weeklyOrders.size(),
                "revenue", weeklyOrders.stream()
                    .filter(order -> order.getTotalPrice() != null)
                    .map(Order::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
            ));

            weekStart = weekEnd.plusDays(1);
            weekNumber++;
        }

        return weeklyStats;
    }

    /**
     * 计算每月统计
     */
    private Map<String, Object> calculateMonthlyStatistics(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> monthlyStats = new HashMap<>();

        LocalDate currentMonth = LocalDate.of(startDate.getYear(), startDate.getMonthValue(), 1);
        while (!currentMonth.isAfter(endDate)) {
            LocalDate monthStart = currentMonth;
            LocalDate monthEnd = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth());
            if (monthEnd.isAfter(endDate)) {
                monthEnd = endDate;
            }

            List<Order> monthlyOrders = orders.stream()
                .filter(order -> order.getCreatedAt() != null
                    && !order.getCreatedAt().toLocalDate().isBefore(monthStart)
                    && !order.getCreatedAt().toLocalDate().isAfter(monthEnd))
                .collect(Collectors.toList());

            monthlyStats.put(currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")), Map.of(
                "orders", monthlyOrders.size(),
                "revenue", monthlyOrders.stream()
                    .filter(order -> order.getTotalPrice() != null)
                    .map(Order::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
            ));

            currentMonth = currentMonth.plusMonths(1);
        }

        return monthlyStats;
    }

    /**
     * 计算状态分布
     */
    private Map<String, Long> calculateStatusDistribution(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
    }

    /**
     * 计算趋势分析
     */
    private Map<String, Object> calculateTrendAnalysis(List<Order> orders) {
        // 简化实现，返回基本趋势数据
        return Map.of(
            "growthRate", 0.0,
            "peakDay", orders.stream()
                .collect(Collectors.groupingBy(
                    order -> order.getCreatedAt().toLocalDate(),
                    Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(date -> date.format(DateTimeFormatter.ISO_DATE))
                .orElse(""),
            "averageDailyOrders", orders.size() / 30.0
        );
    }

    /**
     * 获取金额区间
     */
    private String getAmountRange(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(500)) < 0) {
            return "0-500";
        } else if (amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return "500-1000";
        } else if (amount.compareTo(BigDecimal.valueOf(2000)) < 0) {
            return "1000-2000";
        } else if (amount.compareTo(BigDecimal.valueOf(5000)) < 0) {
            return "2000-5000";
        } else {
            return "5000+";
        }
    }

    /**
     * 导出为CSV
     */
    private byte[] exportToCSV(List<Order> orders) {
        StringBuilder csv = new StringBuilder();
        csv.append("订单号,用户ID,房间ID,入住日期,退房日期,总价,状态,创建时间\n");

        for (Order order : orders) {
            csv.append(String.format("%s,%d,%d,%s,%s,%s,%s,%s\n",
                order.getOrderNumber(),
                order.getUserId(),
                order.getRoomId(),
                order.getCheckInDate(),
                order.getCheckOutDate(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt()
            ));
        }

        return csv.toString().getBytes();
    }

    /**
     * 导出为Excel
     */
    private byte[] exportToExcel(List<Order> orders) {
        // 简化实现，实际应该使用Apache POI
        return exportToCSV(orders);
    }

    /**
     * 导出为JSON
     */
    private byte[] exportToJSON(List<Order> orders) {
        // 简化实现
        StringBuilder json = new StringBuilder();
        json.append("{\"orders\":[\n");

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            json.append("{");
            json.append(String.format("\"id\":%d,", order.getId()));
            json.append(String.format("\"orderNumber\":\"%s\",", order.getOrderNumber()));
            json.append(String.format("\"userId\":%d,", order.getUserId()));
            json.append(String.format("\"roomId\":%d,", order.getRoomId()));
            json.append(String.format("\"totalPrice\":%s,", order.getTotalPrice()));
            json.append(String.format("\"status\":\"%s\",", order.getStatus()));
            json.append(String.format("\"createdAt\":\"%s\"", order.getCreatedAt()));
            json.append("}");
            if (i < orders.size() - 1) {
                json.append(",\n");
            }
        }

        json.append("\n]}");
        return json.toString().getBytes();
    }
}