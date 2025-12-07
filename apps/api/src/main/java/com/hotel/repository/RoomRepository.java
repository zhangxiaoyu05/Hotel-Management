package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.dto.room.RoomSearchRequest;
import com.hotel.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房间数据访问层
 */
@Mapper
public interface RoomRepository extends BaseMapper<Room> {

    /**
     * 分页查询房间
     */
    @Select({
        "<script>",
        "SELECT r.*, rt.name as roomTypeName",
        "FROM rooms r",
        "LEFT JOIN room_types rt ON r.room_type_id = rt.id",
        "WHERE r.deleted = 0",
        "<if test='request.hotelId != null'>",
        "  AND r.hotel_id = #{request.hotelId}",
        "</if>",
        "<if test='request.roomTypeId != null'>",
        "  AND r.room_type_id = #{request.roomTypeId}",
        "</if>",
        "<if test='request.status != null and request.status != \"\"'>",
        "  AND r.status = #{request.status}",
        "</if>",
        "<if test='request.roomNumber != null and request.roomNumber != \"\"'>",
        "  AND r.room_number LIKE CONCAT('%', #{request.roomNumber}, '%')",
        "</if>",
        "<if test='request.floor != null'>",
        "  AND r.floor = #{request.floor}",
        "</if>",
        "<if test='request.minPrice != null'>",
        "  AND r.price >= #{request.minPrice}",
        "</if>",
        "<if test='request.maxPrice != null'>",
        "  AND r.price <= #{request.maxPrice}",
        "</if>",
        "ORDER BY ${request.sortBy} ${request.sortDir}",
        "</script>"
    })
    IPage<Room> searchRooms(Page<Room> page, @Param("request") RoomSearchRequest request);

    /**
     * 根据酒店ID获取房间列表
     */
    @Select("SELECT * FROM rooms WHERE hotel_id = #{hotelId} AND deleted = 0")
    List<Room> findByHotelId(@Param("hotelId") Long hotelId);

    /**
     * 根据房间类型ID获取房间列表
     */
    @Select("SELECT * FROM rooms WHERE room_type_id = #{roomTypeId} AND deleted = 0")
    List<Room> findByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    /**
     * 根据房间号和酒店ID查询房间
     */
    @Select("SELECT * FROM rooms WHERE room_number = #{roomNumber} AND hotel_id = #{hotelId} AND deleted = 0")
    Room findByRoomNumberAndHotelId(@Param("roomNumber") String roomNumber, @Param("hotelId") Long hotelId);

    /**
     * 根据状态获取房间列表
     */
    @Select("SELECT * FROM rooms WHERE status = #{status} AND deleted = 0")
    List<Room> findByStatus(@Param("status") String status);

    /**
     * 批量更新房间状态
     */
    @Update({
        "<script>",
        "UPDATE rooms SET status = #{status}, updated_at = NOW()",
        "WHERE id IN",
        "<foreach collection='roomIds' item='id' open='(' separator=',' close=')'>",
        "  #{id}",
        "</foreach>",
        "AND deleted = 0",
        "</script>"
    })
    int batchUpdateStatus(@Param("roomIds") List<Long> roomIds, @Param("status") String status);

    /**
     * 批量更新房间价格
     */
    @Update({
        "<script>",
        "UPDATE rooms SET price = #{price}, updated_at = NOW()",
        "WHERE id IN",
        "<foreach collection='roomIds' item='id' open='(' separator=',' close=')'>",
        "  #{id}",
        "</foreach>",
        "AND deleted = 0",
        "</script>"
    })
    int batchUpdatePrice(@Param("roomIds") List<Long> roomIds, @Param("price") BigDecimal price);

    /**
     * 检查房间号是否已存在（排除指定房间）
     */
    @Select("SELECT COUNT(*) FROM rooms WHERE room_number = #{roomNumber} AND hotel_id = #{hotelId} AND id != #{excludeId} AND deleted = 0")
    int countByRoomNumberAndHotelIdExcludeId(
        @Param("roomNumber") String roomNumber,
        @Param("hotelId") Long hotelId,
        @Param("excludeId") Long excludeId
    );
}