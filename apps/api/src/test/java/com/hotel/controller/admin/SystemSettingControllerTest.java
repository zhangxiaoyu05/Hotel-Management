package com.hotel.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.settings.*;
import com.hotel.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(SystemSettingController.class)
@ActiveProfiles("test")
public class SystemSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemSettingService systemSettingService;

    @Autowired
    private ObjectMapper objectMapper;

    private BasicSettingsDTO basicSettingsDTO;
    private BusinessRulesDTO businessRulesDTO;
    private NotificationSettingsDTO notificationSettingsDTO;
    private SecuritySettingsDTO securitySettingsDTO;
    private BackupSettingsDTO backupSettingsDTO;

    @BeforeEach
    void setUp() {
        basicSettingsDTO = new BasicSettingsDTO();
        basicSettingsDTO.setSystemName("测试酒店管理系统");
        basicSettingsDTO.setContactPhone("13800138000");
        basicSettingsDTO.setContactEmail("test@example.com");

        businessRulesDTO = new BusinessRulesDTO();
        businessRulesDTO.setMinBookingDays(1);
        businessRulesDTO.setMaxBookingDays(30);
        businessRulesDTO.setCancelFeePercentage(BigDecimal.ZERO);

        notificationSettingsDTO = new NotificationSettingsDTO();
        notificationSettingsDTO.setEnableEmailNotifications(true);
        notificationSettingsDTO.setEnableSmsNotifications(false);

        securitySettingsDTO = new SecuritySettingsDTO();
        securitySettingsDTO.setPasswordMinLength(8);
        securitySettingsDTO.setMaxLoginAttempts(5);

        backupSettingsDTO = new BackupSettingsDTO();
        backupSettingsDTO.setEnableAutoBackup(true);
        backupSettingsDTO.setBackupIntervalDays(7);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetBasicSettings() throws Exception {
        when(systemSettingService.getBasicSettings()).thenReturn(basicSettingsDTO);

        mockMvc.perform(get("/v1/admin/settings/basic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.systemName").value("测试酒店管理系统"))
                .andExpect(jsonPath("$.data.contactPhone").value("13800138000"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testUpdateBasicSettings() throws Exception {
        UpdateBasicSettingsRequest request = new UpdateBasicSettingsRequest();
        request.setSystemName("更新后的系统名称");
        request.setContactPhone("13900139000");

        when(systemSettingService.updateBasicSettings(any(), any())).thenReturn(basicSettingsDTO);

        mockMvc.perform(put("/v1/admin/settings/basic")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetBusinessRules() throws Exception {
        when(systemSettingService.getBusinessRules()).thenReturn(businessRulesDTO);

        mockMvc.perform(get("/v1/admin/settings/business-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.minBookingDays").value(1))
                .andExpect(jsonPath("$.data.maxBookingDays").value(30));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testUpdateBusinessRules() throws Exception {
        UpdateBusinessRulesRequest request = new UpdateBusinessRulesRequest();
        request.setMinBookingDays(2);
        request.setMaxBookingDays(60);
        request.setCancelFeePercentage(new BigDecimal("10"));

        when(systemSettingService.updateBusinessRules(any(), any())).thenReturn(businessRulesDTO);

        mockMvc.perform(put("/v1/admin/settings/business-rules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetNotificationSettings() throws Exception {
        when(systemSettingService.getNotificationSettings()).thenReturn(notificationSettingsDTO);

        mockMvc.perform(get("/v1/admin/settings/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enableEmailNotifications").value(true))
                .andExpect(jsonPath("$.data.enableSmsNotifications").value(false));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetSecuritySettings() throws Exception {
        when(systemSettingService.getSecuritySettings()).thenReturn(securitySettingsDTO);

        mockMvc.perform(get("/v1/admin/settings/security"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.passwordMinLength").value(8))
                .andExpect(jsonPath("$.data.maxLoginAttempts").value(5));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetBackupSettings() throws Exception {
        when(systemSettingService.getBackupSettings()).thenReturn(backupSettingsDTO);

        mockMvc.perform(get("/v1/admin/settings/backup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enableAutoBackup").value(true))
                .andExpect(jsonPath("$.data.backupIntervalDays").value(7));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testExecuteBackup() throws Exception {
        when(systemSettingService.executeBackup()).thenReturn("备份执行成功");

        mockMvc.perform(post("/v1/admin/settings/backup/execute")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("备份执行成功"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/v1/admin/settings/basic"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/v1/admin/settings/basic"))
                .andExpect(status().isUnauthorized());
    }
}