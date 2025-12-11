package com.hotel.dto.review.admin;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class BatchModerationRequest {

    @NotEmpty(message = "评价ID列表不能为空")
    @NotNull(message = "评价ID列表不能为空")
    private List<Long> reviewIds;

    @NotNull(message = "操作类型不能为空")
    private String action; // APPROVE, REJECT, HIDE

    @NotBlank(message = "操作原因不能为空")
    private String reason;
}