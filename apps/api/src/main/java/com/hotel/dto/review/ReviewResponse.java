package com.hotel.dto.review;

import lombok.Data;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.util.XssProtectionUtil;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewResponse {

    private Long id;
    private Long userId;
    private Long orderId;
    private Long roomId;
    private Long hotelId;
    private Integer overallRating;
    private Integer cleanlinessRating;
    private Integer serviceRating;
    private Integer facilitiesRating;
    private Integer locationRating;
    private String comment;
    private List<String> images;
    private Boolean isAnonymous;
    private String status;
    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(com.hotel.entity.Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUserId());
        response.setOrderId(review.getOrderId());
        response.setRoomId(review.getRoomId());
        response.setHotelId(review.getHotelId());
        response.setOverallRating(review.getOverallRating());
        response.setCleanlinessRating(review.getCleanlinessRating());
        response.setServiceRating(review.getServiceRating());
        response.setFacilitiesRating(review.getFacilitiesRating());
        response.setLocationRating(review.getLocationRating());
        response.setComment(XssProtectionUtil.escapeHtml(review.getComment()));
        response.setIsAnonymous(review.getIsAnonymous());
        response.setStatus(review.getStatus());
        response.setCreatedAt(review.getCreatedAt());

        // Parse images JSON string to list
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                if (review.getImages().startsWith("[")) {
                    // JSON array format
                    List<String> imageList = objectMapper.readValue(review.getImages(),
                        new TypeReference<List<String>>() {});
                    response.setImages(imageList);
                } else {
                    // Comma-separated format
                    String[] imageArray = review.getImages().split(",");
                    List<String> imageList = List.of(imageArray);
                    // Trim whitespace from each URL
                    imageList = imageList.stream()
                        .map(String::trim)
                        .filter(url -> !url.isEmpty())
                        .toList();
                    response.setImages(imageList);
                }
            } catch (Exception e) {
                // If parsing fails, log error and return empty list
                System.err.println("Failed to parse images JSON: " + e.getMessage());
                response.setImages(List.of());
            }
        } else {
            response.setImages(List.of());
        }

        return response;
    }
}