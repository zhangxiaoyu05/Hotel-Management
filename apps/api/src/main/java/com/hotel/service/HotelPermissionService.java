package com.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hotel.entity.Hotel;
import com.hotel.entity.Review;
import com.hotel.entity.User;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 酒店权限服务
 * 提供基于酒店的细粒度权限控制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelPermissionService {

    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    /**
     * 检查管理员是否有权限管理指定酒店
     */
    public boolean hasPermissionForHotel(Long adminId, Long hotelId) {
        try {
            // 超级管理员可以管理所有酒店
            if (isSuperAdmin(adminId)) {
                return true;
            }

            // 检查该酒店是否属于该管理员
            QueryWrapper<Hotel> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", hotelId).eq("admin_id", adminId);
            return hotelRepository.selectCount(queryWrapper) > 0;
        } catch (Exception e) {
            log.error("检查酒店权限失败", e);
            return false;
        }
    }

    /**
     * 检查管理员是否有权限管理指定评价
     */
    public boolean hasPermissionForReview(Long adminId, Long reviewId) {
        try {
            // 超级管理员可以管理所有评价
            if (isSuperAdmin(adminId)) {
                return true;
            }

            // 查询评价所属的酒店
            Review review = reviewRepository.selectById(reviewId);
            if (review == null) {
                return false;
            }

            return hasPermissionForHotel(adminId, review.getHotelId());
        } catch (Exception e) {
            log.error("检查评价权限失败", e);
            return false;
        }
    }

    /**
     * 获取管理员可以管理的酒店ID列表
     */
    public List<Long> getManagedHotelIds(Long adminId) {
        try {
            // 超级管理员可以管理所有酒店
            if (isSuperAdmin(adminId)) {
                QueryWrapper<Hotel> queryWrapper = new QueryWrapper<>();
                queryWrapper.select("id");
                return hotelRepository.selectList(queryWrapper).stream()
                    .map(Hotel::getId)
                    .collect(Collectors.toList());
            }

            QueryWrapper<Hotel> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("admin_id", adminId).select("id");
            return hotelRepository.selectList(queryWrapper).stream()
                .map(Hotel::getId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取管理员酒店列表失败", e);
            return List.of();
        }
    }

    /**
     * 检查是否为超级管理员
     */
    private boolean isSuperAdmin(Long adminId) {
        try {
            User user = userRepository.selectById(adminId);
            if (user == null) {
                return false;
            }

            // 检查用户角色是否包含SUPER_ADMIN
            String roles = user.getRoles();
            return roles != null && roles.contains("SUPER_ADMIN");
        } catch (Exception e) {
            log.error("检查超级管理员权限失败", e);
            return false;
        }
    }

    /**
     * 过滤出管理员有权限的评价ID列表
     */
    public List<Long> filterReviewIdsByPermission(Long adminId, List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return List.of();
        }

        // 超级管理员可以管理所有评价
        if (isSuperAdmin(adminId)) {
            return reviewIds;
        }

        try {
            // 获取管理员可以管理的酒店列表
            List<Long> managedHotelIds = getManagedHotelIds(adminId);
            if (managedHotelIds.isEmpty()) {
                return List.of();
            }

            // 查询这些酒店中的评价
            QueryWrapper<Review> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", reviewIds)
                       .in("hotel_id", managedHotelIds)
                       .select("id");

            return reviewRepository.selectList(queryWrapper).stream()
                .map(Review::getId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("过滤评价权限失败", e);
            return List.of();
        }
    }
}