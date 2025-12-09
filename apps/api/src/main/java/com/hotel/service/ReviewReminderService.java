package com.hotel.service;

import com.hotel.dto.review.incentive.ReviewReminderSettingsDTO;
import com.hotel.entity.Order;
import com.hotel.entity.User;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.ReviewRepository;
import com.hotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewReminderService {

    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 每小时检查并发送评价提醒
     */
    @Scheduled(cron = "0 0 * * * ?") // 每小时执行
    @Transactional
    public void processReviewReminders() {
        log.info("开始处理评价提醒任务");

        LocalDateTime now = LocalDateTime.now();

        // 发送首次提醒（入住后24小时）
        sendFirstReminder(now);

        // 发送第二次提醒（入住后3天）
        sendSecondReminder(now);

        // 发送最后提醒（入住后7天）
        sendFinalReminder(now);

        log.info("评价提醒任务处理完成");
    }

    /**
     * 发送首次提醒
     */
    private void sendFirstReminder(LocalDateTime now) {
        LocalDateTime cutoffTime = now.minusHours(24);

        List<Order> ordersForFirstReminder = orderRepository.findCompletedOrdersWithoutReviewAfter(cutoffTime);

        for (Order order : ordersForFirstReminder) {
            try {
                User user = userRepository.selectById(order.getUserId());
                if (user != null && shouldSendReminder(user)) {
                    sendReviewReminder(user, order, "FIRST", "感谢您的入住！请分享您的入住体验");
                    log.info("发送首次评价提醒给用户 {}，订单 {}", user.getId(), order.getId());
                }
            } catch (Exception e) {
                log.error("发送首次提醒失败，订单：{}", order.getId(), e);
            }
        }
    }

    /**
     * 发送第二次提醒
     */
    private void sendSecondReminder(LocalDateTime now) {
        LocalDateTime cutoffTime = now.minusHours(72); // 3天前

        List<Order> ordersForSecondReminder = orderRepository.findCompletedOrdersWithoutReviewAfter(cutoffTime);

        for (Order order : ordersForSecondReminder) {
            try {
                User user = userRepository.selectById(order.getUserId());
                if (user != null && shouldSendReminder(user)) {
                    sendReviewReminder(user, order, "SECOND", "您的入住体验如何？请评价帮助我们改进服务");
                    log.info("发送第二次评价提醒给用户 {}，订单 {}", user.getId(), order.getId());
                }
            } catch (Exception e) {
                log.error("发送第二次提醒失败，订单：{}", order.getId(), e);
            }
        }
    }

    /**
     * 发送最后提醒
     */
    private void sendFinalReminder(LocalDateTime now) {
        LocalDateTime cutoffTime = now.minusHours(168); // 7天前

        List<Order> ordersForFinalReminder = orderRepository.findCompletedOrdersWithoutReviewAfter(cutoffTime);

        for (Order order : ordersForFinalReminder) {
            try {
                User user = userRepository.selectById(order.getUserId());
                if (user != null && shouldSendReminder(user)) {
                    sendReviewReminder(user, order, "FINAL", "最后提醒：分享您的入住体验可获得积分奖励");
                    log.info("发送最后评价提醒给用户 {}，订单 {}", user.getId(), order.getId());
                }
            } catch (Exception e) {
                log.error("发送最后提醒失败，订单：{}", order.getId(), e);
            }
        }
    }

    /**
     * 发送评价提醒
     */
    private void sendReviewReminder(User user, Order order, String reminderType, String message) {
        // 构建提醒内容
        String subject = "入住评价提醒";
        String content = buildReminderContent(user, order, message, reminderType);

        // 发送邮件提醒
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            notificationService.sendEmail(user.getEmail(), subject, content);
        }

        // 发送站内消息
        notificationService.sendInAppNotification(user.getId(), subject, content, "REVIEW_REMINDER");

        // 记录提醒日志
        log.info("评价提醒已发送 - 用户：{}，订单：{}，类型：{}", user.getId(), order.getId(), reminderType);
    }

    /**
     * 构建提醒内容
     */
    private String buildReminderContent(User user, Order order, String message, String reminderType) {
        StringBuilder content = new StringBuilder();
        content.append("尊敬的 ").append(user.getUsername()).append("，\n\n");
        content.append(message).append("。\n\n");
        content.append("订单信息：\n");
        content.append("- 订单号：").append(order.getId()).append("\n");
        content.append("- 酒店名称：").append(getHotelName(order.getHotelId())).append("\n");
        content.append("- 入住时间：").append(order.getCheckInDate()).append("\n");
        content.append("- 退房时间：").append(order.getCheckOutDate()).append("\n\n");

        if ("FIRST".equals(reminderType)) {
            content.append("提交评价将获得10积分奖励！");
        } else if ("SECOND".equals(reminderType)) {
            content.append("详细的评价将获得额外积分奖励！");
        } else if ("FINAL".equals(reminderType)) {
            content.append("这是最后提醒，评价奖励即将结束！");
        }

        content.append("\n\n点击以下链接进行评价：\n");
        content.append("[评价链接]"); // 实际应用中这里应该是具体的评价页面URL

        return content.toString();
    }

    /**
     * 检查是否应该发送提醒给用户
     */
    private boolean shouldSendReminder(User user) {
        // 这里可以根据用户偏好设置来判断
        // 简化实现：所有用户都接收提醒
        return true;
    }

    /**
     * 获取酒店名称（简化实现）
     */
    private String getHotelName(Long hotelId) {
        // 实际应该查询酒店表获取名称
        return "酒店ID-" + hotelId;
    }

    /**
     * 手动触发指定用户的评价提醒
     */
    @Transactional
    public void sendManualReminder(Long userId, Long orderId) {
        Order order = orderRepository.selectById(orderId);
        User user = userRepository.selectById(userId);

        if (order != null && user != null && order.getUserId().equals(userId)) {
            if (!reviewRepository.existsByOrderIdAndUserId(orderId, userId)) {
                sendReviewReminder(user, order, "MANUAL", "请分享您的入住体验");
                log.info("手动触发评价提醒 - 用户：{}，订单：{}", userId, orderId);
            } else {
                log.warn("订单 {} 已评价，跳过提醒", orderId);
            }
        } else {
            log.warn("订单或用户不存在，或用户不匹配");
        }
    }
}