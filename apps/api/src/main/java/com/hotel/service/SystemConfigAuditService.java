package com.hotel.service;

import com.hotel.entity.SystemConfigAudit;
import com.hotel.repository.SystemConfigAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统配置审计日志服务
 *
 * @author System
 * @since 2025-12-14
 */
@Service
@Transactional
public class SystemConfigAuditService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigAuditService.class);

    @Autowired
    private SystemConfigAuditRepository systemConfigAuditRepository;

    /**
     * 记录配置变更审计日志
     *
     * @param configKey 配置键
     * @param configType 配置类型
     * @param oldValue 变更前的值
     * @param newValue 变更后的值
     * @param isEncrypted 是否加密
     * @param operationType 操作类型
     * @param operator 操作人
     * @param request HTTP请求对象
     * @param description 描述
     */
    public void logConfigChange(String configKey, String configType, String oldValue, String newValue,
                               Boolean isEncrypted, String operationType, String operator,
                               HttpServletRequest request, String description) {
        logConfigChange(configKey, configType, oldValue, newValue, isEncrypted, operationType, operator,
                       request, "SUCCESS", null, description);
    }

    /**
     * 记录配置变更审计日志（包含操作结果）
     *
     * @param configKey 配置键
     * @param configType 配置类型
     * @param oldValue 变更前的值
     * @param newValue 变更后的值
     * @param isEncrypted 是否加密
     * @param operationType 操作类型
     * @param operator 操作人
     * @param request HTTP请求对象
     * @param operationResult 操作结果
     * @param failureReason 失败原因
     * @param description 描述
     */
    public void logConfigChange(String configKey, String configType, String oldValue, String newValue,
                               Boolean isEncrypted, String operationType, String operator,
                               HttpServletRequest request, String operationResult, String failureReason,
                               String description) {
        try {
            SystemConfigAudit audit = new SystemConfigAudit();
            audit.setConfigKey(configKey);
            audit.setConfigType(configType);

            // 对于敏感数据，隐藏实际值
            audit.setOldValue(maskSensitiveValue(configKey, oldValue, isEncrypted));
            audit.setNewValue(maskSensitiveValue(configKey, newValue, isEncrypted));

            audit.setIsEncrypted(isEncrypted);
            audit.setOperationType(operationType);
            audit.setOperator(operator);
            audit.setOperationTime(LocalDateTime.now());

            // 获取客户端信息
            if (request != null) {
                audit.setIpAddress(getClientIpAddress(request));
                audit.setUserAgent(request.getHeader("User-Agent"));
            }

            audit.setOperationResult(operationResult);
            audit.setFailureReason(failureReason);
            audit.setDescription(description);
            audit.setCreatedAt(LocalDateTime.now());

            systemConfigAuditRepository.insert(audit);

            logger.info("配置变更审计日志记录成功: {} - {} - {}", configKey, operationType, operator);

        } catch (Exception e) {
            logger.error("记录配置变更审计日志失败: {}", e.getMessage(), e);
            // 审计日志记录失败不应该影响主要业务流程
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 隐藏敏感数据值
     */
    private String maskSensitiveValue(String configKey, String value, Boolean isEncrypted) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        // 对于加密数据或者敏感配置键，隐藏实际值
        if (isEncrypted != null && isEncrypted) {
            return "***ENCRYPTED***";
        }

        String lowerKey = configKey.toLowerCase();
        if (lowerKey.contains("password") || lowerKey.contains("secret") ||
            lowerKey.contains("key") || lowerKey.contains("token")) {
            if (value.length() <= 4) {
                return "***";
            } else {
                return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
            }
        }

        return value;
    }

    /**
     * 根据配置键查询审计日志
     */
    @Transactional(readOnly = true)
    public List<SystemConfigAudit> getAuditLogsByConfigKey(String configKey, int limit) {
        return systemConfigAuditRepository.findByConfigKeyOrderByOperationTimeDesc(configKey, limit);
    }

    /**
     * 根据配置类型查询审计日志
     */
    @Transactional(readOnly = true)
    public List<SystemConfigAudit> getAuditLogsByConfigType(String configType, int limit) {
        return systemConfigAuditRepository.findByConfigTypeOrderByOperationTimeDesc(configType, limit);
    }

    /**
     * 根据操作人查询审计日志
     */
    @Transactional(readOnly = true)
    public List<SystemConfigAudit> getAuditLogsByOperator(String operator, int limit) {
        return systemConfigAuditRepository.findByOperatorOrderByOperationTimeDesc(operator, limit);
    }

    /**
     * 根据时间范围查询审计日志
     */
    @Transactional(readOnly = true)
    public List<SystemConfigAudit> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        return systemConfigAuditRepository.findByOperationTimeBetweenOrderByOperationTimeDesc(startTime, endTime, limit);
    }

    /**
     * 根据操作类型查询审计日志
     */
    @Transactional(readOnly = true)
    public List<SystemConfigAudit> getAuditLogsByOperationType(String operationType, int limit) {
        return systemConfigAuditRepository.findByOperationTypeOrderByOperationTimeDesc(operationType, limit);
    }
}