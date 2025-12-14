package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hotel.dto.log.LogExportRequest;
import com.hotel.dto.log.LogSearchRequest;
import com.hotel.entity.log.ErrorLog;
import com.hotel.entity.log.LoginLog;
import com.hotel.entity.log.OperationLog;
import com.hotel.repository.log.ErrorLogRepository;
import com.hotel.repository.log.LoginLogRepository;
import com.hotel.repository.log.OperationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LogManagementService 单元测试
 * 测试日志管理服务的业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("日志管理服务测试")
class LogManagementServiceTest {

    @Mock
    private OperationLogRepository operationLogRepository;

    @Mock
    private LoginLogRepository loginLogRepository;

    @Mock
    private ErrorLogRepository errorLogRepository;

    @InjectMocks
    private LogManagementService logManagementService;

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
        searchRequest.setOperation("CREATE_USER");
        searchRequest.setStartTime(LocalDateTime.now().minusDays(7));
        searchRequest.setEndTime(LocalDateTime.now());

        exportRequest = new LogExportRequest();
        exportRequest.setLogType("operation");
        exportRequest.setExportFormat("csv");
        exportRequest.setMaxRecords(1000);
        exportRequest.setIncludeSensitiveInfo(false);

        // 创建测试用的操作日志
        operationLog = new OperationLog();
        operationLog.setId(1L);
        operationLog.setUserId(1L);
        operationLog.setUsername("testuser");
        operationLog.setOperation("CREATE_USER");
        operationLog.setMethod("POST");
        operationLog.setParams("{\"username\":\"test\"}");
        operationLog.setTime(150L);
        operationLog.setIp("127.0.0.1");
        operationLog.setUserAgent("Mozilla/5.0");
        operationLog.setStatus("SUCCESS");
        operationLog.setCreateTime(LocalDateTime.now());

        // 创建测试用的登录日志
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

