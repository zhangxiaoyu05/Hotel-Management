package com.hotel.dto.facility;

import lombok.Data;

@Data
public class FacilitySearchRequest {

    private Long hotelId;
    private Long categoryId;
    private String status;
    private Boolean isFeatured;
    private String keyword;
    private Integer page = 1;
    private Integer size = 20;
    private String sortBy = "displayOrder";
    private String sortDirection = "asc";
}