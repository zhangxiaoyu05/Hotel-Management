package com.hotel.repository;

import com.hotel.dto.room.RoomSearchRequestDto;
import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.entity.Hotel;
import com.hotel.entity.Order;
import com.hotel.entity.Order.OrderStatus;
import com.hotel.util.OrderStatusUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoomRepository集成测试
 * 测试数据库查询和SQL映射
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("房间数据访问层集成测试")
class RoomRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoomRepository roomRepository;

    private Hotel testHotel;
    private RoomType testRoomType;
    private Room testRoom1;
    private Room testRoom2;
    private RoomSearchRequestDto searchRequest;

    @BeforeEach
    void setUp() {
        // 创建测试酒店
        testHotel = new Hotel();
        testHotel.setName("测试酒店");
        testHotel.setAddress("测试地址");
        testHotel.setPhone("123456789");
        testHotel.setRating(4.5);
        testHotel.setStatus(Hotel.HotelStatus.ACTIVE);
        entityManager.persistAndFlush(testHotel);

        // 创建测试房型
        testRoomType = new RoomType();
        testRoomType.setHotelId(testHotel.getId());
        testRoomType.setName("标准间");
        testRoomType.setCapacity(2);
        testRoomType.setBasePrice(new BigDecimal("299.00"));
        testRoomType.setFacilities(Arrays.asList("WiFi", "空调", "电视"));
        testRoomType.setDescription("舒适的房间");
        entityManager.persistAndFlush(testRoomType);

        // 创建测试房间1 - 可用房间
        testRoom1 = new Room();
        testRoom1.setHotelId(testHotel.getId());
        testRoom1.setRoomTypeId(testRoomType.getId());
        testRoom1.setRoomNumber("101");
        testRoom1.setFloor(1);
        testRoom1.setArea(25.0);
        testRoom1.setStatus(Room.RoomStatus.AVAILABLE);
        testRoom1.setPrice(new BigDecimal("299.00"));
        testRoom1.setImages(Arrays.asList("/images/room101.jpg"));
        entityManager.persistAndFlush(testRoom1);

        // 创建测试房间2 - 另一个可用房间
        testRoom2 = new Room();
        testRoom2.setHotelId(testHotel.getId());
        testRoom2.setRoomTypeId(testRoomType.getId());
        testRoom2.setRoomNumber("102");
        testRoom2.setFloor(1);
        testRoom2.setArea(30.0);
        testRoom2.setStatus(Room.RoomStatus.AVAILABLE);
        testRoom2.setPrice(new BigDecimal("399.00"));
        testRoom2.setImages(Arrays.asList("/images/room102.jpg"));
        entityManager.persistAndFlush(testRoom2);

        // 创建已预订的房间（用于测试可用性）
        Room occupiedRoom = new Room();
        occupiedRoom.setHotelId(testHotel.getId());
        occupiedRoom.setRoomTypeId(testRoomType.getId());
        occupiedRoom.setRoomNumber("103");
        occupiedRoom.setFloor(1);
        occupiedRoom.setArea(25.0);
        occupiedRoom.setStatus(Room.RoomStatus.AVAILABLE);
        occupiedRoom.setPrice(new BigDecimal("299.00"));
        occupiedRoom.setImages(Arrays.asList("/images/room103.jpg"));
        entityManager.persistAndFlush(occupiedRoom);

        // 创建已确认的订单（占用房间）
        Order confirmedOrder = new Order();
        confirmedOrder.setRoomId(occupiedRoom.getId());
        confirmedOrder.setCheckInDate(LocalDate.now().plusDays(1));
        confirmedOrder.setCheckOutDate(LocalDate.now().plusDays(3));
        confirmedOrder.setStatus(OrderStatus.CONFIRMED);
        entityManager.persistAndFlush(confirmedOrder);

        entityManager.clear();

        // 设置默认搜索请求
        searchRequest = new RoomSearchRequestDto();
        searchRequest.setHotelId(testHotel.getId());
        searchRequest.setRoomTypeId(testRoomType.getId());
        searchRequest.setCheckInDate(LocalDate.now().plusDays(5)); // 避开已预订日期
        searchRequest.setCheckOutDate(LocalDate.now().plusDays(6));
        searchRequest.setGuestCount(2);
        searchRequest.setPriceMin(new BigDecimal("200"));
        searchRequest.setPriceMax(new BigDecimal("500"));
        searchRequest.setSortBy("PRICE");
        searchRequest.setSortOrder("ASC");
    }

    @Nested
    @DisplayName("搜索可用房间测试")
    class SearchAvailableRoomsTest {

        @Test
        @DisplayName("应该返回符合条件的可用房间")
        void shouldReturnAvailableRooms_WhenValidSearchRequest() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);

            // When
            Page<Room> result = roomRepository.searchAvailableRooms(pageable, searchRequest);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getContent().size()); // 应该有2个可用房间
            assertEquals(2, result.getTotalElements());

            // 验证房间按价格升序排列
            List<Room> rooms = result.getContent();
            assertTrue(rooms.get(0).getPrice().compareTo(rooms.get(1).getPrice()) <= 0);
        }

        @Test
        @DisplayName("应该排除已被预订的房间")
        void shouldExcludeOccupiedRooms() {
            // Given
            searchRequest.setCheckInDate(LocalDate.now().plusDays(1)); // 与已预订日期重叠
            searchRequest.setCheckOutDate(LocalDate.now().plusDays(2));
            Pageable pageable = PageRequest.of(0, 20);

            // When
            Page<Room> result = roomRepository.searchAvailableRooms(pageable, searchRequest);

            // Then
            assertEquals(2, result.getContent().size()); // 只有2个房间可用，103房间已被预订
        }

        @Test
        @DisplayName("应该按价格范围筛选")
        void shouldFilterByPriceRange() {
            // Given
            searchRequest.setPriceMin(new BigDecimal("350"));
            searchRequest.setPriceMax(new BigDecimal("400"));
            Pageable pageable = PageRequest.of(0, 20);

            // When
            Page<Room> result = roomRepository.searchAvailableRooms(pageable, searchRequest);

            // Then
            assertEquals(1, result.getContent().size());
            assertEquals(new BigDecimal("399.00"), result.getContent().get(0).getPrice());
        }

        @Test
        @DisplayName("应该按房间类型筛选")
        void shouldFilterByRoomType() {
            // Given
            // 创建另一个房型
            RoomType deluxeRoomType = new RoomType();
            deluxeRoomType.setHotelId(testHotel.getId());
            deluxeRoomType.setName("豪华间");
            deluxeRoomType.setCapacity(3);
            deluxeRoomType.setBasePrice(new BigDecimal("599.00"));
            deluxeRoomType.setFacilities(Arrays.asList("WiFi", "空调", "电视", "迷你吧"));
            deluxeRoomType.setDescription("豪华房间");
            entityManager.persistAndFlush(deluxeRoomType);

            Room deluxeRoom = new Room();
            deluxeRoom.setHotelId(testHotel.getId());
            deluxeRoom.setRoomTypeId(deluxeRoomType.getId());
            deluxeRoom.setRoomNumber("201");
            deluxeRoom.setFloor(2);
            deluxeRoom.setArea(35.0);
            deluxeRoom.setStatus(Room.RoomStatus.AVAILABLE);
            deluxeRoom.setPrice(new BigDecimal("599.00"));
            deluxeRoom.setImages(Arrays.asList("/images/room201.jpg"));
            entityManager.persistAndFlush(deluxeRoom);

            entityManager.clear();

            // When - 只搜索标准间
            Pageable pageable = PageRequest.of(0, 20);
            Page<Room> result = roomRepository.searchAvailableRooms(pageable, searchRequest);

            // Then - 应该只返回标准间
            assertEquals(2, result.getContent().size());
            result.getContent().forEach(room -> {
                assertEquals(testRoomType.getId(), room.getRoomTypeId());
            });
        }

        @Test
        @DisplayName("应该按设施筛选")
        void shouldFilterByFacilities() {
            // Given
            searchRequest.setFacilities(Arrays.asList("WiFi", "空调"));
            Pageable pageable = PageRequest.of(0, 20);

            // When
            Page<Room> result = roomRepository.searchAvailableRooms(pageable, searchRequest);

            // Then
            assertEquals(2, result.getContent().size()); // 两个房间都有WiFi和空调
        }

        @Test
        @DisplayName("应该支持分页")
        void shouldSupportPagination() {
            // Given
            Pageable firstPage = PageRequest.of(0, 1);

            // When
            Page<Room> firstPageResult = roomRepository.searchAvailableRooms(firstPage, searchRequest);

            // Then
            assertEquals(1, firstPageResult.getContent().size());
            assertEquals(2, firstPageResult.getTotalElements());
            assertEquals(2, firstPageResult.getTotalPages());

            // Given - 第二页
            Pageable secondPage = PageRequest.of(1, 1);

            // When
            Page<Room> secondPageResult = roomRepository.searchAvailableRooms(secondPage, searchRequest);

            // Then
            assertEquals(1, secondPageResult.getContent().size());
        }

        @Test
        @DisplayName("应该按不同字段排序")
        void shouldSortByDifferentFields() {
            // Given
            searchRequest.setSortBy("room_number");
            searchRequest.setSortOrder("DESC");
            Pageable pageable = PageRequest.of(0, 20);

            // When
            Page<Room> result = roomRepository.searchAvailableRooms(pageable, searchRequest);

            // Then
            assertEquals(2, result.getContent().size());
            // 验证按房间号降序排列
            assertTrue(result.getContent().get(0).getRoomNumber().compareTo(
                    result.getContent().get(1).getRoomNumber()) > 0);
        }

        @Test
        @DisplayName("应该处理空搜索结果")
        void shouldHandleEmptySearchResult() {
            // Given
            searchRequest.setPriceMin(new BigDecimal("1000")); // 设置不可能的价格
            Pageable pageable = PageRequest.of(0, 20);

            // When
            Page<Room> result = roomRepository.searchAvailableRooms(pageable, searchRequest);

            // Then
            assertTrue(result.getContent().isEmpty());
            assertEquals(0, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("获取可用房间总数测试")
    class CountAvailableRoomsTest {

        @Test
        @DisplayName("应该返回正确的可用房间数量")
        void shouldReturnCorrectCount() {
            // When
            Long count = roomRepository.countAvailableRooms(searchRequest);

            // Then
            assertEquals(2L, count);
        }

        @Test
        @DisplayName("应该考虑预订状态")
        void shouldConsiderBookingStatus() {
            // Given
            searchRequest.setCheckInDate(LocalDate.now().plusDays(1)); // 与已预订日期重叠
            searchRequest.setCheckOutDate(LocalDate.now().plusDays(2));

            // When
            Long count = roomRepository.countAvailableRooms(searchRequest);

            // Then
            assertEquals(2L, count); // 只有2个房间可用，103房间已被预订
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTest {

        @Test
        @DisplayName("应该处理无效的排序字段")
        void shouldHandleInvalidSortField() {
            // Given
            searchRequest.setSortBy("INVALID_FIELD");
            Pageable pageable = PageRequest.of(0, 20);

            // When & Then - 不应该抛出异常
            assertDoesNotThrow(() -> {
                Page<Room> result = roomRepository.searchAvailableRooms(pageable, searchRequest);
                assertNotNull(result);
            });
        }

        @Test
        @DisplayName("应该处理空的设施列表")
        void shouldHandleEmptyFacilitiesList() {
            // Given
            searchRequest.setFacilities(Collections.emptyList());
            Pageable pageable = PageRequest.of(0, 20);

            // When
            Page<Room> result = roomRepository.searchAvailableRooms(pageable, searchRequest);

            // Then
            assertEquals(2, result.getContent().size());
        }

        @Test
        @DisplayName("应该处理null的可选参数")
        void shouldHandleNullOptionalParameters() {
            // Given
            searchRequest.setPriceMin(null);
            searchRequest.setPriceMax(null);
            searchRequest.setRoomTypeId(null);
            Pageable pageable = PageRequest.of(0, 20);

            // When
            Page<Room> result = roomRepository.searchAvailableRooms(pageable, searchRequest);

            // Then
            assertEquals(2, result.getContent().size());
        }
    }
}