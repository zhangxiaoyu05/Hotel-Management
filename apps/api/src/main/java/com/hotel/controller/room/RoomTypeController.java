package com.hotel.controller.room;

import com.hotel.annotation.RateLimit;
import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.roomtype.*;
import com.hotel.enums.RoomTypeStatus;
import com.hotel.service.RoomTypeService;
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

/**
 * 房间类型管理控制器
 */
@Tag(name = "房间类型管理", description = "房间类型管理相关接口")
@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
@Validated
public class RoomTypeController extends BaseController {

    private final RoomTypeService roomTypeService;

    @Operation(summary = "创建房间类型", description = "管理员创建新的房间类型")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(period = 60, limit = 10, type = RateLimit.LimitType.USER,
              prefix = "room_type_create", message = "创建房间类型过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> createRoomType(
            @Valid @RequestBody CreateRoomTypeRequest request) {
        RoomTypeResponse response = roomTypeService.createRoomType(request);
        return success(response, "房间类型创建成功");
    }

    @Operation(summary = "获取房间类型列表", description = "分页获取房间类型列表，支持搜索和筛选")
    @GetMapping
    @RateLimit(period = 60, limit = 100, type = RateLimit.LimitType.IP,
              prefix = "room_type_list", message = "请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<RoomTypeListResponse>> getRoomTypes(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @Parameter(description = "搜索关键词，搜索名称和描述") @RequestParam(required = false) String search,
            @Parameter(description = "酒店ID筛选") @RequestParam(required = false) Long hotelId,
            @Parameter(description = "房间状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向 ASC/DESC") @RequestParam(defaultValue = "DESC") String sortDir) {

        RoomTypeListResponse response = roomTypeService.getRoomTypesWithPage(
                page, size, search, hotelId, status, sortBy, sortDir
        );
        return success(response);
    }

    @Operation(summary = "根据酒店ID获取房间类型", description = "获取指定酒店的所有房间类型")
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<ApiResponse<List<RoomTypeResponse>>> getRoomTypesByHotel(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId) {
        List<RoomTypeResponse> response = roomTypeService.getRoomTypesByHotelId(hotelId);
        return success(response);
    }

    @Operation(summary = "获取房间类型详情", description = "根据ID获取房间类型详细信息")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> getRoomType(
            @Parameter(description = "房间类型ID") @PathVariable Long id) {
        RoomTypeResponse response = roomTypeService.getRoomTypeById(id);
        return success(response);
    }

    @Operation(summary = "更新房间类型", description = "管理员更新房间类型信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(period = 60, limit = 20, type = RateLimit.LimitType.USER,
              prefix = "room_type_update", message = "更新房间类型过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> updateRoomType(
            @Parameter(description = "房间类型ID") @PathVariable Long id,
            @Valid @RequestBody UpdateRoomTypeRequest request) {
        RoomTypeResponse response = roomTypeService.updateRoomType(id, request);
        return success(response, "房间类型更新成功");
    }

    @Operation(summary = "删除房间类型", description = "管理员删除房间类型（需要确保没有关联的房间）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(period = 60, limit = 5, type = RateLimit.LimitType.USER,
              prefix = "room_type_delete", message = "删除房间类型过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<Void>> deleteRoomType(
            @Parameter(description = "房间类型ID") @PathVariable Long id) {
        roomTypeService.deleteRoomType(id);
        return success(null, "房间类型删除成功");
    }

    @Operation(summary = "更新房间类型状态", description = "管理员更新房间类型状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> updateRoomTypeStatus(
            @Parameter(description = "房间类型ID") @PathVariable Long id,
            @Parameter(description = "房间类型状态") @RequestParam RoomTypeStatus status) {
        RoomTypeResponse response = roomTypeService.updateRoomTypeStatus(id, status);
        return success(response, "房间类型状态更新成功");
    }

    @Operation(summary = "获取活跃房间类型", description = "获取所有状态为活跃的房间类型")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<RoomTypeResponse>>> getActiveRoomTypes() {
        List<RoomTypeResponse> response = roomTypeService.getRoomTypesByStatus(RoomTypeStatus.ACTIVE.name());
        return success(response);
    }

    @Operation(summary = "获取房间类型关联的房间", description = "获取与指定房间类型关联的所有房间")
    @GetMapping("/{id}/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getRoomsByRoomType(
            @Parameter(description = "房间类型ID") @PathVariable Long id) {
        // 这里需要实现获取房间列表的逻辑
        // 暂时返回空列表，等房间管理功能实现后再完善
        return success(List.of(), "房间关联功能待实现");
    }
}