package com.hotel.entity.pricing;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格规则实体类
 * 用于管理动态定价规则
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("pricing_rules")
public class PricingRule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("hotel_id")
    private Long hotelId;

    @TableField("room_type_id")
    private Long roomTypeId;

    @TableField("name")
    private String name;

    @TableField("rule_type")
    private String ruleType; // WEEKEND, HOLIDAY, SEASONAL, CUSTOM

    @TableField("adjustment_type")
    private String adjustmentType; // PERCENTAGE, FIXED_AMOUNT

    @TableField("adjustment_value")
    private java.math.BigDecimal adjustmentValue;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("days_of_week")
    private String daysOfWeek; // JSON string: [1,2,3,4,5]

    @TableField("is_active")
    private Boolean isActive;

    @TableField("priority")
    private Integer priority;

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
     * 获取适用的星期几列表
     * @return 星期几列表，1-7表示周一到周日
     */
    public List<Integer> getDaysOfWeekList() {
        if (daysOfWeek == null || daysOfWeek.trim().isEmpty()) {
            return List.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(daysOfWeek, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * 设置适用的星期几列表
     * @param daysOfWeekList 星期几列表，1-7表示周一到周日
     */
    public void setDaysOfWeekList(List<Integer> daysOfWeekList) {
        if (daysOfWeekList == null || daysOfWeekList.isEmpty()) {
            this.daysOfWeek = null;
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.daysOfWeek = mapper.writeValueAsString(daysOfWeekList);
        } catch (JsonProcessingException e) {
            this.daysOfWeek = null;
        }
    }

    /**
     * 检查规则是否适用于指定日期
     * @param date 检查的日期
     * @return 是否适用
     */
    public boolean isApplicableForDate(LocalDate date) {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }

        // 检查日期范围
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }

        // 检查星期几
        List<Integer> applicableDays = getDaysOfWeekList();
        if (!applicableDays.isEmpty()) {
            int dayOfWeek = date.getDayOfWeek().getValue(); // 1-7 (周一到周日)
            if (!applicableDays.contains(dayOfWeek)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 应用价格调整到基础价格
     * @param basePrice 基础价格
     * @return 调整后的价格
     */
    public java.math.BigDecimal applyAdjustment(java.math.BigDecimal basePrice) {
        if ("PERCENTAGE".equals(adjustmentType)) {
            // 百分比调整
            java.math.BigDecimal adjustmentMultiplier = java.math.BigDecimal.ONE
                .add(adjustmentValue.divide(java.math.BigDecimal.valueOf(100)));
            return basePrice.multiply(adjustmentMultiplier);
        } else if ("FIXED_AMOUNT".equals(adjustmentType)) {
            // 固定金额调整
            return basePrice.add(adjustmentValue);
        }
        return basePrice;
    }
}