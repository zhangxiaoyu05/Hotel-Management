package com.hotel.dto.facility;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FacilityCategoryResponse {

    private Long id;
    private Long hotelId;
    private String name;
    private String description;
    private String icon;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<FacilityResponse> facilities;
}