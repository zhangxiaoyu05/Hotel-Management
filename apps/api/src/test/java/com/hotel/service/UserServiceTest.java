package com.hotel.service;

import com.hotel.dto.CreateUserRequest;
import com.hotel.dto.AuthResponse;
import com.hotel.dto.LoginRequest;
import com.hotel.entity.User;
import com.hotel.repository.UserRepository;
import com.hotel.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest validRequest;
    private LoginRequest validLoginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUserRequest();
        validRequest.setUsername("testuser");
        validRequest.setEmail("test@example.com");
        validRequest.setPhone("13800138000");
        validRequest.setPassword("Test123!@#");
        validRequest.setRole("USER");

        validLoginRequest = new LoginRequest();
        validLoginRequest.setIdentifier("testuser");
        validLoginRequest.setPassword("Test123!@#");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setPassword("encodedPassword");
        testUser.setRole("USER");
        testUser.setStatus("ACTIVE");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testRegisterUser_Success() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("13800138000")).thenReturn(false);
        when(passwordEncoder.encode("Test123!@#")).thenReturn("encodedPassword");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        savedUser.setPhone("13800138000");
        savedUser.setRole("USER");
        savedUser.setStatus("ACTIVE");
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setUpdatedAt(LocalDateTime.now());

        when(userRepository.insert(any(User.class))).thenReturn(1);

        // When
        AuthResponse.Data result = userService.registerUser(validRequest);

        // Then
        assertNotNull(result);
        assertNotNull(result.getUser());
        assertEquals("testuser", result.getUser().getUsername());
        assertEquals("test@example.com", result.getUser().getEmail());
        assertEquals("13800138000", result.getUser().getPhone());
        assertEquals("USER", result.getUser().getRole());
        assertEquals("ACTIVE", result.getUser().getStatus());
        assertNull(result.getToken()); // 注册时不返回token

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).existsByPhone("13800138000");
        verify(passwordEncoder).encode("Test123!@#");
        verify(userRepository).insert(any(User.class));
    }

    @Test
    void testRegisterUser_UsernameExists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(validRequest)
        );

        assertEquals("用户名已存在", exception.getMessage());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).insert(any(User.class));
    }

    @Test
    void testRegisterUser_EmailExists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(validRequest)
        );

        assertEquals("邮箱已被注册", exception.getMessage());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).insert(any(User.class));
    }

    @Test
    void testRegisterUser_PhoneExists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("13800138000")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(validRequest)
        );

        assertEquals("手机号已被注册", exception.getMessage());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).existsByPhone("13800138000");
        verify(userRepository, never()).insert(any(User.class));
    }

    @Test
    void testRegisterUser_DefaultRole() {
        // Given
        validRequest.setRole(null); // 不设置role
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("13800138000")).thenReturn(false);
        when(passwordEncoder.encode("Test123!@#")).thenReturn("encodedPassword");
        when(userRepository.insert(any(User.class))).thenReturn(1);

        // When
        AuthResponse.Data result = userService.registerUser(validRequest);

        // Then
        assertNotNull(result);
        assertEquals("USER", result.getUser().getRole()); // 默认应该是USER
    }

    @Test
    void testFindByUsername_Success() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void testFindByUsername_NotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByUsername("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void testFindByEmail_Success() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void testFindByPhone_Success() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");

        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByPhone("13800138000");

        // Then
        assertTrue(result.isPresent());
        assertEquals("13800138000", result.get().getPhone());
        verify(userRepository).findByPhone("13800138000");
    }

    @Test
    void testFindByIdentifier_WithUsername() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        when(userRepository.findByIdentifier("testuser")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByIdentifier("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository).findByIdentifier("testuser");
    }

    @Test
    void testFindByIdentifier_WithEmail() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findByIdentifier("test@example.com")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByIdentifier("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userRepository).findByIdentifier("test@example.com");
    }

    @Test
    void testExistsByUsername() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When
        boolean result = userService.existsByUsername("testuser");

        // Then
        assertTrue(result);
        verify(userRepository).existsByUsername("testuser");
    }

    @Test
    void testExistsByEmail() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When
        boolean result = userService.existsByEmail("test@example.com");

        // Then
        assertTrue(result);
        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    void testExistsByPhone() {
        // Given
        when(userRepository.existsByPhone("13800138000")).thenReturn(true);

        // When
        boolean result = userService.existsByPhone("13800138000");

        // Then
        assertTrue(result);
        verify(userRepository).existsByPhone("13800138000");
    }

    @Test
    void testFindById() {
        // Given
        User user = new User();
        user.setId(1L);

        when(userRepository.selectById(1L)).thenReturn(user);

        // When
        Optional<User> result = userService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(userRepository).selectById(1L);
    }

    @Test
    void testUpdateUserStatus() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setStatus("ACTIVE");

        when(userRepository.selectById(1L)).thenReturn(user);
        when(userRepository.updateById(any(User.class))).thenReturn(1);

        // When
        User result = userService.updateUserStatus(1L, "INACTIVE");

        // Then
        assertEquals("INACTIVE", result.getStatus());
        verify(userRepository).selectById(1L);
        verify(userRepository).updateById(any(User.class));
    }

    @Test
    void testUpdateUserStatus_UserNotFound() {
        // Given
        when(userRepository.selectById(999L)).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUserStatus(999L, "INACTIVE")
        );

        assertEquals("用户不存在", exception.getMessage());
        verify(userRepository).selectById(999L);
        verify(userRepository, never()).updateById(any(User.class));
    }

    // ========== 登录功能测试 ==========

    @Test
    void testLogin_Success() {
        // Given
        when(userRepository.findByIdentifier("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Test123!@#", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("testuser", 1L, "USER")).thenReturn("mock-jwt-token");
        when(jwtUtil.generateRefreshToken("testuser", 1L)).thenReturn("mock-refresh-token");
        when(userRepository.updateById(any(User.class))).thenReturn(1);

        // When
        AuthResponse.Data result = userService.login(validLoginRequest);

        // Then
        assertNotNull(result);
        assertNotNull(result.getUser());
        assertEquals("testuser", result.getUser().getUsername());
        assertEquals("test@example.com", result.getUser().getEmail());
        assertEquals("13800138000", result.getUser().getPhone());
        assertEquals("USER", result.getUser().getRole());
        assertEquals("ACTIVE", result.getUser().getStatus());
        assertEquals("mock-jwt-token", result.getToken());
        assertEquals("mock-refresh-token", result.getRefreshToken());
        assertEquals(86400L, result.getExpiresIn());

        verify(userRepository).findByIdentifier("testuser");
        verify(passwordEncoder).matches("Test123!@#", "encodedPassword");
        verify(jwtUtil).generateToken("testuser", 1L, "USER");
        verify(jwtUtil).generateRefreshToken("testuser", 1L);
        verify(userRepository).updateById(any(User.class));
    }

    @Test
    void testLogin_WithUsername_Success() {
        // Given
        LoginRequest usernameLoginRequest = new LoginRequest();
        usernameLoginRequest.setIdentifier("testuser");
        usernameLoginRequest.setPassword("Test123!@#");

        when(userRepository.findByIdentifier("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Test123!@#", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("testuser", 1L, "USER")).thenReturn("mock-jwt-token");
        when(jwtUtil.generateRefreshToken("testuser", 1L)).thenReturn("mock-refresh-token");
        when(userRepository.updateById(any(User.class))).thenReturn(1);

        // When
        AuthResponse.Data result = userService.login(usernameLoginRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUser().getUsername());
        assertEquals("mock-jwt-token", result.getToken());
        verify(userRepository).findByIdentifier("testuser");
    }

    @Test
    void testLogin_WithEmail_Success() {
        // Given
        LoginRequest emailLoginRequest = new LoginRequest();
        emailLoginRequest.setIdentifier("test@example.com");
        emailLoginRequest.setPassword("Test123!@#");

        when(userRepository.findByIdentifier("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Test123!@#", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("testuser", 1L, "USER")).thenReturn("mock-jwt-token");
        when(jwtUtil.generateRefreshToken("testuser", 1L)).thenReturn("mock-refresh-token");
        when(userRepository.updateById(any(User.class))).thenReturn(1);

        // When
        AuthResponse.Data result = userService.login(emailLoginRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUser().getUsername());
        assertEquals("mock-jwt-token", result.getToken());
        verify(userRepository).findByIdentifier("test@example.com");
    }

    @Test
    void testLogin_WithPhone_Success() {
        // Given
        LoginRequest phoneLoginRequest = new LoginRequest();
        phoneLoginRequest.setIdentifier("13800138000");
        phoneLoginRequest.setPassword("Test123!@#");

        when(userRepository.findByIdentifier("13800138000")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Test123!@#", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("testuser", 1L, "USER")).thenReturn("mock-jwt-token");
        when(jwtUtil.generateRefreshToken("testuser", 1L)).thenReturn("mock-refresh-token");
        when(userRepository.updateById(any(User.class))).thenReturn(1);

        // When
        AuthResponse.Data result = userService.login(phoneLoginRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUser().getUsername());
        assertEquals("mock-jwt-token", result.getToken());
        verify(userRepository).findByIdentifier("13800138000");
    }

    @Test
    void testLogin_UserNotFound() {
        // Given
        when(userRepository.findByIdentifier("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(new LoginRequest("nonexistent", "password"))
        );

        assertEquals("用户名、邮箱或手机号不存在", exception.getMessage());
        verify(userRepository).findByIdentifier("nonexistent");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyLong(), anyString());
    }

    @Test
    void testLogin_UserInactive() {
        // Given
        testUser.setStatus("INACTIVE");
        when(userRepository.findByIdentifier("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(validLoginRequest)
        );

        assertEquals("账户已被禁用，请联系管理员", exception.getMessage());
        verify(userRepository).findByIdentifier("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyLong(), anyString());
    }

    @Test
    void testLogin_WrongPassword() {
        // Given
        when(userRepository.findByIdentifier("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(new LoginRequest("testuser", "wrongpassword"))
        );

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(userRepository).findByIdentifier("testuser");
        verify(passwordEncoder).matches("wrongpassword", "encodedPassword");
        verify(jwtUtil, never()).generateToken(anyString(), anyLong(), anyString());
    }

    @Test
    void testLogin_AdminUser_Success() {
        // Given
        testUser.setRole("ADMIN");
        when(userRepository.findByIdentifier("admin")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Admin123!@#", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("testuser", 1L, "ADMIN")).thenReturn("mock-admin-jwt-token");
        when(jwtUtil.generateRefreshToken("testuser", 1L)).thenReturn("mock-refresh-token");
        when(userRepository.updateById(any(User.class))).thenReturn(1);

        LoginRequest adminLoginRequest = new LoginRequest();
        adminLoginRequest.setIdentifier("admin");
        adminLoginRequest.setPassword("Admin123!@#");

        // When
        AuthResponse.Data result = userService.login(adminLoginRequest);

        // Then
        assertNotNull(result);
        assertEquals("ADMIN", result.getUser().getRole());
        assertEquals("mock-admin-jwt-token", result.getToken());
        verify(jwtUtil).generateToken("testuser", 1L, "ADMIN");
    }

    @Test
    void testLogin_PasswordEncoderThrowsException() {
        // Given
        when(userRepository.findByIdentifier("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Test123!@#", "encodedPassword"))
                .thenThrow(new RuntimeException("Password encoding error"));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(validLoginRequest)
        );

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(userRepository).findByIdentifier("testuser");
        verify(passwordEncoder).matches("Test123!@#", "encodedPassword");
    }
}