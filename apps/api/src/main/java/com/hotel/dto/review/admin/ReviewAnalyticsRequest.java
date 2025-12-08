package com.hotel.dto.review.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewAnalyticsRequest {

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Long hotelId;

    private String groupBy; // day, week, month

    private String metricType; // count, rating, status
}