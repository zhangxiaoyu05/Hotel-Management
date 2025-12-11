package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.Hotel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HotelRepository extends BaseMapper<Hotel> {

    /**
     * 分页查询酒店列表
     *
     * @param page     分页对象
     * @param search   搜索关键词
     * @param status   酒店状态
     * @param sortBy   排序字段
     * @param sortDir  排序方向
     * @return 分页结果
     */
    IPage<Hotel> selectHotelsWithPage(
            Page<Hotel> page,
            @Param("search") String search,
            @Param("status") String status,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir
    );

    /**
     * 根据名称查找酒店
     *
     * @param name 酒店名称
     * @return 酒店实体
     */
    Hotel selectByName(@Param("name") String name);
}