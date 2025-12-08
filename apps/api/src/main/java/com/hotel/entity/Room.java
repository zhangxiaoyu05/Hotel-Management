package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("rooms")
public class Room {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("hotel_id")
    private Long hotelId;

    @TableField("room_type_id")
    private Long roomTypeId;

    @TableField("room_number")
    private String roomNumber;

    @TableField("floor")
    private Integer floor;

    @TableField("area")
    private Integer area;

    @TableField("status")
    private String status;

    @TableField("price")
    private BigDecimal price;

    @TableField("images")
    private String images;

    @TableField("version")
    private Integer version = 1;

    @TableField("last_status_changed_at")
    private LocalDateTime lastStatusChangedAt;

    @TableField("last_status_changed_by")
    private Long lastStatusChangedBy;

    /**
     * 获取房间图片列表
     * @return 图片URL列表
     */
    public List<String> getImageList() {
        if (images == null || images.trim().isEmpty()) {
            return List.of();
        }
        try {
            return new ObjectMapper().readValue(images, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * 设置房间图片列表
     * @param imageList 图片URL列表
     */
    public void setImageList(List<String> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            this.images = null;
            return;
        }
        try {
            this.images = new ObjectMapper().writeValueAsString(imageList);
        } catch (JsonProcessingException e) {
            this.images = null;
        }
    }

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}