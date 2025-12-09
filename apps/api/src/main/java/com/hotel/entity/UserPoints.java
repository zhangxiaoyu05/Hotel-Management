package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_points")
public class UserPoints {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("points")
    private Integer points;

    @TableField("source")
    private String source;

    @TableField("source_id")
    private Long sourceId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("expires_at")
    private LocalDate expiresAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}