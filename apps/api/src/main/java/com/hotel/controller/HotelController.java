package com.hotel.controller;

import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.hotel.*;
import com.hotel.dto.facility.FacilityCategoryResponse;
import com.hotel.dto.facility.FacilityResponse;
import com.hotel.service.HotelService;
import com.hotel.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.List;

@Tag(name = "酒店管理", description = "酒店管理相关接口")
@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Validated
public class HotelController extends BaseController {

    private final HotelService hotelService;

    @Operation(summary = "创建酒店", description = "管理员创建新酒店")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HotelResponse>> createHotel(
            @Valid @RequestBody CreateHotelRequest request,
            Authentication authentication) {
        Long createdBy = SecurityUtils.getCurrentUserId(authentication);
        HotelResponse response = hotelService.createHotel(request, createdBy);
        return success(response, "酒店创建成功");
    }

    @Operation(summary = "获取酒店列表", description = "分页获取酒店列表，支持搜索和筛选")
    @GetMapping
    public ResponseEntity<ApiResponse<HotelListResponse>> getHotels(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @Parameter(description = "搜索关键词，搜索名称、地址和描述") @RequestParam(required = false) String search,
            @Parameter(description = "酒店状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向 ASC/DESC") @RequestParam(defaultValue = "DESC") String sortDir) {
        HotelListResponse response = hotelService.getHotels(page, size, search, status, sortBy, sortDir);
        return success(response, "获取酒店列表成功");
    }

    @Operation(summary = "获取酒店详情", description = "根据ID获取酒店详细信息")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponse>> getHotelById(
            @Parameter(description = "酒店ID") @PathVariable @Min(1) Long id) {
        HotelResponse response = hotelService.getHotelById(id);
        return success(response, "获取酒店详情成功");
    }

    @Operation(summary = "更新酒店", description = "管理员更新酒店信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HotelResponse>> updateHotel(
            @Parameter(description = "酒店ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateHotelRequest request) {
        HotelResponse response = hotelService.updateHotel(id, request);
        return success(response, "酒店更新成功");
    }

    @Operation(summary = "删除酒店", description = "管理员删除酒店（软删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteHotel(
            @Parameter(description = "酒店ID") @PathVariable @Min(1) Long id) {
        hotelService.deleteHotel(id);
        return success(null, "酒店删除成功");
    }

    @Operation(summary = "更新酒店状态", description = "管理员更新酒店营业状态")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HotelResponse>> updateHotelStatus(
            @Parameter(description = "酒店ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateHotelStatusRequest request) {
        HotelResponse response = hotelService.updateHotelStatus(id, request);
        return success(response, "酒店状态更新成功");
    }

    // ========== 酒店设施相关接口 ==========

    @Operation(summary = "获取酒店设施分类", description = "获取酒店的设施分类列表")
    @GetMapping("/{id}/facility-categories")
    public ResponseEntity<ApiResponse<List<FacilityCategoryResponse>>> getHotelFacilityCategories(
            @Parameter(description = "酒店ID") @PathVariable @Min(1) Long id) {
        List<FacilityCategoryResponse> response = hotelService.getHotelFacilityCategories(id);
        return success(response);
    }

    @Operation(summary = "获取酒店设施", description = "获取酒店的所有设施")
    @GetMapping("/{id}/facilities")
    public ResponseEntity<ApiResponse<List<FacilityResponse>>> getHotelFacilities(
            @Parameter(description = "酒店ID") @PathVariable @Min(1) Long id) {
        List<FacilityResponse> response = hotelService.getHotelFacilities(id);
        return success(response);
    }

    @Operation(summary = "获取酒店特色设施", description = "获取酒店的特色设施")
    @GetMapping("/{id}/featured-facilities")
    public ResponseEntity<ApiResponse<List<FacilityResponse>>> getHotelFeaturedFacilities(
            @Parameter(description = "酒店ID") @PathVariable @Min(1) Long id) {
        List<FacilityResponse> response = hotelService.getHotelFeaturedFacilities(id);
        return success(response);
    }
}