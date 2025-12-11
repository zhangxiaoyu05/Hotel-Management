package com.hotel.dto.pricing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 价格计算请求DTO
 */
@Data
@Schema(description = "价格计算请求")
public class PriceCalculationRequest {

    @NotNull(message = "酒店ID不能为空")
    @Schema(description = "酒店ID", example = "1")
    private Long hotelId;

    @Schema(description = "房间ID，房间级别价格计算时必填", example = "1")
    private Long roomId;

    @Schema(description = "房间类型ID，房间类型级别价格计算时必填", example = "1")
    private Long roomTypeId;

    @NotNull(message = "开始日期不能为空")
    @Future(message = "开始日期不能是过去的日期")
    @Schema(description = "开始日期", example = "2025-01-01")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @Future(message = "结束日期不能是过去的日期")
    @Schema(description = "结束日期", example = "2025-01-07")
    private LocalDate endDate;

    @Schema(description = "是否包含早餐", example = "true")
    private Boolean includeBreakfast;

    @Schema(description = "客人数量", example = "2")
    private Integer guestCount;

    @Schema(description = "优惠码", example = "SAVE20")
    private String promoCode;

    /**
     * 验证请求参数
     */
    @AssertTrue(message = "房间ID和房间类型ID必须且只能提供一个")
    private boolean isRoomOrRoomTypeProvided() {
        return (roomId != null && roomTypeId == null) ||
               (roomId == null && roomTypeId != null);
    }

    @AssertTrue(message = "结束日期必须晚于或等于开始日期")
    private boolean isDateRangeValid() {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }
}