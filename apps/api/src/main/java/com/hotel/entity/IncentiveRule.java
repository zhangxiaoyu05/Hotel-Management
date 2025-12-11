package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("incentive_rules")
public class IncentiveRule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("rule_type")
    private String ruleType;

    @TableField("points_value")
    private Integer pointsValue;

    @TableField("conditions")
    private String conditions;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("valid_from")
    private LocalDate validFrom;

    @TableField("valid_to")
    private LocalDate validTo;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}