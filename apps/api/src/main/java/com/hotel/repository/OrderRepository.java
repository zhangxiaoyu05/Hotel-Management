package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hotel.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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

    // ========== 仪表板统计查询方法 ==========

    /**
     * 统计指定日期的订单数量
     */
    default int countOrdersByDate(LocalDate date) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("DATE(created_at) = {0}", date);
        queryWrapper.in("status", Arrays.asList("CONFIRMED", "COMPLETED"));
        return selectCount(queryWrapper);
    }

    /**
     * 计算指定日期的收入
     */
    default BigDecimal calculateRevenueByDate(LocalDate date) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("DATE(created_at) = {0}", date);
        queryWrapper.eq("status", "COMPLETED");
        queryWrapper.select("IFNULL(SUM(total_price), 0) as total");

        List<Order> results = selectList(queryWrapper);
        return results.stream()
            .mapToDouble(order -> order.getTotalPrice() != null ? order.getTotalPrice().doubleValue() : 0.0)
            .sum()
            == 0.0 ? BigDecimal.ZERO :
            results.stream()
                .map(Order::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 统计指定日期范围的订单数量
     */
    default int countOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("created_at", startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        queryWrapper.in("status", Arrays.asList("CONFIRMED", "COMPLETED"));
        return selectCount(queryWrapper);
    }

    /**
     * 计算指定日期范围的收入
     */
    default BigDecimal calculateRevenueByDateRange(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("created_at", startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        queryWrapper.eq("status", "COMPLETED");

        List<Order> results = selectList(queryWrapper);
        return results.stream()
            .map(Order::getTotalPrice)
            .filter(price -> price != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取最近的订单列表
     */
    default List<Order> findRecentOrders(int limit) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_at");
        queryWrapper.last("LIMIT " + limit);
        return selectList(queryWrapper);
    }

    /**
     * 统计指定入住日期的订单数量
     */
    default int countOrdersByCheckInDate(LocalDate checkInDate) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("check_in_date", checkInDate);
        queryWrapper.in("status", Arrays.asList("CONFIRMED", "COMPLETED"));
        return selectCount(queryWrapper);
    }

    /**
     * 统计指定退房日期的订单数量
     */
    default int countOrdersByCheckOutDate(LocalDate checkOutDate) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("check_out_date", checkOutDate);
        queryWrapper.in("status", Arrays.asList("CONFIRMED", "COMPLETED"));
        return selectCount(queryWrapper);
    }

    /**
     * 查找指定日期范围内有冲突的订单
     */
    default List<Order> findConflictingOrders(Long roomId, LocalDate startDate, LocalDate endDate, List<String> statuses) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();

        if (roomId != null) {
            queryWrapper.eq("room_id", roomId);
        }

        if (statuses != null && !statuses.isEmpty()) {
            queryWrapper.in("status", statuses);
        }

        // 查找时间重叠的订单
        queryWrapper.and(wrapper ->
            wrapper.and(w -> w.le("check_in_date", startDate).gt("check_out_date", startDate))
            .or(w -> w.lt("check_in_date", endDate).ge("check_out_date", endDate))
            .or(w -> w.ge("check_in_date", startDate).le("check_out_date", endDate))
        );

        return selectList(queryWrapper);
    }

    /**
     * 计算入住率历史数据
     */
    default List<Double> calculateOccupancyHistory(LocalDate startDate, LocalDate endDate) {
        // 这里应该使用更复杂的SQL查询来计算每日入住率
        // 为了简化，暂时返回空列表，实际实现需要在XML中编写SQL
        return Arrays.asList();
    }

    /**
     * 计算收入趋势
     */
    default List<BigDecimal> calculateRevenueTrend(LocalDate startDate, LocalDate endDate) {
        // 这里应该使用SQL查询来计算每日收入趋势
        // 为了简化，暂时返回空列表，实际实现需要在XML中编写SQL
        return Arrays.asList();
    }
}