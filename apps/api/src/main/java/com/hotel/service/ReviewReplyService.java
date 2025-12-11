package com.hotel.service;

import com.hotel.dto.review.admin.ReviewReplyRequest;
import com.hotel.entity.Review;
import com.hotel.entity.ReviewReply;
import com.hotel.repository.ReviewReplyRepository;
import com.hotel.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewReplyService {

    private final com.hotel.repository.ReviewReplyRepository reviewReplyRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReviewReply createReply(Long reviewId, ReviewReplyRequest request, Long adminId) {
        Review review = reviewRepository.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在: " + reviewId);
        }

        if (!"APPROVED".equals(review.getStatus())) {
            throw new RuntimeException("只能回复已审核通过的评价");
        }

        ReviewReply reply = new ReviewReply();
        reply.setReviewId(reviewId);
        reply.setAdminId(adminId);
        reply.setContent(sanitizeContent(request.getContent()));
        reply.setStatus(request.getStatus());

        reviewReplyRepository.insert(reply);
        ReviewReply savedReply = reply;

        if ("PUBLISHED".equals(request.getStatus())) {
            sendReplyNotification(review, reply);
        }

        log.info("管理员 {} 对评价 {} 创建了回复，状态: {}", adminId, reviewId, request.getStatus());

        return savedReply;
    }

    @Transactional
    public ReviewReply updateReply(Long reviewId, Long replyId, ReviewReplyRequest request, Long adminId) {
        ReviewReply reply = reviewReplyRepository.selectById(replyId);
        if (reply == null) {
            throw new RuntimeException("回复不存在: " + replyId);
        }

        if (!reply.getReviewId().equals(reviewId)) {
            throw new RuntimeException("回复不属于指定评价");
        }

        reply.setContent(sanitizeContent(request.getContent()));
        reply.setStatus(request.getStatus());

        reviewReplyRepository.updateById(reply);
        ReviewReply updatedReply = reply;

        if ("PUBLISHED".equals(request.getStatus()) && !"PUBLISHED".equals(reply.getStatus())) {
            sendReplyNotification(reviewRepository.selectById(reviewId), updatedReply);
        }

        log.info("管理员 {} 更新了评价 {} 的回复 {}", adminId, reviewId, replyId);

        return updatedReply;
    }

    public List<ReviewReply> getReviewReplies(Long reviewId) {
        return reviewReplyRepository.findByReviewIdOrderByCreatedAtDesc(reviewId);
    }

    public IPage<ReviewReply> getAllReplies(String status, Long adminId,
                                         LocalDateTime startDate, LocalDateTime endDate,
                                         Page<ReviewReply> pageable) {
        return reviewReplyRepository.findRepliesWithFilters(
                pageable, status, adminId, startDate, endDate);
    }

    @Transactional
    public void deleteReply(Long reviewId, Long replyId, Long adminId) {
        ReviewReply reply = reviewReplyRepository.selectById(replyId);
        if (reply == null) {
            throw new RuntimeException("回复不存在: " + replyId);
        }

        if (!reply.getReviewId().equals(reviewId)) {
            throw new RuntimeException("回复不属于指定评价");
        }

        reviewReplyRepository.deleteById(replyId);

        log.info("管理员 {} 删除了评价 {} 的回复 {}", adminId, reviewId, replyId);
    }

    private String sanitizeContent(String content) {
        // XSS防护和内容过滤
        if (content == null) {
            return "";
        }

        // 基本的XSS防护
        content = content.replace("<script>", "&lt;script&gt;")
                        .replace("</script>", "&lt;/script&gt;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("javascript:", "")
                        .replace("vbscript:", "")
                        .replace("onload=", "")
                        .replace("onerror=", "");

        return content.trim();
    }

    private void sendReplyNotification(Review review, ReviewReply reply) {
        String title = "管理员回复了您的评价";
        String content = "管理员对您的评价进行了回复：" +
                        (reply.getContent().length() > 50 ?
                         reply.getContent().substring(0, 50) + "..." :
                         reply.getContent());

        try {
            notificationService.sendNotification(review.getUserId(), title, content);
        } catch (Exception e) {
            log.error("发送回复通知失败", e);
        }
    }
}