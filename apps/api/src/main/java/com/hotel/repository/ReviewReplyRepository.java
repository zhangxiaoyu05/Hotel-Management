package com.hotel.repository;

import com.hotel.entity.ReviewReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {

    List<ReviewReply> findByReviewIdOrderByCreatedAtDesc(Long reviewId);

    Optional<ReviewReply> findByReviewIdAndStatus(Long reviewId, String status);

    @Query("SELECT COUNT(r) FROM ReviewReply r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:adminId IS NULL OR r.adminId = :adminId)")
    Long countRepliesByFilters(@Param("status") String status,
                              @Param("adminId") Long adminId);

    @Query("SELECT r FROM ReviewReply r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:adminId IS NULL OR r.adminId = :adminId) AND " +
           "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR r.createdAt <= :endDate)")
    Page<ReviewReply> findRepliesWithFilters(@Param("status") String status,
                                           @Param("adminId") Long adminId,
                                           @Param("startDate") java.time.LocalDateTime startDate,
                                           @Param("endDate") java.time.LocalDateTime endDate,
                                           Pageable pageable);
}