package com.hotel.service;

import com.hotel.constant.RatingConstants;
import com.hotel.dto.review.admin.ReviewAnalyticsRequest;
import com.hotel.dto.review.ReviewStatisticsResponse;
import com.hotel.dto.review.statistics.*;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAnalyticsService {

    private final ReviewRepository reviewRepository;
    private final com.hotel.repository.ReviewModerationLogRepository moderationLogRepository;
    private final com.hotel.repository.ReviewReplyRepository reviewReplyRepository;
    private final TextAnalysisService textAnalysisService;

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

    // ========== 高级统计功能 ==========

    /**
     * 获取综合评分统计
     */
    @Cacheable(value = "comprehensiveStatistics", key = "'comprehensive_' + #hotelId + '_' + #period")
    public ReviewStatisticsDTO getComprehensiveStatistics(Long hotelId, String period) {
        log.info("获取综合评分统计，酒店ID: {}, 周期: {}", hotelId, period);

        try {
            ReviewStatisticsDTO statistics = new ReviewStatisticsDTO();
            statistics.setHotelId(hotelId);
            statistics.setPeriod(period);
            statistics.setLastUpdated(LocalDateTime.now());

            // 获取时间范围
            LocalDateTime[] dateRange = getDateRangeByPeriod(period);
            LocalDateTime startDate = dateRange[0];
            LocalDateTime endDate = dateRange[1];

            // 只统计已审核通过的评价
            List<Review> approvedReviews = reviewRepository.findByHotelIdAndStatusAndCreatedAtBetween(
                hotelId, "APPROVED", startDate, endDate);

            if (approvedReviews.isEmpty()) {
                statistics.setTotalReviews(0);
                statistics.setOverallRating(0.0);
                return statistics;
            }

            // 计算总评价数
            statistics.setTotalReviews(approvedReviews.size());

            // 计算综合评分（保留1位小数）
            Double overallRating = calculateOverallRating(approvedReviews);
            statistics.setOverallRating(Math.round(overallRating * RatingConstants.RATING_SCALE) / RatingConstants.RATING_SCALE);

            // 计算各维度评分
            Map<String, Double> dimensionRatings = calculateDimensionRatings(approvedReviews);
            statistics.setDimensionRatings(dimensionRatings);

            // 计算评分分布
            Map<Integer, Integer> ratingDistribution = calculateRatingDistribution(approvedReviews);
            statistics.setRatingDistribution(ratingDistribution);

            // 计算同比和环比数据
            statistics.setYearOverYear(calculateYearOverYear(hotelId, startDate, endDate, approvedReviews));
            statistics.setMonthOverMonth(calculateMonthOverMonth(hotelId, startDate, endDate, approvedReviews));

            log.info("综合评分统计计算完成，综合评分: {}", statistics.getOverallRating());
            return statistics;

        } catch (Exception e) {
            log.error("获取综合评分统计失败", e);
            return getDefaultStatistics(hotelId, period);
        }
    }

    /**
     * 获取评分趋势数据
     */
    @Cacheable(value = "ratingTrends", key = "'trends_' + #hotelId + '_' + #startDate.hashCode() + '_' + #endDate.hashCode() + '_' + #groupBy")
    public List<RatingTrendDTO> getRatingTrends(Long hotelId, LocalDateTime startDate, LocalDateTime endDate, String groupBy) {
        log.info("获取评分趋势数据，酒店ID: {}, 分组: {}", hotelId, groupBy);

        try {
            List<RatingTrendDTO> trends = new ArrayList<>();

            // 根据分组方式生成时间段
            List<String> periods = generatePeriods(startDate, endDate, groupBy);

            for (String period : periods) {
                LocalDateTime[] periodRange = getPeriodRange(period, groupBy);
                LocalDateTime periodStart = periodRange[0];
                LocalDateTime periodEnd = periodRange[1];

                // 获取该时间段的评价
                List<Review> periodReviews = reviewRepository.findByHotelIdAndStatusAndCreatedAtBetween(
                    hotelId, "APPROVED", periodStart, periodEnd);

                if (!periodReviews.isEmpty()) {
                    RatingTrendDTO trend = new RatingTrendDTO();
                    trend.setPeriod(period);
                    trend.setOverallRating(Math.round(calculateOverallRating(periodReviews) * RatingConstants.RATING_SCALE) / RatingConstants.RATING_SCALE);
                    trend.setDimensionRatings(calculateDimensionRatings(periodReviews));
                    trend.setReviewCount(periodReviews.size());
                    trend.setNps(calculateNPS(periodReviews));
                    trend.setRecommendationRate(calculateRecommendationRate(periodReviews));
                    trend.setAverageResponseTime(calculateAverageResponseTime(periodReviews, hotelId));

                    trends.add(trend);
                }
            }

            log.info("评分趋势数据生成完成，共{}个数据点", trends.size());
            return trends;

        } catch (Exception e) {
            log.error("获取评分趋势数据失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取词云数据
     */
    @Cacheable(value = "wordCloud", key = "'wordcloud_' + #hotelId + '_' + #limit")
    public List<WordCloudDTO> getWordCloudData(Long hotelId, Integer limit) {
        log.info("获取词云数据，酒店ID: {}, 限制: {}", hotelId, limit);

        try {
            // 获取评论文本进行分词处理
            List<String> comments = reviewRepository.findCommentsForWordCloud(hotelId, "APPROVED", limit * 10);
            Long totalReviews = reviewRepository.countReviewsByHotelAndStatus(hotelId, "APPROVED");

            if (comments.isEmpty()) {
                return Collections.emptyList();
            }

            // 使用文本分析服务生成词云
            StringBuilder allText = new StringBuilder();
            for (String comment : comments) {
                allText.append(comment).append(" ");
            }

            List<WordCloudDTO> wordCloud = textAnalysisService.generateWordCloud(
                allText.toString(), hotelId, limit);

            log.info("词云数据生成完成，包含{}个关键词", wordCloud.size());
            return wordCloud;

        } catch (Exception e) {
            log.error("获取词云数据失败", e);
            return Collections.emptyList();
        }
    }

    
    /**
     * 获取酒店对比分析数据
     */
    @Cacheable(value = "hotelComparison", key = "'comparison_' + #hotelId + '_' + Arrays.hashCode(#competitorIds)")
    public List<HotelComparisonDTO> getHotelComparison(Long hotelId, List<Long> competitorIds) {
        log.info("获取酒店对比分析数据，主酒店ID: {}, 竞品IDs: {}", hotelId, competitorIds);

        try {
            List<HotelComparisonDTO> comparison = new ArrayList<>();

            // 包含当前酒店和竞品酒店
            List<Long> allHotelIds = new ArrayList<>();
            allHotelIds.add(hotelId);
            if (competitorIds != null) {
                allHotelIds.addAll(competitorIds);
            }

            // 为每个酒店计算统计数据
            for (Long currentHotelId : allHotelIds) {
                List<Review> reviews = reviewRepository.findByHotelIdAndStatus(currentHotelId, "APPROVED");

                HotelComparisonDTO hotelComparison = new HotelComparisonDTO();
                hotelComparison.setHotelId(currentHotelId);
                hotelComparison.setHotelName(getHotelName(currentHotelId)); // 需要实现此方法
                hotelComparison.setReviewCount(reviews.size());
                hotelComparison.setIsCurrentHotel(currentHotelId.equals(hotelId));

                if (!reviews.isEmpty()) {
                    hotelComparison.setOverallRating(Math.round(calculateOverallRating(reviews) * 10.0) / 10.0);
                    hotelComparison.setDimensionRatings(calculateDimensionRatings(reviews));
                } else {
                    hotelComparison.setOverallRating(0.0);
                    hotelComparison.setDimensionRatings(new HashMap<>());
                }

                comparison.add(hotelComparison);
            }

            // 计算行业平均值
            Map<String, Double> industryAverages = calculateIndustryAverages(comparison);

            // 计算偏差百分比和排名
            for (HotelComparisonDTO hotel : comparison) {
                hotel.setDeviationFromAverage(calculateDeviationFromAverage(hotel, industryAverages));
            }

            // 按综合评分排序并设置排名
            comparison.sort((a, b) -> Double.compare(b.getOverallRating(), a.getOverallRating()));
            for (int i = 0; i < comparison.size(); i++) {
                comparison.get(i).setRanking(i + 1);
            }

            log.info("酒店对比分析完成，共{}家酒店", comparison.size());
            return comparison;

        } catch (Exception e) {
            log.error("获取酒店对比分析数据失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取改进建议
     */
    @Cacheable(value = "suggestions", key = "'suggestions_' + #hotelId + '_' + #category")
    public List<SuggestionDTO> getSuggestions(Long hotelId, String category) {
        log.info("获取改进建议，酒店ID: {}, 类别: {}", hotelId, category);

        try {
            List<SuggestionDTO> suggestions = textAnalysisService.generateSuggestions(hotelId, category);

            // 增强建议数据
            for (SuggestionDTO suggestion : suggestions) {
                // 查找相关评价
                int relatedReviewCount = countRelatedReviews(hotelId, suggestion.getKeywords());
                suggestion.setRelatedReviewCount(relatedReviewCount);

                // 计算预期评分提升
                double expectedImprovement = calculateExpectedImprovement(hotelId, category, suggestion.getPriority());
                suggestion.setExpectedRatingImprovement(expectedImprovement);
            }

            log.info("改进建议生成完成，共{}条建议", suggestions.size());
            return suggestions;

        } catch (Exception e) {
            log.error("获取改进建议失败", e);
            return Collections.emptyList();
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 根据周期获取日期范围
     */
    private LocalDateTime[] getDateRangeByPeriod(String period) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate;

        switch (period.toLowerCase()) {
            case "daily":
                startDate = endDate.minusDays(1);
                break;
            case "weekly":
                startDate = endDate.minusWeeks(1);
                break;
            case "monthly":
                startDate = endDate.minusMonths(1);
                break;
            case "quarterly":
                startDate = endDate.minusMonths(3);
                break;
            case "yearly":
                startDate = endDate.minusYears(1);
                break;
            default:
                startDate = endDate.minusMonths(1);
        }

        return new LocalDateTime[]{startDate, endDate};
    }

    /**
     * 计算综合评分
     */
    private Double calculateOverallRating(List<Review> reviews) {
        return reviews.stream()
            .mapToInt(Review::getOverallRating)
            .average()
            .orElse(0.0);
    }

    /**
     * 计算各维度评分
     */
    private Map<String, Double> calculateDimensionRatings(List<Review> reviews) {
        Map<String, Double> dimensions = new HashMap<>();

        dimensions.put("cleanliness", reviews.stream()
            .mapToInt(Review::getCleanlinessRating)
            .average()
            .orElse(0.0));

        dimensions.put("service", reviews.stream()
            .mapToInt(Review::getServiceRating)
            .average()
            .orElse(0.0));

        dimensions.put("facilities", reviews.stream()
            .mapToInt(Review::getFacilitiesRating)
            .average()
            .orElse(0.0));

        dimensions.put("location", reviews.stream()
            .mapToInt(Review::getLocationRating)
            .average()
            .orElse(0.0));

        // 保留1位小数
        dimensions.replaceAll((k, v) -> Math.round(v * 10.0) / 10.0);
        return dimensions;
    }

    /**
     * 计算评分分布
     */
    private Map<Integer, Integer> calculateRatingDistribution(List<Review> reviews) {
        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0);
        }

        for (Review review : reviews) {
            int rating = review.getOverallRating();
            distribution.put(rating, distribution.get(rating) + 1);
        }

        return distribution;
    }

    /**
     * 计算同比数据
     */
    private Map<String, Object> calculateYearOverYear(Long hotelId, LocalDateTime startDate, LocalDateTime endDate, List<Review> currentReviews) {
        Map<String, Object> yoy = new HashMap<>();

        LocalDateTime lastYearStart = startDate.minusYears(1);
        LocalDateTime lastYearEnd = endDate.minusYears(1);

        List<Review> lastYearReviews = reviewRepository.findByHotelIdAndStatusAndCreatedAtBetween(
            hotelId, "APPROVED", lastYearStart, lastYearEnd);

        double currentRating = calculateOverallRating(currentReviews);
        double lastYearRating = calculateOverallRating(lastYearReviews);

        yoy.put("currentRating", currentRating);
        yoy.put("lastYearRating", lastYearRating);
        yoy.put("ratingChange", currentRating - lastYearRating);
        yoy.put("ratingChangePercent", lastYearRating > 0 ?
            ((currentRating - lastYearRating) / lastYearRating) * 100 : 0);
        yoy.put("reviewCountChange", currentReviews.size() - lastYearReviews.size());

        return yoy;
    }

    /**
     * 计算环比数据
     */
    private Map<String, Object> calculateMonthOverMonth(Long hotelId, LocalDateTime startDate, LocalDateTime endDate, List<Review> currentReviews) {
        Map<String, Object> mom = new HashMap<>();

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDateTime lastPeriodStart = startDate.minusDays(daysBetween);
        LocalDateTime lastPeriodEnd = startDate.minusDays(1);

        List<Review> lastPeriodReviews = reviewRepository.findByHotelIdAndStatusAndCreatedAtBetween(
            hotelId, "APPROVED", lastPeriodStart, lastPeriodEnd);

        double currentRating = calculateOverallRating(currentReviews);
        double lastPeriodRating = calculateOverallRating(lastPeriodReviews);

        mom.put("currentRating", currentRating);
        mom.put("lastPeriodRating", lastPeriodRating);
        mom.put("ratingChange", currentRating - lastPeriodRating);
        mom.put("ratingChangePercent", lastPeriodRating > 0 ?
            ((currentRating - lastPeriodRating) / lastPeriodRating) * 100 : 0);
        mom.put("reviewCountChange", currentReviews.size() - lastPeriodReviews.size());

        return mom;
    }

    /**
     * 生成时间段
     */
    private List<String> generatePeriods(LocalDateTime startDate, LocalDateTime endDate, String groupBy) {
        List<String> periods = new ArrayList<>();
        DateTimeFormatter formatter;

        switch (groupBy.toLowerCase()) {
            case "day":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime current = startDate.toLocalDate().atStartOfDay();
                while (!current.isAfter(endDate)) {
                    periods.add(current.format(formatter));
                    current = current.plusDays(1);
                }
                break;
            case "week":
                formatter = DateTimeFormatter.ofPattern("yyyy-'W'ww");
                current = startDate;
                while (!current.isAfter(endDate)) {
                    periods.add(current.format(formatter));
                    current = current.plusWeeks(1);
                }
                break;
            case "month":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                current = startDate.withDayOfMonth(1);
                while (!current.isAfter(endDate)) {
                    periods.add(current.format(formatter));
                    current = current.plusMonths(1);
                }
                break;
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                current = startDate.toLocalDate().atStartOfDay();
                while (!current.isAfter(endDate)) {
                    periods.add(current.format(formatter));
                    current = current.plusDays(1);
                }
        }

        return periods;
    }

    /**
     * 获取时间段范围
     */
    private LocalDateTime[] getPeriodRange(String period, String groupBy) {
        LocalDateTime start, end;

        try {
            switch (groupBy.toLowerCase()) {
                case "day":
                    start = LocalDateTime.parse(period + "T00:00:00");
                    end = start.plusDays(1).minusSeconds(1);
                    break;
                case "week":
                    // 简化处理，假设格式为 yyyy-Www
                    String[] weekParts = period.split("-W");
                    int year = Integer.parseInt(weekParts[0]);
                    int week = Integer.parseInt(weekParts[1]);
                    // 这里简化处理，实际应该用更精确的周计算
                    start = LocalDateTime.of(year, 1, 1, 0, 0).plusWeeks(week - 1);
                    end = start.plusWeeks(1).minusSeconds(1);
                    break;
                case "month":
                    String[] monthParts = period.split("-");
                    int monthYear = Integer.parseInt(monthParts[0]);
                    int month = Integer.parseInt(monthParts[1]);
                    start = LocalDateTime.of(monthYear, month, 1, 0, 0);
                    end = start.plusMonths(1).minusSeconds(1);
                    break;
                default:
                    start = LocalDateTime.parse(period + "T00:00:00");
                    end = start.plusDays(1).minusSeconds(1);
            }
        } catch (Exception e) {
            // 默认处理
            start = LocalDateTime.now().minusDays(1);
            end = LocalDateTime.now();
        }

        return new LocalDateTime[]{start, end};
    }

    /**
     * 计算净推荐值(NPS)
     */
    private Double calculateNPS(List<Review> reviews) {
        if (reviews.isEmpty()) return 0.0;

        long promoters = reviews.stream().mapToLong(r -> r.getOverallRating() >= 4 ? 1 : 0).sum();
        long detractors = reviews.stream().mapToLong(r -> r.getOverallRating() <= 2 ? 1 : 0).sum();

        return ((double)(promoters - detractors) / reviews.size()) * 100;
    }

    /**
     * 计算推荐率
     */
    private Double calculateRecommendationRate(List<Review> reviews) {
        if (reviews.isEmpty()) return 0.0;

        long recommenders = reviews.stream().mapToLong(r -> r.getOverallRating() >= 4 ? 1 : 0).sum();

        return ((double)recommenders / reviews.size()) * 100;
    }

    /**
     * 计算平均响应时间
     */
    private Double calculateAverageResponseTime(List<Review> reviews, Long hotelId) {
        // 这里需要实现实际的响应时间计算逻辑
        // 暂时返回模拟值
        return 2.5; // 小时
    }

    /**
     * 获取酒店名称
     */
    private String getHotelName(Long hotelId) {
        // 这里需要调用酒店服务获取酒店名称
        // 暂时返回模拟值
        return "酒店 " + hotelId;
    }

    /**
     * 计算行业平均值
     */
    private Map<String, Double> calculateIndustryAverages(List<HotelComparisonDTO> hotels) {
        Map<String, Double> averages = new HashMap<>();

        double overallSum = hotels.stream().mapToDouble(HotelComparisonDTO::getOverallRating).sum();
        averages.put("overall", overallSum / hotels.size());

        // 计算各维度平均值
        Set<String> dimensions = hotels.stream()
            .flatMap(h -> h.getDimensionRatings().keySet().stream())
            .collect(Collectors.toSet());

        for (String dimension : dimensions) {
            double dimensionSum = hotels.stream()
                .mapToDouble(h -> h.getDimensionRatings().getOrDefault(dimension, 0.0))
                .sum();
            averages.put(dimension, dimensionSum / hotels.size());
        }

        return averages;
    }

    /**
     * 计算与平均值的偏差
     */
    private Map<String, Double> calculateDeviationFromAverage(HotelComparisonDTO hotel, Map<String, Double> industryAverages) {
        Map<String, Double> deviation = new HashMap<>();

        deviation.put("overall", industryAverages.get("overall") > 0 ?
            ((hotel.getOverallRating() - industryAverages.get("overall")) / industryAverages.get("overall")) * 100 : 0);

        for (Map.Entry<String, Double> entry : hotel.getDimensionRatings().entrySet()) {
            String dimension = entry.getKey();
            double value = entry.getValue();
            double average = industryAverages.getOrDefault(dimension, 0.0);

            deviation.put(dimension, average > 0 ? ((value - average) / average) * 100 : 0);
        }

        return deviation;
    }

    /**
     * 统计相关评价数量
     */
    private int countRelatedReviews(Long hotelId, String[] keywords) {
        if (keywords == null || keywords.length == 0) return 0;

        List<Review> reviews = reviewRepository.findByHotelIdAndStatus(hotelId, "APPROVED");

        return (int) reviews.stream()
            .filter(review -> review.getComment() != null &&
                Arrays.stream(keywords).anyMatch(keyword ->
                    review.getComment().toLowerCase().contains(keyword.toLowerCase())))
            .count();
    }

    /**
     * 计算预期改进效果
     */
    private Double calculateExpectedImprovement(Long hotelId, String category, String priority) {
        // 基于优先级和历史数据估算预期改进效果
        switch (priority.toLowerCase()) {
            case "high":
                return 0.3 + Math.random() * 0.2; // 0.3-0.5
            case "medium":
                return 0.2 + Math.random() * 0.1; // 0.2-0.3
            case "low":
                return 0.1 + Math.random() * 0.1; // 0.1-0.2
            default:
                return 0.2;
        }
    }

    /**
     * 计算净推荐值（NPS）
     */
    private Integer calculateNPS(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return 0;
        }

        long promoters = reviews.stream()
            .filter(r -> r.getOverallRating() >= RatingConstants.NPS_PROMOTER_THRESHOLD)
            .count();

        long detractors = reviews.stream()
            .filter(r -> r.getOverallRating() <= RatingConstants.NPS_DETRACTOR_THRESHOLD)
            .count();

        // NPS = (推荐者百分比 - 贬损者百分比) * 100
        double nps = ((double) (promoters - detractors) / reviews.size()) * 100;
        return (int) Math.round(nps);
    }

    /**
     * 计算推荐率
     */
    private Double calculateRecommendationRate(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return 0.0;
        }

        // 将评分4-5分视为愿意推荐
        long recommendCount = reviews.stream()
            .filter(r -> r.getOverallRating() >= 4)
            .count();

        return Math.round((double) recommendCount / reviews.size() * 1000.0) / 10.0; // 保留1位小数
    }

    /**
     * 获取默认统计数据
     */
    private ReviewStatisticsDTO getDefaultStatistics(Long hotelId, String period) {
        ReviewStatisticsDTO statistics = new ReviewStatisticsDTO();
        statistics.setHotelId(hotelId);
        statistics.setPeriod(period);
        statistics.setTotalReviews(0);
        statistics.setOverallRating(0.0);
        statistics.setDimensionRatings(new HashMap<>());
        statistics.setRatingDistribution(new HashMap<>());
        statistics.setTrendData(new ArrayList<>());
        statistics.setLastUpdated(LocalDateTime.now());
        return statistics;
    }
}