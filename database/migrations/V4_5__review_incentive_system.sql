-- Story 4.5: 评价激励系统数据库表创建

-- 创建激励规则表
CREATE TABLE IF NOT EXISTS incentive_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型：POINTS_REVIEW, POINTS_HIGH_QUALITY, POINTS_FIRST_REVIEW',
    points_value INT NOT NULL COMMENT '积分值',
    conditions TEXT COMMENT '规则条件（JSON格式）',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    valid_from DATE NOT NULL COMMENT '生效开始日期',
    valid_to DATE COMMENT '生效结束日期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '软删除标识'
);

-- 创建用户积分记录表
CREATE TABLE IF NOT EXISTS user_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    points INT NOT NULL COMMENT '积分数量',
    source VARCHAR(50) NOT NULL COMMENT '积分来源：REVIEW, HIGH_QUALITY_REVIEW, ACTIVITY',
    source_id BIGINT COMMENT '来源ID（评价ID或活动ID）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at DATE COMMENT '过期日期',
    deleted INT DEFAULT 0 COMMENT '软删除标识',
    INDEX idx_user_points_user_id (user_id),
    INDEX idx_user_points_expires_at (expires_at)
);

-- 创建优质评价标识表
CREATE TABLE IF NOT EXISTS high_quality_review_badges (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL COMMENT '评价ID',
    badge_type VARCHAR(20) NOT NULL COMMENT '标识类型：DETAILED, HELPFUL, FEATURED',
    awarded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '颁发时间',
    criteria TEXT COMMENT '评定标准（JSON格式）',
    deleted INT DEFAULT 0 COMMENT '软删除标识',
    UNIQUE KEY uk_review_badge (review_id)
);

-- 创建评价活动表
CREATE TABLE IF NOT EXISTS review_activities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '活动标题',
    description TEXT COMMENT '活动描述',
    activity_type VARCHAR(50) NOT NULL COMMENT '活动类型：REVIEW_CONTEST, DOUBLE_POINTS, MONTHLY_CHAMPION',
    start_date TIMESTAMP NOT NULL COMMENT '开始时间',
    end_date TIMESTAMP NOT NULL COMMENT '结束时间',
    rules TEXT COMMENT '活动规则（JSON格式）',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '软删除标识',
    INDEX idx_activity_dates (start_date, end_date),
    INDEX idx_activity_type (activity_type)
);

-- 插入默认激励规则数据
INSERT INTO incentive_rules (rule_type, points_value, conditions, valid_from, valid_to) VALUES
('POINTS_REVIEW', 10, '{"minWords": 10}', '2025-01-01', NULL),
('POINTS_HIGH_QUALITY', 30, '{"minQualityScore": 7}', '2025-01-01', NULL),
('POINTS_FIRST_REVIEW', 20, '{}', '2025-01-01', NULL);

-- 插入示例活动数据
INSERT INTO review_activities (title, description, activity_type, start_date, end_date, rules, is_active, created_by) VALUES
('春季评价大赛', '提交高质量评价赢取丰厚奖励', 'REVIEW_CONTEST', '2025-03-01 00:00:00', '2025-05-31 23:59:59', '{"participationReward": 10, "winnerReward": 500}', TRUE, 1),
('双倍积分月', '本月提交评价获得双倍积分奖励', 'DOUBLE_POINTS', '2025-04-01 00:00:00', '2025-04-30 23:59:59', '{"multiplier": 2, "maxReviews": 10}', TRUE, 1);