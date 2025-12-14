package com.hotel.dto.admin.user;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 批量操作结果DTO
 */
@Data
public class BatchOperationResultDTO {

    private String operation;

    private Integer totalCount;

    private Integer successCount;

    private Integer failureCount;

    private List<Long> successUserIds;

    private Map<Long, String> failureReasons;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operatedAt;

    private Long operatedBy;

    private String operatorUsername;

    private Boolean isCompleted;

    private String status;
}