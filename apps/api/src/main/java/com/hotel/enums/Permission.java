package com.hotel.enums;

/**
 * 权限枚举
 */
public enum Permission {
    // 用户管理权限
    USER_READ("user:read", "查看用户信息"),
    USER_WRITE("user:write", "修改用户信息"),
    USER_DELETE("user:delete", "删除用户"),

    // 预订管理权限
    BOOKING_READ("booking:read", "查看预订信息"),
    BOOKING_WRITE("booking:write", "创建和修改预订"),
    BOOKING_DELETE("booking:delete", "删除预订"),

    // 房间管理权限
    ROOM_READ("room:read", "查看房间信息"),
    ROOM_WRITE("room:write", "创建和修改房间"),
    ROOM_DELETE("room:delete", "删除房间"),

    // 评价管理权限
    REVIEW_READ("review:read", "查看评价信息"),
    REVIEW_WRITE("review:write", "创建和修改评价"),
    REVIEW_DELETE("review:delete", "删除评价"),

    // 系统管理权限
    SYSTEM_ADMIN("system:admin", "系统管理");

    private final String code;
    private final String description;

    Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Permission fromCode(String code) {
        for (Permission permission : Permission.values()) {
            if (permission.getCode().equals(code)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("Unknown permission code: " + code);
    }
}