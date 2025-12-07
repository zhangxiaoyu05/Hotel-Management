package com.hotel.dto.order;

import com.hotel.entity.Order;
import com.hotel.entity.Room;
import com.hotel.entity.Hotel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long userId;
    private Long roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;
    private BigDecimal totalPrice;
    private String status;
    private String specialRequests;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Room room;
    private Hotel hotel;
    private PriceBreakdown priceBreakdown;

    @Data
    public static class PriceBreakdown {
        private BigDecimal roomFee;
        private BigDecimal serviceFee;
        private BigDecimal discountAmount;
        private BigDecimal totalPrice;
        private Integer nights;
    }
}