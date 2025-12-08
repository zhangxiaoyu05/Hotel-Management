package com.hotel.dto.room;

import lombok.Data;
import java.util.List;

/**
 * 房间列表响应 DTO
 */
@Data
public class RoomListResponse {

    private List<RoomResponse> content;

    private Long totalElements;

    private Integer totalPages;

    private Integer size;

    private Integer number;

    private Boolean first;

    private Boolean last;

    private Boolean empty;
}