package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统配置审计日志实体类
 *
 * @author System
 * @since 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("system_config_audit")
public class SystemConfigAudit {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置键
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 配置类型
     */
    @TableField("config_type")
    private String configType;

    /**
     * 变更前的值
     */
    @TableField("old_value")
    private String oldValue;

    /**
     * 变更后的值
     */
    @TableField("new_value")
    private String newValue;

    /**
     * 是否为加密数据
     */
    @TableField("is_encrypted")
    private Boolean isEncrypted;

    /**
     * 操作类型：CREATE, UPDATE, DELETE
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 操作人
     */
    @TableField("operator")
    private String operator;

    /**
     * 操作时间
     */
    @TableField("operation_time")
    private LocalDateTime operationTime;

    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 用户代理
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 操作结果：SUCCESS, FAILED
     */
    @TableField("operation_result")
    private String operationResult;

    /**
     * 失败原因
     */
    @TableField("failure_reason")
    private String failureReason;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}