package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 预订冲突实体
 *
 * @author System
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("booking_conflicts")
public class BookingConflict {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotNull(message = "房间ID不能为空")
    @TableField("room_id")
    private Long roomId;

    @NotNull(message = "用户ID不能为空")
    @TableField("user_id")
    private Long userId;

    @NotNull(message = "请求入住日期不能为空")
    @TableField("requested_check_in_date")
    private LocalDateTime requestedCheckInDate;

    @NotNull(message = "请求退房日期不能为空")
    @TableField("requested_check_out_date")
    private LocalDateTime requestedCheckOutDate;

    @TableField("conflicting_order_id")
    private Long conflictingOrderId;

    @NotNull(message = "冲突类型不能为空")
    @TableField("conflict_type")
    private ConflictType conflictType;

    @NotNull(message = "状态不能为空")
    @TableField("status")
    private ConflictStatus status;

    @TableField("resolved_at")
    private LocalDateTime resolvedAt;

    @TableField("resolution_details")
    private String resolutionDetails;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 冲突类型枚举
     */
    public enum ConflictType {
        TIME_OVERLAP("时间重叠"),
        DOUBLE_BOOKING("重复预订"),
        CONCURRENT_REQUEST("并发请求");

        private final String description;

        ConflictType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 冲突状态枚举
     */
    public enum ConflictStatus {
        DETECTED("已检测"),
        RESOLVED("已解决"),
        WAITING_LIST("等待列表");

        private final String description;

        ConflictStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}