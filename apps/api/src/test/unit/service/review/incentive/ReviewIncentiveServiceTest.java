package com.hotel.service.review.incentive;

import com.hotel.dto.review.incentive.UserPointsSummaryDTO;
import com.hotel.entity.IncentiveRule;
import com.hotel.entity.Review;
import com.hotel.entity.UserPoints;
import com.hotel.repository.IncentiveRuleRepository;
import com.hotel.repository.ReviewRepository;
import com.hotel.repository.UserPointsRepository;
import com.hotel.service.ReviewIncentiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewIncentiveServiceTest {

    @Mock
    private IncentiveRuleRepository incentiveRuleRepository;

    @Mock
    private UserPointsRepository userPointsRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewIncentiveService reviewIncentiveService;

    private Review testReview;
    private IncentiveRule testRule;

    @BeforeEach
    void setUp() {
        testReview = new Review();
        testReview.setId(1L);
        testReview.setUserId(100L);
        testReview.setOrderId(200L);
        testReview.setComment("这是一个很好的酒店，房间干净整洁，服务态度很好，值得推荐！");
        testReview.setImages("image1.jpg,image2.jpg");
        testReview.setOverallRating(5);
        testReview.setCleanlinessRating(5);
        testReview.setServiceRating(5);
        testReview.setCreatedAt(LocalDateTime.now());

        testRule = new IncentiveRule();
        testRule.setId(1L);
        testRule.setRuleType("POINTS_REVIEW");
        testRule.setPointsValue(10);
        testRule.setConditions("{\"minWords\": 10}");
        testRule.setIsActive(true);
        testRule.setValidFrom(LocalDate.now().minusDays(1));
    }

    @Test
    void testCalculateReviewQuality() {
        // Act
        int qualityScore = reviewIncentiveService.calculateReviewQuality(testReview);

        // Assert
        assertTrue(qualityScore >= 7, "高质量评价应该得分>=7");
        assertTrue(qualityScore <= 10, "评分不应超过10分");
    }

    @Test
    void testCalculateReviewQuality_MinContent() {
        // Arrange
        testReview.setComment("好");
        testReview.setImages(null);
        testReview.setCleanlinessRating(null);
        testReview.setServiceRating(null);

        // Act
        int qualityScore = reviewIncentiveService.calculateReviewQuality(testReview);

        // Assert
        assertTrue(qualityScore < 7, "低质量评价应该得分<7");
    }

    @Test
    void testAwardPointsForReview() {
        // Arrange
        when(reviewRepository.selectById(1L)).thenReturn(testReview);
        when(incentiveRuleRepository.findActiveRulesByType(eq("POINTS_REVIEW"), any(LocalDate.class)))
            .thenReturn(Arrays.asList(testRule));
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(100L)).thenReturn(Arrays.asList(testReview));

        // Act
        reviewIncentiveService.awardPointsForReview(100L, 1L);

        // Assert
        verify(userPointsRepository, atLeastOnce()).insert(any(UserPoints.class));
    }

    @Test
    void testGetUserPointsSummary() {
        // Arrange
        List<UserPoints> mockPoints = Arrays.asList(
            createMockUserPoints(1L, 100L, 10, "REVIEW", 1L),
            createMockUserPoints(2L, 100L, 20, "HIGH_QUALITY_REVIEW", 1L)
        );

        when(userPointsRepository.getUserActivePoints(eq(100L), any(LocalDate.class)))
            .thenReturn(30);
        when(userPointsRepository.getUserPointsHistory(eq(100L), eq(50)))
            .thenReturn(mockPoints);

        // Act
        UserPointsSummaryDTO summary = reviewIncentiveService.getUserPointsSummary(100L);

        // Assert
        assertNotNull(summary);
        assertEquals(100L, summary.getUserId());
        assertEquals(30, summary.getTotalPoints());
        assertEquals(2, summary.getRecentHistory().size());
    }

    @Test
    void testAwardPointsForReview_NonExistentReview() {
        // Arrange
        when(reviewRepository.selectById(1L)).thenReturn(null);

        // Act
        reviewIncentiveService.awardPointsForReview(100L, 1L);

        // Assert
        verify(userPointsRepository, never()).insert(any(UserPoints.class));
    }

    private UserPoints createMockUserPoints(Long id, Long userId, Integer points, String source, Long sourceId) {
        UserPoints userPoints = new UserPoints();
        userPoints.setId(id);
        userPoints.setUserId(userId);
        userPoints.setPoints(points);
        userPoints.setSource(source);
        userPoints.setSourceId(sourceId);
        userPoints.setCreatedAt(LocalDateTime.now());
        userPoints.setExpiresAt(LocalDate.now().plusYears(1));
        return userPoints;
    }
}