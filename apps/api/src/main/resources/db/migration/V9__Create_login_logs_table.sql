-- 创建登录日志表
CREATE TABLE `login_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `login_type` varchar(20) DEFAULT NULL COMMENT '登录类型(LOGIN/LOGOUT)',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(100) DEFAULT NULL COMMENT '地理位置',
  `browser` varchar(100) DEFAULT NULL COMMENT '浏览器信息',
  `os` varchar(100) DEFAULT NULL COMMENT '操作系统',
  `status` varchar(20) DEFAULT NULL COMMENT '登录状态(SUCCESS/FAILED)',
  `message` varchar(500) DEFAULT NULL COMMENT '登录消息',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理信息',
  `session_id` varchar(100) DEFAULT NULL COMMENT '会话ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_type` (`login_type`),
  KEY `idx_ip` (`ip`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_time` (`username`, `create_time`),
  KEY `idx_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';