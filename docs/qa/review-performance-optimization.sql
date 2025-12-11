-- 性能优化建议 - 评价系统相关索引
-- 建议在数据库中添加以下索引以提高查询性能

-- 1. 复合索引：支持按酒店ID和状态查询
CREATE INDEX idx_review_hotel_status ON review (hotel_id, status);

-- 2. 复合索引：支持评分范围查询
CREATE INDEX idx_review_hotel_rating ON review (hotel_id, overall_rating);

-- 3. 复合索引：支持创建时间排序
CREATE INDEX idx_review_hotel_created ON review (hotel_id, created_at DESC);

-- 4. 部分索引：只对有图片的评价建索引
CREATE INDEX idx_review_with_images ON review (hotel_id) WHERE images IS NOT NULL AND images != '';

-- 5. 统计查询优化索引
CREATE INDEX idx_review_hotel_status_created ON review (hotel_id, status, created_at);

-- 注意：
-- 1. 这些索引应根据实际查询模式和数据量进行调整
-- 2. 复合索引的顺序很重要，将选择性高的列放在前面
-- 3. 定期监控索引使用情况，移除未使用的索引