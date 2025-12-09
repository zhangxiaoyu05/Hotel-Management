package com.hotel.dto.review.incentive;

import lombok.Data;

@Data
public class ReviewReminderSettingsDTO {
    private Boolean emailEnabled = true;
    private Boolean smsEnabled = false;
    private Boolean pushEnabled = true;
    private Integer reminderHoursAfterCheckout = 24;
    private Boolean enableSecondReminder = true;
    private Boolean enableFinalReminder = true;
    private String preferredReminderTime = "10:00";
}