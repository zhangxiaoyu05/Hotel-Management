package com.hotel.dto.review.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewModerationLogResponse {

    private Long id;

    private Long reviewId;

    private Long adminId;

    private String adminName;

    private String action;

    private String reason;

    private String oldStatus;

    private String newStatus;

    private LocalDateTime createdAt;
}