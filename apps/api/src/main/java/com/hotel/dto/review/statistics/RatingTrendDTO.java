package com.hotel.dto.review.statistics;

import lombok.Data;
import java.util.Map;

/**
 * 评分趋势数据DTO
 */
@Data
public class RatingTrendDTO {

    /**
     * 时间段（如：2024-01, 2024-W01）
     */
    private String period;

    /**
     * 综合评分
     */
    private Double overallRating;

    /**
     * 各维度评分
     */
    private Map<String, Double> dimensionRatings;

    /**
     * 评价数量
     */
    private Integer reviewCount;

    /**
     * 净推荐值(NPS)
     */
    private Double nps;

    /**
     * 推荐率
     */
    private Double recommendationRate;

    /**
     * 平均响应时间
     */
    private Double averageResponseTime;
}