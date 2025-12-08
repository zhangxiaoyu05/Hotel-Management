package com.hotel.enums;

/**
 * 房间类型状态枚举
 */
public enum RoomTypeStatus {
    /**
     * 营业中
     */
    ACTIVE("营业中"),

    /**
     * 已停用
     */
    INACTIVE("已停用");

    private final String description;

    RoomTypeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}