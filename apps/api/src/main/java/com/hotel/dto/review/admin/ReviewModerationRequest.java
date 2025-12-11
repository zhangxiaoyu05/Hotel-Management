package com.hotel.dto.review.admin;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ReviewModerationRequest {

    @NotNull(message = "操作类型不能为空")
    private String action; // APPROVE, REJECT, MARK, HIDE, DELETE

    @NotBlank(message = "操作原因不能为空")
    private String reason;
}