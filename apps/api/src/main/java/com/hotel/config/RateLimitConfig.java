package com.hotel.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 请求限流配置
 *
 * 使用 Resilience4j 实现不同级别的请求限流
 *
 * @author Hotel Development Team
 * @since 2024-12-07
 */
@Configuration
public class RateLimitConfig {

    /**
     * 订单API限流配置
     * 每秒最多10个请求，突发允许20个
     */
    @Bean("orderRateLimiter")
    public RateLimiter orderRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(10)  // 每个周期内的请求数
                .limitRefreshPeriod(Duration.ofSeconds(1))  // 周期时间
                .timeoutDuration(Duration.ofMillis(100))  // 等待许可的超时时间
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("orderAPI");
    }

    /**
     * 通用API限流配置
     * 每秒最多100个请求
     */
    @Bean("generalRateLimiter")
    public RateLimiter generalRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(50))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("generalAPI");
    }

    /**
     * 搜索API限流配置
     * 每秒最多50个请求（搜索资源消耗较大）
     */
    @Bean("searchRateLimiter")
    public RateLimiter searchRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(50)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("searchAPI");
    }

    /**
     * 价格计算API限流配置
     * 每秒最多30个请求（涉及复杂计算）
     */
    @Bean("pricingRateLimiter")
    public RateLimiter pricingRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(30)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(200))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("pricingAPI");
    }

    /**
     * 通知API限流配置
     * 每秒最多20个请求（防止滥用通知功能）
     */
    @Bean("notificationRateLimiter")
    public RateLimiter notificationRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(20)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("notificationAPI");
    }

    /**
     * 评价查询API限流配置
     * 每秒最多50个请求（防止频繁查询）
     */
    @Bean("reviewQueryRateLimiter")
    public RateLimiter reviewQueryRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(50)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("reviewQueryAPI");
    }

    /**
     * 评价统计API限流配置
     * 每秒最多30个请求（统计计算相对耗时）
     */
    @Bean("reviewStatisticsRateLimiter")
    public RateLimiter reviewStatisticsRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(30)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(150))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("reviewStatisticsAPI");
    }

    /**
     * 用户管理批量操作限流配置
     * 每分钟最多5次批量操作（防止系统过载）
     */
    @Bean("userBatchOperationRateLimiter")
    public RateLimiter userBatchOperationRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("userBatchOperationAPI");
    }

    /**
     * 用户管理单个操作限流配置
     * 每分钟最多30次单个用户操作
     */
    @Bean("userSingleOperationRateLimiter")
    public RateLimiter userSingleOperationRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(30)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(200))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("userSingleOperationAPI");
    }

    /**
     * 报表查询API限流配置
     * 每分钟最多20次报表查询（防止系统过载）
     */
    @Bean("reportQueryRateLimiter")
    public RateLimiter reportQueryRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(20)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(1000)) // 报表查询耗时较长，增加等待时间
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("reportQueryAPI");
    }

    /**
     * 报表导出API限流配置
     * 每分钟最多5次导出操作（防止系统过载和资源滥用）
     */
    @Bean("reportExportRateLimiter")
    public RateLimiter reportExportRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(2000)) // 导出操作更耗时
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("reportExportAPI");
    }
}