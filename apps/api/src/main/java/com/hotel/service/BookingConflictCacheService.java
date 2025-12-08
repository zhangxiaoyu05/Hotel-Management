package com.hotel.service;

import com.hotel.dto.bookingConflict.*;
import com.hotel.entity.BookingConflict;
import com.hotel.entity.WaitingList;
import com.hotel.repository.BookingConflictRepository;
import com.hotel.repository.WaitingListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 预订冲突缓存服务
 * 提供多级缓存策略，包括本地缓存和分布式缓存
 *
 * @author System
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingConflictCacheService {

    private final BookingConflictRepository bookingConflictRepository;
    private final WaitingListRepository waitingListRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // 缓存键前缀
    private static final String CACHE_PREFIX_ROOM_AVAILABILITY = "room:availability:";
    private static final String CACHE_PREFIX_WAITING_LIST = "waiting:list:";
    private static final String CACHE_PREFIX_CONFLICT_STATISTICS = "conflict:stats:";
    private static final String CACHE_PREFIX_ALTERNATIVE_ROOMS = "alternative:rooms:";

    // 缓存过期时间
    private static final Duration ROOM_AVAILABILITY_TTL = Duration.ofMinutes(5);
    private static final Duration WAITING_LIST_TTL = Duration.ofMinutes(2);
    private static final Duration STATISTICS_TTL = Duration.ofHours(1);
    private static final Duration ALTERNATIVE_ROOMS_TTL = Duration.ofMinutes(10);

    /**
     * 检查房间可用性（带缓存）
     */
    @Cacheable(
        value = "roomAvailability",
        key = T(com.hotel.util.CacheKeyGenerator).generateRoomAvailabilityKey(#roomId, #checkInDate, #checkOutDate),
        unless = "#result == null"
    )
    public Boolean checkRoomAvailability(Long roomId, LocalDateTime checkInDate, LocalDateTime checkOutDate) {
        log.debug("检查房间可用性，房间ID: {}, 日期: {} - {}", roomId, checkInDate, checkOutDate);

        List<Long> conflictingOrders = bookingConflictRepository.findConflictingOrders(
            roomId, checkInDate, checkOutDate
        );

        boolean isAvailable = conflictingOrders.isEmpty();
        log.debug("房间 {} 可用性检查结果: {}", roomId, isAvailable);
        return isAvailable;
    }

    /**
     * 获取用户等待列表（带缓存）
     */
    @Cacheable(
        value = "userWaitingList",
        key = T(com.hotel.util.CacheKeyGenerator).generateUserWaitingListKey(#userId, #request.status, #request.page, #request.size),
        unless = "#result == null || #result.content.isEmpty()"
    )
    public Page<WaitingListResponse> getUserWaitingListCached(Long userId, WaitingListQueryRequest request) {
        log.debug("获取用户等待列表，用户ID: {}, 状态: {}", userId, request.getStatus());

        Page<WaitingList> page = new Page<>(request.getPage(), request.getSize());
        // 使用MyBatis-Plus的分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WaitingList> mybatisPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(request.getPage(), request.getSize());

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WaitingList> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(WaitingList::getUserId, userId);
        if (request.getStatus() != null) {
            wrapper.eq(WaitingList::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(WaitingList::getCreatedAt);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WaitingList> result =
            waitingListRepository.selectPage(mybatisPage, wrapper);

        return result.convert(this::convertToWaitingListResponse);
    }

    /**
     * 获取冲突统计数据（带缓存）
     */
    @Cacheable(
        value = "conflictStatistics",
        key = T(com.hotel.util.CacheKeyGenerator).generateConflictStatisticsKey(#request.startDate, #request.endDate, #request.roomId),
        unless = "#result == null"
    )
    public ConflictStatisticsResponse getConflictStatisticsCached(ConflictStatisticsRequest request) {
        log.debug("获取冲突统计数据，房间ID: {}, 开始日期: {}, 结束日期: {}",
                 request.getRoomId(), request.getStartDate(), request.getEndDate());

        LocalDateTime startDate = request.getStartDate() != null ?
            request.getStartDate() : LocalDateTime.now().minusMonths(1);
        LocalDateTime endDate = request.getEndDate() != null ?
            request.getEndDate() : LocalDateTime.now();

        // 统计各类型冲突数量
        int timeOverlapCount = bookingConflictRepository.countByConflictType(
            "TIME_OVERLAP", startDate, endDate, request.getRoomId()
        );
        int doubleBookingCount = bookingConflictRepository.countByConflictType(
            "DOUBLE_BOOKING", startDate, endDate, request.getRoomId()
        );
        int concurrentRequestCount = bookingConflictRepository.countByConflictType(
            "CONCURRENT_REQUEST", startDate, endDate, request.getRoomId()
        );

        // 统计等待列表数量
        int waitingListCount = waitingListRepository.countByStatusAndDateRange(
            "WAITING", startDate, endDate, request.getRoomId()
        );

        // 统计已解决冲突数量
        int resolvedCount = bookingConflictRepository.countByStatusAndDateRange(
            "RESOLVED", startDate, endDate, request.getRoomId()
        );

        // 获取最冲突的房间
        Long mostConflictedRoomId = bookingConflictRepository.findMostConflictedRoom(startDate, endDate);
        String mostConflictedRoomNumber = mostConflictedRoomId != null ?
            bookingConflictRepository.getRoomNumberById(mostConflictedRoomId) : null;

        return ConflictStatisticsResponse.builder()
            .totalConflicts((long) timeOverlapCount + doubleBookingCount + concurrentRequestCount)
            .resolvedConflicts((long) resolvedCount)
            .waitingListCount((long) waitingListCount)
            .mostConflictedRoomId(mostConflictedRoomId)
            .mostConflictedRoomNumber(mostConflictedRoomNumber)
            .timeOverlapCount((long) timeOverlapCount)
            .doubleBookingCount((long) doubleBookingCount)
            .concurrentRequestCount((long) concurrentRequestCount)
            .build();
    }

    /**
     * 获取替代房间建议（带缓存）
     */
    @Cacheable(
        value = "alternativeRooms",
        key = T(com.hotel.util.CacheKeyGenerator).generateAlternativeRoomsKey(#roomId, #checkInDate, #checkOutDate, #guestCount),
        unless = "#result == null || #result.isEmpty()"
    )
    public List<AlternativeRoomResponse> getAlternativeRoomsCached(Long roomId, LocalDateTime checkInDate,
                                                                 LocalDateTime checkOutDate, Integer guestCount) {
        log.debug("获取替代房间建议，原房间ID: {}, 日期: {} - {}, 客人数量: {}",
                 roomId, checkInDate, checkOutDate, guestCount);

        // 查询同类型或其他类型的可用房间
        List<WaitingList> availableRooms = waitingListRepository.findAlternativeRooms(
            roomId, checkInDate, checkOutDate, guestCount
        );

        return availableRooms.stream()
            .map(this::convertToAlternativeRoomResponse)
            .toList();
    }

    /**
     * 获取等待列表位置（带缓存）
     */
    @Cacheable(
        value = "waitingListPosition",
        key = T(com.hotel.util.CacheKeyGenerator).generateWaitingPositionKey(#roomId, #userId, #checkInDate),
        unless = "#result == null || #result < 0"
    )
    public Integer getWaitingListPositionCached(Long roomId, Long userId, LocalDateTime checkInDate) {
        log.debug("获取等待列表位置，房间ID: {}, 用户ID: {}, 日期: {}", roomId, userId, checkInDate);

        return waitingListRepository.getWaitingListPosition(roomId, userId, checkInDate);
    }

    /**
     * 预热热点房间可用性缓存
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void warmUpRoomAvailabilityCache() {
        log.debug("开始预热房间可用性缓存");

        try {
            // 获取热点房间ID列表（最近7天内查询频率最高的房间）
            List<Long> hotRoomIds = bookingConflictRepository.findHotRoomIds(LocalDateTime.now().minusDays(7));

            for (Long roomId : hotRoomIds) {
                // 预热未来7天的可用性
                LocalDateTime baseDate = LocalDateTime.now();
                for (int i = 0; i < 7; i++) {
                    LocalDateTime checkIn = baseDate.plusDays(i);
                    LocalDateTime checkOut = checkIn.plusDays(1);

                    String cacheKey = generateRoomAvailabilityCacheKey(roomId, checkIn, checkOut);

                    // 如果缓存中不存在，则查询并缓存
                    if (!redisTemplate.hasKey(cacheKey)) {
                        Boolean availability = checkRoomAvailability(roomId, checkIn, checkOut);
                        if (availability != null) {
                            redisTemplate.opsForValue().set(cacheKey, availability, ROOM_AVAILABILITY_TTL);
                        }
                    }
                }
            }

            log.debug("房间可用性缓存预热完成，处理房间数量: {}", hotRoomIds.size());
        } catch (Exception e) {
            log.error("预热房间可用性缓存失败", e);
        }
    }

    /**
     * 清理过期缓存
     */
    @Scheduled(fixedRate = 600000) // 每10分钟执行一次
    public void cleanExpiredCache() {
        log.debug("开始清理过期缓存");

        try {
            // 清理过期的等待列表位置缓存
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(1);
            waitingListRepository.deleteExpiredCacheEntries(cutoffTime);

            log.debug("过期缓存清理完成");
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        }
    }

    /**
     * 失效相关缓存
     */
    @Caching(evict = {
        @CacheEvict(value = "roomAvailability", allEntries = true),
        @CacheEvict(value = "userWaitingList", allEntries = true),
        @CacheEvict(value = "conflictStatistics", allEntries = true)
    })
    public void evictRelatedCache(Long roomId, Long userId) {
        log.debug("清理相关缓存，房间ID: {}, 用户ID: {}", roomId, userId);

        // 清理特定房间的可用性缓存
        evictRoomAvailabilityCache(roomId);

        // 清理特定用户的等待列表缓存
        evictUserWaitingListCache(userId);
    }

    /**
     * 预加载用户等待列表
     */
    public void preloadUserWaitingList(Long userId) {
        log.debug("预加载用户等待列表，用户ID: {}", userId);

        try {
            WaitingListQueryRequest request = new WaitingListQueryRequest();
            request.setUserId(userId);
            request.setPage(1);
            request.setSize(10);

            getUserWaitingListCached(userId, request);
        } catch (Exception e) {
            log.error("预加载用户等待列表失败，用户ID: {}", userId, e);
        }
    }

    /**
     * 批量预加载多个用户的等待列表
     */
    public void batchPreloadWaitingLists(List<Long> userIds) {
        log.debug("批量预加载等待列表，用户数量: {}", userIds.size());

        userIds.parallelStream().forEach(this::preloadUserWaitingList);
    }

    /**
     * 获取缓存命中率统计
     */
    public CacheHitRate getCacheHitRateStats() {
        // 这里可以集成缓存监控工具，如Caffeine的统计功能
        return CacheHitRate.builder()
            .roomAvailabilityHitRate(0.85) // 示例数据
            .waitingListHitRate(0.78)
            .statisticsHitRate(0.92)
            .alternativeRoomsHitRate(0.81)
            .build();
    }

    // 私有辅助方法

    private String generateRoomAvailabilityCacheKey(Long roomId, LocalDateTime checkIn, LocalDateTime checkOut) {
        return CACHE_PREFIX_ROOM_AVAILABILITY + roomId + ":" +
               checkIn.toLocalDate() + ":" + checkOut.toLocalDate();
    }

    private void evictRoomAvailabilityCache(Long roomId) {
        String pattern = CACHE_PREFIX_ROOM_AVAILABILITY + roomId + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }

    private void evictUserWaitingListCache(Long userId) {
        String pattern = CACHE_PREFIX_WAITING_LIST + "user:" + userId + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }

    private WaitingListResponse convertToWaitingListResponse(WaitingList waitingList) {
        return WaitingListResponse.builder()
            .id(waitingList.getId())
            .roomId(waitingList.getRoomId())
            .userId(waitingList.getUserId())
            .requestedCheckInDate(waitingList.getRequestedCheckInDate())
            .requestedCheckOutDate(waitingList.getRequestedCheckOutDate())
            .guestCount(waitingList.getGuestCount())
            .priority(waitingList.getPriority())
            .status(waitingList.getStatus().name())
            .notifiedAt(waitingList.getNotifiedAt())
            .expiresAt(waitingList.getExpiresAt())
            .confirmedOrderId(waitingList.getConfirmedOrderId())
            .createdAt(waitingList.getCreatedAt())
            .build();
    }

    private AlternativeRoomResponse convertToAlternativeRoomResponse(WaitingList waitingList) {
        return AlternativeRoomResponse.builder()
            .roomId(waitingList.getRoomId())
            .roomNumber(waitingList.getRoomNumber())
            .roomType(waitingList.getRoomType())
            .price(waitingList.getPrice())
            .available(true)
            .build();
    }

    /**
     * 缓存命中率统计
     */
    @lombok.Builder
    @lombok.Data
    public static class CacheHitRate {
        private double roomAvailabilityHitRate;
        private double waitingListHitRate;
        private double statisticsHitRate;
        private double alternativeRoomsHitRate;
    }
}