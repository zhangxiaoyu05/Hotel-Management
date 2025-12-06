package com.hotel.dto;

import com.hotel.enums.UserStatus;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 更新用户状态请求DTO
 */
@Data
public class UpdateStatusRequest {

    @NotNull(message = "状态不能为空")
    private UserStatus status;
}