package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.ActivityParticipation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ActivityParticipationRepository extends BaseMapper<ActivityParticipation> {

    /**
     * 检查用户是否已参与特定活动
     */
    @Select("SELECT COUNT(*) > 0 FROM activity_participations " +
            "WHERE user_id = #{userId} AND activity_id = #{activityId} AND deleted = 0")
    boolean existsByUserIdAndActivityId(@Param("userId") Long userId, @Param("activityId") Long activityId);

    /**
     * 获取用户参与的所有活动
     */
    @Select("SELECT * FROM activity_participations " +
            "WHERE user_id = #{userId} AND deleted = 0 " +
            "ORDER BY joined_at DESC")
    List<ActivityParticipation> findByUserId(@Param("userId") Long userId);

    /**
     * 获取活动的所有参与者
     */
    @Select("SELECT * FROM activity_participations " +
            "WHERE activity_id = #{activityId} AND deleted = 0 " +
            "ORDER BY joined_at DESC")
    List<ActivityParticipation> findByActivityId(@Param("activityId") Long activityId);

    /**
     * 获取活跃参与记录
     */
    @Select("SELECT * FROM activity_participations " +
            "WHERE activity_id = #{activityId} AND status = #{status} AND deleted = 0 " +
            "ORDER BY joined_at DESC")
    List<ActivityParticipation> findByActivityIdAndStatus(@Param("activityId") Long activityId,
                                                         @Param("status") String status);

    /**
     * 统计活动参与者数量
     */
    @Select("SELECT COUNT(*) FROM activity_participations " +
            "WHERE activity_id = #{activityId} AND deleted = 0")
    Integer countByActivityId(@Param("activityId") Long activityId);

    /**
     * 获取用户在特定时间段内参与的活动
     */
    @Select("SELECT ap.* FROM activity_participations ap " +
            "JOIN review_activities ra ON ap.activity_id = ra.id " +
            "WHERE ap.user_id = #{userId} AND ap.joined_at BETWEEN #{startDate} AND #{endDate} " +
            "AND ap.deleted = 0 " +
            "ORDER BY ap.joined_at DESC")
    List<ActivityParticipation> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);

    /**
     * 软删除参与记录
     */
    @Update("UPDATE activity_participations SET deleted = 1 " +
            "WHERE id = #{id}")
    int softDeleteById(@Param("id") Long id);

    /**
     * 取消参与
     */
    @Update("UPDATE activity_participations SET status = 'CANCELLED', notes = #{reason} " +
            "WHERE user_id = #{userId} AND activity_id = #{activityId} AND deleted = 0")
    int cancelParticipation(@Param("userId") Long userId,
                           @Param("activityId") Long activityId,
                           @Param("reason") String reason);

    /**
     * 更新奖励信息
     */
    @Update("UPDATE activity_participations " +
            "SET reward_points = #{rewardPoints}, reward_at = #{rewardAt}, notes = #{notes} " +
            "WHERE id = #{id}")
    int updateReward(@Param("id") Long id,
                    @Param("rewardPoints") Integer rewardPoints,
                    @Param("rewardAt") LocalDateTime rewardAt,
                    @Param("notes") String notes);
}