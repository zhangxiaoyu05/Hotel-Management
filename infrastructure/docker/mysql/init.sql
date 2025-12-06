-- 酒店管理系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS hotel_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE hotel_management;

-- 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 酒店表
CREATE TABLE hotels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    description TEXT,
    facilities JSON,
    images JSON,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 房间类型表
CREATE TABLE room_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    facilities JSON,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE
);

-- 房间表
CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    room_number VARCHAR(20) NOT NULL,
    floor INT NOT NULL,
    area INT NOT NULL,
    status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'CLEANING') NOT NULL DEFAULT 'AVAILABLE',
    price DECIMAL(10,2) NOT NULL,
    images JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE,
    FOREIGN KEY (room_type_id) REFERENCES room_types(id) ON DELETE CASCADE,
    UNIQUE KEY uk_hotel_room (hotel_id, room_number)
);

-- 订单表
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    guest_count INT NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED') NOT NULL DEFAULT 'PENDING',
    special_requests TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

-- 评价表
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    hotel_id BIGINT NOT NULL,
    overall_rating INT NOT NULL CHECK (overall_rating BETWEEN 1 AND 5),
    cleanliness_rating INT NOT NULL CHECK (cleanliness_rating BETWEEN 1 AND 5),
    service_rating INT NOT NULL CHECK (service_rating BETWEEN 1 AND 5),
    facilities_rating INT NOT NULL CHECK (facilities_rating BETWEEN 1 AND 5),
    location_rating INT NOT NULL CHECK (location_rating BETWEEN 1 AND 5),
    comment TEXT,
    images JSON,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    UNIQUE KEY uk_order_review (order_id)
);

-- 创建索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_hotels_name ON hotels(name);
CREATE INDEX idx_hotels_status ON hotels(status);
CREATE INDEX idx_room_types_hotel_id ON room_types(hotel_id);
CREATE INDEX idx_rooms_hotel_id ON rooms(hotel_id);
CREATE INDEX idx_rooms_room_type_id ON rooms(room_type_id);
CREATE INDEX idx_rooms_status ON rooms(status);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_room_id ON orders(room_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_hotel_id ON reviews(hotel_id);
CREATE INDEX idx_reviews_status ON reviews(status);

-- 插入测试数据
-- 插入测试用户
INSERT INTO users (username, email, phone, password, role, status) VALUES
('admin', 'admin@hotel.com', '13800138000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKZkFm5YKrkX.rEikJgLbmGv5.qe', 'ADMIN', 'ACTIVE'),
('testuser', 'test@hotel.com', '13800138001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKZkFm5YKrkX.rEikJgLbmGv5.qe', 'USER', 'ACTIVE');

-- 插入测试酒店
INSERT INTO hotels (name, address, phone, description, facilities, images, status, created_by) VALUES
('成都望江宾馆', '成都市武侯区望江路300号', '028-88888888', '位于成都市中心，交通便利，设施齐全的五星级酒店。酒店拥有各类客房200余间，配备完善的商务设施和休闲娱乐场所。',
'["免费WiFi", "停车场", "游泳池", "健身房", "会议室", "餐厅", "SPA", "酒吧"]',
'["https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800"]',
'ACTIVE', 1),
('天府酒店', '成都市高新区天府大道1000号', '028-99999999', '现代化商务酒店，服务优质的商务人士首选。',
'["免费WiFi", "停车场", "会议室", "餐厅", "商务中心"]',
'["https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800"]',
'ACTIVE', 1),
('锦江宾馆', '成都市锦江区人民南路一段66号', '028-77777777', '历史悠久的知名酒店，环境优雅，服务周到。',
'["免费WiFi", "停车场", "SPA", "酒吧", "餐厅"]',
'["https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=800"]',
'ACTIVE', 1);

-- 插入房型
INSERT INTO room_types (hotel_id, name, capacity, base_price, facilities, description) VALUES
(1, '标准间', 2, 288.00, '["免费WiFi", "空调", "电视", "独立卫浴"]', '舒适的标准客房，配备基本设施'),
(1, '豪华间', 2, 388.00, '["免费WiFi", "空调", "电视", "独立卫浴", "迷你吧"]', '宽敞的豪华客房，设施更完善'),
(1, '套房', 4, 588.00, '["免费WiFi", "空调", "电视", "独立卫浴", "迷你吧", "客厅"]', '豪华套房，包含独立客厅'),
(2, '商务间', 1, 368.00, '["免费WiFi", "空调", "电视", "独立卫浴", "办公桌"]', '专为商务人士设计的客房'),
(2, '行政套房', 2, 568.00, '["免费WiFi", "空调", "电视", "独立卫浴", "办公桌", "客厅"]', '行政级别套房'),
(3, '江景房', 2, 458.00, '["免费WiFi", "空调", "电视", "独立卫浴", "江景"]', '可欣赏江景的客房');

-- 插入房间
INSERT INTO rooms (hotel_id, room_type_id, room_number, floor, area, status, price, images) VALUES
(1, 1, '101', 1, 30, 'AVAILABLE', 288.00, '["https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400"]'),
(1, 1, '102', 1, 30, 'AVAILABLE', 288.00, '["https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400"]'),
(1, 2, '201', 2, 35, 'AVAILABLE', 388.00, '["https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400"]'),
(1, 2, '202', 2, 35, 'AVAILABLE', 388.00, '["https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400"]'),
(1, 3, '301', 3, 50, 'AVAILABLE', 588.00, '["https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=400"]'),
(2, 4, '101', 1, 32, 'AVAILABLE', 368.00, '["https://images.unsplash.com/photo-1566679064377-75c70485df5c?w=400"]'),
(2, 4, '102', 1, 32, 'AVAILABLE', 368.00, '["https://images.unsplash.com/photo-1566679064377-75c70485df5c?w=400"]'),
(2, 5, '201', 2, 45, 'AVAILABLE', 568.00, '["https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400"]'),
(3, 6, '101', 1, 33, 'AVAILABLE', 458.00, '["https://images.unsplash.com/photo-1611892440507-42a792e24d32?w=400"]'),
(3, 6, '102', 1, 33, 'AVAILABLE', 458.00, '["https://images.unsplash.com/photo-1611892440507-42a792e24d32?w=400"]');