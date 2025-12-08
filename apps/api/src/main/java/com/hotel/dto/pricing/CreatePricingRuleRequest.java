package com.hotel.dto.pricing;

import com.hotel.enums.PricingRuleType;
import com.hotel.enums.AdjustmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建价格规则请求DTO
 */
@Data
@Schema(description = "创建价格规则请求")
public class CreatePricingRuleRequest {

    @NotNull(message = "酒店ID不能为空")
    @Schema(description = "酒店ID", example = "1")
    private Long hotelId;

    @Schema(description = "房间类型ID，为空表示适用于所有房间类型", example = "1")
    private Long roomTypeId;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称不能超过100个字符")
    @Schema(description = "规则名称", example = "周末价格上浮")
    private String name;

    @NotNull(message = "规则类型不能为空")
    @Schema(description = "规则类型", allowableValues = {"WEEKEND", "HOLIDAY", "SEASONAL", "CUSTOM"})
    private PricingRuleType ruleType;

    @NotNull(message = "调整类型不能为空")
    @Schema(description = "调整类型", allowableValues = {"PERCENTAGE", "FIXED_AMOUNT"})
    private AdjustmentType adjustmentType;

    @NotNull(message = "调整值不能为空")
    @DecimalMin(value = "0.01", message = "调整值必须大于0")
    @Schema(description = "调整值", example = "20.00")
    private BigDecimal adjustmentValue;

    @Schema(description = "规则开始日期", example = "2025-01-01")
    private LocalDate startDate;

    @Schema(description = "规则结束日期", example = "2025-12-31")
    private LocalDate endDate;

    @Schema(description = "适用的星期几列表，1-7表示周一到周日", example = "[6, 7]")
    private List<Integer> daysOfWeek;

    @Schema(description = "是否激活", example = "true")
    private Boolean isActive = true;

    @Min(value = 0, message = "优先级不能为负数")
    @Schema(description = "优先级，数值越大优先级越高", example = "100")
    private Integer priority = 0;
}