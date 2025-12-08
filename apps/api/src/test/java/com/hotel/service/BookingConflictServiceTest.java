package com.hotel.service;

import com.hotel.dto.bookingConflict.*;
import com.hotel.entity.BookingConflict;
import com.hotel.entity.WaitingList;
import com.hotel.repository.BookingConflictRepository;
import com.hotel.repository.WaitingListRepository;
import com.hotel.service.BookingConflictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BookingConflictService 单元测试
 *
 * @author Test
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class BookingConflictServiceTest {

    @Mock
    private BookingConflictRepository bookingConflictRepository;

    @Mock
    private WaitingListRepository waitingListRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingConflictService bookingConflictService;

    private BookingConflict testConflict;
    private WaitingList testWaitingList;
    private DetectConflictRequest detectRequest;
    private JoinWaitingListRequest joinRequest;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        testConflict = new BookingConflict();
        testConflict.setId(1L);
        testConflict.setRoomId(100L);
        testConflict.setUserId(200L);
        testConflict.setRequestedCheckInDate(LocalDateTime.now().plusDays(1));
        testConflict.setRequestedCheckOutDate(LocalDateTime.now().plusDays(3));
        testConflict.setConflictType(BookingConflict.ConflictType.TIME_OVERLAP);
        testConflict.setStatus(BookingConflict.ConflictStatus.DETECTED);
        testConflict.setCreatedAt(LocalDateTime.now());

        testWaitingList = new WaitingList();
        testWaitingList.setId(1L);
        testWaitingList.setRoomId(100L);
        testWaitingList.setUserId(200L);
        testWaitingList.setRequestedCheckInDate(LocalDateTime.now().plusDays(7));
        testWaitingList.setRequestedCheckOutDate(LocalDateTime.now().plusDays(9));
        testWaitingList.setGuestCount(2);
        testWaitingList.setPriority(WaitingList.getDefaultPriority());
        testWaitingList.setStatus(WaitingList.WaitingListStatus.WAITING);

        detectRequest = new DetectConflictRequest();
        detectRequest.setRoomId(100L);
        detectRequest.setUserId(200L);
        detectRequest.setCheckInDate(LocalDateTime.now().plusDays(1));
        detectRequest.setCheckOutDate(LocalDateTime.now().plusDays(3));

        joinRequest = new JoinWaitingListRequest();
        joinRequest.setRoomId(100L);
        joinRequest.setUserId(200L);
        joinRequest.setCheckInDate(LocalDateTime.now().plusDays(7));
        joinRequest.setCheckOutDate(LocalDateTime.now().plusDays(9));
        joinRequest.setGuestCount(2);
    }

    @Test
    void testDetectConflict_WhenConflictExists_ShouldReturnConflict() {
        // Given
        when(bookingConflictRepository.findConflictingOrders(anyLong(), any(), any()))
                .thenReturn(Arrays.asList(300L, 301L));

        // When
        ConflictDetectionResult result = bookingConflictService.detectConflict(detectRequest);

        // Then
        assertTrue(result.isHasConflict());
        assertEquals(BookingConflict.ConflictType.TIME_OVERLAP.name(), result.getConflictType());
        assertEquals("检测到时间重叠冲突", result.getMessage());
        verify(bookingConflictRepository).findConflictingOrders(anyLong(), any(), any());
        verify(bookingConflictRepository).save(any(BookingConflict.class));
    }

    @Test
    void testDetectConflict_WhenNoConflict_ShouldReturnNoConflict() {
        // Given
        when(bookingConflictRepository.findConflictingOrders(anyLong(), any(), any()))
                .thenReturn(Arrays.asList());

        // When
        ConflictDetectionResult result = bookingConflictService.detectConflict(detectRequest);

        // Then
        assertFalse(result.isHasConflict());
        assertNull(result.getConflictType());
        assertEquals("无冲突，可以预订", result.getMessage());
        verify(bookingConflictRepository).findConflictingOrders(anyLong(), any(), any());
        verify(bookingConflictRepository, never()).save(any());
    }

    @Test
    void testJoinWaitingList_WhenValidRequest_ShouldCreateWaitingList() {
        // Given
        when(waitingListRepository.findByRoomIdAndUserIdAndStatus(
                anyLong(), anyLong(), any()))
                .thenReturn(Optional.empty());
        when(waitingListRepository.save(any(WaitingList.class)))
                .thenReturn(testWaitingList);

        // When
        WaitingListResponse result = bookingConflictService.joinWaitingList(joinRequest);

        // Then
        assertNotNull(result);
        assertEquals(testWaitingList.getId(), result.getId());
        assertEquals(testWaitingList.getRoomId(), result.getRoomId());
        assertEquals(testWaitingList.getUserId(), result.getUserId());
        assertEquals(WaitingList.WaitingListStatus.WAITING.name(), result.getStatus());

        verify(waitingListRepository).findByRoomIdAndUserIdAndStatus(
                anyLong(), anyLong(), any());
        verify(waitingListRepository).save(any(WaitingList.class));
    }

    @Test
    void testJoinWaitingList_WhenAlreadyExists_ShouldReturnExisting() {
        // Given
        when(waitingListRepository.findByRoomIdAndUserIdAndStatus(
                anyLong(), anyLong(), any()))
                .thenReturn(Optional.of(testWaitingList));

        // When
        WaitingListResponse result = bookingConflictService.joinWaitingList(joinRequest);

        // Then
        assertNotNull(result);
        assertEquals(testWaitingList.getId(), result.getId());

        verify(waitingListRepository).findByRoomIdAndUserIdAndStatus(
                anyLong(), anyLong(), any());
        verify(waitingListRepository, never()).save(any());
    }

    @Test
    void testGetUserWaitingList_WhenUserHasWaitingLists_ShouldReturnList() {
        // Given
        List<WaitingList> waitingLists = Arrays.asList(testWaitingList);
        Page<WaitingList> waitingListPage = new PageImpl<>(waitingLists);

        when(waitingListRepository.findByUserIdOrderByPriorityDescCreatedAtAsc(
                anyLong(), any(PageRequest.class)))
                .thenReturn(waitingListPage);

        WaitingListQueryRequest queryRequest = new WaitingListQueryRequest();
        queryRequest.setUserId(200L);
        queryRequest.setStatus(WaitingList.WaitingListStatus.WAITING.name());
        queryRequest.setPage(1);
        queryRequest.setSize(10);

        // When
        Page<WaitingListResponse> result = bookingConflictService.getUserWaitingList(queryRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        WaitingListResponse response = result.getContent().get(0);
        assertEquals(testWaitingList.getId(), response.getId());
        assertEquals(testWaitingList.getRoomId(), response.getRoomId());

        verify(waitingListRepository).findByUserIdOrderByPriorityDescCreatedAtAsc(
                anyLong(), any(PageRequest.class));
    }

    @Test
    void testConfirmWaitingListBooking_WhenValidConfirmation_ShouldUpdateStatus() {
        // Given
        ConfirmWaitingListRequest confirmRequest = new ConfirmWaitingListRequest();
        confirmRequest.setWaitingListId(1L);
        confirmRequest.setOrderId(400L);

        when(waitingListRepository.findById(1L))
                .thenReturn(Optional.of(testWaitingList));
        when(waitingListRepository.save(any(WaitingList.class)))
                .thenReturn(testWaitingList);

        // When
        boolean result = bookingConflictService.confirmWaitingListBooking(confirmRequest);

        // Then
        assertTrue(result);
        assertEquals(WaitingList.WaitingListStatus.CONFIRMED, testWaitingList.getStatus());
        assertEquals(400L, testWaitingList.getConfirmedOrderId());

        verify(waitingListRepository).findById(1L);
        verify(waitingListRepository).save(testWaitingList);
        verify(notificationService).sendWaitingListConfirmationNotification(
                anyLong(), anyLong(), anyLong());
    }

    @Test
    void testConfirmWaitingListBooking_WhenNotFound_ShouldReturnFalse() {
        // Given
        ConfirmWaitingListRequest confirmRequest = new ConfirmWaitingListRequest();
        confirmRequest.setWaitingListId(999L);
        confirmRequest.setOrderId(400L);

        when(waitingListRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When
        boolean result = bookingConflictService.confirmWaitingListBooking(confirmRequest);

        // Then
        assertFalse(result);
        verify(waitingListRepository).findById(999L);
        verify(waitingListRepository, never()).save(any());
        verify(notificationService, never()).sendWaitingListConfirmationNotification(
                anyLong(), anyLong(), anyLong());
    }

    @Test
    void testCleanupExpiredWaitingList_ShouldUpdateExpiredEntries() {
        // Given
        List<WaitingList> expiredLists = Arrays.asList(testWaitingList);

        when(waitingListRepository.findExpiredWaitingLists(any()))
                .thenReturn(expiredLists);
        when(waitingListRepository.saveAll(expiredLists))
                .thenReturn(expiredLists);

        // When
        int cleanedCount = bookingConflictService.cleanupExpiredWaitingList();

        // Then
        assertEquals(1, cleanedCount);
        assertEquals(WaitingList.WaitingListStatus.EXPIRED, testWaitingList.getStatus());

        verify(waitingListRepository).findExpiredWaitingLists(any());
        verify(waitingListRepository).saveAll(expiredLists);
    }

    @Test
    void testGetConflictStatistics_ShouldReturnStatistics() {
        // Given
        ConflictStatisticsRequest statsRequest = new ConflictStatisticsRequest();
        statsRequest.setStartDate(LocalDateTime.now().minusDays(30));
        statsRequest.setEndDate(LocalDateTime.now());
        statsRequest.setRoomId(100L);

        ConflictStatisticsResponse expectedStats = new ConflictStatisticsResponse();
        expectedStats.setTotalConflicts(10L);
        expectedStats.setResolvedConflicts(8L);
        expectedStats.setWaitingListCount(5L);
        expectedStats.setMostConflictedRoomId(100L);
        expectedStats.setMostConflictedRoomNumber("101");

        when(bookingConflictRepository.getConflictStatistics(
                any(), any(), anyLong()))
                .thenReturn(expectedStats);

        // When
        ConflictStatisticsResponse result = bookingConflictService.getConflictStatistics(statsRequest);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getTotalConflicts());
        assertEquals(8L, result.getResolvedConflicts());
        assertEquals(5L, result.getWaitingListCount());
        assertEquals(100L, result.getMostConflictedRoomId());
        assertEquals("101", result.getMostConflictedRoomNumber());

        verify(bookingConflictRepository).getConflictStatistics(
                any(), any(), anyLong());
    }

    @Test
    void testGetWaitingListPosition_WhenUserInList_ShouldReturnPosition() {
        // Given
        when(waitingListRepository.getWaitingListPosition(anyLong(), anyLong(), anyLong()))
                .thenReturn(3);

        // When
        int position = bookingConflictService.getWaitingListPosition(100L, 200L, LocalDateTime.now());

        // Then
        assertEquals(3, position);
        verify(waitingListRepository).getWaitingListPosition(anyLong(), anyLong(), anyLong());
    }

    @Test
    void testGetWaitingListPosition_WhenUserNotInList_ShouldReturnMinusOne() {
        // Given
        when(waitingListRepository.getWaitingListPosition(anyLong(), anyLong(), anyLong()))
                .thenReturn(-1);

        // When
        int position = bookingConflictService.getWaitingListPosition(100L, 200L, LocalDateTime.now());

        // Then
        assertEquals(-1, position);
        verify(waitingListRepository).getWaitingListPosition(anyLong(), anyLong(), anyLong());
    }

    @Test
    void testCalculateEstimatedWaitTime_ShouldReturnReasonableTime() {
        // Given
        int position = 3;
        int avgStayDays = 2;

        // When - 使用反射调用私有方法
        int waitTime = (Integer) ReflectionTestUtils.invokeMethod(
                bookingConflictService, "calculateEstimatedWaitTime", position, avgStayDays);

        // Then
        assertTrue(waitTime > 0);
        assertTrue(waitTime <= position * avgStayDays * 24); // 不应超过理论最大值
    }

    @Test
    void testValidateConflictRequest_WhenInvalidDates_ShouldThrowException() {
        // Given
        DetectConflictRequest invalidRequest = new DetectConflictRequest();
        invalidRequest.setRoomId(100L);
        invalidRequest.setUserId(200L);
        invalidRequest.setCheckInDate(LocalDateTime.now().plusDays(3));
        invalidRequest.setCheckOutDate(LocalDateTime.now().plusDays(1)); // 退房早于入住

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ReflectionTestUtils.invokeMethod(bookingConflictService,
                    "validateConflictRequest", invalidRequest);
        });
    }

    @Test
    void testValidateJoinWaitingListRequest_WhenInvalidGuestCount_ShouldThrowException() {
        // Given
        JoinWaitingListRequest invalidRequest = new JoinWaitingListRequest();
        invalidRequest.setRoomId(100L);
        invalidRequest.setUserId(200L);
        invalidRequest.setCheckInDate(LocalDateTime.now().plusDays(1));
        invalidRequest.setCheckOutDate(LocalDateTime.now().plusDays(3));
        invalidRequest.setGuestCount(0); // 无效的客人数量

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ReflectionTestUtils.invokeMethod(bookingConflictService,
                    "validateJoinWaitingListRequest", invalidRequest);
        });
    }
}