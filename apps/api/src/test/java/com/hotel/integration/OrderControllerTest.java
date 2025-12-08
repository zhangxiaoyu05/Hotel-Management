package com.hotel.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.HotelApiApplication;
import com.hotel.dto.order.CreateOrderRequest;
import com.hotel.entity.Room;
import com.hotel.entity.Hotel;
import com.hotel.entity.User;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.UserRepository;
import com.hotel.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HotelApiApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String token;
    private Room testRoom;
    private Hotel testHotel;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        testHotel = new Hotel();
        testHotel.setName("测试酒店");
        testHotel.setAddress("测试地址");
        testHotel.setStatus("ACTIVE");
        hotelRepository.insert(testHotel);

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setStatus("ACTIVE");
        userRepository.insert(testUser);

        token = jwtUtil.generateToken(testUser.getUsername(), testUser.getId(), "USER");

        testRoom = new Room();
        testRoom.setName("标准间");
        testRoom.setRoomNumber("101");
        testRoom.setPrice(new BigDecimal("298.00"));
        testRoom.setStatus("AVAILABLE");
        testRoom.setHotelId(testHotel.getId());
        roomRepository.insert(testRoom);
    }

    @Test
    void createOrder_Success() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRoomId(testRoom.getId());
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(2));
        request.setGuestCount(2);
        request.setGuestName("张三");
        request.setGuestPhone("13800138001");
        request.setGuestEmail("zhang@example.com");
        request.setSpecialRequests("需要无烟房");

        mockMvc.perform(post("/v1/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.totalPrice").exists())
                .andExpect(jsonPath("$.data.priceBreakdown").exists());
    }

    @Test
    void createOrder_InvalidRequest_ReturnsBadRequest() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRoomId(null);

        mockMvc.perform(post("/v1/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderById_Success() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRoomId(testRoom.getId());
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(2));
        request.setGuestCount(2);
        request.setGuestName("张三");
        request.setGuestPhone("13800138001");

        String createResponse = mockMvc.perform(post("/v1/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(createResponse)
                .get("data").get("id").asLong();

        mockMvc.perform(get("/v1/orders/" + orderId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.room").exists())
                .andExpect(jsonPath("$.data.hotel").exists());
    }

    @Test
    void getUserOrders_Success() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRoomId(testRoom.getId());
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(2));
        request.setGuestCount(2);
        request.setGuestName("张三");
        request.setGuestPhone("13800138001");

        mockMvc.perform(post("/v1/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse();

        mockMvc.perform(get("/v1/orders")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].orderNumber").exists());
    }

    @Test
    void cancelOrder_Success() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRoomId(testRoom.getId());
        request.setCheckInDate(LocalDate.now().plusDays(5));
        request.setCheckOutDate(LocalDate.now().plusDays(6));
        request.setGuestCount(2);
        request.setGuestName("张三");
        request.setGuestPhone("13800138001");

        String createResponse = mockMvc.perform(post("/v1/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(createResponse)
                .get("data").get("id").asLong();

        mockMvc.perform(put("/v1/orders/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getOrderWithoutAuthorization_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/v1/orders/1"))
                .andExpect(status().isUnauthorized());
    }
}