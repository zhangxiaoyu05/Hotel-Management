package com.hotel.controller.order;

import com.hotel.annotation.RateLimit;
import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.order.CreateOrderRequest;
import com.hotel.dto.order.OrderListResponse;
import com.hotel.dto.order.OrderResponse;
import com.hotel.dto.order.UpdateOrderRequest;
import com.hotel.dto.bookingConflict.ConflictDetectionResult;
import com.hotel.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController extends BaseController {

    private final OrderService orderService;

    @PostMapping
    @RateLimit(period = 60, limit = 10, type = RateLimit.LimitType.USER,
              prefix = "order_create", message = "订单创建过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderListResponse>>> getUserOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String search) {
        List<OrderListResponse> response = orderService.getOrderList(status, page, size, sortBy, sortOrder, search);
        return ResponseEntity.ok(success(response));
    }

    @PutMapping("/{id}")
    @RateLimit(period = 60, limit = 30, type = RateLimit.LimitType.USER,
              prefix = "order_update", message = "订单更新操作过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request) {
        OrderResponse response = orderService.updateOrder(id, request);
        return ResponseEntity.ok(success(response));
    }

    @PutMapping("/{id}/cancel")
    @RateLimit(period = 60, limit = 20, type = RateLimit.LimitType.USER,
              prefix = "order_cancel", message = "订单取消操作过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) String cancelReason) {
        OrderResponse response = orderService.cancelOrder(id, cancelReason);
        return ResponseEntity.ok(success(response));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(success(response));
    }

    @GetMapping("/check-conflict")
    @RateLimit(period = 60, limit = 30, type = RateLimit.LimitType.USER,
              prefix = "order_check_conflict", message = "冲突检查操作过于频繁，请稍后再试")
    public ResponseEntity<ApiResponse<ConflictDetectionResult>> checkBookingConflict(
            @RequestParam Long roomId,
            @RequestParam LocalDate checkInDate,
            @RequestParam LocalDate checkOutDate) {

        Long currentUserId = getCurrentUserId();
        ConflictDetectionResult result = orderService.detectBookingConflict(
                roomId, currentUserId, checkInDate, checkOutDate
        );
        return ResponseEntity.ok(success(result));
    }
}