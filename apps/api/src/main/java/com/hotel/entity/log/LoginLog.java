package com.hotel.entity.log;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 登录日志实体类
 * 记录用户登录和退出行为
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("login_logs")
public class LoginLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 登录类型（登录/退出）
     */
    @TableField("login_type")
    private String loginType;

    /**
     * IP地址
     */
    @TableField("ip")
    private String ip;

    /**
     * 地理位置
     */
    @TableField("location")
    private String location;

    /**
     * 浏览器信息
     */
    @TableField("browser")
    private String browser;

    /**
     * 操作系统
     */
    @TableField("os")
    private String os;

    /**
     * 登录状态（成功/失败）
     */
    @TableField("status")
    private String status;

    /**
     * 登录消息（成功或失败原因）
     */
    @TableField("message")
    private String message;

    /**
     * 用户代理信息
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}