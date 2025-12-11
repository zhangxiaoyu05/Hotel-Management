package com.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.dto.bookingConflict.*;
import com.hotel.entity.BookingConflict;
import com.hotel.entity.WaitingList;
import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.entity.User;
import com.hotel.repository.BookingConflictRepository;
import com.hotel.repository.WaitingListRepository;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;
import com.hotel.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingConflictService {

    private final BookingConflictRepository bookingConflictRepository;
    private final WaitingListRepository waitingListRepository;
    private final OrderRepository orderRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RoomStatusService roomStatusService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 检测预订冲突
     */
    @Transactional
    public ConflictDetectionResult detectConflict(DetectConflictRequest request) {
        log.info("检测预订冲突，房间ID: {}, 用户ID: {}", request.getRoomId(), request.getUserId());

        Room room = roomRepository.selectById(request.getRoomId());
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        // 检查时间重叠的订单
        List<Order> conflictingOrders = orderRepository.findConflictingOrders(
                request.getRoomId(),
                request.getCheckInDate().toLocalDate(),
                request.getCheckOutDate().toLocalDate(),
                "CONFIRMED"
        );

        if (conflictingOrders.isEmpty()) {
            return ConflictDetectionResult.builder()
                    .hasConflict(false)
                    .build();
        }

        // 创建冲突记录
        BookingConflict conflict = new BookingConflict();
        conflict.setRoomId(request.getRoomId());
        conflict.setUserId(request.getUserId());
        conflict.setRequestedCheckInDate(request.getCheckInDate());
        conflict.setRequestedCheckOutDate(request.getCheckOutDate());
        conflict.setConflictingOrderId(conflictingOrders.get(0).getId());
        conflict.setConflictType(determineConflictType(conflictingOrders));
        conflict.setStatus("DETECTED");
        bookingConflictRepository.insert(conflict);

        // 查找替代房间
        List<Room> alternativeRooms = findAlternativeRooms(room, request);

        return ConflictDetectionResult.builder()
                .hasConflict(true)
                .conflictId(conflict.getId())
                .conflictType(conflict.getConflictType())
                .conflictingOrderId(conflictingOrders.get(0).getId())
                .alternativeRooms(alternativeRooms.stream()
                        .map(r -> AlternativeRoom.builder()
                                .roomId(r.getId())
                                .roomNumber(r.getRoomNumber())
                                .roomType(r.getRoomType())
                                .price(r.getBasePrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 加入等待列表
     */
    @Transactional
    public WaitingListResponse joinWaitingList(JoinWaitingListRequest request) {
        log.info("用户 {} 加入房间 {} 的等待列表", request.getUserId(), request.getRoomId());

        // 计算优先级（这里可以加入VIP用户等逻辑）
        Integer priority = calculateWaitingPriority(request.getUserId());

        WaitingList waitingList = new WaitingList();
        waitingList.setRoomId(request.getRoomId());
        waitingList.setUserId(request.getUserId());
        waitingList.setRequestedCheckInDate(request.getCheckInDate());
        waitingList.setRequestedCheckOutDate(request.getCheckOutDate());
        waitingList.setGuestCount(request.getGuestCount());
        waitingList.setPriority(priority);
        waitingList.setStatus("WAITING");
        waitingList.setNotificationSent(false);

        waitingListRepository.insert(waitingList);

        // 发送加入等待列表确认通知
        notificationService.sendWaitingListConfirmation(waitingList);

        return WaitingListResponse.builder()
                .waitingListId(waitingList.getId())
                .roomId(waitingList.getRoomId())
                .status(waitingList.getStatus())
                .priority(waitingList.getPriority())
                .estimatedWaitTime(estimateWaitTime(request.getRoomId()))
                .build();
    }

    /**
     * 处理房间释放时的等待列表
     */
    @Transactional
    public void processWaitingListForRoom(Long roomId) {
        log.info("处理房间 {} 的等待列表", roomId);

        List<WaitingList> waitingList = waitingListRepository.findByRoomIdOrderByPriority(
                roomId, "WAITING"
        );

        if (waitingList.isEmpty()) {
            return;
        }

        for (WaitingList waiting : waitingList) {
            // 检查等待是否仍然有效（时间是否还可用）
            if (isTimeSlotAvailable(roomId, waiting.getRequestedCheckInDate(), waiting.getRequestedCheckOutDate())) {
                // 发送通知
                boolean notificationSent = notificationService.sendRoomAvailableNotification(waiting);

                if (notificationSent) {
                    // 更新状态为已通知
                    waitingListRepository.updateStatusToNotified(
                            waiting.getId(), LocalDateTime.now()
                    );

                    // 设置24小时过期时间
                    waiting.setExpiresAt(LocalDateTime.now().plusHours(24));
                    waitingListRepository.updateById(waiting);

                    // 发送WebSocket事件
                    eventPublisher.publishEvent(new RoomAvailableEvent(
                            roomId, waiting.getUserId(), waiting.getId()
                    ));

                    break; // 每次只处理一个，避免过度通知
                }
            }
        }
    }

    /**
     * 确认等待列表预订
     */
    @Transactional
    public OrderResponse confirmWaitingListBooking(Long waitingListId, ConfirmWaitingListRequest request) {
        WaitingList waitingList = waitingListRepository.selectById(waitingListId);
        if (waitingList == null) {
            throw new RuntimeException("等待列表记录不存在");
        }

        if (!"NOTIFIED".equals(waitingList.getStatus())) {
            throw new RuntimeException("等待列表状态不允许确认");
        }

        if (waitingList.getExpiresAt() != null && waitingList.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("等待列表已过期");
        }

        // 再次检查房间是否可用
        if (!isTimeSlotAvailable(waitingList.getRoomId(),
                waitingList.getRequestedCheckInDate(),
                waitingList.getRequestedCheckOutDate())) {
            throw new RuntimeException("房间时间段已被预订");
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(waitingList.getUserId());
        order.setRoomId(waitingList.getRoomId());
        order.setCheckInDate(waitingList.getRequestedCheckInDate().toLocalDate());
        order.setCheckOutDate(waitingList.getRequestedCheckOutDate().toLocalDate());
        order.setGuestCount(waitingList.getGuestCount());
        order.setStatus("CONFIRMED");

        orderRepository.insert(order);

        // 更新等待列表状态
        waitingListRepository.updateStatusToConfirmed(waitingListId, order.getId());

        return convertToOrderResponse(order);
    }

    /**
     * 获取用户的等待列表
     */
    public IPage<WaitingListResponse> getUserWaitingList(Long userId, WaitingListQueryRequest request) {
        Page<WaitingList> page = new Page<>(request.getPage(), request.getSize());

        LambdaQueryWrapper<WaitingList> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaitingList::getUserId, userId);
        if (request.getStatus() != null) {
            wrapper.eq(WaitingList::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(WaitingList::getCreatedAt);

        IPage<WaitingList> result = waitingListRepository.selectPage(page, wrapper);

        return result.convert(this::convertToWaitingListResponse);
    }

    /**
     * 获取冲突统计
     */
    public ConflictStatisticsResponse getConflictStatistics(ConflictStatisticsRequest request) {
        LocalDateTime startDate = request.getStartDate() != null ?
                request.getStartDate() : LocalDateTime.now().minusMonths(1);
        LocalDateTime endDate = request.getEndDate() != null ?
                request.getEndDate() : LocalDateTime.now();

        // 统计各类型冲突数量
        int timeOverlapCount = bookingConflictRepository.countByConflictType(
                "TIME_OVERLAP", startDate, endDate
        );
        int doubleBookingCount = bookingConflictRepository.countByConflictType(
                "DOUBLE_BOOKING", startDate, endDate
        );
        int concurrentRequestCount = bookingConflictRepository.countByConflictType(
                "CONCURRENT_REQUEST", startDate, endDate
        );

        return ConflictStatisticsResponse.builder()
                .totalConflicts(timeOverlapCount + doubleBookingCount + concurrentRequestCount)
                .timeOverlapConflicts(timeOverlapCount)
                .doubleBookingConflicts(doubleBookingCount)
                .concurrentRequestConflicts(concurrentRequestCount)
                .resolvedConflicts(getResolvedConflictCount(startDate, endDate))
                .waitingListSize(getTotalWaitingListSize(request.getRoomId()))
                .build();
    }

    /**
     * 清理过期的等待列表
     */
    @Transactional
    public void cleanupExpiredWaitingList() {
        List<WaitingList> expiredList = waitingListRepository.findExpiredWaitingList(
                LocalDateTime.now()
        );

        for (WaitingList waiting : expiredList) {
            waiting.setStatus("EXPIRED");
            waitingListRepository.updateById(waiting);

            // 发送过期通知
            notificationService.sendWaitingListExpiredNotification(waiting);
        }

        log.info("清理了 {} 个过期的等待列表记录", expiredList.size());
    }

    // 私有辅助方法

    private String determineConflictType(List<Order> conflictingOrders) {
        if (conflictingOrders.size() > 1) {
            return "DOUBLE_BOOKING";
        }
        return "TIME_OVERLAP";
    }

    private List<Room> findAlternativeRooms(Room originalRoom, DetectConflictRequest request) {
        // 查找同类型的其他可用房间
        return roomRepository.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getRoomType, originalRoom.getRoomType())
                .eq(Room::getStatus, "AVAILABLE")
                .ne(Room::getId, originalRoom.getId())
                .last("LIMIT 3"));
    }

    private Integer calculateWaitingPriority(Long userId) {
        User user = userRepository.selectById(userId);
        if (user != null && "VIP".equals(user.getUserType())) {
            return 100;
        }
        return 50;
    }

    private Long estimateWaitTime(Long roomId) {
        int waitingCount = waitingListRepository.countByRoomId(roomId, "WAITING");
        return waitingCount * 2L; // 估算每人2天等待时间
    }

    private boolean isTimeSlotAvailable(Long roomId, LocalDateTime checkInDate, LocalDateTime checkOutDate) {
        List<Order> conflicts = orderRepository.findConflictingOrders(
                roomId,
                checkInDate.toLocalDate(),
                checkOutDate.toLocalDate(),
                "CONFIRMED"
        );
        return conflicts.isEmpty();
    }

    private int getResolvedConflictCount(LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<BookingConflict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookingConflict::getStatus, "RESOLVED");
        wrapper.between(BookingConflict::getCreatedAt, startDate, endDate);
        return Math.toIntExact(bookingConflictRepository.selectCount(wrapper));
    }

    private int getTotalWaitingListSize(Long roomId) {
        if (roomId != null) {
            return waitingListRepository.countByRoomId(roomId, "WAITING");
        }

        LambdaQueryWrapper<WaitingList> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaitingList::getStatus, "WAITING");
        return Math.toIntExact(waitingListRepository.selectCount(wrapper));
    }

    private WaitingListResponse convertToWaitingListResponse(WaitingList waiting) {
        return WaitingListResponse.builder()
                .waitingListId(waiting.getId())
                .roomId(waiting.getRoomId())
                .status(waiting.getStatus())
                .priority(waiting.getPriority())
                .requestedCheckInDate(waiting.getRequestedCheckInDate())
                .requestedCheckOutDate(waiting.getRequestedCheckOutDate())
                .guestCount(waiting.getGuestCount())
                .createdAt(waiting.getCreatedAt())
                .build();
    }

    private OrderResponse convertToOrderResponse(Order order) {
        // 这里应该返回适当的OrderResponse对象
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .build();
    }

    private String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis();
    }

    // 事件类
    public static class RoomAvailableEvent {
        private final Long roomId;
        private final Long userId;
        private final Long waitingListId;

        public RoomAvailableEvent(Long roomId, Long userId, Long waitingListId) {
            this.roomId = roomId;
            this.userId = userId;
            this.waitingListId = waitingListId;
        }

        public Long getRoomId() { return roomId; }
        public Long getUserId() { return userId; }
        public Long getWaitingListId() { return waitingListId; }
    }
}