package com.hotel.dto.review.statistics;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 综合评分统计DTO
 */
@Data
public class ReviewStatisticsDTO {

    /**
     * 酒店ID
     */
    private Long hotelId;

    /**
     * 总评价数
     */
    private Integer totalReviews;

    /**
     * 综合评分
     */
    private Double overallRating;

    /**
     * 各维度评分
     */
    private Map<String, Double> dimensionRatings;

    /**
     * 评分分布
     */
    private Map<Integer, Integer> ratingDistribution;

    /**
     * 趋势数据
     */
    private List<RatingTrendDTO> trendData;

    /**
     * 数据更新时间
     */
    private LocalDateTime lastUpdated;

    /**
     * 统计时间段
     */
    private String period;

    /**
     * 同比数据
     */
    private Map<String, Object> yearOverYear;

    /**
     * 环比数据
     */
    private Map<String, Object> monthOverMonth;
}