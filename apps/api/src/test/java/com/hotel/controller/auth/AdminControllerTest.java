package com.hotel.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.controller.BaseController;
import com.hotel.dto.UpdateRoleRequest;
import com.hotel.dto.UpdateStatusRequest;
import com.hotel.entity.User;
import com.hotel.enums.Role;
import com.hotel.enums.UserStatus;
import com.hotel.repository.UserRepository;
import com.hotel.security.CustomUserDetails;
import com.hotel.util.PermissionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminController 单元测试
 */
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PermissionUtil permissionUtil;

    private User testAdmin;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试管理员用户
        testAdmin = new User();
        testAdmin.setId(1L);
        testAdmin.setUsername("admin");
        testAdmin.setEmail("admin@example.com");
        testAdmin.setPhone("1234567890");
        testAdmin.setPassword("password");
        testAdmin.setRole(Role.ADMIN.getCode());
        testAdmin.setStatus("ACTIVE");

        // 创建测试普通用户
        testUser = new User();
        testUser.setId(2L);
        testUser.setUsername("user");
        testUser.setEmail("user@example.com");
        testUser.setPhone("0987654321");
        testUser.setPassword("password");
        testUser.setRole(Role.USER.getCode());
        testUser.setStatus("ACTIVE");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetUsers_Success() throws Exception {
        // Mock PermissionUtil
        when(permissionUtil.getCurrentUserId()).thenReturn(1L);

        // Mock repository
        when(userRepository.selectPage(any(), any())).thenAnswer(invocation -> {
            // 模拟分页结果
            return mockPageResult(Arrays.asList(testUser, testAdmin));
        });

        mockMvc.perform(get("/v1/admin/users")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetUser_Success() throws Exception {
        when(permissionUtil.getCurrentUserId()).thenReturn(1L);
        when(userRepository.selectById(2L)).thenReturn(testUser);

        mockMvc.perform(get("/v1/admin/users/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.username").value("user"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetUser_NotFound() throws Exception {
        when(permissionUtil.getCurrentUserId()).thenReturn(1L);
        when(userRepository.selectById(999L)).thenReturn(null);

        mockMvc.perform(get("/v1/admin/users/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserRole_Success() throws Exception {
        when(permissionUtil.getCurrentUserId()).thenReturn(1L);
        when(userRepository.selectById(2L)).thenReturn(testUser);
        when(userRepository.updateById(any(User.class))).thenReturn(1);

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(Role.ADMIN);

        mockMvc.perform(put("/v1/admin/users/2/role")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新用户角色成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserRole_CannotUpdateOwnRole() throws Exception {
        when(permissionUtil.getCurrentUserId()).thenReturn(1L);
        when(userRepository.selectById(1L)).thenReturn(testAdmin);

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(Role.USER);

        mockMvc.perform(put("/v1/admin/users/1/role")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("不能修改自己的角色"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserStatus_Success() throws Exception {
        when(permissionUtil.getCurrentUserId()).thenReturn(1L);
        when(userRepository.selectById(2L)).thenReturn(testUser);
        when(userRepository.updateById(any(User.class))).thenReturn(1);

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(UserStatus.INACTIVE);

        mockMvc.perform(put("/v1/admin/users/2/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新用户状态成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserStatus_CannotDisableOwnAccount() throws Exception {
        when(permissionUtil.getCurrentUserId()).thenReturn(1L);
        when(userRepository.selectById(1L)).thenReturn(testAdmin);

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(UserStatus.INACTIVE);

        mockMvc.perform(put("/v1/admin/users/1/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("不能禁用自己的账户"));
    }

    @Test
    @WithMockUser(roles = "USER") // 非管理员用户
    void testAdminEndpoint_RequiresAdminRole() throws Exception {
        mockMvc.perform(get("/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private com.baomidou.mybatisplus.core.metadata.IPage<User> mockPageResult(List<User> users) {
        com.baomidou.mybatisplus.core.metadata.IPage<User> page = mock(com.baomidou.mybatisplus.core.metadata.IPage.class);
        when(page.getRecords()).thenReturn(users);
        when(page.getTotal()).thenReturn((long) users.size());
        return page;
    }
}