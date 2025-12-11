package com.hotel.dto.review.admin;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ReviewReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    private String content;

    @NotNull(message = "状态不能为空")
    private String status; // DRAFT, PUBLISHED
}