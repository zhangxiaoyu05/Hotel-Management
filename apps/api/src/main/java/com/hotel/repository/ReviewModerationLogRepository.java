package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.ReviewModerationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReviewModerationLogRepository extends BaseMapper<ReviewModerationLog> {

    /**
     * 根据评价ID查找审核日志，按创建时间倒序
     */
    @Select("SELECT * FROM review_moderation_logs WHERE review_id = #{reviewId} ORDER BY created_at DESC")
    List<ReviewModerationLog> findByReviewIdOrderByCreatedAtDesc(@Param("reviewId") Long reviewId);

    /**
     * 统计审核日志数量（带筛选条件）
     */
    Long countLogsByFilters(@Param("reviewId") Long reviewId,
                           @Param("adminId") Long adminId,
                           @Param("action") String action,
                           @Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);

    /**
     * 分页查询审核日志（带筛选条件）
     */
    IPage<ReviewModerationLog> findLogsWithFilters(
            Page<ReviewModerationLog> page,
            @Param("reviewId") Long reviewId,
            @Param("adminId") Long adminId,
            @Param("action") String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 根据管理员ID查找审核日志，按创建时间倒序
     */
    @Select("SELECT * FROM review_moderation_logs WHERE admin_id = #{adminId} ORDER BY created_at DESC")
    List<ReviewModerationLog> findByAdminIdOrderByCreatedAtDesc(@Param("adminId") Long adminId);

    // 按日期统计评价状态变化
    @Select("SELECT DATE(created_at) as date, " +
            "SUM(CASE WHEN new_status = 'APPROVED' THEN 1 ELSE 0 END) as approved, " +
            "SUM(CASE WHEN new_status = 'REJECTED' THEN 1 ELSE 0 END) as rejected, " +
            "SUM(CASE WHEN new_status = 'PENDING' THEN 1 ELSE 0 END) as pending " +
            "FROM review_moderation_logs WHERE created_at BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Object[]> getStatusTrendsByDate(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
}