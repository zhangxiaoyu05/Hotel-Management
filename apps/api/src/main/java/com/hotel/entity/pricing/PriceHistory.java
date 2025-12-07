package com.hotel.entity.pricing;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格历史记录实体类
 * 用于跟踪价格变更的审计信息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("price_history")
public class PriceHistory {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("hotel_id")
    private Long hotelId;

    @TableField("room_type_id")
    private Long roomTypeId;

    @TableField("room_id")
    private Long roomId;

    @TableField("old_price")
    private BigDecimal oldPrice;

    @TableField("new_price")
    private BigDecimal newPrice;

    @TableField("change_type")
    private String changeType; // BASE_PRICE, DYNAMIC_RULE, SPECIAL_PRICE, MANUAL

    @TableField("change_reason")
    private String changeReason;

    @TableField("changed_by")
    private Long changedBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 获取价格变化金额
     * @return 价格变化金额（正数为涨价，负数为降价）
     */
    public BigDecimal getPriceChange() {
        if (oldPrice == null) {
            return BigDecimal.ZERO;
        }
        return newPrice.subtract(oldPrice);
    }

    /**
     * 获取价格变化百分比
     * @return 价格变化百分比
     */
    public BigDecimal getPriceChangePercentage() {
        if (oldPrice == null || oldPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getPriceChange()
            .divide(oldPrice, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 判断是否为涨价
     * @return true表示涨价
     */
    public boolean isPriceIncrease() {
        return getPriceChange().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断是否为降价
     * @return true表示降价
     */
    public boolean isPriceDecrease() {
        return getPriceChange().compareTo(BigDecimal.ZERO) < 0;
    }
}