package com.hotel.repository.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.log.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志数据访问层
 */
@Mapper
public interface LoginLogRepository extends BaseMapper<LoginLog> {

    /**
     * 分页查询登录日志
     */
    IPage<LoginLog> searchLogs(Page<LoginLog> page,
                              @Param("username") String username,
                              @Param("loginType") String loginType,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime,
                              @Param("ip") String ip,
                              @Param("status") String status,
                              @Param("sortField") String sortField,
                              @Param("sortDirection") String sortDirection);

    /**
     * 根据用户名查询登录日志
     */
    List<LoginLog> findByUsername(@Param("username") String username, @Param("limit") Integer limit);

    /**
     * 查询最近的登录失败记录
     */
    List<LoginLog> findRecentFailedLogins(@Param("username") String username, @Param("minutes") Integer minutes);

    /**
     * 根据时间范围删除登录日志（用于日志清理）
     */
    int deleteByTimeRange(@Param("endTime") LocalDateTime endTime);

    /**
     * 统计登录日志数量
     */
    long countByTimeRange(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户登录次数
     */
    long countByUsernameAndTimeRange(@Param("username") String username,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);
}