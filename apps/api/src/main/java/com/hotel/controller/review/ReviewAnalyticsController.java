package com.hotel.controller.review;

import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.review.ReviewStatisticsResponse;
import com.hotel.dto.review.admin.ReviewAnalyticsRequest;
import com.hotel.service.ReviewAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin/reviews/analytics")
@RequiredArgsConstructor
@Tag(name = "评价分析", description = "管理员评价统计分析相关接口")
@PreAuthorize("hasRole('ADMIN')")
public class ReviewAnalyticsController extends BaseController {

    private final ReviewAnalyticsService reviewAnalyticsService;

    @Operation(summary = "获取评价总体统计数据")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ReviewStatisticsResponse>> getOverallStatistics(
            @RequestParam(required = false) Long hotelId) {
        ReviewStatisticsResponse statistics = reviewAnalyticsService.getOverallStatistics(hotelId);
        return ResponseEntity.ok(ApiResponse.success("获取统计数据成功", statistics));
    }

    @Operation(summary = "获取评价趋势分析数据")
    @PostMapping("/trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviewTrends(
            @Valid @RequestBody ReviewAnalyticsRequest request) {
        Map<String, Object> trends = reviewAnalyticsService.getReviewTrends(request);
        return ResponseEntity.ok(ApiResponse.success("获取趋势数据成功", trends));
    }

    @Operation(summary = "获取评价质量分析数据")
    @GetMapping("/quality")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQualityAnalysis(
            @RequestParam(required = false) Long hotelId) {
        Map<String, Object> quality = reviewAnalyticsService.getQualityAnalysis(hotelId);
        return ResponseEntity.ok(ApiResponse.success("获取质量分析数据成功", quality));
    }

    @Operation(summary = "获取管理员审核统计数据")
    @GetMapping("/moderation-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getModerationStatistics() {
        Long adminId = getCurrentUserId();
        Map<String, Object> stats = reviewAnalyticsService.getModerationStatistics(adminId);
        return ResponseEntity.ok(ApiResponse.success("获取审核统计数据成功", stats));
    }

    @Operation(summary = "清空分析缓存（用于数据更新后刷新）")
    @PostMapping("/clear-cache")
    public ResponseEntity<ApiResponse<Void>> clearAnalyticsCache() {
        reviewAnalyticsService.clearAnalyticsCache();
        return ResponseEntity.ok(ApiResponse.success("清空缓存成功", null));
    }

    @Operation(summary = "获取管理仪表板数据")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardData(
            @RequestParam(required = false) Long hotelId) {
        Map<String, Object> dashboard = new java.util.HashMap<>();

        // 总体统计
        ReviewStatisticsResponse statistics = reviewAnalyticsService.getOverallStatistics(hotelId);
        dashboard.put("statistics", statistics);

        // 质量分析
        Map<String, Object> quality = reviewAnalyticsService.getQualityAnalysis(hotelId);
        dashboard.put("quality", quality);

        // 管理员审核统计
        Map<String, Object> moderationStats = reviewAnalyticsService.getModerationStatistics(getCurrentUserId());
        dashboard.put("moderationStats", moderationStats);

        // 最近7天的趋势数据
        ReviewAnalyticsRequest request = new ReviewAnalyticsRequest();
        request.setHotelId(hotelId);
        request.setGroupBy("day");
        Map<String, Object> trends = reviewAnalyticsService.getReviewTrends(request);
        dashboard.put("trends", trends);

        return ResponseEntity.ok(ApiResponse.success("获取仪表板数据成功", dashboard));
    }
}