package com.hotel.controller.review;

import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.review.admin.*;
import com.hotel.entity.Review;
import com.hotel.entity.ReviewReply;
import com.hotel.entity.ReviewModerationLog;
import com.hotel.repository.ReviewRepository;
import com.hotel.service.ReviewModerationService;
import com.hotel.service.ReviewReplyService;
import com.hotel.service.RateLimitService;
import com.hotel.service.HotelPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "评价管理", description = "管理员评价管理相关接口")
@PreAuthorize("hasRole('ADMIN')")
public class ReviewManagementController extends BaseController {

    private final ReviewRepository reviewRepository;
    private final ReviewModerationService reviewModerationService;
    private final ReviewReplyService reviewReplyService;
    private final RateLimitService rateLimitService;
    private final HotelPermissionService hotelPermissionService;

    @Operation(summary = "审核单个评价")
    @PutMapping("/{id}/moderate")
    public ResponseEntity<ApiResponse<Review>> moderateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewModerationRequest request) {
        Long adminId = getCurrentUserId();

        // 权限检查
        if (!hotelPermissionService.hasPermissionForReview(adminId, id)) {
            return ResponseEntity.status(403).body(ApiResponse.error("没有权限管理该评价"));
        }

        // 频率限制检查
        if (!rateLimitService.checkModerationLimit(adminId)) {
            return ResponseEntity.status(429).body(ApiResponse.error("操作过于频繁，请稍后再试"));
        }

        Review review = reviewModerationService.moderateReview(id, request, adminId);

        // 记录操作
        rateLimitService.recordOperation(adminId, "moderate",
            String.format("reviewId=%d,action=%s", id, request.getAction()));

        return ResponseEntity.ok(ApiResponse.success("评价审核完成", review));
    }

    @Operation(summary = "批量审核评价")
    @PutMapping("/batch-moderate")
    public ResponseEntity<ApiResponse<List<Review>>> batchModerateReviews(
            @Valid @RequestBody BatchModerationRequest request) {
        if (request.getReviewIds().size() > 100) {
            throw new RuntimeException("批量操作一次最多处理100条评价");
        }

        Long adminId = getCurrentUserId();

        // 权限检查 - 过滤有权限的评价
        List<Long> permittedReviewIds = hotelPermissionService.filterReviewIdsByPermission(
            adminId, request.getReviewIds());

        if (permittedReviewIds.isEmpty()) {
            return ResponseEntity.status(403).body(ApiResponse.error("没有权限管理这些评价"));
        }

        if (permittedReviewIds.size() < request.getReviewIds().size()) {
            // 部分评价无权限，只处理有权限的
            BatchModerationRequest filteredRequest = new BatchModerationRequest();
            filteredRequest.setReviewIds(permittedReviewIds);
            filteredRequest.setAction(request.getAction());
            filteredRequest.setReason(request.getReason() + " (部分评价因权限不足被跳过)");
            request = filteredRequest;
        }

        // 频率限制检查
        if (!rateLimitService.checkBatchOperationLimit(adminId)) {
            return ResponseEntity.status(429).body(ApiResponse.error("批量操作过于频繁，请稍后再试"));
        }

        List<Review> reviews = reviewModerationService.batchModerateReviews(request, adminId);

        // 记录操作
        rateLimitService.recordOperation(adminId, "batch_moderate",
            String.format("count=%d,action=%s", request.getReviewIds().size(), request.getAction()));

        return ResponseEntity.ok(ApiResponse.success("批量审核完成", reviews));
    }

    @Operation(summary = "获取待审核评价列表")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Review>>> getPendingReviews() {
        List<Review> reviews = reviewModerationService.getPendingReviews();
        return ResponseEntity.ok(ApiResponse.success("获取待审核评价成功", reviews));
    }

    @Operation(summary = "获取管理评价列表（支持筛选）")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Review>>> getReviewsForManagement(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // 创建分页对象
        Page<Review> pageable = new Page<>(page, size);

        // 调用实际的查询方法
        IPage<Review> reviews = reviewRepository.findReviewsForManagement(
            pageable, status, hotelId, userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("获取管理评价列表成功", reviews));
    }

    @Operation(summary = "创建评价回复")
    @PostMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<ReviewReply>> createReply(
            @PathVariable Long id,
            @Valid @RequestBody ReviewReplyRequest request) {
        Long adminId = getCurrentUserId();

        // 权限检查
        if (!hotelPermissionService.hasPermissionForReview(adminId, id)) {
            return ResponseEntity.status(403).body(ApiResponse.error("没有权限回复该评价"));
        }

        // 频率限制检查
        if (!rateLimitService.checkReplyLimit(adminId)) {
            return ResponseEntity.status(429).body(ApiResponse.error("回复操作过于频繁，请稍后再试"));
        }

        ReviewReply reply = reviewReplyService.createReply(id, request, adminId);

        // 记录操作
        rateLimitService.recordOperation(adminId, "create_reply",
            String.format("reviewId=%d,status=%s", id, request.getStatus()));

        return ResponseEntity.ok(ApiResponse.success("创建回复成功", reply));
    }

    @Operation(summary = "更新评价回复")
    @PutMapping("/{id}/reply/{replyId}")
    public ResponseEntity<ApiResponse<ReviewReply>> updateReply(
            @PathVariable Long id,
            @PathVariable Long replyId,
            @Valid @RequestBody ReviewReplyRequest request) {
        Long adminId = getCurrentUserId();
        ReviewReply reply = reviewReplyService.updateReply(id, replyId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("更新回复成功", reply));
    }

    @Operation(summary = "删除评价回复")
    @DeleteMapping("/{id}/reply/{replyId}")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            @PathVariable Long id,
            @PathVariable Long replyId) {
        Long adminId = getCurrentUserId();
        reviewReplyService.deleteReply(id, replyId, adminId);
        return ResponseEntity.ok(ApiResponse.success("删除回复成功", null));
    }

    @Operation(summary = "获取评价的回复列表")
    @GetMapping("/{id}/replies")
    public ResponseEntity<ApiResponse<List<ReviewReply>>> getReviewReplies(
            @PathVariable Long id) {
        List<ReviewReply> replies = reviewReplyService.getReviewReplies(id);
        return ResponseEntity.ok(ApiResponse.success("获取回复列表成功", replies));
    }

    @Operation(summary = "获取所有回复列表（管理员视图）")
    @GetMapping("/replies")
    public ResponseEntity<ApiResponse<Page<ReviewReply>>> getAllReplies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ReviewReply> pageable = new Page<>(page, size);
        IPage<ReviewReply> replies = reviewReplyService.getAllReplies(
                status, adminId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("获取回复列表成功", replies));
    }

    @Operation(summary = "获取审核日志")
    @GetMapping("/{id}/moderation-logs")
    public ResponseEntity<ApiResponse<List<ReviewModerationLog>>> getModerationLogs(
            @PathVariable Long id) {
        Long adminId = getCurrentUserId();
        Page<ReviewModerationLog> pageable = new Page<>(0, 100);
        IPage<ReviewModerationLog> logs = reviewModerationService.getModerationLogs(
                id, null, null, null, null, pageable);
        return ResponseEntity.ok(ApiResponse.success("获取审核日志成功", logs.getRecords()));
    }

    @Operation(summary = "获取管理员的审核日志")
    @GetMapping("/moderation-logs")
    public ResponseEntity<ApiResponse<Page<ReviewModerationLog>>> getModerationLogsForAdmin(
            @RequestParam(required = false) Long reviewId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long adminId = getCurrentUserId();
        Page<ReviewModerationLog> pageable = new Page<>(page, size);
        IPage<ReviewModerationLog> logs = reviewModerationService.getModerationLogs(
                reviewId, adminId, action, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("获取审核日志成功", logs));
    }
}