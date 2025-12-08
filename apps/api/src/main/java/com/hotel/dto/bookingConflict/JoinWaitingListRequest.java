package com.hotel.dto.bookingConflict;

import lombok.Data;
import lombok.Builder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import java.time.LocalDateTime;

@Data
@Builder
public class JoinWaitingListRequest {

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "入住日期不能为空")
    private LocalDateTime checkInDate;

    @NotNull(message = "退房日期不能为空")
    private LocalDateTime checkOutDate;

    @NotNull(message = "客人数量不能为空")
    @Min(value = 1, message = "客人数量至少为1")
    private Integer guestCount;

    private String specialRequests;
}