package com.hotel.controller.room;

import com.hotel.annotation.RateLimit;
import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.room.*;
import com.hotel.service.RoomService;
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
 * 房间管理控制器
 */
@Tag(name = "房间管理", description = "房间管理相关接口")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Validated
public class RoomController extends BaseController {

    private final RoomService roomService;

    @Operation(summary = "创建房间", description = "管理员创建新的房间")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(period = 60, limit = 10, type = RateLimit.LimitType.USER,
              prefix = "room_create", message = "创建房间过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return success(response, "房间创建成功");
    }

    @Operation(summary = "获取房间列表", description = "分页获取房间列表，支持搜索和筛选")
    @GetMapping
    @RateLimit(period = 60, limit = 100, type = RateLimit.LimitType.IP,
              prefix = "room_list", message = "请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<RoomListResponse>> getRooms(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @Parameter(description = "搜索关键词，搜索房间号") @RequestParam(required = false) String search,
            @Parameter(description = "酒店ID筛选") @RequestParam(required = false) Long hotelId,
            @Parameter(description = "房间状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "roomNumber") String sortBy,
            @Parameter(description = "排序方向 ASC/DESC") @RequestParam(defaultValue = "ASC") String sortDir) {

        RoomListResponse response = roomService.getRoomsWithPage(
                page, size, search, hotelId, status, sortBy, sortDir
        );
        return success(response);
    }

    @Operation(summary = "搜索房间", description = "使用高级搜索条件搜索房间")
    @PostMapping("/search")
    @RateLimit(period = 60, limit = 50, type = RateLimit.LimitType.IP,
              prefix = "room_search", message = "搜索请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<RoomListResponse>> searchRooms(
            @Valid @RequestBody RoomSearchRequest searchRequest) {
        RoomListResponse response = roomService.searchRooms(searchRequest);
        return success(response);
    }

    @Operation(summary = "根据酒店ID获取房间", description = "获取指定酒店的所有房间")
    @GetMapping("/hotel/{hotelId}")
    @RateLimit(period = 60, limit = 30, type = RateLimit.LimitType.IP,
              prefix = "room_hotel", message = "请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByHotel(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId) {
        List<RoomResponse> response = roomService.getRoomsByHotelId(hotelId);
        return success(response);
    }

    @Operation(summary = "获取房间详情", description = "根据ID获取房间详细信息")
    @GetMapping("/{id}")
    @RateLimit(period = 60, limit = 60, type = RateLimit.LimitType.IP,
              prefix = "room_detail", message = "请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(
            @Parameter(description = "房间ID") @PathVariable Long id) {
        RoomResponse response = roomService.getRoomById(id);
        return success(response);
    }

    @Operation(summary = "更新房间", description = "管理员更新房间信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(period = 60, limit = 20, type = RateLimit.LimitType.USER,
              prefix = "room_update", message = "更新房间过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @Parameter(description = "房间ID") @PathVariable Long id,
            @Valid @RequestBody UpdateRoomRequest request) {
        RoomResponse response = roomService.updateRoom(id, request);
        return success(response, "房间更新成功");
    }

    @Operation(summary = "删除房间", description = "管理员删除房间")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(period = 60, limit = 5, type = RateLimit.LimitType.USER,
              prefix = "room_delete", message = "删除房间过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @Parameter(description = "房间ID") @PathVariable Long id) {
        roomService.deleteRoom(id);
        return success(null, "房间删除成功");
    }

    @Operation(summary = "批量更新房间", description = "管理员批量更新房间状态和价格")
    @PostMapping("/batch-update")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(period = 60, limit = 10, type = RateLimit.LimitType.USER,
              prefix = "room_batch_update", message = "批量更新房间过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<Void>> batchUpdateRooms(
            @Valid @RequestBody BatchUpdateRequest request) {
        roomService.batchUpdateRooms(request);
        return success(null, "批量更新成功");
    }

    @Operation(summary = "根据房间类型获取房间", description = "获取指定房间类型的所有房间")
    @GetMapping("/room-types/{roomTypeId}")
    @RateLimit(period = 60, limit = 30, type = RateLimit.LimitType.IP,
              prefix = "room_type_rooms", message = "请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByRoomType(
            @Parameter(description = "房间类型ID") @PathVariable Long roomTypeId) {
        List<RoomResponse> response = roomService.getRoomsByRoomTypeId(roomTypeId);
        return success(response);
    }

    @Operation(summary = "获取可用房间", description = "获取所有状态为可用的房间")
    @GetMapping("/available")
    @RateLimit(period = 60, limit = 30, type = RateLimit.LimitType.IP,
              prefix = "room_available", message = "请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRooms() {
        RoomSearchRequest searchRequest = new RoomSearchRequest();
        searchRequest.setStatus("AVAILABLE");
        searchRequest.setSize(1000); // 获取所有可用房间

        RoomListResponse response = roomService.searchRooms(searchRequest);
        return success(response.getContent());
    }
}