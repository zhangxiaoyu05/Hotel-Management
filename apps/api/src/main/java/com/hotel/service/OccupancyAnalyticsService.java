package com.hotel.service;

import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 房间使用率分析服务
 * 提供详细的入住率统计和分析功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OccupancyAnalyticsService {

    private final OrderRepository orderRepository;
    private final RoomRepository roomRepository;

    /**
     * 获取每日入住率趋势
     */
    @Cacheable(value = "occupancy:trends:daily", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public Map<String, Object> getOccupancyTrendsDaily(LocalDate startDate, LocalDate endDate) {
        log.info("获取每日入住率趋势，时间段: {} 至 {}", startDate, endDate);

        try {
            List<String> dates = new ArrayList<>();
            List<Double> occupancyRates = new ArrayList<>();
            List<Integer> totalRooms = new ArrayList<>();
            List<Integer> occupiedRooms = new ArrayList<>();
            List<Integer> availableRooms = new ArrayList<>();
            List<Integer> maintenanceRooms = new ArrayList<>();

            // 获取所有房间总数
            List<Room> allRooms = roomRepository.selectList(null);
            int totalRoomCount = allRooms.size();

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                dates.add(currentDate.format(DateTimeFormatter.ISO_DATE));

                // 计算当日入住情况
                OccupancyData occupancyData = calculateDailyOccupancy(currentDate, allRooms);

                occupancyRates.add(occupancyData.getOccupancyRate());
                totalRooms.add(totalRoomCount);
                occupiedRooms.add(occupancyData.getOccupiedRooms());
                availableRooms.add(occupancyData.getAvailableRooms());
                maintenanceRooms.add(occupancyData.getMaintenanceRooms());

                currentDate = currentDate.plusDays(1);
            }

            Map<String, Object> trends = new HashMap<>();
            trends.put("dates", dates);
            trends.put("occupancyRates", occupancyRates);
            trends.put("totalRooms", totalRooms);
            trends.put("occupiedRooms", occupiedRooms);
            trends.put("availableRooms", availableRooms);
            trends.put("maintenanceRooms", maintenanceRooms);
            trends.put("averageOccupancyRate", calculateAverage(occupancyRates));
            trends.put("peakOccupancyRate", occupancyRates.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
            trends.put("lowestOccupancyRate", occupancyRates.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));

            log.info("每日入住率趋势获取完成");
            return trends;

        } catch (Exception e) {
            log.error("获取每日入住率趋势失败", e);
            throw new RuntimeException("获取每日入住率趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取月度入住率趋势
     */
    @Cacheable(value = "occupancy:trends:monthly", key = "#year", unless = "#result == null")
    public Map<String, Object> getOccupancyTrendsMonthly(int year) {
        log.info("获取月度入住率趋势，年份: {}", year);

        try {
            List<String> months = new ArrayList<>();
            List<Double> occupancyRates = new ArrayList<>();
            List<Integer> totalRooms = new ArrayList<>();
            List<Integer> occupiedRooms = new ArrayList<>();
            List<Double> growthRates = new ArrayList<>();

            // 获取所有房间总数
            List<Room> allRooms = roomRepository.selectList(null);
            int totalRoomCount = allRooms.size();

            Double previousMonthOccupancy = null;

            for (int month = 1; month <= 12; month++) {
                String monthLabel = String.format("%d-%02d", year, month);
                months.add(monthLabel);

                LocalDate monthStart = LocalDate.of(year, month, 1);
                LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

                // 计算月度平均入住率
                OccupancyData monthOccupancy = calculateMonthlyOccupancy(monthStart, monthEnd, allRooms);

                occupancyRates.add(monthOccupancy.getOccupancyRate());
                totalRooms.add(totalRoomCount);
                occupiedRooms.add((int) (totalRoomCount * monthOccupancy.getOccupancyRate() / 100));

                // 计算环比增长率
                if (previousMonthOccupancy != null && previousMonthOccupancy > 0) {
                    double growthRate = ((monthOccupancy.getOccupancyRate() - previousMonthOccupancy) / previousMonthOccupancy) * 100;
                    growthRates.add(Math.round(growthRate * 100.0) / 100.0);
                } else {
                    growthRates.add(0.0);
                }

                previousMonthOccupancy = monthOccupancy.getOccupancyRate();
            }

            Map<String, Object> trends = new HashMap<>();
            trends.put("months", months);
            trends.put("occupancyRates", occupancyRates);
            trends.put("totalRooms", totalRooms);
            trends.put("occupiedRooms", occupiedRooms);
            trends.put("growthRates", growthRates);
            trends.put("averageMonthlyOccupancyRate", calculateAverage(occupancyRates));
            trends.put("peakMonthlyOccupancyRate", occupancyRates.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
            trends.put("lowestMonthlyOccupancyRate", occupancyRates.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));

            log.info("月度入住率趋势获取完成");
            return trends;

        } catch (Exception e) {
            log.error("获取月度入住率趋势失败", e);
            throw new RuntimeException("获取月度入住率趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取房型入住率对比
     */
    @Cacheable(value = "occupancy:by-room-type", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public Map<String, Object> getOccupancyByRoomType(LocalDate startDate, LocalDate endDate) {
        log.info("获取房型入住率对比，时间段: {} 至 {}", startDate, endDate);

        try {
            // 按房型分组房间
            Map<String, List<Room>> roomsByType = roomRepository.selectList(null).stream()
                .collect(Collectors.groupingBy(room -> {
                    // 这里应该关联房型表获取房型名称，简化处理
                    return "房型" + room.getRoomTypeId();
                }));

            Map<String, Double> occupancyRatesByType = new HashMap<>();
            Map<String, Integer> totalRoomsByType = new HashMap<>();
            Map<String, Integer> occupiedRoomsByType = new HashMap<>();

            for (Map.Entry<String, List<Room>> entry : roomsByType.entrySet()) {
                String roomType = entry.getKey();
                List<Room> rooms = entry.getValue();

                // 计算该房型的时间段内平均入住率
                double totalOccupancyRate = 0.0;
                int days = 0;

                LocalDate currentDate = startDate;
                while (!currentDate.isAfter(endDate)) {
                    OccupancyData dailyOccupancy = calculateDailyOccupancy(currentDate, rooms);
                    totalOccupancyRate += dailyOccupancy.getOccupancyRate();
                    days++;
                    currentDate = currentDate.plusDays(1);
                }

                double averageOccupancyRate = days > 0 ? totalOccupancyRate / days : 0.0;
                int totalRooms = rooms.size();
                int occupiedRooms = (int) (totalRooms * averageOccupancyRate / 100);

                occupancyRatesByType.put(roomType, Math.round(averageOccupancyRate * 100.0) / 100.0);
                totalRoomsByType.put(roomType, totalRooms);
                occupiedRoomsByType.put(roomType, occupiedRooms);
            }

            // 按入住率排序
            List<Map.Entry<String, Double>> sortedOccupancy = occupancyRatesByType.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

            Map<String, Object> analysis = new HashMap<>();
            analysis.put("occupancyRatesByType", occupancyRatesByType);
            analysis.put("totalRoomsByType", totalRoomsByType);
            analysis.put("occupiedRoomsByType", occupiedRoomsByType);
            analysis.put("sortedRoomTypes", sortedOccupancy.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()));
            analysis.put("sortedOccupancyRates", sortedOccupancy.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()));
            analysis.put("averageOccupancyRate", occupancyRatesByType.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0));

            log.info("房型入住率对比获取完成");
            return analysis;

        } catch (Exception e) {
            log.error("获取房型入住率对比失败", e);
            throw new RuntimeException("获取房型入住率对比失败: " + e.getMessage());
        }
    }

    /**
     * 获取入住率预测
     */
    @Cacheable(value = "occupancy:forecast", key = "#days", unless = "#result == null")
    public Map<String, Object> getOccupancyForecast(int days) {
        log.info("获取入住率预测，预测天数: {}", days);

        try {
            LocalDate today = LocalDate.now();
            LocalDate historicalStart = today.minusDays(30); // 使用过去30天作为历史数据
            LocalDate historicalEnd = today.minusDays(1);

            // 获取历史入住率数据
            Map<String, Object> historicalData = getOccupancyTrendsDaily(historicalStart, historicalEnd);
            @SuppressWarnings("unchecked")
            List<Double> historicalOccupancyRates = (List<Double>) historicalData.get("occupancyRates");

            if (historicalOccupancyRates.size() < 7) {
                throw new RuntimeException("历史数据不足，无法进行预测");
            }

            // 计算基础预测数据
            double averageOccupancy = calculateAverage(historicalOccupancyRates);
            double weeklyPattern = calculateWeeklyPattern(historicalOccupancyRates);

            // 生成预测数据
            List<String> forecastDates = new ArrayList<>();
            List<Double> forecastOccupancyRates = new ArrayList<>();
            List<Double> confidenceIntervals = new ArrayList<>();

            for (int i = 1; i <= days; i++) {
                LocalDate forecastDate = today.plusDays(i);
                forecastDates.add(forecastDate.format(DateTimeFormatter.ISO_DATE));

                // 基础预测值
                double forecastOccupancy = averageOccupancy;

                // 周模式调整（周末通常入住率较低）
                double weekendAdjustment = isWeekend(forecastDate) ? -5.0 : 2.0;
                forecastOccupancy += weekendAdjustment;

                // 周内模式调整
                double weeklyAdjustment = getWeeklyAdjustment(forecastDate.getDayOfWeek().getValue());
                forecastOccupancy += weeklyAdjustment;

                // 确保在合理范围内
                forecastOccupancy = Math.max(0.0, Math.min(100.0, forecastOccupancy));
                forecastOccupancyRates.add(Math.round(forecastOccupancy * 100.0) / 100.0);

                // 置信区间（±10%）
                double confidenceInterval = 10.0;
                confidenceIntervals.add(confidenceInterval);
            }

            Map<String, Object> forecast = new HashMap<>();
            forecast.put("forecastDates", forecastDates);
            forecast.put("forecastOccupancyRates", forecastOccupancyRates);
            forecast.put("confidenceIntervals", confidenceIntervals);
            forecast.put("averageForecastOccupancy", calculateAverage(forecastOccupancyRates));
            forecast.put("historicalAverageOccupancy", averageOccupancy);
            forecast.put("weeklyPattern", weeklyPattern);

            log.info("入住率预测获取完成");
            return forecast;

        } catch (Exception e) {
            log.error("获取入住率预测失败", e);
            throw new RuntimeException("获取入住率预测失败: " + e.getMessage());
        }
    }

    /**
     * 获取入住率热力图数据
     */
    @Cacheable(value = "occupancy:heatmap", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public Map<String, Object> getOccupancyHeatmap(LocalDate startDate, LocalDate endDate) {
        log.info("获取入住率热力图数据，时间段: {} 至 {}", startDate, endDate);

        try {
            // 按星期几和日期分组
            Map<Integer, Map<Integer, Double>> weeklyHeatmap = new HashMap<>();
            List<Room> allRooms = roomRepository.selectList(null);

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                int dayOfWeek = currentDate.getDayOfWeek().getValue(); // 1-7 (周一到周日)
                int dayOfMonth = currentDate.getDayOfMonth();

                double occupancyRate = calculateDailyOccupancy(currentDate, allRooms).getOccupancyRate();

                weeklyHeatmap.computeIfAbsent(dayOfWeek, k -> new HashMap<>())
                    .put(dayOfMonth, Math.round(occupancyRate * 100.0) / 100.0);

                currentDate = currentDate.plusDays(1);
            }

            // 转换为前端需要的格式
            List<List<Object>> heatmapData = new ArrayList<>();
            for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
                Map<Integer, Double> dayData = weeklyHeatmap.getOrDefault(dayOfWeek, new HashMap<>());
                for (Map.Entry<Integer, Double> entry : dayData.entrySet()) {
                    List<Object> dataPoint = new ArrayList<>();
                    dataPoint.add(dayOfWeek);
                    dataPoint.add(entry.getKey());
                    dataPoint.add(entry.getValue());
                    heatmapData.add(dataPoint);
                }
            }

            Map<String, Object> heatmap = new HashMap<>();
            heatmap.put("heatmapData", heatmapData);
            heatmap.put("startDate", startDate.format(DateTimeFormatter.ISO_DATE));
            heatmap.put("endDate", endDate.format(DateTimeFormatter.ISO_DATE));
            heatmap.put("weekDays", Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日"));

            log.info("入住率热力图数据获取完成");
            return heatmap;

        } catch (Exception e) {
            log.error("获取入住率热力图数据失败", e);
            throw new RuntimeException("获取入住率热力图数据失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 计算每日入住情况
     */
    private OccupancyData calculateDailyOccupancy(LocalDate date, List<Room> rooms) {
        int totalRooms = rooms.size();
        int occupiedRooms = 0;
        int maintenanceRooms = 0;
        int availableRooms = 0;

        for (Room room : rooms) {
            if ("MAINTENANCE".equals(room.getStatus())) {
                maintenanceRooms++;
            } else if ("OCCUPIED".equals(room.getStatus())) {
                occupiedRooms++;
            } else if ("AVAILABLE".equals(room.getStatus())) {
                // 检查当天是否有订单
                boolean hasOrder = hasOrderOnDate(room.getId(), date);
                if (hasOrder) {
                    occupiedRooms++;
                } else {
                    availableRooms++;
                }
            }
        }

        double occupancyRate = totalRooms > 0 ? (double) occupiedRooms / totalRooms * 100 : 0.0;
        return new OccupancyData(totalRooms, occupiedRooms, availableRooms, maintenanceRooms, occupancyRate);
    }

    /**
     * 计算月度平均入住情况
     */
    private OccupancyData calculateMonthlyOccupancy(LocalDate startDate, LocalDate endDate, List<Room> rooms) {
        double totalOccupancyRate = 0.0;
        int days = 0;

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            OccupancyData dailyOccupancy = calculateDailyOccupancy(currentDate, rooms);
            totalOccupancyRate += dailyOccupancy.getOccupancyRate();
            days++;
            currentDate = currentDate.plusDays(1);
        }

        double averageOccupancyRate = days > 0 ? totalOccupancyRate / days : 0.0;
        int totalRooms = rooms.size();
        int occupiedRooms = (int) (totalRooms * averageOccupancyRate / 100);

        return new OccupancyData(totalRooms, occupiedRooms, totalRooms - occupiedRooms, 0, averageOccupancyRate);
    }

    /**
     * 检查房间在指定日期是否有订单
     */
    private boolean hasOrderOnDate(Long roomId, LocalDate date) {
        List<Order> orders = orderRepository.selectList(null);
        return orders.stream()
            .anyMatch(order -> roomId.equals(order.getRoomId())
                && Arrays.asList("CONFIRMED", "COMPLETED").contains(order.getStatus())
                && !date.isBefore(order.getCheckInDate())
                && !date.isAfter(order.getCheckOutDate().minusDays(1)));
    }

    /**
     * 计算平均值
     */
    private double calculateAverage(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /**
     * 计算周模式
     */
    private double calculateWeeklyPattern(List<Double> historicalOccupancyRates) {
        if (historicalOccupancyRates.size() < 7) {
            return 0.0;
        }

        // 计算周末（周六、周日）和工作日的平均入住率差异
        double weekendSum = 0.0, weekdaySum = 0.0;
        int weekendCount = 0, weekdayCount = 0;

        // 简化计算，假设数据是按日期顺序的
        for (int i = 0; i < historicalOccupancyRates.size(); i++) {
            double rate = historicalOccupancyRates.get(i);
            // 这里应该根据实际日期判断是周末还是工作日，简化处理
            if (i % 7 == 5 || i % 7 == 6) { // 周六、周日
                weekendSum += rate;
                weekendCount++;
            } else {
                weekdaySum += rate;
                weekdayCount++;
            }
        }

        double weekendAvg = weekendCount > 0 ? weekendSum / weekendCount : 0.0;
        double weekdayAvg = weekdayCount > 0 ? weekdaySum / weekdayCount : 0.0;

        return weekendAvg - weekdayAvg;
    }

    /**
     * 获取周调整系数
     */
    private double getWeeklyAdjustment(int dayOfWeek) {
        // 周一到周日的调整系数
        return switch (dayOfWeek) {
            case 1 -> 1.0;  // 周一
            case 2 -> 0.5;  // 周二
            case 3 -> 0.0;  // 周三
            case 4 -> -0.5; // 周四
            case 5 -> 2.0;  // 周五
            case 6 -> -3.0; // 周六
            case 7 -> -2.0; // 周日
            default -> 0.0;
        };
    }

    /**
     * 判断是否为周末
     */
    private boolean isWeekend(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }

    /**
     * 入住数据内部类
     */
    private static class OccupancyData {
        private final int totalRooms;
        private final int occupiedRooms;
        private final int availableRooms;
        private final int maintenanceRooms;
        private final double occupancyRate;

        public OccupancyData(int totalRooms, int occupiedRooms, int availableRooms, int maintenanceRooms, double occupancyRate) {
            this.totalRooms = totalRooms;
            this.occupiedRooms = occupiedRooms;
            this.availableRooms = availableRooms;
            this.maintenanceRooms = maintenanceRooms;
            this.occupancyRate = occupancyRate;
        }

        public int getTotalRooms() { return totalRooms; }
        public int getOccupiedRooms() { return occupiedRooms; }
        public int getAvailableRooms() { return availableRooms; }
        public int getMaintenanceRooms() { return maintenanceRooms; }
        public double getOccupancyRate() { return occupancyRate; }
    }
}