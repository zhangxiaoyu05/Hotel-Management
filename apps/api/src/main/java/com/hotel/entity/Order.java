package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("orders")
public class Order {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_number")
    private String orderNumber;

    @TableField("user_id")
    private Long userId;

    @TableField("room_id")
    private Long roomId;

    @TableField("check_in_date")
    private LocalDate checkInDate;

    @TableField("check_out_date")
    private LocalDate checkOutDate;

    @TableField("guest_count")
    private Integer guestCount;

    @TableField("total_price")
    private BigDecimal totalPrice;

    @TableField("status")
    private String status;

    @TableField("special_requests")
    private String specialRequests;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("modified_at")
    private LocalDateTime modifiedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}