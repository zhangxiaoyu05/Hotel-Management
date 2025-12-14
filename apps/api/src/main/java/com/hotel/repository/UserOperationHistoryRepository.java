package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.UserOperationHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户操作历史Repository
 */
@Mapper
public interface UserOperationHistoryRepository extends BaseMapper<UserOperationHistory> {

    /**
     * 分页查询用户操作历史
     */
    @Select("SELECT * FROM user_operation_history WHERE user_id = #{userId} ORDER BY operation_time DESC")
    IPage<UserOperationHistory> findOperationHistoryByUserId(Page<UserOperationHistory> page, @Param("userId") Long userId);

    /**
     * 查询用户指定时间段内的操作历史
     */
    @Select("SELECT * FROM user_operation_history WHERE user_id = #{userId} " +
            "AND operation_time >= #{startTime} AND operation_time <= #{endTime} " +
            "ORDER BY operation_time DESC")
    List<UserOperationHistory> findOperationHistoryByUserIdAndTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定操作类型的用户历史
     */
    @Select("SELECT * FROM user_operation_history WHERE user_id = #{userId} " +
            "AND operation = #{operation} ORDER BY operation_time DESC")
    List<UserOperationHistory> findOperationHistoryByUserIdAndOperation(
            @Param("userId") Long userId,
            @Param("operation") String operation);

    /**
     * 统计用户操作次数
     */
    @Select("SELECT COUNT(*) FROM user_operation_history WHERE user_id = #{userId}")
    Integer countOperationsByUserId(@Param("userId") Long userId);

    /**
     * 查询最近的用户操作
     */
    @Select("SELECT * FROM user_operation_history WHERE user_id = #{userId} " +
            "ORDER BY operation_time DESC LIMIT #{limit}")
    List<UserOperationHistory> findRecentOperationsByUserId(
            @Param("userId") Long userId,
            @Param("limit") Integer limit);
}