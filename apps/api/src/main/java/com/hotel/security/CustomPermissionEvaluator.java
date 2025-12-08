package com.hotel.security;

import com.hotel.enums.Permission;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 自定义权限评估器
 * 用于方法级权限控制
 */
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return false;
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 如果是管理员，允许所有权限
        if (userDetails.isAdmin()) {
            return true;
        }

        // 检查具体权限
        if (permission instanceof String) {
            Permission requiredPermission = Permission.fromCode((String) permission);
            return userDetails.hasPermission(requiredPermission);
        }

        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // 对于基于ID的权限检查，暂时简化处理
        return hasPermission(authentication, null, permission);
    }
}