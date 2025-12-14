package com.hotel.dto.settings;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class NotificationSettingsDTO {

    private Long id;

    private Boolean enableEmailNotifications;

    @Email(message = "发件人邮箱格式不正确")
    private String emailFromAddress;

    @Size(max = 100, message = "发件人名称长度不能超过100个字符")
    private String emailFromName;

    private String smtpHost;

    private Integer smtpPort;

    private Boolean smtpSslEnabled;

    private String smtpUsername;

    private Boolean enableSmsNotifications;

    private String smsProvider;

    private String smsApiKey;

    private String smsApiSecret;

    private String smsSignName;

    private Map<String, String> emailTemplates;

    private Map<String, String> smsTemplates;

    private Boolean enableBookingNotifications;

    private Boolean enableCancelNotifications;

    private Boolean enablePaymentNotifications;

    private Boolean enableCheckInNotifications;

    private Boolean enableCheckOutNotifications;

    private LocalDateTime updatedAt;

    private String updatedBy;

    @Data
    public static class EmailTemplate {
        private String bookingConfirmation;
        private String bookingCancellation;
        private String paymentConfirmation;
        private String checkInReminder;
        private String checkOutReminder;
    }

    @Data
    public static class SmsTemplate {
        private String bookingConfirmation;
        private String bookingCancellation;
        private String paymentConfirmation;
        private String checkInReminder;
        private String checkOutReminder;
    }
}