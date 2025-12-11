package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.HighQualityReviewBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HighQualityReviewBadgeRepository extends BaseMapper<HighQualityReviewBadge> {

    /**
     * 根据评价ID查找优质标识
     */
    @Select("SELECT * FROM high_quality_review_badges WHERE review_id = #{reviewId}")
    List<HighQualityReviewBadge> findByReviewId(@Param("reviewId") Long reviewId);

    /**
     * 根据多个评价ID批量查找优质标识 - 修复N+1查询问题
     */
    @Select("<script>" +
            "SELECT * FROM high_quality_review_badges " +
            "WHERE review_id IN " +
            "<foreach collection='reviewIds' item='reviewId' open='(' separator=',' close=')'>" +
            "#{reviewId}" +
            "</foreach>" +
            "</script>")
    List<HighQualityReviewBadge> findByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    /**
     * 根据标识类型查找优质评价
     */
    @Select("SELECT * FROM high_quality_review_badges WHERE badge_type = #{badgeType} ORDER BY awarded_at DESC LIMIT #{limit}")
    List<HighQualityReviewBadge> findByBadgeType(@Param("badgeType") String badgeType, @Param("limit") Integer limit);

    /**
     * 检查评价是否已获得优质标识
     */
    @Select("SELECT COUNT(*) > 0 FROM high_quality_review_badges WHERE review_id = #{reviewId}")
    boolean existsByReviewId(@Param("reviewId") Long reviewId);
}