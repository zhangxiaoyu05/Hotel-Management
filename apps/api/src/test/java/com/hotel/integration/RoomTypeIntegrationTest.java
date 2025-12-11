package com.hotel.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.roomtype.CreateRoomTypeRequest;
import com.hotel.dto.roomtype.RoomTypeListResponse;
import com.hotel.dto.roomtype.UpdateRoomTypeRequest;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 房间类型集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("房间类型集成测试")
@Transactional
class RoomTypeIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @MockBean
    private HotelService hotelService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Mock hotel service
        when(hotelService.getHotelById(anyLong())).thenReturn(
                com.hotel.dto.HotelResponse.builder()
                        .id(1L)
                        .name("测试酒店")
                        .build()
        );
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("创建房间类型 - 完整流程测试")
    void testCreateRoomType_CompleteFlow() throws Exception {
        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setHotelId(1L);
        request.setName("豪华套房");
        request.setCapacity(4);
        request.setBasePrice(new BigDecimal("888.00"));
        request.setFacilities(Arrays.asList("WiFi", "空调", "电视", "迷你吧"));
        request.setDescription("豪华的套房");
        request.setIconUrl("https://example.com/luxury-icon.png");

        mockMvc.perform(post("/api/room-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("豪华套房"))
                .andExpect(jsonPath("$.data.capacity").value(4))
                .andExpect(jsonPath("$.data.basePrice").value(888.00))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("房间类型创建成功"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("更新房间类型 - 完整流程测试")
    void testUpdateRoomType_CompleteFlow() throws Exception {
        // 首先创建一个房间类型
        Long roomTypeId = createTestRoomType();

        // 然后更新它
        UpdateRoomTypeRequest updateRequest = new UpdateRoomTypeRequest();
        updateRequest.setName("商务间");
        updateRequest.setCapacity(2);
        updateRequest.setBasePrice(new BigDecimal("399.00"));
        updateRequest.setFacilities(Arrays.asList("WiFi", "空调", "办公桌"));
        updateRequest.setDescription("适合商务人士的房间");
        updateRequest.setIconUrl("https://example.com/business-icon.png");

        mockMvc.perform(put("/api/room-types/" + roomTypeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("商务间"))
                .andExpect(jsonPath("$.data.capacity").value(2))
                .andExpect(jsonPath("$.data.basePrice").value(399.00))
                .andExpect(jsonPath("$.message").value("房间类型更新成功"));
    }

    @Test
    @DisplayName("获取房间类型列表 - 分页测试")
    void testGetRoomTypes_WithPagination() throws Exception {
        // 创建测试数据
        createTestRoomType();
        createTestRoomType();

        mockMvc.perform(get("/api/room-types")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0));
    }

    @Test
    @DisplayName("搜索房间类型 - 关键词搜索")
    void testSearchRoomTypes_ByKeyword() throws Exception {
        // 创建不同名称的房间类型
        createRoomTypeWithName("豪华套房");
        createRoomTypeWithName("标准间");
        createRoomTypeWithName("商务间");

        // 搜索包含"豪华"的房间类型
        mockMvc.perform(get("/api/room-types")
                        .param("search", "豪华")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpected(jsonPath("$.data.content[*].name").value(hasItem("豪华套房")));
    }

    @Test
    @DisplayName("获取房间类型详情")
    void testGetRoomTypeById() throws Exception {
        Long roomTypeId = createTestRoomType();

        mockMvc.perform(get("/api/room-types/" + roomTypeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(roomTypeId))
                .andExpect(jsonPath("$.data.name").value("测试房间类型"))
                .andExpect(jsonPath("$.data.capacity").value(2))
                .andExpect(jsonPath("$.data.basePrice").value(299.00));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("删除房间类型 - 成功")
    void testDeleteRoomType_Success() throws Exception {
        Long roomTypeId = createTestRoomType();

        mockMvc.perform(delete("/api/room-types/" + roomTypeId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("房间类型删除成功"));

        // 验证房间类型已被软删除
        mockMvc.perform(get("/api/room-types/" + roomTypeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("更新房间类型状态")
    void testUpdateRoomTypeStatus() throws Exception {
        Long roomTypeId = createTestRoomType();

        mockMvc.perform(put("/api/room-types/" + roomTypeId + "/status")
                        .with(csrf())
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.message").value("房间类型状态更新成功"));
    }

    @Test
    @DisplayName("获取活跃房间类型列表")
    void testGetActiveRoomTypes() throws Exception {
        // 创建不同状态的房间类型
        createTestRoomType(); // 默认 ACTIVE
        Long inactiveRoomTypeId = createTestRoomType();
        setRoomTypeStatus(inactiveRoomTypeId, "INACTIVE");

        mockMvc.perform(get("/api/room-types/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].status").value(everyItem(equalTo("ACTIVE"))));
    }

    @Test
    @DisplayName("根据酒店ID获取房间类型")
    void testGetRoomTypesByHotel() throws Exception {
        createTestRoomTypeWithHotel(1L);
        createTestRoomTypeWithHotel(1L);
        createTestRoomTypeWithHotel(2L);

        mockMvc.perform(get("/api/room-types/hotel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data[*].hotelId").value(everyItem(equalTo(1))));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("房间类型管理端点 - 权限测试")
    void testAdminEndpoints_Authorization() throws Exception {
        // 测试未认证用户的访问
        mockMvc.perform(post("/api/room-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // 测试普通用户访问管理员端点
        mockMvc.perform(post("/api/room-types")
                        .with(csrf())
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // 辅助方法
    private Long createTestRoomType() {
        return createRoomTypeWithName("测试房间类型");
    }

    private Long createRoomTypeWithName(String name) {
        com.hotel.entity.RoomType roomType = new com.hotel.entity.RoomType();
        roomType.setHotelId(1L);
        roomType.setName(name);
        roomType.setCapacity(2);
        roomType.setBasePrice(new BigDecimal("299.00"));
        roomType.setFacilities("[\"WiFi\", \"空调\"]");
        roomType.setDescription("测试描述");
        roomType.setIconUrl("https://example.com/test-icon.png");
        roomType.setStatus("ACTIVE");
        roomTypeRepository.insert(roomType);
        return roomType.getId();
    }

    private Long createTestRoomTypeWithHotel(Long hotelId) {
        com.hotel.entity.RoomType roomType = new com.hotel.entity.RoomType();
        roomType.setHotelId(hotelId);
        roomType.setName("酒店" + hotelId + "的房间类型");
        roomType.setCapacity(2);
        roomType.setBasePrice(new BigDecimal("299.00"));
        roomType.setFacilities("[\"WiFi\", \"空调\"]");
        roomType.setDescription("测试描述");
        roomType.setIconUrl("https://example.com/test-icon.png");
        roomType.setStatus("ACTIVE");
        roomTypeRepository.insert(roomType);
        return roomType.getId();
    }

    private void setRoomTypeStatus(Long roomTypeId, String status) {
        com.hotel.entity.RoomType roomType = roomTypeRepository.selectById(roomTypeId);
        if (roomType != null) {
            roomType.setStatus(status);
            roomTypeRepository.updateById(roomType);
        }
    }
}