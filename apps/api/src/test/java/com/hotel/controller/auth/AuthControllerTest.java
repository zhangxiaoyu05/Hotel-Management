package com.hotel.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.AuthResponse;
import com.hotel.dto.CreateUserRequest;
import com.hotel.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateUserRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUserRequest();
        validRequest.setUsername("testuser");
        validRequest.setEmail("test@example.com");
        validRequest.setPhone("13800138000");
        validRequest.setPassword("Test123!@#");
        validRequest.setRole("USER");
    }

    @Test
    void testRegister_Success() throws Exception {
        // Given
        AuthResponse.Data responseData = new AuthResponse.Data();
        AuthResponse.User user = new AuthResponse.User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPhone("13800138000");
        user.setRole("USER");
        user.setStatus("ACTIVE");

        responseData.setUser(user);
        responseData.setToken(null); // 注册时不返回token

        when(userService.registerUser(any(CreateUserRequest.class))).thenReturn(responseData);

        // When & Then
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.user.username").value("testuser"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.token").isEmpty());
    }

    @Test
    void testRegister_ValidationError() throws Exception {
        // Given - 无效的请求数据
        CreateUserRequest invalidRequest = new CreateUserRequest();
        invalidRequest.setUsername("a"); // 用户名太短
        invalidRequest.setEmail("invalid-email"); // 邮箱格式错误
        invalidRequest.setPhone("123"); // 手机号格式错误
        invalidRequest.setPassword("weak"); // 密码太弱

        // When & Then
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_UsernameExists() throws Exception {
        // Given
        when(userService.registerUser(any(CreateUserRequest.class)))
                .thenThrow(new IllegalArgumentException("用户名已存在"));

        // When & Then
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void testRegister_EmailExists() throws Exception {
        // Given
        when(userService.registerUser(any(CreateUserRequest.class)))
                .thenThrow(new IllegalArgumentException("邮箱已被注册"));

        // When & Then
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("邮箱已被注册"));
    }

    @Test
    void testRegister_PhoneExists() throws Exception {
        // Given
        when(userService.registerUser(any(CreateUserRequest.class)))
                .thenThrow(new IllegalArgumentException("手机号已被注册"));

        // When & Then
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("手机号已被注册"));
    }

    @Test
    void testRegister_SystemError() throws Exception {
        // Given
        when(userService.registerUser(any(CreateUserRequest.class)))
                .thenThrow(new RuntimeException("系统错误"));

        // When & Then
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("注册失败，请稍后重试"));
    }

    @Test
    void testValidateUsername_Success() throws Exception {
        // Given
        when(userService.existsByUsername("testuser")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/v1/auth/validate/username")
                        .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户名可用"));
    }

    @Test
    void testValidateUsername_UsernameExists() throws Exception {
        // Given
        when(userService.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/v1/auth/validate/username")
                        .param("username", "existinguser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void testValidateUsername_EmptyUsername() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/auth/validate/username")
                        .param("username", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名不能为空"));
    }

    @Test
    void testValidateUsername_TooShort() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/auth/validate/username")
                        .param("username", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名长度必须在3到50个字符之间"));
    }

    @Test
    void testValidateUsername_TooLong() throws Exception {
        // When & Then
        String longUsername = "a".repeat(51);
        mockMvc.perform(get("/v1/auth/validate/username")
                        .param("username", longUsername))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名长度必须在3到50个字符之间"));
    }

    @Test
    void testValidateEmail_Success() throws Exception {
        // Given
        when(userService.existsByEmail("test@example.com")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/v1/auth/validate/email")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("邮箱可用"));
    }

    @Test
    void testValidateEmail_EmailExists() throws Exception {
        // Given
        when(userService.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/v1/auth/validate/email")
                        .param("email", "existing@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("邮箱已被注册"));
    }

    @Test
    void testValidateEmail_InvalidFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/auth/validate/email")
                        .param("email", "invalid-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("邮箱格式不正确"));
    }

    @Test
    void testValidateEmail_EmptyEmail() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/auth/validate/email")
                        .param("email", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("邮箱不能为空"));
    }

    @Test
    void testValidatePhone_Success() throws Exception {
        // Given
        when(userService.existsByPhone("13800138000")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/v1/auth/validate/phone")
                        .param("phone", "13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("手机号可用"));
    }

    @Test
    void testValidatePhone_PhoneExists() throws Exception {
        // Given
        when(userService.existsByPhone("13800138001")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/v1/auth/validate/phone")
                        .param("phone", "13800138001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("手机号已被注册"));
    }

    @Test
    void testValidatePhone_InvalidFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/auth/validate/phone")
                        .param("phone", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("手机号格式不正确"));
    }

    @Test
    void testValidatePhone_EmptyPhone() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/auth/validate/phone")
                        .param("phone", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("手机号不能为空"));
    }
}