package com.hotel.service;

import com.hotel.dto.notification.NotificationRequest;
import com.hotel.dto.notification.NotificationResponse;
import com.hotel.entity.Notification;
import com.hotel.entity.WaitingList;
import com.hotel.repository.NotificationRepository;
import com.hotel.entity.User;
import com.hotel.repository.UserRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public void createNotification(Long userId, String title, String content,
                                  String type, String relatedEntityType, Long relatedEntityId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);

        notificationRepository.insert(notification);

        log.info("Created notification for user {}: {}", userId, title);
    }

    public void sendBookingConfirmation(Long userId, String orderNumber, String hotelName,
                                       String roomName, String checkInDate, String checkOutDate) {
        String title = "预订确认通知";
        String content = String.format(
            "您的订单 %s 已确认预订成功！\n酒店：%s\n房间：%s\n入住日期：%s\n退房日期：%s",
            orderNumber, hotelName, roomName, checkInDate, checkOutDate
        );

        createNotification(userId, title, content, "SUCCESS", "ORDER", null);

        // 异步发送邮件通知
        sendBookingConfirmationEmailAsync(userId, orderNumber, hotelName,
                                       roomName, checkInDate, checkOutDate);
    }

    public void sendBookingCancellation(Long userId, String orderNumber, String reason) {
        String title = "预订取消通知";
        String content = String.format(
            "您的订单 %s 已取消。%s",
            orderNumber, reason != null ? "取消原因：" + reason : ""
        );

        createNotification(userId, title, content, "WARNING", "ORDER", null);

        // 异步发送邮件通知
        sendBookingCancellationEmailAsync(userId, orderNumber, reason);
    }

    public void sendBookingReminder(Long userId, String orderNumber, String hotelName,
                                  String checkInDate) {
        String title = "入住提醒";
        String content = String.format(
            "温馨提醒：您的订单 %s 将于明天入住。\n酒店：%s\n入住日期：%s\n请准时办理入住手续。",
            orderNumber, hotelName, checkInDate
        );

        createNotification(userId, title, content, "INFO", "ORDER", null);

        // 异步发送邮件提醒
        sendBookingReminderEmailAsync(userId, orderNumber, hotelName, checkInDate);
    }

    public List<NotificationResponse> getUserNotifications(Long userId, Integer page, Integer limit) {
        int offset = (page - 1) * limit;

        List<Notification> notifications = notificationRepository.findByUserId(
            userId, offset, limit);

        int totalCount = notificationRepository.countByUserId(userId);
        int unreadCount = notificationRepository.countUnreadByUserId(userId);

        return notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.selectById(notificationId);

        if (notification == null || !notification.getUserId().equals(userId)) {
            return false;
        }

        notification.setIsRead(true);
        notification.setUpdatedAt(LocalDateTime.now());

        int result = notificationRepository.updateById(notification);
        return result > 0;
    }

    @Transactional
    public boolean markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(userId);

        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.updateById(notification);
        }

        return true;
    }

    @Transactional
    public boolean deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.selectById(notificationId);

        if (notification == null || !notification.getUserId().equals(userId)) {
            return false;
        }

        int result = notificationRepository.deleteById(notificationId);
        return result > 0;
    }

    @Async
    public void sendBookingConfirmationEmailAsync(Long userId, String orderNumber, String hotelName,
                                                 String roomName, String checkInDate, String checkOutDate) {
        User user = userRepository.selectById(userId);
        if (user != null && user.getEmail() != null) {
            sendBookingConfirmationEmail(user.getEmail(), orderNumber, hotelName,
                                       roomName, checkInDate, checkOutDate);
        } else {
            log.warn("User {} not found or has no email address", userId);
        }
    }

    @Async
    public void sendBookingCancellationEmailAsync(Long userId, String orderNumber, String reason) {
        User user = userRepository.selectById(userId);
        if (user != null && user.getEmail() != null) {
            sendBookingCancellationEmail(user.getEmail(), orderNumber, reason);
        } else {
            log.warn("User {} not found or has no email address", userId);
        }
    }

    @Async
    public void sendBookingReminderEmailAsync(Long userId, String orderNumber, String hotelName,
                                             String checkInDate) {
        User user = userRepository.selectById(userId);
        if (user != null && user.getEmail() != null) {
            sendBookingReminderEmail(user.getEmail(), orderNumber, hotelName, checkInDate);
        } else {
            log.warn("User {} not found or has no email address", userId);
        }
    }

    private void sendBookingConfirmationEmail(String toEmail, String orderNumber, String hotelName,
                                           String roomName, String checkInDate, String checkOutDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("预订确认 - " + orderNumber);
            helper.setFrom("noreply@hotel.com");

            String htmlContent = buildBookingConfirmationEmail(
                orderNumber, hotelName, roomName, checkInDate, checkOutDate);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent booking confirmation email to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email to: " + toEmail, e);
        }
    }

    private void sendBookingCancellationEmail(String toEmail, String orderNumber, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("预订取消 - " + orderNumber);
            helper.setFrom("noreply@hotel.com");

            String htmlContent = buildBookingCancellationEmail(orderNumber, reason);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent booking cancellation email to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send booking cancellation email to: " + toEmail, e);
        }
    }

    private void sendBookingReminderEmail(String toEmail, String orderNumber, String hotelName,
                                       String checkInDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("入住提醒 - " + orderNumber);
            helper.setFrom("noreply@hotel.com");

            String htmlContent = buildBookingReminderEmail(orderNumber, hotelName, checkInDate);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent booking reminder email to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send booking reminder email to: " + toEmail, e);
        }
    }

    private String buildBookingConfirmationEmail(String orderNumber, String hotelName,
                                               String roomName, String checkInDate, String checkOutDate) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>预订确认</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .success-icon { font-size: 48px; color: #4caf50; margin-bottom: 20px; }
                    .order-number { font-size: 24px; font-weight: bold; color: #1976d2; margin-bottom: 10px; }
                    .details { background-color: #f8f9fa; padding: 20px; border-radius: 6px; margin: 20px 0; }
                    .detail-item { margin-bottom: 10px; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="success-icon">✅</div>
                        <h1>预订成功！</h1>
                        <div class="order-number">订单号：%s</div>
                        <p>您的房间已成功预订，请保存好订单信息</p>
                    </div>

                    <div class="details">
                        <div class="detail-item"><strong>酒店：</strong>%s</div>
                        <div class="detail-item"><strong>房间：</strong>%s</div>
                        <div class="detail-item"><strong>入住日期：</strong>%s</div>
                        <div class="detail-item"><strong>退房日期：</strong>%s</div>
                    </div>

                    <div class="footer">
                        <p>请凭此订单号在酒店前台办理入住手续</p>
                        <p>如有疑问，请联系酒店客服</p>
                    </div>
                </div>
            </body>
            </html>
            """, orderNumber, hotelName, roomName, checkInDate, checkOutDate);
    }

    private String buildBookingCancellationEmail(String orderNumber, String reason) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>预订取消</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .cancel-icon { font-size: 48px; color: #f44336; margin-bottom: 20px; }
                    .order-number { font-size: 24px; font-weight: bold; color: #666; margin-bottom: 10px; }
                    .reason { background-color: #fff3e0; padding: 15px; border-radius: 6px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="cancel-icon">❌</div>
                        <h1>预订已取消</h1>
                        <div class="order-number">订单号：%s</div>
                        <p>您的预订已成功取消</p>
                    </div>

                    %s

                    <div class="footer">
                        <p>如有疑问，请联系酒店客服</p>
                    </div>
                </div>
            </body>
            </html>
            """, orderNumber,
            reason != null ?
                String.format("<div class=\"reason\"><strong>取消原因：</strong>%s</div>", reason) :
                "");
    }

    private String buildBookingReminderEmail(String orderNumber, String hotelName, String checkInDate) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>入住提醒</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .reminder-icon { font-size: 48px; color: #ff9800; margin-bottom: 20px; }
                    .order-number { font-size: 24px; font-weight: bold; color: #1976d2; margin-bottom: 10px; }
                    .details { background-color: #e3f2fd; padding: 20px; border-radius: 6px; margin: 20px 0; }
                    .detail-item { margin-bottom: 10px; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="reminder-icon">⏰</div>
                        <h1>入住提醒</h1>
                        <div class="order-number">订单号：%s</div>
                        <p>温馨提醒：您将于明天入住</p>
                    </div>

                    <div class="details">
                        <div class="detail-item"><strong>酒店：</strong>%s</div>
                        <div class="detail-item"><strong>入住日期：</strong>%s</div>
                        <div class="detail-item"><strong>入住时间：</strong>下午2:00后</div>
                        <div class="detail-item"><strong>退房时间：</strong>中午12:00前</div>
                    </div>

                    <div class="footer">
                        <p>请携带有效身份证件办理入住手续</p>
                        <p>如有疑问，请联系酒店客服</p>
                    </div>
                </div>
            </body>
            </html>
            """, orderNumber, hotelName, checkInDate);
    }

    public int countByUserId(Long userId) {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId);
        return Math.toIntExact(notificationRepository.selectCount(queryWrapper));
    }

    public int countUnreadByUserId(Long userId) {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                   .eq(Notification::getIsRead, false);
        return Math.toIntExact(notificationRepository.selectCount(queryWrapper));
    }

    // 冲突和等待列表通知方法

    @Async
    @Transactional
    public void sendWaitingListConfirmation(WaitingList waitingList) {
        String title = "等待列表确认";
        String content = String.format(
            "您已成功加入房间 %d 的等待列表。\n" +
            "入住日期：%s\n" +
            "退房日期：%s\n" +
            "客人数量：%d\n" +
            "您的等待优先级：%d\n\n" +
            "当房间可用时，我们会第一时间通知您。",
            waitingList.getRoomId(),
            waitingList.getRequestedCheckInDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getRequestedCheckOutDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getGuestCount(),
            waitingList.getPriority()
        );

        createNotification(
            waitingList.getUserId(),
            title,
            content,
            "WAITING_LIST",
            "WAITING_LIST",
            waitingList.getId()
        );

        sendWaitingListConfirmationEmailAsync(waitingList);
    }

    @Async
    @Transactional
    public boolean sendRoomAvailableNotification(WaitingList waitingList) {
        String title = "房间可用通知";
        String content = String.format(
            "好消息！您等待的房间现在可用。\n" +
            "房间ID：%d\n" +
            "入住日期：%s\n" +
            "退房日期：%s\n" +
            "客人数量：%d\n\n" +
            "请在24小时内确认预订，否则等待资格将失效。\n" +
            "点击立即预订确认您的房间。",
            waitingList.getRoomId(),
            waitingList.getRequestedCheckInDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getRequestedCheckOutDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getGuestCount()
        );

        createNotification(
            waitingList.getUserId(),
            title,
            content,
            "ROOM_AVAILABLE",
            "WAITING_LIST",
            waitingList.getId()
        );

        sendRoomAvailableEmailAsync(waitingList);
        return true;
    }

    @Async
    @Transactional
    public void sendWaitingListExpiredNotification(WaitingList waitingList) {
        String title = "等待列表已过期";
        String content = String.format(
            "很抱歉，您等待的房间预订资格已过期。\n" +
            "房间ID：%d\n" +
            "原入住日期：%s\n" +
            "原退房日期：%s\n\n" +
            "如需预订，请重新查询房间可用性并提交新的预订请求。",
            waitingList.getRoomId(),
            waitingList.getRequestedCheckInDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getRequestedCheckOutDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        createNotification(
            waitingList.getUserId(),
            title,
            content,
            "WAITING_LIST_EXPIRED",
            "WAITING_LIST",
            waitingList.getId()
        );

        sendWaitingListExpiredEmailAsync(waitingList);
    }

    @Async
    @Transactional
    public void sendBookingConflictNotification(Long userId, Long roomId, String conflictType) {
        String title = "预订冲突提醒";
        String content = String.format(
            "您尝试预订的房间 %d 遇到了冲突。\n" +
            "冲突类型：%s\n\n" +
            "系统已为您检测替代房间或提供等待列表选项。\n" +
            "请查看详情并选择合适的时间段。",
            roomId,
            conflictType
        );

        createNotification(
            userId,
            title,
            content,
            "BOOKING_CONFLICT",
            "ROOM",
            roomId
        );

        sendBookingConflictEmailAsync(userId, roomId, conflictType);
    }

    // 邮件发送方法

    @Async
    public void sendWaitingListConfirmationEmailAsync(WaitingList waitingList) {
        User user = userRepository.selectById(waitingList.getUserId());
        if (user != null && user.getEmail() != null) {
            sendWaitingListConfirmationEmail(user.getEmail(), waitingList);
        } else {
            log.warn("User {} not found or has no email address", waitingList.getUserId());
        }
    }

    @Async
    public void sendRoomAvailableEmailAsync(WaitingList waitingList) {
        User user = userRepository.selectById(waitingList.getUserId());
        if (user != null && user.getEmail() != null) {
            sendRoomAvailableEmail(user.getEmail(), waitingList);
        } else {
            log.warn("User {} not found or has no email address", waitingList.getUserId());
        }
    }

    @Async
    public void sendWaitingListExpiredEmailAsync(WaitingList waitingList) {
        User user = userRepository.selectById(waitingList.getUserId());
        if (user != null && user.getEmail() != null) {
            sendWaitingListExpiredEmail(user.getEmail(), waitingList);
        } else {
            log.warn("User {} not found or has no email address", waitingList.getUserId());
        }
    }

    @Async
    public void sendBookingConflictEmailAsync(Long userId, Long roomId, String conflictType) {
        User user = userRepository.selectById(userId);
        if (user != null && user.getEmail() != null) {
            sendBookingConflictEmail(user.getEmail(), roomId, conflictType);
        } else {
            log.warn("User {} not found or has no email address", userId);
        }
    }

    private void sendWaitingListConfirmationEmail(String toEmail, WaitingList waitingList) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("等待列表确认通知");
            helper.setFrom("noreply@hotel.com");

            String htmlContent = buildWaitingListConfirmationEmail(waitingList);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent waiting list confirmation email to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send waiting list confirmation email to: " + toEmail, e);
        }
    }

    private void sendRoomAvailableEmail(String toEmail, WaitingList waitingList) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("房间可用通知 - 立即预订");
            helper.setFrom("noreply@hotel.com");

            String htmlContent = buildRoomAvailableEmail(waitingList);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent room available email to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send room available email to: " + toEmail, e);
        }
    }

    private void sendWaitingListExpiredEmail(String toEmail, WaitingList waitingList) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("等待列表已过期");
            helper.setFrom("noreply@hotel.com");

            String htmlContent = buildWaitingListExpiredEmail(waitingList);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent waiting list expired email to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send waiting list expired email to: " + toEmail, e);
        }
    }

    private void sendBookingConflictEmail(String toEmail, Long roomId, String conflictType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("预订冲突提醒");
            helper.setFrom("noreply@hotel.com");

            String htmlContent = buildBookingConflictEmail(roomId, conflictType);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent booking conflict email to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send booking conflict email to: " + toEmail, e);
        }
    }

    private String buildWaitingListConfirmationEmail(WaitingList waitingList) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>等待列表确认</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .waiting-icon { font-size: 48px; color: #ff9800; margin-bottom: 20px; }
                    .priority { font-size: 24px; font-weight: bold; color: #1976d2; margin-bottom: 10px; }
                    .details { background-color: #e3f2fd; padding: 20px; border-radius: 6px; margin: 20px 0; }
                    .detail-item { margin-bottom: 10px; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="waiting-icon">⏱️</div>
                        <h1>等待列表确认</h1>
                        <div class="priority">优先级：%d</div>
                        <p>您已成功加入等待列表</p>
                    </div>

                    <div class="details">
                        <div class="detail-item"><strong>房间ID：</strong>%d</div>
                        <div class="detail-item"><strong>入住日期：</strong>%s</div>
                        <div class="detail-item"><strong>退房日期：</strong>%s</div>
                        <div class="detail-item"><strong>客人数量：</strong>%d</div>
                        <div class="detail-item"><strong>预计等待时间：</strong>约%d天</div>
                    </div>

                    <div class="footer">
                        <p>房间可用时我们将第一时间通知您</p>
                        <p>请保持手机畅通，及时查看邮件</p>
                    </div>
                </div>
            </body>
            </html>
            """, waitingList.getPriority(), waitingList.getRoomId(),
            waitingList.getRequestedCheckInDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getRequestedCheckOutDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getGuestCount(), (waitingList.getPriority() / 50) * 2);
    }

    private String buildRoomAvailableEmail(WaitingList waitingList) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>房间可用通知</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .available-icon { font-size: 48px; color: #4caf50; margin-bottom: 20px; }
                    .alert { background-color: #e8f5e8; padding: 15px; border-radius: 6px; margin: 20px 0; border-left: 4px solid #4caf50; }
                    .details { background-color: #f8f9fa; padding: 20px; border-radius: 6px; margin: 20px 0; }
                    .detail-item { margin-bottom: 10px; }
                    .deadline { color: #f44336; font-weight: bold; }
                    .cta-button { display: inline-block; padding: 15px 30px; background-color: #1976d2; color: white; text-decoration: none; border-radius: 6px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="available-icon">🎉</div>
                        <h1>房间可用啦！</h1>
                        <p>您等待的房间现在可以预订了</p>
                    </div>

                    <div class="alert">
                        <strong>重要提醒：</strong>请在<span class="deadline">24小时内</span>确认预订，否则资格将失效
                    </div>

                    <div class="details">
                        <div class="detail-item"><strong>房间ID：</strong>%d</div>
                        <div class="detail-item"><strong>入住日期：</strong>%s</div>
                        <div class="detail-item"><strong>退房日期：</strong>%s</div>
                        <div class="detail-item"><strong>客人数量：</strong>%d</div>
                    </div>

                    <div style="text-align: center;">
                        <a href="#" class="cta-button">立即确认预订</a>
                    </div>

                    <div class="footer">
                        <p>确认预订后，您将收到预订确认邮件</p>
                        <p>如有疑问，请联系酒店客服</p>
                    </div>
                </div>
            </body>
            </html>
            """, waitingList.getRoomId(),
            waitingList.getRequestedCheckInDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getRequestedCheckOutDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getGuestCount());
    }

    private String buildWaitingListExpiredEmail(WaitingList waitingList) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>等待列表已过期</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .expired-icon { font-size: 48px; color: #f44336; margin-bottom: 20px; }
                    .reason { background-color: #ffebee; padding: 15px; border-radius: 6px; margin: 20px 0; }
                    .details { background-color: #f8f9fa; padding: 20px; border-radius: 6px; margin: 20px 0; }
                    .detail-item { margin-bottom: 10px; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="expired-icon">⏰</div>
                        <h1>等待列表已过期</h1>
                        <p>很抱歉，您的等待资格已失效</p>
                    </div>

                    <div class="reason">
                        您在收到房间可用通知后24小时内未确认预订，等待资格已自动过期。
                    </div>

                    <div class="details">
                        <div class="detail-item"><strong>房间ID：</strong>%d</div>
                        <div class="detail-item"><strong>原入住日期：</strong>%s</div>
                        <div class="detail-item"><strong>原退房日期：</strong>%s</div>
                        <div class="detail-item"><strong>客人数量：</strong>%d</div>
                    </div>

                    <div class="footer">
                        <p>如需预订，请重新查询房间可用性</p>
                        <p>如有疑问，请联系酒店客服</p>
                    </div>
                </div>
            </body>
            </html>
            """, waitingList.getRoomId(),
            waitingList.getRequestedCheckInDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getRequestedCheckOutDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            waitingList.getGuestCount());
    }

    private String buildBookingConflictEmail(Long roomId, String conflictType) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>预订冲突提醒</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .conflict-icon { font-size: 48px; color: #ff9800; margin-bottom: 20px; }
                    .conflict-type { background-color: #fff3e0; padding: 15px; border-radius: 6px; margin: 20px 0; }
                    .suggestions { background-color: #e8f5e8; padding: 20px; border-radius: 6px; margin: 20px 0; }
                    .suggestion-item { margin-bottom: 8px; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="conflict-icon">⚠️</div>
                        <h1>预订冲突提醒</h1>
                        <p>您尝试预订的房间遇到时间冲突</p>
                    </div>

                    <div class="conflict-type">
                        <strong>冲突类型：</strong>%s<br>
                        <strong>房间ID：</strong>%d
                    </div>

                    <div class="suggestions">
                        <h3>建议解决方案：</h3>
                        <div class="suggestion-item">1. 选择其他时间段重新预订</div>
                        <div class="suggestion-item">2. 查看系统推荐的替代房间</div>
                        <div class="suggestion-item">3. 加入等待列表，房间可用时通知您</div>
                    </div>

                    <div class="footer">
                        <p>请查看预订页面获取详细信息和解决方案</p>
                        <p>如有疑问，请联系酒店客服</p>
                    </div>
                </div>
            </body>
            </html>
            """, conflictType, roomId);
    }

    private NotificationResponse convertToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setUserId(notification.getUserId());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setType(notification.getType());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        response.setRelatedEntityType(notification.getRelatedEntityType());
        response.setRelatedEntityId(notification.getRelatedEntityId());
        return response;
    }
}