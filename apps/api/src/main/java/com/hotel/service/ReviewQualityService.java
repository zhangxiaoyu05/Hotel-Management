package com.hotel.service;

import com.hotel.dto.review.incentive.HighQualityBadgeDTO;
import com.hotel.dto.review.incentive.ReviewLeaderboardDTO;
import com.hotel.entity.HighQualityReviewBadge;
import com.hotel.entity.Review;
import com.hotel.entity.User;
import com.hotel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewQualityService {

    private final ReviewRepository reviewRepository;
    private final HighQualityReviewBadgeRepository highQualityReviewBadgeRepository;
    private final UserPointsRepository userPointsRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LEADERBOARD_CACHE_PREFIX = "review:leaderboard:";
    private static final int CACHE_EXPIRY_HOURS = 1;

    /**
     * 评估所有待评估的评价质量
     */
    @Transactional
    public void evaluatePendingReviews() {
        log.info("开始评估待处理评价的质量");

        List<Review> pendingReviews = reviewRepository.findByStatus("PENDING");

        for (Review review : pendingReviews) {
            try {
                evaluateReviewQuality(review);
            } catch (Exception e) {
                log.error("评估评价质量失败，评价ID：{}", review.getId(), e);
            }
        }

        log.info("完成 {} 个评价的质量评估", pendingReviews.size());
    }

    /**
     * 评估单个评价的质量
     */
    @Transactional
    public void evaluateReviewQuality(Review review) {
        int qualityScore = calculateQualityScore(review);
        log.info("评价 {} 质量评分：{}", review.getId(), qualityScore);

        if (qualityScore >= 7) {
            awardQualityBadge(review.getId(), qualityScore);
        }

        // 更新评价状态
        review.setStatus("APPROVED");
        reviewRepository.updateById(review);

        log.info("评价 {} 质量评估完成，状态更新为已审核", review.getId());
    }

    /**
     * 计算评价质量评分
     */
    public int calculateQualityScore(Review review) {
        int score = 0;

        // 字数评分 (0-3分)
        if (review.getComment() != null) {
            int commentLength = review.getComment().length();
            if (commentLength >= 50) score += 1;
            if (commentLength >= 100) score += 1;
            if (commentLength >= 200) score += 1;
        }

        // 图片评分 (0-3分)
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            String[] images = review.getImages().split(",");
            int imageCount = images.length;
            score += Math.min(imageCount, 3);
        }

        // 评分完整性 (0-2分)
        int completedRatings = 0;
        if (review.getCleanlinessRating() != null) completedRatings++;
        if (review.getServiceRating() != null) completedRatings++;
        if (review.getFacilitiesRating() != null) completedRatings++;
        if (review.getLocationRating() != null) completedRatings++;

        if (completedRatings >= 3) score += 2;
        else if (completedRatings >= 1) score += 1;

        // 内容质量分析 (0-2分)
        if (review.getComment() != null) {
            score += analyzeContentQuality(review.getComment());
        }

        return score;
    }

    /**
     * 分析内容质量
     */
    private int analyzeContentQuality(String comment) {
        int contentScore = 0;

        String lowerComment = comment.toLowerCase();

        // 检查内容长度
        if (comment.length() > 30) {
            contentScore += 1;
        }

        // 检查正面评价关键词
        List<String> positiveKeywords = Arrays.asList(
            "很好", "满意", "推荐", "不错", "舒适", "干净", "热情", "周到",
            "excellent", "good", "comfortable", "clean", "recommend"
        );

        boolean hasPositiveKeyword = positiveKeywords.stream()
            .anyMatch(lowerComment::contains);

        if (hasPositiveKeyword) {
            contentScore += 1;
        }

        // 检查是否有具体描述
        List<String> descriptiveKeywords = Arrays.asList(
            "房间", "服务", "设施", "位置", "环境", "早餐", "卫生", "员工",
            "room", "service", "facilities", "location", "environment", "breakfast"
        );

        boolean hasDescriptiveKeyword = descriptiveKeywords.stream()
            .anyMatch(lowerComment::contains);

        if (hasDescriptiveKeyword) {
            contentScore = Math.min(contentScore + 1, 2);
        }

        return contentScore;
    }

    /**
     * 颁发优质评价标识
     */
    @Transactional
    private void awardQualityBadge(Long reviewId, int qualityScore) {
        if (highQualityReviewBadgeRepository.existsByReviewId(reviewId)) {
            log.info("评价 {} 已获得优质标识", reviewId);
            return;
        }

        HighQualityReviewBadge badge = new HighQualityReviewBadge();
        badge.setReviewId(reviewId);

        if (qualityScore >= 10) {
            badge.setBadgeType("FEATURED");
        } else if (qualityScore >= 8) {
            badge.setBadgeType("HELPFUL");
        } else {
            badge.setBadgeType("DETAILED");
        }

        badge.setAwardedAt(LocalDateTime.now());

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("qualityScore", qualityScore);
        criteria.put("wordCount", qualityScore >= 3 ? 1 : 0);
        criteria.put("imageCount", qualityScore >= 1 ? 1 : 0);

        try {
            badge.setCriteria(convertToJson(criteria));
        } catch (Exception e) {
            log.error("序列化优质评价标识条件失败", e);
        }

        highQualityReviewBadgeRepository.insert(badge);
        log.info("评价 {} 获得优质标识：{}", reviewId, badge.getBadgeType());
    }

    private String convertToJson(Map<String, Object> map) {
        // 简化的JSON转换
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (json.length() > 1) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
        }
        json.append("}");
        return json.toString();
    }

    /**
     * 获取评价排行榜
     */
    public ReviewLeaderboardDTO getLeaderboard(String periodType, String period) {
        String cacheKey = LEADERBOARD_CACHE_PREFIX + periodType + ":" + period;

        // 尝试从缓存获取
        ReviewLeaderboardDTO cached = (ReviewLeaderboardDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取排行榜：{}", cacheKey);
            return cached;
        }

        // 从数据库计算排行榜
        ReviewLeaderboardDTO leaderboard = calculateLeaderboard(periodType, period);

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, leaderboard, CACHE_EXPIRY_HOURS, TimeUnit.HOURS);
        log.info("计算并缓存排行榜：{}", cacheKey);

        return leaderboard;
    }

    /**
     * 计算排行榜数据
     */
    private ReviewLeaderboardDTO calculateLeaderboard(String periodType, String period) {
        LocalDateTime startDate = getStartDate(periodType, period);
        LocalDateTime endDate = getEndDate(periodType, period);

        List<Review> reviews = reviewRepository.findByHotelIdAndStatusAndCreatedAtBetween(
            null, "APPROVED", startDate, endDate);

        Map<Long, ReviewStats> userStats = new HashMap<>();

        // 性能优化：批量查询所有优质标识，避免N+1查询问题
        List<Long> reviewIds = reviews.stream()
            .map(Review::getId)
            .toList();

        List<HighQualityReviewBadge> allBadges = highQualityReviewBadgeRepository.findByReviewIds(reviewIds);
        Map<Long, Boolean> reviewHasBadgeMap = allBadges.stream()
            .collect(Collectors.toMap(
                HighQualityReviewBadge::getReviewId,
                badge -> true,
                (existing, replacement) -> existing
            ));

        // 统计每个用户的评价数据
        for (Review review : reviews) {
            Long userId = review.getUserId();
            ReviewStats stats = userStats.computeIfAbsent(userId, k -> new ReviewStats());

            stats.totalReviews++;
            stats.qualityScore += calculateQualityScore(review);

            // 优化：使用预查询的结果
            if (reviewHasBadgeMap.containsKey(review.getId())) {
                stats.qualityReviews++;
            }
        }

        // 计算综合评分并排序
        List<ReviewLeaderboardDTO.LeaderboardEntryDTO> entries = userStats.entrySet().stream()
            .map(entry -> createLeaderboardEntry(entry.getKey(), entry.getValue()))
            .sorted((e1, e2) -> {
                // 按综合评分排序
                int scoreCompare = Integer.compare(e2.getQualityScore(), e1.getQualityScore());
                if (scoreCompare != 0) return scoreCompare;
                // 相同评分按评价数量排序
                return Integer.compare(e2.getTotalReviews(), e1.getTotalReviews());
            })
            .limit(100) // 限制排行榜前100名
            .collect(Collectors.toList());

        // 性能优化：批量更新用户名，避免N+1查询
        batchUpdateLeaderboardUserNames(entries);

        // 设置排名
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        ReviewLeaderboardDTO leaderboard = new ReviewLeaderboardDTO();
        leaderboard.setPeriodType(periodType);
        leaderboard.setPeriod(period);
        leaderboard.setUpdatedAt(LocalDateTime.now());
        leaderboard.setEntries(entries);

        return leaderboard;
    }

    private LocalDateTime getStartDate(String periodType, String period) {
        switch (periodType) {
            case "monthly":
                return LocalDateTime.parse(period + "-01 00:00:00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            case "quarterly":
                String[] parts = period.split("-Q");
                int year = Integer.parseInt(parts[0]);
                int quarter = Integer.parseInt(parts[1]);
                int month = (quarter - 1) * 3 + 1;
                return LocalDateTime.of(year, month, 1, 0, 0, 0);
            case "yearly":
                return LocalDateTime.of(Integer.parseInt(period), 1, 1, 0, 0, 0);
            default:
                return LocalDateTime.now().minusMonths(1);
        }
    }

    private LocalDateTime getEndDate(String periodType, String period) {
        LocalDateTime startDate = getStartDate(periodType, period);
        switch (periodType) {
            case "monthly":
                return startDate.plusMonths(1).minusSeconds(1);
            case "quarterly":
                return startDate.plusMonths(3).minusSeconds(1);
            case "yearly":
                return startDate.plusYears(1).minusSeconds(1);
            default:
                return LocalDateTime.now();
        }
    }

    private ReviewLeaderboardDTO.LeaderboardEntryDTO createLeaderboardEntry(Long userId, ReviewStats stats) {
        ReviewLeaderboardDTO.LeaderboardEntryDTO entry = new ReviewLeaderboardDTO.LeaderboardEntryDTO();
        entry.setUserId(userId);
        entry.setTotalReviews(stats.totalReviews);
        entry.setQualityScore(stats.qualityScore);
        entry.setTotalPoints(calculatePoints(stats));

        return entry;
    }

    /**
     * 批量处理排行榜用户名，避免N+1查询
     */
  private void batchUpdateLeaderboardUserNames(List<ReviewLeaderboardDTO.LeaderboardEntryDTO> entries) {
        List<Long> userIds = entries.stream()
            .map(ReviewLeaderboardDTO.LeaderboardEntryDTO::getUserId)
            .toList();

        // 批量查询用户信息
        List<User> users = userRepository.selectBatchIds(userIds);
        Map<Long, String> userIdToNameMap = users.stream()
            .collect(Collectors.toMap(
                User::getId,
                user -> maskUserName(user.getUsername())
            ));

        // 批量更新用户名
        for (ReviewLeaderboardDTO.LeaderboardEntryDTO entry : entries) {
            entry.setUserName(userIdToNameMap.getOrDefault(entry.getUserId(), "匿名用户"));
        }
  }

    /**
     * 对用户名进行脱敏处理，防止用户枚举
     * @param username 原始用户名
     * @return 脱敏后的用户名
     */
    private String maskUserName(String username) {
        if (username == null || username.length() <= 2) {
            return "***";
        }
        // 保留首尾字符，中间用*替代
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }

    private Integer calculatePoints(ReviewStats stats) {
        // 基础分数：评价数量 + 质量分数 + 优质评价额外分数
        return stats.totalReviews + stats.qualityScore + (stats.qualityReviews * 5);
    }

    /**
     * 获取评价的优质标识
     */
    public HighQualityBadgeDTO getReviewBadge(Long reviewId) {
        List<HighQualityReviewBadge> badges = highQualityReviewBadgeRepository.findByReviewId(reviewId);

        if (badges.isEmpty()) {
            return null;
        }

        HighQualityReviewBadge badge = badges.get(0);
        HighQualityBadgeDTO dto = new HighQualityBadgeDTO();
        dto.setId(badge.getId());
        dto.setReviewId(badge.getReviewId());
        dto.setBadgeType(badge.getBadgeType());
        dto.setAwardedAt(badge.getAwardedAt());

        // 设置显示信息
        dto.setDisplayName(getBadgeDisplayName(badge.getBadgeType()));
        dto.setDescription(getBadgeDescription(badge.getBadgeType()));
        dto.setIconUrl(getBadgeIconUrl(badge.getBadgeType()));

        return dto;
    }

    private String getBadgeDisplayName(String badgeType) {
        switch (badgeType) {
            case "FEATURED": return "精选评价";
            case "HELPFUL": return "有用评价";
            case "DETAILED": return "详细评价";
            default: return "优质评价";
        }
    }

    private String getBadgeDescription(String badgeType) {
        switch (badgeType) {
            case "FEATURED": return "优质精选评价，内容详实有价值";
            case "HELPFUL": return "对其他用户有帮助的评价";
            case "DETAILED": return "内容详细的评价";
            default: return "高质量评价";
        }
    }

    private String getBadgeIconUrl(String badgeType) {
        switch (badgeType) {
            case "FEATURED": return "/icons/badge-featured.svg";
            case "HELPFUL": return "/icons/badge-helpful.svg";
            case "DETAILED": return "/icons/badge-detailed.svg";
            default: return "/icons/badge-default.svg";
        }
    }

    /**
     * 清除排行榜缓存
     */
    public void clearLeaderboardCache() {
        Set<String> keys = redisTemplate.keys(LEADERBOARD_CACHE_PREFIX + "*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("清除排行榜缓存，共 {} 个键", keys.size());
        }
    }

    private static class ReviewStats {
        int totalReviews = 0;
        int qualityScore = 0;
        int qualityReviews = 0;
    }
}