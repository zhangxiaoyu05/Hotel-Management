package com.hotel.dto.facility;

import com.hotel.entity.facility.HotelFacility;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CreateFacilityRequest {

    @NotNull(message = "酒店ID不能为空")
    private Long hotelId;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "设施名称不能为空")
    @Size(max = 100, message = "设施名称不能超过100个字符")
    private String name;

    @Size(max = 1000, message = "设施描述不能超过1000个字符")
    private String description;

    @Size(max = 255, message = "图标URL不能超过255个字符")
    private String icon;

    private String status = HotelFacility.FacilityStatus.AVAILABLE.getCode();

    private Boolean isFeatured = false;

    private Integer displayOrder = 0;
}