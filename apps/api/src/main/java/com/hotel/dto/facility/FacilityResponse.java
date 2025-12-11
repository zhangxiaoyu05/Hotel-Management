package com.hotel.dto.facility;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FacilityResponse {

    private Long id;
    private Long hotelId;
    private Long categoryId;
    private String name;
    private String description;
    private String icon;
    private String status;
    private String statusDescription;
    private Boolean isFeatured;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private FacilityCategoryResponse category;
}