package com.hotel.dto.log;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 日志导出请求DTO
 */
@Data
@Schema(description = "日志导出请求")
public class LogExportRequest {

    @NotBlank(message = "日志类型不能为空")
    @Schema(description = "日志类型", allowableValues = {"operation", "login", "error"}, example = "operation")
    private String logType;

    @Schema(description = "用户名搜索")
    private String username;

    @Schema(description = "操作类型")
    private String operation;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "错误级别")
    private String level;

    @Schema(description = "模块名称")
    private String module;

    @Schema(description = "登录类型")
    private String loginType;

    @NotNull(message = "导出格式不能为空")
    @Schema(description = "导出格式", allowableValues = {"excel", "csv"}, example = "excel")
    private String exportFormat;

    @Schema(description = "是否包含敏感信息", example = "false")
    private Boolean includeSensitiveInfo = false;

    @Schema(description = "最大导出记录数", example = "10000")
    private Integer maxRecords = 10000;
}