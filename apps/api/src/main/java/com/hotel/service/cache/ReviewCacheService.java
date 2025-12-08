package com.hotel.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.review.ReviewResponse;
import com.hotel.entity.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 评价缓存服务
 * 用于缓存评价相关数据，提高查询性能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 缓存键前缀
    private static final String CACHE_PREFIX = "hotel:review:";

    // 缓存过期时间
    private static final Duration HOTEL_REVIEWS_TTL = Duration.ofMinutes(30); // 酒店评价缓存30分钟
    private static final Duration USER_REVIEWS_TTL = Duration.ofMinutes(15);   // 用户评价缓存15分钟
    private static final Duration REVIEW_DETAIL_TTL = Duration.ofHours(2);     // 评价详情缓存2小时
    private static final Duration REVIEW_STATS_TTL = Duration.ofMinutes(60);  // 评价统计缓存1小时

    /**
     * 获取酒店评价缓存键
     */
    private String getHotelReviewsKey(Long hotelId, int page, int size) {
        return String.format("%shotel:%d:reviews:%d:%d", CACHE_PREFIX, hotelId, page, size);
    }

    /**
     * 获取用户评价缓存键
     */
    private String getUserReviewsKey(Long userId, int page, int size) {
        return String.format("%suser:%d:reviews:%d:%d", CACHE_PREFIX, userId, page, size);
    }

    /**
     * 获取评价详情缓存键
     */
    private String getReviewDetailKey(Long reviewId) {
        return String.format("%sdetail:%d", CACHE_PREFIX, reviewId);
    }

    /**
     * 获取评价统计缓存键
     */
    private String getReviewStatsKey(Long hotelId) {
        return String.format("%sstats:%d", CACHE_PREFIX, hotelId);
    }

    /**
     * 缓存酒店评价列表
     */
    public void cacheHotelReviews(Long hotelId, int page, int size, List<Review> reviews) {
        try {
            String cacheKey = getHotelReviewsKey(hotelId, page, size);
            List<ReviewResponse> reviewResponses = reviews.stream()
                    .map(ReviewResponse::fromEntity)
                    .collect(Collectors.toList());

            String jsonValue = objectMapper.writeValueAsString(reviewResponses);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, HOTEL_REVIEWS_TTL);

            log.debug("缓存酒店评价列表: hotelId={}, page={}, size={}, count={}",
                    hotelId, page, size, reviews.size());
        } catch (Exception e) {
            log.error("缓存酒店评价列表失败", e);
        }
    }

    /**
     * 获取缓存中的酒店评价列表
     */
    public List<ReviewResponse> getCachedHotelReviews(Long hotelId, int page, int size) {
        try {
            String cacheKey = getHotelReviewsKey(hotelId, page, size);
            String jsonValue = (String) redisTemplate.opsForValue().get(cacheKey);

            if (jsonValue != null) {
                log.debug("从缓存中获取酒店评价列表: hotelId={}, page={}, size={}", hotelId, page, size);
                return objectMapper.readValue(jsonValue,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ReviewResponse.class));
            }
        } catch (Exception e) {
            log.error("获取缓存的酒店评价列表失败", e);
        }

        return null;
    }

    /**
     * 缓存用户评价列表
     */
    public void cacheUserReviews(Long userId, int page, int size, List<Review> reviews) {
        try {
            String cacheKey = getUserReviewsKey(userId, page, size);
            List<ReviewResponse> reviewResponses = reviews.stream()
                    .map(ReviewResponse::fromEntity)
                    .collect(Collectors.toList());

            String jsonValue = objectMapper.writeValueAsString(reviewResponses);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, USER_REVIEWS_TTL);

            log.debug("缓存用户评价列表: userId={}, page={}, size={}, count={}",
                    userId, page, size, reviews.size());
        } catch (Exception e) {
            log.error("缓存用户评价列表失败", e);
        }
    }

    /**
     * 获取缓存中的用户评价列表
     */
    public List<ReviewResponse> getCachedUserReviews(Long userId, int page, int size) {
        try {
            String cacheKey = getUserReviewsKey(userId, page, size);
            String jsonValue = (String) redisTemplate.opsForValue().get(cacheKey);

            if (jsonValue != null) {
                log.debug("从缓存中获取用户评价列表: userId={}, page={}, size={}", userId, page, size);
                return objectMapper.readValue(jsonValue,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ReviewResponse.class));
            }
        } catch (Exception e) {
            log.error("获取缓存的用户评价列表失败", e);
        }

        return null;
    }

    /**
     * 缓存评价详情
     */
    public void cacheReviewDetail(Long reviewId, Review review) {
        try {
            String cacheKey = getReviewDetailKey(reviewId);
            ReviewResponse reviewResponse = ReviewResponse.fromEntity(review);

            String jsonValue = objectMapper.writeValueAsString(reviewResponse);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, REVIEW_DETAIL_TTL);

            log.debug("缓存评价详情: reviewId={}", reviewId);
        } catch (Exception e) {
            log.error("缓存评价详情失败", e);
        }
    }

    /**
     * 获取缓存中的评价详情
     */
    public ReviewResponse getCachedReviewDetail(Long reviewId) {
        try {
            String cacheKey = getReviewDetailKey(reviewId);
            String jsonValue = (String) redisTemplate.opsForValue().get(cacheKey);

            if (jsonValue != null) {
                log.debug("从缓存中获取评价详情: reviewId={}", reviewId);
                return objectMapper.readValue(jsonValue, ReviewResponse.class);
            }
        } catch (Exception e) {
            log.error("获取缓存的评价详情失败", e);
        }

        return null;
    }

    /**
     * 缓存评价统计信息
     */
    public void cacheReviewStats(Long hotelId, ReviewStats stats) {
        try {
            String cacheKey = getReviewStatsKey(hotelId);
            String jsonValue = objectMapper.writeValueAsString(stats);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, REVIEW_STATS_TTL);

            log.debug("缓存评价统计: hotelId={}", hotelId);
        } catch (Exception e) {
            log.error("缓存评价统计失败", e);
        }
    }

    /**
     * 获取缓存中的评价统计信息
     */
    public ReviewStats getCachedReviewStats(Long hotelId) {
        try {
            String cacheKey = getReviewStatsKey(hotelId);
            String jsonValue = (String) redisTemplate.opsForValue().get(cacheKey);

            if (jsonValue != null) {
                log.debug("从缓存中获取评价统计: hotelId={}", hotelId);
                return objectMapper.readValue(jsonValue, ReviewStats.class);
            }
        } catch (Exception e) {
            log.error("获取缓存的评价统计失败", e);
        }

        return null;
    }

    /**
     * 清除酒店相关的所有评价缓存
     */
    public void evictHotelReviewsCache(Long hotelId) {
        try {
            // 使用模糊匹配删除相关缓存
            String pattern = String.format("%shotel:%d:reviews:*", CACHE_PREFIX, hotelId);
            redisTemplate.delete(redisTemplate.keys(pattern));

            // 删除统计缓存
            String statsKey = getReviewStatsKey(hotelId);
            redisTemplate.delete(statsKey);

            log.info("清除酒店评价缓存: hotelId={}", hotelId);
        } catch (Exception e) {
            log.error("清除酒店评价缓存失败", e);
        }
    }

    /**
     * 清除用户相关的所有评价缓存
     */
    public void evictUserReviewsCache(Long userId) {
        try {
            String pattern = String.format("%suser:%d:reviews:*", CACHE_PREFIX, userId);
            redisTemplate.delete(redisTemplate.keys(pattern));

            log.info("清除用户评价缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("清除用户评价缓存失败", e);
        }
    }

    /**
     * 清除单个评价的缓存
     */
    public void evictReviewCache(Long reviewId) {
        try {
            String detailKey = getReviewDetailKey(reviewId);
            redisTemplate.delete(detailKey);

            log.info("清除评价详情缓存: reviewId={}", reviewId);
        } catch (Exception e) {
            log.error("清除评价详情缓存失败", e);
        }
    }

    /**
     * 评价统计信息内部类
     */
    public static class ReviewStats {
        private double averageRating;
        private int totalReviews;
        private int fiveStarCount;
        private int fourStarCount;
        private int threeStarCount;
        private int twoStarCount;
        private int oneStarCount;

        // 构造函数、getter和setter方法
        public ReviewStats() {}

        public ReviewStats(double averageRating, int totalReviews,
                          int fiveStarCount, int fourStarCount, int threeStarCount,
                          int twoStarCount, int oneStarCount) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
            this.fiveStarCount = fiveStarCount;
            this.fourStarCount = fourStarCount;
            this.threeStarCount = threeStarCount;
            this.twoStarCount = twoStarCount;
            this.oneStarCount = oneStarCount;
        }

        // Getters and Setters
        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
        public int getTotalReviews() { return totalReviews; }
        public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
        public int getFiveStarCount() { return fiveStarCount; }
        public void setFiveStarCount(int fiveStarCount) { this.fiveStarCount = fiveStarCount; }
        public int getFourStarCount() { return fourStarCount; }
        public void setFourStarCount(int fourStarCount) { this.fourStarCount = fourStarCount; }
        public int getThreeStarCount() { return threeStarCount; }
        public void setThreeStarCount(int threeStarCount) { this.threeStarCount = threeStarCount; }
        public int getTwoStarCount() { return twoStarCount; }
        public void setTwoStarCount(int twoStarCount) { this.twoStarCount = twoStarCount; }
        public int getOneStarCount() { return oneStarCount; }
        public void setOneStarCount(int oneStarCount) { this.oneStarCount = oneStarCount; }
    }
}