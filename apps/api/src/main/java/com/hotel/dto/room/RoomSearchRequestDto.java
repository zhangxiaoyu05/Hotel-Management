package com.hotel.dto.room;

import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 房间搜索请求 DTO - 用于用户搜索可用房间
 */
@Data
public class RoomSearchRequestDto {

    @NotNull(message = "入住日期不能为空")
    private LocalDate checkInDate;

    @NotNull(message = "退房日期不能为空")
    private LocalDate checkOutDate;

    @NotNull(message = "客人数量不能为空")
    @Min(value = 1, message = "客人数量至少为1")
    private Integer guestCount;

    private Long hotelId;

    private Long roomTypeId;

    @Min(value = 0, message = "最低价格不能小于0")
    private Double priceMin;

    @Min(value = 0, message = "最高价格不能小于0")
    private Double priceMax;

    private List<String> facilities;

    private String sortBy = "PRICE"; // PRICE, RATING, DISTANCE

    private String sortOrder = "ASC"; // ASC, DESC

    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;

    @Min(value = 1, message = "每页大小不能小于1")
    private Integer size = 20;

    @AssertTrue(message = "退房日期必须晚于入住日期")
    public boolean isCheckOutDateAfterCheckInDate() {
        if (checkInDate == null || checkOutDate == null) {
            return false;
        }
        return checkOutDate.isAfter(checkInDate);
    }

    @AssertTrue(message = "入住日期不能早于今天")
    public boolean isCheckInDateNotBeforeToday() {
        if (checkInDate == null) {
            return false;
        }
        return !checkInDate.isBefore(LocalDate.now());
    }
}