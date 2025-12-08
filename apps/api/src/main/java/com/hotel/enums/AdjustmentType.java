package com.hotel.enums;

/**
 * 价格调整类型枚举
 */
public enum AdjustmentType {
    PERCENTAGE("百分比调整"),
    FIXED_AMOUNT("固定金额调整");

    private final String description;

    AdjustmentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static AdjustmentType fromString(String value) {
        try {
            return AdjustmentType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FIXED_AMOUNT; // 默认返回固定金额调整
        }
    }
}