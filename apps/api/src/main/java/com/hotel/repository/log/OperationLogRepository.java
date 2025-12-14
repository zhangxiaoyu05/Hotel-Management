package com.hotel.repository.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.log.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问层
 */
@Mapper
public interface OperationLogRepository extends BaseMapper<OperationLog> {

    /**
     * 分页查询操作日志
     */
    IPage<OperationLog> searchLogs(Page<OperationLog> page,
                                   @Param("username") String username,
                                   @Param("operation") String operation,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("ip") String ip,
                                   @Param("status") String status,
                                   @Param("sortField") String sortField,
                                   @Param("sortDirection") String sortDirection);

    /**
     * 根据用户ID查询操作日志
     */
    List<OperationLog> findByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 根据时间范围删除操作日志（用于日志清理）
     */
    int deleteByTimeRange(@Param("endTime") LocalDateTime endTime);

    /**
     * 统计操作日志数量
     */
    long countByTimeRange(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);
}