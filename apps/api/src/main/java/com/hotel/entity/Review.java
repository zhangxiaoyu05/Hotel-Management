package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("reviews")
public class Review {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("order_id")
    private Long orderId;

    @TableField("room_id")
    private Long roomId;

    @TableField("hotel_id")
    private Long hotelId;

    @TableField("overall_rating")
    private Integer overallRating;

    @TableField("cleanliness_rating")
    private Integer cleanlinessRating;

    @TableField("service_rating")
    private Integer serviceRating;

    @TableField("facilities_rating")
    private Integer facilitiesRating;

    @TableField("location_rating")
    private Integer locationRating;

    @TableField("comment")
    private String comment;

    @TableField("images")
    private String images;

    @TableField("is_anonymous")
    private Boolean isAnonymous;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}