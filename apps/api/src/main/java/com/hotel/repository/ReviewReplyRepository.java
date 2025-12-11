package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.ReviewReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ReviewReplyRepository extends BaseMapper<ReviewReply> {

    /**
     * 根据评价ID查找回复，按创建时间倒序
     */
    @Select("SELECT * FROM review_replies WHERE review_id = #{reviewId} ORDER BY created_at DESC")
    List<ReviewReply> findByReviewIdOrderByCreatedAtDesc(@Param("reviewId") Long reviewId);

    /**
     * 根据评价ID和状态查找回复
     */
    @Select("SELECT * FROM review_replies WHERE review_id = #{reviewId} AND status = #{status} LIMIT 1")
    Optional<ReviewReply> findByReviewIdAndStatus(@Param("reviewId") Long reviewId, @Param("status") String status);

    /**
     * 统计回复数量（带筛选条件）
     */
    Long countRepliesByFilters(@Param("status") String status,
                              @Param("adminId") Long adminId);

    /**
     * 分页查询回复（带筛选条件）
     */
    IPage<ReviewReply> findRepliesWithFilters(
            Page<ReviewReply> page,
            @Param("status") String status,
            @Param("adminId") Long adminId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}