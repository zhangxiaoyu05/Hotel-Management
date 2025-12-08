package com.hotel.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 频率限制服务
 * 用于防止敏感操作的滥用
 */
@Slf4j
@Service
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    // 不同操作的频率限制配置
    private static final int BATCH_OPERATION_LIMIT = 5; // 批量操作：每5分钟最多5次
    private static final int MODERATION_LIMIT = 50; // 单个审核：每分钟最多50次
    private static final int REPLY_LIMIT = 20; // 回复操作：每分钟最多20次

    private static final Duration BATCH_WINDOW = Duration.ofMinutes(5);
    private static final Duration MODERATION_WINDOW = Duration.ofMinutes(1);
    private static final Duration REPLY_WINDOW = Duration.ofMinutes(1);

    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查批量操作频率限制
     */
    public boolean checkBatchOperationLimit(Long adminId) {
        String key = "rate_limit:batch:" + adminId;
        return checkRateLimit(key, BATCH_OPERATION_LIMIT, BATCH_WINDOW);
    }

    /**
     * 检查审核操作频率限制
     */
    public boolean checkModerationLimit(Long adminId) {
        String key = "rate_limit:moderation:" + adminId;
        return checkRateLimit(key, MODERATION_LIMIT, MODERATION_WINDOW);
    }

    /**
     * 检查回复操作频率限制
     */
    public boolean checkReplyLimit(Long adminId) {
        String key = "rate_limit:reply:" + adminId;
        return checkRateLimit(key, REPLY_LIMIT, REPLY_WINDOW);
    }

    /**
     * 通用的频率限制检查
     */
    private boolean checkRateLimit(String key, int limit, Duration window) {
        try {
            String currentCount = redisTemplate.opsForValue().get(key);

            if (currentCount == null) {
                // 第一次操作，设置计数器
                redisTemplate.opsForValue().set(key, "1", window);
                log.debug("频率限制检查通过 - Key: {}, Count: 1/{}", key, limit);
                return true;
            } else {
                int count = Integer.parseInt(currentCount);

                if (count >= limit) {
                    log.warn("频率限制触发 - Key: {}, Count: {}/{}", key, count, limit);
                    return false;
                } else {
                    // 增加计数器，不重置过期时间
                    redisTemplate.opsForValue().increment(key);
                    log.debug("频率限制检查通过 - Key: {}, Count: {}/{}", key, count + 1, limit);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("频率限制检查失败，默认通过", e);
            // 如果Redis出现异常，默认通过，避免影响主业务
            return true;
        }
    }

    /**
     * 记录操作（用于审计）
     */
    public void recordOperation(Long adminId, String operation, String details) {
        try {
            String auditKey = "audit:admin:" + adminId;
            String auditRecord = String.format("%s|%s|%d", operation, details, System.currentTimeMillis());

            // 记录最近100次操作，用于审计
            redisTemplate.opsForList().rightPush(auditKey, auditRecord);
            redisTemplate.opsForList().trim(auditKey, 0, 99);
            redisTemplate.expire(auditKey, Duration.ofDays(7));

        } catch (Exception e) {
            log.error("记录操作审计失败", e);
        }
    }

    /**
     * 获取剩余操作次数
     */
    public int getRemainingOperations(Long adminId, String operationType) {
        try {
            String key = "rate_limit:" + operationType + ":" + adminId;
            String currentCount = redisTemplate.opsForValue().get(key);

            if (currentCount == null) {
                return getLimitByType(operationType);
            } else {
                int count = Integer.parseInt(currentCount);
                return Math.max(0, getLimitByType(operationType) - count);
            }
        } catch (Exception e) {
            log.error("获取剩余操作次数失败", e);
            return getLimitByType(operationType);
        }
    }

    private int getLimitByType(String operationType) {
        switch (operationType) {
            case "batch": return BATCH_OPERATION_LIMIT;
            case "moderation": return MODERATION_LIMIT;
            case "reply": return REPLY_LIMIT;
            default: return 10;
        }
    }

    /**
     * 重置用户频率限制（管理员功能）
     */
    public void resetUserRateLimit(Long adminId, String operationType) {
        try {
            String key = "rate_limit:" + operationType + ":" + adminId;
            redisTemplate.delete(key);
            log.info("已重置用户 {} 的 {} 操作频率限制", adminId, operationType);
        } catch (Exception e) {
            log.error("重置频率限制失败", e);
        }
    }
}