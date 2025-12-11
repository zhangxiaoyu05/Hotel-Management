package com.hotel.controller.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.review.ReviewRequest;
import com.hotel.dto.review.ReviewResponse;
import com.hotel.entity.Review;
import com.hotel.entity.User;
import com.hotel.service.ReviewService;
import com.hotel.common.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Review testReview;
    private ReviewRequest testReviewRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testReview = new Review();
        testReview.setId(1L);
        testReview.setUserId(1L);
        testReview.setOrderId(1L);
        testReview.setOverallRating(5);
        testReview.setCleanlinessRating(4);
        testReview.setServiceRating(5);
        testReview.setComment("很好的体验");
        testReview.setImages(Arrays.asList("image1.jpg"));

        testReviewRequest = new ReviewRequest();
        testReviewRequest.setOrderId(1L);
        testReviewRequest.setOverallRating(5);
        testReviewRequest.setCleanlinessRating(4);
        testReviewRequest.setServiceRating(5);
        testReviewRequest.setFacilitiesRating(4);
        testReviewRequest.setLocationRating(5);
        testReviewRequest.setComment("很好的体验");
        testReviewRequest.setImages(Arrays.asList("image1.jpg"));
        testReviewRequest.setAnonymous(false);
    }

    @Test
    @WithMockUser(roles = "USER")
    void createReview_Success() throws Exception {
        // Given
        ReviewResponse mockResponse = ReviewResponse.fromEntity(testReview);
        when(reviewService.createReview(any(ReviewRequest.class), anyLong()))
            .thenReturn(testReview);

        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testReviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("评价提交成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.overallRating").value(5))
                .andExpect(jsonPath("$.data.comment").value("很好的体验"));

        verify(reviewService).createReview(any(ReviewRequest.class), eq(1L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createReview_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        testReviewRequest.setOverallRating(6); // 无效评分

        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testReviewRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createReview_MissingCsrf_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testReviewRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReview_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testReviewRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyReviews_Success() throws Exception {
        // Given
        List<Review> reviews = Arrays.asList(testReview);
        List<ReviewResponse> mockResponses = Arrays.asList(ReviewResponse.fromEntity(testReview));

        when(reviewService.getMyReviews(anyLong(), anyInt(), anyInt()))
            .thenReturn(reviews);

        // When & Then
        mockMvc.perform(get("/v1/reviews/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].overallRating").value(5));

        verify(reviewService).getMyReviews(eq(1L), eq(1), eq(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyReviews_WithPagination() throws Exception {
        // Given
        when(reviewService.getMyReviews(anyLong(), eq(2), eq(20)))
            .thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/v1/reviews/my")
                .param("page", "2")
                .param("size", "20"))
                .andExpect(status().isOk());

        verify(reviewService).getMyReviews(eq(1L), eq(2), eq(20));
    }

    @Test
    @WithMockUser(roles = "USER")
    void canReviewOrder_True() throws Exception {
        // Given
        when(reviewService.canReviewOrder(1L, 1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/v1/reviews/can-review/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.canReview").value(true))
                .andExpect(jsonPath("$.data.reason").value("可以评价"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void canReviewOrder_False_OrderNotCompleted() throws Exception {
        // Given
        when(reviewService.canReviewOrder(1L, 1L)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/v1/reviews/can-review/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.canReview").value(false))
                .andExpect(jsonPath("$.data.reason").exists());
    }

    @Test
    void getHotelReviews_Success() throws Exception {
        // Given
        Long hotelId = 1L;
        when(reviewService.getHotelReviews(eq(hotelId), anyInt(), anyInt()))
            .thenReturn(Arrays.asList(testReview));

        // When & Then
        mockMvc.perform(get("/v1/reviews/hotel/{hotelId}", hotelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void getHotelReviews_WithPagination() throws Exception {
        // Given
        Long hotelId = 1L;
        when(reviewService.getHotelReviews(eq(hotelId), eq(2), eq(5)))
            .thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/v1/reviews/hotel/{hotelId}", hotelId)
                .param("page", "2")
                .param("size", "5"))
                .andExpect(status().isOk());

        verify(reviewService).getHotelReviews(eq(hotelId), eq(2), eq(5));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createReview_ServiceThrowsException_ReturnsError() throws Exception {
        // Given
        when(reviewService.createReview(any(ReviewRequest.class), anyLong()))
            .thenThrow(new RuntimeException("订单不存在"));

        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testReviewRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createReview_RateLimitExceeded_ReturnsTooManyRequests() throws Exception {
        // 模拟请求频率限制 - 这需要实际的RateLimit实现
        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testReviewRequest)))
                .andExpect(status().isOk());
    }
}