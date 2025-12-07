package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.dto.facility.*;
import com.hotel.entity.facility.FacilityCategory;
import com.hotel.entity.facility.HotelFacility;
import com.hotel.repository.facility.FacilityCategoryRepository;
import com.hotel.repository.facility.HotelFacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("设施服务测试")
class FacilityServiceTest {

    @Mock
    private FacilityCategoryRepository facilityCategoryRepository;

    @Mock
    private HotelFacilityRepository hotelFacilityRepository;

    @InjectMocks
    private FacilityService facilityService;

    private FacilityCategory testCategory;
    private HotelFacility testFacility;
    private CreateFacilityCategoryRequest createCategoryRequest;
    private CreateFacilityRequest createFacilityRequest;

    @BeforeEach
    void setUp() {
        // 创建测试用的设施分类数据
        testCategory = new FacilityCategory();
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
        testFacility = new HotelFacility();
        testFacility.setId(1L);
        testFacility.setHotelId(1L);
        testFacility.setCategoryId(1L);
        testFacility.setName("迷你吧");
        testFacility.setDescription("客房内提供免费饮品和小吃");
        testFacility.setIcon("facility-icon.png");
        testFacility.setStatus("AVAILABLE");
        testFacility.setIsFeatured(false);
        testFacility.setDisplayOrder(1);
        testFacility.setCreatedAt(LocalDateTime.now());
        testFacility.setUpdatedAt(LocalDateTime.now());

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
    }

    @Test
    @DisplayName("创建设施分类 - 成功")
    void createCategory_Success() {
        // Arrange
        when(facilityCategoryRepository.existsByName(anyLong(), anyString(), isNull()))
                .thenReturn(false);
        when(facilityCategoryRepository.insert(any(FacilityCategory.class)))
                .thenReturn(1);

        // Act
        FacilityCategoryResponse result = facilityService.createCategory(createCategoryRequest);

        // Assert
        assertNotNull(result);
        assertEquals(createCategoryRequest.getName(), result.getName());
        assertEquals(createCategoryRequest.getDescription(), result.getDescription());
        verify(facilityCategoryRepository).insert(any(FacilityCategory.class));
    }

