package com.hotel.dto.admin.user;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量操作DTO
 */
@Data
public class UserBatchOperationDTO {

    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;

    @NotBlank(message = "操作类型不能为空")
    private String operation;

    private String reason;

    private Long operatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operatedAt;
}