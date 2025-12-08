package com.hotel.dto.room;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 更新房间请求 DTO
 */
@Data
public class UpdateRoomRequest {

    @Size(max = 20, message = "房间号长度不能超过20个字符")
    private String roomNumber;

    @Positive(message = "房间类型ID必须为正数")
    private Long roomTypeId;

    @Min(value = 1, message = "楼层必须大于0")
    private Integer floor;

    @Min(value = 1, message = "面积必须大于0")
    private Integer area;

    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "价格格式不正确")
    private BigDecimal price;

    private String status;

    private List<String> images;
}