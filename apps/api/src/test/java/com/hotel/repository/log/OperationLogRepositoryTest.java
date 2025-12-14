package com.hotel.repository.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.log.OperationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OperationLogRepository 集成测试
 * 测试操作日志的数据访问层功能
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("操作日志Repository测试")
class OperationLogRepositoryTest {

    @Autowired
    private OperationLogRepository operationLogRepository;

    private OperationLog testLog;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testLog = new OperationLog();
        testLog.setUserId(1L);
        testLog.setUsername("testuser");
        testLog.setOperation("CREATE_USER");
        testLog.setMethod("POST");
        testLog.setParams("{\"username\":\"test\",\"password\":\"***\"}");
        testLog.setTime(150L);
        testLog.setIp("127.0.0.1");
        testLog.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        testLog.setStatus("SUCCESS");
        testLog.setErrorMessage(null);
        testLog.setCreateTime(LocalDateTime.now());

        // 清理数据库
        operationLogRepository.deleteAll();
    }

    @Test
    @DisplayName("保存操作日志 - 成功")
    void saveLog_Success() {
        // When
        operationLogRepository.insert(testLog);

        // Then
        assertNotNull(testLog.getId());

        OperationLog saved = operationLogRepository.selectById(testLog.getId());
        assertNotNull(saved);
        assertEquals("testuser", saved.getUsername());
        assertEquals("CREATE_USER", saved.getOperation());
        assertEquals("POST", saved.getMethod());
        assertEquals(150L, saved.getTime());
        assertEquals("127.0.0.1", saved.getIp());
        assertEquals("SUCCESS", saved.getStatus());
    }

    @Test
    @DisplayName("根据ID查询操作日志 - 成功")
    void findById_Success() {
        // Given
        operationLogRepository.insert(testLog);
        Long id = testLog.getId();

        // When
        OperationLog found = operationLogRepository.selectById(id);

        // Then
        assertNotNull(found);
        assertEquals(id, found.getId());
        assertEquals("testuser", found.getUsername());
        assertEquals("CREATE_USER", found.getOperation());
    }

    @Test
    @DisplayName("根据ID查询操作日志 - 不存在")
    void findById_NotFound() {
        // When
        OperationLog found = operationLogRepository.selectById(999999L);

        // Then
        assertNull(found);
    }

    @Test
    @DisplayName("分页查询所有操作日志 - 成功")
    void findAllWithPagination_Success() {
        // Given
        // 创建多条测试数据
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

        Page<OperationLog> page = new Page<>(1, 10);

        // When
        IPage<OperationLog> result = operationLogRepository.selectPage(page, null);

        // Then
        assertNotNull(result);
        assertEquals(15, result.getTotal());
        assertEquals(2, result.getPages());
        assertEquals(10, result.getRecords().size());
        assertEquals(1, result.getCurrent());
    }

    @Test
    @DisplayName("搜索日志 - 按用户名")
    void searchLogs_ByUsername() {
        // Given
        // 创建不同用户的日志
        OperationLog log1 = createLog("user1", "CREATE_USER", "127.0.0.1");
        OperationLog log2 = createLog("user2", "DELETE_USER", "127.0.0.2");
        OperationLog log3 = createLog("user1", "UPDATE_USER", "127.0.0.3");

        operationLogRepository.insert(log1);
        operationLogRepository.insert(log2);
        operationLogRepository.insert(log3);

        Page<OperationLog> page = new Page<>(1, 10);

        // When
        IPage<OperationLog> result = operationLogRepository.searchLogs(
                page, "user1", null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());

        // 验证返回的都是user1的日志
        result.getRecords().forEach(log -> {
            assertEquals("user1", log.getUsername());
        });
    }

    @Test
    @DisplayName("搜索日志 - 按操作类型")
    void searchLogs_ByOperation() {
        // Given
        OperationLog log1 = createLog("user1", "CREATE_USER", "127.0.0.1");
        OperationLog log2 = createLog("user2", "CREATE_USER", "127.0.0.2");
        OperationLog log3 = createLog("user3", "DELETE_USER", "127.0.0.3");

        operationLogRepository.insert(log1);
        operationLogRepository.insert(log2);
        operationLogRepository.insert(log3);

        Page<OperationLog> page = new Page<>(1, 10);

        // When
        IPage<OperationLog> result = operationLogRepository.searchLogs(
                page, null, "CREATE_USER", null, null, null, null, null, null
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());

        // 验证返回的都是CREATE_USER的日志
        result.getRecords().forEach(log -> {
            assertEquals("CREATE_USER", log.getOperation());
        });
    }

    @Test
    @DisplayName("搜索日志 - 按IP地址")
    void searchLogs_ByIp() {
        // Given
        OperationLog log1 = createLog("user1", "CREATE_USER", "192.168.1.100");
        OperationLog log2 = createLog("user2", "DELETE_USER", "192.168.1.100");
        OperationLog log3 = createLog("user3", "UPDATE_USER", "192.168.1.200");

        operationLogRepository.insert(log1);
        operationLogRepository.insert(log2);
        operationLogRepository.insert(log3);

        Page<OperationLog> page = new Page<>(1, 10);

        // When
        IPage<OperationLog> result = operationLogRepository.searchLogs(
                page, null, null, null, null, "192.168.1.100", null, null, null
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());

        // 验证返回的都是来自192.168.1.100的日志
        result.getRecords().forEach(log -> {
            assertEquals("192.168.1.100", log.getIp());
        });
    }

    @Test
    @DisplayName("搜索日志 - 按时间范围")
    void searchLogs_ByTimeRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);
        LocalDateTime threeHoursAgo = now.minusHours(3);

        OperationLog log1 = createLogWithTime("user1", threeHoursAgo);
        OperationLog log2 = createLogWithTime("user2", twoHoursAgo);
        OperationLog log3 = createLogWithTime("user3", oneHourAgo);

        operationLogRepository.insert(log1);
        operationLogRepository.insert(log2);
        operationLogRepository.insert(log3);

        Page<OperationLog> page = new Page<>(1, 10);

        // When - 查询最近2小时内的日志
        IPage<OperationLog> result = operationLogRepository.searchLogs(
                page, null, null, twoHoursAgo, now, null, null, null, null
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    @DisplayName("搜索日志 - 按状态")
    void searchLogs_ByStatus() {
        // Given
        OperationLog log1 = createLogWithStatus("user1", "SUCCESS");
        OperationLog log2 = createLogWithStatus("user2", "FAILED");
        OperationLog log3 = createLogWithStatus("user3", "SUCCESS");

        operationLogRepository.insert(log1);
        operationLogRepository.insert(log2);
        operationLogRepository.insert(log3);

        Page<OperationLog> page = new Page<>(1, 10);

        // When
        IPage<OperationLog> result = operationLogRepository.searchLogs(
                page, null, null, null, null, null, "SUCCESS", null, null
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());

        // 验证返回的都是SUCCESS状态的日志
        result.getRecords().forEach(log -> {
            assertEquals("SUCCESS", log.getStatus());
        });
    }

    @Test
    @DisplayName("搜索日志 - 复合条件")
    void searchLogs_CombinedConditions() {
        // Given
        OperationLog log1 = createLog("user1", "CREATE_USER", "192.168.1.100");
        log1.setStatus("SUCCESS");
        OperationLog log2 = createLog("user1", "DELETE_USER", "192.168.1.200");
        log2.setStatus("FAILED");
        OperationLog log3 = createLog("user2", "CREATE_USER", "192.168.1.100");
        log3.setStatus("SUCCESS");

        operationLogRepository.insert(log1);
        operationLogRepository.insert(log2);
        operationLogRepository.insert(log3);

        Page<OperationLog> page = new Page<>(1, 10);

        // When - 查询user1的CREATE_USER且状态为SUCCESS的日志
        IPage<OperationLog> result = operationLogRepository.searchLogs(
                page, "user1", "CREATE_USER", null, null, null, "SUCCESS", null, null
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());

        OperationLog found = result.getRecords().get(0);
        assertEquals("user1", found.getUsername());
        assertEquals("CREATE_USER", found.getOperation());
        assertEquals("SUCCESS", found.getStatus());
    }

    @Test
    @DisplayName("搜索日志 - 排序")
    void searchLogs_WithSorting() {
        // Given
        for (int i = 0; i < 5; i++) {
            OperationLog log = new OperationLog();
            log.setUserId((long) i);
            log.setUsername("user" + i);
            log.setOperation("TEST");
            log.setMethod("GET");
            log.setTime((long) (100 - i * 10)); // 不同的执行时间
            log.setIp("127.0.0.1");
            log.setStatus("SUCCESS");
            log.setCreateTime(LocalDateTime.now().minusMinutes(i));
            operationLogRepository.insert(log);
        }

        Page<OperationLog> page = new Page<>(1, 10);

        // When - 按执行时间降序排序
        IPage<OperationLog> result = operationLogRepository.searchLogs(
                page, null, null, null, null, null, null, "time", "desc"
        );

        // Then
        assertNotNull(result);
        assertEquals(5, result.getTotal());
        assertEquals(5, result.getRecords().size());

        // 验证排序结果
        List<OperationLog> logs = result.getRecords();
        for (int i = 0; i < logs.size() - 1; i++) {
            assertTrue(logs.get(i).getTime() >= logs.get(i + 1).getTime());
        }
    }

    @Test
    @DisplayName("删除操作日志 - 成功")
    void deleteLog_Success() {
        // Given
        operationLogRepository.insert(testLog);
        Long id = testLog.getId();
        assertNotNull(operationLogRepository.selectById(id));

        // When
        int deleted = operationLogRepository.deleteById(id);

        // Then
        assertEquals(1, deleted);
        assertNull(operationLogRepository.selectById(id));
    }

    @Test
    @DisplayName("更新操作日志 - 成功")
    void updateLog_Success() {
        // Given
        operationLogRepository.insert(testLog);
        Long id = testLog.getId();

        testLog.setStatus("FAILED");
        testLog.setErrorMessage("操作失败");

        // When
        int updated = operationLogRepository.updateById(testLog);

        // Then
        assertEquals(1, updated);

        OperationLog updatedLog = operationLogRepository.selectById(id);
        assertNotNull(updatedLog);
        assertEquals("FAILED", updatedLog.getStatus());
        assertEquals("操作失败", updatedLog.getErrorMessage());
    }

    /**
     * 辅助方法：创建测试用的操作日志
     */
    private OperationLog createLog(String username, String operation, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(1L);
        log.setUsername(username);
        log.setOperation(operation);
        log.setMethod("POST");
        log.setParams("{}");
        log.setTime(100L);
        log.setIp(ip);
        log.setUserAgent("Test Agent");
        log.setStatus("SUCCESS");
        log.setCreateTime(LocalDateTime.now());
        return log;
    }

    /**
     * 辅助方法：创建指定时间的操作日志
     */
    private OperationLog createLogWithTime(String username, LocalDateTime createTime) {
        OperationLog log = createLog(username, "TEST", "127.0.0.1");
        log.setCreateTime(createTime);
        return log;
    }

    /**
     * 辅助方法：创建指定状态的操作日志
     */
    private OperationLog createLogWithStatus(String username, String status) {
        OperationLog log = createLog(username, "TEST", "127.0.0.1");
        log.setStatus(status);
        return log;
    }
}