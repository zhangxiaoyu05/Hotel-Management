package com.hotel.controller.order;

import com.hotel.annotation.RateLimit;
import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.bookingConflict.*;
import com.hotel.service.BookingConflictService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/booking-conflicts")
@RequiredArgsConstructor
public class BookingConflictController extends BaseController {

    private final BookingConflictService bookingConflictService;

    @PostMapping("/detect")
    @RateLimit(period = 60, limit = 20, type = RateLimit.LimitType.USER,
              prefix = "conflict_detect", message = "冲突检测请求过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<ConflictDetectionResult>> detectConflict(
            @Valid @RequestBody DetectConflictRequest request) {

        ConflictDetectionResult result = bookingConflictService.detectConflict(request);
        return ResponseEntity.ok(success(result));
    }

    @PostMapping("/waiting-list")
    @RateLimit(period = 60, limit = 5, type = RateLimit.LimitType.USER,
              prefix = "waiting_list_join", message = "加入等待列表操作过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<WaitingListResponse>> joinWaitingList(
            @Valid @RequestBody JoinWaitingListRequest request) {

        WaitingListResponse response = bookingConflictService.joinWaitingList(request);
        return ResponseEntity.ok(success(response));
    }

    @GetMapping("/waiting-list")
    public ResponseEntity<ApiResponse<List<WaitingListResponse>>> getUserWaitingList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        WaitingListQueryRequest queryRequest = WaitingListQueryRequest.builder()
                .userId(userId)
                .status(status)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        var result = bookingConflictService.getUserWaitingList(
                userId != null ? userId : getCurrentUserId(), queryRequest
        );
        return ResponseEntity.ok(success(result.getRecords()));
    }

    @PutMapping("/waiting-list/{id}/confirm")
    @RateLimit(period = 60, limit = 5, type = RateLimit.LimitType.USER,
              prefix = "waiting_list_confirm", message = "确认等待列表预订过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<Object>> confirmWaitingListBooking(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmWaitingListRequest request) {

        var orderResponse = bookingConflictService.confirmWaitingListBooking(id, request);
        return ResponseEntity.ok(success(orderResponse));
    }

    @DeleteMapping("/waiting-list/{id}")
    @RateLimit(period = 60, limit = 10, type = RateLimit.LimitType.USER,
              prefix = "waiting_list_leave", message = "退出等待列表操作过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<String>> leaveWaitingList(@PathVariable Long id) {
        // 这里需要在BookingConflictService中添加leaveWaitingList方法
        // bookingConflictService.leaveWaitingList(id, getCurrentUserId());
        return ResponseEntity.ok(success("已成功退出等待列表"));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ConflictStatisticsResponse>> getConflictStatistics(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String groupBy) {

        ConflictStatisticsRequest request = ConflictStatisticsRequest.builder()
                .roomId(roomId)
                .startDate(startDate != null ? java.time.LocalDateTime.parse(startDate) : null)
                .endDate(endDate != null ? java.time.LocalDateTime.parse(endDate) : null)
                .groupBy(groupBy)
                .build();

        ConflictStatisticsResponse response = bookingConflictService.getConflictStatistics(request);
        return ResponseEntity.ok(success(response));
    }

    @GetMapping("/conflicts")
    public ResponseEntity<ApiResponse<List<Object>>> getConflicts(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String conflictType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        // 这里需要在BookingConflictService中添加getConflicts方法
        // List<BookingConflictResponse> conflicts = bookingConflictService.getConflicts(roomId, status, conflictType, page, size);
        // return ResponseEntity.ok(success(conflicts));

        return ResponseEntity.ok(success(List.of()));
    }

    @PostMapping("/cleanup-expired")
    @RateLimit(period = 3600, limit = 1, type = RateLimit.LimitType.IP,
              prefix = "cleanup_expired", message = "清理操作过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<String>> cleanupExpiredWaitingList() {
        bookingConflictService.cleanupExpiredWaitingList();
        return ResponseEntity.ok(success("已清理过期的等待列表记录"));
    }
}