package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("room_status_logs")
public class RoomStatusLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("room_id")
    private Long roomId;

    @TableField("old_status")
    private String oldStatus;

    @TableField("new_status")
    private String newStatus;

    @TableField("reason")
    private String reason;

    @TableField("changed_by")
    private Long changedBy;

    @TableField("order_id")
    private Long orderId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}