package com.hotel.dto.review.statistics;

import lombok.Data;
import java.util.Map;

/**
 * 酒店对比分析数据DTO
 */
@Data
public class HotelComparisonDTO {

    /**
     * 酒店ID
     */
    private Long hotelId;

    /**
     * 酒店名称
     */
    private String hotelName;

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
     * 排名
     */
    private Integer ranking;

    /**
     * 星级
     */
    private Integer starRating;

    /**
     * 区域
     */
    private String region;

    /**
     * 是否为当前酒店
     */
    private Boolean isCurrentHotel;

    /**
     * 与行业平均的偏差百分比
     */
    private Map<String, Double> deviationFromAverage;

    /**
     * 排名变化
     */
    private Integer rankingChange;

    /**
     * 评分变化
     */
    private Double ratingChange;
}