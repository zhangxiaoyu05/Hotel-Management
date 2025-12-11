-- 更新房间类型表结构
-- 添加缺失的字段

USE hotel_management;

-- 添加新字段
ALTER TABLE room_types
ADD COLUMN icon_url VARCHAR(255) COMMENT '房间类型图标URL',
ADD COLUMN status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE' COMMENT '房间类型状态',
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 添加索引
CREATE INDEX idx_room_type_hotel ON room_types(hotel_id);
CREATE INDEX idx_room_type_status ON room_types(status);
CREATE INDEX idx_room_type_hotel_name ON room_types(hotel_id, name);

-- 添加全文搜索索引
ALTER TABLE room_types ADD FULLTEXT idx_room_type_search(name, description);

-- 添加唯一约束（同一酒店下房间类型名称唯一）
ALTER TABLE room_types ADD UNIQUE KEY uk_room_type_name (hotel_id, name);