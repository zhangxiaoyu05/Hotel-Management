package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.RoomStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RoomStatusLogRepository extends BaseMapper<RoomStatusLog> {

    /**
     * 分页查询房间状态变更日志
     */
    IPage<RoomStatusLog> findByRoomIdWithPagination(Page<RoomStatusLog> page,
                                                   @Param("roomId") Long roomId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * 查询指定时间范围内的状态日志
     */
    List<RoomStatusLog> findByRoomIdAndTimeRange(@Param("roomId") Long roomId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * 查询最近的状态变更记录
     */
    List<RoomStatusLog> findRecentByRoomId(@Param("roomId") Long roomId,
                                          @Param("limit") Integer limit);
}