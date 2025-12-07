package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationRepository extends BaseMapper<Notification> {

    List<Notification> findByUserId(@Param("userId") Long userId,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    int countByUserId(@Param("userId") Long userId);

    int countUnreadByUserId(@Param("userId") Long userId);

    List<Notification> findUnreadByUserId(@Param("userId") Long userId);
}