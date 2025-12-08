package com.hotel.test.integration.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotel.dto.bookingConflict.*;
import com.hotel.entity.BookingConflict;
import com.hotel.entity.WaitingList;
import com.hotel.repository.BookingConflictRepository;
import com.hotel.repository.WaitingListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BookingConflictController 集成测试
 *
 * @author Test
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingConflictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingConflictRepository bookingConflictRepository;

    @Autowired
    private WaitingListRepository waitingListRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 清理测试数据
        bookingConflictRepository.deleteAll();
        waitingListRepository.deleteAll();
    }

    @Test
    void testDetectConflict_WhenNoConflict_ShouldReturnNoConflict() throws Exception {
        // Given
        DetectConflictRequest request = new DetectConflictRequest();
        request.setRoomId(100L);
        request.setUserId(200L);
        request.setCheckInDate(LocalDateTime.now().plusDays(10));
        request.setCheckOutDate(LocalDateTime.now().plusDays(12));

        // When & Then
        mockMvc.perform(post("/v1/booking-conflicts/detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ID", "200")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasConflict").value(false))
                .andExpect(jsonPath("$.data.message").value("无冲突，可以预订"));
    }

    @Test
    void testDetectConflict_WhenMissingUserIdHeader_ShouldReturnBadRequest() throws Exception {
        // Given
        DetectConflictRequest request = new DetectConflictRequest();
        request.setRoomId(100L);
        request.setUserId(200L);
        request.setCheckInDate(LocalDateTime.now().plusDays(1));
        request.setCheckOutDate(LocalDateTime.now().plusDays(3));

        // When & Then
        mockMvc.perform(post("/v1/booking-conflicts/detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        // 缺少 X-User-ID header
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testJoinWaitingList_WhenValidRequest_ShouldCreateWaitingList() throws Exception {
        // Given
        JoinWaitingListRequest request = new JoinWaitingListRequest();
        request.setRoomId(100L);
        request.setUserId(200L);
        request.setCheckInDate(LocalDateTime.now().plusDays(7));
        request.setCheckOutDate(LocalDateTime.now().plusDays(9));
        request.setGuestCount(2);

        // When & Then
        MvcResult result = mockMvc.perform(post("/v1/booking-conflicts/waiting-list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ID", "200")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roomId").value(100))
                .andExpect(jsonPath("$.data.userId").value(200))
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andReturn();

        // 验证数据库中确实创建了记录
        WaitingList saved = waitingListRepository.findAll().get(0);
        assertNotNull(saved);
        assertEquals(100L, saved.getRoomId());
        assertEquals(200L, saved.getUserId());
        assertEquals(WaitingList.WaitingListStatus.WAITING, saved.getStatus());
    }

    @Test
    void testJoinWaitingList_WhenInvalidGuestCount_ShouldReturnBadRequest() throws Exception {
        // Given
        JoinWaitingListRequest request = new JoinWaitingListRequest();
        request.setRoomId(100L);
        request.setUserId(200L);
        request.setCheckInDate(LocalDateTime.now().plusDays(7));
        request.setCheckOutDate(LocalDateTime.now().plusDays(9));
        request.setGuestCount(0); // 无效的客人数量

        // When & Then
        mockMvc.perform(post("/v1/booking-conflicts/waiting-list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ID", "200")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUserWaitingList_WhenUserHasWaitingLists_ShouldReturnList() throws Exception {
        // Given - 创建测试等待列表数据
        WaitingList waitingList = new WaitingList();
        waitingList.setRoomId(100L);
        waitingList.setUserId(200L);
        waitingList.setRequestedCheckInDate(LocalDateTime.now().plusDays(7));
        waitingList.setRequestedCheckOutDate(LocalDateTime.now().plusDays(9));
        waitingList.setGuestCount(2);
        waitingList.setPriority(WaitingList.getDefaultPriority());
        waitingList.setStatus(WaitingList.WaitingListStatus.WAITING);
        waitingListRepository.save(waitingList);

        // When & Then
        mockMvc.perform(get("/v1/booking-conflicts/waiting-list")
                        .header("X-User-ID", "200")
                        .param("status", "WAITING")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].roomId").value(100))
                .andExpect(jsonPath("$.data.content[0].userId").value(200))
                .andExpect(jsonPath("$.data.content[0].status").value("WAITING"));
    }

    @Test
    void testConfirmWaitingListBooking_WhenValidConfirmation_ShouldUpdateStatus() throws Exception {
        // Given - 创建等待列表记录
        WaitingList waitingList = new WaitingList();
        waitingList.setRoomId(100L);
        waitingList.setUserId(200L);
        waitingList.setRequestedCheckInDate(LocalDateTime.now().plusDays(7));
        waitingList.setRequestedCheckOutDate(LocalDateTime.now().plusDays(9));
        waitingList.setGuestCount(2);
        waitingList.setPriority(WaitingList.getDefaultPriority());
        waitingList.setStatus(WaitingList.WaitingListStatus.NOTIFIED);
        waitingListRepository.save(waitingList);

        ConfirmWaitingListRequest request = new ConfirmWaitingListRequest();
        request.setOrderId(400L);

        // When & Then
        mockMvc.perform(put("/v1/booking-conflicts/waiting-list/{id}/confirm", waitingList.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ID", "200")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));

        // 验证数据库状态更新
        WaitingList updated = waitingListRepository.findById(waitingList.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(WaitingList.WaitingListStatus.CONFIRMED, updated.getStatus());
        assertEquals(400L, updated.getConfirmedOrderId());
    }

    @Test
    void testConfirmWaitingListBooking_WhenNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        ConfirmWaitingListRequest request = new ConfirmWaitingListRequest();
        request.setOrderId(400L);

        // When & Then
        mockMvc.perform(put("/v1/booking-conflicts/waiting-list/{id}/confirm", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ID", "200")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteWaitingList_WhenOwnerRequests_ShouldDelete() throws Exception {
        // Given - 创建等待列表记录
        WaitingList waitingList = new WaitingList();
        waitingList.setRoomId(100L);
        waitingList.setUserId(200L);
        waitingList.setRequestedCheckInDate(LocalDateTime.now().plusDays(7));
        waitingList.setRequestedCheckOutDate(LocalDateTime.now().plusDays(9));
        waitingList.setGuestCount(2);
        waitingList.setPriority(WaitingList.getDefaultPriority());
        waitingList.setStatus(WaitingList.WaitingListStatus.WAITING);
        waitingListRepository.save(waitingList);

        // When & Then
        mockMvc.perform(delete("/v1/booking-conflicts/waiting-list/{id}", waitingList.getId())
                        .header("X-User-ID", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证记录已被逻辑删除
        WaitingList deleted = waitingListRepository.findById(waitingList.getId()).orElse(null);
        assertNotNull(deleted);
        assertEquals(1, deleted.getDeleted()); // MyBatis-Plus 逻辑删除标记
    }

    @Test
    void testGetConflictStatistics_ShouldReturnStatistics() throws Exception {
        // Given - 创建一些测试冲突数据
        BookingConflict conflict = new BookingConflict();
        conflict.setRoomId(100L);
        conflict.setUserId(200L);
        conflict.setRequestedCheckInDate(LocalDateTime.now().minusDays(1));
        conflict.setRequestedCheckOutDate(LocalDateTime.now().plusDays(1));
        conflict.setConflictType(BookingConflict.ConflictType.TIME_OVERLAP);
        conflict.setStatus(BookingConflict.ConflictStatus.DETECTED);
        bookingConflictRepository.save(conflict);

        // When & Then
        mockMvc.perform(get("/v1/booking-conflicts/statistics")
                        .param("startDate", LocalDateTime.now().minusDays(7).toString())
                        .param("endDate", LocalDateTime.now().plusDays(7).toString())
                        .param("roomId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetWaitingListPosition_WhenUserInList_ShouldReturnPosition() throws Exception {
        // Given - 创建多个等待列表记录以测试位置计算
        for (int i = 0; i < 5; i++) {
            WaitingList waitingList = new WaitingList();
            waitingList.setRoomId(100L);
            waitingList.setUserId(300L + i);
            waitingList.setRequestedCheckInDate(LocalDateTime.now().plusDays(7 + i));
            waitingList.setRequestedCheckOutDate(LocalDateTime.now().plusDays(9 + i));
            waitingList.setGuestCount(2);
            waitingList.setPriority(WaitingList.getDefaultPriority());
            waitingList.setStatus(WaitingList.WaitingListStatus.WAITING);
            waitingListRepository.save(waitingList);
        }

        // When & Then
        mockMvc.perform(get("/v1/booking-conflicts/waiting-list/position")
                        .header("X-User-ID", "302")
                        .param("roomId", "100")
                        .param("checkInDate", LocalDateTime.now().plusDays(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(2)); // 第三个用户，位置应该是2（0-based）
    }

    @Test
    void testCleanupExpiredWaitingList_ShouldUpdateExpiredEntries() throws Exception {
        // Given - 创建已过期的等待列表记录
        WaitingList expiredWaitingList = new WaitingList();
        expiredWaitingList.setRoomId(100L);
        expiredWaitingList.setUserId(200L);
        expiredWaitingList.setRequestedCheckInDate(LocalDateTime.now().minusDays(1));
        expiredWaitingList.setRequestedCheckOutDate(LocalDateTime.now().plusDays(1));
        expiredWaitingList.setGuestCount(2);
        expiredWaitingList.setPriority(WaitingList.getDefaultPriority());
        expiredWaitingList.setStatus(WaitingList.WaitingListStatus.NOTIFIED);
        expiredWaitingList.setExpiresAt(LocalDateTime.now().minusHours(1)); // 已过期
        waitingListRepository.save(expiredWaitingList);

        // When & Then
        mockMvc.perform(post("/v1/booking-conflicts/cleanup-expired")
                        .header("X-User-ID", "1")) // 管理员用户
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", containsString("清理完成")));

        // 验证过期记录状态已更新
        WaitingList updated = waitingListRepository.findById(expiredWaitingList.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(WaitingList.WaitingListStatus.EXPIRED, updated.getStatus());
    }

    @Test
    void testRateLimitExceeded_ShouldReturnTooManyRequests() throws Exception {
        // Given
        DetectConflictRequest request = new DetectConflictRequest();
        request.setRoomId(100L);
        request.setUserId(200L);
        request.setCheckInDate(LocalDateTime.now().plusDays(1));
        request.setCheckOutDate(LocalDateTime.now().plusDays(3));

        // When & Then - 快速发送多个请求应触发频率限制
        for (int i = 0; i < 15; i++) { // 超过默认频率限制
            MvcResult result = mockMvc.perform(post("/v1/booking-conflicts/detect")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-ID", "200")
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn();

            if (result.getResponse().getStatus() == 429) {
                break; // 找到频率限制响应
            }
        }

        // 最后一个请求应该被限制
        mockMvc.perform(post("/v1/booking-conflicts/detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ID", "200")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }
}