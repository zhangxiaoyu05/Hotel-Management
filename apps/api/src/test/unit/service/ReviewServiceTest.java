package com.hotel.service;

import com.hotel.dto.review.ReviewRequest;
import com.hotel.entity.Review;
import com.hotel.entity.Order;
import com.hotel.entity.User;
import com.hotel.enums.OrderStatus;
import com.hotel.exception.BusinessException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.repository.ReviewRepository;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.UserRepository;
import com.hotel.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileService fileService;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Order testOrder;
    private ReviewRequest testReviewRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setStatus(OrderStatus.COMPLETED);

        testReviewRequest = new ReviewRequest();
        testReviewRequest.setOrderId(1L);
        testReviewRequest.setOverallRating(5);
        testReviewRequest.setCleanlinessRating(4);
        testReviewRequest.setServiceRating(5);
        testReviewRequest.setFacilitiesRating(4);
        testReviewRequest.setLocationRating(5);
        testReviewRequest.setComment("很好的入住体验");
        testReviewRequest.setImages(Arrays.asList("image1.jpg", "image2.jpg"));
        testReviewRequest.setAnonymous(false);
    }

    @Test
    void createReview_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(reviewRepository.existsByOrderIdAndUserId(1L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(1L);
            return review;
        });

        // When
        Review result = reviewService.createReview(testReviewRequest, 1L);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getOverallRating());
        assertEquals(4, result.getCleanlinessRating());
        assertEquals(5, result.getServiceRating());
        assertEquals("很好的入住体验", result.getComment());
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getOrderId());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_OrderNotCompleted_ThrowsException() {
        // Given
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> reviewService.createReview(testReviewRequest, 1L)
        );
        assertEquals("只能评价已完成的订单", exception.getMessage());
    }

    @Test
    void createReview_OrderNotFound_ThrowsException() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> reviewService.createReview(testReviewRequest, 1L)
        );
        assertTrue(exception.getMessage().contains("订单不存在"));
    }

    @Test
    void createReview_UserNotOrderOwner_ThrowsException() {
        // Given
        User anotherUser = new User();
        anotherUser.setId(2L);
        testOrder.setUser(anotherUser);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> reviewService.createReview(testReviewRequest, 1L)
        );
        assertEquals("只能评价自己的订单", exception.getMessage());
    }

    @Test
    void createReview_DuplicateReview_ThrowsException() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(reviewRepository.existsByOrderIdAndUserId(1L, 1L)).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> reviewService.createReview(testReviewRequest, 1L)
        );
        assertEquals("该订单已经评价过了", exception.getMessage());
    }

    @Test
    void createReview_WithSensitiveWords_FiltersContent() {
        // Given
        testReviewRequest.setComment("这是一个垃圾酒店，服务很差");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(reviewRepository.existsByOrderIdAndUserId(1L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(1L);
            return review;
        });

        // When
        Review result = reviewService.createReview(testReviewRequest, 1L);

        // Then
        assertTrue(result.getComment().contains("***"));
        assertFalse(result.getComment().contains("垃圾"));
    }

    @Test
    void canReviewOrder_True() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(reviewRepository.existsByOrderIdAndUserId(1L, 1L)).thenReturn(false);

        // When
        Boolean result = reviewService.canReviewOrder(1L, 1L);

        // Then
        assertTrue(result);
    }

    @Test
    void canReviewOrder_False_OrderNotCompleted() {
        // Given
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Boolean result = reviewService.canReviewOrder(1L, 1L);

        // Then
        assertFalse(result);
    }

    @Test
    void canReviewOrder_False_AlreadyReviewed() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(reviewRepository.existsByOrderIdAndUserId(1L, 1L)).thenReturn(true);

        // When
        Boolean result = reviewService.canReviewOrder(1L, 1L);

        // Then
        assertFalse(result);
    }

    @Test
    void validateRatings_AllValid_NoException() {
        // Given
        ReviewRequest validRequest = new ReviewRequest();
        validRequest.setOverallRating(5);
        validRequest.setCleanlinessRating(4);
        validRequest.setServiceRating(3);
        validRequest.setFacilitiesRating(5);
        validRequest.setLocationRating(4);

        // When & Then
        assertDoesNotThrow(() -> reviewService.createReview(validRequest, 1L));
    }

    @Test
    void validateRatings_InvalidRating_ThrowsException() {
        // Given
        testReviewRequest.setOverallRating(6); // 超出范围

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> reviewService.createReview(testReviewRequest, 1L)
        );
        assertTrue(exception.getMessage().contains("评分必须在1-5之间"));
    }
}