package com.hotel.dto.review.incentive;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class IncentiveRuleDTO {
    private Long id;
    private String ruleType;
    private Integer pointsValue;
    private String conditions;
    private Boolean isActive;
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime createdAt;
}