package com.hotel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 速率限制注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限制时间窗口（秒）
     */
    int period() default 60;

    /**
     * 在时间窗口内允许的最大请求数
     */
    int limit() default 10;

    /**
     * 限制类型（IP、用户、全局）
     */
    LimitType type() default LimitType.IP;

    /**
     * 自定义限制键前缀
     */
    String prefix() default "";

    /**
     * 错误消息
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 限制类型枚举
     */
    enum LimitType {
        IP,       // 基于IP地址限制
        USER,     // 基于用户ID限制
        GLOBAL    // 全局限制
    }
}