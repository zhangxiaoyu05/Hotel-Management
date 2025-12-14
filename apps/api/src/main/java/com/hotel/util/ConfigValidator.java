package com.hotel.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 系统配置验证工具类
 *
 * @author System
 * @since 2025-12-14
 */
@Component
public class ConfigValidator {

    // 邮箱验证正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // 电话号码验证正则表达式（支持多种格式）
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[1-9]\\d{1,14}$|^\\d{3,4}[-\\s]?\\d{3,4}[-\\s]?\\d{4}$"
    );

    // IP地址验证正则表达式
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // URL验证正则表达式
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$"
    );

    /**
     * 验证系统名称
     */
    public ValidationResult validateSystemName(String systemName) {
        if (!StringUtils.hasText(systemName)) {
            return ValidationResult.error("系统名称不能为空");
        }

        if (systemName.length() > 100) {
            return ValidationResult.error("系统名称长度不能超过100个字符");
        }

        // 检查是否包含非法字符
        if (systemName.contains("<") || systemName.contains(">") ||
            systemName.contains("\"") || systemName.contains("'")) {
            return ValidationResult.error("系统名称不能包含特殊字符 < > \" '");
        }

        return ValidationResult.success();
    }

    /**
     * 验证邮箱地址
     */
    public ValidationResult validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return ValidationResult.success(); // 邮箱可以为空
        }

        if (email.length() > 255) {
            return ValidationResult.error("邮箱地址长度不能超过255个字符");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.error("邮箱地址格式不正确");
        }

        return ValidationResult.success();
    }

    /**
     * 验证电话号码
     */
    public ValidationResult validatePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return ValidationResult.success(); // 电话可以为空
        }

        if (phone.length() > 20) {
            return ValidationResult.error("电话号码长度不能超过20个字符");
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return ValidationResult.error("电话号码格式不正确");
        }

        return ValidationResult.success();
    }

    /**
     * 验证端口号码
     */
    public ValidationResult validatePort(Integer port) {
        if (port == null) {
            return ValidationResult.success(); // 端口可以为空
        }

        if (port < 1 || port > 65535) {
            return ValidationResult.error("端口号必须在1-65535范围内");
        }

        return ValidationResult.success();
    }

    /**
     * 验证SMTP配置
     */
    public ValidationResult validateSmtpConfig(String host, Integer port, String username, String password) {
        ValidationResult result = ValidationResult.success();

        // 验证SMTP主机
        if (!StringUtils.hasText(host)) {
            result = ValidationResult.error("SMTP主机地址不能为空");
        } else if (host.length() > 255) {
            result = ValidationResult.error("SMTP主机地址长度不能超过255个字符");
        }

        // 验证端口
        ValidationResult portResult = validatePort(port);
        if (!portResult.isValid()) {
            result = ValidationResult.error(result.getErrors().get(0) + "; " + portResult.getErrors().get(0));
        }

        // 验证用户名
        if (StringUtils.hasText(username) && username.length() > 100) {
            result = ValidationResult.error(result.getErrors().get(0) + "; SMTP用户名长度不能超过100个字符");
        }

        return result;
    }

    /**
     * 验证IP地址范围
     */
    public ValidationResult validateIpRange(String ipRange) {
        if (!StringUtils.hasText(ipRange)) {
            return ValidationResult.success(); // IP范围可以为空
        }

        // 支持单个IP或CIDR格式
        if (ipRange.contains("/")) {
            // CIDR格式验证
            String[] parts = ipRange.split("/");
            if (parts.length != 2) {
                return ValidationResult.error("IP范围格式不正确，应为CIDR格式（如192.168.1.0/24）");
            }

            if (!IP_PATTERN.matcher(parts[0]).matches()) {
                return ValidationResult.error("IP地址格式不正确");
            }

            try {
                int prefixLength = Integer.parseInt(parts[1]);
                if (prefixLength < 0 || prefixLength > 32) {
                    return ValidationResult.error("CIDR前缀长度必须在0-32之间");
                }
            } catch (NumberFormatException e) {
                return ValidationResult.error("CIDR前缀长度必须是数字");
            }
        } else {
            // 单个IP验证
            if (!IP_PATTERN.matcher(ipRange).matches()) {
                return ValidationResult.error("IP地址格式不正确");
            }
        }

        return ValidationResult.success();
    }

    /**
     * 验证密码策略配置
     */
    public ValidationResult validatePasswordPolicy(Integer minLength, Boolean requireUppercase,
                                                   Boolean requireLowercase, Boolean requireNumbers,
                                                   Boolean requireSpecialChars) {
        ValidationResult result = ValidationResult.success();

        if (minLength != null) {
            if (minLength < 6) {
                return ValidationResult.error("密码最小长度不能少于6位");
            }
            if (minLength > 50) {
                return ValidationResult.error("密码最小长度不能超过50位");
            }
        }

        return result;
    }

    /**
     * 验证业务规则
     */
    public ValidationResult validateBusinessRules(Integer minBookingDays, Integer maxBookingDays,
                                                  Integer advanceBookingLimit, Integer cancelBeforeHours,
                                                  Integer cancelFeePercentage) {
        ValidationResult result = ValidationResult.success();

        // 验证预订天数范围
        if (minBookingDays != null && maxBookingDays != null) {
            if (minBookingDays < 0 || maxBookingDays < 0) {
                return ValidationResult.error("预订天数不能为负数");
            }
            if (minBookingDays > maxBookingDays) {
                return ValidationResult.error("最少预订天数不能大于最多预订天数");
            }
        }

        // 验证提前预订限制
        if (advanceBookingLimit != null && advanceBookingLimit < 0) {
            return ValidationResult.error("提前预订限制不能为负数");
        }

        // 验证取消时限
        if (cancelBeforeHours != null && cancelBeforeHours < 0) {
            return ValidationResult.error("取消时限不能为负数");
        }

        // 验证取消费用百分比
        if (cancelFeePercentage != null) {
            if (cancelFeePercentage < 0 || cancelFeePercentage > 100) {
                return ValidationResult.error("取消费用百分比必须在0-100之间");
            }
        }

        return result;
    }

    /**
     * 验证备份配置
     */
    public ValidationResult validateBackupConfig(String backupPath, String backupFrequency, Integer retentionDays) {
        ValidationResult result = ValidationResult.success();

        // 验证备份路径
        if (StringUtils.hasText(backupPath)) {
            // 检查路径是否包含非法字符
            if (backupPath.contains("..") || backupPath.contains("~")) {
                return ValidationResult.error("备份路径不能包含 '..' 或 '~'");
            }
        }

        // 验证备份频率
        if (StringUtils.hasText(backupFrequency)) {
            if (!backupFrequency.matches("^(daily|weekly|monthly)$")) {
                return ValidationResult.error("备份频率必须是 daily、weekly 或 monthly");
            }
        }

        // 验证保留天数
        if (retentionDays != null) {
            if (retentionDays < 1 || retentionDays > 365) {
                return ValidationResult.error("备份保留天数必须在1-365之间");
            }
        }

        return result;
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid;
        private java.util.List<String> errors;

        private ValidationResult(boolean valid, java.util.List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, new java.util.ArrayList<>());
        }

        public static ValidationResult error(String message) {
            java.util.List<String> errors = new java.util.ArrayList<>();
            errors.add(message);
            return new ValidationResult(false, errors);
        }

        public boolean isValid() {
            return valid;
        }

        public java.util.List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}