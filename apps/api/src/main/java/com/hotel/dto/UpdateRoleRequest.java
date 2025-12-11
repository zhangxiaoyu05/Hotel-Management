package com.hotel.dto;

import com.hotel.enums.Role;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 更新用户角色请求DTO
 */
@Data
public class UpdateRoleRequest {

    @NotNull(message = "角色不能为空")
    private Role role;
}