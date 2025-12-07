package com.hotel.dto.notification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String relatedEntityType;
    private Long relatedEntityId;
}