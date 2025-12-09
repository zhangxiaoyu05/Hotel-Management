package com.hotel.controller.review;

import com.hotel.common.ApiResponse;
import com.hotel.dto.review.incentive.UserPointsSummaryDTO;
import com.hotel.service.ReviewIncentiveService;
import com.hotel.service.ReviewReminderService;
import com.hotel.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/v1/users/me/review-reminders")
@RequiredArgsConstructor
public class ReviewReminderController {

    private final ReviewReminderService reviewReminderService;
    private final ReviewIncentiveService reviewIncentiveService;
    private final UserContextService userContextService;

    /**
     * 获取用户积分汇总信息
     */
    @GetMapping("/points")
    public ResponseEntity<ApiResponse<UserPointsSummaryDTO>> getUserPointsSummary() {
        Long userId = userContextService.getCurrentUserId();
        UserPointsSummaryDTO summary = reviewIncentiveService.getUserPointsSummary(userId);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * 手动发送评价提醒
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendManualReminder(
            @RequestParam Long orderId) {
        Long userId = userContextService.getCurrentUserId();

        try {
            reviewReminderService.sendManualReminder(userId, orderId);
            return ResponseEntity.ok(ApiResponse.success("提醒发送成功"));
        } catch (Exception e) {
            log.error("发送评价提醒失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("发送提醒失败：" + e.getMessage()));
        }
    }

    /**
     * 获取用户积分历史记录
     */
    @GetMapping("/points/history")
    public ResponseEntity<ApiResponse<Object>> getPointsHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = userContextService.getCurrentUserId();

        // 这里简化实现，实际应该使用分页查询
        UserPointsSummaryDTO summary = reviewIncentiveService.getUserPointsSummary(userId);

        return ResponseEntity.ok(ApiResponse.success(summary.getRecentHistory()));
    }
}