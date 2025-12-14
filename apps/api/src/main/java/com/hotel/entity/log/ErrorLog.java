package com.hotel.entity.log;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 错误日志实体类
 * 记录系统错误和异常信息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("error_logs")
public class ErrorLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 异常类型
     */
    @TableField("exception_type")
    private String exceptionType;

    /**
     * 异常消息
     */
    @TableField("message")
    private String message;

    /**
     * 堆栈追踪
     */
    @TableField("stack_trace")
    private String stackTrace;

    /**
     * 类名
     */
    @TableField("class_name")
    private String className;

    /**
     * 方法名
     */
    @TableField("method_name")
    private String methodName;

    /**
     * 文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 行号
     */
    @TableField("line_number")
    private Integer lineNumber;

    /**
     * 请求URL
     */
    @TableField("url")
    private String url;

    /**
     * 请求参数
     */
    @TableField("params")
    private String params;

    /**
     * IP地址
     */
    @TableField("ip")
    private String ip;

    /**
     * 用户代理
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 用户ID（如果有）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户名（如果有）
     */
    @TableField("username")
    private String username;

    /**
     * 错误级别（ERROR/WARN/FATAL）
     */
    @TableField("level")
    private String level;

    /**
     * 模块名称
     */
    @TableField("module")
    private String module;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}