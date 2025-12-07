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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    // ==================== 价格计算相关API ====================

    @Operation(summary = "获取房间在指定日期的价格", description = "根据价格策略计算房间在指定日期的价格")
    @GetMapping("/{id}/price-for-date")
    @RateLimit(period = 60, limit = 50, type = RateLimit.LimitType.IP,
              prefix = "room_price_date", message = "价格计算请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<BigDecimal>> getRoomPriceForDate(
            @Parameter(description = "房间ID") @PathVariable Long id,
            @Parameter(description = "日期") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {

        BigDecimal price = roomService.getRoomPriceForDate(id, date);
        return success(price, "获取房间价格成功");
    }

    @Operation(summary = "获取房间在日期范围内的价格", description = "计算房间在指定日期范围内的每日价格")
    @GetMapping("/{id}/prices-for-range")
    @RateLimit(period = 60, limit = 20, type = RateLimit.LimitType.IP,
              prefix = "room_prices_range", message = "价格计算请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<Map<LocalDate, BigDecimal>>> getRoomPricesForDateRange(
            @Parameter(description = "房间ID") @PathVariable Long id,
            @Parameter(description = "开始日期") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<LocalDate, BigDecimal> prices = roomService.getRoomPricesForDateRange(id, startDate, endDate);
        return success(prices, "获取房间价格范围成功");
    }

    @Operation(summary = "获取房间列表的价格信息", description = "批量获取多个房间在指定日期的价格")
    @PostMapping("/prices-for-date")
    @RateLimit(period = 60, limit = 20, type = RateLimit.LimitType.IP,
              prefix = "rooms_prices", message = "价格计算请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<Map<Long, BigDecimal>>> getRoomsPricesForDate(
            @Parameter(description = "房间ID列表") @RequestBody List<Long> roomIds,
            @Parameter(description = "日期") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {

        Map<Long, BigDecimal> prices = roomService.getRoomPricesForDate(roomIds, date);
        return success(prices, "获取房间列表价格成功");
    }

    @Operation(summary = "获取包含价格信息的房间详情", description = "获取房间详情，包含根据价格策略计算后的价格信息")
    @GetMapping("/{id}/with-price")
    @RateLimit(period = 60, limit = 30, type = RateLimit.LimitType.IP,
              prefix = "room_with_price", message = "请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomWithPriceInfo(
            @Parameter(description = "房间ID") @PathVariable Long id,
            @Parameter(description = "日期") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {

        RoomResponse response = roomService.getRoomWithPriceInfo(id, date);
        return success(response, "获取房间详情成功");
    }

    @Operation(summary = "批量更新房间价格", description = "批量更新多个房间的价格并记录价格变更历史")
    @PostMapping("/batch-update-price")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(period = 60, limit = 5, type = RateLimit.LimitType.USER,
              prefix = "room_batch_price", message = "批量更新价格过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<Void>> batchUpdateRoomPrice(
            @Parameter(description = "房间ID列表") @RequestBody List<Long> roomIds,
            @Parameter(description = "新价格") @RequestParam BigDecimal newPrice,
            @Parameter(description = "变更原因") @RequestParam(required = false) String reason) {

        roomService.batchUpdateRoomPrice(roomIds, newPrice, reason);
        return success(null, "批量更新房间价格成功");
    }
}