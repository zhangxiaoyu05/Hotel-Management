package com.hotel.dto.review;

import lombok.Data;
import java.util.List;

@Data
public class ReviewListResponse {

    private List<ReviewResponse> reviews;

    private Long total;

    private Integer page;

    private Integer size;

    private Integer totalPages;

    public ReviewListResponse() {}

    public ReviewListResponse(List<ReviewResponse> reviews, Long total, Integer page, Integer size) {
        this.reviews = reviews;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = (int) Math.ceil((double) total / size);
    }
}