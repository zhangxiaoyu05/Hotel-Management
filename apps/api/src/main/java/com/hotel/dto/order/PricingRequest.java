package com.hotel.dto.order;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Future;
import javax.validation.constraints.Past;
import java.time.LocalDate;

/**
 * 价格计算请求DTO
 *
 * @author Hotel Development Team
 * @since 2024-12-07
 */
@Data
public class PricingRequest {

    /**
     * 房间ID
     */
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    /**
     * 入住日期 (YYYY-MM-DD)
     */
    @NotNull(message = "入住日期不能为空")
    private String checkInDate;

    /**
     * 退房日期 (YYYY-MM-DD)
     */
    @NotNull(message = "退房日期不能为空")
    private String checkOutDate;

    /**
     * 入住人数
     */
    @NotNull(message = "入住人数不能为空")
    @Min(value = 1, message = "入住人数至少为1人")
    private Integer guestCount;

    /**
     * 优惠券代码
     */
    private String couponCode;

    /**
     * 是否会员价格
     */
    private Boolean isMemberPrice = false;

    /**
     * 特殊要求标识（用于可能的价格调整）
     */
    private String specialRequestType;
}