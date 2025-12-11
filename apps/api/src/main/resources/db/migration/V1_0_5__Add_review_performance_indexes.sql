-- 为评价表添加性能优化索引
-- 优化统计分析查询性能

-- 1. 酒店ID和状态的复合索引（用于统计查询）
CREATE INDEX IF NOT EXISTS idx_reviews_hotel_status ON reviews(hotel_id, status);

-- 2. 酒店ID、状态和创建时间的复合索引（用于时间范围统计）
CREATE INDEX IF NOT EXISTS idx_reviews_hotel_status_created ON reviews(hotel_id, status, created_at);

-- 3. 综合评分索引（用于排序和统计）
CREATE INDEX IF NOT EXISTS idx_reviews_overall_rating ON reviews(overall_rating);

-- 4. 维度评分索引（用于各维度统计）
CREATE INDEX IF NOT EXISTS idx_reviews_cleanliness_rating ON reviews(cleanliness_rating);
CREATE INDEX IF NOT EXISTS idx_reviews_service_rating ON reviews(service_rating);
CREATE INDEX IF NOT EXISTS idx_reviews_facilities_rating ON reviews(facilities_rating);
CREATE INDEX IF NOT EXISTS idx_reviews_location_rating ON reviews(location_rating);

-- 5. 用户ID索引（用于用户相关查询）
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id);

-- 6. 订单ID唯一索引（避免重复评价）
CREATE INDEX IF NOT EXISTS idx_reviews_order_id ON reviews(order_id);

-- 7. 创建时间索引（用于时间序列查询）
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews(created_at);

-- 8. 评价内容全文索引（用于文本搜索和词云分析）
-- 注意：需要数据库支持全文索引功能
-- CREATE FULLTEXT INDEX IF NOT EXISTS idx_reviews_comment_fulltext ON reviews(comment);

-- 9. 图片字段索引（用于有图片评价的筛选）
CREATE INDEX IF NOT EXISTS idx_reviews_images ON reviews((CASE WHEN images IS NOT NULL AND images != '' THEN 1 ELSE 0 END));

-- 10. 状态索引（用于管理后台筛选）
CREATE INDEX IF NOT EXISTS idx_reviews_status ON reviews(status);

-- 添加索引使用说明注释
COMMENT ON INDEX idx_reviews_hotel_status IS '用于酒店评价统计查询，支持按酒店和状态筛选';
COMMENT ON INDEX idx_reviews_hotel_status_created IS '用于时间范围统计查询，支持按酒店、状态和时间范围筛选';
COMMENT ON INDEX idx_reviews_overall_rating IS '用于评分排序和评分分布统计';
COMMENT ON INDEX idx_reviews_cleanliness_rating IS '用于卫生维度评分统计';
COMMENT ON INDEX idx_reviews_service_rating IS '用于服务维度评分统计';
COMMENT ON INDEX idx_reviews_facilities_rating IS '用于设施维度评分统计';
COMMENT ON INDEX idx_reviews_location_rating IS '用于位置维度评分统计';