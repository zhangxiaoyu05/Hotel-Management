-- Create system_configs table for system settings management
CREATE TABLE IF NOT EXISTS `system_configs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `config_key` VARCHAR(255) NOT NULL COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `config_type` VARCHAR(50) NOT NULL COMMENT '配置类型：BASIC, BUSINESS, NOTIFICATION, SECURITY, BACKUP',
    `description` VARCHAR(500) COMMENT '配置描述',
    `is_encrypted` BOOLEAN DEFAULT FALSE COMMENT '是否加密存储',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_config_type` (`config_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- Insert default basic settings
INSERT INTO `system_configs` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('system.name', '酒店管理系统', 'BASIC', '系统名称'),
('system.logo', '', 'BASIC', '系统Logo'),
('contact.phone', '', 'BASIC', '联系电话'),
('contact.email', '', 'BASIC', '联系邮箱'),
('contact.address', '', 'BASIC', '联系地址'),
('system.description', '专业酒店管理系统', 'BASIC', '系统描述'),
('business.hours', '24小时服务', 'BASIC', '营业时间');

-- Insert default business rules
INSERT INTO `system_configs` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('booking.min_days', '1', 'BUSINESS', '最少预订天数'),
('booking.max_days', '30', 'BUSINESS', '最多预订天数'),
('booking.advance_limit', '365', 'BUSINESS', '提前预订限制'),
('cancel.before_hours', '24', 'BUSINESS', '提前取消时限'),
('cancel.fee_percentage', '0', 'BUSINESS', '取消费用比例'),
('cancel.enable_free', 'true', 'BUSINESS', '启用免费取消'),
('cancel.free_hours', '24', 'BUSINESS', '免费取消时限'),
('price.default', '299', 'BUSINESS', '默认房价'),
('price.enable_dynamic', 'false', 'BUSINESS', '启用动态定价'),
('price.peak_multiplier', '1.5', 'BUSINESS', '旺季价格比例'),
('price.off_season_multiplier', '0.8', 'BUSINESS', '淡季价格比例'),
('price.weekend_multiplier', '1.2', 'BUSINESS', '周末价格比例');

-- Insert default security settings
INSERT INTO `system_configs` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('password.min_length', '8', 'SECURITY', '密码最小长度'),
('password.require_uppercase', 'true', 'SECURITY', '密码要求大写字母'),
('password.require_lowercase', 'true', 'SECURITY', '密码要求小写字母'),
('password.require_numbers', 'true', 'SECURITY', '密码要求数字'),
('password.require_special_chars', 'true', 'SECURITY', '密码要求特殊字符'),
('password.expiry_days', '90', 'SECURITY', '密码过期天数'),
('password.history_count', '5', 'SECURITY', '密码历史记录数'),
('max_login_attempts', '5', 'SECURITY', '最大登录尝试次数'),
('account.lockout_minutes', '30', 'SECURITY', '账户锁定时间'),
('session.timeout_minutes', '120', 'SECURITY', '会话超时时间'),
('enable.two_factor_auth', 'false', 'SECURITY', '启用双因素认证'),
('force.two_factor_auth', 'false', 'SECURITY', '强制双因素认证'),
('enable.captcha', 'false', 'SECURITY', '启用验证码'),
('enable.ip_whitelist', 'false', 'SECURITY', '启用IP白名单'),
('enable.audit_log', 'true', 'SECURITY', '启用审计日志');

-- Insert default backup settings
INSERT INTO `system_configs` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('backup.enable_auto', 'true', 'BACKUP', '启用自动备份'),
('backup.interval_days', '7', 'BACKUP', '备份间隔天数'),
('backup.retention_days', '30', 'BACKUP', '备份保留天数'),
('backup.time', '02:00', 'BACKUP', '备份时间'),
('backup.storage_type', 'local', 'BACKUP', '备份存储类型'),
('backup.local_path', '/backups', 'BACKUP', '本地备份路径'),
('backup.enable_encryption', 'true', 'BACKUP', '启用备份加密'),
('backup.enable_database', 'true', 'BACKUP', '启用数据库备份'),
('backup.enable_files', 'true', 'BACKUP', '启用文件备份'),
('backup.compression_type', 'gzip', 'BACKUP', '备份压缩类型');