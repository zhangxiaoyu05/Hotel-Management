package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.dto.admin.user.UserOperationHistoryDTO;
import com.hotel.entity.UserOperationHistory;
import com.hotel.repository.UserOperationHistoryRepository;
import com.hotel.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户操作历史服务
 */
@Service
@Slf4j
@Transactional
public class UserOperationHistoryService {

    @Autowired
    private UserOperationHistoryRepository operationHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 记录用户操作历史
     */
    public void recordOperation(Long userId, String operation, Long operator, String details, String ipAddress, String userAgent) {
        try {
            UserOperationHistory history = new UserOperationHistory();
            history.setUserId(userId);
            history.setOperation(operation);
            history.setOperator(operator != null ? operator : 0L);
            history.setDetails(details);
            history.setIpAddress(ipAddress);
            history.setUserAgent(userAgent);
            history.setCreatedAt(LocalDateTime.now());

            operationHistoryRepository.insert(history);

            log.debug("记录用户操作历史: userId={}, operation={}, details={}", userId, operation, details);

        } catch (Exception e) {
            log.error("记录用户操作历史失败", e);
            // 不抛出异常，避免影响主要业务流程
        }
    }

    /**
     * 记录登录操作
     */
    public void recordLogin(Long userId, String ipAddress, String userAgent) {
        recordOperation(userId, "LOGIN", userId, "用户登录", ipAddress, userAgent);
    }

    /**
     * 记录登出操作
     */
    public void recordLogout(Long userId, String ipAddress, String userAgent) {
        recordOperation(userId, "LOGOUT", userId, "用户登出", ipAddress, userAgent);
    }

    /**
     * 记录密码修改操作
     */
    public void recordPasswordChange(Long userId, String ipAddress, String userAgent) {
        recordOperation(userId, "PASSWORD_CHANGE", userId, "用户修改密码", ipAddress, userAgent);
    }

    /**
     * 记录资料更新操作
     */
    public void recordProfileUpdate(Long userId, String details, String ipAddress, String userAgent) {
        recordOperation(userId, "PROFILE_UPDATE", userId, details, ipAddress, userAgent);
    }

    /**
     * 记录管理员状态变更操作
     */
    public void recordStatusChange(Long userId, Long operator, String details, String ipAddress, String userAgent) {
        recordOperation(userId, "STATUS_CHANGE", operator, details, ipAddress, userAgent);
    }

    /**
     * 获取用户操作历史（分页）
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserOperationHistoryDTO> getUserOperationHistory(
            Long userId, Pageable pageable) {
        try {
            log.info("获取用户操作历史，用户ID: {}, 页码: {}, 每页数量: {}",
                    userId, pageable.getPageNumber(), pageable.getPageSize());

            Page<UserOperationHistory> page = new Page<>(
                pageable.getPageNumber() + 1,
                pageable.getPageSize()
            );

            IPage<UserOperationHistory> historyPage = operationHistoryRepository
                .findOperationHistoryByUserId(page, userId);

            List<UserOperationHistoryDTO> dtoList = historyPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return new PageImpl<>(dtoList, pageable, historyPage.getTotal());

        } catch (Exception e) {
            log.error("获取用户操作历史失败，用户ID: {}", userId, e);
            throw new RuntimeException("获取用户操作历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户指定时间段内的操作历史
     */
    @Transactional(readOnly = true)
    public List<UserOperationHistoryDTO> getUserOperationHistoryByTimeRange(
            Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            log.info("获取用户操作历史，用户ID: {}, 时间段: {} 至 {}", userId, startTime, endTime);

            List<UserOperationHistory> histories = operationHistoryRepository
                .findOperationHistoryByUserIdAndTimeRange(userId, startTime, endTime);

            return histories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户操作历史失败，用户ID: {}", userId, e);
            throw new RuntimeException("获取用户操作历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户指定操作类型的历史
     */
    @Transactional(readOnly = true)
    public List<UserOperationHistoryDTO> getUserOperationHistoryByOperation(
            Long userId, String operation) {
        try {
            log.info("获取用户操作历史，用户ID: {}, 操作类型: {}", userId, operation);

            List<UserOperationHistory> histories = operationHistoryRepository
                .findOperationHistoryByUserIdAndOperation(userId, operation);

            return histories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户操作历史失败，用户ID: {}", userId, e);
            throw new RuntimeException("获取用户操作历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户最近的操作记录
     */
    @Transactional(readOnly = true)
    public List<UserOperationHistoryDTO> getRecentUserOperations(Long userId, Integer limit) {
        try {
            log.info("获取用户最近操作记录，用户ID: {}, 限制数量: {}", userId, limit);

            List<UserOperationHistory> histories = operationHistoryRepository
                .findRecentOperationsByUserId(userId, limit != null ? limit : 10);

            return histories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户最近操作记录失败，用户ID: {}", userId, e);
            throw new RuntimeException("获取用户最近操作记录失败: " + e.getMessage());
        }
    }

    /**
     * 统计用户操作次数
     */
    @Transactional(readOnly = true)
    public Integer countUserOperations(Long userId) {
        try {
            log.debug("统计用户操作次数，用户ID: {}", userId);

            Integer count = operationHistoryRepository.countOperationsByUserId(userId);
            return count != null ? count : 0;

        } catch (Exception e) {
            log.error("统计用户操作次数失败，用户ID: {}", userId, e);
            throw new RuntimeException("统计用户操作次数失败: " + e.getMessage());
        }
    }

    /**
     * 从HTTP请求中获取客户端IP地址
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 从HTTP请求中获取User-Agent
     */
    public String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return StringUtils.hasText(userAgent) ? userAgent : "Unknown";
    }

    // ========== 辅助方法 ==========

    /**
     * 转换为DTO
     */
    private UserOperationHistoryDTO convertToDTO(UserOperationHistory history) {
        UserOperationHistoryDTO dto = new UserOperationHistoryDTO();
        dto.setId(history.getId());
        dto.setUserId(history.getUserId());
        dto.setOperation(history.getOperation());
        dto.setOperator(history.getOperator());
        dto.setOperationTime(history.getOperationTime());
        dto.setDetails(history.getDetails());
        dto.setIpAddress(history.getIpAddress());
        dto.setUserAgent(history.getUserAgent());

        // 获取操作者用户名
        if (history.getOperator() != null && history.getOperator() > 0) {
            try {
                String operatorUsername = userRepository.selectById(history.getOperator()).getUsername();
                dto.setOperatorUsername(operatorUsername);
            } catch (Exception e) {
                log.warn("获取操作者用户名失败，操作者ID: {}", history.getOperator());
                dto.setOperatorUsername("未知用户");
            }
        } else {
            dto.setOperatorUsername("系统");
        }

        return dto;
    }
}