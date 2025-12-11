package com.hotel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单摘要DTO
 * 用于简化订单信息展示
 */
@Data
@Schema(description = "订单摘要信息")
public class OrderSummaryDTO {

    @Schema(description = "订单ID", example = "1001")
    private Long id;

    @Schema(description = "订单号", example = "ORD-20251211-001")
    private String orderNumber;

    @Schema(description = "用户ID", example = "2001")
    private Long userId;

    @Schema(description = "用户名", example = "张三")
    private String username;

    @Schema(description = "房间ID", example = "3001")
    private Long roomId;

    @Schema(description = "房间号", example = "801")
    private String roomNumber;

    @Schema(description = "房型名称", example = "豪华大床房")
    private String roomTypeName;

    @Schema(description = "酒店ID", example = "1")
    private Long hotelId;

    @Schema(description = "酒店名称", example = "希尔顿大酒店")
    private String hotelName;

    @Schema(description = "入住日期", example = "2025-12-12")
    private String checkInDate;

    @Schema(description = "退房日期", example = "2025-12-14")
    private String checkOutDate;

    @Schema(description = "订单总金额", example = "1280.00")
    private BigDecimal totalPrice;

    @Schema(description = "订单状态", example = "CONFIRMED")
    private String status;

    @Schema(description = "订单状态描述", example = "已确认")
    private String statusDesc;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "支付方式", example = "ALIPAY")
    private String paymentMethod;

    @Schema(description = "支付状态", example = "PAID")
    private String paymentStatus;

    @Schema(description = "入住人数", example = "2")
    private Integer guestCount;

    @Schema(description = "联系人姓名", example = "李四")
    private String contactName;

    @Schema(description = "联系电话", example = "138****1234")
    private String contactPhone;
}