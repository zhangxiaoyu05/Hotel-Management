package com.hotel.dto.pricing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 价格计算响应DTO
 */
@Data
@Builder
@Schema(description = "价格计算响应")
public class PriceCalculationResponse {

    @Schema(description = "酒店ID", example = "1")
    private Long hotelId;

    @Schema(description = "房间ID", example = "1")
    private Long roomId;

    @Schema(description = "房间类型ID", example = "1")
    private Long roomTypeId;

    @Schema(description = "房间号", example = "101")
    private String roomNumber;

    @Schema(description = "房间类型名称", example = "标准间")
    private String roomTypeName;

    @Schema(description = "基础价格", example = "200.00")
    private BigDecimal basePrice;

    @Schema(description = "每日价格详情", example = "{\"2025-01-01\": 240.00, \"2025-01-02\": 200.00}")
    private Map<LocalDate, BigDecimal> dailyPrices;

    @Schema(description = "总价格", example = "1440.00")
    private BigDecimal totalPrice;

    @Schema(description = "平均每日价格", example = "205.71")
    private BigDecimal averageDailyPrice;

    @Schema(description = "最低每日价格", example = "200.00")
    private BigDecimal minDailyPrice;

    @Schema(description = "最高每日价格", example = "240.00")
    private BigDecimal maxDailyPrice;

    @Schema(description = "总天数", example = "7")
    private Integer totalDays;

    @Schema(description = "应用的价格规则列表")
    private List<AppliedPricingRule> appliedRules;

    @Schema(description = "特殊价格日期列表")
    private List<SpecialPriceInfo> specialPrices;

    @Schema(description = "是否包含节假日", example = "true")
    private Boolean containsHolidays;

    @Schema(description = "节假日日期列表", example = "[\"2025-01-01\"]")
    private List<LocalDate> holidayDates;

    /**
     * 应用的价格规则信息
     */
    @Data
    @Builder
    @Schema(description = "应用的价格规则信息")
    public static class AppliedPricingRule {
        @Schema(description = "规则ID", example = "1")
        private Long ruleId;

        @Schema(description = "规则名称", example = "周末价格上浮")
        private String ruleName;

        @Schema(description = "规则类型", example = "WEEKEND")
        private String ruleType;

        @Schema(description = "调整类型", example = "PERCENTAGE")
        private String adjustmentType;

        @Schema(description = "调整值", example = "20.00")
        private BigDecimal adjustmentValue;

        @Schema(description = "应用的日期列表")
        private List<LocalDate> appliedDates;
    }

    /**
     * 特殊价格信息
     */
    @Data
    @Builder
    @Schema(description = "特殊价格信息")
    public static class SpecialPriceInfo {
        @Schema(description = "日期", example = "2025-01-01")
        private LocalDate date;

        @Schema(description = "特殊价格", example = "299.00")
        private BigDecimal specialPrice;

        @Schema(description = "设置原因", example = "元旦特惠")
        private String reason;
    }
}