package com.hotel.dto.review.statistics;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 词云数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordCloudDTO {

    /**
     * 关键词
     */
    private String word;

    /**
     * 出现次数
     */
    private Integer count;

    /**
     * 权重（用于词云大小）
     */
    private Double weight;

    /**
     * 情感倾向（positive/negative/neutral）
     */
    private String sentiment;

    /**
     * 相关评分
     */
    private Double averageRating;

    /**
     * 类别（如：服务、卫生、设施等）
     */
    private String category;
}