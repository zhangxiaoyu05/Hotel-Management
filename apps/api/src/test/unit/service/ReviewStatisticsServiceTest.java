package com.hotel.service;

import com.hotel.dto.review.ReviewStatisticsResponse;
import com.hotel.entity.Review;
import com.hotel.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewStatisticsServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewStatisticsService reviewStatisticsService;

    private List<Review> mockReviews;

    @BeforeEach
    void setUp() {
        // 创建模拟评价数据
        Review review1 = new Review();
        review1.setId(1L);
        review1.setHotelId(1L);
        review1.setOverallRating(5);
        review1.setCleanlinessRating(4);
        review1.setServiceRating(5);
        review1.setFacilitiesRating(4);
        review1.setLocationRating(5);
        review1.setComment("非常好的酒店，服务很棒");
        review1.setImages("image1.jpg,image2.jpg");
        review1.setStatus("APPROVED");
        review1.setCreatedAt(LocalDateTime.now().minusDays(1));

        Review review2 = new Review();
        review2.setId(2L);
        review2.setHotelId(1L);
        review2.setOverallRating(4);
        review2.setCleanlinessRating(4);
        review2.setServiceRating(4);
        review2.setFacilitiesRating(3);
        review2.setLocationRating(4);
        review2.setComment("不错，但设施可以更好");
        review2.setImages("");
        review2.setStatus("APPROVED");
        review2.setCreatedAt(LocalDateTime.now().minusDays(2));

        Review review3 = new Review();
        review3.setId(3L);
        review3.setHotelId(1L);
        review3.setOverallRating(3);
        review3.setCleanlinessRating(3);
        review3.setServiceRating(3);
        review3.setFacilitiesRating(3);
        review3.setLocationRating(3);
        review3.setComment("一般般");
        review3.setImages(null);
        review3.setStatus("APPROVED");
        review3.setCreatedAt(LocalDateTime.now().minusDays(3));

        mockReviews = Arrays.asList(review1, review2, review3);
    }

    @Test
    void getHotelStatistics_WithReviews_ReturnsCorrectStatistics() {
        // Given
        when(reviewRepository.findByHotelIdAndApprovedStatus(1L)).thenReturn(mockReviews);

        // When
        ReviewStatisticsResponse result = reviewStatisticsService.getHotelStatistics(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getHotelId());
        assertEquals(3L, result.getTotalReviews());
        assertEquals(4.0, result.getOverallRating()); // (5+4+3)/3 = 4.0
        assertEquals(3.7, result.getCleanlinessRating()); // (4+4+3)/3 = 3.7
        assertEquals(4.0, result.getServiceRating()); // (5+4+3)/3 = 4.0
        assertEquals(3.3, result.getFacilitiesRating()); // (4+3+3)/3 = 3.3
        assertEquals(4.0, result.getLocationRating()); // (5+4+3)/3 = 4.0

        // 检查评分分布
        assertEquals(1L, result.getRatingDistribution().getRating5());
        assertEquals(1L, result.getRatingDistribution().getRating4());
        assertEquals(1L, result.getRatingDistribution().getRating3());
        assertEquals(0L, result.getRatingDistribution().getRating2());
        assertEquals(0L, result.getRatingDistribution().getRating1());

        // 检查带图片评价数
        assertEquals(1L, result.getReviewsWithImages());

        // 检查平均评价长度
        assertTrue(result.getAverageCommentLength() > 0);

        verify(reviewRepository, times(1)).findByHotelIdAndApprovedStatus(1L);
    }

    @Test
    void getHotelStatistics_NoReviews_ReturnsEmptyStatistics() {
        // Given
        when(reviewRepository.findByHotelIdAndApprovedStatus(1L))
            .thenReturn(Collections.emptyList());

        // When
        ReviewStatisticsResponse result = reviewStatisticsService.getHotelStatistics(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getHotelId());
        assertEquals(0L, result.getTotalReviews());
        assertEquals(0.0, result.getOverallRating());
        assertEquals(0.0, result.getCleanlinessRating());
        assertEquals(0.0, result.getServiceRating());
        assertEquals(0.0, result.getFacilitiesRating());
        assertEquals(0.0, result.getLocationRating());
        assertEquals(0L, result.getReviewsWithImages());
        assertEquals(0.0, result.getAverageCommentLength());

        verify(reviewRepository, times(1)).findByHotelIdAndApprovedStatus(1L);
    }

    @Test
    void getHotelStatistics_WithNullComments_HandlesGracefully() {
        // Given - 创建包含null评论的评价
        Review reviewWithNullComment = new Review();
        reviewWithNullComment.setId(4L);
        reviewWithNullComment.setHotelId(1L);
        reviewWithNullComment.setOverallRating(5);
        reviewWithNullComment.setComment(null);
        reviewWithNullComment.setStatus("APPROVED");

        when(reviewRepository.findByHotelIdAndApprovedStatus(1L))
            .thenReturn(Arrays.asList(reviewWithNullComment));

        // When
        ReviewStatisticsResponse result = reviewStatisticsService.getHotelStatistics(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getTotalReviews());
        assertEquals(0.0, result.getAverageCommentLength()); // null评论应该被忽略
    }

    @Test
    void evictStatisticsCache_ClearsCache() {
        // When
        reviewStatisticsService.evictStatisticsCache(1L);

        // Then - 验证方法调用（实际清除缓存在Spring Cache中）
        // 这里主要是验证方法能正常调用，没有抛出异常
        assertDoesNotThrow(() -> reviewStatisticsService.evictStatisticsCache(1L));
    }

    @Test
    void getBatchStatistics_MultipleHotels_ReturnsCorrectResults() {
        // Given
        List<Long> hotelIds = Arrays.asList(1L, 2L);

        Review hotel2Review = new Review();
        hotel2Review.setId(5L);
        hotel2Review.setHotelId(2L);
        hotel2Review.setOverallRating(4);
        hotel2Review.setComment("不错的酒店");
        hotel2Review.setStatus("APPROVED");

        when(reviewRepository.findByHotelIdAndApprovedStatus(1L))
            .thenReturn(mockReviews);
        when(reviewRepository.findByHotelIdAndApprovedStatus(2L))
            .thenReturn(Arrays.asList(hotel2Review));

        // When
        Map<Long, ReviewStatisticsResponse> results =
            reviewStatisticsService.getBatchStatistics(hotelIds);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsKey(1L));
        assertTrue(results.containsKey(2L));

        ReviewStatisticsResponse hotel1Stats = results.get(1L);
        assertEquals(3L, hotel1Stats.getTotalReviews());

        ReviewStatisticsResponse hotel2Stats = results.get(2L);
        assertEquals(1L, hotel2Stats.getTotalReviews());
        assertEquals(4.0, hotel2Stats.getOverallRating());

        verify(reviewRepository, times(1)).findByHotelIdAndApprovedStatus(1L);
        verify(reviewRepository, times(1)).findByHotelIdAndApprovedStatus(2L);
    }

    @Test
    void getBatchStatistics_WithException_ReturnsEmptyStatsForFailedHotel() {
        // Given
        List<Long> hotelIds = Arrays.asList(1L, 2L);

        when(reviewRepository.findByHotelIdAndApprovedStatus(1L))
            .thenReturn(mockReviews);
        when(reviewRepository.findByHotelIdAndApprovedStatus(2L))
            .thenThrow(new RuntimeException("Database error"));

        // When
        Map<Long, ReviewStatisticsResponse> results =
            reviewStatisticsService.getBatchStatistics(hotelIds);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());

        ReviewStatisticsResponse hotel1Stats = results.get(1L);
        assertEquals(3L, hotel1Stats.getTotalReviews());

        ReviewStatisticsResponse hotel2Stats = results.get(2L);
        assertEquals(0L, hotel2Stats.getTotalReviews()); // 应该返回空统计
    }

    @Test
    void getSimpleStatistics_WithReviews_ReturnsBasicStats() {
        // Given
        when(reviewRepository.findByHotelIdAndApprovedStatus(1L))
            .thenReturn(mockReviews);

        // When
        Map<String, Object> result = reviewStatisticsService.getSimpleStatistics(1L);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.get("totalReviews"));
        assertEquals(4.0, result.get("overallRating"));

        verify(reviewRepository, times(1)).findByHotelIdAndApprovedStatus(1L);
    }

    @Test
    void getSimpleStatistics_NoReviews_ReturnsZeroStats() {
        // Given
        when(reviewRepository.findByHotelIdAndApprovedStatus(1L))
            .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = reviewStatisticsService.getSimpleStatistics(1L);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.get("totalReviews"));
        assertEquals(0.0, result.get("overallRating"));
    }

    @Test
    void calculateAverageRating_RoundsToOneDecimal() {
        // Given - 创建会产生多位小数的评价
        Review review1 = new Review();
        review1.setOverallRating(5);
        Review review2 = new Review();
        review2.setOverallRating(4);

        when(reviewRepository.findByHotelIdAndApprovedStatus(1L))
            .thenReturn(Arrays.asList(review1, review2));

        // When
        ReviewStatisticsResponse result = reviewStatisticsService.getHotelStatistics(1L);

        // Then - 验证保留一位小数
        assertEquals(4.5, result.getOverallRating());
        assertEquals(0.1, Math.abs(result.getOverallRating() - 4.5), 0.01);
    }

    @Test
    void calculateRatingDistribution_CorrectCounting() {
        // Given - 添加更多评价来测试分布
        Review extraReview5 = new Review();
        extraReview5.setOverallRating(5);
        extraReview5.setStatus("APPROVED");

        List<Review> reviewsWithExtra = Arrays.asList(
            mockReviews.get(0), // 5星
            mockReviews.get(1), // 4星
            mockReviews.get(2), // 3星
            extraReview5       // 5星
        );

        when(reviewRepository.findByHotelIdAndApprovedStatus(1L))
            .thenReturn(reviewsWithExtra);

        // When
        ReviewStatisticsResponse result = reviewStatisticsService.getHotelStatistics(1L);

        // Then
        assertEquals(2L, result.getRatingDistribution().getRating5()); // 两个5星
        assertEquals(1L, result.getRatingDistribution().getRating4());
        assertEquals(1L, result.getRatingDistribution().getRating3());
        assertEquals(0L, result.getRatingDistribution().getRating2());
        assertEquals(0L, result.getRatingDistribution().getRating1());
    }
}