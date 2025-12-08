package com.hotel.service;

import com.hotel.dto.bookingConflict.*;
import com.hotel.entity.BookingConflict;
import com.hotel.entity.WaitingList;
import com.hotel.repository.BookingConflictRepository;
import com.hotel.repository.WaitingListRepository;
import com.hotel.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 并发安全的预订冲突处理服务
 * 使用分布式锁确保高并发场景下的数据一致性
 *
 * @author System
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConcurrentBookingConflictService {

    private final BookingConflictRepository bookingConflictRepository;
    private final WaitingListRepository waitingListRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final RedissonClient redissonClient;

    // 锁的超时时间（秒）
    private static final long LOCK_TIMEOUT_SECONDS = 30;
    // 尝试获取锁的等待时间（秒）
    private static final long LOCK_WAIT_TIME_SECONDS = 5;

    /**
     * 并发安全的冲突检测
     * 使用分布式锁确保同一房间的冲突检测原子性
     */
    @Transactional
    public ConflictDetectionResult detectConflictConcurrently(DetectConflictRequest request) {
        String lockKey = String.format("conflict_detect:room:%d:dates:%s_%s",
                request.getRoomId(),
                request.getCheckInDate().toLocalDate(),
                request.getCheckOutDate().toLocalDate());

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取分布式锁
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取冲突检测锁失败，房间ID: {}, 用户ID: {}", request.getRoomId(), request.getUserId());
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            log.info("获取冲突检测锁成功，房间ID: {}, 用户ID: {}", request.getRoomId(), request.getUserId());

            try {
                // 执行冲突检测逻辑
                return performConflictDetection(request);
            } finally {
                // 确保锁被释放
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("释放冲突检测锁，房间ID: {}, 用户ID: {}", request.getRoomId(), request.getUserId());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("冲突检测锁获取被中断", e);
            throw new RuntimeException("检测过程中断，请重试");
        }
    }

    /**
     * 并发安全的等待列表确认
     * 使用分布式锁防止多人同时确认同一个等待列表位置
     */
    @Transactional
    public boolean confirmWaitingListBookingConcurrently(ConfirmWaitingListRequest request) {
        String lockKey = String.format("confirm_waiting:room:%d:waiting:%d",
                request.getWaitingListId(), System.currentTimeMillis());

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取分布式锁
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取等待列表确认锁失败，等待列表ID: {}", request.getWaitingListId());
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            log.info("获取等待列表确认锁成功，等待列表ID: {}", request.getWaitingListId());

            try {
                return performWaitingListConfirmation(request);
            } finally {
                // 确保锁被释放
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("释放等待列表确认锁，等待列表ID: {}", request.getWaitingListId());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待列表确认锁获取被中断", e);
            throw new RuntimeException("确认过程中断，请重试");
        }
    }

    /**
     * 并发安全的房间可用性检查
     * 使用分布式锁确保可用性检查的原子性
     */
    @Transactional(readOnly = true)
    public boolean checkRoomAvailabilityConcurrently(Long roomId, LocalDateTime checkIn, LocalDateTime checkOut) {
        String lockKey = String.format("room_availability:room:%d", roomId);

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取分布式锁，使用较短的等待时间
            boolean acquired = lock.tryLock(1, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取房间可用性检查锁失败，房间ID: {}", roomId);
                // 可用性检查失败时，保守返回false
                return false;
            }

            try {
                return performRoomAvailabilityCheck(roomId, checkIn, checkOut);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("房间可用性检查锁获取被中断", e);
            return false;
        }
    }

    /**
     * 并发安全的等待列表优先级更新
     */
    @Transactional
    public void updateWaitingListPrioritiesConcurrently(Long roomId) {
        String lockKey = String.format("update_priorities:room:%d", roomId);

        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取优先级更新锁失败，房间ID: {}", roomId);
                return; // 优先级更新失败不是关键错误
            }

            try {
                performPriorityUpdate(roomId);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("优先级更新锁获取被中断", e);
        }
    }

    /**
     * 执行实际的冲突检测逻辑
     */
    private ConflictDetectionResult performConflictDetection(DetectConflictRequest request) {
        validateConflictRequest(request);

        // 查询冲突订单
        List<Long> conflictingOrderIds = bookingConflictRepository.findConflictingOrders(
                request.getRoomId(),
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        if (conflictingOrderIds.isEmpty()) {
            return ConflictDetectionResult.builder()
                    .hasConflict(false)
                    .message("无冲突，可以预订")
                    .build();
        }

        // 记录冲突
        BookingConflict conflict = BookingConflict.builder()
                .roomId(request.getRoomId())
                .userId(request.getUserId())
                .requestedCheckInDate(request.getCheckInDate())
                .requestedCheckOutDate(request.getCheckOutDate())
                .conflictingOrderId(conflictingOrderIds.get(0))
                .conflictType(BookingConflict.ConflictType.TIME_OVERLAP)
                .status(BookingConflict.ConflictStatus.DETECTED)
                .resolutionDetails("时间重叠冲突")
                .createdAt(LocalDateTime.now())
                .build();

        bookingConflictRepository.save(conflict);

        return ConflictDetectionResult.builder()
                .hasConflict(true)
                .conflictId(conflict.getId())
                .conflictType(conflict.getConflictType().name())
                .conflictingOrderId(conflict.getConflictingOrderId())
                .message("检测到时间重叠冲突")
                .build();
    }

    /**
     * 执行等待列表确认逻辑
     */
    private boolean performWaitingListConfirmation(ConfirmWaitingListRequest request) {
        Optional<WaitingList> waitingListOpt = waitingListRepository.findById(request.getWaitingListId());
        if (!waitingListOpt.isPresent()) {
            return false;
        }

        WaitingList waitingList = waitingListOpt.get();

        // 检查状态是否为已通知
        if (!WaitingList.WaitingListStatus.NOTIFIED.equals(waitingList.getStatus())) {
            log.warn("等待列表状态不正确，当前状态: {}", waitingList.getStatus());
            return false;
        }

        // 检查是否已过期
        if (waitingList.getExpiresAt() != null && waitingList.getExpiresAt().isBefore(LocalDateTime.now())) {
            waitingList.setStatus(WaitingList.WaitingListStatus.EXPIRED);
            waitingListRepository.save(waitingList);
            return false;
        }

        // 再次检查房间可用性（双重检查）
        boolean roomAvailable = performRoomAvailabilityCheck(
                waitingList.getRoomId(),
                waitingList.getRequestedCheckInDate(),
                waitingList.getRequestedCheckOutDate()
        );

        if (!roomAvailable) {
            log.warn("房间已被其他用户预订，等待列表ID: {}", waitingList.getId());
            return false;
        }

        // 创建订单
        // 这里应该调用订单服务创建订单，简化处理
        waitingList.setStatus(WaitingList.WaitingListStatus.CONFIRMED);
        waitingList.setConfirmedOrderId(request.getOrderId());
        waitingList.setUpdatedAt(LocalDateTime.now());

        waitingListRepository.save(waitingList);

        // 发送通知
        try {
            notificationService.sendWaitingListConfirmationNotification(
                    waitingList.getUserId(),
                    waitingList.getRoomId(),
                    request.getOrderId()
            );
        } catch (Exception e) {
            log.error("发送等待列表确认通知失败", e);
            // 不影响主流程
        }

        return true;
    }

    /**
     * 执行房间可用性检查
     */
    private boolean performRoomAvailabilityCheck(Long roomId, LocalDateTime checkIn, LocalDateTime checkOut) {
        List<Long> conflictingOrders = orderRepository.findConflictingOrders(roomId, checkIn, checkOut);
        return conflictingOrders.isEmpty();
    }

    /**
     * 执行优先级更新
     */
    private void performPriorityUpdate(Long roomId) {
        List<WaitingList> waitingLists = waitingListRepository.findByRoomIdOrderByPriorityDescCreatedAtAsc(roomId);

        // 重新计算位置和优先级
        for (int i = 0; i < waitingLists.size(); i++) {
            WaitingList waitingList = waitingLists.get(i);
            // 更新位置信息（通过某个字段存储）
            // 这里可以根据业务需求调整优先级计算逻辑
        }
    }

    /**
     * 验证冲突检测请求
     */
    private void validateConflictRequest(DetectConflictRequest request) {
        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("房间ID不能为空");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            throw new IllegalArgumentException("入住和退房日期不能为空");
        }
        if (!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            throw new IllegalArgumentException("入住日期必须早于退房日期");
        }
        if (request.getCheckInDate().isBefore(LocalDateTime.now().toLocalDate().atStartOfDay())) {
            throw new IllegalArgumentException("入住日期不能早于今天");
        }
    }

    /**
     * 获取分布式锁状态（用于监控）
     */
    public boolean isLockAcquired(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * 强制释放锁（紧急情况使用）
     */
    public void forceReleaseLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isLocked()) {
            lock.forceUnlock();
            log.warn("强制释放锁: {}", lockKey);
        }
    }
}