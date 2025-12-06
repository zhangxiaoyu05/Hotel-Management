package com.hotel.util;

import com.hotel.enums.Permission;
import com.hotel.enums.Role;
import com.hotel.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 权限检查工具类
 * 提供便捷的权限检查方法
 */
@Component
public class PermissionUtil {

    /**
     * 获取当前用户
     */
    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        CustomUserDetails currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    /**
     * 检查当前用户是否为管理员
     */
    public static boolean isAdmin() {
        CustomUserDetails currentUser = getCurrentUser();
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * 检查当前用户是否有指定权限
     */
    public static boolean hasPermission(Permission permission) {
        CustomUserDetails currentUser = getCurrentUser();
        return currentUser != null && currentUser.hasPermission(permission);
    }

    /**
     * 检查当前用户是否有指定角色
     */
    public static boolean hasRole(Role role) {
        CustomUserDetails currentUser = getCurrentUser();
        return currentUser != null && currentUser.hasRole(role);
    }

    /**
     * 检查用户是否可以访问指定资源
     */
    public static boolean canAccess(String resourceType, String resourceId) {
        CustomUserDetails currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        // 管理员可以访问所有资源
        if (currentUser.isAdmin()) {
            return true;
        }

        // 普通用户只能访问自己的资源
        switch (resourceType) {
            case "user":
                return currentUser.getId().toString().equals(resourceId);
            case "booking":
                // 这里需要根据业务逻辑检查预订是否属于当前用户
                // 暂时简化处理
                return true;
            case "review":
                // 这里需要根据业务逻辑检查评价是否属于当前用户
                // 暂时简化处理
                return true;
            default:
                return false;
        }
    }

    /**
     * 检查当前用户是否可以管理用户
     */
    public static boolean canManageUsers() {
        return hasPermission(Permission.USER_READ) && hasPermission(Permission.USER_WRITE);
    }

    /**
     * 检查当前用户是否可以管理房间
     */
    public static boolean canManageRooms() {
        return hasPermission(Permission.ROOM_READ) && hasPermission(Permission.ROOM_WRITE);
    }

    /**
     * 检查当前用户是否可以管理预订
     */
    public static boolean canManageBookings() {
        return hasPermission(Permission.BOOKING_READ) && hasPermission(Permission.BOOKING_WRITE);
    }

    /**
     * 检查当前用户是否可以管理评价
     */
    public static boolean canManageReviews() {
        return hasPermission(Permission.REVIEW_READ) && hasPermission(Permission.REVIEW_WRITE);
    }

    /**
     * 获取当前用户的所有权限
     */
    public static String[] getCurrentUserPermissions() {
        CustomUserDetails currentUser = getCurrentUser();
        if (currentUser == null) {
            return new String[0];
        }

        return currentUser.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toArray(String[]::new);
    }
}