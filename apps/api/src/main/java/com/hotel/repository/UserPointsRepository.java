package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserPointsRepository extends BaseMapper<UserPoints> {

    /**
     * 获取用户当前有效积分总数
     */
    @Select("SELECT COALESCE(SUM(points), 0) FROM user_points WHERE user_id = #{userId} " +
            "AND (expires_at IS NULL OR expires_at > #{currentDate})")
    Integer getUserActivePoints(@Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);

    /**
     * 获取用户积分历史记录
     */
    @Select("SELECT * FROM user_points WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<UserPoints> getUserPointsHistory(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 查找即将到期的积分
     */
    @Select("SELECT * FROM user_points WHERE expires_at BETWEEN #{startDate} AND #{endDate}")
    List<UserPoints> findExpiringPoints(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}