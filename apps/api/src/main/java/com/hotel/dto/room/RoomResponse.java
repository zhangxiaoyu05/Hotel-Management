package com.hotel.dto.room;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房间响应 DTO
 */
@Data
public class RoomResponse {

    private Long id;

    private Long hotelId;

    private Long roomTypeId;

    private String roomNumber;

    private Integer floor;

    private Integer area;

    private String status;

    private BigDecimal price;

    private List<String> images;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String roomTypeName;

    // 价格计算相关字段
    private BigDecimal calculatedPrice; // 根据价格策略计算后的价格

    private Boolean priceChanged = false; // 价格是否有变化

    private BigDecimal priceChange; // 价格变化金额

    private String priceChangePercentage; // 价格变化百分比

    // 特殊价格信息
    private Boolean hasSpecialPrice = false;

    private String specialPriceReason;
}