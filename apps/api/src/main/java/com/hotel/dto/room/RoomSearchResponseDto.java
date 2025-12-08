package com.hotel.dto.room;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房间搜索响应 DTO
 */
@Data
public class RoomSearchResponseDto {

    private Long id;

    private String roomNumber;

    private Integer floor;

    private BigDecimal area;

    private String status;

    private BigDecimal price;

    private List<String> images;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 房间类型信息
    private Long roomTypeId;

    private String roomTypeName;

    private Integer roomTypeCapacity;

    private String roomTypeDescription;

    private List<String> roomTypeFacilities;

    // 酒店信息
    private Long hotelId;

    private String hotelName;

    private String hotelAddress;

    private String hotelPhone;

    private String hotelDescription;

    private List<String> hotelFacilities;

    private List<String> hotelImages;

    private BigDecimal hotelRating;

    private Double distance; // 距离（如果有用户位置）

    // 价格信息（动态计算）
    private BigDecimal totalPrice;

    private BigDecimal averagePricePerNight;

    private Boolean hasSpecialPrice;

    private String specialPriceDescription;

    // 可用性信息
    private Boolean isAvailable;

    private Integer availableRooms;

    private String availabilityStatus;
}