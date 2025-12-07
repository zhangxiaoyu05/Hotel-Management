package com.hotel.controller.facility;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.facility.*;
import com.hotel.service.FacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.List;

@Tag(name = "酒店设施管理", description = "酒店设施管理相关接口")
@RestController
@RequestMapping("/api/hotels/{hotelId}/facilities")
@RequiredArgsConstructor
@Validated
public class FacilityController extends BaseController {

    private final FacilityService facilityService;

    // ========== 设施分类管理 ==========

    @Operation(summary = "创建设施分类", description = "为酒店创建新的设施分类")
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<FacilityCategoryResponse>> createCategory(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Valid @RequestBody CreateFacilityCategoryRequest request) {
        request.setHotelId(hotelId);
        FacilityCategoryResponse response = facilityService.createCategory(request);
        return success(response, "设施分类创建成功");
    }

    @Operation(summary = "更新设施分类", description = "更新设施分类信息")
    @PutMapping("/categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<FacilityCategoryResponse>> updateCategory(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Valid @RequestBody CreateFacilityCategoryRequest request) {
        request.setHotelId(hotelId);
        FacilityCategoryResponse response = facilityService.updateCategory(categoryId, request);
        return success(response, "设施分类更新成功");
    }

    @Operation(summary = "删除设施分类", description = "删除设施分类")
    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Parameter(description = "分类ID") @PathVariable Long categoryId) {
        facilityService.deleteCategory(categoryId);
        return success(null, "设施分类删除成功");
    }

    @Operation(summary = "获取酒店设施分类列表", description = "获取酒店的所有设施分类")
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<FacilityCategoryResponse>>> getCategories(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Parameter(description = "是否只获取激活的分类") @RequestParam(defaultValue = "true") Boolean activeOnly) {

        List<FacilityCategoryResponse> response = activeOnly
                ? facilityService.getActiveCategoriesByHotel(hotelId)
                : facilityService.getCategoriesByHotel(hotelId);
        return success(response);
    }

    // ========== 设施管理 ==========

    @Operation(summary = "创建设施", description = "为酒店创建新的设施")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<FacilityResponse>> createFacility(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Valid @RequestBody CreateFacilityRequest request) {
        request.setHotelId(hotelId);
        FacilityResponse response = facilityService.createFacility(request);
        return success(response, "设施创建成功");
    }

    @Operation(summary = "更新设施", description = "更新设施信息")
    @PutMapping("/{facilityId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<FacilityResponse>> updateFacility(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Parameter(description = "设施ID") @PathVariable Long facilityId,
            @Valid @RequestBody CreateFacilityRequest request) {
        request.setHotelId(hotelId);
        FacilityResponse response = facilityService.updateFacility(facilityId, request);
        return success(response, "设施更新成功");
    }

    @Operation(summary = "删除设施", description = "删除设施")
    @DeleteMapping("/{facilityId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteFacility(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Parameter(description = "设施ID") @PathVariable Long facilityId) {
        facilityService.deleteFacility(facilityId);
        return success(null, "设施删除成功");
    }

    @Operation(summary = "更新设施状态", description = "更新设施的可用状态")
    @PatchMapping("/{facilityId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> updateFacilityStatus(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Parameter(description = "设施ID") @PathVariable Long facilityId,
            @Parameter(description = "设施状态") @RequestParam String status) {
        facilityService.updateFacilityStatus(facilityId, status);
        return success(null, "设施状态更新成功");
    }

    @Operation(summary = "搜索设施", description = "根据条件搜索设施")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<IPage<FacilityResponse>>> searchFacilities(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "设施状态") @RequestParam(required = false) String status,
            @Parameter(description = "是否特色设施") @RequestParam(required = false) Boolean isFeatured,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "displayOrder") String sortBy,
            @Parameter(description = "排序方向 ASC/DESC") @RequestParam(defaultValue = "ASC") String sortDir) {

        FacilitySearchRequest request = new FacilitySearchRequest();
        request.setHotelId(hotelId);
        request.setCategoryId(categoryId);
        request.setStatus(status);
        request.setIsFeatured(isFeatured);
        request.setKeyword(keyword);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDir);

        IPage<FacilityResponse> response = facilityService.searchFacilities(request);
        return success(response);
    }

    @Operation(summary = "获取酒店设施列表", description = "获取酒店的所有设施")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FacilityResponse>>> getFacilities(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId) {
        List<FacilityResponse> response = facilityService.getFacilitiesByHotel(hotelId);
        return success(response);
    }

    @Operation(summary = "获取分类下的设施", description = "获取指定分类下的设施")
    @GetMapping("/categories/{categoryId}/facilities")
    public ResponseEntity<ApiResponse<List<FacilityResponse>>> getFacilitiesByCategory(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Parameter(description = "分类ID") @PathVariable Long categoryId) {
        List<FacilityResponse> response = facilityService.getFacilitiesByCategory(hotelId, categoryId);
        return success(response);
    }

    @Operation(summary = "获取特色设施", description = "获取酒店的特色设施")
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<FacilityResponse>>> getFeaturedFacilities(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId) {
        List<FacilityResponse> response = facilityService.getFeaturedFacilities(hotelId);
        return success(response);
    }
}