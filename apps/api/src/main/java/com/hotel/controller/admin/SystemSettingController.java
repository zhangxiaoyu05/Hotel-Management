package com.hotel.controller.admin;

import com.hotel.dto.ApiResponse;
import com.hotel.dto.settings.*;
import com.hotel.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@Slf4j
@Tag(name = "系统设置管理", description = "系统设置相关API")
public class SystemSettingController {

    @Autowired
    private SystemSettingService systemSettingService;

    @GetMapping("/basic")
    @Operation(summary = "获取基础设置", description = "获取系统基础配置信息")
    public ResponseEntity<ApiResponse<BasicSettingsDTO>> getBasicSettings() {
        try {
            log.info("获取基础设置");
            BasicSettingsDTO settings = systemSettingService.getBasicSettings();
            return ResponseEntity.ok(ApiResponse.success("获取基础设置成功", settings));
        } catch (Exception e) {
            log.error("获取基础设置失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取基础设置失败: " + e.getMessage()));
        }
    }

    @PutMapping("/basic")
    @Operation(summary = "更新基础设置", description = "更新系统基础配置")
    public ResponseEntity<ApiResponse<BasicSettingsDTO>> updateBasicSettings(
            @Valid @RequestBody UpdateBasicSettingsRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("更新基础设置");
            String updatedBy = getCurrentUsername();
            BasicSettingsDTO updatedSettings = systemSettingService.updateBasicSettings(request, updatedBy, httpRequest);
            return ResponseEntity.ok(ApiResponse.success("更新基础设置成功", updatedSettings));
        } catch (Exception e) {
            log.error("更新基础设置失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("更新基础设置失败: " + e.getMessage()));
        }
    }

    @GetMapping("/business-rules")
    @Operation(summary = "获取业务规则", description = "获取系统业务规则配置")
    public ResponseEntity<ApiResponse<BusinessRulesDTO>> getBusinessRules() {
        try {
            log.info("获取业务规则");
            BusinessRulesDTO rules = systemSettingService.getBusinessRules();
            return ResponseEntity.ok(ApiResponse.success("获取业务规则成功", rules));
        } catch (Exception e) {
            log.error("获取业务规则失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取业务规则失败: " + e.getMessage()));
        }
    }

    @PutMapping("/business-rules")
    @Operation(summary = "更新业务规则", description = "更新系统业务规则配置")
    public ResponseEntity<ApiResponse<BusinessRulesDTO>> updateBusinessRules(
            @Valid @RequestBody UpdateBusinessRulesRequest request) {
        try {
            log.info("更新业务规则");
            String updatedBy = getCurrentUsername();
            BusinessRulesDTO updatedRules = systemSettingService.updateBusinessRules(request, updatedBy);
            return ResponseEntity.ok(ApiResponse.success("更新业务规则成功", updatedRules));
        } catch (Exception e) {
            log.error("更新业务规则失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("更新业务规则失败: " + e.getMessage()));
        }
    }

    @GetMapping("/notifications")
    @Operation(summary = "获取通知设置", description = "获取系统通知配置")
    public ResponseEntity<ApiResponse<NotificationSettingsDTO>> getNotificationSettings() {
        try {
            log.info("获取通知设置");
            NotificationSettingsDTO settings = systemSettingService.getNotificationSettings();
            return ResponseEntity.ok(ApiResponse.success("获取通知设置成功", settings));
        } catch (Exception e) {
            log.error("获取通知设置失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取通知设置失败: " + e.getMessage()));
        }
    }

    @GetMapping("/security")
    @Operation(summary = "获取安全设置", description = "获取系统安全配置")
    public ResponseEntity<ApiResponse<SecuritySettingsDTO>> getSecuritySettings() {
        try {
            log.info("获取安全设置");
            SecuritySettingsDTO settings = systemSettingService.getSecuritySettings();
            return ResponseEntity.ok(ApiResponse.success("获取安全设置成功", settings));
        } catch (Exception e) {
            log.error("获取安全设置失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取安全设置失败: " + e.getMessage()));
        }
    }

    @GetMapping("/backup")
    @Operation(summary = "获取备份设置", description = "获取系统备份配置")
    public ResponseEntity<ApiResponse<BackupSettingsDTO>> getBackupSettings() {
        try {
            log.info("获取备份设置");
            BackupSettingsDTO settings = systemSettingService.getBackupSettings();
            return ResponseEntity.ok(ApiResponse.success("获取备份设置成功", settings));
        } catch (Exception e) {
            log.error("获取备份设置失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取备份设置失败: " + e.getMessage()));
        }
    }

    @PostMapping("/backup/execute")
    @Operation(summary = "执行备份", description = "手动执行系统备份")
    public ResponseEntity<ApiResponse<String>> executeBackup() {
        try {
            log.info("手动执行系统备份");
            String result = systemSettingService.executeBackup();
            return ResponseEntity.ok(ApiResponse.success("备份执行成功", result));
        } catch (Exception e) {
            log.error("备份执行失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("备份执行失败: " + e.getMessage()));
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}