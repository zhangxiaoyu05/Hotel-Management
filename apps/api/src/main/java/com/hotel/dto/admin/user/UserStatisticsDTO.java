package com.hotel.dto.admin.user;

import lombok.Data;

/**
 * 用户统计信息DTO
 */
@Data
public class UserStatisticsDTO {

    private Long totalUsers;

    private Long activeUsers;

    private Long inactiveUsers;

    private Long adminUsers;

    private Long regularUsers;

    private Long newUsersToday;

    private Long newUsersThisMonth;

    private Long newUsersThisYear;

    private Double activeRate;

    private Double growthRateToday;

    private Double growthRateThisMonth;

    private Double growthRateThisYear;
}