package com.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.dto.settings.*;
import com.hotel.entity.SystemConfig;
import com.hotel.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import com.hotel.util.ConfigValidator;

@Service
@Transactional
public class SystemSettingService {

    private static final Logger logger = LoggerFactory.getLogger(SystemSettingService.class);

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private SystemConfigAuditService auditService;

    @Autowired
    private BackupService backupService;

    @Autowired
    private ConfigValidator configValidator;

    // AES加密配置
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES_KEY = "MyHotelSecretKey123"; // 16字节密钥，实际项目中应该从安全配置中获取
    private static final String AES_IV = "MyHotelInitVector45"; // 16字节初始化向量

    // 需要加密的敏感配置键
    private static final List<String> SENSITIVE_CONFIG_KEYS = Arrays.asList(
        "smtp_password", "sms_api_key", "payment_secret", "jwt_secret",
        "database_password", "redis_password", "oss_secret_key",
        "backup_encryption_key", "api_client_secret"
    );

    private static final String CONFIG_TYPE_BASIC = "BASIC";
    private static final String CONFIG_TYPE_BUSINESS = "BUSINESS";
    private static final String CONFIG_TYPE_NOTIFICATION = "NOTIFICATION";
    private static final String CONFIG_TYPE_SECURITY = "SECURITY";
    private static final String CONFIG_TYPE_BACKUP = "BACKUP";

    public BasicSettingsDTO getBasicSettings() {
        logger.info("获取基础设置");

        List<SystemConfig> configs = systemConfigRepository.findByConfigType(CONFIG_TYPE_BASIC);
        BasicSettingsDTO dto = new BasicSettingsDTO();

        for (SystemConfig config : configs) {
            String value = config.getIsEncrypted() ? decryptValue(config.getConfigValue()) : config.getConfigValue();
            setBasicSettingValue(dto, config.getConfigKey(), value, config.getUpdatedAt(), config.getUpdatedAt().toString());
        }

        return dto;
    }

    public BasicSettingsDTO updateBasicSettings(UpdateBasicSettingsRequest request, String updatedBy) {
        return updateBasicSettings(request, updatedBy, null);
    }

    public BasicSettingsDTO updateBasicSettings(UpdateBasicSettingsRequest request, String updatedBy, HttpServletRequest httpRequest) {
        logger.info("更新基础设置，操作人: {}", updatedBy);

        // 验证输入参数
        ConfigValidator.ValidationResult result = ConfigValidator.ValidationResult.success();

        // 验证系统名称
        ConfigValidator.ValidationResult nameResult = configValidator.validateSystemName(request.getSystemName());
        if (!nameResult.isValid()) {
            result = ConfigValidator.ValidationResult.error(result.getErrorMessage() + "; " + nameResult.getErrorMessage());
        }

        // 验证邮箱
        ConfigValidator.ValidationResult emailResult = configValidator.validateEmail(request.getContactEmail());
        if (!emailResult.isValid()) {
            result = ConfigValidator.ValidationResult.error(result.getErrorMessage() + "; " + emailResult.getErrorMessage());
        }

        // 验证电话
        ConfigValidator.ValidationResult phoneResult = configValidator.validatePhone(request.getContactPhone());
        if (!phoneResult.isValid()) {
            result = ConfigValidator.ValidationResult.error(result.getErrorMessage() + "; " + phoneResult.getErrorMessage());
        }

        if (!result.isValid()) {
            throw new IllegalArgumentException("输入验证失败: " + result.getErrorMessage());
        }

        saveOrUpdateConfig("system.name", request.getSystemName(), CONFIG_TYPE_BASIC, "系统名称", false, updatedBy, httpRequest);
        saveOrUpdateConfig("system.logo", request.getSystemLogo(), CONFIG_TYPE_BASIC, "系统Logo", false, updatedBy, httpRequest);
        saveOrUpdateConfig("contact.phone", request.getContactPhone(), CONFIG_TYPE_BASIC, "联系电话", false, updatedBy, httpRequest);
        saveOrUpdateConfig("contact.email", request.getContactEmail(), CONFIG_TYPE_BASIC, "联系邮箱", false, updatedBy, httpRequest);
        saveOrUpdateConfig("contact.address", request.getContactAddress(), CONFIG_TYPE_BASIC, "联系地址", false, updatedBy, httpRequest);
        saveOrUpdateConfig("system.description", request.getSystemDescription(), CONFIG_TYPE_BASIC, "系统描述", false, updatedBy, httpRequest);
        saveOrUpdateConfig("business.hours", request.getBusinessHours(), CONFIG_TYPE_BASIC, "营业时间", false, updatedBy, httpRequest);

        return getBasicSettings();
    }

