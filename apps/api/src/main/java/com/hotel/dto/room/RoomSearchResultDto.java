package com.hotel.dto.room;

import lombok.Data;
import java.util.List;

/**
 * 房间搜索结果 DTO
 */
@Data
public class RoomSearchResultDto {

    private List<RoomSearchResponseDto> rooms;

    private Long total;

    private Integer page;

    private Integer size;

    private Integer totalPages;

    private Boolean hasNext;

    private Boolean hasPrevious;

    public RoomSearchResultDto(List<RoomSearchResponseDto> rooms, Long total, Integer page, Integer size) {
        this.rooms = rooms;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = (int) Math.ceil((double) total / size);
        this.hasNext = page < totalPages - 1;
        this.hasPrevious = page > 0;
    }
}