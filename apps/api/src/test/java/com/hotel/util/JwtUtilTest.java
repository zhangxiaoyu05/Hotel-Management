package com.hotel.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "hotelManagementSecretKey2024VeryLongSecretKeyForHS512Algorithm";
    private static final String TEST_USERNAME = "testuser";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_ROLE = "USER";

    @BeforeEach
    void setUp() {
        // 使用反射设置私有字段
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800L);
    }

    @Test
    void testGenerateToken_Success() {
        // When
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT应该有3个部分
    }

    @Test
    void testGenerateRefreshToken_Success() {
        // When
        String refreshToken = jwtUtil.generateRefreshToken(TEST_USERNAME, TEST_USER_ID);

        // Then
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertTrue(refreshToken.split("\\.").length == 3);
    }

    @Test
    void testGetUsernameFromToken_Success() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // When
        String username = jwtUtil.getUsernameFromToken(token);

        // Then
        assertEquals(TEST_USERNAME, username);
    }

    @Test
    void testGetUserIdFromToken_Success() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // When
        Long userId = jwtUtil.getUserIdFromToken(token);

        // Then
        assertEquals(TEST_USER_ID, userId);
    }

    @Test
    void testGetRoleFromToken_Success() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // When
        String role = jwtUtil.getRoleFromToken(token);

        // Then
        assertEquals(TEST_ROLE, role);
    }

    @Test
    void testGetExpirationDateFromToken_Success() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // When
        Date expirationDate = jwtUtil.getExpirationDateFromToken(token);

        // Then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void testValidateToken_WithValidToken_ReturnsTrue() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // When
        Boolean isValid = jwtUtil.validateToken(token, TEST_USERNAME);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_WithInvalidUsername_ReturnsFalse() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // When
        Boolean isValid = jwtUtil.validateToken(token, "differentuser");

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_WithMalformedToken_ReturnsFalse() {
        // Given
        String malformedToken = "invalid.token.here";

        // When
        Boolean isValid = jwtUtil.validateToken(malformedToken, TEST_USERNAME);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_WithoutUsername_ReturnsTrueForValidToken() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // When
        Boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_WithoutUsername_ReturnsFalseForInvalidToken() {
        // Given
        String malformedToken = "invalid.token.here";

        // When
        Boolean isValid = jwtUtil.validateToken(malformedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsRefreshToken_WithRefreshToken_ReturnsTrue() {
        // Given
        String refreshToken = jwtUtil.generateRefreshToken(TEST_USERNAME, TEST_USER_ID);

        // When
        Boolean isRefresh = jwtUtil.isRefreshToken(refreshToken);

        // Then
        assertTrue(isRefresh);
    }

    @Test
    void testIsRefreshToken_WithAccessToken_ReturnsFalse() {
        // Given
        String accessToken = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // When
        Boolean isRefresh = jwtUtil.isRefreshToken(accessToken);

        // Then
        assertFalse(isRefresh);
    }

    @Test
    void testGetTokenRemainingTime_WithValidToken_ReturnsPositiveTime() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // When
        Long remainingTime = jwtUtil.getTokenRemainingTime(token);

        // Then
        assertNotNull(remainingTime);
        assertTrue(remainingTime > 0);
        assertTrue(remainingTime <= 86400); // 不应该超过24小时
    }

    @Test
    void testGetTokenRemainingTime_WithInvalidToken_ReturnsZero() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        Long remainingTime = jwtUtil.getTokenRemainingTime(invalidToken);

        // Then
        assertEquals(0L, remainingTime);
    }

    @Test
    void testParseToken_WithValidToken_ReturnsTokenInfo() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);

        // When
        JwtUtil.TokenInfo tokenInfo = jwtUtil.parseToken(token);

        // Then
        assertNotNull(tokenInfo);
        assertEquals(TEST_USERNAME, tokenInfo.getUsername());
        assertEquals(TEST_USER_ID, tokenInfo.getUserId());
        assertEquals(TEST_ROLE, tokenInfo.getRole());
        assertNotNull(tokenInfo.getIssuedAt());
        assertNotNull(tokenInfo.getExpiration());
        assertFalse(tokenInfo.isExpired());
    }

    @Test
    void testParseToken_WithInvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtUtil.parseToken(invalidToken);
        });
    }

    @Test
    void testTokenInfo_IsExpired_ReturnsCorrectValue() {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);
        JwtUtil.TokenInfo tokenInfo = jwtUtil.parseToken(token);

        // Then
        assertNotNull(tokenInfo);
        assertFalse(tokenInfo.isExpired()); // 新生成的token不应该过期

        // When - 创建一个过期的token
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1L); // 设置为负数模拟过期
        String expiredToken = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLE);
        JwtUtil.TokenInfo expiredTokenInfo = jwtUtil.parseToken(expiredToken);

        // Then
        assertTrue(expiredTokenInfo.isExpired());
    }

    @Test
    void testGenerateToken_WithDifferentRoles() {
        // When - 生成USER角色token
        String userToken = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, "USER");
        String userRole = jwtUtil.getRoleFromToken(userToken);

        // When - 生成ADMIN角色token
        String adminToken = jwtUtil.generateToken(TEST_USERNAME, TEST_USER_ID, "ADMIN");
        String adminRole = jwtUtil.getRoleFromToken(adminToken);

        // Then
        assertEquals("USER", userRole);
        assertEquals("ADMIN", adminRole);
    }

    @Test
    void testGenerateToken_WithDifferentUserIds() {
        // When
        String token1 = jwtUtil.generateToken(TEST_USERNAME, 1L, TEST_ROLE);
        String token2 = jwtUtil.generateToken(TEST_USERNAME, 2L, TEST_ROLE);

        // Then
        assertEquals(1L, jwtUtil.getUserIdFromToken(token1));
        assertEquals(2L, jwtUtil.getUserIdFromToken(token2));
    }

    @Test
    void testGenerateToken_WithDifferentUsernames() {
        // When
        String token1 = jwtUtil.generateToken("user1", TEST_USER_ID, TEST_ROLE);
        String token2 = jwtUtil.generateToken("user2", TEST_USER_ID, TEST_ROLE);

        // Then
        assertEquals("user1", jwtUtil.getUsernameFromToken(token1));
        assertEquals("user2", jwtUtil.getUsernameFromToken(token2));
    }
}