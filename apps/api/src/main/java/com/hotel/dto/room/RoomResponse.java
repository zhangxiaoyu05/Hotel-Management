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
}