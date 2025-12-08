package com.hotel.repository.facility;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.facility.HotelFacility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HotelFacilityRepository extends BaseMapper<HotelFacility> {

    /**
     * 分页查询酒店设施列表
     *
     * @param page        分页对象
     * @param hotelId     酒店ID
     * @param categoryId  分类ID
     * @param status      设施状态
     * @param isFeatured  是否特色设施
     * @param keyword     关键词搜索
     * @param sortBy      排序字段
     * @param sortDir     排序方向
     * @return 分页结果
     */
    IPage<HotelFacility> selectFacilitiesWithPage(
            Page<HotelFacility> page,
            @Param("hotelId") Long hotelId,
            @Param("categoryId") Long categoryId,
            @Param("status") String status,
            @Param("isFeatured") Boolean isFeatured,
            @Param("keyword") String keyword,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir
    );

    /**
     * 根据酒店ID查询设施列表
     *
     * @param hotelId 酒店ID
     * @return 设施列表
     */
    List<HotelFacility> selectByHotelId(@Param("hotelId") Long hotelId);

    /**
     * 根据分类ID查询设施列表
     *
     * @param categoryId 分类ID
     * @return 设施列表
     */
    List<HotelFacility> selectByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 根据酒店ID和分类ID查询设施列表
     *
     * @param hotelId    酒店ID
     * @param categoryId 分类ID
     * @return 设施列表
     */
    List<HotelFacility> selectByHotelIdAndCategoryId(@Param("hotelId") Long hotelId,
                                                    @Param("categoryId") Long categoryId);

    /**
     * 查询特色设施列表
     *
     * @param hotelId 酒店ID
     * @return 特色设施列表
     */
    List<HotelFacility> selectFeaturedByHotelId(@Param("hotelId") Long hotelId);

    /**
     * 检查设施名称是否已存在
     *
     * @param hotelId     酒店ID
     * @param categoryId  分类ID
     * @param name        设施名称
     * @param excludeId   排除的设施ID（用于更新时检查）
     * @return 是否存在
     */
    boolean existsByName(@Param("hotelId") Long hotelId,
                        @Param("categoryId") Long categoryId,
                        @Param("name") String name,
                        @Param("excludeId") Long excludeId);
}