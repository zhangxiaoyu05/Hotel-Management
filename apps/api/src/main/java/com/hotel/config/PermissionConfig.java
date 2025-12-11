package com.hotel.config;

import com.hotel.enums.Permission;
import com.hotel.security.CustomPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 权限配置类
 * 配置方法级权限控制和自定义权限评估器
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class PermissionConfig {

    /**
     * 自定义方法安全表达式处理器
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(new CustomPermissionEvaluator());
        return handler;
    }
}