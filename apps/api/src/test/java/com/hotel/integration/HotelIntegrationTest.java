package com.hotel.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.hotel.*;
import com.hotel.entity.Hotel;
import com.hotel.enums.HotelStatus;
import com.hotel.repository.HotelRepository;
import com.hotel.service.HotelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("酒店服务集成测试")
class HotelIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HotelService hotelService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private Hotel testHotel;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 创建测试酒店
        testHotel = new Hotel();
        testHotel.setName("测试酒店");
        testHotel.setAddress("测试地址123号");
        testHotel.setPhone("13800138000");
        testHotel.setDescription("这是一个测试酒店");
        testHotel.setFacilities("[\"WiFi\", \"停车场\", \"游泳池\"]");
        testHotel.setImages("[\"hotel1.jpg\", \"hotel2.jpg\"]");
        testHotel.setStatus(HotelStatus.ACTIVE.name());
        testHotel.setCreatedBy(1L);
        testHotel.setCreatedAt(LocalDateTime.now());
        testHotel.setUpdatedAt(LocalDateTime.now());
        testHotel.setDeleted(0);

        hotelRepository.insert(testHotel);
    }

    @AfterEach
    void tearDown() {
        hotelRepository.deleteById(testHotel.getId());
    }

    @Test
    @DisplayName("完整的酒店管理流程测试")
    @WithMockUser(roles = "ADMIN")
    void completeHotelManagementWorkflow() throws Exception {
        // 1. 创建新酒店
        CreateHotelRequest createRequest = new CreateHotelRequest();
        createRequest.setName("新创建的酒店");
        createRequest.setAddress("新创建的酒店地址");
        createRequest.setPhone("13900139000");
        createRequest.setDescription("这是一个新创建的酒店");
        createRequest.setFacilities(Arrays.asList("WiFi", "健身房", "餐厅"));
        createRequest.setImages(Arrays.asList("new-hotel.jpg"));

        String createResponse = mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("酒店创建成功"))
                .andExpect(jsonPath("$.data.name").value("新创建的酒店"))
                .andReturn().getResponse().getContentAsString();

        // 解析创建响应获取酒店ID
        HotelResponse createdHotel = objectMapper.readValue(createResponse,
                objectMapper.getTypeFactory().constructParametricType(
                        com.hotel.dto.ApiResponse.class, HotelResponse.class))
                .getData();
        Long hotelId = createdHotel.getId();

        // 2. 获取酒店详情
        mockMvc.perform(get("/api/hotels/" + hotelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(hotelId))
                .andExpect(jsonPath("$.data.name").value("新创建的酒店"));

        // 3. 更新酒店信息
        UpdateHotelRequest updateRequest = new UpdateHotelRequest();
        updateRequest.setName("更新后的酒店名称");
        updateRequest.setDescription("更新后的酒店描述");
        updateRequest.setFacilities(Arrays.asList("WiFi", "健身房", "餐厅", "SPA"));

        mockMvc.perform(put("/api/hotels/" + hotelId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新后的酒店名称"))
                .andExpect(jsonPath("$.data.description").value("更新后的酒店描述"))
                .andExpect(jsonPath("$.data.facilities").isArray())
                .andExpect(jsonPath("$.data.facilities.length()").value(4));

        // 4. 获取酒店列表验证更新
        mockMvc.perform(get("/api/hotels")
                        .param("search", "更新后的酒店名称")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[?(@.id == " + hotelId + ")]").exists());

        // 5. 更新酒店状态为停业
        UpdateHotelStatusRequest statusRequest = new UpdateHotelStatusRequest();
        statusRequest.setStatus(HotelStatus.INACTIVE.name());

        mockMvc.perform(put("/api/hotels/" + hotelId + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value(HotelStatus.INACTIVE.name()));

        // 6. 验证状态更新
        mockMvc.perform(get("/api/hotels/" + hotelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value(HotelStatus.INACTIVE.name()));

        // 7. 删除酒店
        mockMvc.perform(delete("/api/hotels/" + hotelId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("酒店删除成功"));

        // 8. 验证删除
        mockMvc.perform(get("/api/hotels/" + hotelId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("酒店搜索和分页功能测试")
    void hotelSearchAndPaginationTest() throws Exception {
        // 创建更多测试酒店
        for (int i = 1; i <= 15; i++) {
            Hotel hotel = new Hotel();
            hotel.setName("搜索测试酒店" + i);
            hotel.setAddress("测试地址" + i + "号");
            hotel.setPhone("1380013800" + String.format("%02d", i));
            hotel.setDescription("这是第" + i + "个测试酒店");
            hotel.setFacilities("[\"WiFi\", \"停车场\"]");
            hotel.setImages("[\"hotel" + i + ".jpg\"]");
            hotel.setStatus(i % 2 == 0 ? HotelStatus.ACTIVE.name() : HotelStatus.INACTIVE.name());
            hotel.setCreatedBy(1L);
            hotel.setCreatedAt(LocalDateTime.now());
            hotel.setUpdatedAt(LocalDateTime.now());
            hotel.setDeleted(0);
            hotelRepository.insert(hotel);
        }

        try {
            // 测试搜索功能
            mockMvc.perform(get("/api/hotels")
                            .param("search", "搜索测试酒店")
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(5))
                    .andExpect(jsonPath("$.data.totalElements").value(15))
                    .andExpect(jsonPath("$.data.totalPages").value(3));

            // 测试状态筛选
            mockMvc.perform(get("/api/hotels")
                            .param("status", "ACTIVE")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[?(@.status == 'ACTIVE')]").exists())
                    .andExpect(jsonPath("$.data.content[?(@.status == 'INACTIVE')]").doesNotExist());

            // 测试排序功能
            mockMvc.perform(get("/api/hotels")
                            .param("sortBy", "name")
                            .param("sortDir", "ASC")
                            .param("page", "0")
                            .param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].name").value("搜索测试酒店1"))
                    .andExpect(jsonPath("$.data.content[1].name").value("搜索测试酒店10"))
                    .andExpect(jsonPath("$.data.content[2].name").value("搜索测试酒店11"));

            // 测试第二页
            mockMvc.perform(get("/api/hotels")
                            .param("page", "1")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.number").value(1))
                    .andExpect(jsonPath("$.data.content.length()").value(5));

        } finally {
            // 清理测试数据
            for (int i = 1; i <= 15; i++) {
                List<Hotel> hotels = hotelRepository.selectByName("搜索测试酒店" + i);
                hotels.forEach(hotel -> hotelRepository.deleteById(hotel.getId()));
            }
        }
    }

    @Test
    @DisplayName("酒店数据验证测试")
    @WithMockUser(roles = "ADMIN")
    void hotelDataValidationTest() throws Exception {
        // 测试创建酒店时的数据验证
        CreateHotelRequest invalidRequest1 = new CreateHotelRequest();
        invalidRequest1.setName(""); // 空名称
        invalidRequest1.setAddress("测试地址");

        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        CreateHotelRequest invalidRequest2 = new CreateHotelRequest();
        invalidRequest2.setName("测试酒店");
        invalidRequest2.setAddress(""); // 空地址

        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        CreateHotelRequest invalidRequest3 = new CreateHotelRequest();
        invalidRequest3.setName("a".repeat(101)); // 超长名称
        invalidRequest3.setAddress("测试地址");

        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 测试无效的电话号码格式
        CreateHotelRequest invalidRequest4 = new CreateHotelRequest();
        invalidRequest4.setName("测试酒店");
        invalidRequest4.setAddress("测试地址");
        invalidRequest4.setPhone("invalid-phone");

        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest4)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("酒店并发操作测试")
    @WithMockUser(roles = "ADMIN")
    void hotelConcurrentOperationTest() throws Exception {
        // 创建酒店
        CreateHotelRequest createRequest = new CreateHotelRequest();
        createRequest.setName("并发测试酒店");
        createRequest.setAddress("并发测试地址");
        createRequest.setPhone("13800138001");

        String createResponse = mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        HotelResponse createdHotel = objectMapper.readValue(createResponse,
                objectMapper.getTypeFactory().constructParametricType(
                        com.hotel.dto.ApiResponse.class, HotelResponse.class))
                .getData();
        Long hotelId = createdHotel.getId();

        // 模拟并发更新
        UpdateHotelRequest updateRequest1 = new UpdateHotelRequest();
        updateRequest1.setDescription("并发更新描述1");

        UpdateHotelRequest updateRequest2 = new UpdateHotelRequest();
        updateRequest2.setDescription("并发更新描述2");

        // 两次更新操作，最后一次应该生效
        mockMvc.perform(put("/api/hotels/" + hotelId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest1)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/hotels/" + hotelId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest2)))
                .andExpect(status().isOk());

        // 验证最终状态
        mockMvc.perform(get("/api/hotels/" + hotelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("并发更新描述2"));

        // 清理
        hotelRepository.deleteById(hotelId);
    }

    @Test
    @DisplayName("酒店图片上传集成测试")
    @WithMockUser(roles = "ADMIN")
    void hotelImageUploadIntegrationTest() throws Exception {
        // 创建酒店
        CreateHotelRequest createRequest = new CreateHotelRequest();
        createRequest.setName("图片测试酒店");
        createRequest.setAddress("图片测试地址");

        String createResponse = mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        HotelResponse createdHotel = objectMapper.readValue(createResponse,
                objectMapper.getTypeFactory().constructParametricType(
                        com.hotel.dto.ApiResponse.class, HotelResponse.class))
                .getData();
        Long hotelId = createdHotel.getId();

        try {
            // 测试图片上传
            MockMultipartFile imageFile = new MockMultipartFile(
                    "file",
                    "hotel-test.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "test image content".getBytes()
            );

            mockMvc.perform(multipart("/api/upload/hotel-image")
                            .file(imageFile)
                            .param("hotelId", hotelId.toString())
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.fileName").value("hotel-test.jpg"));

            // 更新酒店信息以包含图片
            UpdateHotelRequest updateRequest = new UpdateHotelRequest();
            updateRequest.setImages(Arrays.asList("hotel-test.jpg"));

            mockMvc.perform(put("/api/hotels/" + hotelId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images[0]").value("hotel-test.jpg"));

        } finally {
            // 清理
            hotelRepository.deleteById(hotelId);
        }
    }
}