package com.hotel.service;

import com.hotel.dto.report.*;
import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.entity.User;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.repository.UserRepository;
import com.hotel.service.impl.ReportServiceImpl;
import com.hotel.util.DataMaskingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 报表服务测试类
 *
 * @author Hotel System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("报表服务测试")
class ReportServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private List<Order> testOrders;
    private List<User> testUsers;
    private List<Room> testRooms;
    private List<RoomType> testRoomTypes;

    @BeforeEach
    void setUp() {
        testOrders = createTestOrders();
        testUsers = createTestUsers();
        testRooms = createTestRooms();
        testRoomTypes = createTestRoomTypes();
    }

    @Test
    @DisplayName("生成订单报表 - 成功")
    void generateOrderReport_Success() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(orderRepository.findByCheckInDateBetweenAndRoomTypeIdAndStatus(
            any(), any(), any(), any())).thenReturn(testOrders);

        // When
        OrderReportDTO result = reportService.generateOrderReport(startDate, endDate, null, null);

        // Then
        assertNotNull(result);
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(testOrders.size(), result.getTotalOrders());
        assertTrue(result.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(result.getOrdersByStatus());
        assertNotNull(result.getOrdersByRoomType());
        assertNotNull(result.getOrdersByChannel());

        verify(orderRepository, times(1)).findByCheckInDateBetweenAndRoomTypeIdAndStatus(
            any(), any(), isNull(), isNull());
    }

    @Test
    @DisplayName("生成收入报表 - 成功")
    void generateRevenueReport_Success() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(orderRepository.findByCheckInDateBetweenAndRoomTypeId(
            any(), any(), any())).thenReturn(testOrders);

        // When
        RevenueReportDTO result = reportService.generateRevenueReport(startDate, endDate, null);

        // Then
        assertNotNull(result);
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertTrue(result.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(result.getMonthlyRevenue());
        assertNotNull(result.getRevenueByRoomType());

        verify(orderRepository, times(1)).findByCheckInDateBetweenAndRoomTypeId(
            any(), any(), isNull());
    }

    @Test
    @DisplayName("生成用户报表 - 成功")
    void generateUserReport_Success() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(userRepository.findAll()).thenReturn(testUsers);
        when(orderRepository.findByCreatedAtBetween(any(), any())).thenReturn(testOrders);

        // When
        UserReportDTO result = reportService.generateUserReport(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(testUsers.size(), result.getTotalUsers());
        assertTrue(result.getActiveUsers() > 0);
        assertNotNull(result.getNewUsersByMonth());
        assertNotNull(result.getTopUsersByOrders());
        assertNotNull(result.getTopUsersBySpending());

        verify(userRepository, times(1)).findAll();
        verify(orderRepository, times(1)).findByCreatedAtBetween(any(), any());
    }

    @Test
    @DisplayName("生成房间报表 - 成功")
    void generateRoomReport_Success() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(roomRepository.findAll()).thenReturn(testRooms);
        when(orderRepository.findByCheckInDateBetweenAndRoomTypeId(
            any(), any(), any())).thenReturn(testOrders);

        // When
        RoomReportDTO result = reportService.generateRoomReport(startDate, endDate, null);

        // Then
        assertNotNull(result);
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(testRooms.size(), result.getTotalRooms());
        assertNotNull(result.getOccupancyRate());
        assertNotNull(result.getRoomUtilization());
        assertNotNull(result.getRevenueByRoomType());

        verify(roomRepository, times(1)).findAll();
        verify(orderRepository, times(1)).findByCheckInDateBetweenAndRoomTypeId(
            any(), any(), isNull());
    }

    @Test
    @DisplayName("获取房型名称 - 存在房型")
    void getRoomTypeNameById_RoomTypeExists() {
        // Given
        Long roomTypeId = 1L;
        RoomType roomType = new RoomType();
        roomType.setId(roomTypeId);
        roomType.setName("豪华单间");

        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));

        // When
        String result = reportService.generateRoomReport(
            LocalDate.now(), LocalDate.now(), roomTypeId).getStartDate().toString();

        // Then
        verify(roomTypeRepository, atLeastOnce()).findById(roomTypeId);
    }

    @Test
    @DisplayName("获取房型名称 - 房型不存在")
    void getRoomTypeNameById_RoomTypeNotExists() {
        // Given
        Long roomTypeId = 999L;
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.empty());

        // When
        RoomReportDTO result = reportService.generateRoomReport(
            LocalDate.now(), LocalDate.now(), roomTypeId);

        // Then
        assertNotNull(result);
        verify(roomTypeRepository, atLeastOnce()).findById(roomTypeId);
    }

    @Test
    @DisplayName("导出报表 - 参数验证")
    void exportReport_InvalidParameters() {
        // Given
        ReportExportRequest request = new ReportExportRequest();
        request.setReportType(null);
        request.setExportFormat("EXCEL");
        request.setStartDate(LocalDate.now().minusDays(30));
        request.setEndDate(LocalDate.now());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.exportReport(request);
        });
    }

    @Test
    @DisplayName("数据脱敏功能测试")
    void testDataMasking() {
        // Test phone masking
        assertEquals("138****5678", DataMaskingUtil.maskPhone("13812345678"));

        // Test email masking
        assertEquals("e******@email.com", DataMaskingUtil.maskEmail("example@email.com"));

        // Test name masking
        assertEquals("张*", DataMaskingUtil.maskName("张三"));

        // Test ID card masking
        assertEquals("110101********1234", DataMaskingUtil.maskIdCard("110101199001011234"));

        // Test null values
        assertNull(DataMaskingUtil.maskPhone(null));
        assertNull(DataMaskingUtil.maskEmail(null));
        assertNull(DataMaskingUtil.maskName(null));
    }

    // 辅助方法：创建测试数据
    private List<Order> createTestOrders() {
        List<Order> orders = new ArrayList<>();

        Order order1 = new Order();
        order1.setId(1L);
        order1.setOrderNumber("ORD001");
        order1.setUserId(1L);
        order1.setRoomId(1L);
        order1.setCheckInDate(LocalDate.now().minusDays(10));
        order1.setCheckOutDate(LocalDate.now().minusDays(8));
        order1.setTotalPrice(new BigDecimal("500.00"));
        order1.setStatus("COMPLETED");
        order1.setCreatedAt(LocalDateTime.now().minusDays(11));
        order1.setSpecialRequests("官网预订");

        Order order2 = new Order();
        order2.setId(2L);
        order2.setOrderNumber("ORD002");
        order2.setUserId(2L);
        order2.setRoomId(2L);
        order2.setCheckInDate(LocalDate.now().minusDays(5));
        order2.setCheckOutDate(LocalDate.now().minusDays(3));
        order2.setTotalPrice(new BigDecimal("800.00"));
        order2.setStatus("COMPLETED");
        order2.setCreatedAt(LocalDateTime.now().minusDays(6));
        order2.setSpecialRequests("微信预订");

        orders.add(order1);
        orders.add(order2);

        return orders;
    }

    private List<User> createTestUsers() {
        List<User> users = new ArrayList<>();

        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("张三");
        user1.setEmail("zhangsan@example.com");
        user1.setPhone("13812345678");
        user1.setRole("USER");
        user1.setCreatedAt(LocalDateTime.now().minusDays(90));

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("李四");
        user2.setEmail("lisi@example.com");
        user2.setPhone("13987654321");
        user2.setRole("USER");
        user2.setCreatedAt(LocalDateTime.now().minusDays(60));

        users.add(user1);
        users.add(user2);

        return users;
    }

    private List<Room> createTestRooms() {
        List<Room> rooms = new ArrayList<>();

        Room room1 = new Room();
        room1.setId(1L);
        room1.setRoomNumber("101");
        room1.setRoomTypeId(1L);
        room1.setFloor(1);
        room1.setArea(25);
        room1.setStatus("AVAILABLE");
        room1.setPrice(new BigDecimal("500.00"));

        Room room2 = new Room();
        room2.setId(2L);
        room2.setRoomNumber("102");
        room2.setRoomTypeId(1L);
        room2.setFloor(1);
        room2.setArea(25);
        room2.setStatus("AVAILABLE");
        room2.setPrice(new BigDecimal("500.00"));

        rooms.add(room1);
        rooms.add(room2);

        return rooms;
    }

    private List<RoomType> createTestRoomTypes() {
        List<RoomType> roomTypes = new ArrayList<>();

        RoomType roomType1 = new RoomType();
        roomType1.setId(1L);
        roomType1.setName("标准间");
        roomType1.setDescription("标准双人间");

        roomTypes.add(roomType1);

        return roomTypes;
    }
}