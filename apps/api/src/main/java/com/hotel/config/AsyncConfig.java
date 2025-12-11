package com.hotel.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 *
 * @author Hotel Development Team
 * @since 2024-12-07
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 邮件发送线程池
     */
    @Bean("emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(2);

        // 最大线程数
        executor.setMaxPoolSize(5);

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程空闲时间
        executor.setKeepAliveSeconds(60);

        // 线程名前缀
        executor.setThreadNamePrefix("email-async-");

        // 拒绝策略：调用者执行
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("邮件任务队列已满，由调用线程执行任务");
            r.run();
        });

        // 等待任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("邮件异步任务线程池初始化完成，核心线程数: {}, 最大线程数: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    /**
     * 通知任务线程池
     */
    @Bean("notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("notification-async-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("通知任务队列已满，由调用线程执行任务");
            r.run();
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("通知异步任务线程池初始化完成，核心线程数: {}, 最大线程数: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    /**
     * 默认异步任务执行器
     */
    @Bean("defaultTaskExecutor")
    public Executor defaultTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(300);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("默认异步任务队列已满，由调用线程执行任务");
            r.run();
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("默认异步任务线程池初始化完成，核心线程数: {}, 最大线程数: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }
}