package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.RoomType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 房间类型数据访问层
 */
@Mapper
public interface RoomTypeRepository extends BaseMapper<RoomType> {

    /**
     * 分页查询房间类型列表
     *
     * @param page     分页对象
     * @param search   搜索关键词（名称或描述）
     * @param hotelId  酒店ID
     * @param status   房间类型状态
     * @param sortBy   排序字段
     * @param sortDir  排序方向
     * @return 分页结果
     */
    IPage<RoomType> selectRoomTypesWithPage(
            Page<RoomType> page,
            @Param("search") String search,
            @Param("hotelId") Long hotelId,
            @Param("status") String status,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir
    );

    /**
     * 根据酒店ID查询房间类型列表
     *
     * @param hotelId 酒店ID
     * @return 房间类型列表
     */
    List<RoomType> selectByHotelId(@Param("hotelId") Long hotelId);

    /**
     * 根据酒店ID和名称查找房间类型
     *
     * @param hotelId 酒店ID
     * @param name    房间类型名称
     * @return 房间类型实体
     */
    RoomType selectByHotelIdAndName(
            @Param("hotelId") Long hotelId,
            @Param("name") String name
    );

    /**
     * 检查房间类型是否有关联的房间
     *
     * @param roomTypeId 房间类型ID
     * @return 关联的房间数量
     */
    Integer countAssociatedRooms(@Param("roomTypeId") Long roomTypeId);

    /**
     * 根据状态查询房间类型列表
     *
     * @param status 房间类型状态
     * @return 房间类型列表
     */
    List<RoomType> selectByStatus(@Param("status") String status);
}