package com.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.dto.order.CreateOrderRequest;
import com.hotel.dto.order.OrderListResponse;
import com.hotel.dto.order.OrderResponse;
import com.hotel.dto.order.UpdateOrderRequest;
import com.hotel.dto.order.PricingRequest;
import com.hotel.dto.order.PriceBreakdown;
import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.entity.Hotel;
import com.hotel.entity.User;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.UserRepository;
import com.hotel.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BookingPricingService bookingPricingService;
    private final RoomStatusService roomStatusService;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.10");

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Room room = roomRepository.selectById(request.getRoomId());
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        if (!"AVAILABLE".equals(room.getStatus())) {
            throw new RuntimeException("房间当前不可预订");
        }

        List<Order> conflictingOrders = orderRepository.findConflictingOrders(
                request.getRoomId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                "CONFIRMED"
        );

        if (!conflictingOrders.isEmpty()) {
            throw new RuntimeException("选择的时间段已有预订");
        }

        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("入住日期不能早于今天");
        }

        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new RuntimeException("退房日期必须晚于入住日期");
        }

        // 使用价格计算服务
        PricingRequest pricingRequest = new PricingRequest();
        pricingRequest.setRoomId(request.getRoomId());
        pricingRequest.setCheckInDate(request.getCheckInDate().toString());
        pricingRequest.setCheckOutDate(request.getCheckOutDate().toString());
        pricingRequest.setGuestCount(request.getGuestCount());
        pricingRequest.setCouponCode(request.getCouponCode());

        PriceBreakdown priceBreakdown = bookingPricingService.calculatePrice(pricingRequest);

        Integer nights = priceBreakdown.getNights();
        BigDecimal roomFee = priceBreakdown.getRoomFee();
        BigDecimal serviceFee = priceBreakdown.getServiceFee();
        BigDecimal discountAmount = priceBreakdown.getDiscountAmount();
        BigDecimal totalPrice = priceBreakdown.getTotalPrice();

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(currentUserId);
        order.setRoomId(request.getRoomId());
        order.setCheckInDate(request.getCheckInDate());
        order.setCheckOutDate(request.getCheckOutDate());
        order.setGuestCount(request.getGuestCount());
        order.setTotalPrice(totalPrice);
        order.setStatus("CONFIRMED");
        order.setSpecialRequests(request.getSpecialRequests());

        orderRepository.insert(order);

        // 更新房间状态为已预订
        Room currentRoom = roomRepository.selectById(request.getRoomId());
        if (currentRoom != null) {
            try {
                roomStatusService.updateRoomStatus(
                        request.getRoomId(),
                        "OCCUPIED",
                        "订单预订成功，房间已预订",
                        currentUserId,
                        order.getId(),
                        currentRoom.getVersion()
                );
            } catch (Exception e) {
                // 状态更新失败不影响订单创建，但记录错误
                System.err.println("Failed to update room status after booking: " + e.getMessage());
                // 这里可以添加日志记录
            }
        }

        Hotel hotel = hotelRepository.selectById(room.getHotelId());
        User user = userRepository.selectById(currentUserId);

        // 发送预订确认通知
        notificationService.sendBookingConfirmation(
            currentUserId,
            order.getOrderNumber(),
            hotel.getName(),
            room.getName(),
            order.getCheckInDate().toString(),
            order.getCheckOutDate().toString()
        );

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setRoomId(order.getRoomId());
        response.setCheckInDate(order.getCheckInDate());
        response.setCheckOutDate(order.getCheckOutDate());
        response.setGuestCount(order.getGuestCount());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus());
        response.setSpecialRequests(order.getSpecialRequests());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        response.setRoom(room);
        response.setHotel(hotel);

        // 设置价格明细
        OrderResponse.PriceBreakdown responsePriceBreakdown = new OrderResponse.PriceBreakdown();
        responsePriceBreakdown.setRoomFee(priceBreakdown.getRoomFee());
        responsePriceBreakdown.setServiceFee(priceBreakdown.getServiceFee());
        responsePriceBreakdown.setTaxAmount(priceBreakdown.getTaxAmount());
        responsePriceBreakdown.setDiscountAmount(priceBreakdown.getDiscountAmount());
        responsePriceBreakdown.setTotalPrice(priceBreakdown.getTotalPrice());
        responsePriceBreakdown.setNights(priceBreakdown.getNights());
        responsePriceBreakdown.setRoomRate(priceBreakdown.getRoomRate());
        if (priceBreakdown.getCouponCode() != null) {
            responsePriceBreakdown.setCouponCode(priceBreakdown.getCouponCode());
        }
        response.setPriceBreakdown(responsePriceBreakdown);

        return response;
    }

    public OrderResponse getOrderById(Long orderId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!order.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权访问此订单");
        }

        Room room = roomRepository.selectById(order.getRoomId());
        Hotel hotel = hotelRepository.selectById(room.getHotelId());

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setRoomId(order.getRoomId());
        response.setCheckInDate(order.getCheckInDate());
        response.setCheckOutDate(order.getCheckOutDate());
        response.setGuestCount(order.getGuestCount());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus());
        response.setSpecialRequests(order.getSpecialRequests());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        response.setRoom(room);
        response.setHotel(hotel);

        return response;
    }

    public List<OrderListResponse> getUserOrders(String status) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, currentUserId);

        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Order::getStatus, status);
        }

        queryWrapper.orderByDesc(Order::getCreatedAt);

        List<Order> orders = orderRepository.selectList(queryWrapper);

        return orders.stream().map(order -> {
            Room room = roomRepository.selectById(order.getRoomId());
            Hotel hotel = hotelRepository.selectById(room.getHotelId());

            OrderListResponse response = new OrderListResponse();
            response.setId(order.getId());
            response.setOrderNumber(order.getOrderNumber());
            response.setRoomId(order.getRoomId());
            response.setRoomName(room.getName());
            response.setRoomNumber(room.getRoomNumber());
            response.setHotelName(hotel.getName());
            response.setCheckInDate(order.getCheckInDate());
            response.setCheckOutDate(order.getCheckOutDate());
            response.setGuestCount(order.getGuestCount());
            response.setTotalPrice(order.getTotalPrice());
            response.setStatus(order.getStatus());
            response.setCreatedAt(order.getCreatedAt());

            return response;
        }).collect(Collectors.toList());
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        Room room = roomRepository.selectById(order.getRoomId());
        Hotel hotel = hotelRepository.selectById(room.getHotelId());

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setRoomId(order.getRoomId());
        response.setCheckInDate(order.getCheckInDate());
        response.setCheckOutDate(order.getCheckOutDate());
        response.setGuestCount(order.getGuestCount());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus());
        response.setSpecialRequests(order.getSpecialRequests());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        response.setRoom(room);
        response.setHotel(hotel);

        return response;
    }

    
    private Integer calculateNights(LocalDate checkInDate, LocalDate checkOutDate) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    public List<OrderListResponse> getOrderList(String status, Integer page, Integer size,
                                               String sortBy, String sortOrder, String search) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, currentUserId);

        // 状态筛选
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Order::getStatus, status);
        }

        // 搜索功能（订单号或酒店名）
        if (search != null && !search.trim().isEmpty()) {
            // 这里需要关联查询，先按订单号搜索
            queryWrapper.like(Order::getOrderNumber, search.trim());
        }

        // 排序功能
        if (sortBy != null && !sortBy.isEmpty()) {
            boolean isDesc = "desc".equalsIgnoreCase(sortOrder);
            switch (sortBy) {
                case "createdAt":
                    queryWrapper.orderBy(true, isDesc, Order::getCreatedAt);
                    break;
                case "checkInDate":
                    queryWrapper.orderBy(true, isDesc, Order::getCheckInDate);
                    break;
                case "totalPrice":
                    queryWrapper.orderBy(true, isDesc, Order::getTotalPrice);
                    break;
                default:
                    queryWrapper.orderByDesc(Order::getCreatedAt);
            }
        } else {
            queryWrapper.orderByDesc(Order::getCreatedAt);
        }

        // 分页处理
        if (page != null && size != null && page > 0 && size > 0) {
            int offset = (page - 1) * size;
            queryWrapper.last("LIMIT " + offset + ", " + size);
        }

        List<Order> orders = orderRepository.selectList(queryWrapper);

        // 如果有酒店名搜索，需要过滤结果
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.trim().toLowerCase();
            orders = orders.stream().filter(order -> {
                Room room = roomRepository.selectById(order.getRoomId());
                Hotel hotel = hotelRepository.selectById(room.getHotelId());
                return hotel.getName().toLowerCase().contains(searchLower);
            }).collect(Collectors.toList());
        }

        return orders.stream().map(order -> {
            Room room = roomRepository.selectById(order.getRoomId());
            Hotel hotel = hotelRepository.selectById(room.getHotelId());

            OrderListResponse response = new OrderListResponse();
            response.setId(order.getId());
            response.setOrderNumber(order.getOrderNumber());
            response.setRoomId(order.getRoomId());
            response.setRoomName(room.getName());
            response.setRoomNumber(room.getRoomNumber());
            response.setHotelName(hotel.getName());
            response.setCheckInDate(order.getCheckInDate());
            response.setCheckOutDate(order.getCheckOutDate());
            response.setGuestCount(order.getGuestCount());
            response.setTotalPrice(order.getTotalPrice());
            response.setStatus(order.getStatus());
            response.setCreatedAt(order.getCreatedAt());

            return response;
        }).collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!order.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权修改此订单");
        }

        // 验证订单状态 - 只有待确认和已确认的订单可以修改
        if (!"PENDING".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            throw new RuntimeException("只能修改待确认或已确认的订单");
        }

        // 检查是否在入住前可以修改
        if (order.getCheckInDate().isBefore(LocalDate.now().plusDays(1))) {
            throw new RuntimeException("入住前24小时内不允许修改订单");
        }

        // 应用更新
        boolean hasChanges = false;
        if (request.getSpecialRequests() != null) {
            order.setSpecialRequests(request.getSpecialRequests());
            hasChanges = true;
        }

        if (hasChanges) {
            order.setModifiedAt(LocalDateTime.now());
            int result = orderRepository.updateById(order);

            if (result <= 0) {
                throw new RuntimeException("订单更新失败");
            }
        }

        // 返回更新后的订单信息
        return getOrderById(orderId);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String cancelReason) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!order.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权取消此订单");
        }

        // 验证订单状态
        if (!"PENDING".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            throw new RuntimeException("只能取消待确认或已确认的订单");
        }

        // 检查取消时间限制
        if (order.getCheckInDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("入住当天或之后不能取消订单");
        }

        // 计算退款金额
        BigDecimal refundAmount = calculateRefundAmount(order);

        // 更新订单状态
        order.setStatus("CANCELLED");
        order.setCancelReason(cancelReason != null ? cancelReason : "用户取消");
        order.setRefundAmount(refundAmount);
        order.setModifiedAt(LocalDateTime.now());

        int result = orderRepository.updateById(order);
        if (result <= 0) {
            throw new RuntimeException("订单取消失败");
        }

        // 恢复房间状态为可用
        try {
            Room currentRoom = roomRepository.selectById(order.getRoomId());
            if (currentRoom != null) {
                roomStatusService.updateRoomStatus(
                        order.getRoomId(),
                        "AVAILABLE",
                        "订单取消，房间恢复可用状态",
                        currentUserId,
                        order.getId(),
                        currentRoom.getVersion()
                );
            }
        } catch (Exception e) {
            // 状态更新失败不影响订单取消，但记录错误
            System.err.println("Failed to restore room status after cancellation: " + e.getMessage());
            // 这里可以添加日志记录
        }

        // 发送取消通知
        Room room = roomRepository.selectById(order.getRoomId());
        Hotel hotel = hotelRepository.selectById(room.getHotelId());

        notificationService.sendBookingCancellation(
            currentUserId,
            order.getOrderNumber(),
            cancelReason != null ? cancelReason : "用户取消"
        );

        // 返回更新后的订单信息
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setRoomId(order.getRoomId());
        response.setCheckInDate(order.getCheckInDate());
        response.setCheckOutDate(order.getCheckOutDate());
        response.setGuestCount(order.getGuestCount());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus());
        response.setSpecialRequests(order.getSpecialRequests());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        response.setRoom(room);
        response.setHotel(hotel);

        // 设置退款信息
        OrderResponse.RefundInfo refundInfo = new OrderResponse.RefundInfo();
        refundInfo.setRefundAmount(refundAmount);
        refundInfo.setCancelReason(cancelReason != null ? cancelReason : "用户取消");
        response.setRefundInfo(refundInfo);

        return response;
    }

    private BigDecimal calculateRefundAmount(Order order) {
        LocalDate now = LocalDate.now();
        LocalDate checkInDate = order.getCheckInDate();
        long daysUntilCheckIn = java.time.temporal.ChronoUnit.DAYS.between(now, checkInDate);

        BigDecimal totalPrice = order.getTotalPrice();

        if (daysUntilCheckIn >= 7) {
            // 提前7天取消：全额退款
            return totalPrice;
        } else if (daysUntilCheckIn >= 3) {
            // 提前3-7天取消：退款80%
            return totalPrice.multiply(new BigDecimal("0.80"));
        } else if (daysUntilCheckIn >= 1) {
            // 提前1-3天取消：退款50%
            return totalPrice.multiply(new BigDecimal("0.50"));
        } else {
            // 当天取消：不可退款
            return BigDecimal.ZERO;
        }
    }

    private String generateOrderNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int count = orderRepository.countTodayOrders(today) + 1;
        return String.format("ORD-%s-%05d", today, count);
    }

    }