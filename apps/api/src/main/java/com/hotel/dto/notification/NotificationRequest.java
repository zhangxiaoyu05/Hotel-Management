package com.hotel.dto.notification;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class NotificationRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "通知标题不能为空")
    private String title;

    @NotBlank(message = "通知内容不能为空")
    private String content;

    @NotBlank(message = "通知类型不能为空")
    private String type;

    private String relatedEntityType;

    private Long relatedEntityId;
}