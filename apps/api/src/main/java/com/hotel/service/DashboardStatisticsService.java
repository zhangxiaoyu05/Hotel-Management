package com.hotel.service;

import com.hotel.repository.OrderRepository;
import com.hotel.repository.ReviewRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 仪表板统计服务
 * 提供复杂的数据统计和查询功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardStatisticsService {

    private final OrderRepository orderRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 预处理今日核心指标数据
     * 定时任务调用，提前计算并缓存常用数据
     */
    public void preprocessTodayMetrics() {
        log.info("开始预处理今日核心指标数据");

        try {
            LocalDate today = LocalDate.now();

            // 预处理今日订单统计
            orderRepository.countOrdersByDate(today);

            // 预处理今日收入统计
            orderRepository.calculateRevenueByDate(today);

            // 预处理今日新用户统计
            userRepository.countUsersByDate(today);

            // 预处理房间状态统计
            roomRepository.getRoomStatusStatistics();

            log.info("今日核心指标数据预处理完成");
        } catch (Exception e) {
            log.error("预处理今日核心指标数据失败", e);
        }
    }

    /**
     * 预处理历史趋势数据
     * 定时任务调用，计算历史趋势数据并缓存
     */
    public void preprocessTrendData() {
        log.info("开始预处理历史趋势数据");

        try {
            // 预处理最近30天的趋势数据
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(29);

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                // 预处理每日数据
                orderRepository.countOrdersByDate(date);
                orderRepository.calculateRevenueByDate(date);
                userRepository.countUsersByDate(date);
            }

            log.info("历史趋势数据预处理完成");
        } catch (Exception e) {
            log.error("预处理历史趋势数据失败", e);
        }
    }

    /**
     * 获取房间使用率历史数据
     */
    public List<Double> getOccupancyHistory(LocalDate startDate, LocalDate endDate) {
        log.info("获取房间使用率历史数据，时间段: {} 至 {}", startDate, endDate);

        try {
            return orderRepository.calculateOccupancyHistory(startDate, endDate);
        } catch (Exception e) {
            log.error("获取房间使用率历史数据失败", e);
            throw new RuntimeException("获取房间使用率历史数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取收入增长趋势
     */
    public List<BigDecimal> getRevenueGrowthTrend(LocalDate startDate, LocalDate endDate) {
        log.info("获取收入增长趋势，时间段: {} 至 {}", startDate, endDate);

        try {
            return orderRepository.calculateRevenueTrend(startDate, endDate);
        } catch (Exception e) {
            log.error("获取收入增长趋势失败", e);
            throw new RuntimeException("获取收入增长趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户活跃度趋势
     */
    public List<Integer> getUserActivityTrend(LocalDate startDate, LocalDate endDate) {
        log.info("获取用户活跃度趋势，时间段: {} 至 {}", startDate, endDate);

        try {
            return userRepository.calculateUserActivityTrend(startDate, endDate);
        } catch (Exception e) {
            log.error("获取用户活跃度趋势失败", e);
            throw new RuntimeException("获取用户活跃度趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取评价质量统计
     */
    public List<Integer> getReviewQualityStats(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("获取评价质量统计，时间段: {} 至 {}", startDate, endDate);

        try {
            return reviewRepository.calculateRatingDistribution(startDate, endDate);
        } catch (Exception e) {
            log.error("获取评价质量统计失败", e);
            throw new RuntimeException("获取评价质量统计失败: " + e.getMessage());
        }
    }
}