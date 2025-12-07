package com.hotel.service.pricing;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.pricing.PricingRule;
import com.hotel.enums.PricingRuleType;
import com.hotel.enums.AdjustmentType;
import com.hotel.exception.BusinessException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.repository.pricing.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 价格规则业务逻辑层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final com.hotel.service.PricingService pricingService;

    /**
     * 创建价格规则
     *
     * @param rule 价格规则
     * @return 创建的规则
     */
    @Transactional
    public PricingRule createRule(PricingRule rule) {
        validateRule(rule);

        // 检查规则名称是否已存在
        int existingCount = pricingRuleRepository.countByName(rule.getHotelId(), rule.getName(), null);
        if (existingCount > 0) {
            throw new BusinessException("规则名称 '" + rule.getName() + "' 已存在");
        }

        pricingRuleRepository.insert(rule);
        log.info("创建价格规则成功: id={}, name={}", rule.getId(), rule.getName());

        return rule;
    }

    /**
     * 更新价格规则
     *
     * @param id 规则ID
     * @param rule 更新的规则信息
     * @return 更新后的规则
     */
    @Transactional
    public PricingRule updateRule(Long id, PricingRule rule) {
        PricingRule existingRule = pricingRuleRepository.selectById(id);
        if (existingRule == null) {
            throw new ResourceNotFoundException("价格规则不存在: " + id);
        }

        validateRule(rule);

        // 检查规则名称是否已存在（排除当前规则）
        int existingCount = pricingRuleRepository.countByName(
            existingRule.getHotelId(), rule.getName(), id);
        if (existingCount > 0) {
            throw new BusinessException("规则名称 '" + rule.getName() + "' 已存在");
        }

        // 更新字段
        existingRule.setName(rule.getName());
        existingRule.setRuleType(rule.getRuleType());
        existingRule.setAdjustmentType(rule.getAdjustmentType());
        existingRule.setAdjustmentValue(rule.getAdjustmentValue());
        existingRule.setStartDate(rule.getStartDate());
        existingRule.setEndDate(rule.getEndDate());
        existingRule.setDaysOfWeekList(rule.getDaysOfWeekList());
        existingRule.setIsActive(rule.getIsActive());
        existingRule.setPriority(rule.getPriority());
        existingRule.setRoomTypeId(rule.getRoomTypeId());

        pricingRuleRepository.updateById(existingRule);
        log.info("更新价格规则成功: id={}, name={}", id, existingRule.getName());

        return existingRule;
    }

    /**
     * 删除价格规则
     *
     * @param id 规则ID
     */
    @Transactional
    public void deleteRule(Long id) {
        PricingRule rule = pricingRuleRepository.selectById(id);
        if (rule == null) {
            throw new ResourceNotFoundException("价格规则不存在: " + id);
        }

        // 软删除
        pricingRuleRepository.deleteById(id);
        log.info("删除价格规则成功: id={}, name={}", id, rule.getName());
    }

    /**
     * 激活/停用价格规则
     *
     * @param id 规则ID
     * @param active 是否激活
     */
    @Transactional
    public void toggleRuleStatus(Long id, Boolean active) {
        PricingRule rule = pricingRuleRepository.selectById(id);
        if (rule == null) {
            throw new ResourceNotFoundException("价格规则不存在: " + id);
        }

        rule.setIsActive(active);
        pricingRuleRepository.updateById(rule);

        log.info("{}价格规则: id={}, name={}",
                active ? "激活" : "停用", id, rule.getName());
    }

    /**
     * 获取价格规则详情
     *
     * @param id 规则ID
     * @return 价格规则
     */
    @Transactional(readOnly = true)
    public PricingRule getRuleById(Long id) {
        PricingRule rule = pricingRuleRepository.selectById(id);
        if (rule == null) {
            throw new ResourceNotFoundException("价格规则不存在: " + id);
        }
        return rule;
    }

    /**
     * 获取指定酒店的所有价格规则
     *
     * @param hotelId 酒店ID
     * @param activeOnly 是否只获取激活的规则
     * @return 价格规则列表
     */
    @Transactional(readOnly = true)
    public List<PricingRule> getRulesByHotelId(Long hotelId, Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return pricingRuleRepository.findByHotelId(hotelId);
        } else {
            QueryWrapper<PricingRule> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("hotel_id", hotelId);
            queryWrapper.orderByDesc("priority", "created_at");
            return pricingRuleRepository.selectList(queryWrapper);
        }
    }

    /**
     * 获取指定房间类型的价格规则
     *
     * @param hotelId 酒店ID
     * @param roomTypeId 房间类型ID
     * @return 价格规则列表
     */
    @Transactional(readOnly = true)
    public List<PricingRule> getRulesByRoomType(Long hotelId, Long roomTypeId) {
        return pricingRuleRepository.findByHotelIdAndRoomTypeId(hotelId, roomTypeId);
    }

    /**
     * 获取指定类型的价格规则
     *
     * @param hotelId 酒店ID
     * @param ruleType 规则类型
     * @return 价格规则列表
     */
    @Transactional(readOnly = true)
    public List<PricingRule> getRulesByType(Long hotelId, PricingRuleType ruleType) {
        return pricingRuleRepository.findByHotelIdAndRuleType(hotelId, ruleType.name());
    }

    /**
     * 分页查询价格规则
     *
     * @param hotelId 酒店ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param keyword 关键词搜索（规则名称）
     * @param ruleType 规则类型过滤
     * @param activeOnly 是否只查询激活的规则
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public Page<PricingRule> getRulesWithPagination(Long hotelId, Integer pageNum, Integer pageSize,
                                                   String keyword, PricingRuleType ruleType, Boolean activeOnly) {
        Page<PricingRule> page = new Page<>(pageNum, pageSize);
        QueryWrapper<PricingRule> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("hotel_id", hotelId);

        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("name", keyword);
        }

        if (ruleType != null) {
            queryWrapper.eq("rule_type", ruleType.name());
        }

        if (Boolean.TRUE.equals(activeOnly)) {
            queryWrapper.eq("is_active", true);
        }

        queryWrapper.orderByDesc("priority", "created_at");

        return pricingRuleRepository.selectPage(page, queryWrapper);
    }

    /**
     * 获取适用于指定日期的规则
     *
     * @param hotelId 酒店ID
     * @param roomTypeId 房间类型ID
     * @param date 日期
     * @return 适用的规则列表
     */
    @Transactional(readOnly = true)
    public List<PricingRule> getApplicableRules(Long hotelId, Long roomTypeId, LocalDate date) {
        return pricingRuleRepository.findApplicableRules(hotelId, roomTypeId, date);
    }

    /**
     * 批量删除规则
     *
     * @param ids 规则ID列表
     */
    @Transactional
    public void batchDeleteRules(List<Long> ids) {
        for (Long id : ids) {
            deleteRule(id);
        }
        log.info("批量删除价格规则成功，数量: {}", ids.size());
    }

    /**
     * 批量激活/停用规则
     *
     * @param ids 规则ID列表
     * @param active 是否激活
     */
    @Transactional
    public void batchToggleRules(List<Long> ids, Boolean active) {
        for (Long id : ids) {
            toggleRuleStatus(id, active);
        }
        log.info("批量{}价格规则成功，数量: {}",
                active ? "激活" : "停用", ids.size());
    }

    /**
     * 创建默认的周末价格规则
     *
     * @param hotelId 酒店ID
     * @param roomTypeId 房间类型ID（可选）
     * @param adjustmentValue 调整值（百分比）
     * @return 创建的规则
     */
    @Transactional
    public PricingRule createWeekendRule(Long hotelId, Long roomTypeId, BigDecimal adjustmentValue) {
        PricingRule rule = new PricingRule();
        rule.setHotelId(hotelId);
        rule.setRoomTypeId(roomTypeId);
        rule.setName("周末价格调整");
        rule.setRuleType(PricingRuleType.WEEKEND.name());
        rule.setAdjustmentType(AdjustmentType.PERCENTAGE.name());
        rule.setAdjustmentValue(adjustmentValue);
        rule.setDaysOfWeekList(List.of(6, 7)); // 周六、周日
        rule.setIsActive(true);
        rule.setPriority(100);

        return createRule(rule);
    }

    /**
     * 创建默认的节假日价格规则
     *
     * @param hotelId 酒店ID
     * @param roomTypeId 房间类型ID（可选）
     * @param adjustmentValue 调整值（百分比）
     * @return 创建的规则
     */
    @Transactional
    public PricingRule createHolidayRule(Long hotelId, Long roomTypeId, BigDecimal adjustmentValue) {
        PricingRule rule = new PricingRule();
        rule.setHotelId(hotelId);
        rule.setRoomTypeId(roomTypeId);
        rule.setName("节假日价格调整");
        rule.setRuleType(PricingRuleType.HOLIDAY.name());
        rule.setAdjustmentType(AdjustmentType.PERCENTAGE.name());
        rule.setAdjustmentValue(adjustmentValue);
        rule.setIsActive(true);
        rule.setPriority(200); // 比周末规则优先级高

        return createRule(rule);
    }

    /**
     * 验证价格规则
     *
     * @param rule 价格规则
     */
    private void validateRule(PricingRule rule) {
        if (rule.getHotelId() == null) {
            throw new BusinessException("酒店ID不能为空");
        }

        if (!StringUtils.hasText(rule.getName())) {
            throw new BusinessException("规则名称不能为空");
        }

        if (rule.getName().length() > 100) {
            throw new BusinessException("规则名称不能超过100个字符");
        }

        if (!StringUtils.hasText(rule.getRuleType())) {
            throw new BusinessException("规则类型不能为空");
        }

        if (!StringUtils.hasText(rule.getAdjustmentType())) {
            throw new BusinessException("调整类型不能为空");
        }

        if (rule.getAdjustmentValue() == null) {
            throw new BusinessException("调整值不能为空");
        }

        if (rule.getAdjustmentValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("调整值必须大于0");
        }

        if ("PERCENTAGE".equals(rule.getAdjustmentType()) &&
            rule.getAdjustmentValue().compareTo(BigDecimal.valueOf(1000)) > 0) {
            throw new BusinessException("百分比调整不能超过1000%");
        }

        // 验证日期范围
        if (rule.getStartDate() != null && rule.getEndDate() != null &&
            rule.getStartDate().isAfter(rule.getEndDate())) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }

        // 验证优先级
        if (rule.getPriority() != null && rule.getPriority() < 0) {
            throw new BusinessException("优先级不能为负数");
        }
    }
}