package com.hotel.dto.settings;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.time.LocalDateTime;

@Data
public class SecuritySettingsDTO {

    private Long id;

    @Min(value = 6, message = "密码最小长度不能小于6")
    @Max(value = 50, message = "密码最小长度不能超过50")
    private Integer passwordMinLength;

    private Boolean passwordRequireUppercase;

    private Boolean passwordRequireLowercase;

    private Boolean passwordRequireNumbers;

    private Boolean passwordRequireSpecialChars;

    @Min(value = 0, message = "密码过期天数不能小于0")
    @Max(value = 365, message = "密码过期天数不能超过365")
    private Integer passwordExpiryDays;

    @Min(value = 0, message = "密码历史记录数不能小于0")
    @Max(value = 20, message = "密码历史记录数不能超过20")
    private Integer passwordHistoryCount;

    @Min(value = 3, message = "最大登录尝试次数不能小于3")
    @Max(value = 10, message = "最大登录尝试次数不能超过10")
    private Integer maxLoginAttempts;

    @Min(value = 1, message = "账户锁定时间不能小于1分钟")
    @Max(value = 1440, message = "账户锁定时间不能超过1440分钟")
    private Integer accountLockoutMinutes;

    @Min(value = 15, message = "会话超时时间不能小于15分钟")
    @Max(value = 1440, message = "会话超时时间不能超过1440分钟")
    private Integer sessionTimeoutMinutes;

    private Boolean enableTwoFactorAuth;

    private Boolean forceTwoFactorAuth;

    private Boolean enableCaptcha;

    private Boolean enableIpWhitelist;

    private String allowedIpRanges;

    private Boolean enableAuditLog;

    private LocalDateTime updatedAt;

    private String updatedBy;
}