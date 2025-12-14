package com.hotel.controller.admin;

import com.hotel.controller.BaseController;
import com.hotel.dto.ApiResponse;
import com.hotel.dto.admin.user.*;
import com.hotel.service.UserManagementService;
import com.hotel.service.UserOperationHistoryService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户管理控制器
 * 提供管理员用户管理接口
 */
@RestController
@RequestMapping("/v1/admin/users")
@Slf4j
@Tag(name = "用户管理接口", description = "管理员用户管理接口")
@SecurityRequirement(name = "bearerAuth")
public class UserManagementController extends BaseController {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private UserOperationHistoryService operationHistoryService;

    @Autowired
    @Qualifier("userBatchOperationRateLimiter")
    private RateLimiter batchOperationRateLimiter;

    @Autowired
    @Qualifier("userSingleOperationRateLimiter")
    private RateLimiter singleOperationRateLimiter;

    /**
     * 获取用户列表（分页、搜索、筛选）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取用户列表", description = "分页查询用户列表，支持筛选和排序")
    public ResponseEntity<ApiResponse<Page<UserListDTO>>> getUserList(
            @Parameter(description = "页码", example = "0") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "关键词搜索", example = "john") @RequestParam(required = false) String keyword,
            @Parameter(description = "角色筛选", example = "USER") @RequestParam(required = false) String role,
            @Parameter(description = "状态筛选", example = "ACTIVE") @RequestParam(required = false) String status,
            @Parameter(description = "排序字段", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向", example = "desc") @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            log.info("管理员请求获取用户列表，页码: {}, 每页数量: {}, 关键词: {}, 角色: {}, 状态: {}",
                    page, size, keyword, role, status);

            UserSearchDTO searchDTO = new UserSearchDTO();
            searchDTO.setPage(page);
            searchDTO.setSize(size);
            searchDTO.setKeyword(keyword);
            searchDTO.setRole(role);
            searchDTO.setStatus(status);
            searchDTO.setSortBy(sortBy);
            searchDTO.setSortDirection(sortDirection);

            Page<UserListDTO> userList = userManagementService.getUserList(searchDTO);

            log.info("用户列表获取成功，共 {} 条记录", userList.getTotalElements());
            return ResponseEntity.ok(success(userList, "获取用户列表成功"));

        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.ok(failed("获取用户列表失败: " + e.getMessage()));
        }
    }

    /**
     * 高级用户搜索
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "高级用户搜索", description = "根据多个条件搜索用户")
    public ResponseEntity<ApiResponse<Page<UserListDTO>>> searchUsers(
            @Parameter(description = "搜索条件") @Valid @RequestBody UserSearchDTO searchDTO) {
        try {
            log.info("管理员执行高级用户搜索，条件: {}", searchDTO);

            Page<UserListDTO> userList = userManagementService.getUserList(searchDTO);

            log.info("用户搜索完成，共 {} 条记录", userList.getTotalElements());
            return ResponseEntity.ok(success(userList, "用户搜索成功"));

        } catch (Exception e) {
            log.error("用户搜索失败", e);
            return ResponseEntity.ok(failed("用户搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取用户详情", description = "获取指定用户的详细信息")
    public ResponseEntity<ApiResponse<UserDetailDTO>> getUserDetail(
            @Parameter(description = "用户ID", example = "1") @PathVariable Long userId) {
        try {
            log.info("管理员请求获取用户详情，用户ID: {}", userId);

            UserDetailDTO userDetail = userManagementService.getUserDetail(userId);

            log.info("用户详情获取成功，用户: {}", userDetail.getUsername());
            return ResponseEntity.ok(success(userDetail, "获取用户详情成功"));

        } catch (Exception e) {
            log.error("获取用户详情失败，用户ID: {}", userId, e);
            return ResponseEntity.ok(failed("获取用户详情失败: " + e.getMessage()));
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新用户状态", description = "启用或禁用用户账户")
    public ResponseEntity<ApiResponse<UserListDTO>> updateUserStatus(
            @Parameter(description = "用户ID", example = "1") @PathVariable Long userId,
            @Parameter(description = "状态更新信息") @Valid @RequestBody UserManagementDTO managementDTO) {
        try {
            // 应用速率限制
            RateLimiter.waitForPermission(singleOperationRateLimiter);

            log.info("管理员请求更新用户状态，用户ID: {}, 新状态: {}", userId, managementDTO.getNewStatus());

            managementDTO.setUserId(userId);
            UserListDTO updatedUser = userManagementService.updateUserStatus(managementDTO);

            log.info("用户状态更新成功，用户: {}, 新状态: {}", updatedUser.getUsername(), updatedUser.getStatus());
            return ResponseEntity.ok(success(updatedUser, "用户状态更新成功"));

        } catch (RequestNotPermitted e) {
            log.warn("用户状态更新请求频率超限，用户ID: {}", userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(failed("操作频率过高，请稍后再试"));
        } catch (Exception e) {
            log.error("更新用户状态失败，用户ID: {}", userId, e);
            return ResponseEntity.ok(failed("更新用户状态失败: " + e.getMessage()));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除用户", description = "逻辑删除用户账户")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "用户ID", example = "1") @PathVariable Long userId,
            @Parameter(description = "删除原因") @RequestParam String reason) {
        try {
            log.info("管理员请求删除用户，用户ID: {}, 原因: {}", userId, reason);

            userManagementService.deleteUser(userId, reason);

            log.info("用户删除成功，用户ID: {}", userId);
            return ResponseEntity.ok(success(null, "用户删除成功"));

        } catch (Exception e) {
            log.error("删除用户失败，用户ID: {}", userId, e);
            return ResponseEntity.ok(failed("删除用户失败: " + e.getMessage()));
        }
    }

    /**
     * 批量更新用户状态
     */
    @PutMapping("/batch/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "批量更新用户状态", description = "批量启用或禁用用户账户")
    public ResponseEntity<ApiResponse<BatchOperationResultDTO>> batchUpdateUserStatus(
            @Parameter(description = "批量操作信息") @Valid @RequestBody UserBatchOperationDTO batchDTO) {
        try {
            // 应用速率限制
            RateLimiter.waitForPermission(batchOperationRateLimiter);

            log.info("管理员请求批量更新用户状态，用户数量: {}, 操作: {}",
                    batchDTO.getUserIds().size(), batchDTO.getOperation());

            BatchOperationResultDTO result = userManagementService.batchUpdateUserStatus(batchDTO);

            log.info("批量用户状态更新完成，成功: {}, 失败: {}",
                    result.getSuccessCount(), result.getFailureCount());
            return ResponseEntity.ok(success(result, "批量用户状态更新完成"));

        } catch (RequestNotPermitted e) {
            log.warn("批量用户状态更新请求频率超限，用户数量: {}", batchDTO.getUserIds().size());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(failed("批量操作频率过高，请稍后再试"));
        } catch (Exception e) {
            log.error("批量更新用户状态失败", e);
            return ResponseEntity.ok(failed("批量更新用户状态失败: " + e.getMessage()));
        }
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "批量删除用户", description = "批量逻辑删除用户账户")
    public ResponseEntity<ApiResponse<BatchOperationResultDTO>> batchDeleteUsers(
            @Parameter(description = "批量删除信息") @Valid @RequestBody UserBatchOperationDTO batchDTO) {
        try {
            // 应用速率限制
            RateLimiter.waitForPermission(batchOperationRateLimiter);

            log.info("管理员请求批量删除用户，用户数量: {}", batchDTO.getUserIds().size());

            BatchOperationResultDTO result = userManagementService.batchDeleteUsers(batchDTO);

            log.info("批量用户删除完成，成功: {}, 失败: {}",
                    result.getSuccessCount(), result.getFailureCount());
            return ResponseEntity.ok(success(result, "批量用户删除完成"));

        } catch (RequestNotPermitted e) {
            log.warn("批量用户删除请求频率超限，用户数量: {}", batchDTO.getUserIds().size());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(failed("批量操作频率过高，请稍后再试"));
        } catch (Exception e) {
            log.error("批量删除用户失败", e);
            return ResponseEntity.ok(failed("批量删除用户失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户操作历史
     */
    @GetMapping("/{userId}/operations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取用户操作历史", description = "分页获取指定用户的操作历史记录")
    public ResponseEntity<ApiResponse<Page<UserOperationHistoryDTO>>> getUserOperationHistory(
            @Parameter(description = "用户ID", example = "1") @PathVariable Long userId,
            @Parameter(description = "页码", example = "0") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        try {
            log.info("管理员请求获取用户操作历史，用户ID: {}, 页码: {}, 每页数量: {}", userId, page, size);

            Pageable pageable = PageRequest.of(page, size);
            Page<UserOperationHistoryDTO> history = operationHistoryService.getUserOperationHistory(userId, pageable);

            log.info("用户操作历史获取成功，共 {} 条记录", history.getTotalElements());
            return ResponseEntity.ok(success(history, "获取用户操作历史成功"));

        } catch (Exception e) {
            log.error("获取用户操作历史失败，用户ID: {}", userId, e);
            return ResponseEntity.ok(failed("获取用户操作历史失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取用户统计信息", description = "获取用户数量、状态分布等统计数据")
    public ResponseEntity<ApiResponse<UserStatisticsDTO>> getUserStatistics() {
        try {
            log.info("管理员请求获取用户统计信息");

            UserStatisticsDTO statistics = userManagementService.getUserStatistics();

            log.info("用户统计信息获取成功");
            return ResponseEntity.ok(success(statistics, "获取用户统计信息成功"));

        } catch (Exception e) {
            log.error("获取用户统计信息失败", e);
            return ResponseEntity.ok(failed("获取用户统计信息失败: " + e.getMessage()));
        }
    }
}