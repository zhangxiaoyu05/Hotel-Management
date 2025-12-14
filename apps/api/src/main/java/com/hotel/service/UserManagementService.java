package com.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.dto.admin.user.*;
import com.hotel.entity.User;
import com.hotel.entity.UserOperationHistory;
import com.hotel.repository.UserRepository;
import com.hotel.repository.UserOperationHistoryRepository;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理服务
 */
@Service
@Slf4j
@Transactional
public class UserManagementService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOperationHistoryRepository operationHistoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserOperationHistoryService operationHistoryService;

    /**
     * 获取用户列表（分页、筛选、排序）
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "user-search", key = "#searchDTO.hashCode()", condition = "#searchDTO.page < 5")
    public org.springframework.data.domain.Page<UserListDTO> getUserList(UserSearchDTO searchDTO) {
        try {
            log.info("查询用户列表，条件: {}", searchDTO);

            // 构建查询条件
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

            // 关键词搜索（用户名、邮箱、手机号）
            if (StringUtils.hasText(searchDTO.getKeyword())) {
                queryWrapper.and(wrapper ->
                    wrapper.like(User::getUsername, searchDTO.getKeyword())
                           .or().like(User::getEmail, searchDTO.getKeyword())
                           .or().like(User::getPhone, searchDTO.getKeyword())
                );
            }

            // 精确搜索条件
            if (StringUtils.hasText(searchDTO.getUsername())) {
                queryWrapper.like(User::getUsername, searchDTO.getUsername());
            }
            if (StringUtils.hasText(searchDTO.getEmail())) {
                queryWrapper.like(User::getEmail, searchDTO.getEmail());
            }
            if (StringUtils.hasText(searchDTO.getPhone())) {
                queryWrapper.like(User::getPhone, searchDTO.getPhone());
            }
            if (StringUtils.hasText(searchDTO.getRole())) {
                queryWrapper.eq(User::getRole, searchDTO.getRole());
            }
            if (StringUtils.hasText(searchDTO.getStatus())) {
                queryWrapper.eq(User::getStatus, searchDTO.getStatus());
            }

            // 注册时间范围筛选
            if (searchDTO.getRegistrationDateStart() != null) {
                queryWrapper.ge(User::getCreatedAt, searchDTO.getRegistrationDateStart());
            }
            if (searchDTO.getRegistrationDateEnd() != null) {
                queryWrapper.le(User::getCreatedAt, searchDTO.getRegistrationDateEnd());
            }

            // 排序
            if (StringUtils.hasText(searchDTO.getSortBy())) {
                String sortField = getSortField(searchDTO.getSortBy());
                if ("asc".equalsIgnoreCase(searchDTO.getSortDirection())) {
                    queryWrapper.orderByAsc(true, getSortColumn(sortField));
                } else {
                    queryWrapper.orderByDesc(true, getSortColumn(sortField));
                }
            } else {
                queryWrapper.orderByDesc(User::getCreatedAt);
            }

            // 分页查询
            Page<User> page = new Page<>(searchDTO.getPage() + 1, searchDTO.getSize());
            IPage<User> userPage = userRepository.selectPage(page, queryWrapper);

            // 转换为DTO
            List<UserListDTO> userDTOList = userPage.getRecords().stream()
                    .map(this::convertToUserListDTO)
                    .collect(Collectors.toList());

            // 转换为Spring Data的Page
            Pageable pageable = Pageable.ofSize(searchDTO.getSize()).withPage(searchDTO.getPage());
            return new PageImpl<>(userDTOList, pageable, userPage.getTotal());

        } catch (Exception e) {
            log.error("查询用户列表失败", e);
            throw new RuntimeException("查询用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户详情
     */
    @Transactional(readOnly = true)
    public UserDetailDTO getUserDetail(Long userId) {
        try {
            log.info("获取用户详情，用户ID: {}", userId);

            User user = userRepository.selectById(userId);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            UserDetailDTO userDetail = convertToUserDetailDTO(user);

            // 统计用户订单数量
            Integer orderCount = orderRepository.countByUserId(userId);
            userDetail.setTotalOrders(orderCount != null ? orderCount : 0);

            // 统计用户评价数量
            Integer reviewCount = reviewRepository.countByUserId(userId);
            userDetail.setTotalReviews(reviewCount != null ? reviewCount : 0);

            // 计算用户总消费（这里简化处理，实际应该根据订单金额统计）
            userDetail.setTotalSpent(0.0);

            // 获取用户平均评分（这里简化处理）
            userDetail.setAverageRating(0.0);

            // 设置会员等级（这里简化处理）
            userDetail.setMemberLevel(getMemberLevel(orderCount != null ? orderCount : 0));

            return userDetail;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户详情失败，用户ID: {}", userId, e);
            throw new RuntimeException("获取用户详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户状态
     */
    @CacheEvict(value = {"user-statistics", "user-search"}, allEntries = true)
    public UserListDTO updateUserStatus(UserManagementDTO managementDTO) {
        try {
            log.info("更新用户状态，用户ID: {}, 新状态: {}",
                    managementDTO.getUserId(), managementDTO.getNewStatus());

            User user = userRepository.selectById(managementDTO.getUserId());
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 防止管理员禁用自己
            if ("ADMIN".equals(user.getRole()) && "INACTIVE".equals(managementDTO.getNewStatus())) {
                throw new IllegalArgumentException("不能禁用管理员账户");
            }

            String oldStatus = user.getStatus();
            user.setStatus(managementDTO.getNewStatus());
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.updateById(user);

            // 记录操作历史
            operationHistoryService.recordOperation(
                managementDTO.getUserId(),
                "STATUS_CHANGE",
                managementDTO.getOperatedBy(),
                String.format("状态从 %s 变更为 %s，原因: %s", oldStatus, managementDTO.getNewStatus(), managementDTO.getReason()),
                null,
                null
            );

            return convertToUserListDTO(user);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            throw new RuntimeException("更新用户状态失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户（逻辑删除）
     */
    @CacheEvict(value = {"user-statistics", "user-search"}, allEntries = true)
    public void deleteUser(Long userId, String reason) {
        try {
            log.info("删除用户，用户ID: {}, 原因: {}", userId, reason);

            User user = userRepository.selectById(userId);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 防止管理员删除自己
            if ("ADMIN".equals(user.getRole())) {
                throw new IllegalArgumentException("不能删除管理员账户");
            }

            // 检查是否有关联的订单或评价
            Integer orderCount = orderRepository.countByUserId(userId);
            if (orderCount != null && orderCount > 0) {
                throw new IllegalArgumentException("用户有关联订单，无法删除");
            }

            // 逻辑删除
            user.setDeleted(1);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.updateById(user);

            // 记录操作历史
            operationHistoryService.recordOperation(
                userId,
                "DELETE",
                null, // 操作者ID可以从前端获取
                String.format("用户被删除，原因: %s", reason),
                null,
                null
            );

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除用户失败，用户ID: {}", userId, e);
            throw new RuntimeException("删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新用户状态
     */
    @CacheEvict(value = {"user-statistics", "user-search"}, allEntries = true)
    public BatchOperationResultDTO batchUpdateUserStatus(UserBatchOperationDTO batchDTO) {
        try {
            log.info("批量更新用户状态，用户数量: {}, 操作: {}",
                    batchDTO.getUserIds().size(), batchDTO.getOperation());

            BatchOperationResultDTO result = new BatchOperationResultDTO();
            result.setOperation(batchDTO.getOperation());
            result.setTotalCount(batchDTO.getUserIds().size());
            result.setSuccessCount(0);
            result.setFailureCount(0);
            result.setSuccessUserIds(new ArrayList<>());
            result.setFailureReasons(new HashMap<>());
            result.setOperatedAt(LocalDateTime.now());
            result.setOperatedBy(batchDTO.getOperatedBy());
            result.setIsCompleted(true);
            result.setStatus("COMPLETED");

            for (Long userId : batchDTO.getUserIds()) {
                try {
                    UserManagementDTO managementDTO = new UserManagementDTO();
                    managementDTO.setUserId(userId);
                    managementDTO.setNewStatus("ENABLE".equals(batchDTO.getOperation()) ? "ACTIVE" : "INACTIVE");
                    managementDTO.setReason(batchDTO.getReason());
                    managementDTO.setOperatedBy(batchDTO.getOperatedBy());
                    managementDTO.setOperatedAt(LocalDateTime.now());

                    updateUserStatus(managementDTO);

                    result.getSuccessUserIds().add(userId);
                    result.setSuccessCount(result.getSuccessCount() + 1);

                } catch (Exception e) {
                    result.getFailureReasons().put(userId, e.getMessage());
                    result.setFailureCount(result.getFailureCount() + 1);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("批量更新用户状态失败", e);
            throw new RuntimeException("批量更新用户状态失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除用户
     */
    @CacheEvict(value = {"user-statistics", "user-search"}, allEntries = true)
    public BatchOperationResultDTO batchDeleteUsers(UserBatchOperationDTO batchDTO) {
        try {
            log.info("批量删除用户，用户数量: {}", batchDTO.getUserIds().size());

            BatchOperationResultDTO result = new BatchOperationResultDTO();
            result.setOperation("DELETE");
            result.setTotalCount(batchDTO.getUserIds().size());
            result.setSuccessCount(0);
            result.setFailureCount(0);
            result.setSuccessUserIds(new ArrayList<>());
            result.setFailureReasons(new HashMap<>());
            result.setOperatedAt(LocalDateTime.now());
            result.setOperatedBy(batchDTO.getOperatedBy());
            result.setIsCompleted(true);
            result.setStatus("COMPLETED");

            for (Long userId : batchDTO.getUserIds()) {
                try {
                    deleteUser(userId, batchDTO.getReason());

                    result.getSuccessUserIds().add(userId);
                    result.setSuccessCount(result.getSuccessCount() + 1);

                } catch (Exception e) {
                    result.getFailureReasons().put(userId, e.getMessage());
                    result.setFailureCount(result.getFailureCount() + 1);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("批量删除用户失败", e);
            throw new RuntimeException("批量删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户统计信息
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "user-statistics", key = "'statistics'")
    public UserStatisticsDTO getUserStatistics() {
        try {
            log.info("获取用户统计信息");

            UserStatisticsDTO statistics = new UserStatisticsDTO();

            // 总用户数
            Long totalUsers = userRepository.selectCount(null);
            statistics.setTotalUsers(totalUsers != null ? totalUsers : 0L);

            // 活跃用户数
            LambdaQueryWrapper<User> activeQuery = new LambdaQueryWrapper<>();
            activeQuery.eq(User::getStatus, "ACTIVE");
            Long activeUsers = userRepository.selectCount(activeQuery);
            statistics.setActiveUsers(activeUsers != null ? activeUsers : 0L);

            // 非活跃用户数
            statistics.setInactiveUsers(statistics.getTotalUsers() - statistics.getActiveUsers());

            // 管理员用户数
            LambdaQueryWrapper<User> adminQuery = new LambdaQueryWrapper<>();
            adminQuery.eq(User::getRole, "ADMIN");
            Long adminUsers = userRepository.selectCount(adminQuery);
            statistics.setAdminUsers(adminUsers != null ? adminUsers : 0L);

            // 普通用户数
            statistics.setRegularUsers(statistics.getTotalUsers() - statistics.getAdminUsers());

            // 今日新增用户
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LambdaQueryWrapper<User> todayQuery = new LambdaQueryWrapper<>();
            todayQuery.ge(User::getCreatedAt, todayStart);
            Long newUsersToday = userRepository.selectCount(todayQuery);
            statistics.setNewUsersToday(newUsersToday != null ? newUsersToday : 0L);

            // 本月新增用户
            LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LambdaQueryWrapper<User> monthQuery = new LambdaQueryWrapper<>();
            monthQuery.ge(User::getCreatedAt, monthStart);
            Long newUsersThisMonth = userRepository.selectCount(monthQuery);
            statistics.setNewUsersThisMonth(newUsersThisMonth != null ? newUsersThisMonth : 0L);

            // 本年新增用户
            LocalDateTime yearStart = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LambdaQueryWrapper<User> yearQuery = new LambdaQueryWrapper<>();
            yearQuery.ge(User::getCreatedAt, yearStart);
            Long newUsersThisYear = userRepository.selectCount(yearQuery);
            statistics.setNewUsersThisYear(newUsersThisYear != null ? newUsersThisYear : 0L);

            // 计算活跃率
            if (statistics.getTotalUsers() > 0) {
                statistics.setActiveRate((double) statistics.getActiveUsers() / statistics.getTotalUsers() * 100);
            } else {
                statistics.setActiveRate(0.0);
            }

            // 增长率（这里简化处理）
            statistics.setGrowthRateToday(0.0);
            statistics.setGrowthRateThisMonth(0.0);
            statistics.setGrowthRateThisYear(0.0);

            return statistics;

        } catch (Exception e) {
            log.error("获取用户统计信息失败", e);
            throw new RuntimeException("获取用户统计信息失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 转换为用户列表DTO
     */
    private UserListDTO convertToUserListDTO(User user) {
        UserListDTO dto = new UserListDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setRegistrationDate(user.getCreatedAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setIsDeleted(user.getDeleted() == 1);

        // 这里应该设置最后登录时间，需要从登录日志中获取
        dto.setLastLoginAt(null);

        // 统计订单和评价数量（简化处理）
        dto.setTotalOrders(0);
        dto.setTotalReviews(0);

        return dto;
    }

    /**
     * 转换为用户详情DTO
     */
    private UserDetailDTO convertToUserDetailDTO(User user) {
        UserDetailDTO dto = new UserDetailDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRealName(user.getRealName());
        dto.setGender(user.getGender());
        dto.setBirthDate(user.getBirthDate());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        // 这里应该设置最后登录信息和IP地址，需要从登录日志中获取
        dto.setLastLoginAt(null);
        dto.setLastLoginIp(null);

        return dto;
    }

    /**
     * 获取排序字段
     */
    private String getSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "username" -> "username";
            case "email" -> "email";
            case "phone" -> "phone";
            case "role" -> "role";
            case "status" -> "status";
            case "createdat", "created_at" -> "created_at";
            case "updatedat", "updated_at" -> "updated_at";
            default -> "created_at";
        };
    }

    /**
     * 获取排序列
     */
    private String getSortColumn(String sortField) {
        return switch (sortField) {
            case "username" -> User::getUsername;
            case "email" -> User::getEmail;
            case "phone" -> User::getPhone;
            case "role" -> User::getRole;
            case "status" -> User::getStatus;
            case "created_at" -> User::getCreatedAt;
            case "updated_at" -> User::getUpdatedAt;
            default -> User::getCreatedAt;
        };
    }

    /**
     * 根据订单数量获取会员等级
     */
    private String getMemberLevel(Integer orderCount) {
        if (orderCount >= 50) return "钻石会员";
        if (orderCount >= 30) return "金牌会员";
        if (orderCount >= 20) return "银牌会员";
        if (orderCount >= 10) return "铜牌会员";
        if (orderCount >= 1) return "普通会员";
        return "未注册";
    }
}