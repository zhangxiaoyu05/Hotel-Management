package com.hotel.controller.auth;

import com.hotel.dto.ApiResponse;
import com.hotel.dto.ChangePasswordRequest;
import com.hotel.dto.UpdateProfileRequest;
import com.hotel.entity.User;
import com.hotel.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "用户管理", description = "用户个人信息管理相关接口")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User user = userService.getUserByUsername(username);

            return ResponseEntity.ok(ApiResponse.success(user, "获取用户信息成功"));
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取用户信息失败: " + e.getMessage()));
        }
    }

    @PutMapping("/me")
    @Operation(summary = "更新当前用户信息", description = "更新当前登录用户的基本信息")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest updateRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User updatedUser = userService.updateUserProfile(username, updateRequest);

            return ResponseEntity.ok(ApiResponse.success(updatedUser, "更新用户信息成功"));
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("更新用户信息失败: " + e.getMessage()));
        }
    }

    @PutMapping("/me/password")
    @Operation(summary = "修改密码", description = "修改当前用户的登录密码")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // 验证新密码和确认密码是否一致
            if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("新密码和确认密码不一致"));
            }

            userService.changePassword(username, changePasswordRequest);

            return ResponseEntity.ok(ApiResponse.success(null, "密码修改成功"));
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("修改密码失败: " + e.getMessage()));
        }
    }
}