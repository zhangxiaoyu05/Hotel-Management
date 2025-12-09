package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.ReviewActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReviewActivityRepository extends BaseMapper<ReviewActivity> {

    /**
     * 查找当前有效的活动
     */
    @Select("SELECT * FROM review_activities WHERE is_active = true " +
            "AND start_date <= #{currentTime} AND end_date >= #{currentTime}")
    List<ReviewActivity> findActiveActivities(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 查找指定类型的活动
     */
    @Select("SELECT * FROM review_activities WHERE activity_type = #{activityType} AND is_active = true")
    List<ReviewActivity> findByActivityType(@Param("activityType") String activityType);

    /**
     * 查找即将开始的活动
     */
    @Select("SELECT * FROM review_activities WHERE is_active = true AND start_date > #{currentTime}")
    List<ReviewActivity> findUpcomingActivities(@Param("currentTime") LocalDateTime currentTime);
}