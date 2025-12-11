package com.hotel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.review.incentive.ReviewActivityDTO;
import com.hotel.entity.ActivityParticipation;
import com.hotel.entity.ReviewActivity;
import com.hotel.entity.UserPoints;
import com.hotel.repository.ActivityParticipationRepository;
import com.hotel.repository.ReviewActivityRepository;
import com.hotel.repository.UserPointsRepository;
import com.hotel.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewActivityService {

    private final ReviewActivityRepository reviewActivityRepository;
    private final UserPointsRepository userPointsRepository;
    private final ActivityParticipationRepository activityParticipationRepository;
    private final ReviewIncentiveService reviewIncentiveService;
    private final UserContextService userContextService;
    private final ObjectMapper objectMapper;

    /**
     * 获取所有有效的活动
     */
    public List<ReviewActivityDTO> getActiveActivities() {
        LocalDateTime now = LocalDateTime.now();
        List<ReviewActivity> activities = reviewActivityRepository.findActiveActivities(now);

        return activities.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取即将开始的活动
     */
    public List<ReviewActivityDTO> getUpcomingActivities() {
        LocalDateTime now = LocalDateTime.now();
        List<ReviewActivity> activities = reviewActivityRepository.findUpcomingActivities(now);

        return activities.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * 根据活动类型获取活动
     */
    public List<ReviewActivityDTO> getActivitiesByType(String activityType) {
        List<ReviewActivity> activities = reviewActivityRepository.findByActivityType(activityType);

        return activities.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取用户参与的活动
     */
    public List<ActivityParticipation> getUserParticipations(Long userId) {
        return activityParticipationRepository.findByUserId(userId);
    }

    /**
     * 获取用户在特定时间段内参与的活动
     */
    public List<ActivityParticipation> getUserParticipationsInDateRange(Long userId,
                                                                       LocalDateTime startDate,
                                                                       LocalDateTime endDate) {
        return activityParticipationRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * 取消活动参与
     */
    @Transactional
    public void cancelParticipation(Long activityId, String reason) {
        Long userId = userContextService.getCurrentUserId();
        int affected = activityParticipationRepository.cancelParticipation(userId, activityId, reason);

        if (affected == 0) {
            throw new RuntimeException("未找到有效的参与记录或已取消");
        }

        log.info("用户 {} 取消参与活动 {}，原因：{}", userId, activityId, reason);
    }

    /**
     * 参与活动
     */
    @Transactional
    public Map<String, Object> joinActivity(Long activityId) {
        Long userId = userContextService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        ReviewActivity activity = reviewActivityRepository.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        if (!activity.getIsActive()) {
            throw new RuntimeException("活动未激活");
        }

        if (now.isBefore(activity.getStartDate())) {
            throw new RuntimeException("活动尚未开始");
        }

        if (now.isAfter(activity.getEndDate())) {
            throw new RuntimeException("活动已结束");
        }

        // 检查用户是否已参与该活动
        if (hasUserParticipated(userId, activityId)) {
            throw new RuntimeException("用户已参与该活动");
        }

        // 处理活动参与逻辑
        Map<String, Object> result = processActivityParticipation(userId, activity);

        // 记录活动参与
        recordActivityParticipation(userId, activityId);

        log.info("用户 {} 成功参与活动：{}", userId, activity.getTitle());

        return result;
    }

    /**
     * 处理活动参与逻辑
     */
    private Map<String, Object> processActivityParticipation(Long userId, ReviewActivity activity) {
        Map<String, Object> result = new HashMap<>();
        String activityType = activity.getActivityType();

        try {
            Map<String, Object> rules = objectMapper.readValue(activity.getRules(), Map.class);

            switch (activityType) {
                case "DOUBLE_POINTS":
                    result = handleDoublePointsActivity(userId, rules);
                    break;
                case "REVIEW_CONTEST":
                    result = handleReviewContestActivity(userId, rules);
                    break;
                case "MONTHLY_CHAMPION":
                    result = handleMonthlyChampionActivity(userId, rules);
                    break;
                default:
                    result.put("message", "成功参与活动：" + activity.getTitle());
                    break;
            }
        } catch (JsonProcessingException e) {
            log.error("解析活动规则失败", e);
            result.put("error", "活动规则解析失败");
        }

        return result;
    }

    /**
     * 处理双倍积分活动
     */
    private Map<String, Object> handleDoublePointsActivity(Long userId, Map<String, Object> rules) {
        Map<String, Object> result = new HashMap<>();

        // 双倍积分会在评价提交时自动应用
        result.put("message", "成功参与双倍积分活动，后续评价将获得双倍积分奖励");
        result.put("bonusType", "DOUBLE_POINTS");
        result.put("multiplier", rules.getOrDefault("multiplier", 2));

        return result;
    }

    /**
     * 处理评价竞赛活动
     */
    private Map<String, Object> handleReviewContestActivity(Long userId, Map<String, Object> rules) {
        Map<String, Object> result = new HashMap<>();

        // 发放参与奖励
        Integer participationReward = (Integer) rules.get("participationReward");
        if (participationReward != null && participationReward > 0) {
            UserPoints userPoints = new UserPoints();
            userPoints.setUserId(userId);
            userPoints.setPoints(participationReward);
            userPoints.setSource("ACTIVITY_PARTICIPATION");
            userPoints.setSourceId(System.currentTimeMillis()); // 临时ID
            userPoints.setCreatedAt(LocalDateTime.now());
            userPoints.setExpiresAt(java.time.LocalDate.now().plusYears(1));

            userPointsRepository.insert(userPoints);

            result.put("participationReward", participationReward);
            result.put("message", "成功参与评价竞赛，获得参与奖励：" + participationReward + "积分");
        } else {
            result.put("message", "成功参与评价竞赛活动");
        }

        result.put("bonusType", "REVIEW_CONTEST");

        return result;
    }

    /**
     * 处理月度冠军活动
     */
    private Map<String, Object> handleMonthlyChampionActivity(Long userId, Map<String, Object> rules) {
        Map<String, Object> result = new HashMap<>();

        result.put("message", "成功参与月度冠军活动");
        result.put("bonusType", "MONTHLY_CHAMPION");
        result.put("description", "在本月提交最多高质量评价的用户将获得额外奖励");

        return result;
    }

    /**
     * 检查用户是否已参与活动
     */
    private boolean hasUserParticipated(Long userId, Long activityId) {
        return activityParticipationRepository.existsByUserIdAndActivityId(userId, activityId);
    }

    /**
     * 记录活动参与
     */
    private void recordActivityParticipation(Long userId, Long activityId) {
        ActivityParticipation participation = new ActivityParticipation();
        participation.setUserId(userId);
        participation.setActivityId(activityId);
        participation.setJoinedAt(LocalDateTime.now());
        participation.setStatus("ACTIVE");
        participation.setDeleted(0);

        // 获取活动快照
        ReviewActivity activity = reviewActivityRepository.selectById(activityId);
        if (activity != null) {
            try {
                // 创建活动快照
                HashMap<String, Object> snapshot = new HashMap<>();
                snapshot.put("title", activity.getTitle());
                snapshot.put("activityType", activity.getActivityType());
                snapshot.put("rules", activity.getRules());
                participation.setActivitySnapshot(objectMapper.writeValueAsString(snapshot));
            } catch (JsonProcessingException e) {
                log.warn("创建活动快照失败，活动ID：{}", activityId, e);
            }
        }

        activityParticipationRepository.insert(participation);
        log.info("记录用户 {} 参与活动 {}，参与ID：{}", userId, activityId, participation.getId());
    }

    /**
     * 创建新活动（管理员功能）
     */
    @Transactional
    public ReviewActivityDTO createActivity(ReviewActivityDTO dto) {
        ReviewActivity activity = convertToEntity(dto);
        activity.setCreatedBy(userContextService.getCurrentUserId());
        activity.setCreatedAt(LocalDateTime.now());

        // 设置默认活动规则
        if (activity.getRules() == null || activity.getRules().isEmpty()) {
            activity.setRules(createDefaultRules(dto.getActivityType()));
        }

        reviewActivityRepository.insert(activity);

        log.info("创建新活动：{}", activity.getTitle());

        return convertToDTO(activity);
    }

    /**
     * 创建默认活动规则
     * 安全改进：验证活动类型，防止SpEL注入
     */
    private String createDefaultRules(String activityType) {
        // 安全验证：限制允许的活动类型
        if (!isValidActivityType(activityType)) {
            log.warn("不支持的活动类型：{}", activityType);
            return "{}";
        }

        Map<String, Object> rules = new HashMap<>();

        switch (activityType) {
            case "DOUBLE_POINTS":
                rules.put("multiplier", 2);
                rules.put("maxReviews", 10);
                break;
            case "REVIEW_CONTEST":
                rules.put("participationReward", 10);
                rules.put("winnerReward", 500);
                break;
            case "MONTHLY_CHAMPION":
                rules.put("championReward", 1000);
                rules.put("runnerUpReward", 500);
                break;
        }

        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            log.error("序列化默认活动规则失败", e);
            return "{}";
        }
    }

    /**
     * 验证活动类型是否合法
     */
    private boolean isValidActivityType(String activityType) {
        if (activityType == null || activityType.trim().isEmpty()) {
            return false;
        }

        // 白名单验证，防止SpEL注入
        return Arrays.asList("DOUBLE_POINTS", "REVIEW_CONTEST", "MONTHLY_CHAMPION")
            .contains(activityType.trim().toUpperCase());
    }

    /**
     * 更新活动（管理员功能）
     */
    @Transactional
    public ReviewActivityDTO updateActivity(Long activityId, ReviewActivityDTO dto) {
        ReviewActivity existing = reviewActivityRepository.selectById(activityId);
        if (existing == null) {
            throw new RuntimeException("活动不存在");
        }

        // 更新活动信息
        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setActivityType(dto.getActivityType());
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setIsActive(dto.getIsActive());

        if (dto.getRules() != null) {
            try {
                existing.setRules(objectMapper.writeValueAsString(dto.getRules()));
            } catch (JsonProcessingException e) {
                log.error("序列化活动规则失败", e);
            }
        }

        reviewActivityRepository.updateById(existing);

        log.info("更新活动：{}", existing.getTitle());

        return convertToDTO(existing);
    }

    /**
     * 删除活动（管理员功能）
     */
    @Transactional
    public void deleteActivity(Long activityId) {
        ReviewActivity activity = reviewActivityRepository.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 软删除
        reviewActivityRepository.deleteById(activityId);

        log.info("删除活动：{}", activity.getTitle());
    }

    /**
     * 处理评价提交时的活动奖励
     */
    @Transactional
    public void processReviewActivityRewards(Long userId, Long reviewId) {
        LocalDateTime now = LocalDateTime.now();
        List<ReviewActivity> activeActivities = reviewActivityRepository.findActiveActivities(now);

        for (ReviewActivity activity : activeActivities) {
            try {
                if (shouldApplyActivityReward(userId, activity)) {
                    applyActivityReward(userId, reviewId, activity);
                }
            } catch (Exception e) {
                log.error("处理活动奖励失败，活动ID：{}", activity.getId(), e);
            }
        }
    }

    /**
     * 检查是否应该应用活动奖励
     */
    private boolean shouldApplyActivityReward(Long userId, ReviewActivity activity) {
        // 检查用户是否参与了该活动
        return hasUserParticipated(userId, activity.getId());
    }

    /**
     * 应用活动奖励
     */
    private void applyActivityReward(Long userId, Long reviewId, ReviewActivity activity) {
        try {
            Map<String, Object> rules = objectMapper.readValue(activity.getRules(), Map.class);

            switch (activity.getActivityType()) {
                case "DOUBLE_POINTS":
                    applyDoublePointsReward(userId, reviewId, rules);
                    break;
                case "REVIEW_CONTEST":
                    trackReviewForContest(userId, reviewId, activity.getId());
                    break;
                case "MONTHLY_CHAMPION":
                    trackReviewForChampion(userId, reviewId, activity.getId());
                    break;
            }
        } catch (JsonProcessingException e) {
            log.error("解析活动奖励规则失败", e);
        }
    }

    /**
     * 应用双倍积分奖励
     */
    private void applyDoublePointsReward(Long userId, Long reviewId, Map<String, Object> rules) {
        Integer multiplier = (Integer) rules.getOrDefault("multiplier", 2);

        // 这里应该调用 ReviewIncentiveService 来发放额外积分
        // 简化实现，实际应该基于原始积分计算额外奖励
        int bonusPoints = 10 * (multiplier - 1); // 假设基础积分为10

        UserPoints userPoints = new UserPoints();
        userPoints.setUserId(userId);
        userPoints.setPoints(bonusPoints);
        userPoints.setSource("DOUBLE_POINTS_ACTIVITY");
        userPoints.setSourceId(reviewId);
        userPoints.setCreatedAt(LocalDateTime.now());
        userPoints.setExpiresAt(java.time.LocalDate.now().plusYears(1));

        userPointsRepository.insert(userPoints);

        log.info("为用户 {} 应用双倍积分奖励，额外积分：{}", userId, bonusPoints);
    }

    /**
     * 追踪竞赛评价
     */
    private void trackReviewForContest(Long userId, Long reviewId, Long activityId) {
        // 更新参与记录，记录评价提交
        List<ActivityParticipation> participations = activityParticipationRepository
            .findByActivityId(activityId).stream()
            .filter(p -> p.getUserId().equals(userId))
            .toList();

        if (!participations.isEmpty()) {
            ActivityParticipation participation = participations.get(0);
            participation.setNotes(participation.getNotes() +
                String.format(" | 提交评价ID：%d（%s）", reviewId, LocalDateTime.now()));
            activityParticipationRepository.updateById(participation);
        }

        log.info("记录竞赛评价 - 用户：{}，评价：{}，活动：{}", userId, reviewId, activityId);
    }

    /**
     * 追踪冠军评价
     */
    private void trackReviewForChampion(Long userId, Long reviewId, Long activityId) {
        // 更新参与记录，记录评价提交
        List<ActivityParticipation> participations = activityParticipationRepository
            .findByActivityId(activityId).stream()
            .filter(p -> p.getUserId().equals(userId))
            .toList();

        if (!participations.isEmpty()) {
            ActivityParticipation participation = participations.get(0);
            participation.setNotes(participation.getNotes() +
                String.format(" | 提交评价ID：%d（%s）", reviewId, LocalDateTime.now()));
            activityParticipationRepository.updateById(participation);
        }

        log.info("记录冠军评价 - 用户：{}，评价：{}，活动：{}", userId, reviewId, activityId);
    }

    /**
     * 转换为DTO
     */
    private ReviewActivityDTO convertToDTO(ReviewActivity entity) {
        ReviewActivityDTO dto = new ReviewActivityDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setActivityType(entity.getActivityType());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());

        // 解析规则
        try {
            if (entity.getRules() != null && !entity.getRules().isEmpty()) {
                dto.setRules(objectMapper.readValue(entity.getRules(), Map.class));
            }
        } catch (JsonProcessingException e) {
            log.error("解析活动规则失败", e);
        }

        // 设置状态
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(entity.getStartDate())) {
            dto.setStatus("UPCOMING");
        } else if (now.isAfter(entity.getEndDate())) {
            dto.setStatus("ENDED");
        } else {
            dto.setStatus("ACTIVE");
        }

        return dto;
    }

    /**
     * 转换为实体
     */
    private ReviewActivity convertToEntity(ReviewActivityDTO dto) {
        ReviewActivity entity = new ReviewActivity();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setActivityType(dto.getActivityType());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setIsActive(dto.getIsActive());

        // 序列化规则
        try {
            if (dto.getRules() != null) {
                entity.setRules(objectMapper.writeValueAsString(dto.getRules()));
            }
        } catch (JsonProcessingException e) {
            log.error("序列化活动规则失败", e);
        }

        return entity;
    }
}