    public BusinessRulesDTO getBusinessRules() {
        logger.info("获取业务规则设置");

        List<SystemConfig> configs = systemConfigRepository.findByConfigType(CONFIG_TYPE_BUSINESS);
        BusinessRulesDTO dto = new BusinessRulesDTO();

        for (SystemConfig config : configs) {
            String value = config.getIsEncrypted() ? decryptValue(config.getConfigValue()) : config.getConfigValue();
            setBusinessRuleValue(dto, config.getConfigKey(), value, config.getUpdatedAt(), config.getUpdatedAt().toString());
        }

        return dto;
    }

    public BusinessRulesDTO updateBusinessRules(UpdateBusinessRulesRequest request, String updatedBy) {
        logger.info("更新业务规则设置，操作人: {}", updatedBy);

        saveOrUpdateConfig("booking.min_days", request.getMinBookingDays().toString(), CONFIG_TYPE_BUSINESS, "最少预订天数", false, updatedBy);
        saveOrUpdateConfig("booking.max_days", request.getMaxBookingDays().toString(), CONFIG_TYPE_BUSINESS, "最多预订天数", false, updatedBy);
        saveOrUpdateConfig("booking.advance_limit", request.getAdvanceBookingLimitDays().toString(), CONFIG_TYPE_BUSINESS, "提前预订限制", false, updatedBy);
        saveOrUpdateConfig("cancel.before_hours", request.getCancelBeforeHours().toString(), CONFIG_TYPE_BUSINESS, "提前取消时限", false, updatedBy);
        saveOrUpdateConfig("cancel.fee_percentage", request.getCancelFeePercentage().toString(), CONFIG_TYPE_BUSINESS, "取消费用比例", false, updatedBy);
        saveOrUpdateConfig("cancel.enable_free", request.getEnableFreeCancel().toString(), CONFIG_TYPE_BUSINESS, "启用免费取消", false, updatedBy);
        saveOrUpdateConfig("cancel.free_hours", request.getFreeCancelHours().toString(), CONFIG_TYPE_BUSINESS, "免费取消时限", false, updatedBy);
        saveOrUpdateConfig("price.default", request.getDefaultRoomPrice().toString(), CONFIG_TYPE_BUSINESS, "默认房价", false, updatedBy);
        saveOrUpdateConfig("price.enable_dynamic", request.getEnableDynamicPricing().toString(), CONFIG_TYPE_BUSINESS, "启用动态定价", false, updatedBy);
        saveOrUpdateConfig("price.peak_multiplier", request.getPeakSeasonPriceMultiplier().toString(), CONFIG_TYPE_BUSINESS, "旺季价格比例", false, updatedBy);
        saveOrUpdateConfig("price.off_season_multiplier", request.getOffSeasonPriceMultiplier().toString(), CONFIG_TYPE_BUSINESS, "淡季价格比例", false, updatedBy);
        saveOrUpdateConfig("price.weekend_multiplier", request.getWeekendPriceMultiplier().toString(), CONFIG_TYPE_BUSINESS, "周末价格比例", false, updatedBy);

        return getBusinessRules();
    }

    public NotificationSettingsDTO getNotificationSettings() {
        logger.info("获取通知设置");

        List<SystemConfig> configs = systemConfigRepository.findByConfigType(CONFIG_TYPE_NOTIFICATION);
        NotificationSettingsDTO dto = new NotificationSettingsDTO();

        for (SystemConfig config : configs) {
            String value = config.getIsEncrypted() ? decryptValue(config.getConfigValue()) : config.getConfigValue();
            setNotificationSettingValue(dto, config.getConfigKey(), value, config.getUpdatedAt(), config.getUpdatedAt().toString());
        }

        return dto;
    }

