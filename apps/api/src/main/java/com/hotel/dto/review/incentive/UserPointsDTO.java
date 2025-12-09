package com.hotel.dto.review.incentive;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserPointsDTO {
    private Long id;
    private Long userId;
    private Integer points;
    private String source;
    private Long sourceId;
    private LocalDateTime createdAt;
    private LocalDate expiresAt;

    // 用户积分汇总信息
    private Integer totalPoints;
    private Integer expiringPoints;
    private Integer earnedThisMonth;
}