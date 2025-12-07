package com.hotel.repository.pricing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.pricing.PricingRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 价格规则Repository
 */
@Mapper
public interface PricingRuleRepository extends BaseMapper<PricingRule> {

    /**
     * 查找适用于指定酒店、房间类型和日期的价格规则
     * @param hotelId 酒店ID
     * @param roomTypeId 房间类型ID
     * @param date 适用日期
     * @return 适用的价格规则列表，按优先级降序排列
     */
    @Select("SELECT * FROM pricing_rules " +
            "WHERE hotel_id = #{hotelId} " +
            "AND (room_type_id = #{roomTypeId} OR room_type_id IS NULL) " +
            "AND is_active = true " +
            "AND (start_date IS NULL OR start_date <= #{date}) " +
            "AND (end_date IS NULL OR end_date >= #{date}) " +
            "AND deleted = 0 " +
            "ORDER BY priority DESC, created_at DESC")
    List<PricingRule> findApplicableRules(@Param("hotelId") Long hotelId,
                                          @Param("roomTypeId") Long roomTypeId,
                                          @Param("date") LocalDate date);

    /**
     * 查找指定酒店的所有活跃价格规则
     * @param hotelId 酒店ID
     * @return 价格规则列表
     */
    @Select("SELECT * FROM pricing_rules " +
            "WHERE hotel_id = #{hotelId} " +
            "AND is_active = true " +
            "AND deleted = 0 " +
            "ORDER BY priority DESC, created_at DESC")
    List<PricingRule> findByHotelId(@Param("hotelId") Long hotelId);

    /**
     * 查找指定房间类型的价格规则
     * @param hotelId 酒店ID
     * @param roomTypeId 房间类型ID
     * @return 价格规则列表
     */
    @Select("SELECT * FROM pricing_rules " +
            "WHERE hotel_id = #{hotelId} " +
            "AND (room_type_id = #{roomTypeId} OR room_type_id IS NULL) " +
            "AND deleted = 0 " +
            "ORDER BY priority DESC, created_at DESC")
    List<PricingRule> findByHotelIdAndRoomTypeId(@Param("hotelId") Long hotelId,
                                                 @Param("roomTypeId") Long roomTypeId);

    /**
     * 查找指定类型的规则
     * @param hotelId 酒店ID
     * @param ruleType 规则类型
     * @return 价格规则列表
     */
    @Select("SELECT * FROM pricing_rules " +
            "WHERE hotel_id = #{hotelId} " +
            "AND rule_type = #{ruleType} " +
            "AND deleted = 0 " +
            "ORDER BY priority DESC, created_at DESC")
    List<PricingRule> findByHotelIdAndRuleType(@Param("hotelId") Long hotelId,
                                               @Param("ruleType") String ruleType);

    /**
     * 检查规则名称是否已存在
     * @param hotelId 酒店ID
     * @param name 规则名称
     * @param excludeId 排除的规则ID（用于更新时检查）
     * @return 存在的规则数量
     */
    @Select("SELECT COUNT(*) FROM pricing_rules " +
            "WHERE hotel_id = #{hotelId} " +
            "AND name = #{name} " +
            "AND (#{excludeId} IS NULL OR id != #{excludeId}) " +
            "AND deleted = 0")
    int countByName(@Param("hotelId") Long hotelId,
                    @Param("name") String name,
                    @Param("excludeId") Long excludeId);
}