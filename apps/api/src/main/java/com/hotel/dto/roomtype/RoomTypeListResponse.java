package com.hotel.dto.roomtype;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 房间类型列表响应DTO
 */
@Data
@Schema(description = "房间类型列表响应")
public class RoomTypeListResponse {

    @Schema(description = "房间类型列表")
    private List<RoomTypeResponse> content;

    @Schema(description = "总记录数", example = "100")
    private Long totalElements;

    @Schema(description = "总页数", example = "5")
    private Integer totalPages;

    @Schema(description = "每页大小", example = "20")
    private Integer size;

    @Schema(description = "当前页码（从0开始）", example = "0")
    private Integer number;

    @Schema(description = "是否为首页", example = "true")
    private Boolean first;

    @Schema(description = "是否为末页", example = "false")
    private Boolean last;

    @Schema(description = "是否有下一页", example = "true")
    private Boolean hasNext;

    @Schema(description = "是否有上一页", example = "false")
    private Boolean hasPrevious;
}