package com.hotel.dto.admin.user;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 用户管理DTO
 */
@Data
public class UserManagementDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private String newStatus;

    @NotBlank(message = "操作原因不能为空")
    private String reason;

    private Long operatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operatedAt;
}