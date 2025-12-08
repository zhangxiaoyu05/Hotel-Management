package com.hotel.dto.roomtype;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房间类型响应DTO
 */
@Data
@Schema(description = "房间类型响应")
public class RoomTypeResponse {

    @Schema(description = "房间类型ID", example = "1")
    private Long id;

    @Schema(description = "酒店ID", example = "1")
    private Long hotelId;

    @Schema(description = "酒店名称", example = "示例酒店")
    private String hotelName;

    @Schema(description = "房间类型名称", example = "标准间")
    private String name;

    @Schema(description = "房间容量", example = "2")
    private Integer capacity;

    @Schema(description = "基础价格", example = "299.00")
    private BigDecimal basePrice;

    @Schema(description = "房间设施列表", example = "[\"WiFi\", \"空调\", \"电视\"]")
    private List<String> facilities;

    @Schema(description = "房间描述", example = "舒适的标准间，配备基本设施")
    private String description;

    @Schema(description = "房间类型图标URL", example = "https://example.com/icon.png")
    private String iconUrl;

    @Schema(description = "房间类型状态", example = "ACTIVE")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}