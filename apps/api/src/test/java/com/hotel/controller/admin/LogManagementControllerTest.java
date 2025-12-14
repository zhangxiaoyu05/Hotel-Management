package com.hotel.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.controller.BaseController;
import com.hotel.dto.log.LogExportRequest;
import com.hotel.dto.log.LogSearchRequest;
import com.hotel.entity.log.ErrorLog;
import com.hotel.entity.log.LoginLog;
import com.hotel.entity.log.OperationLog;
import com.hotel.service.LogManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LogManagementController 单元测试
 * 测试日志管理控制器的各个API端点
 */
@WebMvcTest(LogManagementController.class)
@ActiveProfiles("test")
@DisplayName("日志管理控制器测试")
class LogManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogManagementService logManagementService;

    @Autowired
    private ObjectMapper objectMapper;

    private LogSearchRequest searchRequest;
    private LogExportRequest exportRequest;
    private OperationLog operationLog;
    private LoginLog loginLog;
    private ErrorLog errorLog;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        searchRequest = new LogSearchRequest();
        searchRequest.setPage(1);
        searchRequest.setSize(10);
        searchRequest.setUsername("testuser");
        searchRequest.setStartTime(LocalDateTime.now().minusDays(7));
        searchRequest.setEndTime(LocalDateTime.now());

        exportRequest = new LogExportRequest();
        exportRequest.setLogType("operation");
        exportRequest.setExportFormat("csv");
        exportRequest.setMaxRecords(1000);

        // 创建操作日志测试数据
        operationLog = new OperationLog();
        operationLog.setId(1L);
        operationLog.setUserId(1L);
        operationLog.setUsername("testuser");
        operationLog.setOperation("CREATE_USER");
        operationLog.setMethod("POST");
        operationLog.setParams("{\"username\":\"test\",\"password\":\"***\"}");
        operationLog.setTime(100L);
        operationLog.setIp("127.0.0.1");
        operationLog.setUserAgent("Mozilla/5.0");
        operationLog.setCreateTime(LocalDateTime.now());

        // 创建登录日志测试数据
        loginLog = new LoginLog();
        loginLog.setId(1L);
        loginLog.setUsername("testuser");
        loginLog.setLoginType("PASSWORD");
        loginLog.setIp("127.0.0.1");
        loginLog.setLocation("本地");
        loginLog.setBrowser("Chrome");
        loginLog.setOs("Windows");
        loginLog.setStatus("SUCCESS");
        loginLog.setMessage("登录成功");
        loginLog.setCreateTime(LocalDateTime.now());

        // 创建错误日志测试数据
        errorLog = new ErrorLog();
        errorLog.setId(1L);
        errorLog.setExceptionType("NullPointerException");
        errorLog.setMessage("空指针异常");
        errorLog.setStackTrace("java.lang.NullPointerException\n    at com.example.Service.method(Service.java:123)");
        errorLog.setClassName("com.example.Service");
        errorLog.setMethodName("method");
        errorLog.setFileName("Service.java");
        errorLog.setLineNumber(123);
        errorLog.setUrl("/api/test");
        errorLog.setParams("{\"id\":null}");
        errorLog.setIp("127.0.0.1");
        errorLog.setUserAgent("Mozilla/5.0");
        errorLog.setCreateTime(LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("获取操作日志 - 成功")
    void getOperationLogs_Success() throws Exception {
        // Given
        IPage<OperationLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getRecords()).thenReturn(Arrays.asList(operationLog));
        when(page.getCurrent()).thenReturn(1L);
        when(page.getSize()).thenReturn(10L);

        when(logManagementService.getOperationLogs(any(LogSearchRequest.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .param("username", "testuser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取操作日志成功"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("testuser"))
                .andExpect(jsonPath("$.data.records[0].operation").value("CREATE_USER"));

        verify(logManagementService, times(1)).getOperationLogs(any(LogSearchRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("获取操作日志 - 服务异常")
    void getOperationLogs_ServiceException() throws Exception {
        // Given
        when(logManagementService.getOperationLogs(any(LogSearchRequest.class)))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // When & Then
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("获取操作日志失败: 数据库连接失败"));

        verify(logManagementService, times(1)).getOperationLogs(any(LogSearchRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("获取登录日志 - 成功")
    void getLoginLogs_Success() throws Exception {
        // Given
        IPage<LoginLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getRecords()).thenReturn(Arrays.asList(loginLog));
        when(page.getCurrent()).thenReturn(1L);
        when(page.getSize()).thenReturn(10L);

        when(logManagementService.getLoginLogs(any(LogSearchRequest.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/v1/admin/logs/login")
                        .param("page", "1")
                        .param("size", "10")
                        .param("username", "testuser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取登录日志成功"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("testuser"))
                .andExpect(jsonPath("$.data.records[0].loginType").value("PASSWORD"));

        verify(logManagementService, times(1)).getLoginLogs(any(LogSearchRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("获取错误日志 - 成功")
    void getErrorLogs_Success() throws Exception {
        // Given
        IPage<ErrorLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getRecords()).thenReturn(Arrays.asList(errorLog));
        when(page.getCurrent()).thenReturn(1L);
        when(page.getSize()).thenReturn(10L);

        when(logManagementService.getErrorLogs(any(LogSearchRequest.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/v1/admin/logs/error")
                        .param("page", "1")
                        .param("size", "10")
                        .param("level", "ERROR")
                        .param("module", "UserService")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取错误日志成功"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].exceptionType").value("NullPointerException"));

        verify(logManagementService, times(1)).getErrorLogs(any(LogSearchRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("导出日志 - CSV格式成功")
    void exportLogs_CsvFormat_Success() throws Exception {
        // Given
        byte[] csvData = "用户名,操作,时间\nuser1,CREATE_USER,2024-01-01\n".getBytes(StandardCharsets.UTF_8);
        when(logManagementService.exportLogs(any(LogExportRequest.class)))
                .thenReturn(csvData);

        // When & Then
        mockMvc.perform(post("/v1/admin/logs/export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        contains("attachment; filename=\"操作日志_")))
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(content().bytes(csvData));

        verify(logManagementService, times(1)).exportLogs(any(LogExportRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("导出日志 - Excel格式成功")
    void exportLogs_ExcelFormat_Success() throws Exception {
        // Given
        exportRequest.setExportFormat("xlsx");
        byte[] excelData = new byte[]{(byte) 0x50, 0x4B, 0x03, 0x04}; // Excel文件头
        when(logManagementService.exportLogs(any(LogExportRequest.class)))
                .thenReturn(excelData);

        // When & Then
        mockMvc.perform(post("/v1/admin/logs/export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        contains("attachment; filename=\"操作日志_")))
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(content().bytes(excelData));

        verify(logManagementService, times(1)).exportLogs(any(LogExportRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("导出日志 - IO异常")
    void exportLogs_IOException() throws Exception {
        // Given
        when(logManagementService.exportLogs(any(LogExportRequest.class)))
                .thenThrow(new java.io.IOException("文件生成失败"));

        // When & Then
        mockMvc.perform(post("/v1/admin/logs/export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(logManagementService, times(1)).exportLogs(any(LogExportRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("导出日志 - 其他异常")
    void exportLogs_GeneralException() throws Exception {
        // Given
        when(logManagementService.exportLogs(any(LogExportRequest.class)))
                .thenThrow(new RuntimeException("服务不可用"));

        // When & Then
        mockMvc.perform(post("/v1/admin/logs/export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(logManagementService, times(1)).exportLogs(any(LogExportRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("获取日志统计信息 - 成功")
    void getLogStatistics_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/admin/logs/statistics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取日志统计成功"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("权限验证 - 非管理员用户访问被拒绝")
    void accessDenied_ForNonAdminUser() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(logManagementService, never()).getOperationLogs(any());
    }

    @Test
    @DisplayName("权限验证 - 未认证用户访问被拒绝")
    void accessDenied_ForUnauthenticatedUser() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(logManagementService, never()).getOperationLogs(any());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("参数验证 - 无效的页码")
    void getOperationLogs_InvalidPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "0") // 无效的页码
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(logManagementService, never()).getOperationLogs(any());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("参数验证 - 无效的页面大小")
    void getOperationLogs_InvalidSize() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/admin/logs/operation")
                        .param("page", "1")
                        .param("size", "0") // 无效的页面大小
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(logManagementService, never()).getOperationLogs(any());
    }
}