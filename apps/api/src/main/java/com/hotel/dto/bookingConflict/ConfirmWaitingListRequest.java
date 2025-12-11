package com.hotel.dto.bookingConflict;

import lombok.Data;
import lombok.Builder;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class ConfirmWaitingListRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    private String specialRequests;

    private Boolean notifyUser;
}