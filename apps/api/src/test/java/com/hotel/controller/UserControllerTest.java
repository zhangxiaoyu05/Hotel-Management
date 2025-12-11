package com.hotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.ChangePasswordRequest;
import com.hotel.dto.UpdateProfileRequest;
import com.hotel.entity.User;
import com.hotel.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("用户控制器测试")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setRole("USER");
        testUser.setStatus("ACTIVE");
        testUser.setNickname("测试用户");
        testUser.setRealName("张三");
        testUser.setGender("MALE");
        testUser.setBirthDate("1990-01-01");
        testUser.setAvatar("http://example.com/avatar.jpg");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("获取当前用户信息 - 成功")
    void getCurrentUser_Success() throws Exception {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取用户信息成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("更新用户资料 - 成功")
    void updateProfile_Success() throws Exception {
        // Given
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setNickname("新昵称");
        updateRequest.setRealName("李四");
        updateRequest.setGender("FEMALE");
        updateRequest.setBirthDate("1995-05-05");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setNickname("新昵称");
        updatedUser.setRealName("李四");
        updatedUser.setGender("FEMALE");
        updatedUser.setBirthDate("1995-05-05");

        when(userService.updateUserProfile(eq("testuser"), any(UpdateProfileRequest.class)))
                .thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新用户信息成功"))
                .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                .andExpect(jsonPath("$.data.realName").value("李四"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("修改密码 - 成功")
    void changePassword_Success() throws Exception {
        // Given
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("oldPassword");
        changePasswordRequest.setNewPassword("newPassword");
        changePasswordRequest.setConfirmPassword("newPassword");

        // Mock the service method to do nothing (void return)
        when(userService.changePassword(eq("testuser"), any(ChangePasswordRequest.class)))
                .thenAnswer(invocation -> null);

        // When & Then
        mockMvc.perform(put("/api/users/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("密码修改成功"));
    }

    @Test
    @DisplayName("获取当前用户信息 - 未认证")
    void getCurrentUser_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("修改密码 - 密码不一致")
    void changePassword_PasswordMismatch() throws Exception {
        // Given
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("oldPassword");
        changePasswordRequest.setNewPassword("newPassword");
        changePasswordRequest.setConfirmPassword("differentPassword");

        // When & Then
        mockMvc.perform(put("/api/users/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("新密码和确认密码不一致"));
    }
}