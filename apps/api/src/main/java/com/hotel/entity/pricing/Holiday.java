package com.hotel.entity.pricing;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 节假日实体类
 * 用于管理节假日信息，支持动态定价
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("holidays")
public class Holiday {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("date")
    private LocalDate date;

    @TableField("is_national_holiday")
    private Boolean isNationalHoliday;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 检查指定日期是否为节假日
     * @param date 检查的日期
     * @return true表示是节假日
     */
    public boolean isHoliday(LocalDate date) {
        return this.date.equals(date);
    }
}