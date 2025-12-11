package com.hotel.service;

import com.hotel.dto.admin.chart.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图表数据格式化服务
 * 将各种数据转换为前端图表库需要的格式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChartDataFormatterService {

    private final TrendAnalysisService trendAnalysisService;
    private final RevenueAnalyticsService revenueAnalyticsService;
    private final OccupancyAnalyticsService occupancyAnalyticsService;

    /**
     * 格式化订单趋势图表数据
     */
    @Cacheable(value = "chart:orders:trends", key = "#startDate + '_' + #endDate + '_' + #period", unless = "#result == null")
    public OrderTrendChartDTO formatOrderTrendsChart(LocalDate startDate, LocalDate endDate, String period) {
        log.info("格式化订单趋势图表数据，时间段: {} 至 {}，周期: {}", startDate, endDate, period);

        try {
            Map<String, Object> trendData;

            switch (period.toLowerCase()) {
                case "daily":
                    trendData = trendAnalysisService.getOrderTrendsDaily(startDate, endDate);
                    break;
                case "weekly":
                    trendData = trendAnalysisService.getOrderTrendsWeekly(startDate, endDate);
                    break;
                case "monthly":
                    trendData = trendAnalysisService.getOrderTrendsMonthly(startDate, endDate);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的周期: " + period);
            }

            OrderTrendChartDTO chartData = new OrderTrendChartDTO();

            // X轴数据
            chartData.setCategories(getLabelsFromData(trendData, period));

            // 订单数量系列
            ChartSeries ordersSeries = new ChartSeries();
            ordersSeries.setName("订单数量");
            ordersSeries.setData(getIntegerDataFromData(trendData, "orderCounts"));
            ordersSeries.setColor("#1890ff");
            ordersSeries.setType("line");
            chartData.getSeries().add(ordersSeries);

            // 收入系列
            ChartSeries revenueSeries = new ChartSeries();
            revenueSeries.setName("收入 (元)");
            revenueSeries.setData(getBigDecimalDataFromData(trendData, "revenues"));
            revenueSeries.setColor("#52c41a");
            revenueSeries.setType("column");
            chartData.getSeries().add(revenueSeries);

            // 待处理订单系列
            ChartSeries pendingSeries = new ChartSeries();
            pendingSeries.setName("待处理");
            pendingSeries.setData(getIntegerDataFromData(trendData, "pendingOrders"));
            pendingSeries.setColor("#faad14");
            pendingSeries.setType("line");
            chartData.getSeries().add(pendingSeries);

            // 已完成订单系列
            ChartSeries completedSeries = new ChartSeries();
            completedSeries.setName("已完成");
            completedSeries.setData(getIntegerDataFromData(trendData, "completedOrders"));
            completedSeries.setColor("#52c41a");
            completedSeries.setType("line");
            chartData.getSeries().add(completedSeries);

            chartData.setTitle("订单趋势分析");
            chartData.setSubtitle(String.format("%s 至 %s",
                startDate.format(DateTimeFormatter.ISO_DATE),
                endDate.format(DateTimeFormatter.ISO_DATE)));

            log.info("订单趋势图表数据格式化完成");
            return chartData;

        } catch (Exception e) {
            log.error("格式化订单趋势图表数据失败", e);
            throw new RuntimeException("格式化订单趋势图表数据失败: " + e.getMessage());
        }
    }

    /**
     * 格式化收入分析图表数据
     */
    @Cacheable(value = "chart:revenue:analysis", key = "#startDate + '_' + #endDate + '_' + #type", unless = "#result == null")
    public RevenueChartDTO formatRevenueAnalysisChart(LocalDate startDate, LocalDate endDate, String type) {
        log.info("格式化收入分析图表数据，时间段: {} 至 {}，类型: {}", startDate, endDate, type);

        try {
            RevenueChartDTO chartData = new RevenueChartDTO();

            switch (type.toLowerCase()) {
                case "trends":
                    Map<String, Object> trendData = revenueAnalyticsService.getRevenueTrendsDaily(startDate, endDate);
                    chartData.setCategories(getLabelsFromData(trendData, "daily"));

                    // 收入趋势
                    ChartSeries revenueSeries = new ChartSeries();
                    revenueSeries.setName("日收入");
                    revenueSeries.setData(getBigDecimalDataFromData(trendData, "revenues"));
                    revenueSeries.setColor("#1890ff");
                    revenueSeries.setType("line");
                    chartData.getSeries().add(revenueSeries);

                    // 平均订单价值
                    ChartSeries avgSeries = new ChartSeries();
                    avgSeries.setName("平均订单价值");
                    avgSeries.setData(getBigDecimalDataFromData(trendData, "averageOrderValues"));
                    avgSeries.setColor("#52c41a");
                    avgSeries.setType("column");
                    chartData.getSeries().add(avgSeries);

                    chartData.setTitle("收入趋势分析");
                    break;

                case "by_room_type":
                    Map<String, Object> roomTypeData = revenueAnalyticsService.getRevenueByRoomType(startDate, endDate);
                    @SuppressWarnings("unchecked")
                    Map<String, BigDecimal> revenuesByRoomType = (Map<String, BigDecimal>) roomTypeData.get("revenuesByRoomType");
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> orderCountsByRoomType = (Map<String, Integer>) roomTypeData.get("orderCountsByRoomType");

                    chartData.setCategories(new ArrayList<>(revenuesByRoomType.keySet()));

                    // 房型收入
                    ChartSeries roomRevenueSeries = new ChartSeries();
                    roomRevenueSeries.setName("房型收入");
                    roomRevenueSeries.setData(revenuesByRoomType.values().stream().map(BigDecimal::doubleValue).collect(Collectors.toList()));
                    roomRevenueSeries.setColor("#1890ff");
                    roomRevenueSeries.setType("column");
                    chartData.getSeries().add(roomRevenueSeries);

                    chartData.setTitle("按房型收入分析");
                    break;

                case "structure":
                    Map<String, Object> structureData = revenueAnalyticsService.getRevenueStructureAnalysis(startDate, endDate);
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> priceRangeDistribution = (Map<String, Integer>) structureData.get("priceRangeDistribution");

                    chartData.setCategories(new ArrayList<>(priceRangeDistribution.keySet()));

                    // 价格区间分布
                    ChartSeries priceRangeSeries = new ChartSeries();
                    priceRangeSeries.setName("订单数量");
                    priceRangeSeries.setData(priceRangeDistribution.values().stream().map(Integer::doubleValue).collect(Collectors.toList()));
                    priceRangeSeries.setColor("#1890ff");
                    priceRangeSeries.setType("column");
                    chartData.getSeries().add(priceRangeSeries);

                    chartData.setTitle("价格区间分布");
                    break;

                default:
                    throw new IllegalArgumentException("不支持的图表类型: " + type);
            }

            chartData.setSubtitle(String.format("%s 至 %s",
                startDate.format(DateTimeFormatter.ISO_DATE),
                endDate.format(DateTimeFormatter.ISO_DATE)));

            log.info("收入分析图表数据格式化完成");
            return chartData;

        } catch (Exception e) {
            log.error("格式化收入分析图表数据失败", e);
            throw new RuntimeException("格式化收入分析图表数据失败: " + e.getMessage());
        }
    }

    /**
     * 格式化入住率图表数据
     */
    @Cacheable(value = "chart:occupancy:analysis", key = "#startDate + '_' + #endDate + '_' + #type", unless = "#result == null")
    public OccupancyChartDTO formatOccupancyChart(LocalDate startDate, LocalDate endDate, String type) {
        log.info("格式化入住率图表数据，时间段: {} 至 {}，类型: {}", startDate, endDate, type);

        try {
            OccupancyChartDTO chartData = new OccupancyChartDTO();

            switch (type.toLowerCase()) {
                case "trends":
                    Map<String, Object> trendsData = occupancyAnalyticsService.getOccupancyTrendsDaily(startDate, endDate);
                    chartData.setCategories(getLabelsFromData(trendsData, "daily"));

                    // 入住率趋势
                    ChartSeries occupancySeries = new ChartSeries();
                    occupancySeries.setName("入住率 (%)");
                    occupancySeries.setData(getDoubleDataFromData(trendsData, "occupancyRates"));
                    occupancySeries.setColor("#1890ff");
                    occupancySeries.setType("line");
                    chartData.getSeries().add(occupancySeries);

                    // 已入住房间数
                    ChartSeries occupiedSeries = new ChartSeries();
                    occupiedSeries.setName("已入住房间");
                    occupiedSeries.setData(getIntegerDataFromData(trendsData, "occupiedRooms"));
                    occupiedSeries.setColor("#52c41a");
                    occupiedSeries.setType("column");
                    chartData.getSeries().add(occupiedSeries);

                    chartData.setTitle("入住率趋势分析");
                    break;

                case "by_room_type":
                    Map<String, Object> roomTypeData = occupancyAnalyticsService.getOccupancyByRoomType(startDate, endDate);
                    @SuppressWarnings("unchecked")
                    Map<String, Double> occupancyRatesByType = (Map<String, Double>) roomTypeData.get("occupancyRatesByType");

                    chartData.setCategories(new ArrayList<>(occupancyRatesByType.keySet()));

                    // 房型入住率
                    ChartSeries roomOccupancySeries = new ChartSeries();
                    roomOccupancySeries.setName("房型入住率 (%)");
                    roomOccupancySeries.setData(new ArrayList<>(occupancyRatesByType.values()));
                    roomOccupancySeries.setColor("#1890ff");
                    roomOccupancySeries.setType("column");
                    chartData.getSeries().add(roomOccupancySeries);

                    chartData.setTitle("房型入住率对比");
                    break;

                case "heatmap":
                    Map<String, Object> heatmapData = occupancyAnalyticsService.getOccupancyHeatmap(startDate, endDate);
                    chartData.setHeatmapData((List<List<Object>>) heatmapData.get("heatmapData"));
                    chartData.setTitle("入住率热力图");
                    chartData.setWeekDays((List<String>) heatmapData.get("weekDays"));
                    break;

                default:
                    throw new IllegalArgumentException("不支持的图表类型: " + type);
            }

            chartData.setSubtitle(String.format("%s 至 %s",
                startDate.format(DateTimeFormatter.ISO_DATE),
                endDate.format(DateTimeFormatter.ISO_DATE)));

            log.info("入住率图表数据格式化完成");
            return chartData;

        } catch (Exception e) {
            log.error("格式化入住率图表数据失败", e);
            throw new RuntimeException("格式化入住率图表数据失败: " + e.getMessage());
        }
    }

    /**
     * 格式化饼图数据
     */
    @Cacheable(value = "chart:pie", key = "#dataType + '_' + #startDate + '_' + #endDate", unless = "#result == null")
    public PieChartDTO formatPieChartData(String dataType, LocalDate startDate, LocalDate endDate) {
        log.info("格式化饼图数据，数据类型: {}，时间段: {} 至 {}", dataType, startDate, endDate);

        try {
            PieChartDTO chartData = new PieChartDTO();

            switch (dataType.toLowerCase()) {
                case "room_status":
                    // 房间状态分布
                    List<PieDataItem> roomStatusItems = Arrays.asList(
                        new PieDataItem("可用", 30, "#52c41a"),
                        new PieDataItem("已入住", 65, "#1890ff"),
                        new PieDataItem("维护中", 5, "#faad14")
                    );
                    chartData.setData(roomStatusItems);
                    chartData.setTitle("房间状态分布");
                    break;

                case "order_status":
                    // 订单状态分布
                    Map<String, Object> orderStatusTrends = trendAnalysisService.getOrderStatusTrends(startDate, endDate);
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> statusDistribution = (Map<String, Integer>) orderStatusTrends.get("statusDistribution");

                    List<PieDataItem> orderStatusItems = statusDistribution.entrySet().stream()
                        .map(entry -> {
                            String color = switch (entry.getKey()) {
                                case "PENDING" -> "#faad14";
                                case "CONFIRMED" -> "#1890ff";
                                case "COMPLETED" -> "#52c41a";
                                case "CANCELLED" -> "#f5222d";
                                default -> "#d9d9d9";
                            };
                            return new PieDataItem(getStatusDisplayName(entry.getKey()), entry.getValue(), color);
                        })
                        .collect(Collectors.toList());

                    chartData.setData(orderStatusItems);
                    chartData.setTitle("订单状态分布");
                    break;

                case "revenue_by_room_type":
                    // 按房型收入分布
                    Map<String, Object> revenueByRoomType = revenueAnalyticsService.getRevenueByRoomType(startDate, endDate);
                    @SuppressWarnings("unchecked")
                    Map<String, BigDecimal> revenuesByRoomType = (Map<String, BigDecimal>) revenueByRoomType.get("revenuesByRoomType");

                    List<PieDataItem> revenueItems = revenuesByRoomType.entrySet().stream()
                        .map(entry -> new PieDataItem(
                            entry.getKey(),
                            entry.getValue().doubleValue(),
                            getRandomColor()
                        ))
                        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                        .collect(Collectors.toList());

                    chartData.setData(revenueItems);
                    chartData.setTitle("房型收入分布");
                    break;

                default:
                    throw new IllegalArgumentException("不支持的数据类型: " + dataType);
            }

            log.info("饼图数据格式化完成");
            return chartData;

        } catch (Exception e) {
            log.error("格式化饼图数据失败", e);
            throw new RuntimeException("格式化饼图数据失败: " + e.getMessage());
        }
    }

    /**
     * 格式化仪表板图表数据（综合多个图表）
     */
    @Cacheable(value = "chart:dashboard", key = "#startDate + '_' + #endDate", unless = "#result == null")
    public DashboardChartDataDTO formatDashboardCharts(LocalDate startDate, LocalDate endDate) {
        log.info("格式化仪表板图表数据，时间段: {} 至 {}", startDate, endDate);

        try {
            DashboardChartDataDTO dashboardData = new DashboardChartDataDTO();

            // 订单趋势图
            dashboardData.setOrderTrendChart(formatOrderTrendsChart(startDate, endDate, "daily"));

            // 收入分析图
            dashboardData.setRevenueChart(formatRevenueAnalysisChart(startDate, endDate, "trends"));

            // 入住率图
            dashboardData.setOccupancyChart(formatOccupancyChart(startDate, endDate, "trends"));

            // 房间状态饼图
            dashboardData.setRoomStatusPieChart(formatPieChartData("room_status", startDate, endDate));

            // 订单状态饼图
            dashboardData.setOrderStatusPieChart(formatPieChartData("order_status", startDate, endDate));

            // 房型收入饼图
            dashboardData.setRevenuePieChart(formatPieChartData("revenue_by_room_type", startDate, endDate));

            dashboardData.setTitle("仪表板图表数据");
            dashboardData.setPeriod(String.format("%s 至 %s",
                startDate.format(DateTimeFormatter.ISO_DATE),
                endDate.format(DateTimeFormatter.ISO_DATE)));

            log.info("仪表板图表数据格式化完成");
            return dashboardData;

        } catch (Exception e) {
            log.error("格式化仪表板图表数据失败", e);
            throw new RuntimeException("格式化仪表板图表数据失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 从数据中获取标签
     */
    @SuppressWarnings("unchecked")
    private List<String> getLabelsFromData(Map<String, Object> data, String period) {
        switch (period) {
            case "daily":
                return (List<String>) data.get("dates");
            case "weekly":
                return (List<String>) data.get("weekLabels");
            case "monthly":
                return (List<String>) data.get("monthLabels");
            default:
                return (List<String>) data.get("dates");
        }
    }

    /**
     * 从数据中获取整数列表
     */
    @SuppressWarnings("unchecked")
    private List<Double> getIntegerDataFromData(Map<String, Object> data, String key) {
        List<Integer> intList = (List<Integer>) data.get(key);
        return intList.stream().map(Integer::doubleValue).collect(Collectors.toList());
    }

    /**
     * 从数据中获取BigDecimal列表
     */
    @SuppressWarnings("unchecked")
    private List<Double> getBigDecimalDataFromData(Map<String, Object> data, String key) {
        List<BigDecimal> decimalList = (List<BigDecimal>) data.get(key);
        return decimalList.stream()
            .map(bd -> bd != null ? bd.setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0)
            .collect(Collectors.toList());
    }

    /**
     * 从数据中获取Double列表
     */
    @SuppressWarnings("unchecked")
    private List<Double> getDoubleDataFromData(Map<String, Object> data, String key) {
        List<Double> doubleList = (List<Double>) data.get(key);
        return doubleList.stream()
            .map(d -> Math.round(d * 100.0) / 100.0)
            .collect(Collectors.toList());
    }

    /**
     * 获取状态显示名称
     */
    private String getStatusDisplayName(String status) {
        return switch (status) {
            case "PENDING" -> "待处理";
            case "CONFIRMED" -> "已确认";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    /**
     * 生成随机颜色
     */
    private String getRandomColor() {
        String[] colors = {
            "#1890ff", "#52c41a", "#faad14", "#f5222d", "#722ed1",
            "#13c2c2", "#eb2f96", "#fa541c", "#a0d911", "#2f54eb"
        };
        return colors[new Random().nextInt(colors.length)];
    }
}