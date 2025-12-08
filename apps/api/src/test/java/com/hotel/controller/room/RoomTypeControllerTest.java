package com.hotel.controller.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.roomtype.*;
import com.hotel.enums.RoomTypeStatus;
import com.hotel.service.RoomTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomTypeController.class)
@DisplayName("房间类型控制器测试")
class RoomTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomTypeService roomTypeService;

    @Autowired
    private ObjectMapper objectMapper;

    private RoomTypeResponse mockRoomTypeResponse;
    private RoomTypeListResponse mockRoomTypeListResponse;
    private CreateRoomTypeRequest createRequest;
    private UpdateRoomTypeRequest updateRequest;

    @BeforeEach
    void setUp() {
        mockRoomTypeResponse = new RoomTypeResponse();
        mockRoomTypeResponse.setId(1L);
        mockRoomTypeResponse.setHotelId(1L);
        mockRoomTypeResponse.setHotelName("测试酒店");
        mockRoomTypeResponse.setName("标准间");
        mockRoomTypeResponse.setCapacity(2);
        mockRoomTypeResponse.setBasePrice(new BigDecimal("299.00"));
        mockRoomTypeResponse.setFacilities(Arrays.asList("WiFi", "空调", "电视"));
        mockRoomTypeResponse.setDescription("舒适的标准间");
        mockRoomTypeResponse.setIconUrl("https://example.com/icon.png");
        mockRoomTypeResponse.setStatus("ACTIVE");
        mockRoomTypeResponse.setCreatedAt(LocalDateTime.now());
        mockRoomTypeResponse.setUpdatedAt(LocalDateTime.now());

        mockRoomTypeListResponse = new RoomTypeListResponse();
        mockRoomTypeListResponse.setContent(Arrays.asList(mockRoomTypeResponse));
        mockRoomTypeListResponse.setTotalElements(1);
        mockRoomTypeListResponse.setTotalPages(1);
        mockRoomTypeListResponse.setSize(20);
        mockRoomTypeListResponse.setNumber(0);
        mockRoomTypeListResponse.setFirst(true);
        mockRoomTypeListResponse.setLast(true);
        mockRoomTypeListResponse.setHasNext(false);
        mockRoomTypeListResponse.setHasPrevious(false);

        createRequest = new CreateRoomTypeRequest();
        createRequest.setHotelId(1L);
        createRequest.setName("豪华套房");
        createRequest.setCapacity(4);
        createRequest.setBasePrice(new BigDecimal("888.00"));
        createRequest.setFacilities(Arrays.asList("WiFi", "空调", "电视", "迷你吧"));
        createRequest.setDescription("豪华的套房");
        createRequest.setIconUrl("https://example.com/luxury-icon.png");

        updateRequest = new UpdateRoomTypeRequest();
        updateRequest.setName("商务间");
        updateRequest.setCapacity(2);
        updateRequest.setBasePrice(new BigDecimal("399.00"));
        updateRequest.setFacilities(Arrays.asList("WiFi", "空调", "办公桌"));
        updateRequest.setDescription("适合商务人士的房间");
        updateRequest.setIconUrl("https://example.com/business-icon.png");
        updateRequest.setStatus(RoomTypeStatus.ACTIVE);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("创建房间类型 - 成功")
    void createRoomType_Success() throws Exception {
        when(roomTypeService.createRoomType(any(CreateRoomTypeRequest.class)))
                .thenReturn(mockRoomTypeResponse);

        mockMvc.perform(post("/api/room-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("标准间"))
                .andExpect(jsonPath("$.message").value("房间类型创建成功"));

        verify(roomTypeService, times(1)).createRoomType(any(CreateRoomTypeRequest.class));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("创建房间类型 - 权限不足")
    void createRoomType_Forbidden() throws Exception {
        mockMvc.perform(post("/api/room-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        verify(roomTypeService, never()).createRoomType(any());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("创建房间类型 - 验证失败")
    void createRoomType_ValidationError() throws Exception {
        CreateRoomTypeRequest invalidRequest = new CreateRoomTypeRequest();
        // 设置无效数据 - 名称为空
        invalidRequest.setHotelId(1L);
        invalidRequest.setName("");
        invalidRequest.setCapacity(2);
        invalidRequest.setBasePrice(new BigDecimal("299.00"));

        mockMvc.perform(post("/api/room-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(roomTypeService, never()).createRoomType(any());
    }

    @Test
    @DisplayName("获取房间类型列表 - 成功")
    void getRoomTypes_Success() throws Exception {
        when(roomTypeService.getRoomTypesWithPage(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(mockRoomTypeListResponse);

        mockMvc.perform(get("/api/room-types")
                        .param("page", "0")
                        .param("size", "20")
                        .param("search", "标准")
                        .param("hotelId", "1")
                        .param("status", "ACTIVE")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(roomTypeService, times(1)).getRoomTypesWithPage(
                eq(0), eq(20), eq("标准"), eq(1L), eq("ACTIVE"), eq("createdAt"), eq("DESC"));
    }

    @Test
    @DisplayName("获取房间类型列表 - 默认参数")
    void getRoomTypes_DefaultParams() throws Exception {
        when(roomTypeService.getRoomTypesWithPage(anyInt(), anyInt(), isNull(), isNull(), isNull(), anyString(), anyString()))
                .thenReturn(mockRoomTypeListResponse);

        mockMvc.perform(get("/api/room-types"))
                .andExpect(status().isOk());

        verify(roomTypeService, times(1)).getRoomTypesWithPage(
                eq(0), eq(20), isNull(), isNull(), isNull(), eq("createdAt"), eq("DESC"));
    }

    @Test
    @DisplayName("根据酒店ID获取房间类型 - 成功")
    void getRoomTypesByHotel_Success() throws Exception {
        List<RoomTypeResponse> roomTypes = Arrays.asList(mockRoomTypeResponse);
        when(roomTypeService.getRoomTypesByHotelId(1L))
                .thenReturn(roomTypes);

        mockMvc.perform(get("/api/room-types/hotel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(roomTypeService, times(1)).getRoomTypesByHotelId(1L);
    }

    @Test
    @DisplayName("获取房间类型详情 - 成功")
    void getRoomType_Success() throws Exception {
        when(roomTypeService.getRoomTypeById(1L))
                .thenReturn(mockRoomTypeResponse);

        mockMvc.perform(get("/api/room-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("标准间"));

        verify(roomTypeService, times(1)).getRoomTypeById(1L);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("更新房间类型 - 成功")
    void updateRoomType_Success() throws Exception {
        when(roomTypeService.updateRoomType(eq(1L), any(UpdateRoomTypeRequest.class)))
                .thenReturn(mockRoomTypeResponse);

        mockMvc.perform(put("/api/room-types/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.message").value("房间类型更新成功"));

        verify(roomTypeService, times(1)).updateRoomType(eq(1L), any(UpdateRoomTypeRequest.class));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("更新房间类型 - 权限不足")
    void updateRoomType_Forbidden() throws Exception {
        mockMvc.perform(put("/api/room-types/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        verify(roomTypeService, never()).updateRoomType(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("删除房间类型 - 成功")
    void deleteRoomType_Success() throws Exception {
        doNothing().when(roomTypeService).deleteRoomType(1L);

        mockMvc.perform(delete("/api/room-types/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("房间类型删除成功"));

        verify(roomTypeService, times(1)).deleteRoomType(1L);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("删除房间类型 - 权限不足")
    void deleteRoomType_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/room-types/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(roomTypeService, never()).deleteRoomType(anyLong());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("更新房间类型状态 - 成功")
    void updateRoomTypeStatus_Success() throws Exception {
        when(roomTypeService.updateRoomTypeStatus(1L, RoomTypeStatus.INACTIVE))
                .thenReturn(mockRoomTypeResponse);

        mockMvc.perform(put("/api/room-types/1/status")
                        .with(csrf())
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("房间类型状态更新成功"));

        verify(roomTypeService, times(1)).updateRoomTypeStatus(1L, RoomTypeStatus.INACTIVE);
    }

    @Test
    @DisplayName("获取活跃房间类型 - 成功")
    void getActiveRoomTypes_Success() throws Exception {
        List<RoomTypeResponse> activeRoomTypes = Arrays.asList(mockRoomTypeResponse);
        when(roomTypeService.getRoomTypesByStatus("ACTIVE"))
                .thenReturn(activeRoomTypes);

        mockMvc.perform(get("/api/room-types/active"))
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

        verify(roomTypeService, times(1)).getRoomTypesByStatus("ACTIVE");
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("获取房间类型关联的房间 - 成功")
    void getRoomsByRoomType_Success() throws Exception {
        mockMvc.perform(get("/api/room-types/1/rooms")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.message").value("房间关联功能待实现"));
    }

    @Test
    @DisplayName("获取房间类型列表 - 分页参数验证")
    void getRoomTypes_InvalidPageParams() throws Exception {
        mockMvc.perform(get("/api/room-types")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("获取房间类型列表 - 分页参数边界值")
    void getRoomTypes_PageParamBoundaries() throws Exception {
        when(roomTypeService.getRoomTypesWithPage(anyInt(), anyInt(), isNull(), isNull(), isNull(), anyString(), anyString()))
                .thenReturn(mockRoomTypeListResponse);

        // 测试最小有效值
        mockMvc.perform(get("/api/room-types")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk());

        // 测试最大有效值
        mockMvc.perform(get("/api/room-types")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk());
    }
}