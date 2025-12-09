package com.hotel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.review.incentive.*;
import com.hotel.entity.*;
import com.hotel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewIncentiveService {

    private final IncentiveRuleRepository incentiveRuleRepository;
    private final UserPointsRepository userPointsRepository;
    private final ReviewRepository reviewRepository;
    private final HighQualityReviewBadgeRepository highQualityReviewBadgeRepository;
    private final ReviewActivityRepository reviewActivityRepository;
    private final ObjectMapper objectMapper;

    /**
     * 为用户评价计算并发放积分奖励
     */
    @Transactional
    public void awardPointsForReview(Long userId, Long reviewId) {
        log.info("开始为用户 {} 的评价 {} 计算积分奖励", userId, reviewId);

        // 安全改进：增加空值检查，避免NullPointerException
        if (userId == null || reviewId == null) {
            log.warn("用户ID或评价ID为空，跳过积分奖励");
            return;
        }

        Review review = reviewRepository.selectById(reviewId);
        if (review == null) {
            log.warn("评价ID {} 不存在，跳过积分奖励", reviewId);
            return;
        }

        if (!review.getUserId().equals(userId)) {
            log.warn("用户 {} 不匹配评价 {} 的所有者，跳过积分奖励", userId, reviewId);
            return;
        }

        LocalDate currentDate = LocalDate.now();
        List<IncentiveRule> activeRules = incentiveRuleRepository.findActiveRulesByType("POINTS_REVIEW", currentDate);

        int totalPoints = 0;

        for (IncentiveRule rule : activeRules) {
            try {
                if (meetsRuleConditions(review, rule)) {
                    int points = rule.getPointsValue();
                    awardPointsToUser(userId, points, "REVIEW", reviewId, rule);
                    totalPoints += points;
                    log.info("应用激励规则 {}，奖励积分 {}", rule.getId(), points);
                }
            } catch (Exception e) {
                log.error("应用激励规则 {} 时出错", rule.getId(), e);
            }
        }

        // 检查是否为首次评价
        if (isFirstReview(userId)) {
            IncentiveRule firstReviewRule = findFirstReviewRule();
            if (firstReviewRule != null) {
                awardPointsToUser(userId, firstReviewRule.getPointsValue(), "FIRST_REVIEW", reviewId, firstReviewRule);
                totalPoints += firstReviewRule.getPointsValue();
                log.info("首次评价额外奖励积分 {}", firstReviewRule.getPointsValue());
            }
        }

        log.info("用户 {} 的评价 {} 总共获得 {} 积分", userId, reviewId, totalPoints);
    }

    /**
     * 评价质量评估，如果达到高质量标准则发放额外积分
     */
    @Transactional
    public void evaluateReviewQuality(Long reviewId) {
        Review review = reviewRepository.selectById(reviewId);
        if (review == null) {
            log.warn("评价 {} 不存在，无法评估质量", reviewId);
            return;
        }

        int qualityScore = calculateReviewQuality(review);
        log.info("评价 {} 质量评分：{}", reviewId, qualityScore);

        if (qualityScore >= 7) {
            // 发放高质量评价积分
            LocalDate currentDate = LocalDate.now();
            List<IncentiveRule> qualityRules = incentiveRuleRepository.findActiveRulesByType("POINTS_HIGH_QUALITY", currentDate);

            for (IncentiveRule rule : qualityRules) {
                try {
                    Map<String, Object> conditions = objectMapper.readValue(rule.getConditions(), Map.class);
                    Integer requiredScore = (Integer) conditions.get("minQualityScore");

                    if (requiredScore != null && qualityScore >= requiredScore) {
                        awardPointsToUser(review.getUserId(), rule.getPointsValue(), "HIGH_QUALITY_REVIEW", reviewId, rule);
                        log.info("评价 {} 达到高质量标准，额外奖励积分 {}", reviewId, rule.getPointsValue());
                    }
                } catch (JsonProcessingException e) {
                    log.error("解析高质量评价规则条件失败", e);
                }
            }

            // 颁发优质评价标识
            awardQualityBadge(reviewId, qualityScore);
        }
    }

    /**
     * 计算评价质量评分
     */
    public int calculateReviewQuality(Review review) {
        int score = 0;

        // 字数评分
        if (review.getComment() != null) {
            int commentLength = review.getComment().length();
            if (commentLength >= 50) score += 1;
            if (commentLength >= 100) score += 1;
            if (commentLength >= 200) score += 1;
        }

        // 图片评分
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            String[] images = review.getImages().split(",");
            int imageCount = images.length;
            score += Math.min(imageCount, 3); // 最多3分
        }

        // 评分完整性
        if (review.getCleanlinessRating() != null) score += 1;
        if (review.getServiceRating() != null) score += 1;

        // 内容质量（简单的关键词分析）
        if (review.getComment() != null) {
            String comment = review.getComment().toLowerCase();
            if (comment.length() > 30) score += 1; // 有实质内容
            if (comment.contains("很好") || comment.contains("满意") || comment.contains("推荐") ||
                comment.contains("不错") || comment.contains("舒适")) {
                score += 1; // 正面评价词汇
            }
        }

        return score;
    }

    /**
     * 颁发优质评价标识
     */
    @Transactional
    private void awardQualityBadge(Long reviewId, int qualityScore) {
        // 检查是否已获得标识
        if (highQualityReviewBadgeRepository.existsByReviewId(reviewId)) {
            log.info("评价 {} 已获得优质标识，跳过", reviewId);
            return;
        }

        HighQualityReviewBadge badge = new HighQualityReviewBadge();
        badge.setReviewId(reviewId);

        // 根据评分确定标识类型
        if (qualityScore >= 10) {
            badge.setBadgeType("FEATURED");
        } else if (qualityScore >= 8) {
            badge.setBadgeType("HELPFUL");
        } else {
            badge.setBadgeType("DETAILED");
        }

        badge.setAwardedAt(LocalDateTime.now());

        try {
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("qualityScore", qualityScore);
            criteria.put("wordCount", qualityScore >= 3 ? 1 : 0);
            criteria.put("imageCount", qualityScore >= 1 ? 1 : 0);
            badge.setCriteria(objectMapper.writeValueAsString(criteria));
        } catch (JsonProcessingException e) {
            log.error("序列化优质评价标识条件失败", e);
        }

        highQualityReviewBadgeRepository.insert(badge);
        log.info("评价 {} 获得优质标识：{}", reviewId, badge.getBadgeType());
    }

    /**
     * 发放积分给用户
     */
    @Transactional
    private void awardPointsToUser(Long userId, Integer points, String source, Long sourceId, IncentiveRule rule) {
        UserPoints userPoints = new UserPoints();
        userPoints.setUserId(userId);
        userPoints.setPoints(points);
        userPoints.setSource(source);
        userPoints.setSourceId(sourceId);
        userPoints.setCreatedAt(LocalDateTime.now());
        userPoints.setExpiresAt(LocalDate.now().plusYears(1)); // 积分有效期1年

        userPointsRepository.insert(userPoints);
        log.info("用户 {} 获得 {} 积分，来源：{}，规则：{}", userId, points, source, rule.getId());
    }

    /**
     * 检查评价是否符合激励规则条件
     */
    private boolean meetsRuleConditions(Review review, IncentiveRule rule) {
        try {
            if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
                return true;
            }

            Map<String, Object> conditions = objectMapper.readValue(rule.getConditions(), Map.class);

            // 检查字数条件
            Integer minWords = (Integer) conditions.get("minWords");
            if (minWords != null && review.getComment() != null) {
                int wordCount = review.getComment().length();
                if (wordCount < minWords) {
                    return false;
                }
            }

            // 检查图片条件
            Boolean hasImages = (Boolean) conditions.get("hasImages");
            if (hasImages != null && hasImages) {
                if (review.getImages() == null || review.getImages().isEmpty()) {
                    return false;
                }
            }

            // 检查评分条件
            Integer minRating = (Integer) conditions.get("minRating");
            if (minRating != null && review.getOverallRating() != null) {
                if (review.getOverallRating() < minRating) {
                    return false;
                }
            }

            return true;
        } catch (JsonProcessingException e) {
            log.error("解析激励规则条件失败", e);
            return false;
        }
    }

    /**
     * 检查是否为首次评价
     * 性能改进：使用计数查询而不是加载所有评价
     */
    private boolean isFirstReview(Long userId) {
        // 性能优化：使用计数查询避免加载大量数据
        Long reviewCount = reviewRepository.countByUserId(userId);
        return reviewCount != null && reviewCount == 1;
    }

    /**
     * 查找首次评价激励规则
     */
    private IncentiveRule findFirstReviewRule() {
        LocalDate currentDate = LocalDate.now();
        List<IncentiveRule> rules = incentiveRuleRepository.findActiveRulesByType("POINTS_FIRST_REVIEW", currentDate);
        return rules.isEmpty() ? null : rules.get(0);
    }

    /**
     * 获取用户积分汇总信息
     */
    public UserPointsSummaryDTO getUserPointsSummary(Long userId) {
        LocalDate currentDate = LocalDate.now();
        Integer totalPoints = userPointsRepository.getUserActivePoints(userId, currentDate);

        List<UserPoints> history = userPointsRepository.getUserPointsHistory(userId, 50);

        // 计算本月获得的积分
        int earnedThisMonth = history.stream()
            .filter(up -> up.getCreatedAt().toLocalDate().getMonth() == currentDate.getMonth() &&
                          up.getCreatedAt().toLocalDate().getYear() == currentDate.getYear())
            .mapToInt(UserPoints::getPoints)
            .sum();

        UserPointsSummaryDTO summary = new UserPointsSummaryDTO();
        summary.setUserId(userId);
        summary.setTotalPoints(totalPoints != null ? totalPoints : 0);
        summary.setEarnedThisMonth(earnedThisMonth);
        summary.setRecentHistory(history.stream()
            .map(this::convertToDTO)
            .limit(10)
            .toList());

        return summary;
    }

    private UserPointsDTO convertToDTO(UserPoints entity) {
        UserPointsDTO dto = new UserPointsDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setPoints(entity.getPoints());
        dto.setSource(entity.getSource());
        dto.setSourceId(entity.getSourceId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        return dto;
    }
}