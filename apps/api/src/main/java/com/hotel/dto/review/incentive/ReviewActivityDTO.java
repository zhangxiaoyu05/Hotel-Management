package com.hotel.dto.review.incentive;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ReviewActivityDTO {
    private Long id;
    private String title;
    private String description;
    private String activityType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Map<String, Object> rules;
    private Boolean isActive;
    private Long createdBy;
    private LocalDateTime createdAt;

    // 额外显示信息
    private String status; // UPCOMING, ACTIVE, ENDED
    private Integer participantCount;
    private String imageUrl;
}