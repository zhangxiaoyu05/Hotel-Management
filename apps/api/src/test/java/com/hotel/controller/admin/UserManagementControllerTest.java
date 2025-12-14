package com.hotel.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.admin.user.UserManagementDTO;
import com.hotel.dto.admin.user.UserSearchCriteria;
import com.hotel.dto.admin.user.UserStatisticsDTO;
import com.hotel.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserManagementController.class)
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserManagementService userManagementService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserManagementDTO testUserDTO;
    private UserSearchCriteria searchCriteria;
    private UserStatisticsDTO statisticsDTO;

    @BeforeEach
    void setUp() {
        testUserDTO = new UserManagementDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setPhoneNumber("138****8000");
        testUserDTO.setStatus("ACTIVE");
        testUserDTO.setCreatedAt(LocalDateTime.now());
        testUserDTO.setLastLoginAt(LocalDateTime.now());

        searchCriteria = new UserSearchCriteria();
        searchCriteria.setUsername("test");

        statisticsDTO = new UserStatisticsDTO();
        statisticsDTO.setTotalUsers(100L);
        statisticsDTO.setActiveUsers(80L);
        statisticsDTO.setInactiveUsers(20L);
        statisticsDTO.setNewUsersThisMonth(10L);
    }

    @Test
    void testSearchUsers_Success() throws Exception {
        Page<UserManagementDTO> userPage = new PageImpl<>(
            Arrays.asList(testUserDTO),
            PageRequest.of(0, 10),
            1
        );

        when(userManagementService.searchUsers(any(UserSearchCriteria.class), any()))
            .thenReturn(userPage);

        mockMvc.perform(post("/api/admin/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCriteria))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetUserById_Success() throws Exception {
        when(userManagementService.getUserById(1L))
            .thenReturn(Optional.of(testUserDTO));

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        when(userManagementService.getUserById(1L))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateUserStatus_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("status", "INACTIVE");

        when(userManagementService.updateUserStatus(eq(1L), eq("INACTIVE"), anyString()))
            .thenReturn(true);

        mockMvc.perform(put("/api/admin/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户状态更新成功"));
    }

    @Test
    void testUpdateUserStatus_Failed() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("status", "INACTIVE");

        when(userManagementService.updateUserStatus(eq(1L), eq("INACTIVE"), anyString()))
            .thenReturn(false);

        mockMvc.perform(put("/api/admin/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户未找到"));
    }

    @Test
    void testGetUserStatistics() throws Exception {
        when(userManagementService.getUserStatistics())
            .thenReturn(statisticsDTO);

        mockMvc.perform(get("/api/admin/users/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.activeUsers").value(80))
                .andExpect(jsonPath("$.inactiveUsers").value(20))
                .andExpect(jsonPath("$.newUsersThisMonth").value(10));
    }

    @Test
    void testBatchUpdateUserStatus_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userIds", Arrays.asList(1L, 2L, 3L));
        request.put("status", "INACTIVE");

        when(userManagementService.batchUpdateUserStatus(
            anyList(), eq("INACTIVE"), anyString()))
            .thenReturn(3);

        mockMvc.perform(put("/api/admin/users/batch-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("成功更新3个用户的状态"))
                .andExpect(jsonPath("$.updatedCount").value(3));
    }

    @Test
    void testBatchUpdateUserStatus_EmptyList() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userIds", Arrays.asList());
        request.put("status", "INACTIVE");

        when(userManagementService.batchUpdateUserStatus(
            anyList(), eq("INACTIVE"), anyString()))
            .thenReturn(0);

        mockMvc.perform(put("/api/admin/users/batch-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("成功更新0个用户的状态"))
                .andExpect(jsonPath("$.updatedCount").value(0));
    }

    @Test
    void testSearchUsers_WithInvalidPage() throws Exception {
        mockMvc.perform(post("/api/admin/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCriteria))
                .param("page", "-1")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchUsers_WithInvalidSize() throws Exception {
        mockMvc.perform(post("/api/admin/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCriteria))
                .param("page", "0")
                .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateUserStatus_WithInvalidStatus() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("status", "INVALID_STATUS");

        mockMvc.perform(put("/api/admin/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBatchUpdateUserStatus_WithNullUserIds() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("status", "INACTIVE");

        mockMvc.perform(put("/api/admin/users/batch-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBatchUpdateUserStatus_WithInvalidStatus() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userIds", Arrays.asList(1L, 2L));
        request.put("status", "INVALID_STATUS");

        mockMvc.perform(put("/api/admin/users/batch-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}