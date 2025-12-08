package com.hotel.entity.facility;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("facility_categories")
public class FacilityCategory {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("hotel_id")
    private Long hotelId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("icon")
    private String icon;

    @TableField("display_order")
    private Integer displayOrder;

    @TableField("is_active")
    private Boolean isActive;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}