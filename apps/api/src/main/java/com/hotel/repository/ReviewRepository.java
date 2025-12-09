package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewRepository extends BaseMapper<Review> {

    /**
     * 根据订单ID和用户ID查找评价
     */
    @Select("SELECT * FROM reviews WHERE order_id = #{orderId} AND user_id = #{userId}")
    Review findByOrderIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    /**
     * 根据用户ID查找评价，按创建时间倒序
     */
    @Select("SELECT * FROM reviews WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Review> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 查找指定酒店的已审核评价
     */
    @Select("SELECT * FROM reviews WHERE hotel_id = #{hotelId} AND status = 'APPROVED' ORDER BY created_at DESC")
    List<Review> findByHotelIdAndApprovedStatus(@Param("hotelId") Long hotelId);

    /**
     * 检查是否存在评价
     */
    @Select("SELECT COUNT(*) > 0 FROM reviews WHERE order_id = #{orderId} AND user_id = #{userId}")
    boolean existsByOrderIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    /**
     * 分页查询评价（带筛选条件）
     */
    IPage<Review> findReviewsWithFilters(
            Page<Review> page,
            @Param("hotelId") Long hotelId,
            @Param("roomId") Long roomId,
            @Param("minRating") Integer minRating,
            @Param("maxRating") Integer maxRating,
            @Param("hasImages") Boolean hasImages
    );

    /**
     * 统计评价数量（带筛选条件）
     */
    Long countReviewsWithFilters(
            @Param("hotelId") Long hotelId,
            @Param("roomId") Long roomId,
            @Param("minRating") Integer minRating,
            @Param("maxRating") Integer maxRating,
            @Param("hasImages") Boolean hasImages
    );

    /**
     * 查找酒店最近评价
     */
    @Select("SELECT * FROM reviews WHERE hotel_id = #{hotelId} AND status = 'APPROVED' AND created_at >= #{startDate}")
    List<Review> findRecentReviewsByHotelId(@Param("hotelId") Long hotelId, @Param("startDate") LocalDateTime startDate);

    /**
     * 查找酒店带图片的评价
     */
    @Select("SELECT * FROM reviews WHERE hotel_id = #{hotelId} AND status = 'APPROVED' AND images IS NOT NULL AND images != ''")
    List<Review> findReviewsWithImagesByHotelId(@Param("hotelId") Long hotelId);

    // 管理功能查询方法
    /**
     * 分页查询管理评价
     */
    IPage<Review> findReviewsForManagement(
            Page<Review> page,
            @Param("status") String status,
            @Param("hotelId") Long hotelId,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 查找待审核评价
     */
    @Select("SELECT * FROM reviews WHERE status = 'PENDING'")
    List<Review> findPendingReviews();

    /**
     * 统计指定状态的评价数量
     */
    @Select("SELECT COUNT(*) FROM reviews WHERE status = #{status}")
    Long countByStatus(@Param("status") String status);

    /**
     * 查找酒店和状态指定的评价
     */
    @Select("SELECT * FROM reviews WHERE hotel_id = #{hotelId} AND status = #{status}")
    List<Review> findByHotelIdAndStatus(@Param("hotelId") Long hotelId, @Param("status") String status);

    /**
     * 查找用户和状态指定的评价
     */
    @Select("SELECT * FROM reviews WHERE user_id = #{userId} AND status = #{status}")
    List<Review> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    // 统计分析相关查询方法
    /**
     * 获取平均评分统计
     */
    @Select("SELECT AVG(overall_rating), AVG(cleanliness_rating), AVG(service_rating), AVG(facilities_rating), AVG(location_rating) " +
            "FROM reviews WHERE (#{hotelId} IS NULL OR hotel_id = #{hotelId}) AND status = 'APPROVED'")
    Object[] getAverageRatingStatistics(@Param("hotelId") Long hotelId);

    /**
     * 获取评分分布（原始数据）
     */
    @Select("SELECT overall_rating, COUNT(*) FROM reviews WHERE (#{hotelId} IS NULL OR hotel_id = #{hotelId}) AND status = 'APPROVED' GROUP BY overall_rating")
    List<Object[]> getRatingDistributionRaw(@Param("hotelId") Long hotelId);

    // 按日期分组统计评价数量
    @Select("SELECT DATE(created_at) as date, COUNT(*) as count " +
            "FROM reviews WHERE (#{hotelId} IS NULL OR hotel_id = #{hotelId}) AND " +
            "created_at BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Object[]> getReviewCountByDate(@Param("hotelId") Long hotelId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // 按日期统计平均评分
    @Select("SELECT DATE(created_at) as date, AVG(overall_rating) as avgRating " +
            "FROM reviews WHERE (#{hotelId} IS NULL OR hotel_id = #{hotelId}) AND " +
            "status = 'APPROVED' AND created_at BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Object[]> getRatingByDate(@Param("hotelId") Long hotelId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * 获取评分分布
     */
    @Select("SELECT overall_rating, COUNT(*) FROM reviews WHERE (#{hotelId} IS NULL OR hotel_id = #{hotelId}) AND status = 'APPROVED' GROUP BY overall_rating ORDER BY overall_rating")
    Map<Integer, Long> getRatingDistribution(@Param("hotelId") Long hotelId);

    /**
     * 根据评价ID列表和酒店ID列表查找评价
     */
    List<Review> findReviewIdsByHotelIdsAndIds(@Param("hotelIds") List<Long> hotelIds, @Param("reviewIds") List<Long> reviewIds);

    /**
     * 根据酒店ID、状态和创建时间范围查找评价
     */
    @Select("SELECT * FROM reviews WHERE hotel_id = #{hotelId} AND status = #{status} AND " +
            "created_at BETWEEN #{startDate} AND #{endDate} ORDER BY created_at DESC")
    List<Review> findByHotelIdAndStatusAndCreatedAtBetween(
            @Param("hotelId") Long hotelId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 获取酒店评价文本用于词云分析
     * 注意：为了简化，这里返回评论文本，在应用层进行分词处理
     */
    @Select("SELECT comment FROM reviews " +
            "WHERE hotel_id = #{hotelId} AND status = #{status} " +
            "  AND comment IS NOT NULL AND comment != '' " +
            "LIMIT #{limit}")
    List<String> findCommentsForWordCloud(
            @Param("hotelId") Long hotelId,
            @Param("status") String status,
            @Param("limit") Integer limit
    );

    /**
     * 获取评价总数
     */
    @Select("SELECT COUNT(*) FROM reviews WHERE hotel_id = #{hotelId} AND status = #{status}")
    Long countReviewsByHotelAndStatus(
            @Param("hotelId") Long hotelId,
            @Param("status") String status
    );
}