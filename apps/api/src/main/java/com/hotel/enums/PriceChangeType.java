package com.hotel.enums;

/**
 * 价格变更类型枚举
 */
public enum PriceChangeType {
    BASE_PRICE("基础价格变更"),
    DYNAMIC_RULE("动态规则变更"),
    SPECIAL_PRICE("特殊价格变更"),
    MANUAL("手动价格调整");

    private final String description;

    PriceChangeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PriceChangeType fromString(String value) {
        try {
            return PriceChangeType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MANUAL; // 默认返回手动调整
        }
    }
}