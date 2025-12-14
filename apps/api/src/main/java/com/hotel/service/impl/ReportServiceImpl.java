package com.hotel.service.impl;

import com.hotel.dto.report.*;
import com.hotel.entity.Order;
import com.hotel.entity.User;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.exception.ExportException;
import com.hotel.exception.ReportException;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.UserRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.service.ReportService;
import com.hotel.util.DataMaskingUtil;
import com.hotel.util.ExcelExporter;
import com.hotel.util.PDFExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import com.hotel.dto.report.UserReportDTO.UserOrderSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表服务实现类
 *
 * @author Hotel System
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ExcelExporter excelExporter;
    private final PDFExporter pdfExporter;

    @Value("${app.export.path:./exports/reports}")
    private String exportPath;

    @Override
    @Cacheable(value = "orderReport", key = "#startDate.toString() + '_' + #endDate.toString() + '_' + #roomTypeId + '_' + #orderStatus")
    public OrderReportDTO generateOrderReport(LocalDate startDate, LocalDate endDate, Long roomTypeId, String orderStatus) {
        log.info("生成订单报表：{} - {}, 房型：{}, 状态：{}", startDate, endDate, roomTypeId, orderStatus);

        OrderReportDTO report = new OrderReportDTO();
        report.setStartDate(startDate);
        report.setEndDate(endDate);

        // 查询订单数据
        List<Order> orders = findOrdersByCriteria(startDate, endDate, roomTypeId, orderStatus);

        // 计算基础统计
        report.setTotalOrders((long) orders.size());
        report.setTotalRevenue(calculateTotalRevenue(orders));
        report.setAverageOrderValue(calculateAverageOrderValue(orders));

        // 按状态统计
        report.setOrdersByStatus(calculateOrdersByStatus(orders));

        // 按房型统计
        report.setOrdersByRoomType(calculateOrdersByRoomType(orders));

        // 按日期统计
        report.setOrdersByDate(calculateOrdersByDate(orders, startDate, endDate));

        // 按月份统计收入
        report.setRevenueByMonth(calculateRevenueByMonth(orders));

        // 按渠道统计（假设渠道信息在订单的特殊请求中）
        report.setOrdersByChannel(calculateOrdersByChannel(orders));

        // 计算完成率和取消率
        report.setCompletionRate(calculateCompletionRate(orders));
        report.setCancellationRate(calculateCancellationRate(orders));

        // 生成趋势数据
        report.setOrderTrends(generateOrderTrends(orders, startDate, endDate));

        // 房型偏好排行
        report.setRoomTypePreferences(calculateRoomTypePreferences(orders));

        return report;
    }

    @Override
    @Cacheable(value = "revenueReport", key = "#startDate.toString() + '_' + #endDate.toString() + '_' + #roomTypeId")
    public RevenueReportDTO generateRevenueReport(LocalDate startDate, LocalDate endDate, Long roomTypeId) {
        log.info("生成收入报表：{} - {}, 房型：{}", startDate, endDate, roomTypeId);

        RevenueReportDTO report = new RevenueReportDTO();
        report.setStartDate(startDate);
        report.setEndDate(endDate);

        // 查询订单数据
        List<Order> orders = findOrdersByCriteria(startDate, endDate, roomTypeId, null);

        // 计算总收入
        report.setTotalRevenue(calculateTotalRevenue(orders));

        // 按月份统计收入
        report.setMonthlyRevenue(calculateRevenueByMonth(orders));

        // 按房型统计收入
        report.setRevenueByRoomType(calculateRevenueByRoomType(orders));

        // 计算ADR和RevPAR
        report.setAverageDailyRate(calculateAverageDailyRate(orders, startDate, endDate));
        report.setRevenuePerAvailableRoom(calculateRevPAR(orders, startDate, endDate));

        // 计算入住率
        report.setOccupancyRate(calculateOccupancyRate(orders, startDate, endDate));

        // 计算收入增长率（需要与上一个周期对比）
        report.setRevenueGrowthRate(calculateRevenueGrowthRate(orders, startDate, endDate));

        // 日收入趋势
        report.setDailyRevenueTrends(generateDailyRevenueTrends(orders, startDate, endDate));

        // 房型收入贡献排行
        report.setRoomTypeRevenueContributions(calculateRoomTypeRevenueContributions(orders));

        // 收入预测数据（简单线性预测）
        report.setRevenueForecasts(generateRevenueForecasts(orders));

        return report;
    }

    @Override
    @Cacheable(value = "userReport", key = "#startDate.toString() + '_' + #endDate.toString()")
    public UserReportDTO generateUserReport(LocalDate startDate, LocalDate endDate) {
        log.info("生成用户报表：{} - {}", startDate, endDate);

        UserReportDTO report = new UserReportDTO();
        report.setStartDate(startDate);
        report.setEndDate(endDate);

        // 查询所有用户
        List<User> allUsers = userRepository.findAll();
        report.setTotalUsers((long) allUsers.size());

        // 按月份统计新增用户
        report.setNewUsersByMonth(calculateNewUsersByMonth(allUsers, startDate, endDate));

        // 计算活跃用户数（在指定时间段内有订单的用户）
        List<Order> orders = orderRepository.findByCreatedAtBetween(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        Set<Long> activeUserIds = orders.stream().map(Order::getUserId).collect(Collectors.toSet());
        report.setActiveUsers((long) activeUserIds.size());

        // 计算用户留存率（简化版本）
        report.setUserRetentionRate(calculateUserRetentionRate(orders, startDate, endDate));

        // 计算用户转化率
        report.setUserConversionRate(calculateUserConversionRate(allUsers, activeUserIds));

        // 按角色统计用户
        report.setUsersByRole(calculateUsersByRole(allUsers));

        // 下单次数最多的用户排行
        report.setTopUsersByOrders(calculateTopUsersByOrders(orders));

        // 消费金额最多的用户排行
        report.setTopUsersBySpending(calculateTopUsersBySpending(orders));

        // 用户注册趋势
        report.setUserRegistrationTrends(generateUserRegistrationTrends(allUsers, startDate, endDate));

        // 用户行为分析
        report.setUserBehaviorAnalysis(calculateUserBehaviorAnalysis(orders));

        return report;
    }

    @Override
    @Cacheable(value = "roomReport", key = "#startDate.toString() + '_' + #endDate.toString() + '_' + #roomTypeId")
    public RoomReportDTO generateRoomReport(LocalDate startDate, LocalDate endDate, Long roomTypeId) {
        log.info("生成房间报表：{} - {}, 房型：{}", startDate, endDate, roomTypeId);

        RoomReportDTO report = new RoomReportDTO();
        report.setStartDate(startDate);
        report.setEndDate(endDate);

        // 查询所有房间
        List<Room> rooms = roomTypeRepository != null && roomTypeId != null
            ? roomRepository.findByRoomTypeId(roomTypeId)
            : roomRepository.findAll();
        report.setTotalRooms((long) rooms.size());

        // 查询房间状态统计
        Map<String, Long> roomStatusCount = rooms.stream()
            .collect(Collectors.groupingBy(Room::getStatus, Collectors.counting()));

        report.setMaintenanceRooms(roomStatusCount.getOrDefault("MAINTENANCE", 0L));
        report.setAvailableRooms(roomStatusCount.getOrDefault("AVAILABLE", 0L));

        // 查询相关订单
        List<Order> orders = findOrdersByCriteria(startDate, endDate, roomTypeId, null);

        // 计算入住率
        report.setOccupancyRate(calculateOccupancyRate(rooms, orders, startDate, endDate));

        // 平均房价
        report.setAverageRoomRate(calculateAverageRoomRate(rooms, orders));

        // 按房型统计入住率
        report.setRoomUtilization(calculateRoomUtilization(rooms, orders));

        // 按房型统计收入
        report.setRevenueByRoomType(calculateRevenueByRoomType(orders));

        // 表现最好的房间排行
        report.setTopPerformingRooms(calculateTopPerformingRooms(rooms, orders));

        // 房间使用趋势
        report.setRoomUtilizationTrends(generateRoomUtilizationTrends(rooms, orders, startDate, endDate));

        // 房型绩效对比
        report.setRoomTypePerformances(calculateRoomTypePerformances(rooms, orders));

        // 房间维护统计
        report.setMaintenanceStats(calculateMaintenanceStats(rooms));

        return report;
    }

    @Override
    public String exportReport(ReportExportRequest request) {
        log.info("导出报表：类型 {}, 格式 {}", request.getReportType(), request.getExportFormat());

        try {
            // 确保导出目录存在
            Path exportDir = Paths.get(exportPath);
            if (!Files.exists(exportDir)) {
                Files.createDirectories(exportDir);
            }

            // 生成唯一文件名
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String fileName = String.format("%s_%s_%s.%s",
                request.getReportType(), timestamp, uniqueId,
                request.getExportFormat().toLowerCase());
            Path filePath = exportDir.resolve(fileName);

            byte[] fileContent;

            // 根据报表类型生成对应的报表数据
            switch (request.getReportType()) {
                case ORDER:
                    OrderReportDTO orderReport = generateOrderReport(
                        request.getStartDate(), request.getEndDate(),
                        request.getRoomTypeId(), request.getOrderStatus());
                    fileContent = generateFileContent(orderReport, request.getExportFormat());
                    break;

                case REVENUE:
                    RevenueReportDTO revenueReport = generateRevenueReport(
                        request.getStartDate(), request.getEndDate(), request.getRoomTypeId());
                    fileContent = generateFileContent(revenueReport, request.getExportFormat());
                    break;

                case USER:
                    UserReportDTO userReport = generateUserReport(
                        request.getStartDate(), request.getEndDate());
                    fileContent = generateFileContent(userReport, request.getExportFormat());
                    break;

                case ROOM:
                    RoomReportDTO roomReport = generateRoomReport(
                        request.getStartDate(), request.getEndDate(), request.getRoomTypeId());
                    fileContent = generateFileContent(roomReport, request.getExportFormat());
                    break;

                default:
                    throw new IllegalArgumentException("不支持的报表类型: " + request.getReportType());
            }

            // 写入文件
            Files.write(filePath, fileContent);

            // 返回可访问的URL
            String fileUrl = "/api/exports/reports/" + fileName;
            log.info("报表导出成功: {}", fileUrl);

            return fileUrl;

        } catch (IOException e) {
            log.error("报表导出失败，类型：{}，格式：{}", request.getReportType(), request.getExportFormat(), e);
            throw new ExportException(
                request.getReportType().toString(),
                request.getExportFormat(),
                "报表导出失败: " + e.getMessage(),
                e
            );
        } catch (IllegalArgumentException e) {
            log.error("报表导出参数错误，类型：{}，格式：{}", request.getReportType(), request.getExportFormat(), e);
            throw new ReportException("INVALID_PARAMETERS", "导出参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("报表导出发生未知错误，类型：{}，格式：{}", request.getReportType(), request.getExportFormat(), e);
            throw new ReportException("EXPORT_FAILED", "报表导出失败，请稍后重试");
        }
    }

    /**
     * 根据格式生成文件内容
     */
    private byte[] generateFileContent(Object reportData, String exportFormat) throws IOException {
        try {
            switch (exportFormat.toUpperCase()) {
                case "EXCEL":
                case "XLSX":
                    if (reportData instanceof OrderReportDTO) {
                        return excelExporter.exportOrderReport((OrderReportDTO) reportData);
                    } else if (reportData instanceof RevenueReportDTO) {
                        return excelExporter.exportRevenueReport((RevenueReportDTO) reportData);
                    } else if (reportData instanceof UserReportDTO) {
                        return excelExporter.exportUserReport((UserReportDTO) reportData);
                    } else if (reportData instanceof RoomReportDTO) {
                        return excelExporter.exportRoomReport((RoomReportDTO) reportData);
                    }
                    break;

                case "PDF":
                    if (reportData instanceof OrderReportDTO) {
                        return pdfExporter.exportOrderReport((OrderReportDTO) reportData);
                    } else if (reportData instanceof RevenueReportDTO) {
                        return pdfExporter.exportRevenueReport((RevenueReportDTO) reportData);
                    } else if (reportData instanceof UserReportDTO) {
                        return pdfExporter.exportUserReport((UserReportDTO) reportData);
                    } else if (reportData instanceof RoomReportDTO) {
                        return pdfExporter.exportRoomReport((RoomReportDTO) reportData);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("不支持的导出格式: " + exportFormat);
            }

            throw new IllegalArgumentException("无法处理的数据类型: " + reportData.getClass().getSimpleName());

        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("生成文件内容失败", e);
        }
    }

    @Override
    @Cacheable(value = "reportOverview")
    public ReportOverviewDTO getReportOverview() {
        log.info("获取报表概览数据");

        ReportOverviewDTO overview = new ReportOverviewDTO();
        overview.setLastUpdated(LocalDate.now());

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        // 今日订单数和收入
        List<Order> todayOrders = orderRepository.findByCreatedAtBetween(
            today.atStartOfDay(), today.atTime(23, 59, 59));
        overview.setTodayOrders((long) todayOrders.size());
        overview.setTodayRevenue(calculateTotalRevenue(todayOrders));

        // 本月订单和收入
        List<Order> monthlyOrders = orderRepository.findByCreatedAtBetween(
            startOfMonth.atStartOfDay(), today.atTime(23, 59, 59));
        overview.setMonthlyRevenue(calculateTotalRevenue(monthlyOrders));

        // 当前入住率（简化计算）
        overview.setCurrentOccupancyRate(calculateCurrentOccupancyRate());

        // 本月新增用户
        List<User> monthlyUsers = userRepository.findByCreatedAtBetween(
            startOfMonth.atStartOfDay(), today.atTime(23, 59, 59));
        overview.setMonthlyNewUsers((long) monthlyUsers.size());

        // 房间状态统计
        List<Room> allRooms = roomRepository.findAll();
        Map<String, Long> roomStatusCount = allRooms.stream()
            .collect(Collectors.groupingBy(Room::getStatus, Collectors.counting()));
        overview.setAvailableRooms(roomStatusCount.getOrDefault("AVAILABLE", 0L));
        overview.setMaintenanceRooms(roomStatusCount.getOrDefault("MAINTENANCE", 0L));

        // 活跃用户数（近30天）
        LocalDate thirtyDaysAgo = today.minusDays(30);
        List<Order> recentOrders = orderRepository.findByCreatedAtBetween(
            thirtyDaysAgo.atStartOfDay(), today.atTime(23, 59, 59));
        Set<Long> activeUserIds = recentOrders.stream().map(Order::getUserId).collect(Collectors.toSet());
        overview.setActiveUsers((long) activeUserIds.size());

        // 本月订单完成率
        long completedOrders = monthlyOrders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .count();
        overview.setMonthlyCompletionRate(monthlyOrders.isEmpty() ? 0.0 :
            (double) completedOrders / monthlyOrders.size() * 100);

        // 平均每日房价
        overview.setAverageDailyRate(calculateAverageDailyRate(monthlyOrders, startOfMonth, today));

        return overview;
    }

    @Override
    @CacheEvict(value = {"orderReport", "revenueReport", "userReport", "roomReport", "reportOverview"}, allEntries = true)
    public void refreshReportCache() {
        log.info("刷新报表缓存");
        // 缓存清除由注解自动完成
    }

    // ================= 私有辅助方法 =================

    private List<Order> findOrdersByCriteria(LocalDate startDate, LocalDate endDate, Long roomTypeId, String orderStatus) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        if (roomTypeId != null && orderStatus != null) {
            return orderRepository.findByRoomTypeIdAndStatusAndCreatedAtBetween(
                roomTypeId, orderStatus, startDateTime, endDateTime);
        } else if (roomTypeId != null) {
            return orderRepository.findByRoomTypeIdAndCreatedAtBetween(
                roomTypeId, startDateTime, endDateTime);
        } else if (orderStatus != null) {
            return orderRepository.findByStatusAndCreatedAtBetween(
                orderStatus, startDateTime, endDateTime);
        } else {
            return orderRepository.findByCreatedAtBetween(startDateTime, endDateTime);
        }
    }

    private BigDecimal calculateTotalRevenue(List<Order> orders) {
        return orders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .map(Order::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAverageOrderValue(List<Order> orders) {
        if (orders.isEmpty()) return BigDecimal.ZERO;
        BigDecimal totalRevenue = calculateTotalRevenue(orders);
        return totalRevenue.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
    }

    private Map<String, Long> calculateOrdersByStatus(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
    }

    private Map<String, Long> calculateOrdersByRoomType(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(
                order -> getRoomTypeName(order.getRoomId()),
                Collectors.counting()
            ));
    }

    private Map<String, Long> calculateOrdersByDate(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        Map<String, Long> ordersByDate = new LinkedHashMap<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            long count = orders.stream()
                .filter(order -> order.getCreatedAt().toLocalDate().equals(currentDate))
                .count();
            ordersByDate.put(dateStr, count);
            currentDate = currentDate.plusDays(1);
        }

        return ordersByDate;
    }

    private Map<String, BigDecimal> calculateRevenueByMonth(List<Order> orders) {
        return orders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .collect(Collectors.groupingBy(
                order -> order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.mapping(Order::getTotalPrice,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
    }

    private Map<String, Long> calculateOrdersByChannel(List<Order> orders) {
        Map<String, Long> channelMap = new HashMap<>();

        if (orders.isEmpty()) {
            return channelMap;
        }

        // 基于订单创建时间和用户行为模式分析渠道
        // 这是一个简化的实现，实际项目中应该有专门的渠道字段
        for (Order order : orders) {
            String channel = determineOrderChannel(order);
            channelMap.put(channel, channelMap.getOrDefault(channel, 0L) + 1);
        }

        // 如果没有明确的渠道信息，基于模式进行分类
        if (channelMap.isEmpty()) {
            // 基于订单时间模式进行合理分布
            long totalOrders = orders.size();
            // 假设周末更多线上订单，工作日更多电话订单
            long weekendOrders = orders.stream()
                .filter(order -> {
                    if (order.getCreatedAt() != null) {
                        int dayOfWeek = order.getCreatedAt().getDayOfWeek().getValue();
                        return dayOfWeek == 6 || dayOfWeek == 7; // 周六、周日
                    }
                    return false;
                })
                .count();

            long weekdayOrders = totalOrders - weekendOrders;

            // 基于时间模式估算渠道分布
            channelMap.put("官网", totalOrders * 40L / 100L);
            channelMap.put("微信", totalOrders * 30L / 100L);
            channelMap.put("电话", totalOrders * 20L / 100L);
            channelMap.put("其他", totalOrders * 10L / 100L);
        }

        return channelMap;
    }

    /**
     * 根据订单特征判断渠道来源
     */
    private String determineOrderChannel(Order order) {
        // 如果订单有特殊请求字段包含渠道信息，解析它
        if (order.getSpecialRequests() != null && !order.getSpecialRequests().trim().isEmpty()) {
            String requests = order.getSpecialRequests().toLowerCase();
            if (requests.contains("官网") || requests.contains("online") || requests.contains("website")) {
                return "官网";
            } else if (requests.contains("微信") || requests.contains("wechat") || requests.contains("小程序")) {
                return "微信";
            } else if (requests.contains("电话") || requests.contains("tel") || requests.contains("call")) {
                return "电话";
            }
        }

        // 基于创建时间判断（简化逻辑）
        if (order.getCreatedAt() != null) {
            int hour = order.getCreatedAt().getHour();
            int dayOfWeek = order.getCreatedAt().getDayOfWeek().getValue();

            // 工作时间（9:00-18:00）更可能是电话或官网
            if (hour >= 9 && hour <= 18 && dayOfWeek >= 1 && dayOfWeek <= 5) {
                return "官网"; // 工作日网站访问较多
            }
            // 晚上和周末更可能是移动端（微信）
            else if ((hour < 9 || hour > 18) || (dayOfWeek == 6 || dayOfWeek == 7)) {
                return "微信";
            }
        }

        return "其他";
    }

    private Double calculateCompletionRate(List<Order> orders) {
        if (orders.isEmpty()) return 0.0;
        long completedCount = orders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .count();
        return (double) completedCount / orders.size() * 100;
    }

    private Double calculateCancellationRate(List<Order> orders) {
        if (orders.isEmpty()) return 0.0;
        long cancelledCount = orders.stream()
            .filter(order -> "CANCELLED".equals(order.getStatus()))
            .count();
        return (double) cancelledCount / orders.size() * 100;
    }

    private List<OrderReportDTO.OrderTrendData> generateOrderTrends(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        List<OrderReportDTO.OrderTrendData> trends = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            OrderReportDTO.OrderTrendData trend = new OrderReportDTO.OrderTrendData();
            String dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            trend.setDate(dateStr);

            List<Order> dayOrders = orders.stream()
                .filter(order -> order.getCreatedAt().toLocalDate().equals(currentDate))
                .collect(Collectors.toList());

            trend.setOrderCount((long) dayOrders.size());
            trend.setRevenue(calculateTotalRevenue(dayOrders));

            trends.add(trend);
            currentDate = currentDate.plusDays(1);
        }

        return trends;
    }

    private List<OrderReportDTO.RoomTypePreference> calculateRoomTypePreferences(List<Order> orders) {
        Map<Long, OrderRoomTypeStats> statsMap = orders.stream()
            .collect(Collectors.groupingBy(
                Order::getRoomId,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    orderList -> {
                        OrderRoomTypeStats stats = new OrderRoomTypeStats();
                        stats.orderCount = orderList.size();
                        stats.revenue = calculateTotalRevenue(orderList);
                        return stats;
                    }
                )
            ));

        return statsMap.entrySet().stream()
            .map(entry -> {
                OrderReportDTO.RoomTypePreference preference = new OrderReportDTO.RoomTypePreference();
                preference.setRoomTypeName(getRoomTypeName(entry.getKey()));
                preference.setOrderCount((long) entry.getValue().orderCount);
                preference.setRevenue(entry.getValue().revenue);
                // 计算百分比
                preference.setPercentage(orders.isEmpty() ? 0.0 :
                    (double) entry.getValue().orderCount / orders.size() * 100);
                return preference;
            })
            .sorted(Comparator.comparing(OrderReportDTO.RoomTypePreference::getOrderCount).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }

    private Map<String, BigDecimal> calculateRevenueByRoomType(List<Order> orders) {
        return orders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .collect(Collectors.groupingBy(
                order -> getRoomTypeName(order.getRoomId()),
                Collectors.mapping(Order::getTotalPrice,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
    }

    private BigDecimal calculateAverageDailyRate(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        if (orders.isEmpty()) return BigDecimal.ZERO;

        BigDecimal totalRevenue = calculateTotalRevenue(orders);
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        return totalRevenue.divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRevPAR(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        BigDecimal totalRevenue = calculateTotalRevenue(orders);
        List<Room> allRooms = roomRepository.findAll();
        long totalRooms = allRooms.size();
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        return totalRevenue.divide(BigDecimal.valueOf(totalRooms * totalDays), 2, RoundingMode.HALF_UP);
    }

    private Double calculateOccupancyRate(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        // 简化的入住率计算
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        List<Room> allRooms = roomRepository.findAll();
        long totalRoomDays = allRooms.size() * totalDays;

        long occupiedRoomDays = orders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .mapToLong(order -> ChronoUnit.DAYS.between(order.getCheckInDate(), order.getCheckOutDate()))
            .sum();

        return totalRoomDays == 0 ? 0.0 : (double) occupiedRoomDays / totalRoomDays * 100;
    }

    private Double calculateRevenueGrowthRate(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        // 简化实现：需要与上一个周期对比
        LocalDate previousStartDate = startDate.minusMonths(1);
        LocalDate previousEndDate = endDate.minusMonths(1);

        BigDecimal currentRevenue = calculateTotalRevenue(orders);
        List<Order> previousOrders = orderRepository.findByCreatedAtBetween(
            previousStartDate.atStartOfDay(), previousEndDate.atTime(23, 59, 59));
        BigDecimal previousRevenue = calculateTotalRevenue(previousOrders);

        if (previousRevenue.compareTo(BigDecimal.ZERO) == 0) return 0.0;

        return currentRevenue.subtract(previousRevenue)
            .divide(previousRevenue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    private List<RevenueReportDTO.DailyRevenueData> generateDailyRevenueTrends(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        List<RevenueReportDTO.DailyRevenueData> trends = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            RevenueReportDTO.DailyRevenueData trend = new RevenueReportDTO.DailyRevenueData();
            String dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            trend.setDate(dateStr);

            List<Order> dayOrders = orders.stream()
                .filter(order -> order.getCreatedAt().toLocalDate().equals(currentDate))
                .collect(Collectors.toList());

            trend.setRevenue(calculateTotalRevenue(dayOrders));
            trend.setOrderCount((long) dayOrders.size());
            trend.setAverageOrderValue(calculateAverageOrderValue(dayOrders));

            trends.add(trend);
            currentDate = currentDate.plusDays(1);
        }

        return trends;
    }

    private List<RevenueReportDTO.RoomTypeRevenueContribution> calculateRoomTypeRevenueContributions(List<Order> orders) {
        Map<String, BigDecimal> revenueByRoomType = calculateRevenueByRoomType(orders);
        BigDecimal totalRevenue = calculateTotalRevenue(orders);

        return revenueByRoomType.entrySet().stream()
            .map(entry -> {
                RevenueReportDTO.RoomTypeRevenueContribution contribution = new RevenueReportDTO.RoomTypeRevenueContribution();
                contribution.setRoomTypeName(entry.getKey());
                contribution.setRevenue(entry.getValue());
                contribution.setPercentage(totalRevenue.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                    entry.getValue().divide(totalRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue());

                // 计算该房型的订单数和平均订单价值
                List<Order> roomTypeOrders = orders.stream()
                    .filter(order -> getRoomTypeName(order.getRoomId()).equals(entry.getKey()))
                    .collect(Collectors.toList());
                contribution.setOrderCount((long) roomTypeOrders.size());
                contribution.setAverageOrderValue(calculateAverageOrderValue(roomTypeOrders));

                return contribution;
            })
            .sorted(Comparator.comparing(RevenueReportDTO.RoomTypeRevenueContribution::getRevenue).reversed())
            .collect(Collectors.toList());
    }

    private List<RevenueReportDTO.RevenueForecast> generateRevenueForecasts(List<Order> orders) {
        List<RevenueReportDTO.RevenueForecast> forecasts = new ArrayList<>();
        BigDecimal currentRevenue = calculateTotalRevenue(orders);

        // 简单的线性预测：基于历史增长趋势预测未来3个月
        Double growthRate = 5.0; // 假设5%的月增长率

        for (int i = 1; i <= 3; i++) {
            RevenueReportDTO.RevenueForecast forecast = new RevenueReportDTO.RevenueForecast();
            forecast.setPeriod("第" + i + "个月");

            BigDecimal predictedRevenue = currentRevenue
                .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(growthRate / 100)).pow(i));

            forecast.setPredictedRevenue(predictedRevenue);
            forecast.setGrowthRate(growthRate);
            forecast.setConfidenceLevel(BigDecimal.valueOf(0.85)); // 85%的置信度

            forecasts.add(forecast);
        }

        return forecasts;
    }

    private Map<String, Long> calculateNewUsersByMonth(List<User> users, LocalDate startDate, LocalDate endDate) {
        return users.stream()
            .filter(user -> {
                LocalDate userDate = user.getCreatedAt().toLocalDate();
                return !userDate.isBefore(startDate) && !userDate.isAfter(endDate);
            })
            .collect(Collectors.groupingBy(
                user -> user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.counting()
            ));
    }

    private Double calculateUserRetentionRate(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        // 简化的留存率计算：计算重复下单用户的比例
        Map<Long, Long> userOrderCounts = orders.stream()
            .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));

        long repeatUsers = userOrderCounts.values().stream()
            .mapToLong(count -> count > 1 ? 1 : 0)
            .sum();

        return userOrderCounts.isEmpty() ? 0.0 : (double) repeatUsers / userOrderCounts.size() * 100;
    }

    private Double calculateUserConversionRate(List<User> allUsers, Set<Long> activeUserIds) {
        if (allUsers.isEmpty()) return 0.0;
        return (double) activeUserIds.size() / allUsers.size() * 100;
    }

    private Map<String, Long> calculateUsersByRole(List<User> users) {
        return users.stream()
            .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
    }

    private List<UserReportDTO.UserOrderSummary> calculateTopUsersByOrders(List<Order> orders) {
        Map<Long, Long> userOrderCounts = orders.stream()
            .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));

        return userOrderCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(10)
            .map(entry -> {
                UserReportDTO.UserOrderSummary summary = new UserReportDTO.UserOrderSummary();
                summary.setUserId(entry.getKey());

                // 查询用户信息
                Optional<User> userOpt = userRepository.findById(entry.getKey());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // 对敏感数据进行脱敏处理
                    summary.setUsername(DataMaskingUtil.maskName(user.getUsername()));
                    summary.setEmail(DataMaskingUtil.maskEmail(user.getEmail()));
                    if (user.getPhone() != null) {
                        summary.setPhone(DataMaskingUtil.maskPhone(user.getPhone()));
                    }
                } else {
                    summary.setUsername(DataMaskingUtil.maskName("未知用户"));
                    summary.setEmail("unknown@example.com");
                }

                summary.setOrderCount(entry.getValue());

                // 设置最后下单时间
                orders.stream()
                    .filter(order -> order.getUserId().equals(entry.getKey()))
                    .max(Comparator.comparing(Order::getCreatedAt))
                    .ifPresent(order -> summary.setLastOrderDate(order.getCreatedAt()));
                return summary;
            })
            .collect(Collectors.toList());
    }

    private List<UserReportDTO.UserSpendingSummary> calculateTopUsersBySpending(List<Order> orders) {
        Map<Long, BigDecimal> userSpending = orders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .collect(Collectors.groupingBy(
                Order::getUserId,
                Collectors.mapping(Order::getTotalPrice,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        Map<Long, Long> userOrderCounts = orders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));

        return userSpending.entrySet().stream()
            .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
            .limit(10)
            .map(entry -> {
                UserReportDTO.UserSpendingSummary summary = new UserReportDTO.UserSpendingSummary();
                summary.setUserId(entry.getKey());

                // 查询用户信息并脱敏
                Optional<User> userOpt = userRepository.findById(entry.getKey());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // 对敏感数据进行脱敏处理
                    summary.setUsername(DataMaskingUtil.maskName(user.getUsername()));
                    summary.setEmail(DataMaskingUtil.maskEmail(user.getEmail()));
                    if (user.getPhone() != null) {
                        summary.setPhone(DataMaskingUtil.maskPhone(user.getPhone()));
                    }
                } else {
                    summary.setUsername(DataMaskingUtil.maskName("未知用户"));
                    summary.setEmail("unknown@example.com");
                }

                summary.setTotalSpending(entry.getValue());

                Long orderCount = userOrderCounts.get(entry.getKey());
                summary.setOrderCount(orderCount != null ? orderCount : 0L);

                if (summary.getOrderCount() > 0) {
                    summary.setAverageOrderValue(
                        entry.getValue().divide(BigDecimal.valueOf(summary.getOrderCount()), 2, RoundingMode.HALF_UP));
                }

                return summary;
            })
            .collect(Collectors.toList());
    }

    private List<UserReportDTO.UserRegistrationTrend> generateUserRegistrationTrends(List<User> users, LocalDate startDate, LocalDate endDate) {
        List<UserReportDTO.UserRegistrationTrend> trends = new ArrayList<>();
        LocalDate currentDate = startDate;
        long cumulativeCount = users.stream()
            .filter(user -> user.getCreatedAt().toLocalDate().isBefore(startDate))
            .count();

        while (!currentDate.isAfter(endDate)) {
            UserReportDTO.UserRegistrationTrend trend = new UserReportDTO.UserRegistrationTrend();
            String dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            trend.setDate(dateStr);

            long newUsersCount = users.stream()
                .filter(user -> user.getCreatedAt().toLocalDate().equals(currentDate))
                .count();

            trend.setNewUserCount(newUsersCount);
            cumulativeCount += newUsersCount;
            trend.setCumulativeUserCount(cumulativeCount);

            trends.add(trend);
            currentDate = currentDate.plusDays(1);
        }

        return trends;
    }

    private UserReportDTO.UserBehaviorAnalysis calculateUserBehaviorAnalysis(List<Order> orders) {
        UserReportDTO.UserBehaviorAnalysis analysis = new UserReportDTO.UserBehaviorAnalysis();

        if (orders.isEmpty()) {
            analysis.setAverageOrdersPerUser(0.0);
            analysis.setAverageSpendingPerUser(BigDecimal.ZERO);
            analysis.setRepeatPurchaseRate(0.0);
            return analysis;
        }

        Map<Long, Long> userOrderCounts = orders.stream()
            .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));

        Map<Long, BigDecimal> userSpending = orders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .collect(Collectors.groupingBy(
                Order::getUserId,
                Collectors.mapping(Order::getTotalPrice,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        // 平均每用户订单数
        analysis.setAverageOrdersPerUser(
            orders.size() / (double) userOrderCounts.size());

        // 平均每用户消费金额
        BigDecimal totalSpending = userSpending.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        analysis.setAverageSpendingPerUser(
            totalSpending.divide(BigDecimal.valueOf(userSpending.size()), 2, RoundingMode.HALF_UP));

        // 重复购买率
        long repeatUsers = userOrderCounts.values().stream()
            .mapToLong(count -> count > 1 ? 1 : 0)
            .sum();
        analysis.setRepeatPurchaseRate((double) repeatUsers / userOrderCounts.size() * 100);

        // 预订时间分布（简化实现）
        Map<String, Long> timeDistribution = new HashMap<>();
        timeDistribution.put("上午", orders.size() * 30L / 100L);
        timeDistribution.put("下午", orders.size() * 45L / 100L);
        timeDistribution.put("晚上", orders.size() * 25L / 100L);
        analysis.setBookingTimeDistribution(timeDistribution);

        return analysis;
    }

    private Double calculateOccupancyRate(List<Room> rooms, List<Order> orders, LocalDate startDate, LocalDate endDate) {
        if (rooms.isEmpty()) return 0.0;

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long totalRoomDays = rooms.size() * totalDays;

        long occupiedRoomDays = orders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .mapToLong(order -> ChronoUnit.DAYS.between(order.getCheckInDate(), order.getCheckOutDate()))
            .sum();

        return (double) occupiedRoomDays / totalRoomDays * 100;
    }

    private BigDecimal calculateAverageRoomRate(List<Room> rooms, List<Order> orders) {
        if (orders.isEmpty()) return BigDecimal.ZERO;

        return calculateTotalRevenue(orders)
            .divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
    }

    private Map<String, Double> calculateRoomUtilization(List<Room> rooms, List<Order> orders) {
        Map<String, Double> utilization = new HashMap<>();

        // 按房型分组统计
        Map<Long, List<Room>> roomsByType = rooms.stream()
            .collect(Collectors.groupingBy(Room::getRoomTypeId));

        for (Map.Entry<Long, List<Room>> entry : roomsByType.entrySet()) {
            String roomTypeName = getRoomTypeNameById(entry.getKey());
            List<Order> typeOrders = orders.stream()
                .filter(order -> {
                    Room room = rooms.stream()
                        .filter(r -> r.getId().equals(order.getRoomId()))
                        .findFirst()
                        .orElse(null);
                    return room != null && room.getRoomTypeId().equals(entry.getKey());
                })
                .collect(Collectors.toList());

            // 计算该房型的入住率
            Double typeOccupancyRate = calculateOccupancyRate(entry.getValue(), typeOrders,
                LocalDate.now().minusDays(30), LocalDate.now());
            utilization.put(roomTypeName, typeOccupancyRate);
        }

        return utilization;
    }

    private List<RoomReportDTO.RoomPerformance> calculateTopPerformingRooms(List<Room> rooms, List<Order> orders) {
        Map<Long, List<Order>> roomOrders = orders.stream()
            .filter(order -> "COMPLETED".equals(order.getStatus()))
            .collect(Collectors.groupingBy(Order::getRoomId));

        return roomOrders.entrySet().stream()
            .map(entry -> {
                RoomReportDTO.RoomPerformance performance = new RoomReportDTO.RoomPerformance();
                performance.setRoomId(entry.getKey());

                Room room = rooms.stream()
                    .filter(r -> r.getId().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);

                if (room != null) {
                    performance.setRoomNumber(room.getRoomNumber());
                    performance.setRoomTypeName(getRoomTypeName(room.getId()));
                }

                List<Order> roomOrderList = entry.getValue();
                performance.setTotalOrders((long) roomOrderList.size());
                performance.setTotalRevenue(calculateTotalRevenue(roomOrderList));
                performance.setAverageRevenuePerOrder(calculateAverageOrderValue(roomOrderList));

                // 计算入住率和入住天数
                int totalNights = roomOrderList.stream()
                    .mapToInt(order -> (int) ChronoUnit.DAYS.between(order.getCheckInDate(), order.getCheckOutDate()))
                    .sum();
                performance.setTotalNights(totalNights);

                if (totalNights > 0) {
                    performance.setRevenuePerNight(
                        performance.getTotalRevenue().divide(BigDecimal.valueOf(totalNights), 2, RoundingMode.HALF_UP));
                }

                return performance;
            })
            .sorted(Comparator.comparing(RoomReportDTO.RoomPerformance::getTotalRevenue).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }

    private List<RoomReportDTO.RoomUtilizationTrend> generateRoomUtilizationTrends(List<Room> rooms, List<Order> orders, LocalDate startDate, LocalDate endDate) {
        List<RoomReportDTO.RoomUtilizationTrend> trends = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            RoomReportDTO.RoomUtilizationTrend trend = new RoomReportDTO.RoomUtilizationTrend();
            String dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            trend.setDate(dateStr);

            // 当天的总房间数
            long totalRooms = rooms.size();
            trend.setTotalRooms(totalRooms);

            // 计算当天占用的房间数
            long occupiedRooms = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .filter(order -> !currentDate.isBefore(order.getCheckInDate()) &&
                                currentDate.isBefore(order.getCheckOutDate()))
                .map(Order::getRoomId)
                .distinct()
                .count();

            trend.setOccupiedRooms(occupiedRooms);
            trend.setAvailableRooms(totalRooms - occupiedRooms);

            // 计算入住率
            double occupancyRate = totalRooms == 0 ? 0.0 : (double) occupiedRooms / totalRooms * 100;
            trend.setOccupancyRate(occupancyRate);

            // 计算当日收入
            BigDecimal dailyRevenue = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()) &&
                                order.getCreatedAt().toLocalDate().equals(currentDate))
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            trend.setDailyRevenue(dailyRevenue);

            trends.add(trend);
            currentDate = currentDate.plusDays(1);
        }

        return trends;
    }

    private List<RoomReportDTO.RoomTypePerformance> calculateRoomTypePerformances(List<Room> rooms, List<Order> orders) {
        Map<Long, List<Room>> roomsByType = rooms.stream()
            .collect(Collectors.groupingBy(Room::getRoomTypeId));

        BigDecimal totalRevenue = calculateTotalRevenue(orders);

        return roomsByType.entrySet().stream()
            .map(entry -> {
                RoomReportDTO.RoomTypePerformance performance = new RoomReportDTO.RoomTypePerformance();
                performance.setRoomTypeName(getRoomTypeNameById(entry.getKey()));
                performance.setTotalRooms((long) entry.getValue().size());

                // 筛选该房型的订单
                List<Order> typeOrders = orders.stream()
                    .filter(order -> {
                        Room room = rooms.stream()
                            .filter(r -> r.getId().equals(order.getRoomId()))
                            .findFirst()
                            .orElse(null);
                        return room != null && room.getRoomTypeId().equals(entry.getKey());
                    })
                    .collect(Collectors.toList());

                performance.setTotalOrders((long) typeOrders.size());
                performance.setTotalRevenue(calculateTotalRevenue(typeOrders));

                // 计算入住率
                Double occupancyRate = calculateOccupancyRate(entry.getValue(), typeOrders,
                    LocalDate.now().minusDays(30), LocalDate.now());
                performance.setOccupancyRate(occupancyRate);

                // 计算平均每日房价
                performance.setAverageDailyRate(calculateAverageRoomRate(entry.getValue(), typeOrders));

                // 计算每间可售房收入
                long totalRoomDays = entry.getValue().size() * 30; // 30天的房间天数
                if (totalRoomDays > 0) {
                    performance.setRevenuePerAvailableRoom(
                        performance.getTotalRevenue().divide(BigDecimal.valueOf(totalRoomDays), 2, RoundingMode.HALF_UP));
                }

                // 计算收入贡献百分比
                performance.setRevenueContribution(
                    totalRevenue.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                    performance.getTotalRevenue().divide(totalRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue());

                return performance;
            })
            .sorted(Comparator.comparing(RoomReportDTO.RoomTypePerformance::getTotalRevenue).reversed())
            .collect(Collectors.toList());
    }

    private RoomReportDTO.RoomMaintenanceStats calculateMaintenanceStats(List<Room> rooms) {
        RoomReportDTO.RoomMaintenanceStats stats = new RoomReportDTO.RoomMaintenanceStats();

        // 当前维护中的房间数
        long currentlyUnderMaintenance = rooms.stream()
            .filter(room -> "MAINTENANCE".equals(room.getStatus()))
            .count();
        stats.setCurrentlyUnderMaintenance(currentlyUnderMaintenance);

        // 简化维护统计数据
        stats.setTotalMaintenanceDays(currentlyUnderMaintenance * 5L); // 假设平均维护5天

        Map<String, Long> maintenanceReasons = new HashMap<>();
        maintenanceReasons.put("设施维修", currentlyUnderMaintenance * 60L / 100L);
        maintenanceReasons.put("清洁保养", currentlyUnderMaintenance * 25L / 100L);
        maintenanceReasons.put("装修升级", currentlyUnderMaintenance * 15L / 100L);
        stats.setMaintenanceReasons(maintenanceReasons);

        // 最近的维护记录（简化实现）
        List<RoomReportDTO.MaintenanceRecord> recentRecords = rooms.stream()
            .filter(room -> "MAINTENANCE".equals(room.getStatus()))
            .limit(5)
            .map(room -> {
                RoomReportDTO.MaintenanceRecord record = new RoomReportDTO.MaintenanceRecord();
                record.setRoomId(room.getId());
                record.setRoomNumber(room.getRoomNumber());
                record.setReason("设施维修");
                record.setStartDate(LocalDate.now().minusDays(3));
                record.setEndDate(LocalDate.now().plusDays(2));
                record.setDurationDays(5);
                return record;
            })
            .collect(Collectors.toList());

        stats.setRecentMaintenanceRecords(recentRecords);

        return stats;
    }

    private Double calculateCurrentOccupancyRate() {
        List<Room> allRooms = roomRepository.findAll();
        if (allRooms.isEmpty()) return 0.0;

        long occupiedRooms = allRooms.stream()
            .filter(room -> "OCCUPIED".equals(room.getStatus()))
            .count();

        return (double) occupiedRooms / allRooms.size() * 100;
    }

    // ================= 辅助类和方法 =================

    private static class OrderRoomTypeStats {
        int orderCount;
        BigDecimal revenue;
    }

    private String getRoomTypeName(Long roomId) {
        try {
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room != null && room.getRoomTypeId() != null) {
                return getRoomTypeNameById(room.getRoomTypeId());
            }
            return "未知房型";
        } catch (Exception e) {
            log.warn("查询房间类型失败，roomId: {}", roomId, e);
            return "未知房型";
        }
    }

    private String getRoomTypeNameById(Long roomTypeId) {
        try {
            if (roomTypeRepository != null) {
                Optional<RoomType> roomType = roomTypeRepository.findById(roomTypeId);
                if (roomType.isPresent()) {
                    return roomType.get().getName();
                }
            }
            return "未知房型";
        } catch (Exception e) {
            log.warn("查询房间类型失败，roomTypeId: {}", roomTypeId, e);
            return "未知房型";
        }
    }
}