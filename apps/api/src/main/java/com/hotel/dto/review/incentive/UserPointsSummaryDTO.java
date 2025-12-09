package com.hotel.dto.review.incentive;

import lombok.Data;
import java.util.List;

@Data
public class UserPointsSummaryDTO {
    private Long userId;
    private Integer totalPoints;
    private Integer earnedThisMonth;
    private Integer expiringPoints;
    private List<UserPointsDTO> recentHistory;
}