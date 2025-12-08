package com.hotel.entity.pricing;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 特殊价格实体类
 * 用于特定日期的个性化价格设置
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("special_prices")
public class SpecialPrice {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("hotel_id")
    private Long hotelId;

    @TableField("room_type_id")
    private Long roomTypeId;

    @TableField("room_id")
    private Long roomId;

    @TableField("date")
    private LocalDate date;

    @TableField("price")
    private java.math.BigDecimal price;

    @TableField("reason")
    private String reason;

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 检查特殊价格是否适用于指定房间和日期
     * @param roomId 房间ID
     * @param date 日期
     * @return 是否适用
     */
    public boolean isApplicable(Long roomId, LocalDate date) {
        if (this.roomId != null && !this.roomId.equals(roomId)) {
            return false;
        }
        return this.date.equals(date);
    }

    /**
     * 检查特殊价格是否适用于指定房间类型和日期
     * @param roomTypeId 房间类型ID
     * @param date 日期
     * @return 是否适用
     */
    public boolean isApplicableForRoomType(Long roomTypeId, LocalDate date) {
        return this.roomTypeId.equals(roomTypeId) && this.date.equals(date);
    }
}