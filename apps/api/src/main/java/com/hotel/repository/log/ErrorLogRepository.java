package com.hotel.repository.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.log.ErrorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 错误日志数据访问层
 */
@Mapper
public interface ErrorLogRepository extends BaseMapper<ErrorLog> {

    /**
     * 分页查询错误日志
     */
    IPage<ErrorLog> searchLogs(Page<ErrorLog> page,
                              @Param("username") String username,
                              @Param("level") String level,
                              @Param("module") String module,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime,
                              @Param("ip") String ip,
                              @Param("sortField") String sortField,
                              @Param("sortDirection") String sortDirection);

    /**
     * 根据用户ID查询错误日志
     */
    List<ErrorLog> findByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 根据模块名称查询错误日志
     */
    List<ErrorLog> findByModule(@Param("module") String module, @Param("limit") Integer limit);

    /**
     * 统计错误日志按级别分组
     */
    List<Map<String, Object>> countByLevel(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计错误日志按模块分组
     */
    List<Map<String, Object>> countByModule(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据时间范围删除错误日志（用于日志清理）
     */
    int deleteByTimeRange(@Param("endTime") LocalDateTime endTime);

    /**
     * 统计错误日志数量
     */
    long countByTimeRange(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最近的错误记录
     */
    List<ErrorLog> findRecentErrors(@Param("minutes") Integer minutes, @Param("limit") Integer limit);
}