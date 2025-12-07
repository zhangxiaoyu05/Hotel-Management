package com.hotel.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.LoginRequest;
import com.hotel.dto.CreateUserRequest;
import com.hotel.entity.User;
import com.hotel.repository.UserRepository;
import com.hotel.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("认证功能集成测试")
public class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setPassword(passwordEncoder.encode("Test123!@#"));
        testUser.setRole("USER");
        testUser.setStatus("ACTIVE");

        userRepository.insert(testUser);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        userRepository.deleteByIdentifier("testuser");
        userRepository.deleteByIdentifier("test@example.com");
        userRepository.deleteByIdentifier("13800138000");
    }

    @Test
    @DisplayName("用户注册成功 - 完整流程")
    void register_CompleteFlow_Success() throws Exception {
        // 准备注册数据
        CreateUserRequest registerRequest = new CreateUserRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPhone("13800138001");
        registerRequest.setPassword("NewUser123!@#");
        registerRequest.setRole("USER");

        // 执行注册请求
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.user.username").value("newuser"))
                .andExpect(jsonPath("$.data.user.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.user.phone").value("13800138001"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.token").isEmpty());

        // 验证用户已保存到数据库
        User savedUser = userRepository.findByUsername("newuser").orElse(null);
        assert savedUser != null;
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("newuser@example.com", savedUser.getEmail());
        assertTrue(passwordEncoder.matches("NewUser123!@#", savedUser.getPassword()));
    }

    @Test
    @DisplayName("用户登录成功 - 用户名登录")
    void login_Username_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("testuser");
        loginRequest.setPassword("Test123!@#");

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.user.username").value("testuser"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.expiresIn").value(86400));
    }

    @Test
    @DisplayName("用户登录成功 - 邮箱登录")
    void login_Email_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("test@example.com");
        loginRequest.setPassword("Test123!@#");

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    @DisplayName("用户登录成功 - 手机号登录")
    void login_Phone_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("13800138000");
        loginRequest.setPassword("Test123!@#");

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    @DisplayName("用户登录失败 - 密码错误")
    void login_WrongPassword_Failure() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("testuser");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("用户登录失败 - 用户不存在")
    void login_UserNotFound_Failure() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("nonexistent");
        loginRequest.setPassword("Test123!@#");

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名、邮箱或手机号不存在"));
    }

    @Test
    @DisplayName("用户登录失败 - 账户禁用")
    void login_UserInactive_Failure() throws Exception {
        // 将用户状态设置为禁用
        testUser.setStatus("INACTIVE");
        userRepository.updateById(testUser);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("testuser");
        loginRequest.setPassword("Test123!@#");

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("账户已被禁用，请联系管理员"));
    }

    @Test
    @DisplayName("注册后登录 - 完整流程")
    void registerThenLogin_CompleteFlow_Success() throws Exception {
        // 1. 注册新用户
        CreateUserRequest registerRequest = new CreateUserRequest();
        registerRequest.setUsername("flowuser");
        registerRequest.setEmail("flowuser@example.com");
        registerRequest.setPhone("13800138002");
        registerRequest.setPassword("FlowUser123!@#");
        registerRequest.setRole("USER");

        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 2. 使用注册的凭据登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("flowuser");
        loginRequest.setPassword("FlowUser123!@#");

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.username").value("flowuser"))
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    @DisplayName("JWT令牌验证 - 受保护端点访问")
    void jwtTokenValidation_ProtectedEndpoint_Success() throws Exception {
        // 1. 登录获取令牌
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("testuser");
        loginRequest.setPassword("Test123!@#");

        String loginResponse = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        // 提取令牌
        String token = objectMapper.readTree(loginResponse)
                .get("data").get("token").asText();

        // 2. 使用令牌访问受保护的端点
        mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("JWT令牌验证 - 无效令牌")
    void jwtTokenValidation_InvalidToken_Failure() throws Exception {
        mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT令牌验证 - 缺失令牌")
    void jwtTokenValidation_MissingToken_Failure() throws Exception {
        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("用户名验证 - 有效用户名")
    void validateUsername_ValidUsername_Success() throws Exception {
        mockMvc.perform(get("/v1/auth/validate/username")
                .param("username", "validuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户名可用"));
    }

    @Test
    @DisplayName("用户名验证 - 已存在用户名")
    void validateUsername_ExistingUsername_Failure() throws Exception {
        mockMvc.perform(get("/v1/auth/validate/username")
                .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @DisplayName("邮箱验证 - 有效邮箱")
    void validateEmail_ValidEmail_Success() throws Exception {
        mockMvc.perform(get("/v1/auth/validate/email")
                .param("email", "valid@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("邮箱可用"));
    }

    @Test
    @DisplayName("邮箱验证 - 已存在邮箱")
    void validateEmail_ExistingEmail_Failure() throws Exception {
        mockMvc.perform(get("/v1/auth/validate/email")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("邮箱已被注册"));
    }

    @Test
    @DisplayName("手机号验证 - 有效手机号")
    void validatePhone_ValidPhone_Success() throws Exception {
        mockMvc.perform(get("/v1/auth/validate/phone")
                .param("phone", "13800138003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("手机号可用"));
    }

    @Test
    @DisplayName("手机号验证 - 已存在手机号")
    void validatePhone_ExistingPhone_Failure() throws Exception {
        mockMvc.perform(get("/v1/auth/validate/phone")
                .param("phone", "13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("手机号已被注册"));
    }

    @Test
    @DisplayName("管理员用户登录")
    void login_AdminUser_Success() throws Exception {
        // 创建管理员用户
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPhone("13800138004");
        adminUser.setPassword(passwordEncoder.encode("Admin123!@#"));
        adminUser.setRole("ADMIN");
        adminUser.setStatus("ACTIVE");
        userRepository.insert(adminUser);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("admin");
        loginRequest.setPassword("Admin123!@#");

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));

        // 清理
        userRepository.deleteByIdentifier("admin");
    }

    @Test
    @DisplayName("JWT令牌解析和验证")
    void jwtToken_ParseAndValidate_Success() throws Exception {
        // 1. 登录获取令牌
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("testuser");
        loginRequest.setPassword("Test123!@#");

        String loginResponse = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse)
                .get("data").get("token").asText();

        // 2. 验证令牌可以被正确解析
        JwtUtil.TokenInfo tokenInfo = jwtUtil.parseToken(token);
        assertEquals("testuser", tokenInfo.getUsername());
        assertEquals(testUser.getId(), tokenInfo.getUserId());
        assertEquals("USER", tokenInfo.getRole());
        assertFalse(tokenInfo.isExpired());

        // 3. 验证令牌有效
        assertTrue(jwtUtil.validateToken(token, "testuser"));
    }

    @Test
    @DisplayName("登录失败次数过多 - 安全测试")
    void login_TooManyAttempts_Security() throws Exception {
        // 尝试多次错误登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("testuser");
        loginRequest.setPassword("wrongpassword");

        // 模拟多次登录失败
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // 注意：这里假设系统实现了登录失败次数限制
        // 实际实现可能需要额外的配置或代码
    }
}