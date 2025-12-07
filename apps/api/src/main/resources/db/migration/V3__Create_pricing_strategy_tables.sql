-- 创建价格策略相关表
-- Story 2.4: 房间价格策略

USE hotel_management;

-- 1. 创建价格规则表 (pricing_rules)
CREATE TABLE pricing_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    hotel_id BIGINT NOT NULL COMMENT '酒店ID',
    room_type_id BIGINT COMMENT '房间类型ID，null表示适用于所有房间类型',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type ENUM('WEEKEND', 'HOLIDAY', 'SEASONAL', 'CUSTOM') NOT NULL COMMENT '规则类型',
    adjustment_type ENUM('PERCENTAGE', 'FIXED_AMOUNT') NOT NULL COMMENT '调整类型：百分比或固定金额',
    adjustment_value DECIMAL(10,2) NOT NULL COMMENT '调整值，百分比或具体金额',
    start_date DATE COMMENT '规则开始日期',
    end_date DATE COMMENT '规则结束日期',
    days_of_week JSON COMMENT '适用的星期几，[1,2,3,4,5]表示周一到周五',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否激活',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级，数值越大优先级越高',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE,
    FOREIGN KEY (room_type_id) REFERENCES room_types(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_pricing_rules_hotel (hotel_id),
    INDEX idx_pricing_rules_room_type (room_type_id),
    INDEX idx_pricing_rules_active (is_active),
    INDEX idx_pricing_rules_date_range (start_date, end_date),
    INDEX idx_pricing_rules_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格动态调价规则表';

-- 2. 创建特殊价格表 (special_prices)
CREATE TABLE special_prices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    hotel_id BIGINT NOT NULL COMMENT '酒店ID',
    room_type_id BIGINT NOT NULL COMMENT '房间类型ID',
    room_id BIGINT COMMENT '具体房间ID，null表示适用于该类型的所有房间',
    date DATE NOT NULL COMMENT '价格适用日期',
    price DECIMAL(10,2) NOT NULL COMMENT '特殊价格',
    reason VARCHAR(255) COMMENT '设置特殊价格的原因',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE,
    FOREIGN KEY (room_type_id) REFERENCES room_types(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    UNIQUE KEY uk_hotel_room_type_date (hotel_id, room_type_id, room_id, date),
    INDEX idx_special_prices_hotel (hotel_id),
    INDEX idx_special_prices_room_type (room_type_id),
    INDEX idx_special_prices_room (room_id),
    INDEX idx_special_prices_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='特定日期价格表';

-- 3. 创建价格历史记录表 (price_history)
CREATE TABLE price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    hotel_id BIGINT NOT NULL COMMENT '酒店ID',
    room_type_id BIGINT NOT NULL COMMENT '房间类型ID',
    room_id BIGINT COMMENT '具体房间ID，null表示房间类型级别的价格变更',
    old_price DECIMAL(10,2) COMMENT '旧价格',
    new_price DECIMAL(10,2) NOT NULL COMMENT '新价格',
    change_type ENUM('BASE_PRICE', 'DYNAMIC_RULE', 'SPECIAL_PRICE', 'MANUAL') NOT NULL COMMENT '变更类型',
    change_reason VARCHAR(255) COMMENT '变更原因',
    changed_by BIGINT NOT NULL COMMENT '变更操作者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE,
    FOREIGN KEY (room_type_id) REFERENCES room_types(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_price_history_hotel (hotel_id),
    INDEX idx_price_history_room_type (room_type_id),
    INDEX idx_price_history_room (room_id),
    INDEX idx_price_history_date (created_at),
    INDEX idx_price_history_change_type (change_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格变更历史记录表';

-- 4. 创建节假日表 (holidays) 用于动态定价
CREATE TABLE holidays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '节假日名称',
    date DATE NOT NULL COMMENT '节假日日期',
    is_national_holiday BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否为国家法定节假日',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_holiday_date (date),
    INDEX idx_holiday_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节假日表';

-- 5. 插入一些基础节假日数据
INSERT INTO holidays (name, date, is_national_holiday) VALUES
('2025年元旦', '2025-01-01', TRUE),
('2025年春节', '2025-01-28', TRUE),
('2025年春节', '2025-01-29', TRUE),
('2025年春节', '2025-01-30', TRUE),
('2025年清明节', '2025-04-05', TRUE),
('2025年劳动节', '2025-05-01', TRUE),
('2025年端午节', '2025-05-31', TRUE),
('2025年中秋节', '2025-10-06', TRUE),
('2025年国庆节', '2025-10-01', TRUE),
('2025年国庆节', '2025-10-02', TRUE),
('2025年国庆节', '2025-10-03', TRUE),
('2025年国庆节', '2025-10-04', TRUE),
('2025年国庆节', '2025-10-05', TRUE),
('2025年国庆节', '2025-10-07', TRUE);