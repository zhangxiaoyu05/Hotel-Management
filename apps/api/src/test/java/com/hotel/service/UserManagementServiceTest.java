package com.hotel.service;

import com.hotel.dto.admin.user.UserManagementDTO;
import com.hotel.dto.admin.user.UserSearchCriteria;
import com.hotel.dto.admin.user.UserStatisticsDTO;
import com.hotel.entity.User;
import com.hotel.entity.UserOperationHistory;
import com.hotel.repository.UserRepository;
import com.hotel.repository.UserOperationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserOperationHistoryRepository historyRepository;

    @InjectMocks
    private UserManagementService userManagementService;

    private User testUser;
    private UserSearchCriteria searchCriteria;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("13800138000");
        testUser.setStatus("ACTIVE");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastLoginAt(LocalDateTime.now());

        searchCriteria = new UserSearchCriteria();
        searchCriteria.setUsername("test");
        searchCriteria.setEmail("example.com");
        searchCriteria.setStatus("ACTIVE");

        pageable = PageRequest.of(0, 10);

        // 设置必要的私有字段
        ReflectionTestUtils.setField(userManagementService, "userRepository", userRepository);
        ReflectionTestUtils.setField(userManagementService, "historyRepository", historyRepository);
    }

    @Test
    void testSearchUsers_Success() {
        // 准备测试数据
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);

        // 执行测试
        Page<UserManagementDTO> result = userManagementService.searchUsers(searchCriteria, pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        UserManagementDTO dto = result.getContent().get(0);
        assertEquals("testuser", dto.getUsername());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("138****8000", dto.getPhoneNumber()); // 验证数据脱敏
        assertEquals("ACTIVE", dto.getStatus());

        verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testSearchUsers_EmptyResult() {
        Page<User> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        Page<UserManagementDTO> result = userManagementService.searchUsers(searchCriteria, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<UserManagementDTO> result = userManagementService.getUserById(1L);

        assertTrue(result.isPresent());
        UserManagementDTO dto = result.get();
        assertEquals("testuser", dto.getUsername());
        assertEquals("test@example.com", dto.getEmail());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<UserManagementDTO> result = userManagementService.getUserById(1L);

        assertFalse(result.isPresent());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testUpdateUserStatus_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean result = userManagementService.updateUserStatus(1L, "INACTIVE", "admin");

        assertTrue(result);
        assertEquals("INACTIVE", testUser.getStatus());

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
        verify(historyRepository, times(1)).save(any(UserOperationHistory.class));
    }

    @Test
    void testUpdateUserStatus_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        boolean result = userManagementService.updateUserStatus(1L, "INACTIVE", "admin");

        assertFalse(result);

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
        verify(historyRepository, never()).save(any(UserOperationHistory.class));
    }

    @Test
    void testGetUserStatistics() {
        long totalUsers = 100L;
        long activeUsers = 80L;
        long inactiveUsers = 20L;
        long newUsersThisMonth = 10L;

        when(userRepository.count()).thenReturn(totalUsers);
        when(userRepository.countByStatus("ACTIVE")).thenReturn(activeUsers);
        when(userRepository.countByStatus("INACTIVE")).thenReturn(inactiveUsers);
        when(userRepository.countNewUsersThisMonth()).thenReturn(newUsersThisMonth);

        UserStatisticsDTO result = userManagementService.getUserStatistics();

        assertNotNull(result);
        assertEquals(totalUsers, result.getTotalUsers());
        assertEquals(activeUsers, result.getActiveUsers());
        assertEquals(inactiveUsers, result.getInactiveUsers());
        assertEquals(newUsersThisMonth, result.getNewUsersThisMonth());

        verify(userRepository, times(1)).count();
        verify(userRepository, times(1)).countByStatus("ACTIVE");
        verify(userRepository, times(1)).countByStatus("INACTIVE");
        verify(userRepository, times(1)).countNewUsersThisMonth();
    }

    @Test
    void testBatchUpdateUserStatus_Success() {
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        String newStatus = "INACTIVE";
        String operator = "admin";

        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);
        User user3 = new User();
        user3.setId(3L);

        List<User> users = Arrays.asList(user1, user2, user3);
        when(userRepository.findAllById(userIds)).thenReturn(users);
        when(userRepository.saveAll(users)).thenReturn(users);

        int result = userManagementService.batchUpdateUserStatus(userIds, newStatus, operator);

        assertEquals(3, result);
        users.forEach(user -> assertEquals(newStatus, user.getStatus()));

        verify(userRepository, times(1)).findAllById(userIds);
        verify(userRepository, times(1)).saveAll(users);
        verify(historyRepository, times(3)).save(any(UserOperationHistory.class));
    }

    @Test
    void testBatchUpdateUserStatus_EmptyList() {
        List<Long> userIds = Arrays.asList();

        int result = userManagementService.batchUpdateUserStatus(userIds, "INACTIVE", "admin");

        assertEquals(0, result);

        verify(userRepository, never()).findAllById(any());
        verify(userRepository, never()).saveAll(any());
        verify(historyRepository, never()).save(any(UserOperationHistory.class));
    }

    @Test
    void testMaskSensitiveData() {
        UserManagementDTO dto = new UserManagementDTO();
        dto.setPhoneNumber("13800138000");
        dto.setIdCard("123456789012345678");
        dto.setRealName("张三");

        // 使用反射调用私有方法
        ReflectionTestUtils.invokeMethod(userManagementService, "maskSensitiveData", dto);

        assertEquals("138****8000", dto.getPhoneNumber());
        assertEquals("123456********5678", dto.getIdCard());
        assertEquals("张*", dto.getRealName());
    }

    @Test
    void testBuildSearchSpecification() {
        Specification<User> specification = ReflectionTestUtils.invokeMethod(
            userManagementService, "buildSearchSpecification", searchCriteria);

        assertNotNull(specification);
    }
}