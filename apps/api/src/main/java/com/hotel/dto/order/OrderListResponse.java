package com.hotel.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderListResponse {

    private Long id;
    private String orderNumber;
    private Long roomId;
    private String roomName;
    private String roomNumber;
    private String hotelName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;
}