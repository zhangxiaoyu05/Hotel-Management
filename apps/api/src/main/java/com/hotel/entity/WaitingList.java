package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 等待列表实体
 *
 * @author System
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("waiting_list")
public class WaitingList {

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

    @NotNull(message = "客人数量不能为空")
    @Min(value = 1, message = "客人数量至少为1")
    @Max(value = 10, message = "客人数量不能超过10")
    @TableField("guest_count")
    private Integer guestCount;

    @NotNull(message = "优先级不能为空")
    @Min(value = 0, message = "优先级不能小于0")
    @Max(value = 100, message = "优先级不能超过100")
    @TableField("priority")
    private Integer priority;

    @NotNull(message = "状态不能为空")
    @TableField("status")
    private WaitingListStatus status;

    @TableField("notified_at")
    private LocalDateTime notifiedAt;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("notification_sent")
    private Boolean notificationSent;

    @TableField("confirmed_order_id")
    private Long confirmedOrderId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 等待列表状态枚举
     */
    public enum WaitingListStatus {
        WAITING("等待中"),
        NOTIFIED("已通知"),
        EXPIRED("已过期"),
        CONFIRMED("已确认");

        private final String description;

        WaitingListStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 默认优先级常量
     */
    public static final int DEFAULT_PRIORITY = 50;

    /**
     * VIP用户优先级常量
     */
    public static final int VIP_PRIORITY = 80;

    /**
     * 获取默认优先级
     * @return 默认优先级值
     */
    public static Integer getDefaultPriority() {
        return DEFAULT_PRIORITY;
    }
}