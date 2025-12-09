package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderRepository extends BaseMapper<Order> {

    List<Order> findConflictingOrders(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("status") String status
    );

    int countTodayOrders(@Param("date") String date);

    Order findByOrderNumber(@Param("orderNumber") String orderNumber);

    List<Order> findByUserId(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 查找已完成但未评价的订单（指定时间之后）
     */
    @Select("SELECT o.* FROM orders o " +
            "LEFT JOIN reviews r ON o.id = r.order_id " +
            "WHERE o.status = 'COMPLETED' AND r.id IS NULL " +
            "AND o.check_out_date >= #{cutoffDate} " +
            "ORDER BY o.check_out_date ASC")
    List<Order> findCompletedOrdersWithoutReviewAfter(@Param("cutoffDate") LocalDateTime cutoffDate);
}