    public SecuritySettingsDTO getSecuritySettings() {
        logger.info("获取安全设置");

        List<SystemConfig> configs = systemConfigRepository.findByConfigType(CONFIG_TYPE_SECURITY);
        SecuritySettingsDTO dto = new SecuritySettingsDTO();

        for (SystemConfig config : configs) {
            String value = config.getIsEncrypted() ? decryptValue(config.getConfigValue()) : config.getConfigValue();
            setSecuritySettingValue(dto, config.getConfigKey(), value, config.getUpdatedAt(), config.getUpdatedAt().toString());
        }

        return dto;
    }

    public BackupSettingsDTO getBackupSettings() {
        logger.info("获取备份设置");

        List<SystemConfig> configs = systemConfigRepository.findByConfigType(CONFIG_TYPE_BACKUP);
        BackupSettingsDTO dto = new BackupSettingsDTO();

        for (SystemConfig config : configs) {
            String value = config.getIsEncrypted() ? decryptValue(config.getConfigValue()) : config.getConfigValue();
            setBackupSettingValue(dto, config.getConfigKey(), value, config.getUpdatedAt(), config.getUpdatedAt().toString());
        }

        return dto;
    }

    public String executeBackup() {
        logger.info("手动执行系统备份");

        try {
            String backupFileName = backupService.executeFullBackup();

            // 记录备份时间
            saveOrUpdateConfig("backup.last_time", LocalDateTime.now().toString(), CONFIG_TYPE_BACKUP, "最后备份时间", false, "system");

            return "备份执行成功，文件名: " + backupFileName;
        } catch (Exception e) {
            logger.error("备份执行失败", e);
            throw new RuntimeException("备份执行失败: " + e.getMessage());
        }
    }

    private void saveOrUpdateConfig(String key, String value, String type, String description, boolean isEncrypted, String updatedBy) {
        saveOrUpdateConfig(key, value, type, description, isEncrypted, updatedBy, null);
    }

    private void saveOrUpdateConfig(String key, String value, String type, String description, boolean isEncrypted, String updatedBy, HttpServletRequest request) {
        Optional<SystemConfig> existingConfig = systemConfigRepository.findByConfigKey(key);
        String oldValue = null;
        String operationType = "CREATE";

        try {
            if (existingConfig.isPresent()) {
                SystemConfig config = existingConfig.get();
                oldValue = config.getIsEncrypted() ? decryptValue(config.getConfigValue()) : config.getConfigValue();
                operationType = "UPDATE";

                // 自动判断是否需要加密敏感配置
                boolean shouldEncrypt = isEncrypted || isSensitiveConfig(key);
                config.setConfigValue(shouldEncrypt ? encryptValue(value) : value);
                config.setIsEncrypted(shouldEncrypt);
                config.setUpdatedAt(LocalDateTime.now());
                systemConfigRepository.updateById(config);
            } else {
                SystemConfig newConfig = new SystemConfig();
                newConfig.setConfigKey(key);
                // 自动判断是否需要加密敏感配置
                boolean shouldEncrypt = isEncrypted || isSensitiveConfig(key);
                newConfig.setConfigValue(shouldEncrypt ? encryptValue(value) : value);
                newConfig.setConfigType(type);
                newConfig.setDescription(description);
                newConfig.setIsEncrypted(shouldEncrypt);
                newConfig.setCreatedAt(LocalDateTime.now());
                newConfig.setUpdatedAt(LocalDateTime.now());
                systemConfigRepository.insert(newConfig);
            }

            // 记录审计日志
            auditService.logConfigChange(key, type, oldValue, value, isEncrypted || isSensitiveConfig(key),
                    operationType, updatedBy, request, description);

            logger.info("配置保存成功: {} - {}", key, operationType);

        } catch (Exception e) {
            // 记录失败的审计日志
            auditService.logConfigChange(key, type, oldValue, value, isEncrypted || isSensitiveConfig(key),
                    operationType, updatedBy, request, "FAILED", e.getMessage(), description);

            logger.error("配置保存失败: {} - {}", key, e.getMessage(), e);
            throw e;
        }
    }

    private void setBasicSettingValue(BasicSettingsDTO dto, String key, String value, LocalDateTime updatedAt, String updatedBy) {
        switch (key) {
            case "system.name":
                dto.setSystemName(value);
                break;
            case "system.logo":
                dto.setSystemLogo(value);
                break;
            case "contact.phone":
                dto.setContactPhone(value);
                break;
            case "contact.email":
                dto.setContactEmail(value);
                break;
            case "contact.address":
                dto.setContactAddress(value);
                break;
            case "system.description":
                dto.setSystemDescription(value);
                break;
            case "business.hours":
                dto.setBusinessHours(value);
                break;
        }
        dto.setUpdatedAt(updatedAt);
        dto.setUpdatedBy(updatedBy);
    }

