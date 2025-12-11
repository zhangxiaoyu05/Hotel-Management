package com.hotel.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.entity.Hotel;
import com.hotel.entity.Review;
import com.hotel.entity.User;
import com.hotel.enums.Role;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.ReviewRepository;
import com.hotel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class ReviewControllerListTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Hotel testHotel;
    private List<Review> testReviews;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // 创建测试用户
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRole(Role.USER);
        testUser.setEnabled(true);
        userRepository.save(testUser);

        // 创建测试酒店
        testHotel = new Hotel();
        testHotel.setName("Test Hotel");
        testHotel.setDescription("Test Description");
        testHotel.setAddress("Test Address");
        testHotel.setCity("Test City");
        testHotel.setCountry("Test Country");
        testHotel.setRating(4.5);
        testHotel.setEnabled(true);
        testHotel = hotelRepository.save(testHotel);

        // 创建测试评价
        testReviews = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Review review = new Review();
            review.setHotelId(testHotel.getId());
            review.setUserId(testUser.getId());
            review.setOrderId((long) i);
            review.setOverallRating(i); // 1到5星
            review.setCleanlinessRating(i);
            review.setServiceRating(i);
            review.setFacilitiesRating(i);
            review.setLocationRating(i);
            review.setComment("Test review " + i);
            review.setImages(i % 2 == 0 ? "image" + i + ".jpg" : null); // 偶数评价有图片
            review.setAnonymous(false);
            review.setStatus("APPROVED");
            review.setCreatedAt(LocalDateTime.now().minusDays(i));
            review = reviewRepository.save(review);
            testReviews.add(review);
        }
    }

    @Test
    void getReviews_WithoutFilters_ReturnsAllReviews() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(5)))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0));
    }

    @Test
    void getReviews_WithHotelIdFilter_ReturnsFilteredReviews() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .param("page", "0")
                .param("size", "3")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void getReviews_WithMinRatingFilter_ReturnsHighRatedReviews() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .param("minRating", "4")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(2))) // 4星和5星评价
                .andExpect(jsonPath("$.data.content[*].overallRating", everyItem(greaterThanOrEqualTo(4))));
    }

    @Test
    void getReviews_WithMaxRatingFilter_ReturnsLowRatedReviews() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .param("maxRating", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(2))) // 1星和2星评价
                .andExpect(jsonPath("$.data.content[*].overallRating", everyItem(lessThanOrEqualTo(2))));
    }

    @Test
    void getReviews_WithRatingRangeFilter_ReturnsReviewsInRange() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .param("minRating", "3")
                .param("maxRating", "4")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(2))) // 3星和4星评价
                .andExpect(jsonPath("$.data.content[*].overallRating", everyItem(allOf(
                    greaterThanOrEqualTo(3), lessThanOrEqualTo(4)))));
    }

    @Test
    void getReviews_WithHasImagesTrue_ReturnsOnlyReviewsWithImages() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .param("hasImages", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(2))) // 只有2个评价有图片
                .andExpect(jsonPath("$.data.content[*].images", everyItem(notNullValue())));
    }

    @Test
    void getReviews_WithHasImagesFalse_ReturnsOnlyReviewsWithoutImages() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .param("hasImages", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(3))) // 3个评价没有图片
                .andExpect(jsonPath("$.data.content[*].images", everyItem(anyOf(nullValue(), isEmptyString()))));
    }

    @Test
    void getReviews_SortedByDate_ReturnsReviewsSortedByDate() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .param("sortBy", "date")
                .param("sortOrder", "desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].createdAt", greaterThan(
                    jsonPath("$.data.content[1].createdAt").toString())));
    }

    @Test
    void getReviews_SortedByRating_ReturnsReviewsSortedByRating() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .param("sortBy", "rating")
                .param("sortOrder", "desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].overallRating").value(5))
                .andExpect(jsonPath("$.data.content[1].overallRating").value(4));
    }

    @Test
    void getReviews_WithPagination_ReturnsCorrectPage() throws Exception {
        // 获取第二页
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .param("page", "1")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.size").value(2));
    }

    @Test
    void getReviews_InvalidHotelId_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", "99999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getReviews_InvalidRatingRange_ReturnsValidationError() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .param("minRating", "6") // 超出范围
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReviews_MissingHotelId_ReturnsValidationError() throws Exception {
        mockMvc.perform(get("/v1/reviews")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHotelStatistics_WithReviews_ReturnsCorrectStatistics() throws Exception {
        mockMvc.perform(get("/v1/reviews/statistics/{hotelId}", testHotel.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hotelId").value(testHotel.getId()))
                .andExpect(jsonPath("$.data.totalReviews").value(5))
                .andExpect(jsonPath("$.data.overallRating").value(3.0))
                .andExpect(jsonPath("$.data.ratingDistribution.rating5").value(1))
                .andExpect(jsonPath("$.data.ratingDistribution.rating4").value(1))
                .andExpect(jsonPath("$.data.ratingDistribution.rating3").value(1))
                .andExpect(jsonPath("$.data.ratingDistribution.rating2").value(1))
                .andExpect(jsonPath("$.data.ratingDistribution.rating1").value(1))
                .andExpect(jsonPath("$.data.reviewsWithImages").value(2));
    }

    @Test
    void getHotelStatistics_NoReviews_ReturnsZeroStatistics() throws Exception {
        // 创建一个没有评价的新酒店
        Hotel emptyHotel = new Hotel();
        emptyHotel.setName("Empty Hotel");
        emptyHotel.setDescription("No reviews");
        emptyHotel.setAddress("Empty Address");
        emptyHotel.setCity("Empty City");
        emptyHotel.setCountry("Empty Country");
        emptyHotel.setRating(0.0);
        emptyHotel.setEnabled(true);
        emptyHotel = hotelRepository.save(emptyHotel);

        mockMvc.perform(get("/v1/reviews/statistics/{hotelId}", emptyHotel.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hotelId").value(emptyHotel.getId()))
                .andExpect(jsonPath("$.data.totalReviews").value(0))
                .andExpect(jsonPath("$.data.overallRating").value(0.0))
                .andExpect(jsonPath("$.data.ratingDistribution.rating5").value(0))
                .andExpect(jsonPath("$.data.ratingDistribution.rating4").value(0))
                .andExpect(jsonPath("$.data.ratingDistribution.rating3").value(0))
                .andExpect(jsonPath("$.data.ratingDistribution.rating2").value(0))
                .andExpect(jsonPath("$.data.ratingDistribution.rating1").value(0))
                .andExpect(jsonPath("$.data.reviewsWithImages").value(0));
    }

    @Test
    void getHotelStatistics_InvalidHotelId_ReturnsError() throws Exception {
        mockMvc.perform(get("/v1/reviews/statistics/{hotelId}", 99999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalReviews").value(0));
    }

    @Test
    void getReviews_ExcludesPendingAndRejectedReviews() throws Exception {
        // 创建PENDING状态的评价
        Review pendingReview = new Review();
        pendingReview.setHotelId(testHotel.getId());
        pendingReview.setUserId(1L);
        pendingReview.setOrderId(100L);
        pendingReview.setOverallRating(5);
        pendingReview.setComment("Pending review");
        pendingReview.setStatus("PENDING");
        reviewRepository.save(pendingReview);

        // 创建REJECTED状态的评价
        Review rejectedReview = new Review();
        rejectedReview.setHotelId(testHotel.getId());
        rejectedReview.setUserId(1L);
        rejectedReview.setOrderId(101L);
        rejectedReview.setOverallRating(1);
        rejectedReview.setComment("Rejected review");
        rejectedReview.setStatus("REJECTED");
        reviewRepository.save(rejectedReview);

        mockMvc.perform(get("/v1/reviews")
                .param("hotelId", testHotel.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(5))) // 仍然是5个，不包含PENDING和REJECTED
                .andExpect(jsonPath("$.data.totalElements").value(5));
    }
}