    @Test
    @DisplayName("创建设施分类 - 名称已存在")
    void createCategory_NameExists() {
        // Arrange
        when(facilityCategoryRepository.existsByName(anyLong(), anyString(), isNull()))
                .thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                facilityService.createCategory(createCategoryRequest));
        assertEquals("该分类名称已存在", exception.getMessage());
        verify(facilityCategoryRepository, never()).insert(any(FacilityCategory.class));
    }

    @Test
    @DisplayName("更新设施分类 - 成功")
    void updateCategory_Success() {
        // Arrange
        when(facilityCategoryRepository.selectById(1L))
                .thenReturn(testCategory);
        when(facilityCategoryRepository.existsByName(anyLong(), anyString(), eq(1L)))
                .thenReturn(false);
        when(facilityCategoryRepository.updateById(any(FacilityCategory.class)))
                .thenReturn(1);

        // Act
        FacilityCategoryResponse result = facilityService.updateCategory(1L, createCategoryRequest);

        // Assert
        assertNotNull(result);
        assertEquals(createCategoryRequest.getName(), result.getName());
        verify(facilityCategoryRepository).updateById(any(FacilityCategory.class));
    }

    @Test
    @DisplayName("更新设施分类 - 分类不存在")
    void updateCategory_NotFound() {
        // Arrange
        when(facilityCategoryRepository.selectById(1L))
                .thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                facilityService.updateCategory(1L, createCategoryRequest));
        assertEquals("设施分类不存在", exception.getMessage());
        verify(facilityCategoryRepository, never()).updateById(any(FacilityCategory.class));
    }

    @Test
    @DisplayName("删除设施分类 - 成功")
    void deleteCategory_Success() {
        // Arrange
        when(facilityCategoryRepository.selectById(1L))
                .thenReturn(testCategory);
        when(hotelFacilityRepository.selectByCategoryId(1L))
                .thenReturn(Arrays.asList());

        // Act
        facilityService.deleteCategory(1L);

        // Assert
        verify(facilityCategoryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("删除设施分类 - 分类下有设施")
    void deleteCategory_HasFacilities() {
        // Arrange
        when(facilityCategoryRepository.selectById(1L))
                .thenReturn(testCategory);
        when(hotelFacilityRepository.selectByCategoryId(1L))
                .thenReturn(Arrays.asList(testFacility));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                facilityService.deleteCategory(1L));
        assertEquals("该分类下还有设施，无法删除", exception.getMessage());
        verify(facilityCategoryRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("获取酒店设施分类列表")
    void getCategoriesByHotel() {
        // Arrange
        List<FacilityCategory> categories = Arrays.asList(testCategory);
        when(facilityCategoryRepository.selectByHotelId(1L))
                .thenReturn(categories);

        // Act
        List<FacilityCategoryResponse> result = facilityService.getCategoriesByHotel(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCategory.getName(), result.get(0).getName());
    }

    @Test
    @DisplayName("创建设施 - 成功")
    void createFacility_Success() {
        // Arrange
        when(facilityCategoryRepository.selectById(1L))
                .thenReturn(testCategory);
        when(hotelFacilityRepository.existsByName(anyLong(), anyLong(), anyString(), isNull()))
                .thenReturn(false);
        when(hotelFacilityRepository.insert(any(HotelFacility.class)))
                .thenReturn(1);

        // Act
        FacilityResponse result = facilityService.createFacility(createFacilityRequest);

        // Assert
        assertNotNull(result);
        assertEquals(createFacilityRequest.getName(), result.getName());
        assertEquals(createFacilityRequest.getDescription(), result.getDescription());
        verify(hotelFacilityRepository).insert(any(HotelFacility.class));
    }

    @Test
    @DisplayName("创建设施 - 分类不存在")
    void createFacility_CategoryNotExists() {
        // Arrange
        when(facilityCategoryRepository.selectById(1L))
                .thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                facilityService.createFacility(createFacilityRequest));
        assertEquals("设施分类不存在或已禁用", exception.getMessage());
        verify(hotelFacilityRepository, never()).insert(any(HotelFacility.class));
    }

    @Test
    @DisplayName("更新设施状态 - 成功")
    void updateFacilityStatus_Success() {
        // Arrange
        when(hotelFacilityRepository.selectById(1L))
                .thenReturn(testFacility);
        when(hotelFacilityRepository.updateById(any(HotelFacility.class)))
                .thenReturn(1);

        // Act
        facilityService.updateFacilityStatus(1L, "MAINTENANCE");

        // Assert
        verify(hotelFacilityRepository).updateById(any(HotelFacility.class));
    }

    @Test
    @DisplayName("更新设施状态 - 设施不存在")
    void updateFacilityStatus_NotFound() {
        // Arrange
        when(hotelFacilityRepository.selectById(1L))
                .thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                facilityService.updateFacilityStatus(1L, "MAINTENANCE"));
        assertEquals("设施不存在", exception.getMessage());
        verify(hotelFacilityRepository, never()).updateById(any(HotelFacility.class));
    }

    @Test
    @DisplayName("获取酒店设施列表")
    void getFacilitiesByHotel() {
        // Arrange
        List<HotelFacility> facilities = Arrays.asList(testFacility);
        when(hotelFacilityRepository.selectByHotelId(1L))
                .thenReturn(facilities);
        when(facilityCategoryRepository.selectBatchIds(anyList()))
                .thenReturn(Arrays.asList(testCategory));

        // Act
        List<FacilityResponse> result = facilityService.getFacilitiesByHotel(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testFacility.getName(), result.get(0).getName());
    }

    @Test
    @DisplayName("获取特色设施")
    void getFeaturedFacilities() {
        // Arrange
        testFacility.setIsFeatured(true);
        List<HotelFacility> facilities = Arrays.asList(testFacility);
        when(hotelFacilityRepository.selectFeaturedByHotelId(1L))
                .thenReturn(facilities);
        when(facilityCategoryRepository.selectBatchIds(anyList()))
                .thenReturn(Arrays.asList(testCategory));

        // Act
        List<FacilityResponse> result = facilityService.getFeaturedFacilities(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsFeatured());
    }
}