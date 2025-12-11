package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.IncentiveRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface IncentiveRuleRepository extends BaseMapper<IncentiveRule> {

    /**
     * 查找指定类型的有效激励规则
     */
    @Select("SELECT * FROM incentive_rules WHERE rule_type = #{ruleType} AND is_active = true " +
            "AND valid_from <= #{currentDate} AND (valid_to IS NULL OR valid_to >= #{currentDate})")
    List<IncentiveRule> findActiveRulesByType(@Param("ruleType") String ruleType, @Param("currentDate") LocalDate currentDate);

    /**
     * 查找所有有效的激励规则
     */
    @Select("SELECT * FROM incentive_rules WHERE is_active = true " +
            "AND valid_from <= #{currentDate} AND (valid_to IS NULL OR valid_to >= #{currentDate})")
    List<IncentiveRule> findAllActiveRules(@Param("currentDate") LocalDate currentDate);
}