package com.hotel.service.review.incentive;

import com.hotel.entity.Review;
import com.hotel.repository.HighQualityReviewBadgeRepository;
import com.hotel.repository.ReviewRepository;
import com.hotel.service.ReviewQualityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewQualityServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private HighQualityReviewBadgeRepository highQualityReviewBadgeRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private ReviewQualityService reviewQualityService;

    private Review testReview;

    @BeforeEach
    void setUp() {
        testReview = new Review();
        testReview.setId(1L);
        testReview.setUserId(100L);
        testReview.setComment("这是一个很棒的酒店，房间非常干净整洁，服务人员态度友好，设施也很完善，位置便利，强烈推荐给大家！");
        testReview.setImages("image1.jpg,image2.jpg,image3.jpg");
        testReview.setOverallRating(5);
        testReview.setCleanlinessRating(5);
        testReview.setServiceRating(5);
        testReview.setFacilitiesRating(4);
        testReview.setLocationRating(5);
    }

    @Test
    void testCalculateQualityScore_HighQuality() {
        // Act
        int qualityScore = reviewQualityService.calculateQualityScore(testReview);

        // Assert
        assertTrue(qualityScore >= 7, "高质量评价应该得分>=7");
        assertEquals(10, qualityScore, "完整高质量评价应该得满分10");
    }

    @Test
    void testCalculateQualityScore_MediumQuality() {
        // Arrange
        testReview.setComment("不错的酒店，服务还可以");
        testReview.setImages("image1.jpg");

        // Act
        int qualityScore = reviewQualityService.calculateQualityScore(testReview);

        // Assert
        assertTrue(qualityScore >= 4 && qualityScore <= 6, "中等质量评价应该得分4-6");
    }

    @Test
    void testCalculateQualityScore_LowQuality() {
        // Arrange
        testReview.setComment("好");
        testReview.setImages(null);
        testReview.setCleanlinessRating(null);
        testReview.setServiceRating(null);
        testReview.setFacilitiesRating(null);
        testReview.setLocationRating(null);

        // Act
        int qualityScore = reviewQualityService.calculateQualityScore(testReview);

        // Assert
        assertTrue(qualityScore < 7, "低质量评价应该得分<7");
    }

    @Test
    void testAnalyzeContentQuality() {
        // 使用反射调用私有方法进行测试
        try {
            var method = ReviewQualityService.class.getDeclaredMethod("analyzeContentQuality", String.class);
            method.setAccessible(true);

            // 测试高质量内容
            int score1 = (int) method.invoke(reviewQualityService, "房间很干净，服务很好，强烈推荐");
            assertTrue(score1 >= 1, "高质量内容应该得分>=1");

            // 测试低质量内容
            int score2 = (int) method.invoke(reviewQualityService, "好");
            assertTrue(score2 >= 0, "低质量内容应该得分>=0");

        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    @Test
    void testEvaluatePendingReviews() {
        // Arrange
        List<Review> pendingReviews = Arrays.asList(testReview);
        when(reviewRepository.findByStatus("PENDING")).thenReturn(pendingReviews);
        when(highQualityReviewBadgeRepository.existsByReviewId(1L)).thenReturn(false);

        // Act
        reviewQualityService.evaluatePendingReviews();

        // Assert
        verify(reviewRepository, atLeastOnce()).updateById(any(Review.class));
    }

    @Test
    void testClearLeaderboardCache() {
        // Arrange
        when(redisTemplate.keys(any(String.class))).thenReturn(Arrays.asList("key1", "key2"));

        // Act
        reviewQualityService.clearLeaderboardCache();

        // Assert
        verify(redisTemplate).delete(Arrays.asList("key1", "key2"));
    }
}