package com.hotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.hotel.*;
import com.hotel.entity.Hotel;
import com.hotel.enums.HotelStatus;
import com.hotel.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HotelController.class)
@DisplayName("酒店控制器测试")
class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HotelService hotelService;

    @Autowired
    private ObjectMapper objectMapper;

    private Hotel testHotel;
    private HotelResponse testHotelResponse;
    private CreateHotelRequest createHotelRequest;
    private UpdateHotelRequest updateHotelRequest;
    private UpdateHotelStatusRequest statusRequest;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("测试酒店");
        testHotel.setAddress("测试地址");
        testHotel.setPhone("13800138000");
        testHotel.setDescription("测试描述");
        testHotel.setFacilities("[\"WiFi\", \"停车场\"]");
        testHotel.setImages("[\"image1.jpg\", \"image2.jpg\"]");
        testHotel.setStatus(HotelStatus.ACTIVE.name());
        testHotel.setCreatedBy(1L);
        testHotel.setCreatedAt(LocalDateTime.now());
        testHotel.setUpdatedAt(LocalDateTime.now());

        testHotelResponse = new HotelResponse();
        testHotelResponse.setId(1L);
        testHotelResponse.setName("测试酒店");
        testHotelResponse.setAddress("测试地址");
        testHotelResponse.setPhone("13800138000");
        testHotelResponse.setDescription("测试描述");
        testHotelResponse.setFacilities(Arrays.asList("WiFi", "停车场"));
        testHotelResponse.setImages(Arrays.asList("image1.jpg", "image2.jpg"));
        testHotelResponse.setStatus(HotelStatus.ACTIVE);
        testHotelResponse.setCreatedBy(1L);
        testHotelResponse.setCreatedAt(testHotel.getCreatedAt().toString());
        testHotelResponse.setUpdatedAt(testHotel.getUpdatedAt().toString());

        createHotelRequest = new CreateHotelRequest();
        createHotelRequest.setName("新酒店");
        createHotelRequest.setAddress("新地址");
        createHotelRequest.setPhone("13900139000");
        createHotelRequest.setDescription("新描述");
        createHotelRequest.setFacilities(Arrays.asList("WiFi", "游泳池"));
        createHotelRequest.setImages(Arrays.asList("image1.jpg"));

        updateHotelRequest = new UpdateHotelRequest();
        updateHotelRequest.setName("更新酒店");
        updateHotelRequest.setAddress("更新地址");
        updateHotelRequest.setDescription("更新描述");

        statusRequest = new UpdateHotelStatusRequest();
        statusRequest.setStatus(HotelStatus.INACTIVE.name());
    }

    @Test
    @DisplayName("获取酒店列表 - 成功")
    void getHotels_Success() throws Exception {
        // Given
        List<HotelResponse> hotelList = Arrays.asList(testHotelResponse);
        HotelListResponse listResponse = new HotelListResponse();
        listResponse.setContent(hotelList);
        listResponse.setTotalElements(1);
        listResponse.setTotalPages(1);
        listResponse.setSize(20);
        listResponse.setNumber(0);

        when(hotelService.getHotels(anyInt(), anyInt(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(listResponse);

        // When & Then
        mockMvc.perform(get("/api/hotels")
                        .param("page", "0")
                        .param("size", "20")
                        .param("search", "测试")
                        .param("status", "ACTIVE")
                        .param("sortBy", "name")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取酒店列表成功"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("测试酒店"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("获取酒店详情 - 成功")
    void getHotelById_Success() throws Exception {
        // Given
        when(hotelService.getHotelById(1L)).thenReturn(testHotelResponse);

        // When & Then
        mockMvc.perform(get("/api/hotels/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取酒店详情成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("测试酒店"))
                .andExpect(jsonPath("$.data.address").value("测试地址"));
    }

    @Test
    @DisplayName("创建酒店 - 成功")
    @WithMockUser(roles = "ADMIN")
    void createHotel_Success() throws Exception {
        // Given
        HotelResponse createdHotel = new HotelResponse();
        createdHotel.setId(2L);
        createdHotel.setName("新酒店");
        createdHotel.setAddress("新地址");
        createdHotel.setPhone("13900139000");
        createdHotel.setDescription("新描述");
        createdHotel.setFacilities(Arrays.asList("WiFi", "游泳池"));
        createdHotel.setImages(Arrays.asList("image1.jpg"));
        createdHotel.setStatus(HotelStatus.ACTIVE);
        createdHotel.setCreatedBy(1L);
        createdHotel.setCreatedAt(LocalDateTime.now().toString());
        createdHotel.setUpdatedAt(LocalDateTime.now().toString());

        when(hotelService.createHotel(any(CreateHotelRequest.class), anyLong())).thenReturn(createdHotel);

        // When & Then
        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createHotelRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("酒店创建成功"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.name").value("新酒店"))
                .andExpect(jsonPath("$.data.address").value("新地址"));
    }

    @Test
    @DisplayName("创建酒店 - 无管理员权限")
    @WithMockUser(roles = "USER")
    void createHotel_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createHotelRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("创建酒店 - 未认证")
    void createHotel_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createHotelRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("更新酒店 - 成功")
    @WithMockUser(roles = "ADMIN")
    void updateHotel_Success() throws Exception {
        // Given
        HotelResponse updatedHotel = new HotelResponse();
        updatedHotel.setId(1L);
        updatedHotel.setName("更新酒店");
        updatedHotel.setAddress("更新地址");
        updatedHotel.setPhone("13800138000");
        updatedHotel.setDescription("更新描述");
        updatedHotel.setFacilities(Arrays.asList("WiFi", "停车场"));
        updatedHotel.setImages(Arrays.asList("image1.jpg", "image2.jpg"));
        updatedHotel.setStatus(HotelStatus.ACTIVE);
        updatedHotel.setCreatedBy(1L);
        updatedHotel.setCreatedAt(LocalDateTime.now().toString());
        updatedHotel.setUpdatedAt(LocalDateTime.now().toString());

        when(hotelService.updateHotel(eq(1L), any(UpdateHotelRequest.class))).thenReturn(updatedHotel);

        // When & Then
        mockMvc.perform(put("/api/hotels/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateHotelRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("酒店更新成功"))
                .andExpect(jsonPath("$.data.name").value("更新酒店"))
                .andExpect(jsonPath("$.data.address").value("更新地址"));
    }

    @Test
    @DisplayName("更新酒店 - 无管理员权限")
    @WithMockUser(roles = "USER")
    void updateHotel_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/hotels/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateHotelRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("删除酒店 - 成功")
    @WithMockUser(roles = "ADMIN")
    void deleteHotel_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/hotels/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("酒店删除成功"));
    }

    @Test
    @DisplayName("删除酒店 - 无管理员权限")
    @WithMockUser(roles = "USER")
    void deleteHotel_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/hotels/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("更新酒店状态 - 成功")
    @WithMockUser(roles = "ADMIN")
    void updateHotelStatus_Success() throws Exception {
        // Given
        HotelResponse updatedHotel = new HotelResponse();
        updatedHotel.setId(1L);
        updatedHotel.setName("测试酒店");
        updatedHotel.setAddress("测试地址");
        updatedHotel.setStatus(HotelStatus.INACTIVE);
        updatedHotel.setCreatedBy(1L);

        when(hotelService.updateHotelStatus(eq(1L), any(UpdateHotelStatusRequest.class))).thenReturn(updatedHotel);

        // When & Then
        mockMvc.perform(put("/api/hotels/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("酒店状态更新成功"));
    }

    @Test
    @DisplayName("获取酒店列表 - 无需认证")
    void getHotels_NoAuthRequired() throws Exception {
        // Given
        List<HotelResponse> hotelList = Arrays.asList(testHotelResponse);
        HotelListResponse listResponse = new HotelListResponse();
        listResponse.setContent(hotelList);
        listResponse.setTotalElements(1);
        listResponse.setTotalPages(1);
        listResponse.setSize(20);
        listResponse.setNumber(0);

        when(hotelService.getHotels(anyInt(), anyInt(), isNull(), isNull(), anyString(), anyString()))
                .thenReturn(listResponse);

        // When & Then
        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("获取酒店详情 - 无需认证")
    void getHotelById_NoAuthRequired() throws Exception {
        // Given
        when(hotelService.getHotelById(1L)).thenReturn(testHotelResponse);

        // When & Then
        mockMvc.perform(get("/api/hotels/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("创建酒店 - 参数验证失败")
    @WithMockUser(roles = "ADMIN")
    void createHotel_ValidationError() throws Exception {
        // Given
        CreateHotelRequest invalidRequest = new CreateHotelRequest();
        invalidRequest.setName(""); // 空名称
        invalidRequest.setAddress(""); // 空地址

        // When & Then
        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("获取酒店详情 - 不存在的ID")
    void getHotelById_NotFound() throws Exception {
        // Given
        when(hotelService.getHotelById(999L)).thenThrow(new RuntimeException("酒店不存在"));

        // When & Then
        mockMvc.perform(get("/api/hotels/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("更新酒店状态 - 无效状态值")
    @WithMockUser(roles = "ADMIN")
    void updateHotelStatus_InvalidStatus() throws Exception {
        // Given
        UpdateHotelStatusRequest invalidRequest = new UpdateHotelStatusRequest();
        invalidRequest.setStatus("INVALID_STATUS");

        // When & Then
        mockMvc.perform(put("/api/hotels/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}