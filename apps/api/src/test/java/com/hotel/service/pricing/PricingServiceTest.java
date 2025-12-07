package com.hotel.service.pricing;

import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.entity.pricing.PricingRule;
import com.hotel.entity.pricing.SpecialPrice;
import com.hotel.entity.pricing.Holiday;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.repository.pricing.PricingRuleRepository;
import com.hotel.repository.pricing.SpecialPriceRepository;
import com.hotel.repository.pricing.HolidayRepository;
import com.hotel.repository.pricing.PriceHistoryRepository;
import com.hotel.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("价格服务测试")
class PricingServiceTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private SpecialPriceRepository specialPriceRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private PricingService pricingService;

    private Room testRoom;
    private RoomType testRoomType;
    private PricingRule weekendRule;
    private PricingRule holidayRule;
    private SpecialPrice specialPrice;
    private Holiday holiday;

    @BeforeEach
    void setUp() {
        // 设置测试房间类型
        testRoomType = new RoomType();
        testRoomType.setId(1L);
        testRoomType.setHotelId(1L);
        testRoomType.setName("标准间");
        testRoomType.setBasePrice(new BigDecimal("200.00"));

        // 设置测试房间
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setHotelId(1L);
        testRoom.setRoomTypeId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setPrice(new BigDecimal("200.00"));

        // 设置周末价格规则
        weekendRule = new PricingRule();
        weekendRule.setId(1L);
        weekendRule.setHotelId(1L);
        weekendRule.setRoomTypeId(1L);
        weekendRule.setName("周末价格");
        weekendRule.setRuleType("WEEKEND");
        weekendRule.setAdjustmentType("PERCENTAGE");
        weekendRule.setAdjustmentValue(new BigDecimal("20.00"));
        weekendRule.setDaysOfWeekList(Arrays.asList(6, 7)); // 周六、周日
        weekendRule.setIsActive(true);
        weekendRule.setPriority(100);

        // 设置节假日价格规则
        holidayRule = new PricingRule();
        holidayRule.setId(2L);
        holidayRule.setHotelId(1L);
        holidayRule.setName("节假日价格");
        holidayRule.setRuleType("HOLIDAY");
        holidayRule.setAdjustmentType("PERCENTAGE");
        holidayRule.setAdjustmentValue(new BigDecimal("30.00"));
        holidayRule.setIsActive(true);
        holidayRule.setPriority(200);

        // 设置特殊价格
        specialPrice = new SpecialPrice();
        specialPrice.setId(1L);
        specialPrice.setHotelId(1L);
        specialPrice.setRoomTypeId(1L);
        specialPrice.setRoomId(1L);
        specialPrice.setDate(LocalDate.of(2025, 1, 1));
        specialPrice.setPrice(new BigDecimal("299.00"));
        specialPrice.setReason("元旦特惠");

        // 设置节假日
        holiday = new Holiday();
        holiday.setId(1L);
        holiday.setName("2025年元旦");
        holiday.setDate(LocalDate.of(2025, 1, 1));
        holiday.setIsNationalHoliday(true);
    }

    @Test
    @DisplayName("测试基础价格计算 - 无规则应用")
    void testCalculateRoomPrice_BasePrice() {
        // Given
        when(roomRepository.selectById(1L)).thenReturn(testRoom);
        when(roomTypeRepository.selectById(1L)).thenReturn(testRoomType);
        when(specialPriceRepository.findByRoomIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(null);
        when(specialPriceRepository.findByRoomTypeIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(null);
        when(pricingRuleRepository.findApplicableRules(anyLong(), anyLong(), any(LocalDate.class()))).thenReturn(List.of());

        LocalDate testDate = LocalDate.of(2025, 1, 15); // 周三

        // When
        BigDecimal price = pricingService.calculateRoomPrice(1L, testDate);

        // Then
        assertEquals(new BigDecimal("200.00"), price);
    }

    @Test
    @DisplayName("测试周末价格计算")
    void testCalculateRoomPrice_WeekendSurcharge() {
        // Given
        when(roomRepository.selectById(1L)).thenReturn(testRoom);
        when(roomTypeRepository.selectById(1L)).thenReturn(testRoomType);
        when(specialPriceRepository.findByRoomIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(null);
        when(specialPriceRepository.findByRoomTypeIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(null);
        when(pricingRuleRepository.findApplicableRules(anyLong(), anyLong(), any(LocalDate.class()))).thenReturn(List.of(weekendRule));

        LocalDate testDate = LocalDate.of(2025, 1, 18); // 周六

        // When
        BigDecimal price = pricingService.calculateRoomPrice(1L, testDate);

        // Then
        assertEquals(new BigDecimal("240.00"), price); // 200 * 1.2
    }

    @Test
    @DisplayName("测试特殊价格优先级")
    void testCalculateRoomPrice_SpecialPricePriority() {
        // Given
        when(roomRepository.selectById(1L)).thenReturn(testRoom);
        when(roomTypeRepository.selectById(1L)).thenReturn(testRoomType);
        when(specialPriceRepository.findByRoomIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(specialPrice);
        when(pricingRuleRepository.findApplicableRules(anyLong(), anyLong(), any(LocalDate.class()))).thenReturn(List.of(weekendRule, holidayRule));

        LocalDate testDate = LocalDate.of(2025, 1, 1); // 元旦（周三）

        // When
        BigDecimal price = pricingService.calculateRoomPrice(1L, testDate);

        // Then
        assertEquals(new BigDecimal("299.00"), price); // 特殊价格优先，不应用其他规则
    }

    @Test
    @DisplayName("测试多规则叠加应用")
    void testCalculateRoomPrice_MultipleRules() {
        // Given
        PricingRule customRule = new PricingRule();
        customRule.setId(3L);
        customRule.setHotelId(1L);
        customRule.setAdjustmentType("FIXED_AMOUNT");
        customRule.setAdjustmentValue(new BigDecimal("50.00"));
        customRule.setIsActive(true);
        customRule.setPriority(50);

        when(roomRepository.selectById(1L)).thenReturn(testRoom);
        when(roomTypeRepository.selectById(1L)).thenReturn(testRoomType);
        when(specialPriceRepository.findByRoomIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(null);
        when(specialPriceRepository.findByRoomTypeIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(null);
        when(pricingRuleRepository.findApplicableRules(anyLong(), anyLong(), any(LocalDate.class())))
                .thenReturn(List.of(customRule, weekendRule)); // 按优先级排序

        LocalDate testDate = LocalDate.of(2025, 1, 18); // 周六

        // When
        BigDecimal price = pricingService.calculateRoomPrice(1L, testDate);

        // Then
        // 基础价格200 + 固定金额50 = 250
        // 然后应用周末20%上浮: 250 * 1.2 = 300
        assertEquals(new BigDecimal("300.00"), price);
    }

    @Test
    @DisplayName("测试批量价格计算")
    void testCalculateRoomPricesForDateRange() {
        // Given
        when(roomRepository.selectById(1L)).thenReturn(testRoom);
        when(roomTypeRepository.selectById(1L)).thenReturn(testRoomType);
        when(specialPriceRepository.findByRoomIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(null);
        when(specialPriceRepository.findByRoomTypeIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(null);
        when(pricingRuleRepository.findApplicableRules(anyLong(), anyLong(), any(LocalDate.class()))).thenReturn(List.of(weekendRule));

        LocalDate startDate = LocalDate.of(2025, 1, 17); // 周五
        LocalDate endDate = LocalDate.of(2025, 1, 19); // 周日

        // When
        Map<LocalDate, BigDecimal> prices = pricingService.calculateRoomPricesForDateRange(1L, startDate, endDate);

        // Then
        assertEquals(3, prices.size());
        assertEquals(new BigDecimal("200.00"), prices.get(LocalDate.of(2025, 1, 17))); // 周五
        assertEquals(new BigDecimal("240.00"), prices.get(LocalDate.of(2025, 1, 18))); // 周六
        assertEquals(new BigDecimal("240.00"), prices.get(LocalDate.of(2025, 1, 19))); // 周日
    }

    @Test
    @DisplayName("测试房间类型价格计算")
    void testCalculateRoomTypePrice() {
        // Given
        when(roomTypeRepository.selectById(1L)).thenReturn(testRoomType);
        when(specialPriceRepository.findByRoomTypeIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(null);
        when(pricingRuleRepository.findApplicableRules(anyLong(), anyLong(), any(LocalDate.class()))).thenReturn(List.of(weekendRule));

        LocalDate testDate = LocalDate.of(2025, 1, 18); // 周六

        // When
        BigDecimal price = pricingService.calculateRoomTypePrice(1L, 1L, testDate);

        // Then
        assertEquals(new BigDecimal("240.00"), price); // 200 * 1.2
    }

    @Test
    @DisplayName("测试节假日检查")
    void testIsHoliday() {
        // Given
        when(holidayRepository.isHoliday(any(LocalDate.class()))).thenReturn(true);

        LocalDate testDate = LocalDate.of(2025, 1, 1);

        // When
        boolean isHoliday = pricingService.isHoliday(testDate);

        // Then
        assertTrue(isHoliday);
        verify(holidayRepository).isHoliday(testDate);
    }

    @Test
    @DisplayName("测试获取节假日信息")
    void testGetHoliday() {
        // Given
        when(holidayRepository.findByDate(any(LocalDate.class()))).thenReturn(holiday);

        LocalDate testDate = LocalDate.of(2025, 1, 1);

        // When
        Holiday result = pricingService.getHoliday(testDate);

        // Then
        assertNotNull(result);
        assertEquals("2025年元旦", result.getName());
        assertEquals(testDate, result.getDate());
        assertTrue(result.getIsNationalHoliday());
    }

    @Test
    @DisplayName("测试价格变更历史记录")
    void testRecordPriceChange() {
        // Given
        when(roomTypeRepository.selectById(1L)).thenReturn(testRoomType);
        when(priceHistoryRepository.insert(any())).thenReturn(1);

        // When
        pricingService.recordPriceChange(
                1L, // hotelId
                1L, // roomTypeId
                1L, // roomId
                new BigDecimal("200.00"), // oldPrice
                new BigDecimal("250.00"), // newPrice
                "MANUAL", // changeType
                "价格调整", // changeReason
                1L // changedBy
        );

        // Then
        verify(priceHistoryRepository).insert(argThat(history ->
                history.getHotelId().equals(1L) &&
                history.getRoomTypeId().equals(1L) &&
                history.getRoomId().equals(1L) &&
                history.getOldPrice().equals(new BigDecimal("200.00")) &&
                history.getNewPrice().equals(new BigDecimal("250.00")) &&
                history.getChangeType().equals("MANUAL") &&
                history.getChangeReason().equals("价格调整") &&
                history.getChangedBy().equals(1L)
        ));
    }

    @Test
    @DisplayName("测试房间不存在异常")
    void testCalculateRoomPrice_RoomNotFound() {
        // Given
        when(roomRepository.selectById(1L)).thenReturn(null);

        LocalDate testDate = LocalDate.of(2025, 1, 15);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculateRoomPrice(1L, testDate)
        );

        assertEquals("房间不存在: 1", exception.getMessage());
    }

    @Test
    @DisplayName("测试房间类型不存在异常")
    void testCalculateRoomTypePrice_RoomTypeNotFound() {
        // Given
        when(roomTypeRepository.selectById(1L)).thenReturn(null);

        LocalDate testDate = LocalDate.of(2025, 1, 15);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculateRoomTypePrice(1L, 1L, testDate)
        );

        assertEquals("房间类型不存在: 1", exception.getMessage());
    }

    @Test
    @DisplayName("测试基础价格为空异常")
    void testCalculateRoomTypePrice_NoBasePrice() {
        // Given
        testRoomType.setBasePrice(null);
        when(roomTypeRepository.selectById(1L)).thenReturn(testRoomType);
        when(specialPriceRepository.findByRoomTypeIdAndDate(anyLong(), any(LocalDate.class()))).thenReturn(null);
        when(pricingRuleRepository.findApplicableRules(anyLong(), anyLong(), any(LocalDate.class()))).thenReturn(List.of());

        LocalDate testDate = LocalDate.of(2025, 1, 15);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> pricingService.calculateRoomTypePrice(1L, 1L, testDate)
        );

        assertEquals("房间和房间类型都没有设置价格", exception.getMessage());
    }
}