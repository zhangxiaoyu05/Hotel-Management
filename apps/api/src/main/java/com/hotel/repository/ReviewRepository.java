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
}