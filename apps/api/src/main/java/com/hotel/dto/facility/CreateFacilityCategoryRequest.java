package com.hotel.dto.facility;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CreateFacilityCategoryRequest {

    @NotNull(message = "酒店ID不能为空")
    private Long hotelId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String name;

    @Size(max = 500, message = "分类描述不能超过500个字符")
    private String description;

    @Size(max = 255, message = "图标URL不能超过255个字符")
    private String icon;

    private Integer displayOrder = 0;

    private Boolean isActive = true;
}