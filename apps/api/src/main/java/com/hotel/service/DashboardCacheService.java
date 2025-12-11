package com.hotel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 仪表板缓存服务
 * 负责仪表板数据的定时缓存和预处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardCacheService {

    private final DashboardService dashboardService;
    private final DashboardStatisticsService dashboardStatisticsService;

    /**
     * 每5分钟预处理一次实时数据
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void cacheRealTimeData() {
        log.debug("开始定时缓存实时数据");

        try {
            // 触发实时数据缓存
            dashboardService.getRealTimeData();

            log.debug("实时数据缓存完成");
        } catch (Exception e) {
            log.warn("实时数据缓存失败", e);
        }
    }

    /**
     * 每15分钟预处理一次核心指标数据
     */
    @Scheduled(fixedRate = 900000) // 15分钟 = 900000毫秒
    public void cacheCoreMetrics() {
        log.debug("开始定时缓存核心指标数据");

        try {
            // 触发核心指标缓存
            dashboardService.getDashboardMetrics();

            log.debug("核心指标数据缓存完成");
        } catch (Exception e) {
            log.warn("核心指标数据缓存失败", e);
        }
    }

    /**
     * 每小时预处理一次收入统计数据
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void cacheRevenueStatistics() {
        log.debug("开始定时缓存收入统计数据");

        try {
            // 触发收入统计缓存
            dashboardService.getRevenueStatistics();

            log.debug("收入统计数据缓存完成");
        } catch (Exception e) {
            log.warn("收入统计数据缓存失败", e);
        }
    }

    /**
     * 每天凌晨1点预处理历史趋势数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void preprocessTrendData() {
        log.info("开始每日预处理历史趋势数据");

        try {
            dashboardStatisticsService.preprocessTrendData();

            log.info("历史趋势数据预处理完成");
        } catch (Exception e) {
            log.error("历史趋势数据预处理失败", e);
        }
    }

    /**
     * 每天凌晨2点预处理今日核心指标数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void preprocessTodayMetrics() {
        log.info("开始每日预处理今日核心指标数据");

        try {
            dashboardStatisticsService.preprocessTodayMetrics();

            log.info("今日核心指标数据预处理完成");
        } catch (Exception e) {
            log.error("今日核心指标数据预处理失败", e);
        }
    }

    /**
     * 每周一凌晨3点清理过期缓存
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void cleanupExpiredCache() {
        log.info("开始清理过期缓存");

        try {
            // 清除仪表板缓存
            dashboardService.clearDashboardCache();

            log.info("过期缓存清理完成");
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        }
    }
}