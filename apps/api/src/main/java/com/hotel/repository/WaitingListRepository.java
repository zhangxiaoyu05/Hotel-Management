package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.WaitingList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WaitingListRepository extends BaseMapper<WaitingList> {

    List<WaitingList> findByRoomId(
            @Param("roomId") Long roomId,
            @Param("status") String status
    );

    List<WaitingList> findByUserId(
            @Param("userId") Long userId,
            @Param("status") String status
    );

    List<WaitingList> findByRoomIdOrderByPriority(
            @Param("roomId") Long roomId,
            @Param("status") String status
    );

    List<WaitingList> findExpiredWaitingList(@Param("currentTime") LocalDateTime currentTime);

    List<WaitingList> findWaitingListForRoom(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDateTime checkInDate,
            @Param("checkOutDate") LocalDateTime checkOutDate,
            @Param("status") String status
    );

    int updateStatusToNotified(@Param("id") Long id, @Param("notifiedAt") LocalDateTime notifiedAt);

    int updateStatusToConfirmed(
            @Param("id") Long id,
            @Param("confirmedOrderId") Long confirmedOrderId
    );

    int countByRoomId(
            @Param("roomId") Long roomId,
            @Param("status") String status
    );

    List<WaitingList> findNextInLine(@Param("roomId") Long roomId, @Param("limit") int limit);
}