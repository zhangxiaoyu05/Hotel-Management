package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.dto.facility.*;
import com.hotel.entity.facility.FacilityCategory;
import com.hotel.entity.facility.HotelFacility;
import com.hotel.exception.FacilityException;
import com.hotel.repository.facility.FacilityCategoryRepository;
import com.hotel.repository.facility.HotelFacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityCategoryRepository facilityCategoryRepository;
    private final HotelFacilityRepository hotelFacilityRepository;

    // ========== 设施分类管理 ==========

    @Transactional
    public FacilityCategoryResponse createCategory(CreateFacilityCategoryRequest request) {
        // 检查分类名称是否已存在
        if (facilityCategoryRepository.existsByName(request.getHotelId(), request.getName(), null)) {
            throw new FacilityException.CategoryNameExistsException("该分类名称已存在");
        }

        FacilityCategory category = new FacilityCategory();
        category.setHotelId(request.getHotelId());
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setIsActive(request.getIsActive());

        facilityCategoryRepository.insert(category);

        return convertToCategoryResponse(category);
    }

    @Transactional
    public FacilityCategoryResponse updateCategory(Long categoryId, CreateFacilityCategoryRequest request) {
        FacilityCategory category = facilityCategoryRepository.selectById(categoryId);
        if (category == null) {
            throw new FacilityException.CategoryNotFoundException("设施分类不存在");
        }

        // 检查分类名称是否已存在（排除当前分类）
        if (facilityCategoryRepository.existsByName(request.getHotelId(), request.getName(), categoryId)) {
            throw new FacilityException.CategoryNameExistsException("该分类名称已存在");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setIsActive(request.getIsActive());

        facilityCategoryRepository.updateById(category);

        return convertToCategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        FacilityCategory category = facilityCategoryRepository.selectById(categoryId);
        if (category == null) {
            throw new FacilityException.CategoryNotFoundException("设施分类不存在");
        }

        // 检查是否有设施使用该分类
        List<HotelFacility> facilities = hotelFacilityRepository.selectByCategoryId(categoryId);
        if (!facilities.isEmpty()) {
            throw new FacilityException.FacilityOperationException("该分类下还有设施，无法删除");
        }

        facilityCategoryRepository.deleteById(categoryId);
    }

    public List<FacilityCategoryResponse> getCategoriesByHotel(Long hotelId) {
        List<FacilityCategory> categories = facilityCategoryRepository.selectByHotelId(hotelId);
        return categories.stream()
                .map(this::convertToCategoryResponse)
                .collect(Collectors.toList());
    }

    public List<FacilityCategoryResponse> getActiveCategoriesByHotel(Long hotelId) {
        List<FacilityCategory> categories = facilityCategoryRepository.selectActiveByHotelId(hotelId);
        return categories.stream()
                .map(this::convertToCategoryResponse)
                .collect(Collectors.toList());
    }

    // ========== 设施管理 ==========

    @Transactional
    public FacilityResponse createFacility(CreateFacilityRequest request) {
        // 检查分类是否存在
        FacilityCategory category = facilityCategoryRepository.selectById(request.getCategoryId());
        if (category == null || !category.getIsActive()) {
            throw new FacilityException.CategoryNotFoundException("设施分类不存在或已禁用");
        }

        // 检查设施名称是否已存在
        if (hotelFacilityRepository.existsByName(request.getHotelId(), request.getCategoryId(), request.getName(), null)) {
            throw new FacilityException.FacilityNameExistsException("该设施名称在当前分类下已存在");
        }

        HotelFacility facility = new HotelFacility();
        facility.setHotelId(request.getHotelId());
        facility.setCategoryId(request.getCategoryId());
        facility.setName(request.getName());
        facility.setDescription(request.getDescription());
        facility.setIcon(request.getIcon());
        facility.setStatus(request.getStatus());
        facility.setIsFeatured(request.getIsFeatured());
        facility.setDisplayOrder(request.getDisplayOrder());

        hotelFacilityRepository.insert(facility);

        return convertToFacilityResponse(facility, category);
    }

    @Transactional
    public FacilityResponse updateFacility(Long facilityId, CreateFacilityRequest request) {
        HotelFacility facility = hotelFacilityRepository.selectById(facilityId);
        if (facility == null) {
            throw new FacilityException.FacilityNotFoundException("设施不存在");
        }

        // 检查分类是否存在
        FacilityCategory category = facilityCategoryRepository.selectById(request.getCategoryId());
        if (category == null || !category.getIsActive()) {
            throw new FacilityException.CategoryNotFoundException("设施分类不存在或已禁用");
        }

        // 检查设施名称是否已存在（排除当前设施）
        if (hotelFacilityRepository.existsByName(request.getHotelId(), request.getCategoryId(), request.getName(), facilityId)) {
            throw new FacilityException.FacilityNameExistsException("该设施名称在当前分类下已存在");
        }

        facility.setCategoryId(request.getCategoryId());
        facility.setName(request.getName());
        facility.setDescription(request.getDescription());
        facility.setIcon(request.getIcon());
        facility.setStatus(request.getStatus());
        facility.setIsFeatured(request.getIsFeatured());
        facility.setDisplayOrder(request.getDisplayOrder());

        hotelFacilityRepository.updateById(facility);

        return convertToFacilityResponse(facility, category);
    }

    @Transactional
    public void deleteFacility(Long facilityId) {
        HotelFacility facility = hotelFacilityRepository.selectById(facilityId);
        if (facility == null) {
            throw new FacilityException.FacilityNotFoundException("设施不存在");
        }

        hotelFacilityRepository.deleteById(facilityId);
    }

    @Transactional
    public void updateFacilityStatus(Long facilityId, String status) {
        HotelFacility facility = hotelFacilityRepository.selectById(facilityId);
        if (facility == null) {
            throw new FacilityException.FacilityNotFoundException("设施不存在");
        }

        // 验证状态值
        try {
            HotelFacility.FacilityStatus.fromCode(status);
        } catch (Exception e) {
            throw new FacilityException.InvalidFacilityStatusException("无效的设施状态");
        }

        facility.setStatus(status);
        hotelFacilityRepository.updateById(facility);
    }

    public IPage<FacilityResponse> searchFacilities(FacilitySearchRequest request) {
        Page<HotelFacility> page = new Page<>(request.getPage(), request.getSize());

        IPage<HotelFacility> facilityPage = hotelFacilityRepository.selectFacilitiesWithPage(
                page,
                request.getHotelId(),
                request.getCategoryId(),
                request.getStatus(),
                request.getIsFeatured(),
                request.getKeyword(),
                request.getSortBy(),
                request.getSortDirection()
        );

        // 获取所有分类信息
        List<Long> categoryIds = facilityPage.getRecords().stream()
                .map(HotelFacility::getCategoryId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, FacilityCategory> categoryMap = facilityCategoryRepository.selectBatchIds(categoryIds)
                .stream()
                .collect(Collectors.toMap(FacilityCategory::getId, category -> category));

        return facilityPage.convert(facility -> {
            FacilityCategory category = categoryMap.get(facility.getCategoryId());
            return convertToFacilityResponse(facility, category);
        });
    }

    public List<FacilityResponse> getFacilitiesByHotel(Long hotelId) {
        List<HotelFacility> facilities = hotelFacilityRepository.selectByHotelId(hotelId);

        Map<Long, FacilityCategory> categoryMap = facilityCategoryRepository.selectBatchIds(
                facilities.stream().map(HotelFacility::getCategoryId).distinct().collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(FacilityCategory::getId, category -> category));

        return facilities.stream()
                .map(facility -> convertToFacilityResponse(facility, categoryMap.get(facility.getCategoryId())))
                .collect(Collectors.toList());
    }

    public List<FacilityResponse> getFacilitiesByCategory(Long hotelId, Long categoryId) {
        List<HotelFacility> facilities = hotelFacilityRepository.selectByHotelIdAndCategoryId(hotelId, categoryId);

        FacilityCategory category = facilityCategoryRepository.selectById(categoryId);

        return facilities.stream()
                .map(facility -> convertToFacilityResponse(facility, category))
                .collect(Collectors.toList());
    }

    public List<FacilityResponse> getFeaturedFacilities(Long hotelId) {
        List<HotelFacility> facilities = hotelFacilityRepository.selectFeaturedByHotelId(hotelId);

        Map<Long, FacilityCategory> categoryMap = facilityCategoryRepository.selectBatchIds(
                facilities.stream().map(HotelFacility::getCategoryId).distinct().collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(FacilityCategory::getId, category -> category));

        return facilities.stream()
                .map(facility -> convertToFacilityResponse(facility, categoryMap.get(facility.getCategoryId())))
                .collect(Collectors.toList());
    }

    // ========== 转换方法 ==========

    private FacilityCategoryResponse convertToCategoryResponse(FacilityCategory category) {
        FacilityCategoryResponse response = new FacilityCategoryResponse();
        response.setId(category.getId());
        response.setHotelId(category.getHotelId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setIcon(category.getIcon());
        response.setDisplayOrder(category.getDisplayOrder());
        response.setIsActive(category.getIsActive());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }

    private FacilityResponse convertToFacilityResponse(HotelFacility facility, FacilityCategory category) {
        FacilityResponse response = new FacilityResponse();
        response.setId(facility.getId());
        response.setHotelId(facility.getHotelId());
        response.setCategoryId(facility.getCategoryId());
        response.setName(facility.getName());
        response.setDescription(facility.getDescription());
        response.setIcon(facility.getIcon());
        response.setStatus(facility.getStatus());
        response.setStatusDescription(HotelFacility.FacilityStatus.fromCode(facility.getStatus()).getDescription());
        response.setIsFeatured(facility.getIsFeatured());
        response.setDisplayOrder(facility.getDisplayOrder());
        response.setCreatedAt(facility.getCreatedAt());
        response.setUpdatedAt(facility.getUpdatedAt());

        if (category != null) {
            response.setCategory(convertToCategoryResponse(category));
        }

        return response;
    }
}