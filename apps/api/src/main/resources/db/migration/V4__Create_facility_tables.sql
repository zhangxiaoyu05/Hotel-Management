-- 创建酒店设施相关表
-- Story 2.5: 酒店设施管理

USE hotel_management;

-- 1. 创建设施分类表 (facility_categories)
CREATE TABLE facility_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    hotel_id BIGINT NOT NULL COMMENT '酒店ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description TEXT COMMENT '分类描述',
    icon VARCHAR(255) COMMENT '分类图标URL',
    display_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否激活',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE,
    INDEX idx_facility_categories_hotel (hotel_id),
    INDEX idx_facility_categories_order (display_order),
    INDEX idx_facility_categories_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设施分类表';

-- 2. 创建酒店设施表 (hotel_facilities)
CREATE TABLE hotel_facilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    hotel_id BIGINT NOT NULL COMMENT '酒店ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    name VARCHAR(100) NOT NULL COMMENT '设施名称',
    description TEXT COMMENT '设施描述',
    icon VARCHAR(255) COMMENT '设施图标URL',
    status ENUM('AVAILABLE', 'MAINTENANCE', 'UNAVAILABLE') NOT NULL DEFAULT 'AVAILABLE' COMMENT '设施状态',
    is_featured BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为特色设施',
    display_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES facility_categories(id) ON DELETE CASCADE,
    INDEX idx_hotel_facilities_hotel (hotel_id),
    INDEX idx_hotel_facilities_category (category_id),
    INDEX idx_hotel_facilities_status (status),
    INDEX idx_hotel_facilities_featured (is_featured),
    INDEX idx_hotel_facilities_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='酒店设施表';

-- 3. 插入一些基础设施数据
INSERT INTO facility_categories (hotel_id, name, description, display_order) VALUES
(1, '客房设施', '客房内的设施和服务', 1),
(1, '公共设施', '酒店公共区域的设施', 2),
(1, '服务设施', '酒店提供的各类服务', 3),
(2, '客房设施', '客房内的设施和服务', 1),
(2, '公共设施', '酒店公共区域的设施', 2),
(2, '服务设施', '酒店提供的各类服务', 3);

-- 4. 插入一些基础设施数据
INSERT INTO hotel_facilities (hotel_id, category_id, name, description, status, display_order) VALUES
-- 酒店ID=1的设施
(1, 1, '迷你吧', '客房内提供免费饮品和小吃', 'AVAILABLE', 1),
(1, 1, '空调系统', '独立温控空调系统', 'AVAILABLE', 2),
(1, 1, '平板电视', '55寸智能平板电视', 'AVAILABLE', 3),
(1, 1, '保险箱', '客房内电子保险箱', 'AVAILABLE', 4),
(1, 1, '免费WiFi', '高速无线网络覆盖', 'AVAILABLE', 5),
(1, 2, '游泳池', '室外恒温游泳池', 'AVAILABLE', 1),
(1, 2, '健身中心', '24小时开放的健身房', 'AVAILABLE', 2),
(1, 2, '餐厅', '提供中西式美食', 'AVAILABLE', 3),
(1, 2, '停车场', '免费地下停车场', 'AVAILABLE', 4),
(1, 3, '24小时前台', '全天候接待服务', 'AVAILABLE', 1),
(1, 3, '客房服务', '24小时客房送餐服务', 'AVAILABLE', 2),
(1, 3, '礼宾服务', '旅游咨询和安排服务', 'AVAILABLE', 3),
-- 酒店ID=2的设施
(2, 1, '迷你吧', '客房内提供付费饮品和小吃', 'AVAILABLE', 1),
(2, 1, '空调系统', '中央空调系统', 'AVAILABLE', 2),
(2, 1, '液晶电视', '42寸液晶电视', 'AVAILABLE', 3),
(2, 1, '免费WiFi', '全酒店WiFi覆盖', 'AVAILABLE', 4),
(2, 2, '商务中心', '商务办公设施', 'AVAILABLE', 1),
(2, 2, '会议室', '多功能会议室', 'AVAILABLE', 2),
(2, 2, '咖啡厅', '休闲咖啡厅', 'AVAILABLE', 3),
(2, 3, '前台服务', '6:00-24:00前台服务', 'AVAILABLE', 1),
(2, 3, '叫醒服务', '电话叫醒服务', 'AVAILABLE', 2);