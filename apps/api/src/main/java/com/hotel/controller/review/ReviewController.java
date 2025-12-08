package com.hotel.controller.review;

import com.hotel.common.ApiResponse;
import com.hotel.dto.review.ReviewRequest;
import com.hotel.dto.review.ReviewResponse;
import com.hotel.dto.review.ReviewQueryRequest;
import com.hotel.dto.review.ReviewListResponse;
import com.hotel.dto.review.ReviewStatisticsResponse;
import com.hotel.service.ReviewService;
import com.hotel.service.ReviewStatisticsService;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.ReviewRepository;
import com.hotel.security.UserPrincipal;
import com.hotel.annotation.RateLimit;
import com.hotel.entity.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "评价管理", description = "评价提交和管理相关API")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewStatisticsService reviewStatisticsService;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    @PostMapping
    @RateLimit(period = 300, limit = 3, type = RateLimit.LimitType.USER, message = "评价提交过于频繁，请5分钟后再试")
    @Operation(summary = "提交评价", description = "用户提交对订单的评价")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ReviewRequest reviewRequest) {

        try {
            ReviewResponse response = reviewService.submitReview(
                userPrincipal.getId(),
                reviewRequest
            );

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (com.hotel.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (com.hotel.exception.BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("评价提交失败，请稍后重试"));
        }
    }

    @GetMapping("/my")
    @RateLimit(period = 60, limit = 30, type = RateLimit.LimitType.USER)
    @Operation(summary = "获取我的评价", description = "获取当前用户提交的所有评价")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            List<ReviewResponse> reviews = reviewService.getUserReviews(userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success(reviews));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取评价列表失败"));
        }
    }

    @GetMapping("/hotel/{hotelId}")
    @RateLimit(period = 60, limit = 50, type = RateLimit.LimitType.IP)
    @Operation(summary = "获取酒店评价", description = "获取指定酒店的评价列表")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getHotelReviews(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId) {

        try {
            List<ReviewResponse> reviews = reviewService.getHotelReviews(hotelId);
            return ResponseEntity.ok(ApiResponse.success(reviews));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取酒店评价失败"));
        }
    }

    @GetMapping("/can-review/{orderId}")
    @RateLimit(period = 60, limit = 20, type = RateLimit.LimitType.USER)
    @Operation(summary = "检查是否可以评价订单", description = "检查当前用户是否可以评价指定订单")
    public ResponseEntity<ApiResponse<Map<String, Object>>> canReviewOrder(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            boolean canReview = reviewService.canReviewOrder(userPrincipal.getId(), orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("canReview", canReview);

            if (!canReview) {
                result.put("reason", getCannotReviewReason(userPrincipal.getId(), orderId));
            }

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("检查评价权限失败"));
        }
    }

    private String getCannotReviewReason(Long userId, Long orderId) {
        try {
            // 检查订单是否存在
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return "订单不存在";
            }

            Order order = orderOpt.get();

            // 检查订单是否属于当前用户
            if (!order.getUserId().equals(userId)) {
                return "无权限评价此订单";
            }

            // 检查订单状态
            if (!"COMPLETED".equals(order.getStatus())) {
                return "只有已完成的订单才能评价";
            }

            // 检查是否已评价
            if (reviewRepository.existsByOrderIdAndUserId(orderId, userId)) {
                return "该订单已经评价过了";
            }

            return "未知原因";
        } catch (Exception e) {
            return "无法确定原因，请稍后重试";
        }
    }

    @GetMapping
    @RateLimit(period = 60, limit = 50, type = RateLimit.LimitType.IP, message = "查询过于频繁，请稍后再试")
    @Operation(summary = "获取评价列表", description = "分页获取评价列表，支持筛选和排序")
    public ResponseEntity<ApiResponse<ReviewListResponse>> getReviews(
            @Parameter(description = "酒店ID") @RequestParam Long hotelId,
            @Parameter(description = "房间ID") @RequestParam(required = false) Long roomId,
            @Parameter(description = "最低评分") @RequestParam(required = false) Integer minRating,
            @Parameter(description = "最高评分") @RequestParam(required = false) Integer maxRating,
            @Parameter(description = "是否包含图片") @RequestParam(required = false) Boolean hasImages,
            @Parameter(description = "排序方式：date或rating") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "排序顺序：asc或desc") @RequestParam(defaultValue = "desc") String sortOrder,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {

        try {
            ReviewQueryRequest queryRequest = new ReviewQueryRequest();
            queryRequest.setHotelId(hotelId);
            queryRequest.setRoomId(roomId);
            queryRequest.setMinRating(minRating);
            queryRequest.setMaxRating(maxRating);
            queryRequest.setHasImages(hasImages);
            queryRequest.setSortBy(sortBy);
            queryRequest.setSortOrder(sortOrder);
            queryRequest.setPage(page);
            queryRequest.setSize(size);

            ReviewListResponse response = reviewService.getReviewsWithFilters(queryRequest);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查询评价列表失败"));
        }
    }

    @PostMapping("/query")
    @RateLimit(period = 60, limit = 100, type = RateLimit.LimitType.IP)
    @Operation(summary = "分页查询评价列表", description = "支持多种筛选条件和排序方式的评价列表查询")
    public ResponseEntity<ApiResponse<ReviewListResponse>> queryReviews(
            @Valid @RequestBody ReviewQueryRequest queryRequest) {

        try {
            ReviewListResponse response = reviewService.getReviewsWithFilters(queryRequest);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查询评价列表失败"));
        }
    }

    @GetMapping("/recent/{hotelId}")
    @RateLimit(period = 60, limit = 50, type = RateLimit.LimitType.IP)
    @Operation(summary = "获取酒店最新评价", description = "获取指定酒店的最新评价列表")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getRecentReviews(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Parameter(description = "返回数量限制，默认5条") @RequestParam(defaultValue = "5") Integer limit) {

        try {
            List<ReviewResponse> reviews = reviewService.getRecentReviewsByHotelId(hotelId, limit);
            return ResponseEntity.ok(ApiResponse.success(reviews));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取最新评价失败"));
        }
    }

    @GetMapping("/with-images/{hotelId}")
    @RateLimit(period = 60, limit = 30, type = RateLimit.LimitType.IP)
    @Operation(summary = "获取带图片的评价", description = "获取指定酒店的带图片评价列表")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsWithImages(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId,
            @Parameter(description = "返回数量限制，默认10条") @RequestParam(defaultValue = "10") Integer limit) {

        try {
            List<ReviewResponse> reviews = reviewService.getReviewsWithImagesByHotelId(hotelId, limit);
            return ResponseEntity.ok(ApiResponse.success(reviews));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取带图片评价失败"));
        }
    }

    @GetMapping("/statistics/{hotelId}")
    @RateLimit(period = 60, limit = 30, type = RateLimit.LimitType.IP)
    @Operation(summary = "获取酒店评价统计", description = "获取指定酒店的详细评价统计信息")
    public ResponseEntity<ApiResponse<ReviewStatisticsResponse>> getHotelStatistics(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId) {

        try {
            ReviewStatisticsResponse statistics = reviewStatisticsService.getHotelStatistics(hotelId);
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取酒店评价统计失败"));
        }
    }

    @PostMapping("/statistics/batch")
    @RateLimit(period = 60, limit = 20, type = RateLimit.LimitType.IP)
    @Operation(summary = "批量获取酒店统计", description = "批量获取多个酒店的简单统计信息")
    public ResponseEntity<ApiResponse<Map<Long, Map<String, Object>>>> getBatchStatistics(
            @RequestBody List<Long> hotelIds) {

        try {
            Map<Long, Map<String, Object>> statistics = new HashMap<>();
            for (Long hotelId : hotelIds) {
                statistics.put(hotelId, reviewStatisticsService.getSimpleStatistics(hotelId));
            }
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取批量统计信息失败"));
        }
    }

    @GetMapping("/statistics/{hotelId}/simple")
    @RateLimit(period = 60, limit = 100, type = RateLimit.LimitType.IP)
    @Operation(summary = "获取简单统计", description = "获取酒店的简单统计信息（用于列表展示）")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSimpleStatistics(
            @Parameter(description = "酒店ID") @PathVariable Long hotelId) {

        try {
            Map<String, Object> statistics = reviewStatisticsService.getSimpleStatistics(hotelId);
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取简单统计信息失败"));
        }
    }
}