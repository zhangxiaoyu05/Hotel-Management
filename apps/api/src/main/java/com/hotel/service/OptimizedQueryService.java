package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.dto.bookingConflict.WaitingListQueryRequest;
import com.hotel.dto.bookingConflict.WaitingListResponse;
import com.hotel.entity.WaitingList;
import com.hotel.repository.OptimizedWaitingListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优化的查询服务
 * 提供高效的分页查询和数据分析功能
 *
 * @author System
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizedQueryService {

    private final OptimizedWaitingListRepository optimizedWaitingListRepository;

    /**
     * 高效分页查询用户等待列表
     */
    @Transactional(readOnly = true)
    @Cacheable(
        value = "userWaitingListOptimized",
        key = T(com.hotel.util.CacheKeyGenerator).generateUserWaitingListKey(#userId, #request.status, #request.page, #request.size),
        unless = "#result == null || #result.records.isEmpty()"
    )
    public Page<WaitingListResponse> getUserWaitingListOptimized(Long userId, WaitingListQueryRequest request) {
        log.debug("高效查询用户等待列表，用户ID: {}, 状态: {}, 页码: {}, 大小: {}",
                 userId, request.getStatus(), request.getPage(), request.getSize());

        // 计算偏移量
        long offset = (long) (request.getPage() - 1) * request.getSize();

        // 创建分页对象
        Page<WaitingList> page = new Page<>(request.getPage(), request.getSize());

        // 执行优化的分页查询
        IPage<WaitingList> result = optimizedWaitingListRepository.selectWaitingListPageOptimized(
                userId, request.getStatus(), offset, request.getSize(), page);

        // 转换为响应对象
        return result.convert(this::convertToWaitingListResponse);
    }

    /**
     * 获取等待列表总数
     */
    @Transactional(readOnly = true)
    public Long getWaitingListCount(Long userId, String status) {
        return optimizedWaitingListRepository.countWaitingListByUserIdAndStatus(userId, status);
    }

    /**
     * 获取房间等待列表（带数量限制）
     */
    @Transactional(readOnly = true)
    @Cacheable(
        value = "roomWaitingList",
        key = T(com.hotel.util.CacheKeyGenerator).generateRoomWaitingListKey(#roomId, #limit),
        unless = "#result == null || #result.isEmpty()"
    )
    public List<WaitingListResponse> getRoomWaitingListWithLimit(Long roomId, Integer limit) {
        log.debug("查询房间等待列表，房间ID: {}, 限制数量: {}", roomId, limit);

        List<WaitingList> waitingLists = optimizedWaitingListRepository.selectWaitingListByRoomIdWithLimit(roomId, limit);

        return waitingLists.stream()
                .map(this::convertToWaitingListResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取等待列表位置（优化版本）
     */
    @Transactional(readOnly = true)
    @Cacheable(
        value = "waitingListPositionOptimized",
        key = T(com.hotel.util.CacheKeyGenerator).generateWaitingPositionKey(#roomId, #userId, #createdAt),
        unless = "#result == null || #result < 0"
    )
    public Integer getWaitingListPositionOptimized(Long roomId, Long userId, LocalDateTime createdAt) {
        log.debug("查询等待列表位置，房间ID: {}, 用户ID: {}", roomId, userId);

        // 首先获取用户的等待列表项以获取优先级
        WaitingList userWaitingList = optimizedWaitingListRepository.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WaitingList>()
                .eq(WaitingList::getRoomId, roomId)
                .eq(WaitingList::getUserId, userId)
                .eq(WaitingList::getStatus, WaitingList.WaitingListStatus.WAITING)
                .orderByDesc(WaitingList::getCreatedAt)
                .last("LIMIT 1")
        );

        if (userWaitingList == null) {
            return -1; // 不在等待列表中
        }

        return optimizedWaitingListRepository.getWaitingListPositionOptimized(
                roomId, userWaitingList.getPriority(), userWaitingList.getCreatedAt());
    }

    /**
     * 批量处理过期等待列表
     */
    @Transactional
    public int processExpiredWaitingList(Integer batchSize) {
        log.info("开始批量处理过期等待列表，批次大小: {}", batchSize);

        LocalDateTime now = LocalDateTime.now();
        List<WaitingList> expiredLists = optimizedWaitingListRepository.selectExpiredWaitingListBatch(now, batchSize);

        if (expiredLists.isEmpty()) {
            log.info("没有找到过期的等待列表");
            return 0;
        }

        List<Long> expiredIds = expiredLists.stream()
                .map(WaitingList::getId)
                .collect(Collectors.toList());

        // 批量更新状态
        int updatedCount = optimizedWaitingListRepository.batchUpdateWaitingListStatus(
                expiredIds, WaitingList.WaitingListStatus.EXPIRED.name());

        log.info("批量处理过期等待列表完成，更新数量: {}", updatedCount);
        return updatedCount;
    }

    /**
     * 获取等待列表统计数据
     */
    @Transactional(readOnly = true)
    @Cacheable(
        value = "waitingListStats",
        key = T(com.hotel.util.CacheKeyGenerator).generateWaitingListStatsKey(#roomId),
        unless = "#result == null"
    )
    public WaitingListStatistics getWaitingListStatistics(LocalDateTime startDate, LocalDateTime endDate, Long roomId) {
        log.debug("查询等待列表统计数据，开始日期: {}, 结束日期: {}, 房间ID: {}", startDate, endDate, roomId);

        List<OptimizedWaitingListRepository.WaitingListStatusCount> statusCounts =
                optimizedWaitingListRepository.selectWaitingListStatsByDateRange(startDate, endDate, roomId);

        Double avgWaitingTime = optimizedWaitingListRepository.selectAverageWaitingTime(startDate, endDate, roomId);

        OptimizedWaitingListRepository.WaitingListConversionStats conversionStats =
                optimizedWaitingListRepository.selectConversionStats(startDate, endDate, roomId);

        return WaitingListStatistics.builder()
                .startDate(startDate)
                .endDate(endDate)
                .roomId(roomId)
                .statusCounts(statusCounts)
                .averageWaitingHours(avgWaitingTime)
                .totalWaiting(conversionStats.getTotalWaiting())
                .confirmedCount(conversionStats.getConfirmedCount())
                .expiredCount(conversionStats.getExpiredCount())
                .conversionRate(conversionStats.getConversionRate())
                .build();
    }

    /**
     * 获取热门房间（等待列表最多的房间）
     */
    @Transactional(readOnly = true)
    @Cacheable(
        value = "hotRooms",
        key = T(com.hotel.util.CacheKeyGenerator).generateHotRoomsKey(#sinceDate),
        unless = "#result == null || #result.isEmpty()"
    )
    public List<HotRoomStatistics> getHotRooms(LocalDateTime sinceDate, Integer limit) {
        log.debug("查询热门房间统计，起始日期: {}, 限制数量: {}", sinceDate, limit);

        List<OptimizedWaitingListRepository.RoomWaitingCount> roomWaitingCounts =
                optimizedWaitingListRepository.selectHotRooms(sinceDate, limit);

        return roomWaitingCounts.stream()
                .map(this::convertToHotRoomStatistics)
                .collect(Collectors.toList());
    }

    /**
     * 清理过期等待列表
     */
    @Transactional
    public int cleanUpExpiredWaitingList(Integer daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);

        log.info("开始清理过期等待列表，保留天数: {}, 截止日期: {}", daysToKeep, cutoffDate);

        int cleanedCount = optimizedWaitingListRepository.cleanUpExpiredWaitingList(cutoffDate);

        log.info("清理过期等待列表完成，清理数量: {}", cleanedCount);
        return cleanedCount;
    }

    /**
     * 预热缓存
     */
    public void warmUpCache(Long userId) {
        log.debug("预热用户等待列表缓存，用户ID: {}", userId);

        // 预热用户等待列表
        WaitingListQueryRequest request = new WaitingListQueryRequest();
        request.setUserId(userId);
        request.setPage(1);
        request.setSize(10);

        getUserWaitingListOptimized(userId, request);
    }

    // 私有辅助方法

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

    private HotRoomStatistics convertToHotRoomStatistics(OptimizedWaitingListRepository.RoomWaitingCount roomWaitingCount) {
        return HotRoomStatistics.builder()
                .roomId(roomWaitingCount.getRoomId())
                .waitingCount(roomWaitingCount.getWaitingCount())
                .build();
    }

    /**
     * 等待列表统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class WaitingListStatistics {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Long roomId;
        private List<OptimizedWaitingListRepository.WaitingListStatusCount> statusCounts;
        private Double averageWaitingHours;
        private Long totalWaiting;
        private Long confirmedCount;
        private Long expiredCount;
        private Double conversionRate;
    }

    /**
     * 热门房间统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class HotRoomStatistics {
        private Long roomId;
        private Long waitingCount;
    }
}