package com.hotel.service;

import com.hotel.dto.review.ReviewRequest;
import com.hotel.dto.review.ReviewResponse;
import com.hotel.entity.Review;
import com.hotel.repository.ReviewRepository;
import com.hotel.repository.OrderRepository;
import com.hotel.util.HtmlSanitizer;
import com.hotel.service.cache.ReviewCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final FileService fileService;
    private final ReviewCacheService reviewCacheService;

    // 敏感词列表 - 实际项目中应该从数据库或配置文件加载
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
        "垃圾", "骗子", "差劲", "恶心", "糟糕", "退钱"
    );

    @Transactional
    public ReviewResponse submitReview(Long userId, ReviewRequest reviewRequest) {
        // 验证评分有效性
        validateRatings(reviewRequest);

        // 验证订单是否存在且属于当前用户
        Optional<com.hotel.entity.Order> orderOpt = orderRepository.findById(reviewRequest.getOrderId());
        if (orderOpt.isEmpty() || !orderOpt.get().getUserId().equals(userId)) {
            throw new com.hotel.exception.ResourceNotFoundException("订单不存在或无权限评价");
        }

        com.hotel.entity.Order order = orderOpt.get();

        // 验证订单状态是否为已完成
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new com.hotel.exception.BusinessException("只有已完成的订单才能评价");
        }

        // 检查是否已经评价过
        if (reviewRepository.existsByOrderIdAndUserId(reviewRequest.getOrderId(), userId)) {
            throw new com.hotel.exception.BusinessException("该订单已经评价过了");
        }

        // 处理敏感词过滤
        // 防止XSS攻击：先清理HTML内容，再过滤敏感词
        String sanitizedComment = HtmlSanitizer.sanitize(reviewRequest.getComment());
        String filteredComment = filterSensitiveWords(sanitizedComment);

        // 处理图片上传
        String imagesJson = "";
        if (reviewRequest.getImages() != null && !reviewRequest.getImages().isEmpty()) {
            // 验证图片URL有效性（实际应该在上传时处理）
            imagesJson = String.join(",", reviewRequest.getImages());
        }

        // 创建评价记录
        Review review = new Review();
        review.setUserId(userId);
        review.setOrderId(reviewRequest.getOrderId());
        review.setRoomId(order.getRoomId());
        review.setHotelId(order.getHotelId());
        review.setOverallRating(reviewRequest.getOverallRating());
        review.setCleanlinessRating(reviewRequest.getCleanlinessRating());
        review.setServiceRating(reviewRequest.getServiceRating());
        review.setFacilitiesRating(reviewRequest.getFacilitiesRating());
        review.setLocationRating(reviewRequest.getLocationRating());
        review.setComment(filteredComment);
        review.setImages(imagesJson);
        review.setIsAnonymous(reviewRequest.getIsAnonymous() != null ? reviewRequest.getIsAnonymous() : false);
        review.setStatus("PENDING"); // 需要审核
        review.setCreatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        log.info("用户 {} 成功提交订单 {} 的评价", userId, reviewRequest.getOrderId());

        // 清除相关缓存
        reviewCacheService.evictHotelReviewsCache(review.getHotelId());
        reviewCacheService.evictUserReviewsCache(userId);

        return ReviewResponse.fromEntity(review);
    }

    /**
     * 敏感词过滤
     */
    private String filterSensitiveWords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        String filtered = content;
        for (String word : SENSITIVE_WORDS) {
            filtered = filtered.replace(word, "***");
        }

        return filtered;
    }

    /**
     * 获取用户的所有评价
     */
    public List<ReviewResponse> getUserReviews(Long userId) {
        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return reviews.stream()
                     .map(ReviewResponse::fromEntity)
                     .toList();
    }

    /**
     * 获取酒店的评价
     */
    public List<ReviewResponse> getHotelReviews(Long hotelId) {
        List<Review> reviews = reviewRepository.findByHotelIdAndApprovedStatus(hotelId);
        return reviews.stream()
                     .map(ReviewResponse::fromEntity)
                     .toList();
    }

    /**
     * 验证评分是否在有效范围内
     */
    public boolean isValidRating(Integer rating) {
        return rating != null && rating >= 1 && rating <= 5;
    }

    /**
     * 验证评价请求中的评分
     */
    private void validateRatings(ReviewRequest reviewRequest) {
        if (!isValidRating(reviewRequest.getOverallRating())) {
            throw new com.hotel.exception.BusinessException("总体评分必须在1-5星之间");
        }
        if (!isValidRating(reviewRequest.getCleanlinessRating())) {
            throw new com.hotel.exception.BusinessException("清洁度评分必须在1-5星之间");
        }
        if (!isValidRating(reviewRequest.getServiceRating())) {
            throw new com.hotel.exception.BusinessException("服务评分必须在1-5星之间");
        }
        if (!isValidRating(reviewRequest.getFacilitiesRating())) {
            throw new com.hotel.exception.BusinessException("设施评分必须在1-5星之间");
        }
        if (!isValidRating(reviewRequest.getLocationRating())) {
            throw new com.hotel.exception.BusinessException("位置评分必须在1-5星之间");
        }
    }

    /**
     * 检查用户是否可以评价指定订单
     */
    public boolean canReviewOrder(Long userId, Long orderId) {
        // 检查订单是否存在
        Optional<com.hotel.entity.Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        com.hotel.entity.Order order = orderOpt.get();

        // 检查订单是否属于当前用户
        if (!order.getUserId().equals(userId)) {
            return false;
        }

        // 检查订单状态是否为已完成
        if (!"COMPLETED".equals(order.getStatus())) {
            return false;
        }

        // 检查是否已经评价过
        if (reviewRepository.existsByOrderIdAndUserId(orderId, userId)) {
            return false;
        }

        return true;
    }
}