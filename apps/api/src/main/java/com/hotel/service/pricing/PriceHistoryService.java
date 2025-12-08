package com.hotel.service.pricing;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.pricing.PriceHistory;
import com.hotel.enums.PriceChangeType;
import com.hotel.repository.pricing.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格历史业务逻辑层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;

    /**
     * 获取指定酒店的价格历史记录
     *
     * @param hotelId 酒店ID
     * @param limit 限制数量
     * @return 价格历史记录列表
     */
    @Transactional(readOnly = true)
    public List<PriceHistory> getPriceHistoryByHotelId(Long hotelId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 100; // 默认限制100条
        }
        return priceHistoryRepository.findByHotelId(hotelId, limit);
    }

    /**
     * 获取指定房间的价格历史记录
     *
     * @param roomId 房间ID
     * @param limit 限制数量
     * @return 价格历史记录列表
     */
    @Transactional(readOnly = true)
    public List<PriceHistory> getPriceHistoryByRoomId(Long roomId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 100; // 默认限制100条
        }
        return priceHistoryRepository.findByRoomId(roomId, limit);
    }

    /**
     * 获取指定房间类型的价格历史记录
     *
     * @param roomTypeId 房间类型ID
     * @param limit 限制数量
     * @return 价格历史记录列表
     */
    @Transactional(readOnly = true)
    public List<PriceHistory> getPriceHistoryByRoomTypeId(Long roomTypeId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 100; // 默认限制100条
        }
        return priceHistoryRepository.findByRoomTypeId(roomTypeId, limit);
    }

    /**
     * 获取指定时间范围内的价格历史记录
     *
     * @param hotelId 酒店ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 价格历史记录列表
     */
    @Transactional(readOnly = true)
    public List<PriceHistory> getPriceHistoryByTimeRange(Long hotelId, LocalDateTime startTime, LocalDateTime endTime) {
        return priceHistoryRepository.findByHotelIdAndTimeRange(hotelId, startTime, endTime);
    }

    /**
     * 获取指定变更类型的价格历史记录
     *
     * @param hotelId 酒店ID
     * @param changeType 变更类型
     * @param limit 限制数量
     * @return 价格历史记录列表
     */
    @Transactional(readOnly = true)
    public List<PriceHistory> getPriceHistoryByChangeType(Long hotelId, PriceChangeType changeType, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 100; // 默认限制100条
        }
        return priceHistoryRepository.findByHotelIdAndChangeType(hotelId, changeType.name(), limit);
    }

    /**
     * 分页查询价格历史记录
     *
     * @param hotelId 酒店ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param changeType 变更类型过滤
     * @param roomTypeId 房间类型过滤
     * @param startTime 开始时间过滤
     * @param endTime 结束时间过滤
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public Page<PriceHistory> getPriceHistoryWithPagination(Long hotelId, Integer pageNum, Integer pageSize,
                                                           PriceChangeType changeType, Long roomTypeId,
                                                           LocalDateTime startTime, LocalDateTime endTime) {
        Page<PriceHistory> page = new Page<>(pageNum, pageSize);
        QueryWrapper<PriceHistory> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("hotel_id", hotelId);

        if (changeType != null) {
            queryWrapper.eq("change_type", changeType.name());
        }

        if (roomTypeId != null) {
            queryWrapper.eq("room_type_id", roomTypeId);
        }

        if (startTime != null) {
            queryWrapper.ge("created_at", startTime);
        }

        if (endTime != null) {
            queryWrapper.le("created_at", endTime);
        }

        queryWrapper.orderByDesc("created_at");

        return priceHistoryRepository.selectPage(page, queryWrapper);
    }

    /**
     * 获取价格变更统计信息
     *
     * @param hotelId 酒店ID
     * @param days 统计天数
     * @return 统计信息Map
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getPriceChangeStatistics(Long hotelId, Integer days) {
        if (days == null || days <= 0) {
            days = 30; // 默认统计30天
        }

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);

        List<PriceHistory> historyList = priceHistoryRepository.findByHotelIdAndTimeRange(hotelId, startTime, endTime);

        // 统计各种变更类型
        java.util.Map<String, Integer> changeTypeCount = new java.util.HashMap<>();
        java.util.Map<String, java.math.BigDecimal> changeTypeAmount = new java.util.HashMap<>();

        int totalChanges = 0;
        int increaseCount = 0;
        int decreaseCount = 0;
        java.math.BigDecimal totalIncrease = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalDecrease = java.math.BigDecimal.ZERO;

        for (PriceHistory history : historyList) {
            String changeType = history.getChangeType();
            changeTypeCount.put(changeType, changeTypeCount.getOrDefault(changeType, 0) + 1);

            if (history.getOldPrice() != null) {
                java.math.BigDecimal changeAmount = history.getPriceChange();
                changeTypeAmount.put(changeType, changeTypeAmount.getOrDefault(changeType, java.math.BigDecimal.ZERO)
                    .add(changeAmount.abs()));

                totalChanges++;

                if (history.isPriceIncrease()) {
                    increaseCount++;
                    totalIncrease = totalIncrease.add(changeAmount);
                } else if (history.isPriceDecrease()) {
                    decreaseCount++;
                    totalDecrease = totalDecrease.add(changeAmount.abs());
                }
            }
        }

        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        statistics.put("totalChanges", totalChanges);
        statistics.put("increaseCount", increaseCount);
        statistics.put("decreaseCount", decreaseCount);
        statistics.put("totalIncrease", totalIncrease);
        statistics.put("totalDecrease", totalDecrease);
        statistics.put("changeTypeCount", changeTypeCount);
        statistics.put("changeTypeAmount", changeTypeAmount);
        statistics.put("period", days + "天");

        return statistics;
    }

    /**
     * 获取价格变更趋势数据
     *
     * @param hotelId 酒店ID
     * @param days 统计天数
     * @return 按日期分组的变更趋势
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getPriceChangeTrend(Long hotelId, Integer days) {
        if (days == null || days <= 0) {
            days = 30; // 默认统计30天
        }

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);

        List<PriceHistory> historyList = priceHistoryRepository.findByHotelIdAndTimeRange(hotelId, startTime, endTime);

        // 按日期分组
        java.util.Map<String, Integer> dailyChangeCount = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.math.BigDecimal> dailyChangeAmount = new java.util.LinkedHashMap<>();

        // 初始化每一天的数据
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateStr = date.toLocalDate().toString();
            dailyChangeCount.put(dateStr, 0);
            dailyChangeAmount.put(dateStr, java.math.BigDecimal.ZERO);
        }

        // 统计每天的数据
        for (PriceHistory history : historyList) {
            String dateStr = history.getCreatedAt().toLocalDate().toString();

            if (dailyChangeCount.containsKey(dateStr)) {
                dailyChangeCount.put(dateStr, dailyChangeCount.get(dateStr) + 1);
                if (history.getOldPrice() != null) {
                    java.math.BigDecimal currentAmount = dailyChangeAmount.get(dateStr);
                    dailyChangeAmount.put(dateStr, currentAmount.add(history.getPriceChange().abs()));
                }
            }
        }

        java.util.Map<String, Object> trend = new java.util.HashMap<>();
        trend.put("dailyChangeCount", dailyChangeCount);
        trend.put("dailyChangeAmount", dailyChangeAmount);
        trend.put("period", days + "天");

        return trend;
    }

    /**
     * 获取最近的重大价格变更
     *
     * @param hotelId 酒店ID
     * @param threshold 变更百分比阈值
     * @param limit 限制数量
     * @return 重大价格变更列表
     */
    @Transactional(readOnly = true)
    public List<PriceHistory> getSignificantPriceChanges(Long hotelId, Double threshold, Integer limit) {
        if (threshold == null || threshold <= 0) {
            threshold = 10.0; // 默认10%阈值
        }
        if (limit == null || limit <= 0) {
            limit = 20; // 默认20条
        }

        List<PriceHistory> allHistory = priceHistoryRepository.findByHotelId(hotelId, limit * 10); // 获取更多数据进行筛选

        return allHistory.stream()
            .filter(history -> history.getOldPrice() != null)
            .filter(history -> Math.abs(history.getPriceChangePercentage().doubleValue()) >= threshold)
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 清理过期的价格历史记录
     *
     * @param hotelId 酒店ID
     * @param daysToKeep 保留天数
     * @return 清理的记录数量
     */
    @Transactional
    public int cleanupExpiredPriceHistory(Long hotelId, Integer daysToKeep) {
        if (daysToKeep == null || daysToKeep <= 0) {
            daysToKeep = 365; // 默认保留1年
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);

        QueryWrapper<PriceHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("hotel_id", hotelId);
        queryWrapper.lt("created_at", cutoffDate);

        int deletedCount = priceHistoryRepository.delete(queryWrapper);

        log.info("清理价格历史记录完成，酒店ID: {}, 删除数量: {}, 保留天数: {}",
                hotelId, deletedCount, daysToKeep);

        return deletedCount;
    }
}