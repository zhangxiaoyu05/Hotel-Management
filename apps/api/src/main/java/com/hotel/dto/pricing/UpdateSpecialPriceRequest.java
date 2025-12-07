package com.hotel.dto.pricing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 更新特殊价格请求DTO
 */
@Data
@Schema(description = "更新特殊价格请求")
public class UpdateSpecialPriceRequest {

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "价格格式不正确，最多8位整数和2位小数")
    @Schema(description = "特殊价格", example = "299.00")
    private BigDecimal price;

    @Size(max = 255, message = "设置原因不能超过255个字符")
    @Schema(description = "设置特殊价格的原因", example = "春节期间特价")
    private String reason;

    @Schema(description = "房间ID，为空表示适用于该类型所有房间", example = "1")
    private Long roomId;
}