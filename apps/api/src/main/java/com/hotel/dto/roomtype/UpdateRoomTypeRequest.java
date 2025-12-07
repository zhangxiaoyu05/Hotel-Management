package com.hotel.dto.roomtype;

import com.hotel.enums.RoomTypeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 更新房间类型请求DTO
 */
@Data
@Schema(description = "更新房间类型请求")
public class UpdateRoomTypeRequest {

    @Schema(description = "房间类型名称", example = "豪华标准间")
    @Size(min = 2, max = 50, message = "房间类型名称长度必须在2-50个字符之间")
    private String name;

    @Schema(description = "房间容量", example = "3")
    @Min(value = 1, message = "房间容量至少为1人")
    @Max(value = 20, message = "房间容量不能超过20人")
    private Integer capacity;

    @Schema(description = "基础价格", example = "399.00")
    @DecimalMin(value = "0.0", inclusive = false, message = "基础价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "价格格式不正确")
    private BigDecimal basePrice;

    @Schema(description = "房间设施列表", example = "[\"WiFi\", \"空调\", \"电视\", \"冰箱\"]")
    private List<String> facilities;

    @Schema(description = "房间描述", example = "豪华装修的标准间，配备齐全设施")
    @Size(max = 500, message = "房间描述不能超过500个字符")
    private String description;

    @Schema(description = "房间类型图标URL", example = "https://example.com/luxury-icon.png")
    private String iconUrl;

    @Schema(description = "房间类型状态", example = "ACTIVE")
    private RoomTypeStatus status;
}