package com.hotel.dto.review.incentive;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class HighQualityBadgeDTO {
    private Long id;
    private Long reviewId;
    private String badgeType;
    private LocalDateTime awardedAt;
    private Map<String, Object> criteria;

    // 显示信息
    private String displayName;
    private String description;
    private String iconUrl;
}