package com.hotel.dto.bookingConflict;

import lombok.Data;
import lombok.Builder;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
public class DetectConflictRequest {

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "入住日期不能为空")
    private LocalDateTime checkInDate;

    @NotNull(message = "退房日期不能为空")
    private LocalDateTime checkOutDate;
}