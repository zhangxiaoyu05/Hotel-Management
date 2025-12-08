package com.hotel.service;

import com.hotel.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 用户上下文服务
 * 用于获取当前认证用户的信息，支持多租户架构
 */
@Slf4j
@Service
public class UserContextService {

    /**
     * 获取当前认证用户
     * @return 当前用户，如果未认证则返回null
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("用户未认证");
            return null;
        }

        // 如果认证主体是User对象
        if (authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }

        // 如果认证主体是用户名字符串，需要从数据库加载用户信息
        if (authentication.getPrincipal() instanceof String) {
            String username = (String) authentication.getPrincipal();
            log.debug("获取用户信息: {}", username);
            // TODO: 根据用户名从数据库加载用户信息
            // return userService.findByUsername(username);
        }

        log.warn("无法识别的认证主体类型: {}", authentication.getPrincipal().getClass());
        return null;
    }

    /**
     * 获取当前用户的酒店ID
     * @return 酒店ID，如果用户未关联酒店或未认证则抛出异常
     */
    public Long getCurrentUserHotelId() {
        User currentUser = getCurrentUser();

        if (currentUser == null) {
            throw new SecurityException("用户未认证");
        }

        Long hotelId = currentUser.getHotelId();

        if (hotelId == null) {
            // 检查用户角色，管理员可能不需要绑定特定酒店
            if (currentUser.getRole() == User.UserRole.ADMIN) {
                log.warn("管理员用户未绑定酒店，使用默认酒店ID");
                // TODO: 从配置或请求参数获取酒店ID
                // 这里暂时返回null，让调用方处理
                return null;
            }

            throw new SecurityException("用户未关联酒店，请联系管理员");
        }

        return hotelId;
    }

    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    public Long getCurrentUserId() {
        User currentUser = getCurrentUser();

        if (currentUser == null) {
            throw new SecurityException("用户未认证");
        }

        return currentUser.getId();
    }

    /**
     * 获取当前用户角色
     * @return 用户角色
     */
    public User.UserRole getCurrentUserRole() {
        User currentUser = getCurrentUser();

        if (currentUser == null) {
            throw new SecurityException("用户未认证");
        }

        return currentUser.getRole();
    }

    /**
     * 检查当前用户是否有指定角色
     * @param role 要检查的角色
     * @return 是否有该角色
     */
    public boolean hasRole(User.UserRole role) {
        return getCurrentUserRole() == role;
    }

    /**
     * 检查当前用户是否有管理员权限
     * @return 是否是管理员
     */
    public boolean isAdmin() {
        return hasRole(User.UserRole.ADMIN);
    }

    /**
     * 检查当前用户是否可以访问指定酒店
     * @param hotelId 要访问的酒店ID
     * @return 是否可以访问
     */
    public boolean canAccessHotel(Long hotelId) {
        // 管理员可以访问所有酒店
        if (isAdmin()) {
            return true;
        }

        // 其他用户只能访问自己关联的酒店
        Long userHotelId = getCurrentUserHotelId();
        return userHotelId != null && userHotelId.equals(hotelId);
    }

    /**
     * 验证酒店访问权限
     * @param hotelId 要访问的酒店ID
     * @throws SecurityException 如果没有访问权限
     */
    public void validateHotelAccess(Long hotelId) {
        if (!canAccessHotel(hotelId)) {
            throw new SecurityException("没有权限访问该酒店的数据");
        }
    }

    /**
     * 获取当前用户名
     * @return 用户名
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getName();
    }

    /**
     * 检查用户是否已认证
     * @return 是否已认证
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}