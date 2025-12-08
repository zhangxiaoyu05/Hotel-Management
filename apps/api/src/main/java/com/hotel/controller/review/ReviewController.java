package com.hotel.controller.review;

import com.hotel.common.ApiResponse;
import com.hotel.dto.review.ReviewRequest;
import com.hotel.dto.review.ReviewResponse;
import com.hotel.service.ReviewService;
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
}