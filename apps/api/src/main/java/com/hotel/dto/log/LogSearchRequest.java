package com.hotel.dto.log;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.time.LocalDateTime;

/**
 * 日志搜索请求DTO
 */
@Data
@Schema(description = "日志搜索请求")
public class LogSearchRequest {

    @Schema(description = "页码", example = "0")
    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;

    @Schema(description = "每页大小", example = "20")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能大于100")
    private Integer size = 20;

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

    @Schema(description = "排序字段", example = "createTime")
    private String sortField = "createTime";

    @Schema(description = "排序方向", example = "desc", allowableValues = {"asc", "desc"})
    private String sortDirection = "desc";
}