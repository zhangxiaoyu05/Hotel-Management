package com.hotel.integration.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.review.ReviewRequest;
import com.hotel.entity.*;
import com.hotel.enums.OrderStatus;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.UserRole;
import com.hotel.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class ReviewControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private User testUser;
    private Order testOrder;
    private Hotel testHotel;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // 创建测试酒店
        testHotel = new Hotel();
        testHotel.setName("测试酒店");
        testHotel.setAddress("测试地址");
        testHotel.setDescription("测试描述");
        hotelRepository.save(testHotel);

        // 创建测试房间
        testRoom = new Room();
        testRoom.setHotel(testHotel);
        testRoom.setRoomNumber("101");
        testRoom.setRoomType("标准间");
        testRoom.setPrice(new BigDecimal("299.00"));
        testRoom.setStatus(RoomStatus.AVAILABLE);
        roomRepository.save(testRoom);

        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.USER);
        userRepository.save(testUser);

        // 创建测试订单
        testOrder = new Order();
        testOrder.setUser(testUser);
        testOrder.setHotel(testHotel);
        testOrder.setRoom(testRoom);
        testOrder.setStatus(OrderStatus.COMPLETED);
        testOrder.setCheckInDate(LocalDateTime.now().minusDays(3));
        testOrder.setCheckOutDate(LocalDateTime.now().minusDays(1));
        testOrder.setTotalPrice(new BigDecimal("598.00"));
        orderRepository.save(testOrder);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createReview_EndToEnd_Success() throws Exception {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setOrderId(testOrder.getId());
        request.setOverallRating(5);
        request.setCleanlinessRating(4);
        request.setServiceRating(5);
        request.setFacilitiesRating(4);
        request.setLocationRating(5);
        request.setComment("非常好的入住体验，房间干净整洁，服务态度很好");
        request.setImages(Arrays.asList("image1.jpg", "image2.jpg"));
        request.setAnonymous(false);

        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("评价提交成功"))
                .andExpect(jsonPath("$.data.orderId").value(testOrder.getId()))
                .andExpect(jsonPath("$.data.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.data.overallRating").value(5))
                .andExpect(jsonPath("$.data.comment").value("非常好的入住体验，房间干净整洁，服务态度很好"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // Verify review is saved in database
        Review savedReview = reviewRepository.findByOrderIdAndUserId(testOrder.getId(), testUser.getId()).orElse(null);
        assertNotNull(savedReview);
        assertEquals(5, savedReview.getOverallRating());
        assertEquals("非常好的入住体验，房间干净整洁，服务态度很好", savedReview.getComment());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createReview_WithSensitiveWords_FiltersContent() throws Exception {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setOrderId(testOrder.getId());
        request.setOverallRating(1);
        request.setCleanlinessRating(1);
        request.setServiceRating(1);
        request.setFacilitiesRating(1);
        request.setLocationRating(1);
        request.setComment("这是一个垃圾酒店，服务态度极差，骗子！");
        request.setImages(Arrays.asList());
        request.setAnonymous(false);

        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comment").value("这是一个***酒店，服务态度极差，***！"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createReview_OrderNotCompleted_ReturnsError() throws Exception {
        // Given
        testOrder.setStatus(OrderStatus.PENDING);
        orderRepository.save(testOrder);

        ReviewRequest request = new ReviewRequest();
        request.setOrderId(testOrder.getId());
        request.setOverallRating(5);
        request.setCleanlinessRating(5);
        request.setServiceRating(5);
        request.setFacilitiesRating(5);
        request.setLocationRating(5);
        request.setComment("测试评价");
        request.setImages(Arrays.asList());
        request.setAnonymous(false);

        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createReview_DuplicateReview_ReturnsError() throws Exception {
        // Given
        // 创建第一个评价
        Review existingReview = new Review();
        existingReview.setUser(testUser);
        existingReview.setOrder(testOrder);
        existingReview.setHotel(testHotel);
        existingReview.setRoom(testRoom);
        existingReview.setOverallRating(5);
        existingReview.setComment("第一个评价");
        reviewRepository.save(existingReview);

        ReviewRequest request = new ReviewRequest();
        request.setOrderId(testOrder.getId());
        request.setOverallRating(4);
        request.setComment("第二个评价");
        request.setImages(Arrays.asList());
        request.setAnonymous(false);

        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getMyReviews_ReturnsUserReviews() throws Exception {
        // Given
        Review review1 = createTestReview(5, "第一个评价");
        Review review2 = createTestReview(4, "第二个评价");
        reviewRepository.saveAll(Arrays.asList(review1, review2));

        // When & Then
        mockMvc.perform(get("/v1/reviews/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].comment").exists())
                .andExpect(jsonPath("$.data[1].comment").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void canReviewOrder_BeforeReview_ReturnsTrue() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/reviews/can-review/{orderId}", testOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.canReview").value(true));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void canReviewOrder_AfterReview_ReturnsFalse() throws Exception {
        // Given
        Review review = createTestReview(5, "已评价");
        reviewRepository.save(review);

        // When & Then
        mockMvc.perform(get("/v1/reviews/can-review/{orderId}", testOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.canReview").value(false))
                .andExpect(jsonPath("$.data.reason").value("该订单已经评价过了"));
    }

    @Test
    void getHotelReviews_ReturnsHotelReviews() throws Exception {
        // Given
        Review review1 = createTestReview(5, "好评");
        Review review2 = createTestReview(3, "中评");
        reviewRepository.saveAll(Arrays.asList(review1, review2));

        // When & Then
        mockMvc.perform(get("/v1/reviews/hotel/{hotelId}", testHotel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createReview_AnonymousReview_HidesUserIdentity() throws Exception {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setOrderId(testOrder.getId());
        request.setOverallRating(5);
        request.setComment("匿名评价");
        request.setImages(Arrays.asList());
        request.setAnonymous(true);

        // When & Then
        mockMvc.perform(post("/v1/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAnonymous").value(true))
                .andExpect(jsonPath("$.data.userId").doesNotExist()); // 匿名评价不应返回用户ID

        // Verify review is saved as anonymous
        Review savedReview = reviewRepository.findByOrderIdAndUserId(testOrder.getId(), testUser.getId()).orElse(null);
        assertNotNull(savedReview);
        assertTrue(savedReview.getIsAnonymous());
    }

    private Review createTestReview(int rating, String comment) {
        Review review = new Review();
        review.setUser(testUser);
        review.setOrder(testOrder);
        review.setHotel(testHotel);
        review.setRoom(testRoom);
        review.setOverallRating(rating);
        review.setCleanlinessRating(rating);
        review.setServiceRating(rating);
        review.setFacilitiesRating(rating);
        review.setLocationRating(rating);
        review.setComment(comment);
        review.setImages(Arrays.asList());
        review.setIsAnonymous(false);
        review.setStatus("PENDING");
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }
}