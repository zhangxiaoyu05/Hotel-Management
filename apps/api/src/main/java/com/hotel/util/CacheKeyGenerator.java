package com.hotel.util;

import com.hotel.dto.bookingConflict.ConflictStatisticsRequest;
import com.hotel.dto.bookingConflict.WaitingListQueryRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 缓存键生成器
 * 提供统一的缓存键生成策略
 *
 * @author System
 * @since 1.0
 */
public class CacheKeyGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm");

    /**
     * 生成房间可用性缓存键
     */
    public static String generateRoomAvailabilityKey(Long roomId, LocalDateTime checkIn, LocalDateTime checkOut) {
        return String.format("room_availability_%d_%s_%s",
                roomId,
                checkIn.toLocalDate().format(DATE_FORMATTER),
                checkOut.toLocalDate().format(DATE_FORMATTER));
    }

    /**
     * 生成用户等待列表缓存键
     */
    public static String generateUserWaitingListKey(Long userId, String status, Integer page, Integer size) {
        return String.format("user_waiting_list_%d_%s_page_%d_size_%d",
                userId,
                status != null ? status : "all",
                page != null ? page : 1,
                size != null ? size : 10);
    }

    /**
     * 生成冲突统计数据缓存键
     */
    public static String generateConflictStatisticsKey(LocalDateTime startDate, LocalDateTime endDate, Long roomId) {
        return String.format("conflict_stats_%s_%s_%s",
                startDate != null ? startDate.toLocalDate().format(DATE_FORMATTER) : "default",
                endDate != null ? endDate.toLocalDate().format(DATE_FORMATTER) : "default",
                roomId != null ? roomId : "all");
    }

    /**
     * 生成替代房间缓存键
     */
    public static String generateAlternativeRoomsKey(Long roomId, LocalDateTime checkIn, LocalDateTime checkOut, Integer guestCount) {
        return String.format("alternative_rooms_%d_%s_%s_guest_%d",
                roomId,
                checkIn.toLocalDate().format(DATE_FORMATTER),
                checkOut.toLocalDate().format(DATE_FORMATTER),
                guestCount != null ? guestCount : 1);
    }

    /**
     * 生成等待列表位置缓存键
     */
    public static String generateWaitingPositionKey(Long roomId, Long userId, LocalDateTime checkIn) {
        return String.format("waiting_position_room_%d_user_%d_date_%s",
                roomId,
                userId,
                checkIn.toLocalDate().format(DATE_FORMATTER));
    }

    /**
     * 生成等待列表详情缓存键
     */
    public static String generateWaitingListDetailKey(Long waitingListId) {
        return String.format("waiting_list_detail_%d", waitingListId);
    }

    /**
     * 生成冲突检测结果缓存键
     */
    public static String generateConflictDetectionKey(Long roomId, Long userId, LocalDateTime checkIn, LocalDateTime checkOut) {
        return String.format("conflict_detection_room_%d_user_%d_%s_%s",
                roomId,
                userId,
                checkIn.format(DATE_TIME_FORMATTER),
                checkOut.format(DATE_TIME_FORMATTER));
    }

    /**
     * 生成用户权限缓存键
     */
    public static String generateUserPermissionKey(Long userId) {
        return String.format("user_permissions_%d", userId);
    }

    /**
     * 生成房间详情缓存键
     */
    public static String generateRoomDetailKey(Long roomId) {
        return String.format("room_detail_%d", roomId);
    }

    /**
     * 生成热门房间列表缓存键
     */
    public static String generateHotRoomsKey(LocalDateTime date) {
        return String.format("hot_rooms_%s", date.toLocalDate().format(DATE_FORMATTER));
    }

    /**
     * 生成等待列表统计缓存键
     */
    public static String generateWaitingListStatsKey(Long roomId) {
        return String.format("waiting_list_stats_room_%d", roomId);
    }

    /**
     * 生成预订冲突趋势数据缓存键
     */
    public static String generateConflictTrendKey(Integer days) {
        return String.format("conflict_trend_last_%d_days", days);
    }

    /**
     * 生成邮件发送限制缓存键
     */
    public static String generateEmailRateLimitKey(String email) {
        return String.format("email_rate_limit_%s", email.hashCode());
    }

    /**
     * 生成API调用频率限制缓存键
     */
    public static String generateApiRateLimitKey(String apiKey, String endpoint) {
        return String.format("api_rate_limit_%s_%s", apiKey, endpoint);
    }

    /**
     * 生成分布式锁键
     */
    public static String generateDistributedLockKey(String resourceType, Long resourceId) {
        return String.format("distributed_lock_%s_%d", resourceType, resourceId);
    }

    /**
     * 生成缓存版本键（用于缓存版本控制）
     */
    public static String generateCacheVersionKey(String cacheName) {
        return String.format("cache_version_%s", cacheName);
    }

    /**
     * 生成会话缓存键
     */
    public static String generateSessionKey(String sessionId) {
        return String.format("session_%s", sessionId);
    }

    /**
     * 生成验证码缓存键
     */
    public static String generateVerificationCodeKey(String identifier) {
        return String.format("verification_code_%s", identifier);
    }

    /**
     * 生成临时数据缓存键
     */
    public static String generateTempDataKey(String type, String identifier) {
        return String.format("temp_data_%s_%s", type, identifier);
    }

    /**
     * 生成搜索结果缓存键
     */
    public static String generateSearchResultKey(String searchType, String parameters) {
        return String.format("search_result_%s_%s", searchType, parameters.hashCode());
    }

    /**
     * 生成统计报告缓存键
     */
    public static String generateReportKey(String reportType, LocalDateTime date) {
        return String.format("report_%s_%s", reportType, date.toLocalDate().format(DATE_FORMATTER));
    }

    /**
     * 生成配置缓存键
     */
    public static String generateConfigKey(String configType, String key) {
        return String.format("config_%s_%s", configType, key);
    }

    /**
     * 生成监控数据缓存键
     */
    public static String generateMonitoringDataKey(String metricType, LocalDateTime timestamp) {
        return String.format("monitoring_%s_%s", metricType, timestamp.format(DATE_TIME_FORMATTER));
    }
}