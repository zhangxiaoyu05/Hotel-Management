package com.hotel.repository.pricing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.pricing.SpecialPrice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 特殊价格Repository
 */
@Mapper
public interface SpecialPriceRepository extends BaseMapper<SpecialPrice> {

    /**
     * 查找指定房间和日期的特殊价格
     * @param roomId 房间ID
     * @param date 日期
     * @return 特殊价格，可能为null
     */
    @Select("SELECT * FROM special_prices " +
            "WHERE room_id = #{roomId} " +
            "AND date = #{date} " +
            "AND deleted = 0 " +
            "LIMIT 1")
    SpecialPrice findByRoomIdAndDate(@Param("roomId") Long roomId,
                                     @Param("date") LocalDate date);

    /**
     * 查找指定房间类型和日期的特殊价格
     * @param roomTypeId 房间类型ID
     * @param date 日期
     * @return 特殊价格列表
     */
    @Select("SELECT * FROM special_prices " +
            "WHERE room_type_id = #{roomTypeId} " +
            "AND room_id IS NULL " +
            "AND date = #{date} " +
            "AND deleted = 0")
    List<SpecialPrice> findByRoomTypeIdAndDate(@Param("roomTypeId") Long roomTypeId,
                                               @Param("date") LocalDate date);

    /**
     * 查找指定酒店在日期范围内的特殊价格
     * @param hotelId 酒店ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 特殊价格列表
     */
    @Select("SELECT sp.*, rt.name as room_type_name, r.room_number " +
            "FROM special_prices sp " +
            "LEFT JOIN room_types rt ON sp.room_type_id = rt.id " +
            "LEFT JOIN rooms r ON sp.room_id = r.id " +
            "WHERE sp.hotel_id = #{hotelId} " +
            "AND sp.date BETWEEN #{startDate} AND #{endDate} " +
            "AND sp.deleted = 0 " +
            "ORDER BY sp.date DESC, sp.room_type_id, sp.room_id")
    List<SpecialPrice> findByHotelIdAndDateRange(@Param("hotelId") Long hotelId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * 查找指定酒店的所有特殊价格
     * @param hotelId 酒店ID
     * @return 特殊价格列表
     */
    @Select("SELECT sp.*, rt.name as room_type_name, r.room_number " +
            "FROM special_prices sp " +
            "LEFT JOIN room_types rt ON sp.room_type_id = rt.id " +
            "LEFT JOIN rooms r ON sp.room_id = r.id " +
            "WHERE sp.hotel_id = #{hotelId} " +
            "AND sp.deleted = 0 " +
            "ORDER BY sp.date DESC")
    List<SpecialPrice> findByHotelId(@Param("hotelId") Long hotelId);

    /**
     * 查找未来的特殊价格
     * @param hotelId 酒店ID
     * @param fromDate 起始日期
     * @return 特殊价格列表
     */
    @Select("SELECT sp.*, rt.name as room_type_name, r.room_number " +
            "FROM special_prices sp " +
            "LEFT JOIN room_types rt ON sp.room_type_id = rt.id " +
            "LEFT JOIN rooms r ON sp.room_id = r.id " +
            "WHERE sp.hotel_id = #{hotelId} " +
            "AND sp.date >= #{fromDate} " +
            "AND sp.deleted = 0 " +
            "ORDER BY sp.date ASC")
    List<SpecialPrice> findFuturePrices(@Param("hotelId") Long hotelId,
                                        @Param("fromDate") LocalDate fromDate);

    /**
     * 检查是否已存在相同房间和日期的特殊价格
     * @param hotelId 酒店ID
     * @param roomTypeId 房间类型ID
     * @param roomId 房间ID
     * @param date 日期
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 存在的记录数量
     */
    @Select("SELECT COUNT(*) FROM special_prices " +
            "WHERE hotel_id = #{hotelId} " +
            "AND room_type_id = #{roomTypeId} " +
            "AND (room_id = #{roomId} OR (room_id IS NULL AND #{roomId} IS NULL)) " +
            "AND date = #{date} " +
            "AND (#{excludeId} IS NULL OR id != #{excludeId}) " +
            "AND deleted = 0")
    int countByDuplicate(@Param("hotelId") Long hotelId,
                        @Param("roomTypeId") Long roomTypeId,
                        @Param("roomId") Long roomId,
                        @Param("date") LocalDate date,
                        @Param("excludeId") Long excludeId);
}