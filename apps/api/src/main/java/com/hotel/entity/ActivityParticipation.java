package com.hotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动参与记录实体
 */
@Data
@TableName("activity_participations")
public class ActivityParticipation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 参与时间
     */
    private LocalDateTime joinedAt;

    /**
     * 参与状态：ACTIVE, COMPLETED, CANCELLED
     */
    private String status;

    /**
     * 参与时的活动快照（JSON格式）
     */
    private String activitySnapshot;

    /**
     * 获得的奖励积分
     */
    private Integer rewardPoints;

    /**
     * 奖励发放时间
     */
    private LocalDateTime rewardAt;

    /**
     * 备注
     */
    private String notes;

    /**
     * 软删除标识：0-未删除，1-已删除
     */
    private Integer deleted;
}