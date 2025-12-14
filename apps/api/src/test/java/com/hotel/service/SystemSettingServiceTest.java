package com.hotel.service;

import com.hotel.dto.settings.UpdateBasicSettingsRequest;
import com.hotel.dto.settings.BasicSettingsDTO;
import com.hotel.dto.settings.UpdateBusinessRulesRequest;
import com.hotel.dto.settings.BusinessRulesDTO;
import com.hotel.entity.SystemConfig;
import com.hotel.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SystemSettingServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @InjectMocks
    private SystemSettingService systemSettingService;

    private SystemConfig mockBasicConfig;
    private SystemConfig mockBusinessConfig;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        mockBasicConfig = new SystemConfig();
        mockBasicConfig.setId(1L);
        mockBasicConfig.setConfigKey("system.name");
        mockBasicConfig.setConfigValue("测试酒店管理系统");
        mockBasicConfig.setConfigType("BASIC");
        mockBasicConfig.setDescription("系统名称");
        mockBasicConfig.setIsEncrypted(false);
        mockBasicConfig.setCreatedAt(now);
        mockBasicConfig.setUpdatedAt(now);

        mockBusinessConfig = new SystemConfig();
        mockBusinessConfig.setId(2L);
        mockBusinessConfig.setConfigKey("booking.min_days");
        mockBusinessConfig.setConfigValue("1");
        mockBusinessConfig.setConfigType("BUSINESS");
        mockBusinessConfig.setDescription("最少预订天数");
        mockBusinessConfig.setIsEncrypted(false);
        mockBusinessConfig.setCreatedAt(now);
        mockBusinessConfig.setUpdatedAt(now);
    }

    @Test
    void testGetBasicSettings() {
        // Given
        when(systemConfigRepository.findByConfigType("BASIC"))
                .thenReturn(Arrays.asList(mockBasicConfig));

        // When
        BasicSettingsDTO result = systemSettingService.getBasicSettings();

        // Then
        assertNotNull(result);
        assertEquals("测试酒店管理系统", result.getSystemName());
        verify(systemConfigRepository, times(1)).findByConfigType("BASIC");
    }

    @Test
    void testGetBasicSettingsEmpty() {
        // Given
        when(systemConfigRepository.findByConfigType("BASIC"))
                .thenReturn(Collections.emptyList());

        // When
        BasicSettingsDTO result = systemSettingService.getBasicSettings();

        // Then
        assertNotNull(result);
        verify(systemConfigRepository, times(1)).findByConfigType("BASIC");
    }

    @Test
    void testUpdateBasicSettings() {
        // Given
        UpdateBasicSettingsRequest request = new UpdateBasicSettingsRequest();
        request.setSystemName("更新后的系统名称");
        request.setContactPhone("13900139000");

        when(systemConfigRepository.findByConfigKey(any())).thenReturn(Optional.empty());
        when(systemConfigRepository.insert(any())).thenReturn(1);

        // When
        BasicSettingsDTO result = systemSettingService.updateBasicSettings(request, "admin");

        // Then
        assertNotNull(result);
        verify(systemConfigRepository, times(2)).findByConfigKey(any());
        verify(systemConfigRepository, times(2)).insert(any());
    }

    @Test
    void testUpdateBasicSettingsExisting() {
        // Given
        UpdateBasicSettingsRequest request = new UpdateBasicSettingsRequest();
        request.setSystemName("更新后的系统名称");

        when(systemConfigRepository.findByConfigKey("system.name")).thenReturn(Optional.of(mockBasicConfig));
        when(systemConfigRepository.updateById(any())).thenReturn(1);

        // When
        BasicSettingsDTO result = systemSettingService.updateBasicSettings(request, "admin");

        // Then
        assertNotNull(result);
        verify(systemConfigRepository, times(2)).findByConfigKey(any());
        verify(systemConfigRepository, times(2)).updateById(any());
    }

    @Test
    void testGetBusinessRules() {
        // Given
        when(systemConfigRepository.findByConfigType("BUSINESS"))
                .thenReturn(Arrays.asList(mockBusinessConfig));

        // When
        BusinessRulesDTO result = systemSettingService.getBusinessRules();

        // Then
        assertNotNull(result);
        assertEquals(1, result.getMinBookingDays());
        verify(systemConfigRepository, times(1)).findByConfigType("BUSINESS");
    }

    @Test
    void testUpdateBusinessRules() {
        // Given
        UpdateBusinessRulesRequest request = new UpdateBusinessRulesRequest();
        request.setMinBookingDays(2);
        request.setMaxBookingDays(60);
        request.setCancelFeePercentage(new BigDecimal("10"));

        when(systemConfigRepository.findByConfigKey(any())).thenReturn(Optional.empty());
        when(systemConfigRepository.insert(any())).thenReturn(1);

        // When
        BusinessRulesDTO result = systemSettingService.updateBusinessRules(request, "admin");

        // Then
        assertNotNull(result);
        verify(systemConfigRepository, atLeast(10)).findByConfigKey(any());
        verify(systemConfigRepository, atLeast(10)).insert(any());
    }

    @Test
    void testGetNotificationSettings() {
        // Given
        when(systemConfigRepository.findByConfigType("NOTIFICATION"))
                .thenReturn(Collections.emptyList());

        // When
        var result = systemSettingService.getNotificationSettings();

        // Then
        assertNotNull(result);
        verify(systemConfigRepository, times(1)).findByConfigType("NOTIFICATION");
    }

    @Test
    void testGetSecuritySettings() {
        // Given
        when(systemConfigRepository.findByConfigType("SECURITY"))
                .thenReturn(Collections.emptyList());

        // When
        var result = systemSettingService.getSecuritySettings();

        // Then
        assertNotNull(result);
        verify(systemConfigRepository, times(1)).findByConfigType("SECURITY");
    }

    @Test
    void testGetBackupSettings() {
        // Given
        when(systemConfigRepository.findByConfigType("BACKUP"))
                .thenReturn(Collections.emptyList());

        // When
        var result = systemSettingService.getBackupSettings();

        // Then
        assertNotNull(result);
        verify(systemConfigRepository, times(1)).findByConfigType("BACKUP");
    }

    @Test
    void testExecuteBackup() {
        // Given
        when(systemConfigRepository.findByConfigKey("backup.last_time"))
                .thenReturn(Optional.empty());
        when(systemConfigRepository.insert(any())).thenReturn(1);

        // When
        String result = systemSettingService.executeBackup();

        // Then
        assertEquals("备份执行成功", result);
        verify(systemConfigRepository, times(1)).findByConfigKey("backup.last_time");
        verify(systemConfigRepository, times(1)).insert(any());
    }

    @Test
    void testExecuteBackupException() {
        // Given
        when(systemConfigRepository.findByConfigKey("backup.last_time"))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            systemSettingService.executeBackup();
        });
    }
}