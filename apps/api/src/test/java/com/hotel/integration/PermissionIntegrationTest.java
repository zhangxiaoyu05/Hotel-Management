package com.hotel.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.UpdateRoleRequest;
import com.hotel.dto.UpdateStatusRequest;
import com.hotel.entity.User;
import com.hotel.enums.Role;
import com.hotel.enums.UserStatus;
import com.hotel.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 权限变更流程集成测试
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class PermissionIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private User testAdmin;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 创建测试管理员
        testAdmin = new User();
        testAdmin.setUsername("admin");
        testAdmin.setEmail("admin@example.com");
        testAdmin.setPhone("1234567890");
        testAdmin.setPassword("$2a$10$encodedPassword");
        testAdmin.setRole(Role.ADMIN.getCode());
        testAdmin.setStatus("ACTIVE");
        userRepository.insert(testAdmin);

        // 创建测试普通用户
        testUser = new User();
        testUser.setUsername("user");
        testUser.setEmail("user@example.com");
        testUser.setPhone("0987654321");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setRole(Role.USER.getCode());
        testUser.setStatus("ACTIVE");
        userRepository.insert(testUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteById(testAdmin.getId());
        userRepository.deleteById(testUser.getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCompletePermissionChangeFlow() throws Exception {
        // 1. 获取用户列表
        mockMvc.perform(get("/v1/admin/users")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records").isArray());

        // 2. 将普通用户提升为管理员
        UpdateRoleRequest roleRequest = new UpdateRoleRequest();
        roleRequest.setRole(Role.ADMIN);

        mockMvc.perform(put("/v1/admin/users/" + testUser.getId() + "/role")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. 验证角色变更
        User updatedUser = userRepository.selectById(testUser.getId());
        assertEquals(Role.ADMIN.getCode(), updatedUser.getRole());

        // 4. 将用户状态设置为禁用
        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(UserStatus.INACTIVE);

        mockMvc.perform(put("/v1/admin/users/" + testUser.getId() + "/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. 验证状态变更
        updatedUser = userRepository.selectById(testUser.getId());
        assertEquals(UserStatus.INACTIVE.getCode(), updatedUser.getStatus());

        // 6. 重新激活用户
        statusRequest.setStatus(UserStatus.ACTIVE);

        mockMvc.perform(put("/v1/admin/users/" + testUser.getId() + "/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 7. 验证重新激活
        updatedUser = userRepository.selectById(testUser.getId());
        assertEquals(UserStatus.ACTIVE.getCode(), updatedUser.getStatus());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testNonAdminCannotAccessAdminEndpoints() throws Exception {
        // 普通用户不能访问管理员端点
        mockMvc.perform(get("/v1/admin/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/v1/admin/users/" + testUser.getId() + "/role")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/v1/admin/users/" + testUser.getId() + "/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedUserCannotAccessAdminEndpoints() throws Exception {
        // 未认证用户不能访问管理员端点
        mockMvc.perform(get("/v1/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/v1/admin/users/" + testUser.getId() + "/role")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/v1/admin/users/" + testUser.getId() + "/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminCannotModifyOwnRole() throws Exception {
        UpdateRoleRequest roleRequest = new UpdateRoleRequest();
        roleRequest.setRole(Role.USER);

        mockMvc.perform(put("/v1/admin/users/" + testAdmin.getId() + "/role")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("不能修改自己的角色"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminCannotDisableOwnAccount() throws Exception {
        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(UserStatus.INACTIVE);

        mockMvc.perform(put("/v1/admin/users/" + testAdmin.getId() + "/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("不能禁用自己的账户"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUserSearchAndFiltering() throws Exception {
        // 按用户名搜索
        mockMvc.perform(get("/v1/admin/users")
                .param("username", "user")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 按角色筛选
        mockMvc.perform(get("/v1/admin/users")
                .param("role", "USER")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 按状态筛选
        mockMvc.perform(get("/v1/admin/users")
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}