package com.hotel.service;

import com.hotel.dto.review.admin.ReviewAnalyticsRequest;
import com.hotel.dto.review.ReviewStatisticsResponse;
import com.hotel.entity.Review;
import com.hotel.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAnalyticsService {

    private final ReviewRepository reviewRepository;
    private final com.hotel.repository.ReviewModerationLogRepository moderationLogRepository;
    private final com.hotel.repository.ReviewReplyRepository reviewReplyRepository;

    @Cacheable(value = "reviewStatistics", key = "'overview_' + #hotelId")
    public ReviewStatisticsResponse getOverallStatistics(Long hotelId) {
        ReviewStatisticsResponse response = new ReviewStatisticsResponse();

        // 基础统计
        long totalReviews = reviewRepository.count();
        response.setTotalReviews(totalReviews);

        // 按状态统计
        long pendingCount = reviewRepository.countByStatus("PENDING");
        long approvedCount = reviewRepository.countByStatus("APPROVED");
        long rejectedCount = reviewRepository.countByStatus("REJECTED");
        long hiddenCount = reviewRepository.countByStatus("HIDDEN");

        response.setPendingReviews(pendingCount);
        response.setApprovedReviews(approvedCount);
        response.setRejectedReviews(rejectedCount);
        response.setHiddenReviews(hiddenCount);

        // 平均评分
        Object[] ratingStats = reviewRepository.getAverageRatingStatistics(hotelId);
        if (ratingStats != null && ratingStats.length > 0) {
            response.setAverageOverallRating((Double) ratingStats[0]);
            response.setAverageCleanlinessRating((Double) ratingStats[1]);
            response.setAverageServiceRating((Double) ratingStats[2]);
            response.setAverageFacilitiesRating((Double) ratingStats[3]);
            response.setAverageLocationRating((Double) ratingStats[4]);
        }

        // 评分分布
        Map<Integer, Long> ratingDistribution = reviewRepository.getRatingDistribution(hotelId);
        response.setRatingDistribution(ratingDistribution);

        return response;
    }

    @Cacheable(value = "reviewTrends", key = "#request.hashCode()")
    public Map<String, Object> getReviewTrends(ReviewAnalyticsRequest request) {
        Map<String, Object> result = new HashMap<>();

        LocalDateTime startDate = request.getStartDate() != null ?
            request.getStartDate() : LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = request.getEndDate() != null ?
            request.getEndDate() : LocalDateTime.now();

        // 评价数量趋势
        List<Map<String, Object>> countTrends = getReviewCountTrends(
            request.getHotelId(), startDate, endDate, request.getGroupBy());
        result.put("countTrends", countTrends);

        // 评分趋势
        List<Map<String, Object>> ratingTrends = getRatingTrends(
            request.getHotelId(), startDate, endDate, request.getGroupBy());
        result.put("ratingTrends", ratingTrends);

        // 状态变化趋势
        List<Map<String, Object>> statusTrends = getStatusTrends(
            request.getHotelId(), startDate, endDate, request.getGroupBy());
        result.put("statusTrends", statusTrends);

        return result;
    }

    @Cacheable(value = "reviewQuality", key = "#hotelId")
    public Map<String, Object> getQualityAnalysis(Long hotelId) {
        Map<String, Object> result = new HashMap<>();

        // 违规率分析
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("PENDING", reviewRepository.countByStatus("PENDING"));
        statusCounts.put("APPROVED", reviewRepository.countByStatus("APPROVED"));
        statusCounts.put("REJECTED", reviewRepository.countByStatus("REJECTED"));
        statusCounts.put("HIDDEN", reviewRepository.countByStatus("HIDDEN"));

        long totalModerated = statusCounts.get("APPROVED") + statusCounts.get("REJECTED") + statusCounts.get("HIDDEN");
        double violationRate = totalModerated > 0 ?
            ((double)(statusCounts.get("REJECTED") + statusCounts.get("HIDDEN")) / totalModerated) * 100 : 0;

        result.put("statusCounts", statusCounts);
        result.put("violationRate", violationRate);

        // 回复率分析
        long totalApprovedReviews = reviewRepository.countByStatus("APPROVED");
        long totalReplies = totalApprovedReviews > 0 ?
            reviewReplyRepository.countRepliesByFilters("PUBLISHED", null) : 0;
        double replyRate = totalApprovedReviews > 0 ?
            ((double)totalReplies / totalApprovedReviews) * 100 : 0;

        result.put("replyRate", replyRate);
        result.put("totalReplies", totalReplies);

        // 审核效率分析
        List<Review> recentReviews = reviewRepository.findRecentReviewsByHotelId(
            hotelId, LocalDateTime.now().minusDays(30));

        double averageModerationTime = calculateAverageModerationTime(recentReviews);
        result.put("averageModerationTime", averageModerationTime);

        return result;
    }

    @Cacheable(value = "moderationStats", key = "#adminId")
    public Map<String, Object> getModerationStatistics(Long adminId) {
        Map<String, Object> result = new HashMap<>();

        // 审核操作统计
        List<ReviewModerationLog> adminLogs = moderationLogRepository.findByAdminIdOrderByCreatedAtDesc(adminId);

        Map<String, Long> actionCounts = adminLogs.stream()
            .collect(Collectors.groupingBy(ReviewModerationLog::getAction, Collectors.counting()));

        result.put("actionCounts", actionCounts);
        result.put("totalActions", adminLogs.size());

        // 最近7天的审核活动
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long recentActions = adminLogs.stream()
            .filter(log -> log.getCreatedAt().isAfter(weekAgo))
            .count();

        result.put("recentActions", recentActions);

        return result;
    }

    private List<Map<String, Object>> getReviewCountTrends(Long hotelId, LocalDateTime startDate,
                                                          LocalDateTime endDate, String groupBy) {
        String dateFormat = getDateFormat(groupBy);
        List<Map<String, Object>> trends = new ArrayList<>();

        try {
            List<Object[]> results = reviewRepository.getReviewCountByDate(hotelId, startDate, endDate);

            for (Object[] result : results) {
                java.sql.Date date = (java.sql.Date) result[0];
                Long count = ((Number) result[1]).longValue();

                Map<String, Object> point = new HashMap<>();
                point.put("date", date.toLocalDate().format(DateTimeFormatter.ofPattern(dateFormat)));
                point.put("count", count);
                trends.add(point);
            }
        } catch (Exception e) {
            log.error("获取评价数量趋势失败", e);
            // 如果查询失败，返回空列表而不是模拟数据
        }

        return trends;
    }

    private List<Map<String, Object>> getRatingTrends(Long hotelId, LocalDateTime startDate,
                                                     LocalDateTime endDate, String groupBy) {
        String dateFormat = getDateFormat(groupBy);
        List<Map<String, Object>> trends = new ArrayList<>();

        try {
            List<Object[]> results = reviewRepository.getRatingByDate(hotelId, startDate, endDate);

            for (Object[] result : results) {
                java.sql.Date date = (java.sql.Date) result[0];
                Double avgRating = ((Number) result[1]).doubleValue();

                Map<String, Object> point = new HashMap<>();
                point.put("date", date.toLocalDate().format(DateTimeFormatter.ofPattern(dateFormat)));
                point.put("rating", avgRating);
                trends.add(point);
            }
        } catch (Exception e) {
            log.error("获取评分趋势失败", e);
        }

        return trends;
    }

    private List<Map<String, Object>> getStatusTrends(Long hotelId, LocalDateTime startDate,
                                                     LocalDateTime endDate, String groupBy) {
        String dateFormat = getDateFormat(groupBy);
        List<Map<String, Object>> trends = new ArrayList<>();

        try {
            List<Object[]> results = moderationLogRepository.getStatusTrendsByDate(startDate, endDate);

            for (Object[] result : results) {
                java.sql.Date date = (java.sql.Date) result[0];
                Long approved = result[1] != null ? ((Number) result[1]).longValue() : 0L;
                Long rejected = result[2] != null ? ((Number) result[2]).longValue() : 0L;
                Long pending = result[3] != null ? ((Number) result[3]).longValue() : 0L;

                Map<String, Object> point = new HashMap<>();
                point.put("date", date.toLocalDate().format(DateTimeFormatter.ofPattern(dateFormat)));
                point.put("approved", approved);
                point.put("rejected", rejected);
                point.put("pending", pending);
                trends.add(point);
            }
        } catch (Exception e) {
            log.error("获取状态趋势失败", e);
        }

        return trends;
    }

    private String getDateFormat(String groupBy) {
        switch (groupBy) {
            case "week":
                return "yyyy-'W'ww";
            case "month":
                return "yyyy-MM";
            default:
                return "yyyy-MM-dd";
        }
    }

    private double calculateAverageModerationTime(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return 0.0;
        }

        // 这里需要实际的审核时间计算逻辑
        // 暂时返回模拟值
        return 2.5; // 平均2.5小时
    }

    @CacheEvict(value = {"reviewStatistics", "reviewTrends", "reviewQuality", "moderationStats"}, allEntries = true)
    public void clearAnalyticsCache() {
        log.info("清空评价分析缓存");
    }
}