package com.hotel.repository.pricing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.pricing.PriceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格历史记录Repository
 */
@Mapper
public interface PriceHistoryRepository extends BaseMapper<PriceHistory> {

    /**
     * 查找指定酒店的价格历史记录
     * @param hotelId 酒店ID
     * @param limit 限制数量
     * @return 价格历史记录列表
     */
    @Select("SELECT ph.*, rt.name as room_type_name, r.room_number, u.nickname as changed_by_name " +
            "FROM price_history ph " +
            "LEFT JOIN room_types rt ON ph.room_type_id = rt.id " +
            "LEFT JOIN rooms r ON ph.room_id = r.id " +
            "LEFT JOIN users u ON ph.changed_by = u.id " +
            "WHERE ph.hotel_id = #{hotelId} " +
            "ORDER BY ph.created_at DESC " +
            "LIMIT #{limit}")
    List<PriceHistory> findByHotelId(@Param("hotelId") Long hotelId,
                                     @Param("limit") Integer limit);

    /**
     * 查找指定房间的价格历史记录
     * @param roomId 房间ID
     * @param limit 限制数量
     * @return 价格历史记录列表
     */
    @Select("SELECT ph.*, rt.name as room_type_name, r.room_number, u.nickname as changed_by_name " +
            "FROM price_history ph " +
            "LEFT JOIN room_types rt ON ph.room_type_id = rt.id " +
            "LEFT JOIN rooms r ON ph.room_id = r.id " +
            "LEFT JOIN users u ON ph.changed_by = u.id " +
            "WHERE ph.room_id = #{roomId} " +
            "ORDER BY ph.created_at DESC " +
            "LIMIT #{limit}")
    List<PriceHistory> findByRoomId(@Param("roomId") Long roomId,
                                    @Param("limit") Integer limit);

    /**
     * 查找指定房间类型的价格历史记录
     * @param roomTypeId 房间类型ID
     * @param limit 限制数量
     * @return 价格历史记录列表
     */
    @Select("SELECT ph.*, rt.name as room_type_name, r.room_number, u.nickname as changed_by_name " +
            "FROM price_history ph " +
            "LEFT JOIN room_types rt ON ph.room_type_id = rt.id " +
            "LEFT JOIN rooms r ON ph.room_id = r.id " +
            "LEFT JOIN users u ON ph.changed_by = u.id " +
            "WHERE ph.room_type_id = #{roomTypeId} " +
            "ORDER BY ph.created_at DESC " +
            "LIMIT #{limit}")
    List<PriceHistory> findByRoomTypeId(@Param("roomTypeId") Long roomTypeId,
                                        @Param("limit") Integer limit);

    /**
     * 查找指定时间范围内的价格历史记录
     * @param hotelId 酒店ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 价格历史记录列表
     */
    @Select("SELECT ph.*, rt.name as room_type_name, r.room_number, u.nickname as changed_by_name " +
            "FROM price_history ph " +
            "LEFT JOIN room_types rt ON ph.room_type_id = rt.id " +
            "LEFT JOIN rooms r ON ph.room_id = r.id " +
            "LEFT JOIN users u ON ph.changed_by = u.id " +
            "WHERE ph.hotel_id = #{hotelId} " +
            "AND ph.created_at BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY ph.created_at DESC")
    List<PriceHistory> findByHotelIdAndTimeRange(@Param("hotelId") Long hotelId,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 查找指定变更类型的价格历史记录
     * @param hotelId 酒店ID
     * @param changeType 变更类型
     * @param limit 限制数量
     * @return 价格历史记录列表
     */
    @Select("SELECT ph.*, rt.name as room_type_name, r.room_number, u.nickname as changed_by_name " +
            "FROM price_history ph " +
            "LEFT JOIN room_types rt ON ph.room_type_id = rt.id " +
            "LEFT JOIN rooms r ON ph.room_id = r.id " +
            "LEFT JOIN users u ON ph.changed_by = u.id " +
            "WHERE ph.hotel_id = #{hotelId} " +
            "AND ph.change_type = #{changeType} " +
            "ORDER BY ph.created_at DESC " +
            "LIMIT #{limit}")
    List<PriceHistory> findByHotelIdAndChangeType(@Param("hotelId") Long hotelId,
                                                  @Param("changeType") String changeType,
                                                  @Param("limit") Integer limit);

    /**
     * 统计指定时间范围内的价格变更次数
     * @param hotelId 酒店ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 价格变更次数
     */
    @Select("SELECT COUNT(*) FROM price_history " +
            "WHERE hotel_id = #{hotelId} " +
            "AND created_at BETWEEN #{startTime} AND #{endTime}")
    int countChangesInTimeRange(@Param("hotelId") Long hotelId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}