package com.hotel.controller.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.controller.BaseController;
import com.hotel.dto.UpdateRoleRequest;
import com.hotel.dto.UpdateStatusRequest;
import com.hotel.entity.User;
import com.hotel.enums.Permission;
import com.hotel.enums.Role;
import com.hotel.repository.UserRepository;
import com.hotel.util.PermissionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 管理员控制器
 * 处理用户管理相关的操作
 */
@RestController
@RequestMapping("/v1/admin")
@Validated
@Slf4j
@Tag(name = "管理员接口", description = "管理员专用接口，用于用户管理")
@SecurityRequirement(name = "bearerAuth")
public class AdminController extends BaseController {

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取用户列表", description = "管理员获取系统用户列表，支持分页和筛选")
    public ResponseEntity<ApiResponse<IPage<User>>> getUsers(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "角色筛选") @RequestParam(required = false) String role,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "用户名搜索") @RequestParam(required = false) String username) {

        try {
            log.info("管理员 {} 获取用户列表，页码: {}, 每页大小: {}, 角色: {}, 状态: {}, 用户名: {}",
                    PermissionUtil.getCurrentUserId(), page, size, role, status, username);

            // 构建查询条件
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();

            if (role != null && !role.trim().isEmpty()) {
                queryWrapper.eq("role", role);
            }

            if (status != null && !status.trim().isEmpty()) {
                queryWrapper.eq("status", status);
            }

            if (username != null && !username.trim().isEmpty()) {
                queryWrapper.like("username", username);
            }

            // 按创建时间倒序
            queryWrapper.orderByDesc("created_at");

            // 分页查询
            Page<User> pageParam = new Page<>(page, size);
            IPage<User> userPage = userRepository.selectPage(pageParam, queryWrapper);

            // 清除密码信息
            userPage.getRecords().forEach(user -> user.setPassword(null));

            log.info("获取用户列表成功，共 {} 条记录", userPage.getTotal());
            return ResponseEntity.ok(success(userPage, "获取用户列表成功"));

        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.ok(failed("获取用户列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取用户详情", description = "管理员获取指定用户的详细信息")
    public ResponseEntity<ApiResponse<User>> getUser(
            @Parameter(description = "用户ID") @PathVariable @Min(1) Long id) {

        try {
            log.info("管理员 {} 获取用户详情，用户ID: {}", PermissionUtil.getCurrentUserId(), id);

            User user = userRepository.selectById(id);
            if (user == null) {
                return ResponseEntity.ok(failed("用户不存在"));
            }

            // 清除密码信息
            user.setPassword(null);

            log.info("获取用户详情成功，用户ID: {}", id);
            return ResponseEntity.ok(success(user, "获取用户详情成功"));

        } catch (Exception e) {
            log.error("获取用户详情失败，用户ID: {}", id, e);
            return ResponseEntity.ok(failed("获取用户详情失败: " + e.getMessage()));
        }
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新用户角色", description = "管理员更新指定用户的角色")
    public ResponseEntity<ApiResponse<User>> updateUserRole(
            @Parameter(description = "用户ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateRoleRequest request) {

        try {
            log.info("管理员 {} 更新用户角色，用户ID: {}, 新角色: {}",
                    PermissionUtil.getCurrentUserId(), id, request.getRole());

            User user = userRepository.selectById(id);
            if (user == null) {
                return ResponseEntity.ok(failed("用户不存在"));
            }

            // 不能修改自己的角色
            Long currentUserId = PermissionUtil.getCurrentUserId();
            if (currentUserId != null && currentUserId.equals(id)) {
                return ResponseEntity.ok(failed("不能修改自己的角色"));
            }

            // 更新角色
            user.setRole(request.getRole().getCode());
            int result = userRepository.updateById(user);

            if (result > 0) {
                // 清除密码信息
                user.setPassword(null);
                log.info("更新用户角色成功，用户ID: {}, 新角色: {}", id, request.getRole());
                return ResponseEntity.ok(success(user, "更新用户角色成功"));
            } else {
                return ResponseEntity.ok(failed("更新用户角色失败"));
            }

        } catch (Exception e) {
            log.error("更新用户角色失败，用户ID: {}", id, e);
            return ResponseEntity.ok(failed("更新用户角色失败: " + e.getMessage()));
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新用户状态", description = "管理员启用或禁用用户账户")
    public ResponseEntity<ApiResponse<User>> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateStatusRequest request) {

        try {
            log.info("管理员 {} 更新用户状态，用户ID: {}, 新状态: {}",
                    PermissionUtil.getCurrentUserId(), id, request.getStatus());

            User user = userRepository.selectById(id);
            if (user == null) {
                return ResponseEntity.ok(failed("用户不存在"));
            }

            // 不能禁用自己的账户
            Long currentUserId = PermissionUtil.getCurrentUserId();
            if (currentUserId != null && currentUserId.equals(id) &&
                request.getStatus().getCode().equals("INACTIVE")) {
                return ResponseEntity.ok(failed("不能禁用自己的账户"));
            }

            // 更新状态
            user.setStatus(request.getStatus().getCode());
            int result = userRepository.updateById(user);

            if (result > 0) {
                // 清除密码信息
                user.setPassword(null);
                log.info("更新用户状态成功，用户ID: {}, 新状态: {}", id, request.getStatus());
                return ResponseEntity.ok(success(user, "更新用户状态成功"));
            } else {
                return ResponseEntity.ok(failed("更新用户状态失败"));
            }

        } catch (Exception e) {
            log.error("更新用户状态失败，用户ID: {}", id, e);
            return ResponseEntity.ok(failed("更新用户状态失败: " + e.getMessage()));
        }
    }

    /**
     * 批量更新用户状态
     */
    @PutMapping("/users/batch/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "批量更新用户状态", description = "管理员批量启用或禁用用户账户")
    public ResponseEntity<ApiResponse<Integer>> batchUpdateUserStatus(
            @Parameter(description = "用户ID列表") @RequestBody List<Long> userIds,
            @Parameter(description = "状态") @RequestParam String status) {

        try {
            log.info("管理员 {} 批量更新用户状态，用户数量: {}, 状态: {}",
                    PermissionUtil.getCurrentUserId(), userIds.size(), status);

            if (userIds == null || userIds.isEmpty()) {
                return ResponseEntity.ok(failed("用户ID列表不能为空"));
            }

            // 不能批量更新自己的状态
            Long currentUserId = PermissionUtil.getCurrentUserId();
            if (currentUserId != null && userIds.contains(currentUserId) &&
                "INACTIVE".equals(status)) {
                return ResponseEntity.ok(failed("不能禁用自己的账户"));
            }

            // 批量更新
            int updatedCount = userRepository.updateStatusBatch(userIds, status);

            log.info("批量更新用户状态成功，更新数量: {}", updatedCount);
            return ResponseEntity.ok(success(updatedCount, "批量更新用户状态成功"));

        } catch (Exception e) {
            log.error("批量更新用户状态失败", e);
            return ResponseEntity.ok(failed("批量更新用户状态失败: " + e.getMessage()));
        }
    }
}