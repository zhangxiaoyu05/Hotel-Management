package com.hotel.service;

import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.entity.pricing.*;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.repository.pricing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 价格策略业务逻辑层
 * 包含价格计算引擎的核心功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;
    private final SpecialPriceRepository specialPriceRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final HolidayRepository holidayRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    /**
     * 计算指定房间在指定日期的价格
     * 价格计算优先级：
     * 1. 特殊价格（最高优先级）
     * 2. 动态定价规则
     * 3. 基础价格（最低优先级）
     *
     * @param roomId 房间ID
     * @param date 日期
     * @return 计算后的价格
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateRoomPrice(Long roomId, LocalDate date) {
        log.debug("计算房间价格: roomId={}, date={}", roomId, date);

        // 获取房间信息
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            throw new IllegalArgumentException("房间不存在: " + roomId);
        }

        // 1. 检查特殊价格（最高优先级）
        SpecialPrice specialPrice = specialPriceRepository.findByRoomIdAndDate(roomId, date);
        if (specialPrice != null) {
            log.debug("找到特殊价格: {}", specialPrice.getPrice());
            return specialPrice.getPrice();
        }

        // 检查房间类型的特殊价格
        List<SpecialPrice> roomTypeSpecialPrices =
            specialPriceRepository.findByRoomTypeIdAndDate(room.getRoomTypeId(), date);
        if (!roomTypeSpecialPrices.isEmpty()) {
            // 取第一个匹配的房间类型特殊价格
            BigDecimal price = roomTypeSpecialPrices.get(0).getPrice();
            log.debug("找到房间类型特殊价格: {}", price);
            return price;
        }

        // 2. 获取基础价格
        RoomType roomType = roomTypeRepository.findById(room.getRoomTypeId()).orElse(null);
        if (roomType == null) {
            throw new IllegalArgumentException("房间类型不存在: " + room.getRoomTypeId());
        }

        BigDecimal basePrice = roomType.getBasePrice();
        if (basePrice == null) {
            // 如果房间类型没有基础价格，使用房间的当前价格
            basePrice = room.getPrice();
        }

        if (basePrice == null) {
            throw new IllegalStateException("房间和房间类型都没有设置价格");
        }

        BigDecimal finalPrice = basePrice;
        log.debug("基础价格: {}", basePrice);

        // 3. 应用动态定价规则
        List<PricingRule> applicableRules = pricingRuleRepository.findApplicableRules(
            room.getHotelId(), room.getRoomTypeId(), date);

        log.debug("适用的动态规则数量: {}", applicableRules.size());

        for (PricingRule rule : applicableRules) {
            if (rule.isApplicableForDate(date)) {
                BigDecimal rulePrice = rule.applyAdjustment(finalPrice);
                log.debug("应用规则 '{}': {} -> {}", rule.getName(), finalPrice, rulePrice);
                finalPrice = rulePrice;
            }
        }

        log.debug("最终计算价格: {}", finalPrice);
        return finalPrice;
    }

    /**
     * 计算房间类型在指定日期的价格
     *
     * @param roomTypeId 房间类型ID
     * @param hotelId 酒店ID
     * @param date 日期
     * @return 计算后的价格
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateRoomTypePrice(Long roomTypeId, Long hotelId, LocalDate date) {
        log.debug("计算房间类型价格: roomTypeId={}, hotelId={}, date={}", roomTypeId, hotelId, date);

        // 1. 检查房间类型的特殊价格
        List<SpecialPrice> specialPrices =
            specialPriceRepository.findByRoomTypeIdAndDate(roomTypeId, date);
        if (!specialPrices.isEmpty()) {
            BigDecimal price = specialPrices.get(0).getPrice();
            log.debug("找到房间类型特殊价格: {}", price);
            return price;
        }

        // 2. 获取基础价格
        RoomType roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
        if (roomType == null) {
            throw new IllegalArgumentException("房间类型不存在: " + roomTypeId);
        }

        BigDecimal basePrice = roomType.getBasePrice();
        if (basePrice == null) {
            throw new IllegalStateException("房间类型没有设置基础价格");
        }

        BigDecimal finalPrice = basePrice;
        log.debug("基础价格: {}", basePrice);

        // 3. 应用动态定价规则
        List<PricingRule> applicableRules = pricingRuleRepository.findApplicableRules(
            hotelId, roomTypeId, date);

        log.debug("适用的动态规则数量: {}", applicableRules.size());

        for (PricingRule rule : applicableRules) {
            if (rule.isApplicableForDate(date)) {
                BigDecimal rulePrice = rule.applyAdjustment(finalPrice);
                log.debug("应用规则 '{}': {} -> {}", rule.getName(), finalPrice, rulePrice);
                finalPrice = rulePrice;
            }
        }

        log.debug("最终计算价格: {}", finalPrice);
        return finalPrice;
    }

    /**
     * 批量计算房间在日期范围内的价格
     *
     * @param roomId 房间ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日期到价格的映射
     */
    @Transactional(readOnly = true)
    public java.util.Map<LocalDate, BigDecimal> calculateRoomPricesForDateRange(
            Long roomId, LocalDate startDate, LocalDate endDate) {

        java.util.Map<LocalDate, BigDecimal> priceMap = new java.util.LinkedHashMap<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            BigDecimal price = calculateRoomPrice(roomId, currentDate);
            priceMap.put(currentDate, price);
            currentDate = currentDate.plusDays(1);
        }

        return priceMap;
    }

    /**
     * 记录价格变更历史
     *
     * @param hotelId 酒店ID
     * @param roomTypeId 房间类型ID
     * @param roomId 房间ID（可选）
     * @param oldPrice 旧价格
     * @param newPrice 新价格
     * @param changeType 变更类型
     * @param changeReason 变更原因
     * @param changedBy 变更操作者ID
     */
    @Transactional
    public void recordPriceChange(Long hotelId, Long roomTypeId, Long roomId,
                                 BigDecimal oldPrice, BigDecimal newPrice,
                                 String changeType, String changeReason, Long changedBy) {

        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setHotelId(hotelId);
        priceHistory.setRoomTypeId(roomTypeId);
        priceHistory.setRoomId(roomId);
        priceHistory.setOldPrice(oldPrice);
        priceHistory.setNewPrice(newPrice);
        priceHistory.setChangeType(changeType);
        priceHistory.setChangeReason(changeReason);
        priceHistory.setChangedBy(changedBy);

        priceHistoryRepository.insert(priceHistory);
        log.info("记录价格变更历史: hotelId={}, roomTypeId={}, roomId={}, {} -> {}",
                hotelId, roomTypeId, roomId, oldPrice, newPrice);
    }

    /**
     * 检查指定日期是否为节假日
     *
     * @param date 日期
     * @return 是否为节假日
     */
    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        return holidayRepository.isHoliday(date);
    }

    /**
     * 获取指定日期的节假日信息
     *
     * @param date 日期
     * @return 节假日信息，可能为null
     */
    @Transactional(readOnly = true)
    public Holiday getHoliday(LocalDate date) {
        return holidayRepository.findByDate(date);
    }

    /**
     * 获取指定年份的所有节假日
     *
     * @param year 年份
     * @return 节假日列表
     */
    @Transactional(readOnly = true)
    public List<Holiday> getHolidaysByYear(Integer year) {
        return holidayRepository.findByYear(year);
    }

    /**
     * 获取适用于指定房间的所有价格规则
     *
     * @param roomId 房间ID
     * @return 价格规则列表
     */
    @Transactional(readOnly = true)
    public List<PricingRule> getApplicableRulesForRoom(Long roomId) {
        Room room = roomRepository.selectById(roomId);
        if (room == null) {
            return List.of();
        }

        return pricingRuleRepository.findByHotelIdAndRoomTypeId(
            room.getHotelId(), room.getRoomTypeId());
    }

    /**
     * 获取指定房间在指定日期的特殊价格
     *
     * @param roomId 房间ID
     * @param date 日期
     * @return 特殊价格，可能为null
     */
    @Transactional(readOnly = true)
    public SpecialPrice getSpecialPriceForRoom(Long roomId, LocalDate date) {
        return specialPriceRepository.findByRoomIdAndDate(roomId, date);
    }
}