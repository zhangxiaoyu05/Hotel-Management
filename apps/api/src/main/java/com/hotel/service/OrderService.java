package com.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.dto.order.CreateOrderRequest;
import com.hotel.dto.order.OrderListResponse;
import com.hotel.dto.order.OrderResponse;
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

    @Transactional
    public boolean cancelOrder(Long orderId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!order.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权取消此订单");
        }

        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new RuntimeException("只能取消已确认的订单");
        }

        if (order.getCheckInDate().isBefore(LocalDate.now()) ||
            order.getCheckInDate().isEqual(LocalDate.now())) {
            throw new RuntimeException("入住当天或之后不能取消订单");
        }

        order.setStatus("CANCELLED");
        int result = orderRepository.updateById(order);

        if (result > 0) {
            // 发送取消通知
            Room room = roomRepository.selectById(order.getRoomId());
            Hotel hotel = hotelRepository.selectById(room.getHotelId());

            notificationService.sendBookingCancellation(
                order.getUserId(),
                order.getOrderNumber(),
                "用户主动取消"
            );
        }

        return result > 0;
    }

    private Integer calculateNights(LocalDate checkInDate, LocalDate checkOutDate) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    private String generateOrderNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int count = orderRepository.countTodayOrders(today) + 1;
        return String.format("ORD-%s-%05d", today, count);
    }

    }