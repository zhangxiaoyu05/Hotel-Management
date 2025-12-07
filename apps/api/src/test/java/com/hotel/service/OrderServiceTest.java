package com.hotel.service;

import com.hotel.dto.order.CreateOrderRequest;
import com.hotel.dto.order.UpdateOrderRequest;
import com.hotel.dto.order.OrderResponse;
import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.entity.Hotel;
import com.hotel.entity.User;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.UserRepository;
import com.hotel.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private Room mockRoom;
    private Hotel mockHotel;
    private User mockUser;
    private CreateOrderRequest validRequest;

    @BeforeEach
    void setUp() {
        mockRoom = new Room();
        mockRoom.setId(1L);
        mockRoom.setName("标准间");
        mockRoom.setPrice(new BigDecimal("298.00"));
        mockRoom.setStatus("AVAILABLE");
        mockRoom.setHotelId(1L);

        mockHotel = new Hotel();
        mockHotel.setId(1L);
        mockHotel.setName("测试酒店");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        validRequest = new CreateOrderRequest();
        validRequest.setRoomId(1L);
        validRequest.setCheckInDate(LocalDate.now().plusDays(1));
        validRequest.setCheckOutDate(LocalDate.now().plusDays(2));
        validRequest.setGuestCount(2);
        validRequest.setGuestName("张三");
        validRequest.setGuestPhone("13800138000");
        validRequest.setGuestEmail("zhang@example.com");
        validRequest.setSpecialRequests("需要无烟房");
    }

    @Test
    void createOrder_Success() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(roomRepository.selectById(1L)).thenReturn(mockRoom);
            when(hotelRepository.selectById(1L)).thenReturn(mockHotel);
            when(userRepository.selectById(1L)).thenReturn(mockUser);
            when(orderRepository.findConflictingOrders(anyLong(), any(), any(), anyString()))
                    .thenReturn(Collections.emptyList());
            when(orderRepository.countTodayOrders(anyString())).thenReturn(0);

            Order mockOrder = new Order();
            mockOrder.setId(1L);
            mockOrder.setOrderNumber("ORD-20241207-00001");
            when(orderRepository.insert(any(Order.class))).thenReturn(1);

            OrderResponse response = orderService.createOrder(validRequest);

            assertNotNull(response);
            assertEquals("ORD-20241207-00001", response.getOrderNumber());
            assertEquals(1L, response.getRoomId());
            assertNotNull(response.getPriceBreakdown());
            assertEquals(Integer.valueOf(1), response.getPriceBreakdown().getNights());

            verify(orderRepository).insert(any(Order.class));
        }
    }

    @Test
    void createOrder_RoomNotAvailable_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            mockRoom.setStatus("OCCUPIED");
            when(roomRepository.selectById(1L)).thenReturn(mockRoom);

            assertThrows(RuntimeException.class, () -> orderService.createOrder(validRequest));
        }
    }

    @Test
    void createOrder_ConflictingDates_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(roomRepository.selectById(1L)).thenReturn(mockRoom);

            Order conflictingOrder = new Order();
            conflictingOrder.setId(1L);
            List<Order> conflictingOrders = Arrays.asList(conflictingOrder);

            when(orderRepository.findConflictingOrders(anyLong(), any(), any(), anyString()))
                    .thenReturn(conflictingOrders);

            assertThrows(RuntimeException.class, () -> orderService.createOrder(validRequest));
        }
    }

    @Test
    void createOrder_InvalidDateRange_ThrowsException() {
        validRequest.setCheckOutDate(LocalDate.now());

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(roomRepository.selectById(1L)).thenReturn(mockRoom);

            assertThrows(RuntimeException.class, () -> orderService.createOrder(validRequest));
        }
    }

    @Test
    void cancelOrder_Success() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            Order order = new Order();
            order.setId(1L);
            order.setUserId(1L);
            order.setStatus("CONFIRMED");
            order.setCheckInDate(LocalDate.now().plusDays(1));

            when(orderRepository.selectById(1L)).thenReturn(order);
            when(orderRepository.updateById(any(Order.class))).thenReturn(1);

            boolean result = orderService.cancelOrder(1L);

            assertTrue(result);
            verify(orderRepository).updateById(any(Order.class));
        }
    }

    @Test
    void cancelOrder_AlreadyCheckedIn_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            Order order = new Order();
            order.setId(1L);
            order.setUserId(1L);
            order.setStatus("CONFIRMED");
            order.setCheckInDate(LocalDate.now());

            when(orderRepository.selectById(1L)).thenReturn(order);

            assertThrows(RuntimeException.class, () -> orderService.cancelOrder(1L));
        }
    }

    @Test
    void calculateDiscount_ValidCoupon_ReturnsDiscount() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(roomRepository.selectById(1L)).thenReturn(mockRoom);
            when(hotelRepository.selectById(1L)).thenReturn(mockHotel);
            when(userRepository.selectById(1L)).thenReturn(mockUser);
            when(orderRepository.findConflictingOrders(anyLong(), any(), any(), anyString()))
                    .thenReturn(Collections.emptyList());
            when(orderRepository.countTodayOrders(anyString())).thenReturn(0);

            validRequest.setCouponCode("WELCOME10");

            Order mockOrder = new Order();
            mockOrder.setId(1L);
            mockOrder.setOrderNumber("ORD-20241207-00001");
            when(orderRepository.insert(any(Order.class))).thenReturn(1);

            OrderResponse response = orderService.createOrder(validRequest);

            assertNotNull(response);
            assertTrue(response.getPriceBreakdown().getDiscountAmount().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    void updateOrder_Success() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            Order existingOrder = new Order();
            existingOrder.setId(1L);
            existingOrder.setUserId(1L);
            existingOrder.setStatus("CONFIRMED");
            existingOrder.setCheckInDate(LocalDate.now().plusDays(2));
            existingOrder.setSpecialRequests("原特殊要求");

            when(orderRepository.selectById(1L)).thenReturn(existingOrder);
            when(orderRepository.updateById(any(Order.class))).thenReturn(1);
            when(roomRepository.selectById(1L)).thenReturn(mockRoom);
            when(hotelRepository.selectById(1L)).thenReturn(mockHotel);

            UpdateOrderRequest updateRequest = new UpdateOrderRequest();
            updateRequest.setSpecialRequests("新的特殊要求");

            OrderResponse response = orderService.updateOrder(1L, updateRequest);

            assertNotNull(response);
            verify(orderRepository).updateById(any(Order.class));
        }
    }

    @Test
    void updateOrder_OrderNotModifiable_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            Order existingOrder = new Order();
            existingOrder.setId(1L);
            existingOrder.setUserId(1L);
            existingOrder.setStatus("COMPLETED"); // 已完成的订单不能修改

            when(orderRepository.selectById(1L)).thenReturn(existingOrder);

            UpdateOrderRequest updateRequest = new UpdateOrderRequest();
            updateRequest.setSpecialRequests("新的特殊要求");

            assertThrows(RuntimeException.class, () -> orderService.updateOrder(1L, updateRequest));
        }
    }

    @Test
    void cancelOrderWithRefund_FullRefund() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            Order order = new Order();
            order.setId(1L);
            order.setUserId(1L);
            order.setStatus("CONFIRMED");
            order.setCheckInDate(LocalDate.now().plusDays(10)); // 提前10天，应该全额退款
            order.setTotalPrice(new BigDecimal("1000.00"));

            when(orderRepository.selectById(1L)).thenReturn(order);
            when(orderRepository.updateById(any(Order.class))).thenReturn(1);
            when(roomRepository.selectById(1L)).thenReturn(mockRoom);
            when(hotelRepository.selectById(1L)).thenReturn(mockHotel);

            OrderResponse response = orderService.cancelOrder(1L, "行程变更");

            assertNotNull(response);
            assertEquals("CANCELLED", response.getStatus());
            assertNotNull(response.getRefundInfo());
            assertEquals(new BigDecimal("1000.00"), response.getRefundInfo().getRefundAmount());
        }
    }

    @Test
    void cancelOrderWithRefund_PartialRefund() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            Order order = new Order();
            order.setId(1L);
            order.setUserId(1L);
            order.setStatus("CONFIRMED");
            order.setCheckInDate(LocalDate.now().plusDays(5)); // 提前5天，应该退款80%
            order.setTotalPrice(new BigDecimal("1000.00"));

            when(orderRepository.selectById(1L)).thenReturn(order);
            when(orderRepository.updateById(any(Order.class))).thenReturn(1);
            when(roomRepository.selectById(1L)).thenReturn(mockRoom);
            when(hotelRepository.selectById(1L)).thenReturn(mockHotel);

            OrderResponse response = orderService.cancelOrder(1L, "行程变更");

            assertNotNull(response);
            assertEquals("CANCELLED", response.getStatus());
            assertNotNull(response.getRefundInfo());
            assertEquals(new BigDecimal("800.00"), response.getRefundInfo().getRefundAmount());
        }
    }

    @Test
    void getOrderList_WithFilters_Success() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            Order order1 = new Order();
            order1.setId(1L);
            order1.setUserId(1L);
            order1.setStatus("CONFIRMED");
            order1.setOrderNumber("ORD-001");

            Order order2 = new Order();
            order2.setId(2L);
            order2.setUserId(1L);
            order2.setStatus("PENDING");
            order2.setOrderNumber("ORD-002");

            List<Order> orders = Arrays.asList(order1, order2);

            when(orderRepository.selectList(any())).thenReturn(orders);
            when(roomRepository.selectById(1L)).thenReturn(mockRoom);
            when(hotelRepository.selectById(1L)).thenReturn(mockHotel);

            var result = orderService.getOrderList("CONFIRMED", 1, 10, "createdAt", "desc", null);

            assertNotNull(result);
            verify(orderRepository).selectList(any());
        }
    }
}