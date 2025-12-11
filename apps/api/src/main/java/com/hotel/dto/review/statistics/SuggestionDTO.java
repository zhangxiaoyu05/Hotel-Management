package com.hotel.dto.review.statistics;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 改进建议DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionDTO {

    /**
     * 建议ID
     */
    private Long id;

    /**
     * 建议类别
     */
    private String category;

    /**
     * 建议标题
     */
    private String title;

    /**
     * 建议描述
     */
    private String description;

    /**
     * 优先级（high/medium/low）
     */
    private String priority;

    /**
     * 影响评分的关键词
     */
    private String[] keywords;

    /**
     * 相关评价数量
     */
    private Integer relatedReviewCount;

    /**
     * 改进后预期评分提升
     */
    private Double expectedRatingImprovement;

    /**
     * 实施难度
     */
    private String difficulty;

    /**
     * 预估成本
     */
    private String estimatedCost;

    /**
     * 实施时间
     */
    private String implementationTime;

    /**
     * 数据分析结果
     */
    private String analysisResult;

    /**
     * 创建时间
     */
    private String createdAt;
}