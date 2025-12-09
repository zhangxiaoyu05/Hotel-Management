-- Story 4.5.1: 活动参与追踪功能
-- 创建活动参与记录表

CREATE TABLE IF NOT EXISTS activity_participations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '参与时间',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '参与状态：ACTIVE, COMPLETED, CANCELLED',
    activity_snapshot TEXT COMMENT '参与时的活动快照（JSON格式）',
    reward_points INT COMMENT '获得的奖励积分',
    reward_at TIMESTAMP COMMENT '奖励发放时间',
    notes TEXT COMMENT '备注',
    deleted INT DEFAULT 0 COMMENT '软删除标识：0-未删除，1-已删除',
    INDEX idx_activity_participation_user (user_id),
    INDEX idx_activity_participation_activity (activity_id),
    INDEX idx_activity_participation_status (status),
    INDEX idx_activity_participation_joined_at (joined_at),
    INDEX idx_activity_participation_deleted (deleted),
    UNIQUE KEY uk_user_activity (user_id, activity_id, deleted)
);

-- 为现有表添加额外的索引以提升性能
ALTER TABLE user_points ADD INDEX IF NOT EXISTS idx_user_points_source_created (source, created_at);
ALTER TABLE incentive_rules ADD INDEX IF NOT EXISTS idx_incentive_rules_type_active (rule_type, is_active, valid_from, valid_to);
ALTER TABLE high_quality_review_badges ADD INDEX IF NOT EXISTS idx_badge_type_awarded (badge_type, awarded_at);
ALTER TABLE review_activities ADD INDEX IF NOT EXISTS idx_activity_dates_type (start_date, end_date, activity_type);

-- 创建视图：活动统计信息
CREATE OR REPLACE VIEW activity_statistics AS
SELECT
    ra.id as activity_id,
    ra.title,
    ra.activity_type,
    ra.start_date,
    ra.end_date,
    ra.is_active,
    COUNT(ap.id) as participation_count,
    SUM(ap.reward_points) as total_rewards_given,
    COUNT(CASE WHEN ap.status = 'ACTIVE' THEN 1 END) as active_participations
FROM review_activities ra
LEFT JOIN activity_participations ap ON ra.id = ap.activity_id AND ap.deleted = 0
WHERE ra.deleted = 0
GROUP BY ra.id, ra.title, ra.activity_type, ra.start_date, ra.end_date, ra.is_active;