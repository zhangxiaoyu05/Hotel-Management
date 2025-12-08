package com.hotel.service;

import com.hotel.dto.review.admin.BatchModerationRequest;
import com.hotel.dto.review.admin.ReviewModerationRequest;
import com.hotel.entity.Review;
import com.hotel.entity.ReviewModerationLog;
import com.hotel.repository.ReviewModerationLogRepository;
import com.hotel.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewModerationService {

    private final ReviewRepository reviewRepository;
    private final ReviewModerationLogRepository moderationLogRepository;
    private final NotificationService notificationService;

    @Transactional
    public Review moderateReview(Long reviewId, ReviewModerationRequest request, Long adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在: " + reviewId));

        String oldStatus = review.getStatus();
        String newStatus = determineNewStatus(request.getAction(), oldStatus);

        review.setStatus(newStatus);
        reviewRepository.save(review);

        // 记录审核日志
        ReviewModerationLog log = new ReviewModerationLog();
        log.setReviewId(reviewId);
        log.setAdminId(adminId);
        log.setAction(request.getAction());
        log.setReason(request.getReason());
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setCreatedAt(LocalDateTime.now());
        moderationLogRepository.save(log);

        // 发送通知（如果需要）
        if ("APPROVED".equals(newStatus) || "REJECTED".equals(newStatus)) {
            sendNotificationToUser(review, newStatus, request.getReason());
        }

        log.info("管理员 {} 对评价 {} 执行了 {} 操作，状态从 {} 变更为 {}",
                adminId, reviewId, request.getAction(), oldStatus, newStatus);

        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Review> batchModerateReviews(BatchModerationRequest request, Long adminId) {
        List<Review> reviews = reviewRepository.findAllById(request.getReviewIds());

        if (reviews.size() != request.getReviewIds().size()) {
            throw new RuntimeException("部分评价不存在");
        }

        // 先保存所有评价的更新
        List<Review> updatedReviews = new ArrayList<>();
        List<ReviewModerationLog> logs = new ArrayList<>();

        for (Review review : reviews) {
            String oldStatus = review.getStatus();
            String newStatus = determineNewStatus(request.getAction(), oldStatus);

            review.setStatus(newStatus);
            updatedReviews.add(review);

            // 创建审核日志
            ReviewModerationLog log = new ReviewModerationLog();
            log.setReviewId(review.getId());
            log.setAdminId(adminId);
            log.setAction(request.getAction());
            log.setReason(request.getReason() + " (批量操作)");
            log.setOldStatus(oldStatus);
            log.setNewStatus(newStatus);
            log.setCreatedAt(LocalDateTime.now());
            logs.add(log);
        }

        // 批量保存评价
        List<Review> savedReviews = reviewRepository.saveAll(updatedReviews);

        // 批量保存日志
        moderationLogRepository.saveAll(logs);

        // 发送通知（异步处理，不影响事务）
        for (int i = 0; i < savedReviews.size(); i++) {
            Review review = savedReviews.get(i);
            String newStatus = review.getStatus();

            if ("APPROVED".equals(newStatus) || "REJECTED".equals(newStatus)) {
                // 异步发送通知，避免影响主事务
                try {
                    sendNotificationToUser(review, newStatus, request.getReason());
                } catch (Exception e) {
                    log.warn("发送通知失败，但不影响批量操作: {}", e.getMessage());
                }
            }
        }

        return savedReviews;
    }

    public Page<ReviewModerationLog> getModerationLogs(Long reviewId, Long adminId,
                                                      String action, LocalDateTime startDate,
                                                      LocalDateTime endDate, Pageable pageable) {
        return moderationLogRepository.findLogsWithFilters(
                reviewId, adminId, action, startDate, endDate, pageable);
    }

    public List<Review> getPendingReviews() {
        return reviewRepository.findPendingReviews();
    }

    private String determineNewStatus(String action, String currentStatus) {
        switch (action) {
            case "APPROVE":
                return "APPROVED";
            case "REJECT":
                return "REJECTED";
            case "MARK":
                return currentStatus; // MARK操作不改变状态
            case "HIDE":
                return "HIDDEN";
            case "DELETE":
                return "DELETED";
            default:
                throw new RuntimeException("未知的操作类型: " + action);
        }
    }

    private void sendNotificationToUser(Review review, String status, String reason) {
        String title = "";
        String content = "";

        if ("APPROVED".equals(status)) {
            title = "评价审核通过";
            content = "您提交的评价已通过审核，感谢您的反馈！";
        } else if ("REJECTED".equals(status)) {
            title = "评价审核未通过";
            content = "您提交的评价未通过审核。原因：" + reason;
        }

        try {
            notificationService.sendNotification(review.getUserId(), title, content);
        } catch (Exception e) {
            log.error("发送通知失败", e);
        }
    }
}