-- 房间搜索功能数据库索引优化脚本
-- 创建日期: 2025-12-07
-- 优化目标: 提高房间搜索查询性能

-- 1. 房间表索引优化
-- ===================

-- 房间基础搜索索引 (酒店ID + 状态 + 房间类型ID + 价格)
CREATE INDEX idx_rooms_hotel_status_type_price ON rooms(hotel_id, status, room_type_id, price);

-- 房间号索引 (用于按房间号排序)
CREATE INDEX idx_rooms_room_number ON rooms(room_number);

-- 楼层索引 (用于按楼层筛选)
CREATE INDEX idx_rooms_floor ON rooms(floor);

-- 房间创建时间索引 (用于按创建时间排序)
CREATE INDEX idx_rooms_created_at ON rooms(created_at);

-- 2. 房间类型表索引优化
-- =====================

-- 酒店ID和容量索引 (用于容量筛选)
CREATE INDEX idx_room_types_hotel_capacity ON room_types(hotel_id, capacity);

-- 3. 订单表索引优化
-- ===================

-- 订单房间和日期索引 (用于可用性检查)
CREATE INDEX idx_orders_room_dates ON orders(room_id, check_in_date, check_out_date);

-- 订单状态索引 (用于筛选有效订单)
CREATE INDEX idx_orders_status ON orders(status);

-- 复合索引：房间ID + 状态 + 日期范围 (优化可用性查询)
CREATE INDEX idx_orders_room_status_dates ON orders(
    room_id,
    status,
    check_in_date,
    check_out_date
) WHERE status IN ('CONFIRMED', 'COMPLETED');

-- 4. 酒店表索引优化
-- ===================

-- 酒店状态索引
CREATE INDEX idx_hotels_status ON hotels(status);

-- 酒店评分索引 (用于按评分排序)
CREATE INDEX idx_hotels_rating ON hotels(rating);

-- 5. 全文搜索索引 (如果支持)
-- =====================

-- 房间类型设施全文搜索 (MySQL 5.7+)
-- ALTER TABLE room_types ADD FULLTEXT INDEX ft_room_types_facilities(facilities);

-- 酒店描述全文搜索
-- ALTER TABLE hotels ADD FULLTEXT INDEX ft_hotels_description(name, description);

-- 6. 分析表统计信息
-- ==================

-- 更新表统计信息以优化查询计划
ANALYZE TABLE rooms;
ANALYZE TABLE room_types;
ANALYZE TABLE hotels;
ANALYZE TABLE orders;

-- 7. 优化查询示例
-- ==================

-- 优化前的查询（可能较慢）:
-- SELECT DISTINCT r.* FROM rooms r
-- LEFT JOIN room_types rt ON r.room_type_id = rt.id
-- LEFT JOIN hotels h ON r.hotel_id = h.id
-- WHERE r.hotel_id = 1
-- AND r.status = 'AVAILABLE'
-- AND rt.capacity >= 2
-- AND r.id NOT IN (
--   SELECT o.room_id FROM orders o
--   WHERE o.status IN ('CONFIRMED', 'COMPLETED')
--   AND (
--     (o.check_in_date <= '2024-12-15' AND o.check_out_date > '2024-12-10')
--   )
-- )
-- ORDER BY r.price ASC;

-- 优化后的查询（使用索引）:
-- 使用EXPLAIN分析查询计划
EXPLAIN SELECT r.*, rt.name as room_type_name, rt.capacity, h.name as hotel_name, h.rating
FROM rooms r
FORCE INDEX (idx_rooms_hotel_status_type_price)
INNER JOIN room_types rt FORCE INDEX (idx_room_types_hotel_capacity) ON r.room_type_id = rt.id
INNER JOIN hotels h FORCE INDEX (idx_hotels_status) ON r.hotel_id = h.id
WHERE r.hotel_id = 1
AND r.status = 'AVAILABLE'
AND r.hotel_id = rt.hotel_id
AND rt.capacity >= 2
AND h.status = 'ACTIVE'
AND NOT EXISTS (
  SELECT 1 FROM orders o
  FORCE INDEX (idx_orders_room_status_dates)
  WHERE o.room_id = r.id
  AND o.status IN ('CONFIRMED', 'COMPLETED')
  AND o.check_in_date < '2024-12-15'
  AND o.check_out_date > '2024-12-10'
)
ORDER BY r.price ASC
LIMIT 20;

-- 8. 监控和维护
-- ==============

-- 查看索引使用情况
SELECT
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    PACKED,
    NULLABLE,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME IN ('rooms', 'room_types', 'hotels', 'orders')
ORDER BY TABLE_NAME, INDEX_NAME;

-- 查看查询执行计划
EXPLAIN FORMAT=JSON
SELECT r.*, rt.name as room_type_name, h.name as hotel_name
FROM rooms r
INNER JOIN room_types rt ON r.room_type_id = rt.id
INNER JOIN hotels h ON r.hotel_id = h.id
WHERE r.hotel_id = 1
AND r.status = 'AVAILABLE'
ORDER BY r.price ASC
LIMIT 20;

-- 9. 性能测试查询
-- ================

-- 测试查询响应时间
SELECT sql_no_cache COUNT(*) as available_rooms
FROM rooms r
WHERE r.hotel_id = 1
AND r.status = 'AVAILABLE'
AND r.price BETWEEN 200 AND 500;

-- 测试复杂搜索查询
SELECT sql_no_cache r.*, rt.name as room_type_name
FROM rooms r
INNER JOIN room_types rt ON r.room_type_id = rt.id
WHERE r.hotel_id = 1
AND r.status = 'AVAILABLE'
AND rt.capacity >= 2
AND r.price BETWEEN 200 AND 500
ORDER BY r.price ASC
LIMIT 20;

-- 10. 定期维护任务
-- =================

-- 清理过期数据（可选）
-- DELETE FROM search_cache WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- 重建索引（如果碎片化严重）
-- ALTER TABLE rooms ENGINE=InnoDB;
-- ALTER TABLE room_types ENGINE=InnoDB;
-- ALTER TABLE hotels ENGINE=InnoDB;
-- ALTER TABLE orders ENGINE=InnoDB;

-- 优化表
OPTIMIZE TABLE rooms;
OPTIMIZE TABLE room_types;
OPTIMIZE TABLE hotels;
OPTIMIZE TABLE orders;