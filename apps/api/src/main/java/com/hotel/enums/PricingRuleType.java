package com.hotel.enums;

/**
 * 价格规则类型枚举
 */
public enum PricingRuleType {
    WEEKEND("周末价格"),
    HOLIDAY("节假日价格"),
    SEASONAL("季节性价格"),
    CUSTOM("自定义价格");

    private final String description;

    PricingRuleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PricingRuleType fromString(String value) {
        try {
            return PricingRuleType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOM; // 默认返回自定义类型
        }
    }
}