package com.hotel.dto.admin.user;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 用户详情DTO
 */
@Data
public class UserDetailDTO {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private String nickname;

    private String avatar;

    private String realName;

    private String gender;

    private String birthDate;

    private String role;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    private Integer totalOrders;

    private Integer totalReviews;

    private Double totalSpent;

    private Double averageRating;

    private String memberLevel;
}