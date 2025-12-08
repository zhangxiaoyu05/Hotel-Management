package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
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
}