package com.hotel.aspect;

import com.hotel.annotation.RateLimit;
import com.hotel.dto.ApiResponse;
import com.hotel.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * API 速率限制切面
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private final RedisTemplate<String, String> redisTemplate;

    // Lua 脚本实现原子性计数器
    private static final String RATE_LIMIT_SCRIPT =
            "local key = KEYS[1] " +
            "local period = tonumber(ARGV[1]) " +
            "local limit = tonumber(ARGV[2]) " +
            "local current_time = tonumber(ARGV[3]) " +
            "local window_start = current_time - period " +

            "redis.call('zremrangebyscore', key, '-inf', window_start) " +
            "local current_requests = redis.call('zcard', key) " +

            "if current_requests < limit then " +
            "    redis.call('zadd', key, current_time, current_time) " +
            "    redis.call('expire', key, period) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    private final RedisScript<Long> redisScript;

    public RateLimitAspect(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
    }

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 构建限制键
        String limitKey = buildLimitKey(rateLimit);

        // 检查速率限制
        boolean allowed = isRequestAllowed(limitKey, rateLimit.period(), rateLimit.limit());

        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}, limit: {}/{}s",
                    limitKey, rateLimit.limit(), rateLimit.period());

            return ApiResponse.error(rateLimit.message());
        }

        // 执行原方法
        return joinPoint.proceed();
    }

    /**
     * 构建限制键
     */
    private String buildLimitKey(RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder("rate_limit:");

        // 添加自定义前缀
        if (!rateLimit.prefix().isEmpty()) {
            keyBuilder.append(rateLimit.prefix()).append(":");
        }

        // 添加方法标识
        String methodName = "";
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                methodName = request.getMethod() + ":" + request.getRequestURI();
            }
        } catch (Exception e) {
            log.warn("Failed to get method name for rate limit", e);
        }

        keyBuilder.append(methodName);

        // 根据限制类型添加标识
        switch (rateLimit.type()) {
            case IP:
                String ip = getClientIp();
                keyBuilder.append(":ip:").append(ip);
                break;
            case USER:
                String userId = getCurrentUserId();
                keyBuilder.append(":user:").append(userId != null ? userId : "anonymous");
                break;
            case GLOBAL:
                // 全局限制不需要额外标识
                break;
        }

        return keyBuilder.toString();
    }

    /**
     * 检查请求是否被允许
     */
    private boolean isRequestAllowed(String key, int period, int limit) {
        try {
            Long result = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(key),
                    String.valueOf(period),
                    String.valueOf(limit),
                    String.valueOf(System.currentTimeMillis() / 1000)
            );

            return result != null && result == 1;
        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}", key, e);
            // 发生异常时允许请求通过（避免影响正常业务）
            return true;
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return IpUtil.getClientIp(request);
            }
        } catch (Exception e) {
            log.warn("Failed to get client IP", e);
        }
        return "unknown";
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.warn("Failed to get current user ID", e);
        }
        return null;
    }
}