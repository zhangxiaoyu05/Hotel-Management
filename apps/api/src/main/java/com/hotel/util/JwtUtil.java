package com.hotel.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 工具类
 * 用于生成和验证 JWT 令牌
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:hotelManagementSecretKey2024VeryLongSecretKeyForHS512Algorithm}")
    private String secret;

    @Value("${jwt.expiration:86400}") // 默认24小时
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800}") // 默认7天
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 从token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从token中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从token中获取指定声明
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从token中获取所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查token是否过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 为用户生成token
     */
    public String generateToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return createToken(claims, username, expiration);
    }

    /**
     * 为用户生成刷新token
     */
    public String generateRefreshToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        return createToken(claims, username, refreshExpiration);
    }

    /**
     * 创建token
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 验证token
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = getUsernameFromToken(token);
            return (tokenUsername.equals(username) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 验证token是否有效（不需要用户名）
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从token中获取用户角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 检查是否是刷新token
     */
    public Boolean isRefreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return "refresh".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取token剩余有效时间（秒）
     */
    public Long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return (expiration.getTime() - System.currentTimeMillis()) / 1000;
        } catch (JwtException | IllegalArgumentException e) {
            return 0L;
        }
    }

    /**
     * 解析token获取所有信息
     */
    public TokenInfo parseToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return new TokenInfo(
                    claims.getSubject(),
                    claims.get("userId", Long.class),
                    claims.get("role", String.class),
                    claims.getIssuedAt(),
                    claims.getExpiration()
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    /**
     * Token信息封装类
     */
    public static class TokenInfo {
        private final String username;
        private final Long userId;
        private final String role;
        private final Date issuedAt;
        private final Date expiration;

        public TokenInfo(String username, Long userId, String role, Date issuedAt, Date expiration) {
            this.username = username;
            this.userId = userId;
            this.role = role;
            this.issuedAt = issuedAt;
            this.expiration = expiration;
        }

        // Getters
        public String getUsername() { return username; }
        public Long getUserId() { return userId; }
        public String getRole() { return role; }
        public Date getIssuedAt() { return issuedAt; }
        public Date getExpiration() { return expiration; }
        public Boolean isExpired() { return expiration.before(new Date()); }
    }
}