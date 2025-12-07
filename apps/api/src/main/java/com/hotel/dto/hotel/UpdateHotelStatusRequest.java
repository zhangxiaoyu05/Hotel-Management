package com.hotel.dto.hotel;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class UpdateHotelStatusRequest {

    @NotBlank(message = "酒店状态不能为空")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "酒店状态必须是ACTIVE或INACTIVE")
    private String status;
}