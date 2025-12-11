package com.hotel.dto.roomtype;

import com.hotel.validation.RoomTypeName;
import com.hotel.validation.ValidPriceRange;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创建房间类型请求DTO
 */
@Data
@Schema(description = "创建房间类型请求")
public class CreateRoomTypeRequest {

    @Schema(description = "酒店ID", example = "1")
    @NotNull(message = "酒店ID不能为空")
    private Long hotelId;

    @Schema(description = "房间类型名称", example = "标准间")
    @NotBlank(message = "房间类型名称不能为空")
    @RoomTypeName
    private String name;

    @Schema(description = "房间容量", example = "2")
    @NotNull(message = "房间容量不能为空")
    @Min(value = 1, message = "房间容量至少为1人")
    @Max(value = 20, message = "房间容量不能超过20人")
    private Integer capacity;

    @Schema(description = "基础价格", example = "299.00")
    @NotNull(message = "基础价格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "基础价格必须大于0")
    @ValidPriceRange
    private BigDecimal basePrice;

    @Schema(description = "房间设施列表", example = "[\"WiFi\", \"空调\", \"电视\"]")
    private List<String> facilities;

    @Schema(description = "房间描述", example = "舒适的标准间，配备基本设施")
    @Size(max = 500, message = "房间描述不能超过500个字符")
    private String description;

    @Schema(description = "房间类型图标URL", example = "https://example.com/icon.png")
    private String iconUrl;
}