package com.hotel.controller.facility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.facility.*;
import com.hotel.service.FacilityService;
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

@WebMvcTest(FacilityController.class)
@DisplayName("设施控制器测试")
class FacilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FacilityService facilityService;

    @Autowired
    private ObjectMapper objectMapper;

    private FacilityCategoryResponse testCategory;
    private FacilityResponse testFacility;
    private CreateFacilityCategoryRequest createCategoryRequest;
    private CreateFacilityRequest createFacilityRequest;
    private FacilitySearchRequest searchRequest;

    @BeforeEach
    void setUp() {
        // 创建测试用的设施分类数据
        testCategory = new FacilityCategoryResponse();
        testCategory.setId(1L);
        testCategory.setHotelId(1L);
        testCategory.setName("客房设施");
        testCategory.setDescription("客房内的设施和服务");
        testCategory.setIcon("category-icon.png");
        testCategory.setDisplayOrder(1);
        testCategory.setIsActive(true);
        testCategory.setCreatedAt(LocalDateTime.now());
        testCategory.setUpdatedAt(LocalDateTime.now());

        // 创建测试用的设施数据
        testFacility = new FacilityResponse();
        testFacility.setId(1L);
        testFacility.setHotelId(1L);
        testFacility.setCategoryId(1L);
        testFacility.setName("迷你吧");
        testFacility.setDescription("客房内提供免费饮品和小吃");
        testFacility.setIcon("facility-icon.png");
        testFacility.setStatus("AVAILABLE");
        testFacility.setStatusDescription("正常可用");
        testFacility.setIsFeatured(false);
        testFacility.setDisplayOrder(1);
        testFacility.setCreatedAt(LocalDateTime.now());
        testFacility.setUpdatedAt(LocalDateTime.now());
        testFacility.setCategory(testCategory);

        // 创建设施分类请求对象
        createCategoryRequest = new CreateFacilityCategoryRequest();
        createCategoryRequest.setHotelId(1L);
        createCategoryRequest.setName("客房设施");
        createCategoryRequest.setDescription("客房内的设施和服务");
        createCategoryRequest.setIcon("category-icon.png");
        createCategoryRequest.setDisplayOrder(1);
        createCategoryRequest.setIsActive(true);

        // 创建设施请求对象
        createFacilityRequest = new CreateFacilityRequest();
        createFacilityRequest.setHotelId(1L);
        createFacilityRequest.setCategoryId(1L);
        createFacilityRequest.setName("迷你吧");
        createFacilityRequest.setDescription("客房内提供免费饮品和小吃");
        createFacilityRequest.setIcon("facility-icon.png");
        createFacilityRequest.setStatus("AVAILABLE");
        createFacilityRequest.setIsFeatured(false);
        createFacilityRequest.setDisplayOrder(1);

        // 创建搜索请求对象
        searchRequest = new FacilitySearchRequest();
        searchRequest.setHotelId(1L);
        searchRequest.setPage(1);
        searchRequest.setSize(20);
        searchRequest.setSortBy("displayOrder");
        searchRequest.setSortDirection("ASC");
    }

    @Test
    @WithMockUser(roles = {"HOTEL_MANAGER"})
    @DisplayName("创建设施分类 - 成功")
    void createCategory_Success() throws Exception {
        when(facilityService.createCategory(any(CreateFacilityCategoryRequest.class)))
                .thenReturn(testCategory);

        mockMvc.perform(post("/api/hotels/1/facilities/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCategoryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("设施分类创建成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("客房设施"));
    }

    @Test
    @WithMockUser(roles = {"HOTEL_MANAGER"})
    @DisplayName("更新设施分类 - 成功")
    void updateCategory_Success() throws Exception {
        when(facilityService.updateCategory(eq(1L), any(CreateFacilityCategoryRequest.class)))
                .thenReturn(testCategory);

        mockMvc.perform(put("/api/hotels/1/facilities/categories/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCategoryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("设施分类更新成功"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(roles = {"HOTEL_MANAGER"})
    @DisplayName("删除设施分类 - 成功")
    void deleteCategory_Success() throws Exception {
        mockMvc.perform(delete("/api/hotels/1/facilities/categories/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("设施分类删除成功"));
    }

    @Test
    @DisplayName("获取酒店设施分类列表 - 成功")
    void getCategories_Success() throws Exception {
        List<FacilityCategoryResponse> categories = Arrays.asList(testCategory);
        when(facilityService.getActiveCategoriesByHotel(1L)).thenReturn(categories);

        mockMvc.perform(get("/api/hotels/1/facilities/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("客房设施"));
    }

    @Test
    @WithMockUser(roles = {"HOTEL_MANAGER"})
    @DisplayName("创建设施 - 成功")
    void createFacility_Success() throws Exception {
        when(facilityService.createFacility(any(CreateFacilityRequest.class)))
                .thenReturn(testFacility);

        mockMvc.perform(post("/api/hotels/1/facilities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createFacilityRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("设施创建成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("迷你吧"));
    }

    @Test
    @WithMockUser(roles = {"HOTEL_MANAGER"})
    @DisplayName("更新设施 - 成功")
    void updateFacility_Success() throws Exception {
        when(facilityService.updateFacility(eq(1L), any(CreateFacilityRequest.class)))
                .thenReturn(testFacility);

        mockMvc.perform(put("/api/hotels/1/facilities/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createFacilityRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("设施更新成功"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(roles = {"HOTEL_MANAGER"})
    @DisplayName("删除设施 - 成功")
    void deleteFacility_Success() throws Exception {
        mockMvc.perform(delete("/api/hotels/1/facilities/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("设施删除成功"));
    }

    @Test
    @WithMockUser(roles = {"HOTEL_MANAGER"})
    @DisplayName("更新设施状态 - 成功")
    void updateFacilityStatus_Success() throws Exception {
        mockMvc.perform(patch("/api/hotels/1/facilities/1/status")
                        .with(csrf())
                        .param("status", "MAINTENANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("设施状态更新成功"));
    }

    @Test
    @DisplayName("搜索设施 - 成功")
    void searchFacilities_Success() throws Exception {
        List<FacilityResponse> facilities = Arrays.asList(testFacility);
        Page<FacilityResponse> facilityPage = new PageImpl<>(facilities);

        when(facilityService.searchFacilities(any(FacilitySearchRequest.class)))
                .thenReturn(facilityPage);

        mockMvc.perform(get("/api/hotels/1/facilities/search")
                        .param("categoryId", "1")
                        .param("status", "AVAILABLE")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    @DisplayName("获取酒店设施列表 - 成功")
    void getFacilities_Success() throws Exception {
        List<FacilityResponse> facilities = Arrays.asList(testFacility);
        when(facilityService.getFacilitiesByHotel(1L)).thenReturn(facilities);

        mockMvc.perform(get("/api/hotels/1/facilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("迷你吧"));
    }

    @Test
    @DisplayName("获取特色设施 - 成功")
    void getFeaturedFacilities_Success() throws Exception {
        List<FacilityResponse> facilities = Arrays.asList(testFacility);
        when(facilityService.getFeaturedFacilities(1L)).thenReturn(facilities);

        mockMvc.perform(get("/api/hotels/1/facilities/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("获取分类下的设施 - 成功")
    void getFacilitiesByCategory_Success() throws Exception {
        List<FacilityResponse> facilities = Arrays.asList(testFacility);
        when(facilityService.getFacilitiesByCategory(1L, 1L)).thenReturn(facilities);

        mockMvc.perform(get("/api/hotels/1/facilities/categories/1/facilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }
}