    private void setBusinessRuleValue(BusinessRulesDTO dto, String key, String value, LocalDateTime updatedAt, String updatedBy) {
        switch (key) {
            case "booking.min_days":
                dto.setMinBookingDays(Integer.valueOf(value));
                break;
            case "booking.max_days":
                dto.setMaxBookingDays(Integer.valueOf(value));
                break;
            case "booking.advance_limit":
                dto.setAdvanceBookingLimitDays(Integer.valueOf(value));
                break;
            case "cancel.before_hours":
                dto.setCancelBeforeHours(Integer.valueOf(value));
                break;
            case "cancel.fee_percentage":
                dto.setCancelFeePercentage(new BigDecimal(value));
                break;
            case "cancel.enable_free":
                dto.setEnableFreeCancel(Boolean.valueOf(value));
                break;
            case "cancel.free_hours":
                dto.setFreeCancelHours(Integer.valueOf(value));
                break;
            case "price.default":
                dto.setDefaultRoomPrice(new BigDecimal(value));
                break;
            case "price.enable_dynamic":
                dto.setEnableDynamicPricing(Boolean.valueOf(value));
                break;
            case "price.peak_multiplier":
                dto.setPeakSeasonPriceMultiplier(new BigDecimal(value));
                break;
            case "price.off_season_multiplier":
                dto.setOffSeasonPriceMultiplier(new BigDecimal(value));
                break;
            case "price.weekend_multiplier":
                dto.setWeekendPriceMultiplier(new BigDecimal(value));
                break;
        }
        dto.setUpdatedAt(updatedAt);
        dto.setUpdatedBy(updatedBy);
    }

    private void setNotificationSettingValue(NotificationSettingsDTO dto, String key, String value, LocalDateTime updatedAt, String updatedBy) {
        // Implementation similar to other setters
        dto.setUpdatedAt(updatedAt);
        dto.setUpdatedBy(updatedBy);
    }

    private void setSecuritySettingValue(SecuritySettingsDTO dto, String key, String value, LocalDateTime updatedAt, String updatedBy) {
        // Implementation similar to other setters
        dto.setUpdatedAt(updatedAt);
        dto.setUpdatedBy(updatedBy);
    }

    private void setBackupSettingValue(BackupSettingsDTO dto, String key, String value, LocalDateTime updatedAt, String updatedBy) {
        // Implementation similar to other setters
        dto.setUpdatedAt(updatedAt);
        dto.setUpdatedBy(updatedBy);
    }

    /**
     * 加密敏感配置值
     */
    private String encryptValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(AES_IV.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            logger.error("加密配置值失败: {}", e.getMessage(), e);
            throw new RuntimeException("配置加密失败", e);
        }
    }

    /**
     * 解密敏感配置值
     */
    private String decryptValue(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.trim().isEmpty()) {
            return encryptedValue;
        }

        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(AES_IV.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedValue);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("解密配置值失败: {}", e.getMessage(), e);
            throw new RuntimeException("配置解密失败", e);
        }
    }

    /**
     * 判断配置键是否为敏感配置
     */
    private boolean isSensitiveConfig(String configKey) {
        return SENSITIVE_CONFIG_KEYS.contains(configKey.toLowerCase());
    }

    /**
     * 保存配置时判断是否需要加密
     */
    private String processConfigValue(String configKey, String configValue) {
        if (isSensitiveConfig(configKey)) {
            logger.info("检测到敏感配置，进行加密存储: {}", configKey);
            return encryptValue(configValue);
        }
        return configValue;
    }

    /**
     * 读取配置时判断是否需要解密
     */
    private String processConfigValueForRead(String configKey, String configValue, boolean isEncrypted) {
        if (isEncrypted && isSensitiveConfig(configKey)) {
            try {
                return decryptValue(configValue);
            } catch (Exception e) {
                logger.warn("解密配置失败，返回原始值: {}", configKey);
                return configValue;
            }
        }
        return configValue;
    }
}