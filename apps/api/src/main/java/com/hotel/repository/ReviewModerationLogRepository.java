package com.hotel.repository;

import com.hotel.entity.ReviewModerationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewModerationLogRepository extends JpaRepository<ReviewModerationLog, Long> {

    List<ReviewModerationLog> findByReviewIdOrderByCreatedAtDesc(Long reviewId);

    @Query("SELECT COUNT(r) FROM ReviewModerationLog r WHERE " +
           "(:reviewId IS NULL OR r.reviewId = :reviewId) AND " +
           "(:adminId IS NULL OR r.adminId = :adminId) AND " +
           "(:action IS NULL OR r.action = :action) AND " +
           "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR r.createdAt <= :endDate)")
    Long countLogsByFilters(@Param("reviewId") Long reviewId,
                           @Param("adminId") Long adminId,
                           @Param("action") String action,
                           @Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT r FROM ReviewModerationLog r WHERE " +
           "(:reviewId IS NULL OR r.reviewId = :reviewId) AND " +
           "(:adminId IS NULL OR r.adminId = :adminId) AND " +
           "(:action IS NULL OR r.action = :action) AND " +
           "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR r.createdAt <= :endDate)")
    Page<ReviewModerationLog> findLogsWithFilters(@Param("reviewId") Long reviewId,
                                                 @Param("adminId") Long adminId,
                                                 @Param("action") String action,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 Pageable pageable);

    List<ReviewModerationLog> findByAdminIdOrderByCreatedAtDesc(Long adminId);

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
}