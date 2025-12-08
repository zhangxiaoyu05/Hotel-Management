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
}