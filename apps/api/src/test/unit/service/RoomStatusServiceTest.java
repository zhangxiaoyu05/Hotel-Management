package com.hotel.service;

import com.hotel.entity.Room;
import com.hotel.entity.RoomStatusLog;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomStatusLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomStatusServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomStatusLogRepository roomStatusLogRepository;

    @InjectMocks
    private RoomStatusService roomStatusService;

    private Room testRoom;
    private final Long ROOM_ID = 1L;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        testRoom = new Room();
        testRoom.setId(ROOM_ID);
        testRoom.setRoomNumber("101");
        testRoom.setStatus("AVAILABLE");
        testRoom.setVersion(1);
        testRoom.setHotelId(1L);
        testRoom.setRoomTypeId(1L);
    }

    @Test
    void updateRoomStatus_Success_ValidTransition() {
        // Given
        when(roomRepository.selectById(ROOM_ID)).thenReturn(testRoom);
        when(roomRepository.update(any(), any())).thenReturn(1);
        when(roomStatusLogRepository.insert(any(RoomStatusLog.class))).thenReturn(1);

        // When
        boolean result = roomStatusService.updateRoomStatus(
                ROOM_ID,
                "OCCUPIED",
                "预订成功",
                USER_ID,
                null,
                1
        );

        // Then
        assertTrue(result);
        verify(roomRepository).update(any(), any());
        verify(roomStatusLogRepository).insert(any(RoomStatusLog.class));
    }

    @Test
    void updateRoomStatus_Fail_RoomNotFound() {
        // Given
        when(roomRepository.selectById(ROOM_ID)).thenReturn(null);

        // When
        boolean result = roomStatusService.updateRoomStatus(
                ROOM_ID,
                "OCCUPIED",
                "预订成功",
                USER_ID,
                null,
                1
        );

        // Then
        assertFalse(result);
        verify(roomRepository, never()).update(any(), any());
        verify(roomStatusLogRepository, never()).insert(any());
    }

    @Test
    void updateRoomStatus_Fail_InvalidStatusTransition() {
        // Given
        when(roomRepository.selectById(ROOM_ID)).thenReturn(testRoom);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> roomStatusService.updateRoomStatus(
                        ROOM_ID,
                        "MAINTENANCE",
                        "直接设置维护",
                        USER_ID,
                        null,
                        1
                )
        );

        assertTrue(exception.getMessage().contains("Invalid status transition"));
        verify(roomRepository, never()).update(any(), any());
        verify(roomStatusLogRepository, never()).insert(any());
    }

    @Test
    void updateRoomStatus_Fail_OptimisticLockConflict() {
        // Given
        when(roomRepository.selectById(ROOM_ID)).thenReturn(testRoom);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> roomStatusService.updateRoomStatus(
                        ROOM_ID,
                        "OCCUPIED",
                        "预订成功",
                        USER_ID,
                        null,
                        2  // 期望版本号与实际不匹配
                )
        );

        assertTrue(exception.getMessage().contains("modified by another user"));
        verify(roomRepository, never()).update(any(), any());
        verify(roomStatusLogRepository, never()).insert(any());
    }

    @Test
    void isRoomAvailable_True() {
        // Given
        testRoom.setStatus("AVAILABLE");
        when(roomRepository.selectById(ROOM_ID)).thenReturn(testRoom);

        // When
        boolean result = roomStatusService.isRoomAvailable(ROOM_ID);

        // Then
        assertTrue(result);
    }

    @Test
    void isRoomAvailable_False() {
        // Given
        testRoom.setStatus("OCCUPIED");
        when(roomRepository.selectById(ROOM_ID)).thenReturn(testRoom);

        // When
        boolean result = roomStatusService.isRoomAvailable(ROOM_ID);

        // Then
        assertFalse(result);
    }

    @Test
    void isRoomAvailable_RoomNotFound() {
        // Given
        when(roomRepository.selectById(ROOM_ID)).thenReturn(null);

        // When
        boolean result = roomStatusService.isRoomAvailable(ROOM_ID);

        // Then
        assertFalse(result);
    }

    @Test
    void checkRoomsAvailability_MultipleRooms() {
        // Given
        List<Long> roomIds = Arrays.asList(1L, 2L, 3L);

        Room room1 = new Room();
        room1.setId(1L);
        room1.setStatus("AVAILABLE");

        Room room2 = new Room();
        room2.setId(2L);
        room2.setStatus("OCCUPIED");

        Room room3 = new Room();
        room3.setId(3L);
        room3.setStatus("AVAILABLE");

        when(roomRepository.selectBatchIds(roomIds)).thenReturn(Arrays.asList(room1, room2, room3));

        // When
        var result = roomStatusService.checkRoomsAvailability(roomIds);

        // Then
        assertTrue(result.get(1L));
        assertFalse(result.get(2L));
        assertTrue(result.get(3L));
    }

    @Test
    void getRoomStatusHistory_WithTimeRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        RoomStatusLog log1 = new RoomStatusLog();
        log1.setId(1L);
        log1.setRoomId(ROOM_ID);
        log1.setOldStatus("AVAILABLE");
        log1.setNewStatus("OCCUPIED");
        log1.setReason("预订成功");

        List<RoomStatusLog> expectedLogs = Arrays.asList(log1);
        when(roomStatusLogRepository.findByRoomIdAndTimeRange(ROOM_ID, startDate, endDate))
                .thenReturn(expectedLogs);

        // When
        List<RoomStatusLog> result = roomStatusService.getRoomStatusHistory(
                ROOM_ID, startDate, endDate, null
        );

        // Then
        assertEquals(1, result.size());
        assertEquals("AVAILABLE", result.get(0).getOldStatus());
        assertEquals("OCCUPIED", result.get(0).getNewStatus());
        verify(roomStatusLogRepository).findByRoomIdAndTimeRange(ROOM_ID, startDate, endDate);
    }

    @Test
    void getRoomStatusHistory_WithLimit() {
        // Given
        RoomStatusLog log1 = new RoomStatusLog();
        log1.setId(1L);
        log1.setRoomId(ROOM_ID);
        log1.setOldStatus("AVAILABLE");
        log1.setNewStatus("OCCUPIED");

        List<RoomStatusLog> expectedLogs = Arrays.asList(log1);
        when(roomStatusLogRepository.findRecentByRoomId(ROOM_ID, 5))
                .thenReturn(expectedLogs);

        // When
        List<RoomStatusLog> result = roomStatusService.getRoomStatusHistory(
                ROOM_ID, null, null, 5
        );

        // Then
        assertEquals(1, result.size());
        verify(roomStatusLogRepository).findRecentByRoomId(ROOM_ID, 5);
    }
}