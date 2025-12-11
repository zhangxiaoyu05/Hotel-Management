package com.hotel.security;

import com.hotel.entity.User;
import com.hotel.enums.Role;
import com.hotel.enums.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomUserDetails 单元测试
 */
class CustomUserDetailsTest {

    private User testUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("1234567890");
        testUser.setPassword("password");
        testUser.setRole(Role.USER.getCode());
        testUser.setStatus("ACTIVE");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        userDetails = new CustomUserDetails(testUser);
    }

    @Test
    void testBasicUserDetails() {
        assertEquals(1L, userDetails.getId());
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("test@example.com", userDetails.getEmail());
        assertEquals("1234567890", userDetails.getPhone());
        assertEquals("password", userDetails.getPassword());
        assertEquals(Role.USER, userDetails.getRole());
        assertEquals("ACTIVE", userDetails.getStatus());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    void testAdminUserHasAllPermissions() {
        // 设置为管理员
        testUser.setRole(Role.ADMIN.getCode());
        CustomUserDetails adminUser = new CustomUserDetails(testUser);

        assertTrue(adminUser.isAdmin());
        assertTrue(adminUser.hasRole(Role.ADMIN));
        assertFalse(adminUser.hasRole(Role.USER));

        // 检查管理员拥有所有权限
        for (Permission permission : Permission.values()) {
            assertTrue(adminUser.hasPermission(permission),
                "管理员应该拥有权限: " + permission.getCode());
        }
    }

    @Test
    void testNormalUserHasLimitedPermissions() {
        // 普通用户应该只有基础权限
        assertTrue(userDetails.hasRole(Role.USER));
        assertFalse(userDetails.hasRole(Role.ADMIN));
        assertFalse(userDetails.isAdmin());

        // 检查基础权限
        assertTrue(userDetails.hasPermission(Permission.USER_READ));
        assertTrue(userDetails.hasPermission(Permission.BOOKING_READ));
        assertTrue(userDetails.hasPermission(Permission.BOOKING_WRITE));
        assertTrue(userDetails.hasPermission(Permission.REVIEW_READ));
        assertTrue(userDetails.hasPermission(Permission.REVIEW_WRITE));
        assertTrue(userDetails.hasPermission(Permission.ROOM_READ));

        // 检查管理员权限（不应该拥有）
        assertFalse(userDetails.hasPermission(Permission.USER_DELETE));
        assertFalse(userDetails.hasPermission(Permission.SYSTEM_ADMIN));
    }

    @Test
    void testInactiveUser() {
        testUser.setStatus("INACTIVE");
        CustomUserDetails inactiveUser = new CustomUserDetails(testUser);

        assertFalse(inactiveUser.isEnabled());
    }

    @Test
    void testAuthoritiesContainRole() {
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertTrue(authorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));

        // 检查权限数量
        long expectedPermissions = 6; // USER_READ, BOOKING_READ, BOOKING_WRITE, REVIEW_READ, REVIEW_WRITE, ROOM_READ
        assertEquals(expectedPermissions + 1, authorities.size()); // +1 for ROLE_USER
    }

    @Test
    void testAdminAuthoritiesContainAllPermissions() {
        testUser.setRole(Role.ADMIN.getCode());
        CustomUserDetails adminUser = new CustomUserDetails(testUser);

        Collection<? extends GrantedAuthority> authorities = adminUser.getAuthorities();

        assertTrue(authorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));

        // 管理员应该拥有所有权限
        assertEquals(Permission.values().length + 1, authorities.size()); // +1 for ROLE_ADMIN
    }
}