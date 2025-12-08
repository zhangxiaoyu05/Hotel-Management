package com.hotel.repository;

import com.hotel.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByOrderIdAndUserId(Long orderId, Long userId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT r FROM Review r WHERE r.hotelId = :hotelId AND r.status = 'APPROVED' ORDER BY r.createdAt DESC")
    List<Review> findByHotelIdAndApprovedStatus(@Param("hotelId") Long hotelId);

    boolean existsByOrderIdAndUserId(Long orderId, Long userId);
}