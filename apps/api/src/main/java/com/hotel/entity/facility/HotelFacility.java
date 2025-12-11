package com.hotel.entity.facility;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hotel_facilities")
public class HotelFacility {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("hotel_id")
    private Long hotelId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("icon")
    private String icon;

    @TableField("status")
    private String status;

    @TableField("is_featured")
    private Boolean isFeatured;

    @TableField("display_order")
    private Integer displayOrder;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public enum FacilityStatus {
        AVAILABLE("AVAILABLE", "正常可用"),
        MAINTENANCE("MAINTENANCE", "维护中"),
        UNAVAILABLE("UNAVAILABLE", "暂不可用");

        private final String code;
        private final String description;

        FacilityStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static FacilityStatus fromCode(String code) {
            for (FacilityStatus status : FacilityStatus.values()) {
                if (status.getCode().equals(code)) {
                    return status;
                }
            }
            return AVAILABLE;
        }
    }
}