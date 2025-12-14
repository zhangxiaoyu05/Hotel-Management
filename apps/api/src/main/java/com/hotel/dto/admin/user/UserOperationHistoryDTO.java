package com.hotel.dto.admin.user;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 用户操作历史DTO
 */
@Data
public class UserOperationHistoryDTO {

    private Long id;

    private Long userId;

    private String operation;

    private Long operator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operationTime;

    private String details;

    private String ipAddress;

    private String userAgent;

    private String operatorUsername;
}