package com.hotel.controller.notification;

import com.hotel.annotation.RateLimit;
import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.notification.NotificationResponse;
import com.hotel.service.NotificationService;
import com.hotel.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserNotifications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        List<NotificationResponse> notifications = notificationService.getUserNotifications(
                currentUserId, page, limit);

        int totalCount = notificationService.countByUserId(currentUserId);
        int unreadCount = notificationService.countUnreadByUserId(currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("notifications", notifications);
        data.put("total", totalCount);
        data.put("unreadCount", unreadCount);
        data.put("page", page);
        data.put("limit", limit);

        return ResponseEntity.ok(success(data));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Boolean>> markAsRead(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        boolean result = notificationService.markAsRead(id, currentUserId);
        return ResponseEntity.ok(success(result));
    }

    @PutMapping("/read-all")
    @RateLimit(period = 60, limit = 5, type = RateLimit.LimitType.USER,
              prefix = "notification_read_all", message = "批量标记已读操作过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<Boolean>> markAllAsRead() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        boolean result = notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(success(result));
    }

    @DeleteMapping("/{id}")
    @RateLimit(period = 60, limit = 30, type = RateLimit.LimitType.USER,
              prefix = "notification_delete", message = "通知删除操作过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<Boolean>> deleteNotification(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        boolean result = notificationService.deleteNotification(id, currentUserId);
        return ResponseEntity.ok(success(result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        int unreadCount = notificationService.countUnreadByUserId(currentUserId);
        return ResponseEntity.ok(success(unreadCount));
    }
}