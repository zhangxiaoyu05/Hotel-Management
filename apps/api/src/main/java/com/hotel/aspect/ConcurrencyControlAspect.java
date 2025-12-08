package com.hotel.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 并发控制切面
 * 提供基于注解的分布式锁功能
 *
 * @author System
 * @since 1.0
 */
@Slf4j
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class ConcurrencyControlAspect {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 并发控制注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ConcurrencyControl {
        /**
         * 锁的key前缀
         */
        String prefix() default "";

        /**
         * 锁的key表达式，支持SpEL
         */
        String key() default "";

        /**
         * 等待锁的时间（秒）
         */
        long waitTime() default 5;

        /**
         * 锁的超时时间（秒）
         */
        long leaseTime() default 30;

        /**
         * 锁获取失败时的错误消息
         */
        String errorMessage() default "系统繁忙，请稍后重试";
    }

    /**
     * 乐观锁控制注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OptimisticLock {
        /**
         * 重试次数
         */
        int retryTimes() default 3;

        /**
         * 重试间隔（毫秒）
         */
        long retryInterval() default 100;
    }

    /**
     * 分布式锁控制
     */
    @Around("@annotation(concurrencyControl)")
    public Object aroundConcurrencyControl(ProceedingJoinPoint joinPoint, ConcurrencyControl concurrencyControl) throws Throwable {
        String lockKey = generateLockKey(joinPoint, concurrencyControl);

        log.debug("尝试获取分布式锁: {}", lockKey);

        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(
                concurrencyControl.waitTime(),
                concurrencyControl.leaseTime(),
                TimeUnit.SECONDS
            );

            if (!acquired) {
                log.warn("获取分布式锁失败: {}", lockKey);
                throw new RuntimeException(concurrencyControl.errorMessage());
            }

            log.debug("获取分布式锁成功: {}", lockKey);

            try {
                return joinPoint.proceed();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("释放分布式锁: {}", lockKey);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: {}", lockKey, e);
            throw new RuntimeException("操作被中断，请重试");
        }
    }

    /**
     * 乐观锁控制
     */
    @Around("@annotation(optimisticLock)")
    public Object aroundOptimisticLock(ProceedingJoinPoint joinPoint, OptimisticLock optimisticLock) throws Throwable {
        int retryTimes = optimisticLock.retryTimes();
        long retryInterval = optimisticLock.retryInterval();

        Exception lastException = null;

        for (int i = 0; i <= retryTimes; i++) {
            try {
                if (i > 0) {
                    log.debug("乐观锁重试 {}/{}", i, retryTimes);
                    Thread.sleep(retryInterval);
                }

                return joinPoint.proceed();

            } catch (Exception e) {
                lastException = e;

                // 检查是否是乐观锁异常
                if (!isOptimisticLockException(e) || i == retryTimes) {
                    throw e;
                }

                log.debug("检测到乐观锁冲突，准备重试: {}", e.getMessage());
            }
        }

        throw lastException;
    }

    /**
     * 生成锁的key
     */
    private String generateLockKey(ProceedingJoinPoint joinPoint, ConcurrencyControl concurrencyControl) {
        String prefix = concurrencyControl.prefix();

        if (prefix.isEmpty()) {
            prefix = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        }

        String keyExpression = concurrencyControl.key();
        if (keyExpression.isEmpty()) {
            // 使用方法名和参数生成默认key
            return prefix + ":" + generateKeyFromArgs(joinPoint.getArgs());
        }

        // 支持简单的SpEL表达式解析（简化版本）
        String resolvedKey = resolveKeyExpression(keyExpression, joinPoint.getArgs());
        return prefix + ":" + resolvedKey;
    }

    /**
     * 从方法参数生成key
     */
    private String generateKeyFromArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "no_args";
        }

        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg != null) {
                sb.append(arg.toString()).append(":");
            }
        }

        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "null_args";
    }

    /**
     * 解析key表达式（简化版本）
     */
    private String resolveKeyExpression(String expression, Object[] args) {
        // 简单的表达式解析，支持 #arg0, #arg1 等
        if (expression.startsWith("#arg") && args.length > 0) {
            try {
                int index = Integer.parseInt(expression.substring(4));
                if (index < args.length && args[index] != null) {
                    return args[index].toString();
                }
            } catch (NumberFormatException e) {
                log.warn("无法解析表达式: {}", expression);
            }
        }

        return expression;
    }

    /**
     * 检查是否是乐观锁异常
     */
    private boolean isOptimisticLockException(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        // 检查常见的乐观锁异常消息
        return message.contains("optimistic") ||
               message.contains("version") ||
               message.contains("concurrent") ||
               message.contains("stale") ||
               e.getClass().getSimpleName().contains("Optimistic");
    }
}