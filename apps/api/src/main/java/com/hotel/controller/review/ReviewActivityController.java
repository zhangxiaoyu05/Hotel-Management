package com.hotel.controller.review;

import com.hotel.common.ApiResponse;
import com.hotel.dto.review.incentive.ReviewActivityDTO;
import com.hotel.dto.review.incentive.ReviewLeaderboardDTO;
import com.hotel.dto.review.incentive.HighQualityBadgeDTO;
import com.hotel.service.ReviewActivityService;
import com.hotel.service.ReviewQualityService;
import com.hotel.validation.ValidActivityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/reviews/activities")
@RequiredArgsConstructor
@Validated
public class ReviewActivityController {

    private final ReviewActivityService reviewActivityService;
    private final ReviewQualityService reviewQualityService;

    /**
     * 获取所有有效的活动
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewActivityDTO>>> getActiveActivities(
            @RequestParam(value = "active", defaultValue = "true") Boolean active) {

        List<ReviewActivityDTO> activities;
        if (active) {
            activities = reviewActivityService.getActiveActivities();
        } else {
            activities = reviewActivityService.getUpcomingActivities();
        }

        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    /**
     * 根据类型获取活动
     */
    @GetMapping("/type/{activityType}")
    public ResponseEntity<ApiResponse<List<ReviewActivityDTO>>> getActivitiesByType(
            @PathVariable @ValidActivityType String activityType) {
        List<ReviewActivityDTO> activities = reviewActivityService.getActivitiesByType(activityType);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    /**
     * 参与活动
     */
    @PostMapping("/{activityId}/join")
    public ResponseEntity<ApiResponse<Map<String, Object>>> joinActivity(
            @PathVariable @NotNull @Min(1) Long activityId) {
        try {
            Map<String, Object> result = reviewActivityService.joinActivity(activityId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("参与活动失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("参与活动失败：" + e.getMessage()));
        }
    }

    /**
     * 获取评价排行榜
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<ReviewLeaderboardDTO>> getLeaderboard(
            @RequestParam(defaultValue = "monthly")
            @Pattern(regexp = "monthly|quarterly|yearly", message = "无效的排行榜类型") String type,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().toString()}")
            @Pattern(regexp = "\\d{4}-\\d{2}|\\d{4}-Q[1-4]|\\d{4}", message = "无效的周期格式") String period) {

        ReviewLeaderboardDTO leaderboard = reviewQualityService.getLeaderboard(type, period);
        return ResponseEntity.ok(ApiResponse.success(leaderboard));
    }

    /**
     * 获取评价的优质标识
     */
    @GetMapping("/{reviewId}/badge")
    public ResponseEntity<ApiResponse<HighQualityBadgeDTO>> getReviewBadge(
            @PathVariable @NotNull @Min(1) Long reviewId) {
        HighQualityBadgeDTO badge = reviewQualityService.getReviewBadge(reviewId);

        if (badge == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        return ResponseEntity.ok(ApiResponse.success(badge));
    }

    // 管理员接口
    /**
     * 创建新活动（管理员）
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewActivityDTO>> createActivity(@Valid @RequestBody ReviewActivityDTO dto) {
        try {
            ReviewActivityDTO activity = reviewActivityService.createActivity(dto);
            return ResponseEntity.ok(ApiResponse.success(activity));
        } catch (Exception e) {
            log.error("创建活动失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("创建活动失败：" + e.getMessage()));
        }
    }

    /**
     * 更新活动（管理员）
     */
    @PutMapping("/admin/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewActivityDTO>> updateActivity(
            @PathVariable Long activityId,
            @Valid @RequestBody ReviewActivityDTO dto) {
        try {
            ReviewActivityDTO activity = reviewActivityService.updateActivity(activityId, dto);
            return ResponseEntity.ok(ApiResponse.success(activity));
        } catch (Exception e) {
            log.error("更新活动失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("更新活动失败：" + e.getMessage()));
        }
    }

    /**
     * 删除活动（管理员）
     */
    @DeleteMapping("/admin/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteActivity(@PathVariable Long activityId) {
        try {
            reviewActivityService.deleteActivity(activityId);
            return ResponseEntity.ok(ApiResponse.success("活动删除成功"));
        } catch (Exception e) {
            log.error("删除活动失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("删除活动失败：" + e.getMessage()));
        }
    }

    /**
     * 清除排行榜缓存（管理员）
     */
    @PostMapping("/admin/clear-cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearLeaderboardCache() {
        try {
            reviewQualityService.clearLeaderboardCache();
            return ResponseEntity.ok(ApiResponse.success("排行榜缓存清除成功"));
        } catch (Exception e) {
            log.error("清除排行榜缓存失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("清除缓存失败：" + e.getMessage()));
        }
    }
}