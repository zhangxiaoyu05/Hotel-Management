package com.hotel.dto.hotel;

import lombok.Data;

import java.util.List;

@Data
public class HotelListResponse {

    private List<HotelResponse> content;

    private Long totalElements;

    private Integer totalPages;

    private Integer size;

    private Integer number;

    private Boolean first;

    private Boolean last;

    private Integer numberOfElements;
}