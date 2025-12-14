package com.hotel.dto.settings;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.math.BigDecimal;

@Data
public class UpdateBusinessRulesRequest {

    @Min(value = 1, message = "最少预订天数不能小于1")
    @Max(value = 365, message = "最少预订天数不能超过365")
    private Integer minBookingDays;

    @Min(value = 1, message = "最多预订天数不能小于1")
    @Max(value = 365, message = "最多预订天数不能超过365")
    private Integer maxBookingDays;

    @Min(value = 0, message = "提前预订限制不能小于0")
    @Max(value = 365, message = "提前预订限制不能超过365")
    private Integer advanceBookingLimitDays;

    @Min(value = 0, message = "提前取消时限不能小于0")
    @Max(value = 720, message = "提前取消时限不能超过720")
    private Integer cancelBeforeHours;

    @Min(value = 0, message = "取消费用比例不能小于0")
    @Max(value = 100, message = "取消费用比例不能超过100")
    private BigDecimal cancelFeePercentage;

    private Boolean enableFreeCancel;

    @Min(value = 0, message = "免费取消时限不能小于0")
    @Max(value = 720, message = "免费取消时限不能超过720")
    private Integer freeCancelHours;

    @Min(value = 0, message = "默认房价不能小于0")
    private BigDecimal defaultRoomPrice;

    private Boolean enableDynamicPricing;

    @Min(value = 0, message = "旺季价格比例不能小于0")
    @Max(value = 200, message = "旺季价格比例不能超过200")
    private BigDecimal peakSeasonPriceMultiplier;

    @Min(value = 0, message = "淡季价格比例不能小于0")
    @Max(value = 100, message = "淡季价格比例不能超过100")
    private BigDecimal offSeasonPriceMultiplier;

    @Min(value = 0, message = "周末价格比例不能小于0")
    @Max(value = 200, message = "周末价格比例不能超过200")
    private BigDecimal weekendPriceMultiplier;
}