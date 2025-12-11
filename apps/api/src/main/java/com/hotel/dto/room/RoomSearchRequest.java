package com.hotel.dto.room;

import lombok.Data;
import javax.validation.constraints.Min;
import java.math.BigDecimal;

/**
 * 房间搜索请求 DTO
 */
@Data
public class RoomSearchRequest {

    private Long hotelId;

    private Long roomTypeId;

    private String status;

    private String roomNumber;

    private Integer floor;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;

    @Min(value = 1, message = "每页大小不能小于1")
    private Integer size = 20;

    private String sortBy = "roomNumber";

    private String sortDir = "ASC";
}