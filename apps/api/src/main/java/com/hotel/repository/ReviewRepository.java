package com.hotel.repository;

import com.hotel.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByOrderIdAndUserId(Long orderId, Long userId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT r FROM Review r WHERE r.hotelId = :hotelId AND r.status = 'APPROVED' ORDER BY r.createdAt DESC")
    List<Review> findByHotelIdAndApprovedStatus(@Param("hotelId") Long hotelId);

    boolean existsByOrderIdAndUserId(Long orderId, Long userId);

    @Query("SELECT r FROM Review r WHERE " +
           "(:hotelId IS NULL OR r.hotelId = :hotelId) AND " +
           "(:roomId IS NULL OR r.roomId = :roomId) AND " +
           "(:minRating IS NULL OR r.overallRating >= :minRating) AND " +
           "(:maxRating IS NULL OR r.overallRating <= :maxRating) AND " +
           "(:hasImages IS NULL OR (:hasImages = true AND r.images IS NOT NULL AND r.images != '') OR (:hasImages = false AND (r.images IS NULL OR r.images = ''))) AND " +
           "r.status = 'APPROVED'")
    Page<Review> findReviewsWithFilters(@Param("hotelId") Long hotelId,
                                       @Param("roomId") Long roomId,
                                       @Param("minRating") Integer minRating,
                                       @Param("maxRating") Integer maxRating,
                                       @Param("hasImages") Boolean hasImages,
                                       Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE " +
           "r.hotelId = :hotelId AND " +
           "(:minRating IS NULL OR r.overallRating >= :minRating) AND " +
           "(:maxRating IS NULL OR r.overallRating <= :maxRating) AND " +
           "(:hasImages IS NULL OR (:hasImages = true AND r.images IS NOT NULL AND r.images != '') OR (:hasImages = false AND (r.images IS NULL OR r.images = ''))) AND " +
           "r.status = 'APPROVED'")
    Long countReviewsWithFilters(@Param("hotelId") Long hotelId,
                                @Param("minRating") Integer minRating,
                                @Param("maxRating") Integer maxRating,
                                @Param("hasImages") Boolean hasImages);

    @Query("SELECT r FROM Review r WHERE " +
           "r.hotelId = :hotelId AND " +
           "r.status = 'APPROVED' AND " +
           "r.createdAt >= :startDate")
    List<Review> findRecentReviewsByHotelId(@Param("hotelId") Long hotelId,
                                           @Param("startDate") LocalDateTime startDate);

    @Query("SELECT r FROM Review r WHERE " +
           "r.hotelId = :hotelId AND " +
           "r.status = 'APPROVED' AND " +
           "r.images IS NOT NULL AND r.images != ''")
    List<Review> findReviewsWithImagesByHotelId(@Param("hotelId") Long hotelId);

    // 管理功能查询方法
    @Query("SELECT r FROM Review r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:hotelId IS NULL OR r.hotelId = :hotelId) AND " +
           "(:userId IS NULL OR r.userId = :userId) AND " +
           "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR r.createdAt <= :endDate)")
    Page<Review> findReviewsForManagement(@Param("status") String status,
                                         @Param("hotelId") Long hotelId,
                                         @Param("userId") Long userId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.status = 'PENDING'")
    List<Review> findPendingReviews();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT r FROM Review r WHERE " +
           "r.hotelId = :hotelId AND " +
           "r.status = :status")
    List<Review> findByHotelIdAndStatus(@Param("hotelId") Long hotelId,
                                       @Param("status") String status);

    @Query("SELECT r FROM Review r WHERE " +
           "r.userId = :userId AND " +
           "r.status = :status")
    List<Review> findByUserIdAndStatus(@Param("userId") Long userId,
                                      @Param("status") String status);

    // 统计分析相关查询方法
    @Query("SELECT AVG(r.overallRating), AVG(r.cleanlinessRating), " +
           "AVG(r.serviceRating), AVG(r.facilitiesRating), AVG(r.locationRating) " +
           "FROM Review r WHERE " +
           "(:hotelId IS NULL OR r.hotelId = :hotelId) AND " +
           "r.status = 'APPROVED'")
    Object[] getAverageRatingStatistics(@Param("hotelId") Long hotelId);

    @Query("SELECT r.overallRating, COUNT(r) " +
           "FROM Review r WHERE " +
           "(:hotelId IS NULL OR r.hotelId = :hotelId) AND " +
           "r.status = 'APPROVED' " +
           "GROUP BY r.overallRating")
    List<Object[]> getRatingDistributionRaw(@Param("hotelId") Long hotelId);

    // 按日期分组统计评价数量
    @Query(value = "SELECT DATE(r.created_at) as date, COUNT(*) as count " +
                   "FROM reviews r WHERE " +
                   "(:hotelId IS NULL OR r.hotel_id = :hotelId) AND " +
                   "r.created_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE(r.created_at) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getReviewCountByDate(@Param("hotelId") Long hotelId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // 按日期统计平均评分
    @Query(value = "SELECT DATE(r.created_at) as date, AVG(r.overall_rating) as avgRating " +
                   "FROM reviews r WHERE " +
                   "(:hotelId IS NULL OR r.hotel_id = :hotelId) AND " +
                   "r.status = 'APPROVED' AND " +
                   "r.created_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE(r.created_at) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getRatingByDate(@Param("hotelId") Long hotelId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    // 按日期统计评价状态变化
    @Query(value = "SELECT DATE(l.created_at) as date, " +
                   "SUM(CASE WHEN l.new_status = 'APPROVED' THEN 1 ELSE 0 END) as approved, " +
                   "SUM(CASE WHEN l.new_status = 'REJECTED' THEN 1 ELSE 0 END) as rejected, " +
                   "SUM(CASE WHEN l.new_status = 'PENDING' THEN 1 ELSE 0 END) as pending " +
                   "FROM review_moderation_logs l " +
                   "WHERE l.created_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE(l.created_at) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getStatusTrendsByDate(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT r.overallRating, COUNT(r) " +
           "FROM Review r WHERE " +
           "(:hotelId IS NULL OR r.hotelId = :hotelId) AND " +
           "r.status = 'APPROVED' " +
           "GROUP BY r.overallRating " +
           "ORDER BY r.overallRating")
    Map<Integer, Long> getRatingDistribution(@Param("hotelId") Long hotelId);
}