package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.BookingConflict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookingConflictRepository extends BaseMapper<BookingConflict> {

    List<BookingConflict> findByRoomId(
            @Param("roomId") Long roomId,
            @Param("status") String status
    );

    List<BookingConflict> findByUserId(
            @Param("userId") Long userId,
            @Param("status") String status
    );

    List<BookingConflict> findByConflictType(
            @Param("conflictType") String conflictType,
            @Param("status") String status
    );

    List<BookingConflict> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    int countByConflictType(
            @Param("conflictType") String conflictType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    List<BookingConflict> findUnresolvedConflicts(@Param("roomId") Long roomId);
}