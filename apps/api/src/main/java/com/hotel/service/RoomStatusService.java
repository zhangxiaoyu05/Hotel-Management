package com.hotel.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hotel.entity.Room;
import com.hotel.entity.RoomStatusLog;
import com.hotel.entity.User;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomStatusLogRepository;
import com.hotel.repository.UserRepository;
import com.hotel.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomStatusService extends ServiceImpl<RoomRepository, Room> {

    private final RoomRepository roomRepository;
    private final RoomStatusLogRepository roomStatusLogRepository;
    private final UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 限制用户在指定时间内的状态更新频率
    private static final String RATE_LIMIT_KEY_PREFIX = "room_status_rate_limit:";
    private static final int RATE_LIMIT_WINDOW = 60; // 60秒
    private static final int RATE_LIMIT_COUNT = 5; // 最多5次操作

    /**
     * 更新房间状态
     * @param roomId 房间ID
     * @param newStatus 新状态
     * @param reason 变更原因
     * @param changedBy 操作用户ID
     * @param orderId 关联订单ID（可选）
     * @param expectedVersion 乐观锁版本号
     * @return 是否更新成功
     */
    @Transactional
    @CacheEvict(value = "room_status", key = "#roomId")
    public boolean updateRoomStatus(Long roomId, String newStatus, String reason,
                                   Long changedBy, Long orderId, Integer expectedVersion) {

        // 1. 验证房间是否存在
        Room room = roomRepository.selectById(roomId);
        if (room == null) {
            log.warn("Room not found: {}", roomId);
            return false;
        }

        // 2. 权限验证
        if (!hasPermissionToChangeStatus(changedBy, roomId, newStatus)) {
            log.warn("User {} does not have permission to change room {} status to {}",
                    changedBy, roomId, newStatus);
            throw new SecurityException("您没有权限修改此房间状态");
        }

        // 3. 频率限制检查
        if (!checkRateLimit(changedBy)) {
            log.warn("Rate limit exceeded for user: {}", changedBy);
            throw new RuntimeException("操作过于频繁，请稍后再试");
        }

        // 4. 验证状态流转规则
        if (!isValidStatusTransition(room.getStatus(), newStatus)) {
            log.warn("Invalid status transition from {} to {} for room {}",
                    room.getStatus(), newStatus, roomId);
            throw new IllegalArgumentException("Invalid status transition: " +
                    room.getStatus() + " -> " + newStatus);
        }

        // 5. 乐观锁验证
        if (expectedVersion != null && !Objects.equals(room.getVersion(), expectedVersion)) {
            log.warn("Optimistic lock conflict for room {}, expected version {}, actual version {}",
                    roomId, expectedVersion, room.getVersion());
            throw new RuntimeException("Room has been modified by another user");
        }

        // 6. 记录审计日志
        logAuditEvent("ROOM_STATUS_CHANGE_ATTEMPT", changedBy, roomId,
                String.format("Attempting to change status from %s to %s", room.getStatus(), newStatus));

        // 4. 记录旧状态
        String oldStatus = room.getStatus();

        try {
            // 5. 更新房间状态
            UpdateWrapper<Room> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", roomId)
                    .eq("version", expectedVersion) // 乐观锁
                    .set("status", newStatus)
                    .set("version", room.getVersion() + 1)
                    .set("last_status_changed_at", LocalDateTime.now())
                    .set("last_status_changed_by", changedBy);

            int updateCount = roomRepository.update(null, updateWrapper);

            if (updateCount == 0) {
                log.warn("Failed to update room status, possibly due to version conflict: {}", roomId);
                throw new RuntimeException("Failed to update room status, please refresh and try again");
            }

            // 6. 记录状态变更日志
            RoomStatusLog statusLog = new RoomStatusLog();
            statusLog.setRoomId(roomId);
            statusLog.setOldStatus(oldStatus);
            statusLog.setNewStatus(newStatus);
            statusLog.setReason(reason);
            statusLog.setChangedBy(changedBy);
            statusLog.setOrderId(orderId);
            statusLog.setCreatedAt(LocalDateTime.now());

            roomStatusLogRepository.insert(statusLog);

            log.info("Successfully updated room {} status from {} to {}", roomId, oldStatus, newStatus);
            return true;

        } catch (Exception e) {
            log.error("Failed to update room status for room {}: {}", roomId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 验证状态流转是否合法
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (Objects.equals(currentStatus, newStatus)) {
            return false;
        }

        return switch (currentStatus) {
            case "AVAILABLE" -> List.of("OCCUPIED", "MAINTENANCE").contains(newStatus);
            case "OCCUPIED" -> List.of("CLEANING").contains(newStatus);
            case "CLEANING" -> List.of("AVAILABLE", "MAINTENANCE").contains(newStatus);
            case "MAINTENANCE" -> List.of("AVAILABLE").contains(newStatus);
            default -> false;
        };
    }

    /**
     * 获取房间状态变更历史
     */
    public List<RoomStatusLog> getRoomStatusHistory(Long roomId, LocalDateTime startDate,
                                                    LocalDateTime endDate, Integer limit) {
        if (startDate != null && endDate != null) {
            return roomStatusLogRepository.findByRoomIdAndTimeRange(roomId, startDate, endDate);
        } else if (limit != null) {
            return roomStatusLogRepository.findRecentByRoomId(roomId, limit);
        } else {
            return roomStatusLogRepository.findByRoomIdAndTimeRange(roomId, null, null);
        }
    }

    /**
     * 检查房间是否可用
     */
    public boolean isRoomAvailable(Long roomId) {
        Room room = roomRepository.selectById(roomId);
        return room != null && "AVAILABLE".equals(room.getStatus());
    }

    /**
     * 批量检查房间可用性
     */
    public java.util.Map<Long, Boolean> checkRoomsAvailability(List<Long> roomIds) {
        List<Room> rooms = roomRepository.selectBatchIds(roomIds);
        java.util.Map<Long, Boolean> availability = new java.util.HashMap<>();

        for (Room room : rooms) {
            availability.put(room.getId(), "AVAILABLE".equals(room.getStatus()));
        }

        // 确保所有请求的房间ID都有结果
        for (Long roomId : roomIds) {
            availability.putIfAbsent(roomId, false);
        }

        return availability;
    }

    /**
     * 分页获取房间状态变更日志
     */
    public IPage<RoomStatusLog> getRoomStatusLogsWithPagination(Page<RoomStatusLog> page,
                                                               Long roomId,
                                                               LocalDateTime startDate,
                                                               LocalDateTime endDate) {
        return roomStatusLogRepository.findByRoomIdWithPagination(page, roomId, startDate, endDate);
    }

    /**
     * 权限验证：检查用户是否有权限修改房间状态
     */
    private boolean hasPermissionToChangeStatus(Long userId, Long roomId, String newStatus) {
        try {
            User user = userRepository.selectById(userId);
            if (user == null) {
                log.warn("User not found: {}", userId);
                return false;
            }

            // 管理员可以修改任何房间状态
            if ("ADMIN".equals(user.getRole())) {
                return true;
            }

            // 员工可以修改部分状态
            if ("STAFF".equals(user.getRole())) {
                // 员工可以将房间设置为维护或清洁状态
                return "MAINTENANCE".equals(newStatus) || "CLEANING".equals(newStatus);
            }

            // 普通用户不能手动修改房间状态
            if ("USER".equals(user.getRole())) {
                return false;
            }

            return false;
        } catch (Exception e) {
            log.error("Error checking permission for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 频率限制检查
     */
    private boolean checkRateLimit(Long userId) {
        try {
            String key = RATE_LIMIT_KEY_PREFIX + userId;
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount == 1) {
                // 第一次设置，添加过期时间
                redisTemplate.expire(key, RATE_LIMIT_WINDOW, TimeUnit.SECONDS);
            }

            return currentCount <= RATE_LIMIT_COUNT;
        } catch (Exception e) {
            log.error("Error checking rate limit for user {}: {}", userId, e.getMessage());
            // 如果Redis出现异常，为了不影响业务，允许操作通过
            return true;
        }
    }

    /**
     * 记录审计日志
     */
    private void logAuditEvent(String eventType, Long userId, Long roomId, String details) {
        try {
            String auditKey = "audit:room_status:" + System.currentTimeMillis();
            String auditData = String.format(
                "{\"eventType\":\"%s\",\"userId\":%d,\"roomId\":%d,\"details\":\"%s\",\"timestamp\":\"%s\"}",
                eventType, userId, roomId, details, LocalDateTime.now().toString()
            );

            // 存储到Redis，设置30天过期
            redisTemplate.opsForValue().set(auditKey, auditData, 30, TimeUnit.DAYS);

            // 同时记录到应用日志
            log.info("AUDIT: {} - User: {}, Room: {}, Details: {}", eventType, userId, roomId, details);
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage());
        }
    }

    /**
     * 获取用户操作历史（用于审计）
     */
    public List<String> getUserOperationHistory(Long userId, int limit) {
        try {
            String pattern = "audit:room_status:*";
            Set<Object> keys = redisTemplate.keys(pattern);

            return keys.stream()
                .limit(limit)
                .map(key -> (String) redisTemplate.opsForValue().get(key))
                .filter(data -> data != null && data.contains("\"userId\":" + userId))
                .toList();
        } catch (Exception e) {
            log.error("Failed to get user operation history: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 检测可疑操作模式
     */
    public boolean detectSuspiciousActivity(Long userId) {
        try {
            // 检查用户在最近1小时内的操作次数
            long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
            String pattern = "audit:room_status:*";
            Set<Object> keys = redisTemplate.keys(pattern);

            long recentOperations = keys.stream()
                .mapToLong(key -> {
                    String keyStr = (String) key;
                    long timestamp = Long.parseLong(keyStr.split(":")[2]);
                    return timestamp >= oneHourAgo ? 1 : 0;
                })
                .sum();

            // 如果1小时内操作超过20次，认为可疑
            if (recentOperations > 20) {
                log.warn("Suspicious activity detected for user {}: {} operations in 1 hour",
                        userId, recentOperations);

                // 记录可疑活动
                logAuditEvent("SUSPICIOUS_ACTIVITY", userId, null,
                        String.format("Too many operations: %d in 1 hour", recentOperations));

                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Error detecting suspicious activity: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取房间状态（带缓存）
     */
    @Cacheable(value = "room_status", key = "#roomId", unless = "#result == null")
    public String getRoomStatusCached(Long roomId) {
        Room room = roomRepository.selectById(roomId);
        return room != null ? room.getStatus() : null;
    }

    /**
     * 批量检查房间可用性（带缓存优化）
     */
    @Cacheable(value = "room_availability_batch", key = "#roomIds.hashCode()", unless = "#result.isEmpty()")
    public java.util.Map<Long, Boolean> checkRoomsAvailabilityCached(List<Long> roomIds) {
        return checkRoomsAvailability(roomIds);
    }
}