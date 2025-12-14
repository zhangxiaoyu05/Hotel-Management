package com.hotel.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.log.LogExportRequest;
import com.hotel.entity.log.OperationLog;
import com.hotel.repository.log.OperationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 日志管理集成测试
 * 测试日志管理功能从头到尾的完整流程
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("日志管理集成测试")
class LogManagementIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 清理数据库
        operationLogRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("完整的日志管理流程测试")
    void completeLogManagementFlow() throws Exception {
        // Step 1: 创建一些测试日志数据
        OperationLog log1 = createTestLog("user1", "CREATE_USER", "127.0.0.1");
        OperationLog log2 = createTestLog("user2", "DELETE_USER", "192.168.1.100");
        OperationLog log3 = createTestLog("user1", "UPDATE_USER", "127.0.0.1");

        operationLogRepository.insert(log1);
        operationLogRepository.insert(log2);
        operationLogRepository.insert(log3);

        // Step 2: 测试获取所有操作日志
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records", hasSize(3)));

        // Step 3: 测试按用户名搜索
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .param("username", "user1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records", hasSize(2)))
                .andExpect(jsonPath("$.data.records[*].username", everyItem(equalTo("user1"))));

        // Step 4: 测试按操作类型搜索
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .param("operation", "CREATE_USER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].operation").value("CREATE_USER"));

        // Step 5: 测试按IP地址搜索
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .param("ip", "127.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[*].ip", everyItem(equalTo("127.0.0.1"))));

        // Step 6: 测试复合条件搜索
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .param("username", "user1")
                        .param("ip", "127.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[*].username", everyItem(equalTo("user1"))))
                .andExpect(jsonPath("$.data.records[*].ip", everyItem(equalTo("127.0.0.1"))));

        // Step 7: 测试导出功能 - CSV格式
        LogExportRequest exportRequest = new LogExportRequest();
        exportRequest.setLogType("operation");
        exportRequest.setExportFormat("csv");
        exportRequest.setMaxRecords(1000);
        exportRequest.setIncludeSensitiveInfo(false);

        mockMvc.perform(post("/v1/admin/logs/export")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Type", contains("text/csv")))
                .andExpect(content().bytes(notNullValue()));

        // Step 8: 测试导出功能 - Excel格式
        exportRequest.setExportFormat("xlsx");

        mockMvc.perform(post("/v1/admin/logs/export")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Type", contains("application/octet-stream")))
                .andExpect(content().bytes(notNullValue()));

        // Step 9: 测试获取日志统计信息
        mockMvc.perform(get("/v1/admin/logs/statistics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("时间范围搜索集成测试")
    void timeRangeSearchIntegrationTest() throws Exception {
        // Given - 创建不同时间的日志
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursAgo = now.minusHours(2);
        LocalDateTime fiveHoursAgo = now.minusHours(5);

        OperationLog log1 = createTestLogWithTime("user1", "TEST_OPERATION", now);
        OperationLog log2 = createTestLogWithTime("user2", "TEST_OPERATION", twoHoursAgo);
        OperationLog log3 = createTestLogWithTime("user3", "TEST_OPERATION", fiveHoursAgo);

        operationLogRepository.insert(log1);
        operationLogRepository.insert(log2);
        operationLogRepository.insert(log3);

        // When & Then - 查询最近3小时的日志
        LocalDateTime threeHoursAgo = now.minusHours(3);
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .param("startTime", threeHoursAgo.toString())
                        .param("endTime", now.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("分页功能集成测试")
    void paginationIntegrationTest() throws Exception {
        // Given - 创建超过一页的数据
        for (int i = 0; i < 15; i++) {
            OperationLog log = new OperationLog();
            log.setUserId((long) i);
            log.setUsername("user" + i);
            log.setOperation("TEST_OPERATION");
            log.setMethod("GET");
            log.setTime(100L + i);
            log.setIp("192.168.1." + (i + 1));
            log.setUserAgent("Test Agent");
            log.setStatus("SUCCESS");
            log.setCreateTime(LocalDateTime.now().minusMinutes(i));
            operationLogRepository.insert(log);
        }

        // When & Then - 测试第一页
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.records", hasSize(10)));

        // When & Then - 测试第二页
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "2")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.records", hasSize(5)));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("权限控制集成测试")
    void authorizationIntegrationTest() throws Exception {
        // When & Then - 普通用户应该无法访问日志管理接口
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/admin/logs/export")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未认证用户访问控制测试")
    void unauthenticatedUserAccessTest() throws Exception {
        // When & Then - 未认证用户应该无法访问日志管理接口
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/v1/admin/logs/export")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("参数验证集成测试")
    void parameterValidationIntegrationTest() throws Exception {
        // When & Then - 无效的页码
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // When & Then - 无效的页面大小
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // When & Then - 过大的页面大小
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "1000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("错误处理集成测试")
    void errorHandlingIntegrationTest() throws Exception {
        // When & Then - 导出不支持的日志类型
        LogExportRequest invalidRequest = new LogExportRequest();
        invalidRequest.setLogType("invalid_type");
        invalidRequest.setExportFormat("csv");

        mockMvc.perform(post("/v1/admin/logs/export")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // When & Then - 导出无效的格式
        LogExportRequest invalidFormatRequest = new LogExportRequest();
        invalidFormatRequest.setLogType("operation");
        invalidFormatRequest.setExportFormat("invalid_format");

        mockMvc.perform(post("/v1/admin/logs/export")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidFormatRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    /**
     * 创建测试用的操作日志
     */
    private OperationLog createTestLog(String username, String operation, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(1L);
        log.setUsername(username);
        log.setOperation(operation);
        log.setMethod("POST");
        log.setParams("{\"test\": true}");
        log.setTime(150L);
        log.setIp(ip);
        log.setUserAgent("Test Agent");
        log.setStatus("SUCCESS");
        log.setCreateTime(LocalDateTime.now());
        return log;
    }

    /**
     * 创建指定时间的测试操作日志
     */
    private OperationLog createTestLogWithTime(String username, String operation, LocalDateTime createTime) {
        OperationLog log = createTestLog(username, operation, "127.0.0.1");
        log.setCreateTime(createTime);
        return log;
    }
}