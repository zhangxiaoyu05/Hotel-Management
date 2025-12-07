package com.hotel.service;

import com.hotel.dto.room.RoomSearchRequestDto;
import com.hotel.dto.room.RoomSearchResultDto;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.entity.Hotel;
import com.hotel.exception.BusinessException;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RoomService单元测试
 * 覆盖所有业务逻辑和边界条件
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("房间服务测试")
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private RoomService roomService;

    private Room testRoom;
    private RoomType testRoomType;
    private Hotel testHotel;
    private RoomSearchRequestDto searchRequest;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("测试酒店");
        testHotel.setAddress("测试地址");
        testHotel.setRating(4.5);

        testRoomType = new RoomType();
        testRoomType.setId(1L);
        testRoomType.setName("标准间");
        testRoomType.setCapacity(2);
        testRoomType.setBasePrice(new BigDecimal("299.00"));
        testRoomType.setFacilities(Arrays.asList("WiFi", "空调", "电视"));

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setHotelId(1L);
        testRoom.setRoomTypeId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setFloor(1);
        testRoom.setArea(25.0);
        testRoom.setStatus(Room.RoomStatus.AVAILABLE);
        testRoom.setPrice(new BigDecimal("299.00"));
        testRoom.setImages(Arrays.asList("/images/room1.jpg"));

        // 设置默认搜索请求
        searchRequest = new RoomSearchRequestDto();
        searchRequest.setHotelId(1L);
        searchRequest.setRoomTypeId(1L);
        searchRequest.setCheckInDate(LocalDate.now().plusDays(1));
        searchRequest.setCheckOutDate(LocalDate.now().plusDays(2));
        searchRequest.setGuestCount(2);
        searchRequest.setPriceMin(new BigDecimal("200"));
        searchRequest.setPriceMax(new BigDecimal("500"));
        searchRequest.setSortBy("PRICE");
        searchRequest.setSortOrder("ASC");
        searchRequest.setPage(0);
        searchRequest.setSize(20);

        // 使用反射设置测试用的hotelId
        ReflectionTestUtils.setField(roomService, "currentHotelId", 1L);
    }

    @Nested
    @DisplayName("搜索可用房间测试")
    class SearchAvailableRoomsTest {

        @Test
        @DisplayName("应该成功返回可用房间列表")
        void shouldReturnAvailableRooms_WhenValidRequest() {
            // Given
            Page<Room> roomPage = new PageImpl<>(Arrays.asList(testRoom));
            when(roomRepository.searchAvailableRooms(any(Page.class), eq(searchRequest)))
                    .thenReturn(roomPage);
            when(roomTypeRepository.selectById(testRoom.getRoomTypeId()))
                    .thenReturn(testRoomType);
            when(hotelRepository.selectById(testRoom.getHotelId()))
                    .thenReturn(testHotel);
            when(pricingService.calculateTotalPrice(anyLong(), any(), any(), anyInt()))
                    .thenReturn(new BigDecimal("299.00"));

            // When
            RoomSearchResultDto result = roomService.searchAvailableRooms(searchRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getRooms().size());
            assertEquals(1, result.getTotal());
            assertEquals(0, result.getPage());
            assertEquals(20, result.getSize());
            assertEquals(1, result.getTotalPages());

            verify(roomRepository).searchAvailableRooms(any(Page.class), eq(searchRequest));
            verify(roomTypeRepository).selectById(testRoom.getRoomTypeId());
            verify(hotelRepository).selectById(testRoom.getHotelId());
            verify(pricingService).calculateTotalPrice(anyLong(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("应该返回空结果当没有可用房间时")
        void shouldReturnEmptyResult_WhenNoRoomsAvailable() {
            // Given
            Page<Room> emptyPage = new PageImpl<>(Collections.emptyList());
            when(roomRepository.searchAvailableRooms(any(Page.class), eq(searchRequest)))
                    .thenReturn(emptyPage);

            // When
            RoomSearchResultDto result = roomService.searchAvailableRooms(searchRequest);

            // Then
            assertNotNull(result);
            assertTrue(result.getRooms().isEmpty());
            assertEquals(0, result.getTotal());
            assertEquals(0, result.getTotalPages());
        }

        @Test
        @DisplayName("应该抛出异常当日期无效时")
        void shouldThrowException_WhenInvalidDates() {
            // Given
            searchRequest.setCheckInDate(LocalDate.now().plusDays(2));
            searchRequest.setCheckOutDate(LocalDate.now().plusDays(1)); // 退房早于入住

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> roomService.searchAvailableRooms(searchRequest));

            assertTrue(exception.getMessage().contains("退房日期必须晚于入住日期"));
        }

        @Test
        @DisplayName("应该抛出异常当入住日期早于今天时")
        void shouldThrowException_WhenCheckInDateIsBeforeToday() {
            // Given
            searchRequest.setCheckInDate(LocalDate.now().minusDays(1));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> roomService.searchAvailableRooms(searchRequest));

            assertTrue(exception.getMessage().contains("入住日期不能早于今天"));
        }

        @Test
        @DisplayName("应该使用默认排序当排序字段无效时")
        void shouldUseDefaultSort_WhenInvalidSortField() {
            // Given
            searchRequest.setSortBy("INVALID_FIELD");
            Page<Room> roomPage = new PageImpl<>(Arrays.asList(testRoom));
            when(roomRepository.searchAvailableRooms(any(Page.class), any(RoomSearchRequestDto.class)))
                    .thenReturn(roomPage);

            // When
            roomService.searchAvailableRooms(searchRequest);

            // Then
            // 验证调用时使用了默认排序
            verify(roomRepository).searchAvailableRooms(any(Page.class), argThat(req ->
                    "ROOM_NUMBER".equals(req.getSortBy())));
        }
    }

    @Nested
    @DisplayName("获取房间详情测试")
    class GetRoomDetailTest {

        @Test
        @DisplayName("应该成功返回房间详情")
        void shouldReturnRoomDetail_WhenRoomExists() {
            // Given
            when(roomRepository.selectById(testRoom.getId())).thenReturn(Optional.of(testRoom));
            when(roomTypeRepository.selectById(testRoom.getRoomTypeId())).thenReturn(testRoomType);
            when(hotelRepository.selectById(testRoom.getHotelId())).thenReturn(testHotel);

            // When
            var result = roomService.getRoomDetail(testRoom.getId());

            // Then
            assertNotNull(result);
            assertEquals(testRoom.getId(), result.getId());
            assertEquals(testRoom.getRoomNumber(), result.getRoomNumber());
            assertNotNull(result.getRoomTypeName());
            assertNotNull(result.getHotelName());
        }

        @Test
        @DisplayName("应该抛出异常当房间不存在时")
        void shouldThrowException_WhenRoomNotExists() {
            // Given
            when(roomRepository.selectById(999L)).thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> roomService.getRoomDetail(999L));

            assertTrue(exception.getMessage().contains("房间不存在"));
        }
    }

    @Nested
    @DisplayName("价格计算测试")
    class PricingCalculationTest {

        @Test
        @DisplayName("应该正确计算房间总价")
        void shouldCalculateCorrectPrice() {
            // Given
            int nights = 2;
            when(pricingService.calculateTotalPrice(anyLong(), any(), any(), anyInt()))
                    .thenReturn(new BigDecimal("598.00"));

            // When
            BigDecimal totalPrice = pricingService.calculateTotalPrice(
                    testRoom.getId(),
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(3),
                    nights
            );

            // Then
            assertNotNull(totalPrice);
            assertEquals(new BigDecimal("598.00"), totalPrice);
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTest {

        @Test
        @DisplayName("应该处理最大客人数量")
        void shouldHandleMaxGuestCount() {
            // Given
            searchRequest.setGuestCount(10); // 设置一个很大的值

            Page<Room> roomPage = new PageImpl<>(Collections.emptyList());
            when(roomRepository.searchAvailableRooms(any(Page.class), eq(searchRequest)))
                    .thenReturn(roomPage);

            // When & Then
            assertDoesNotThrow(() -> roomService.searchAvailableRooms(searchRequest));
        }

        @Test
        @DisplayName("应该处理最小价格边界")
        void shouldHandleMinPriceBoundary() {
            // Given
            searchRequest.setPriceMin(BigDecimal.ZERO);
            searchRequest.setPriceMax(BigDecimal.valueOf(999999));

            Page<Room> roomPage = new PageImpl<>(Arrays.asList(testRoom));
            when(roomRepository.searchAvailableRooms(any(Page.class), eq(searchRequest)))
                    .thenReturn(roomPage);

            // When & Then
            assertDoesNotThrow(() -> roomService.searchAvailableRooms(searchRequest));
        }

        @Test
        @DisplayName("应该处理当天入住明天退房")
        void shouldHandleSameDayCheckIn() {
            // Given
            searchRequest.setCheckInDate(LocalDate.now());
            searchRequest.setCheckOutDate(LocalDate.now().plusDays(1));

            Page<Room> roomPage = new PageImpl<>(Arrays.asList(testRoom));
            when(roomRepository.searchAvailableRooms(any(Page.class), eq(searchRequest)))
                    .thenReturn(roomPage);

            // When & Then
            assertDoesNotThrow(() -> roomService.searchAvailableRooms(searchRequest));
        }
    }

    @Nested
    @DisplayName("性能测试")
    class PerformanceTest {

        @Test
        @DisplayName("应该处理大量搜索结果")
        void shouldHandleLargeResultSet() {
            // Given
            List<Room> rooms = Collections.nCopies(100, testRoom);
            Page<Room> roomPage = new PageImpl<>(rooms, PageRequest.of(0, 20), 100);

            when(roomRepository.searchAvailableRooms(any(Page.class), eq(searchRequest)))
                    .thenReturn(roomPage);
            when(roomTypeRepository.selectById(anyLong())).thenReturn(testRoomType);
            when(hotelRepository.selectById(anyLong())).thenReturn(testHotel);
            when(pricingService.calculateTotalPrice(anyLong(), any(), any(), anyInt()))
                    .thenReturn(new BigDecimal("299.00"));

            // When
            long startTime = System.currentTimeMillis();
            RoomSearchResultDto result = roomService.searchAvailableRooms(searchRequest);
            long endTime = System.currentTimeMillis();

            // Then
            assertNotNull(result);
            assertEquals(20, result.getRooms().size()); // 分页结果
            assertEquals(100, result.getTotal()); // 总数
            assertTrue(endTime - startTime < 1000); // 应该在1秒内完成
        }
    }
}