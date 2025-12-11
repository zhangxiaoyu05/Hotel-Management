package com.hotel.enums;

import lombok.Getter;

@Getter
public enum HotelStatus {
    ACTIVE("营业中"),
    INACTIVE("已停业");

    private final String description;

    HotelStatus(String description) {
        this.description = description;
    }
}