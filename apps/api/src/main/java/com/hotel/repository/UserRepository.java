package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hotel.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserRepository extends BaseMapper<User> {

    /**
     * 检查用户是否具有指定角色
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE id = #{userId} AND roles LIKE CONCAT('%', #{role}, '%')")
    boolean hasRole(@Param("userId") Long userId, @Param("role") String role);

    /**
     * 根据用户名查找用户
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * 根据邮箱查找用户
     */
    @Select("SELECT * FROM users WHERE email = #{email} AND deleted = 0")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * 根据手机号查找用户
     */
    @Select("SELECT * FROM users WHERE phone = #{phone} AND deleted = 0")
    Optional<User> findByPhone(@Param("phone") String phone);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(1) FROM users WHERE username = #{username} AND deleted = 0")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     */
    @Select("SELECT COUNT(1) FROM users WHERE email = #{email} AND deleted = 0")
    boolean existsByEmail(@Param("email") String email);

    /**
     * 检查手机号是否存在
     */
    @Select("SELECT COUNT(1) FROM users WHERE phone = #{phone} AND deleted = 0")
    boolean existsByPhone(@Param("phone") String phone);

    /**
     * 根据用户名或邮箱或手机号查找用户（用于登录）
     */
    @Select("SELECT * FROM users WHERE (username = #{identifier} OR email = #{identifier} OR phone = #{identifier}) AND deleted = 0")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    /**
     * 根据用户ID和状态查找用户
     */
    @Select("SELECT * FROM users WHERE id = #{id} AND status = #{status} AND deleted = 0")
    Optional<User> findByIdAndStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 批量更新用户状态
     */
    @Update("<script>" +
            "UPDATE users SET status = #{status} WHERE id IN " +
            "<foreach collection='userIds' item='id' open='(' close=')' separator=','>" +
            "#{id}" +
            "</foreach>" +
            " AND deleted = 0" +
            "</script>")
    int updateStatusBatch(@Param("userIds") List<Long> userIds, @Param("status") String status);

    // ========== 仪表板统计查询方法 ==========

    /**
     * 统计指定日期的新用户数量
     */
    default int countUsersByDate(LocalDate date) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("DATE(created_at) = {0}", date);
        queryWrapper.eq("deleted", 0);
        return selectCount(queryWrapper);
    }

    /**
     * 统计近N天有订单的活跃用户数量
     */
    default int countActiveUsersByOrders(LocalDate sinceDate) {
        // 这里应该使用更复杂的SQL查询来统计有订单的活跃用户
        // 为了简化，暂时返回总用户数，实际实现需要在XML中编写SQL
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("created_at", sinceDate);
        queryWrapper.eq("deleted", 0);
        return selectCount(queryWrapper);
    }

    /**
     * 计算用户活跃度趋势
     */
    default List<Integer> calculateUserActivityTrend(LocalDate startDate, LocalDate endDate) {
        // 这里应该使用SQL查询来计算每日活跃用户数
        // 为了简化，暂时返回空列表，实际实现需要在XML中编写SQL
        return Arrays.asList();
    }
}