package com.hotel.repository.facility;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.facility.FacilityCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FacilityCategoryRepository extends BaseMapper<FacilityCategory> {

    /**
     * 根据酒店ID查询设施分类列表
     *
     * @param hotelId 酒店ID
     * @return 设施分类列表
     */
    List<FacilityCategory> selectByHotelId(@Param("hotelId") Long hotelId);

    /**
     * 根据酒店ID查询激活的设施分类列表
     *
     * @param hotelId 酒店ID
     * @return 激活的设施分类列表
     */
    List<FacilityCategory> selectActiveByHotelId(@Param("hotelId") Long hotelId);

    /**
     * 检查分类名称是否已存在
     *
     * @param hotelId 酒店ID
     * @param name    分类名称
     * @param excludeId 排除的分类ID（用于更新时检查）
     * @return 是否存在
     */
    boolean existsByName(@Param("hotelId") Long hotelId,
                        @Param("name") String name,
                        @Param("excludeId") Long excludeId);
}