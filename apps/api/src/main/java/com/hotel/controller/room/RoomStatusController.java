package com.hotel.controller.room;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.entity.RoomStatusLog;
import com.hotel.service.RoomStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "房间状态管理", description = "房间状态管理相关接口")
public class RoomStatusController extends BaseController {

    private final RoomStatusService roomStatusService;

    @Operation(summary = "更新房间状态")
    @PutMapping("/{roomId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Boolean> updateRoomStatus(
            @Parameter(description = "房间ID") @PathVariable Long roomId,
            @RequestBody UpdateRoomStatusRequest request) {

        boolean success = roomStatusService.updateRoomStatus(
                roomId,
                request.getStatus(),
                request.getReason(),
                getCurrentUserId(),
                request.getOrderId(),
                request.getExpectedVersion()
        );

        return success ? ApiResponse.success(true) : ApiResponse.error("更新房间状态失败");
    }

    @Operation(summary = "获取房间状态变更日志")
    @GetMapping("/{roomId}/status/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public ApiResponse<IPage<RoomStatusLog>> getRoomStatusLogs(
            @Parameter(description = "房间ID") @PathVariable Long roomId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "开始日期") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

        IPage<RoomStatusLog> logs = roomStatusService.getRoomStatusLogsWithPagination(
                new Page<>(page, size),
                roomId,
                startDate,
                endDate
        );

        return ApiResponse.success(logs);
    }

    @Operation(summary = "获取最近状态变更记录")
    @GetMapping("/{roomId}/status/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public ApiResponse<List<RoomStatusLog>> getRecentStatusLogs(
            @Parameter(description = "房间ID") @PathVariable Long roomId,
            @Parameter(description = "记录数量限制") @RequestParam(defaultValue = "10") Integer limit) {

        List<RoomStatusLog> logs = roomStatusService.getRoomStatusHistory(roomId, null, null, limit);
        return ApiResponse.success(logs);
    }

    @Operation(summary = "检查房间可用性")
    @GetMapping("/{roomId}/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public ApiResponse<Boolean> checkRoomAvailability(
            @Parameter(description = "房间ID") @PathVariable Long roomId) {

        boolean available = roomStatusService.isRoomAvailable(roomId);
        return ApiResponse.success(available);
    }

    @Operation(summary = "批量检查房间可用性")
    @PostMapping("/availability/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public ApiResponse<java.util.Map<Long, Boolean>> checkRoomsAvailability(
            @RequestBody CheckAvailabilityRequest request) {

        java.util.Map<Long, Boolean> availability = roomStatusService.checkRoomsAvailability(request.getRoomIds());
        return ApiResponse.success(availability);
    }

    // DTO Classes
    public static class UpdateRoomStatusRequest {
        private String status;
        private String reason;
        private Long orderId;
        private Integer expectedVersion;

        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public Integer getExpectedVersion() { return expectedVersion; }
        public void setExpectedVersion(Integer expectedVersion) { this.expectedVersion = expectedVersion; }
    }

    public static class CheckAvailabilityRequest {
        private List<Long> roomIds;

        public List<Long> getRoomIds() { return roomIds; }
        public void setRoomIds(List<Long> roomIds) { this.roomIds = roomIds; }
    }
}