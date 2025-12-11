package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("high_quality_review_badges")
public class HighQualityReviewBadge {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("review_id")
    private Long reviewId;

    @TableField("badge_type")
    private String badgeType;

    @TableField("awarded_at")
    private LocalDateTime awardedAt;

    @TableField("criteria")
    private String criteria;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}