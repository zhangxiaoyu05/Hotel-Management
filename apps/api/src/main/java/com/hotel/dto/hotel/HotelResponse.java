package com.hotel.dto.hotel;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HotelResponse {

    private Long id;

    private String name;

    private String address;

    private String phone;

    private String description;

    private List<String> facilities;

    private List<String> images;

    private String status;

    private Long createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}