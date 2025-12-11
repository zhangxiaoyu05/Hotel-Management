package com.hotel.service;

import com.hotel.dto.review.ReviewStatisticsResponse;
import com.hotel.entity.Review;
import com.hotel.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewStatisticsService {

    private final ReviewRepository reviewRepository;

    /**
     * 获取酒店评价统计信息（带缓存）
     */
    @Cacheable(value = "hotelReviewStatistics", key = "#hotelId")
    public ReviewStatisticsResponse getHotelStatistics(Long hotelId) {
        log.info("正在计算酒店 {} 的评价统计信息", hotelId);

        // 获取所有已审核通过的评价
        List<Review> approvedReviews = reviewRepository.findByHotelIdAndApprovedStatus(hotelId);

        if (approvedReviews.isEmpty()) {
            log.info("酒店 {} 暂无已审核通过的评价", hotelId);
            return createEmptyStatistics(hotelId);
        }

        // 计算各维度评分
        RatingAverages ratingAverages = calculateRatingAverages(approvedReviews);

        // 计算其他统计数据
        Long reviewsWithImages = countReviewsWithImages(approvedReviews);
        Double averageCommentLength = calculateAverageCommentLength(approvedReviews);

        // 构建响应对象
        ReviewStatisticsResponse response = buildStatisticsResponse(
            hotelId,
            approvedReviews.size(),
            ratingAverages,
            calculateRatingDistribution(approvedReviews),
            reviewsWithImages,
            averageCommentLength
        );

        log.info("酒店 {} 评价统计计算完成，总评价数: {}, 平均分: {}",
            hotelId, approvedReviews.size(), ratingAverages.overall);

        return response;
    }

    /**
     * 清除酒店统计缓存
     */
    @CacheEvict(value = "hotelReviewStatistics", key = "#hotelId")
    public void evictStatisticsCache(Long hotelId) {
        log.info("已清除酒店 {} 的评价统计缓存", hotelId);
    }

    /**
     * 计算平均评分
     */
    private Double calculateAverageRating(List<Review> reviews, java.util.function.Function<Review, Integer> ratingExtractor) {
        if (reviews.isEmpty()) {
            return 0.0;
        }

        double sum = reviews.stream()
            .mapToDouble(review -> ratingExtractor.apply(review))
            .sum();

        return Math.round(sum / reviews.size() * 10.0) / 10.0; // 保留一位小数
    }

    /**
     * 计算评分分布
     */
    private ReviewStatisticsResponse.RatingDistribution calculateRatingDistribution(List<Review> reviews) {
        Map<Integer, Long> ratingCount = reviews.stream()
            .collect(Collectors.groupingBy(Review::getOverallRating, Collectors.counting()));

        return new ReviewStatisticsResponse.RatingDistribution(
            ratingCount.getOrDefault(5, 0L),
            ratingCount.getOrDefault(4, 0L),
            ratingCount.getOrDefault(3, 0L),
            ratingCount.getOrDefault(2, 0L),
            ratingCount.getOrDefault(1, 0L)
        );
    }

    /**
     * 统计带图片的评价数量
     */
    private Long countReviewsWithImages(List<Review> reviews) {
        return reviews.stream()
            .filter(review -> review.getImages() != null && !review.getImages().trim().isEmpty())
            .count();
    }

    /**
     * 计算平均评价长度
     */
    private Double calculateAverageCommentLength(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return 0.0;
        }

        int totalLength = reviews.stream()
            .filter(review -> review.getComment() != null)
            .mapToInt(review -> review.getComment().length())
            .sum();

        long commentCount = reviews.stream()
            .filter(review -> review.getComment() != null)
            .count();

        return commentCount == 0 ? 0.0 : Math.round((double) totalLength / commentCount);
    }

    /**
     * 创建空的统计响应
     */
    private ReviewStatisticsResponse createEmptyStatistics(Long hotelId) {
        ReviewStatisticsResponse response = new ReviewStatisticsResponse();
        response.setHotelId(hotelId);
        response.setTotalReviews(0L);
        response.setOverallRating(0.0);
        response.setCleanlinessRating(0.0);
        response.setServiceRating(0.0);
        response.setFacilitiesRating(0.0);
        response.setLocationRating(0.0);
        response.setRatingDistribution(new ReviewStatisticsResponse.RatingDistribution(0L, 0L, 0L, 0L, 0L));
        response.setReviewsWithImages(0L);
        response.setAverageCommentLength(0.0);
        return response;
    }

    /**
     * 批量获取多个酒店的统计信息
     */
    public Map<Long, ReviewStatisticsResponse> getBatchStatistics(List<Long> hotelIds) {
        Map<Long, ReviewStatisticsResponse> results = new HashMap<>();

        for (Long hotelId : hotelIds) {
            try {
                results.put(hotelId, getHotelStatistics(hotelId));
            } catch (Exception e) {
                log.error("获取酒店 {} 统计信息失败: {}", hotelId, e.getMessage());
                results.put(hotelId, createEmptyStatistics(hotelId));
            }
        }

        return results;
    }

    /**
     * 获取简单的评分统计（用于列表页展示）
     */
    @Cacheable(value = "hotelSimpleStats", key = "#hotelId")
    public Map<String, Object> getSimpleStatistics(Long hotelId) {
        List<Review> approvedReviews = reviewRepository.findByHotelIdAndApprovedStatus(hotelId);

        if (approvedReviews.isEmpty()) {
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("totalReviews", 0L);
            emptyStats.put("overallRating", 0.0);
            return emptyStats;
        }

        Double overallRating = calculateAverageRating(approvedReviews, Review::getOverallRating);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReviews", (long) approvedReviews.size());
        stats.put("overallRating", overallRating);

        return stats;
    }

    /**
     * 计算各维度评分的平均值
     */
    private RatingAverages calculateRatingAverages(List<Review> reviews) {
        return new RatingAverages(
            calculateAverageRating(reviews, Review::getOverallRating),
            calculateAverageRating(reviews, Review::getCleanlinessRating),
            calculateAverageRating(reviews, Review::getServiceRating),
            calculateAverageRating(reviews, Review::getFacilitiesRating),
            calculateAverageRating(reviews, Review::getLocationRating)
        );
    }

    /**
     * 构建统计响应对象
     */
    private ReviewStatisticsResponse buildStatisticsResponse(
            Long hotelId,
            int totalReviews,
            RatingAverages ratingAverages,
            ReviewStatisticsResponse.RatingDistribution distribution,
            Long reviewsWithImages,
            Double averageCommentLength) {

        ReviewStatisticsResponse response = new ReviewStatisticsResponse();
        response.setHotelId(hotelId);
        response.setTotalReviews((long) totalReviews);
        response.setOverallRating(ratingAverages.overall);
        response.setCleanlinessRating(ratingAverages.cleanliness);
        response.setServiceRating(ratingAverages.service);
        response.setFacilitiesRating(ratingAverages.facilities);
        response.setLocationRating(ratingAverages.location);
        response.setRatingDistribution(distribution);
        response.setReviewsWithImages(reviewsWithImages);
        response.setAverageCommentLength(averageCommentLength);

        return response;
    }

    /**
     * 评分平均值内部类
     */
    private static class RatingAverages {
        public final Double overall;
        public final Double cleanliness;
        public final Double service;
        public final Double facilities;
        public final Double location;

        public RatingAverages(Double overall, Double cleanliness, Double service,
                            Double facilities, Double location) {
            this.overall = overall;
            this.cleanliness = cleanliness;
            this.service = service;
            this.facilities = facilities;
            this.location = location;
        }
    }
}