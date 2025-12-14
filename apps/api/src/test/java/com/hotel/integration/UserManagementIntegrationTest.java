package com.hotel.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.entity.User;
import com.hotel.entity.UserOperationHistory;
import com.hotel.repository.UserOperationHistoryRepository;
import com.hotel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class UserManagementIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOperationHistoryRepository historyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 创建测试用户
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setEmail("test1@example.com");
        testUser1.setPhoneNumber("13800138001");
        testUser1.setPassword("password");
        testUser1.setStatus("ACTIVE");
        testUser1.setRealName("张三");
        testUser1.setIdCard("123456789012345678");
        testUser1.setCreatedAt(LocalDateTime.now());
        testUser1.setLastLoginAt(LocalDateTime.now());

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setEmail("test2@example.com");
        testUser2.setPhoneNumber("13800138002");
        testUser2.setPassword("password");
        testUser2.setStatus("INACTIVE");
        testUser2.setRealName("李四");
        testUser2.setIdCard("987654321098765432");
        testUser2.setCreatedAt(LocalDateTime.now().minusDays(5));
        testUser2.setLastLoginAt(LocalDateTime.now().minusDays(1));

        testUser3 = new User();
        testUser3.setUsername("otheruser");
        testUser3.setEmail("other@example.com");
        testUser3.setPhoneNumber("13800138003");
        testUser3.setPassword("password");
        testUser3.setStatus("ACTIVE");
        testUser3.setRealName("王五");
        testUser3.setIdCard("456789012345678901");
        testUser3.setCreatedAt(LocalDateTime.now().minusMonths(1));
        testUser3.setLastLoginAt(LocalDateTime.now().minusWeeks(1));

        // 保存测试数据
        testUser1 = userRepository.save(testUser1);
        testUser2 = userRepository.save(testUser2);
        testUser3 = userRepository.save(testUser3);
    }

    @Test
    void testUserSearchWorkflow() throws Exception {
        // 测试搜索用户
        Map<String, Object> searchCriteria = new HashMap<>();
        searchCriteria.put("username", "test");
        searchCriteria.put("status", "ACTIVE");

        mockMvc.perform(post("/api/admin/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCriteria))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username", is("testuser1")))
                .andExpect(jsonPath("$.content[0].email", is("test1@example.com")))
                .andExpect(jsonPath("$.content[0].phoneNumber", is("138****8001")))
                .andExpect(jsonPath("$.content[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$.content[0].realName", is("张*")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void testGetUserDetailsWorkflow() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", testUser1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testUser1.getId().intValue())))
                .andExpect(jsonPath("$.username", is("testuser1")))
                .andExpect(jsonPath("$.email", is("test1@example.com")))
                .andExpect(jsonPath("$.phoneNumber", is("138****8001")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.realName", is("张*")));
    }

    @Test
    void testUpdateUserStatusWorkflow() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("status", "INACTIVE");

        mockMvc.perform(put("/api/admin/users/{id}/status", testUser1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("用户状态更新成功")));

        // 验证数据库中的状态已更新
        User updatedUser = userRepository.findById(testUser1.getId()).orElseThrow();
        assertEquals("INACTIVE", updatedUser.getStatus());

        // 验证操作历史已记录
        List<UserOperationHistory> history = historyRepository.findByUserId(testUser1.getId());
        assertFalse(history.isEmpty());
        assertTrue(history.stream().anyMatch(h ->
            h.getOperation().equals("STATUS_UPDATE") &&
            h.getNewValue().equals("INACTIVE")));
    }

    @Test
    void testBatchUpdateUserStatusWorkflow() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userIds", Arrays.asList(testUser1.getId(), testUser3.getId()));
        request.put("status", "INACTIVE");

        mockMvc.perform(put("/api/admin/users/batch-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("成功更新2个用户的状态")))
                .andExpect(jsonPath("$.updatedCount", is(2)));

        // 验证数据库中的状态已更新
        User updatedUser1 = userRepository.findById(testUser1.getId()).orElseThrow();
        User updatedUser3 = userRepository.findById(testUser3.getId()).orElseThrow();
        assertEquals("INACTIVE", updatedUser1.getStatus());
        assertEquals("INACTIVE", updatedUser3.getStatus());

        // 验证操作历史已记录
        List<UserOperationHistory> history1 = historyRepository.findByUserId(testUser1.getId());
        List<UserOperationHistory> history3 = historyRepository.findByUserId(testUser3.getId());
        assertFalse(history1.isEmpty());
        assertFalse(history3.isEmpty());
    }

    @Test
    void testGetUserStatisticsWorkflow() throws Exception {
        mockMvc.perform(get("/api/admin/users/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", is(3)))
                .andExpect(jsonPath("$.activeUsers", is(2)))
                .andExpect(jsonPath("$.inactiveUsers", is(1)))
                .andExpect(jsonPath("$.newUsersThisMonth", greaterThanOrEqualTo(2)));
    }

    @Test
    void testComplexSearchWorkflow() throws Exception {
        // 测试复杂搜索条件
        Map<String, Object> searchCriteria = new HashMap<>();
        searchCriteria.put("username", "user");
        searchCriteria.put("email", "example.com");

        mockMvc.perform(post("/api/admin/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCriteria))
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    @Test
    void testErrorHandlingWorkflow() throws Exception {
        // 测试获取不存在的用户
        mockMvc.perform(get("/api/admin/users/999"))
                .andExpect(status().isNotFound());

        // 测试更新不存在用户的状态
        Map<String, Object> request = new HashMap<>();
        request.put("status", "INACTIVE");

        mockMvc.perform(put("/api/admin/users/999/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("用户未找到")));
    }

    @Test
    void testValidationErrorHandling() throws Exception {
        // 测试无效的状态值
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("status", "INVALID_STATUS");

        mockMvc.perform(put("/api/admin/users/{id}/status", testUser1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // 测试批量更新空用户列表
        Map<String, Object> emptyRequest = new HashMap<>();
        emptyRequest.put("userIds", Arrays.asList());
        emptyRequest.put("status", "INACTIVE");

        mockMvc.perform(put("/api/admin/users/batch-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount", is(0)));
    }

    @Test
    void testDataSecurityWorkflow() throws Exception {
        // 验证敏感数据脱敏
        mockMvc.perform(get("/api/admin/users/{id}", testUser1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber", matchesPattern("^\\d{3}\\*{4}\\d{4}$")))
                .andExpect(jsonPath("$.idCard", matchesPattern("^\\d{6}\\*{8}\\d{4}$")))
                .andExpect(jsonPath("$.realName", matchesPattern("^.{1}\\*$")))
                .andExpect(jsonPath("$.password", nullValue())); // 确保密码不返回
    }

    @Test
    void testPaginationWorkflow() throws Exception {
        // 创建更多测试数据
        for (int i = 4; i <= 15; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setPhoneNumber("1380013800" + (i % 10));
            user.setPassword("password");
            user.setStatus("ACTIVE");
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        // 测试第一页
        Map<String, Object> searchCriteria = new HashMap<>();
        searchCriteria.put("status", "ACTIVE");

        mockMvc.perform(post("/api/admin/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCriteria))
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.number", is(0)));

        // 测试第二页
        mockMvc.perform(post("/api/admin/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCriteria))
                .param("page", "1")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.number", is(1)));
    }
}