package com.hotel.enums;

/**
 * 用户角色枚举
 */
public enum Role {
    USER("USER", "普通用户"),
    ADMIN("ADMIN", "管理员");

    private final String code;
    private final String description;

    Role(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Role fromCode(String code) {
        for (Role role : Role.values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role code: " + code);
    }
}