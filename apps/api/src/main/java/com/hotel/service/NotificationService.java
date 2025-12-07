package com.hotel.service;

import com.hotel.dto.notification.NotificationRequest;
import com.hotel.dto.notification.NotificationResponse;
import com.hotel.entity.Notification;
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