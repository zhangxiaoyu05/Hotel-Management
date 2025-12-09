package com.hotel.service.review.incentive;

import com.hotel.dto.review.incentive.ReviewActivityDTO;
import com.hotel.entity.ReviewActivity;
import com.hotel.repository.ReviewActivityRepository;
import com.hotel.repository.UserPointsRepository;
import com.hotel.service.ReviewActivityService;
import com.hotel.service.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewActivityServiceTest {

    @Mock
    private ReviewActivityRepository reviewActivityRepository;

    @Mock
    private UserPointsRepository userPointsRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private ReviewActivityService reviewActivityService;

    private ReviewActivity testActivity;

    @BeforeEach
    void setUp() {
        testActivity = new ReviewActivity();
        testActivity.setId(1L);
        testActivity.setTitle("春季评价大赛");
        testActivity.setDescription("提交高质量评价赢取丰厚奖励");
        testActivity.setActivityType("REVIEW_CONTEST");
        testActivity.setStartDate(LocalDateTime.now().minusDays(1));
        testActivity.setEndDate(LocalDateTime.now().plusDays(30));
        testActivity.setIsActive(true);
        testActivity.setCreatedBy(1L);
        testActivity.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testGetActiveActivities() {
        // Arrange
        List<ReviewActivity> mockActivities = Arrays.asList(testActivity);
        when(reviewActivityRepository.findActiveActivities(any(LocalDateTime.class)))
            .thenReturn(mockActivities);

        // Act
        List<ReviewActivityDTO> result = reviewActivityService.getActiveActivities();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testActivity.getTitle(), result.get(0).getTitle());
        assertEquals("ACTIVE", result.get(0).getStatus());
    }

    @Test
    void testGetUpcomingActivities() {
        // Arrange
        testActivity.setStartDate(LocalDateTime.now().plusDays(1));
        List<ReviewActivity> mockActivities = Arrays.asList(testActivity);
        when(reviewActivityRepository.findUpcomingActivities(any(LocalDateTime.class)))
            .thenReturn(mockActivities);

        // Act
        List<ReviewActivityDTO> result = reviewActivityService.getUpcomingActivities();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testActivity.getTitle(), result.get(0).getTitle());
        assertEquals("UPCOMING", result.get(0).getStatus());
    }

    @Test
    void testJoinActivity_Success() {
        // Arrange
        when(userContextService.getCurrentUserId()).thenReturn(100L);
        when(reviewActivityRepository.selectById(1L)).thenReturn(testActivity);

        // Act
        var result = reviewActivityService.joinActivity(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("message"));
        assertEquals("成功参与评价竞赛活动", result.get("message"));
    }

    @Test
    void testJoinActivity_NotFound() {
        // Arrange
        when(reviewActivityRepository.selectById(1L)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            reviewActivityService.joinActivity(1L);
        });
        assertEquals("活动不存在", exception.getMessage());
    }

    @Test
    void testJoinActivity_NotActive() {
        // Arrange
        testActivity.setIsActive(false);
        when(reviewActivityRepository.selectById(1L)).thenReturn(testActivity);
        when(userContextService.getCurrentUserId()).thenReturn(100L);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            reviewActivityService.joinActivity(1L);
        });
        assertEquals("活动未激活", exception.getMessage());
    }

    @Test
    void testJoinActivity_NotStarted() {
        // Arrange
        testActivity.setStartDate(LocalDateTime.now().plusDays(1));
        when(reviewActivityRepository.selectById(1L)).thenReturn(testActivity);
        when(userContextService.getCurrentUserId()).thenReturn(100L);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            reviewActivityService.joinActivity(1L);
        });
        assertEquals("活动尚未开始", exception.getMessage());
    }

    @Test
    void testJoinActivity_Ended() {
        // Arrange
        testActivity.setEndDate(LocalDateTime.now().minusDays(1));
        when(reviewActivityRepository.selectById(1L)).thenReturn(testActivity);
        when(userContextService.getCurrentUserId()).thenReturn(100L);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            reviewActivityService.joinActivity(1L);
        });
        assertEquals("活动已结束", exception.getMessage());
    }

    @Test
    void testCreateActivity() {
        // Arrange
        when(userContextService.getCurrentUserId()).thenReturn(100L);
        when(reviewActivityRepository.insert(any(ReviewActivity.class))).thenReturn(1);

        ReviewActivityDTO dto = new ReviewActivityDTO();
        dto.setTitle("新活动");
        dto.setDescription("活动描述");
        dto.setActivityType("DOUBLE_POINTS");
        dto.setStartDate(LocalDateTime.now());
        dto.setEndDate(LocalDateTime.now().plusDays(7));
        dto.setIsActive(true);

        // Act
        ReviewActivityDTO result = reviewActivityService.createActivity(dto);

        // Assert
        assertNotNull(result);
        assertEquals(dto.getTitle(), result.getTitle());
        verify(reviewActivityRepository).insert(any(ReviewActivity.class));
    }

    @Test
    void testUpdateActivity() {
        // Arrange
        when(reviewActivityRepository.selectById(1L)).thenReturn(testActivity);
        when(reviewActivityRepository.updateById(any(ReviewActivity.class))).thenReturn(1);

        ReviewActivityDTO dto = new ReviewActivityDTO();
        dto.setTitle("更新后的活动");
        dto.setDescription("更新后的描述");

        // Act
        ReviewActivityDTO result = reviewActivityService.updateActivity(1L, dto);

        // Assert
        assertNotNull(result);
        assertEquals(dto.getTitle(), result.getTitle());
        verify(reviewActivityRepository).updateById(any(ReviewActivity.class));
    }

    @Test
    void testUpdateActivity_NotFound() {
        // Arrange
        when(reviewActivityRepository.selectById(1L)).thenReturn(null);

        ReviewActivityDTO dto = new ReviewActivityDTO();

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            reviewActivityService.updateActivity(1L, dto);
        });
        assertEquals("活动不存在", exception.getMessage());
    }

    @Test
    void testDeleteActivity() {
        // Arrange
        when(reviewActivityRepository.selectById(1L)).thenReturn(testActivity);
        when(reviewActivityRepository.deleteById(1L)).thenReturn(1);

        // Act
        reviewActivityService.deleteActivity(1L);

        // Assert
        verify(reviewActivityRepository).deleteById(1L);
    }

    @Test
    void testDeleteActivity_NotFound() {
        // Arrange
        when(reviewActivityRepository.selectById(1L)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            reviewActivityService.deleteActivity(1L);
        });
        assertEquals("活动不存在", exception.getMessage());
    }
}