        // 创建测试用的错误日志
        errorLog = new ErrorLog();
        errorLog.setId(1L);
        errorLog.setExceptionType("NullPointerException");
        errorLog.setMessage("空指针异常");
        errorLog.setStackTrace("java.lang.NullPointerException");
        errorLog.setModule("UserService");
        errorLog.setLevel("ERROR");
        errorLog.setClassName("com.example.UserService");
        errorLog.setMethodName("createUser");
        errorLog.setFileName("UserService.java");
        errorLog.setLineNumber(123);
        errorLog.setUrl("/api/users");
        errorLog.setParams("{\"id\":null}");
        errorLog.setIp("127.0.0.1");
        errorLog.setUserAgent("Mozilla/5.0");
        errorLog.setUsername("testuser");
        errorLog.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("获取操作日志 - 成功")
    void getOperationLogs_Success() {
        // Given
        IPage<OperationLog> expectedPage = mock(IPage.class);
        when(expectedPage.getTotal()).thenReturn(1L);
        when(expectedPage.getRecords()).thenReturn(Arrays.asList(operationLog));
        when(expectedPage.getCurrent()).thenReturn(1L);
        when(expectedPage.getSize()).thenReturn(10L);

        when(operationLogRepository.searchLogs(any(), anyString(), anyString(), any(), any(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(expectedPage);

        // When
        IPage<OperationLog> result = logManagementService.getOperationLogs(searchRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("testuser", result.getRecords().get(0).getUsername());
        assertEquals("CREATE_USER", result.getRecords().get(0).getOperation());

        verify(operationLogRepository, times(1)).searchLogs(
                any(), eq("testuser"), eq("CREATE_USER"), any(), any(),
                eq(null), eq(null), eq(null), eq(null)
        );
    }

    @Test
    @DisplayName("获取登录日志 - 成功")
    void getLoginLogs_Success() {
        // Given
        IPage<LoginLog> expectedPage = mock(IPage.class);
        when(expectedPage.getTotal()).thenReturn(1L);
        when(expectedPage.getRecords()).thenReturn(Arrays.asList(loginLog));
        when(expectedPage.getCurrent()).thenReturn(1L);
        when(expectedPage.getSize()).thenReturn(10L);

        when(loginLogRepository.searchLogs(any(), anyString(), anyString(), any(), any(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(expectedPage);

        // When
        IPage<LoginLog> result = logManagementService.getLoginLogs(searchRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("testuser", result.getRecords().get(0).getUsername());
        assertEquals("PASSWORD", result.getRecords().get(0).getLoginType());

        verify(loginLogRepository, times(1)).searchLogs(
                any(), eq("testuser"), eq(null), any(), any(),
                eq(null), eq(null), eq(null), eq(null)
        );
    }

    @Test
    @DisplayName("获取错误日志 - 成功")
    void getErrorLogs_Success() {
        // Given
        searchRequest.setLevel("ERROR");
        searchRequest.setModule("UserService");

        IPage<ErrorLog> expectedPage = mock(IPage.class);
        when(expectedPage.getTotal()).thenReturn(1L);
        when(expectedPage.getRecords()).thenReturn(Arrays.asList(errorLog));
        when(expectedPage.getCurrent()).thenReturn(1L);
        when(expectedPage.getSize()).thenReturn(10L);

        when(errorLogRepository.searchLogs(any(), anyString(), anyString(), anyString(), any(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(expectedPage);

        // When
        IPage<ErrorLog> result = logManagementService.getErrorLogs(searchRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("testuser", result.getRecords().get(0).getUsername());
        assertEquals("NullPointerException", result.getRecords().get(0).getExceptionType());

        verify(errorLogRepository, times(1)).searchLogs(
                any(), eq("testuser"), eq("ERROR"), eq("UserService"), any(),
                eq(null), eq(null), eq(null)
        );
    }

    @Test
    @DisplayName("导出操作日志 - CSV格式")
    void exportLogs_OperationLogCsvFormat() throws IOException {
        // Given
        IPage<OperationLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getRecords()).thenReturn(Arrays.asList(operationLog));

        when(operationLogRepository.searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(page);

        // When
        byte[] result = logManagementService.exportLogs(exportRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);

        String csvContent = new String(result);
        assertTrue(csvContent.contains("ID,用户名,操作,方法,IP,执行时长(ms),状态,创建时间"));
        assertTrue(csvContent.contains("testuser"));
        assertTrue(csvContent.contains("CREATE_USER"));

        verify(operationLogRepository, times(1)).searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("导出操作日志 - Excel格式")
    void exportLogs_OperationLogExcelFormat() throws IOException {
        // Given
        exportRequest.setExportFormat("xlsx");
        IPage<OperationLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getRecords()).thenReturn(Arrays.asList(operationLog));

        when(operationLogRepository.searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(page);

        // When
        byte[] result = logManagementService.exportLogs(exportRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
        // Excel文件的魔数验证
        assertTrue(result[0] == 0x50 && result[1] == 0x4B && result[2] == 0x03 && result[3] == 0x04);

        verify(operationLogRepository, times(1)).searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("导出登录日志 - CSV格式")
    void exportLogs_LoginLogCsvFormat() throws IOException {
        // Given
        exportRequest.setLogType("login");
        IPage<LoginLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getRecords()).thenReturn(Arrays.asList(loginLog));

        when(loginLogRepository.searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(page);

        // When
        byte[] result = logManagementService.exportLogs(exportRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);

        String csvContent = new String(result);
        assertTrue(csvContent.contains("ID,用户名,登录类型,IP,地理位置,浏览器,状态,消息,创建时间"));
        assertTrue(csvContent.contains("testuser"));
        assertTrue(csvContent.contains("PASSWORD"));

        verify(loginLogRepository, times(1)).searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("导出错误日志 - CSV格式")
    void exportLogs_ErrorLogCsvFormat() throws IOException {
        // Given
        exportRequest.setLogType("error");
        IPage<ErrorLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getRecords()).thenReturn(Arrays.asList(errorLog));

        when(errorLogRepository.searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(page);

        // When
        byte[] result = logManagementService.exportLogs(exportRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);

        String csvContent = new String(result);
        assertTrue(csvContent.contains("ID,异常类型,消息,模块,级别,类名,方法名,IP,用户名,创建时间"));
        assertTrue(csvContent.contains("testuser"));
        assertTrue(csvContent.contains("NullPointerException"));

        verify(errorLogRepository, times(1)).searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("导出日志 - 包含敏感信息")
    void exportLogs_WithSensitiveInfo() throws IOException {
        // Given
        exportRequest.setIncludeSensitiveInfo(true);
        IPage<OperationLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getRecords()).thenReturn(Arrays.asList(operationLog));

        when(operationLogRepository.searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(page);

        // When
        byte[] result = logManagementService.exportLogs(exportRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);

        String csvContent = new String(result);
        assertTrue(csvContent.contains("请求参数,用户代理,错误信息"));
        assertTrue(csvContent.contains("{\"username\":\"test\"}"));
        assertTrue(csvContent.contains("Mozilla/5.0"));
    }

    @Test
    @DisplayName("导出日志 - 不支持的日志类型")
    void exportLogs_UnsupportedLogType() {
        // Given
        exportRequest.setLogType("unsupported");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            logManagementService.exportLogs(exportRequest);
        });
    }

    @Test
    @DisplayName("CSV字段转义测试 - 包含逗号")
    void escapeCsv_WithComma() {
        // Given
        String fieldWithComma = "field,with,comma";

        // When
        // 由于escapeCsv是私有方法，我们通过导出功能间接测试
        OperationLog logWithComma = new OperationLog();
        logWithComma.setId(1L);
        logWithComma.setUsername("user,with,comma");
        logWithComma.setOperation("TEST");
        logWithComma.setMethod("GET");
        logWithComma.setTime(100L);
        logWithComma.setStatus("SUCCESS");
        logWithComma.setCreateTime(LocalDateTime.now());

        IPage<OperationLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getRecords()).thenReturn(Arrays.asList(logWithComma));
        when(operationLogRepository.searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(page);

        byte[] result = logManagementService.exportLogs(exportRequest);
        String csvContent = new String(result);

        // Then
        assertTrue(csvContent.contains("\"user,with,comma\""));
    }

    @Test
    @DisplayName("CSV字段转义测试 - 包含引号")
    void escapeCsv_WithQuotes() {
        // Given
        OperationLog logWithQuotes = new OperationLog();
        logWithQuotes.setId(1L);
        logWithQuotes.setUsername("user\"with\"quotes");
        logWithQuotes.setOperation("TEST");
        logWithQuotes.setMethod("GET");
        logWithQuotes.setTime(100L);
        logWithQuotes.setStatus("SUCCESS");
        logWithQuotes.setCreateTime(LocalDateTime.now());

        IPage<OperationLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getRecords()).thenReturn(Arrays.asList(logWithQuotes));
        when(operationLogRepository.searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(page);

        // When
        byte[] result = logManagementService.exportLogs(exportRequest);
        String csvContent = new String(result);

        // Then
        assertTrue(csvContent.contains("\"user\"\"with\"\"quotes\""));
    }

    @Test
    @DisplayName("导出日志 - 空数据集")
    void exportLogs_EmptyDataSet() throws IOException {
        // Given
        IPage<OperationLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(0L);
        when(page.getRecords()).thenReturn(Arrays.asList());

        when(operationLogRepository.searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(page);

        // When
        byte[] result = logManagementService.exportLogs(exportRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);

        String csvContent = new String(result);
        assertTrue(csvContent.contains("ID,用户名,操作,方法,IP,执行时长(ms),状态,创建时间"));
        // 只有标题行，没有数据行
        assertTrue(csvContent.split("\n").length == 1);
    }

    @Test
    @DisplayName("导出请求转换为搜索请求")
    void convertToSearchRequest() {
        // Given
        exportRequest.setUsername("testuser");
        exportRequest.setOperation("CREATE_USER");
        exportRequest.setIp("127.0.0.1");
        exportRequest.setStatus("SUCCESS");
        exportRequest.setLevel("ERROR");
        exportRequest.setModule("UserService");
        exportRequest.setLoginType("PASSWORD");
        exportRequest.setStartTime(LocalDateTime.now().minusDays(7));
        exportRequest.setEndTime(LocalDateTime.now());

        IPage<OperationLog> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(0L);
        when(page.getRecords()).thenReturn(Arrays.asList());
        when(operationLogRepository.searchLogs(any(), any(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(page);

        // When
        logManagementService.exportLogs(exportRequest);

        // Then
        verify(operationLogRepository, times(1)).searchLogs(
                any(), eq("testuser"), eq("CREATE_USER"), any(), any(),
                eq("127.0.0.1"), eq("SUCCESS"), any(), any()
        );
    }
}