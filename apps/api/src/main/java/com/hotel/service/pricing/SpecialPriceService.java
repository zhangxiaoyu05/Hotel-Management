package com.hotel.service.pricing;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.entity.pricing.PriceHistory;
import com.hotel.entity.pricing.SpecialPrice;
import com.hotel.enums.PriceChangeType;
import com.hotel.exception.BusinessException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.repository.pricing.SpecialPriceRepository;
import com.hotel.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 特殊价格业务逻辑层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpecialPriceService {

    private final SpecialPriceRepository specialPriceRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PricingService pricingService;

    /**
     * 创建特殊价格
     *
     * @param specialPrice 特殊价格
     * @param createdBy 创建者ID
     * @return 创建的特殊价格
     */
    @Transactional
    public SpecialPrice createSpecialPrice(SpecialPrice specialPrice, Long createdBy) {
        validateSpecialPrice(specialPrice, null);

        // 获取房间类型信息用于记录历史
        RoomType roomType = roomTypeRepository.selectById(specialPrice.getRoomTypeId());
        if (roomType == null) {
            throw new ResourceNotFoundException("房间类型不存在: " + specialPrice.getRoomTypeId());
        }

        // 如果指定了房间，验证房间是否存在
        if (specialPrice.getRoomId() != null) {
            Room room = roomRepository.selectById(specialPrice.getRoomId());
            if (room == null || !room.getRoomTypeId().equals(specialPrice.getRoomTypeId())) {
                throw new BusinessException("房间不存在或不属于指定的房间类型");
            }
        }

        // 获取旧价格用于记录历史
        java.math.BigDecimal oldPrice = null;
        if (specialPrice.getRoomId() != null) {
            oldPrice = pricingService.calculateRoomPrice(specialPrice.getRoomId(), specialPrice.getDate());
        } else {
            oldPrice = pricingService.calculateRoomTypePrice(
                specialPrice.getRoomTypeId(), specialPrice.getHotelId(), specialPrice.getDate());
        }

        // 设置创建者
        specialPrice.setCreatedBy(createdBy);

        // 插入特殊价格
        specialPriceRepository.insert(specialPrice);

        // 记录价格变更历史
        pricingService.recordPriceChange(
            specialPrice.getHotelId(),
            specialPrice.getRoomTypeId(),
            specialPrice.getRoomId(),
            oldPrice,
            specialPrice.getPrice(),
            PriceChangeType.SPECIAL_PRICE.name(),
            "设置特殊价格: " + (specialPrice.getReason() != null ? specialPrice.getReason() : "无原因"),
            createdBy
        );

        log.info("创建特殊价格成功: id={}, roomId={}, date={}, price={}",
                specialPrice.getId(), specialPrice.getRoomId(),
                specialPrice.getDate(), specialPrice.getPrice());

        return specialPrice;
    }

    /**
     * 更新特殊价格
     *
     * @param id 特殊价格ID
     * @param specialPrice 更新的特殊价格信息
     * @param updatedBy 更新者ID
     * @return 更新后的特殊价格
     */
    @Transactional
    public SpecialPrice updateSpecialPrice(Long id, SpecialPrice specialPrice, Long updatedBy) {
        SpecialPrice existingPrice = specialPriceRepository.selectById(id);
        if (existingPrice == null) {
            throw new ResourceNotFoundException("特殊价格不存在: " + id);
        }

        validateSpecialPrice(specialPrice, id);

        // 获取旧价格
        java.math.BigDecimal oldPrice = existingPrice.getPrice();

        // 更新字段
        existingPrice.setPrice(specialPrice.getPrice());
        existingPrice.setReason(specialPrice.getReason());

        // 如果指定了房间，验证房间
        if (specialPrice.getRoomId() != null) {
            Room room = roomRepository.selectById(specialPrice.getRoomId());
            if (room == null || !room.getRoomTypeId().equals(existingPrice.getRoomTypeId())) {
                throw new BusinessException("房间不存在或不属于指定的房间类型");
            }
            existingPrice.setRoomId(specialPrice.getRoomId());
        }

        specialPriceRepository.updateById(existingPrice);

        // 记录价格变更历史
        pricingService.recordPriceChange(
            existingPrice.getHotelId(),
            existingPrice.getRoomTypeId(),
            existingPrice.getRoomId(),
            oldPrice,
            existingPrice.getPrice(),
            PriceChangeType.SPECIAL_PRICE.name(),
            "更新特殊价格: " + (existingPrice.getReason() != null ? existingPrice.getReason() : "无原因"),
            updatedBy
        );

        log.info("更新特殊价格成功: id={}, roomId={}, date={}, {} -> {}",
                id, existingPrice.getRoomId(), existingPrice.getDate(),
                oldPrice, existingPrice.getPrice());

        return existingPrice;
    }

    /**
     * 删除特殊价格
     *
     * @param id 特殊价格ID
     */
    @Transactional
    public void deleteSpecialPrice(Long id) {
        SpecialPrice specialPrice = specialPriceRepository.selectById(id);
        if (specialPrice == null) {
            throw new ResourceNotFoundException("特殊价格不存在: " + id);
        }

        // 获取旧价格
        java.math.BigDecimal oldPrice = specialPrice.getPrice();

        // 计算删除后的价格
        java.math.BigDecimal newPrice = null;
        if (specialPrice.getRoomId() != null) {
            newPrice = pricingService.calculateRoomPrice(specialPrice.getRoomId(), specialPrice.getDate());
        } else {
            newPrice = pricingService.calculateRoomTypePrice(
                specialPrice.getRoomTypeId(), specialPrice.getHotelId(), specialPrice.getDate());
        }

        // 软删除
        specialPriceRepository.deleteById(id);

        // 记录价格变更历史
        pricingService.recordPriceChange(
            specialPrice.getHotelId(),
            specialPrice.getRoomTypeId(),
            specialPrice.getRoomId(),
            oldPrice,
            newPrice,
            PriceChangeType.SPECIAL_PRICE.name(),
            "删除特殊价格",
            specialPrice.getCreatedBy()
        );

        log.info("删除特殊价格成功: id={}, roomId={}, date={}, price={}",
                id, specialPrice.getRoomId(), specialPrice.getDate(), specialPrice.getPrice());
    }

    /**
     * 获取特殊价格详情
     *
     * @param id 特殊价格ID
     * @return 特殊价格
     */
    @Transactional(readOnly = true)
    public SpecialPrice getSpecialPriceById(Long id) {
        SpecialPrice specialPrice = specialPriceRepository.selectById(id);
        if (specialPrice == null) {
            throw new ResourceNotFoundException("特殊价格不存在: " + id);
        }
        return specialPrice;
    }

    /**
     * 获取指定酒店的所有特殊价格
     *
     * @param hotelId 酒店ID
     * @param dateStart 开始日期（可选）
     * @param dateEnd 结束日期（可选）
     * @return 特殊价格列表
     */
    @Transactional(readOnly = true)
    public List<SpecialPrice> getSpecialPricesByHotelId(Long hotelId, LocalDate dateStart, LocalDate dateEnd) {
        if (dateStart != null && dateEnd != null) {
            return specialPriceRepository.findByHotelIdAndDateRange(hotelId, dateStart, dateEnd);
        } else {
            return specialPriceRepository.findByHotelId(hotelId);
        }
    }

    /**
     * 获取指定房间的特殊价格
     *
     * @param roomId 房间ID
     * @param date 日期
     * @return 特殊价格，可能为null
     */
    @Transactional(readOnly = true)
    public SpecialPrice getSpecialPriceForRoom(Long roomId, LocalDate date) {
        return specialPriceRepository.findByRoomIdAndDate(roomId, date);
    }

    /**
     * 获取指定房间类型的特殊价格
     *
     * @param roomTypeId 房间类型ID
     * @param date 日期
     * @return 特殊价格列表
     */
    @Transactional(readOnly = true)
    public List<SpecialPrice> getSpecialPricesForRoomType(Long roomTypeId, LocalDate date) {
        return specialPriceRepository.findByRoomTypeIdAndDate(roomTypeId, date);
    }

    /**
     * 获取未来的特殊价格
     *
     * @param hotelId 酒店ID
     * @param fromDate 起始日期
     * @return 特殊价格列表
     */
    @Transactional(readOnly = true)
    public List<SpecialPrice> getFutureSpecialPrices(Long hotelId, LocalDate fromDate) {
        return specialPriceRepository.findFuturePrices(hotelId, fromDate);
    }

    /**
     * 分页查询特殊价格
     *
     * @param hotelId 酒店ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param dateStart 开始日期过滤
     * @param dateEnd 结束日期过滤
     * @param roomTypeId 房间类型过滤
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public Page<SpecialPrice> getSpecialPricesWithPagination(Long hotelId, Integer pageNum, Integer pageSize,
                                                           LocalDate dateStart, LocalDate dateEnd, Long roomTypeId) {
        Page<SpecialPrice> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SpecialPrice> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("hotel_id", hotelId);

        if (dateStart != null) {
            queryWrapper.ge("date", dateStart);
        }
        if (dateEnd != null) {
            queryWrapper.le("date", dateEnd);
        }
        if (roomTypeId != null) {
            queryWrapper.eq("room_type_id", roomTypeId);
        }

        queryWrapper.orderByDesc("date", "created_at");

        return specialPriceRepository.selectPage(page, queryWrapper);
    }

    /**
     * 批量删除特殊价格
     *
     * @param ids 特殊价格ID列表
     */
    @Transactional
    public void batchDeleteSpecialPrices(List<Long> ids) {
        for (Long id : ids) {
            deleteSpecialPrice(id);
        }
        log.info("批量删除特殊价格成功，数量: {}", ids.size());
    }

    /**
     * 复制特殊价格到其他日期
     *
     * @param id 源特殊价格ID
     * @param targetDates 目标日期列表
     * @param copiedBy 复制者ID
     * @return 复制的特殊价格列表
     */
    @Transactional
    public List<SpecialPrice> copySpecialPriceToDateRange(Long id, List<LocalDate> targetDates, Long copiedBy) {
        SpecialPrice sourcePrice = getSpecialPriceById(id);
        List<SpecialPrice> copiedPrices = new java.util.ArrayList<>();

        for (LocalDate targetDate : targetDates) {
            // 检查目标日期是否已存在特殊价格
            List<SpecialPrice> existingPrices = specialPriceRepository
                .findByRoomTypeIdAndDate(sourcePrice.getRoomTypeId(), targetDate);

            boolean hasConflict = existingPrices.stream()
                .anyMatch(price -> price.getRoomId().equals(sourcePrice.getRoomId()));

            if (hasConflict) {
                log.warn("目标日期 {} 已存在特殊价格，跳过复制", targetDate);
                continue;
            }

            // 创建新的特殊价格
            SpecialPrice newPrice = new SpecialPrice();
            newPrice.setHotelId(sourcePrice.getHotelId());
            newPrice.setRoomTypeId(sourcePrice.getRoomTypeId());
            newPrice.setRoomId(sourcePrice.getRoomId());
            newPrice.setDate(targetDate);
            newPrice.setPrice(sourcePrice.getPrice());
            newPrice.setReason("复制自 " + sourcePrice.getDate() +
                             (sourcePrice.getReason() != null ? ": " + sourcePrice.getReason() : ""));

            SpecialPrice createdPrice = createSpecialPrice(newPrice, copiedBy);
            copiedPrices.add(createdPrice);
        }

        log.info("复制特殊价格成功，源ID: {}, 复制数量: {}", id, copiedPrices.size());
        return copiedPrices;
    }

    /**
     * 验证特殊价格
     *
     * @param specialPrice 特殊价格
     * @param excludeId 排除的ID（用于更新时检查）
     */
    private void validateSpecialPrice(SpecialPrice specialPrice, Long excludeId) {
        if (specialPrice.getHotelId() == null) {
            throw new BusinessException("酒店ID不能为空");
        }

        if (specialPrice.getRoomTypeId() == null) {
            throw new BusinessException("房间类型ID不能为空");
        }

        if (specialPrice.getDate() == null) {
            throw new BusinessException("日期不能为空");
        }

        if (specialPrice.getPrice() == null) {
            throw new BusinessException("价格不能为空");
        }

        if (specialPrice.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("价格必须大于0");
        }

        // 检查日期是否为过去日期（特殊情况可以允许，但给出警告）
        if (specialPrice.getDate().isBefore(LocalDate.now().minusDays(1))) {
            log.warn("设置的日期为过去日期: {}", specialPrice.getDate());
        }

        // 检查是否已存在相同配置的特殊价格
        int duplicateCount = specialPriceRepository.countByDuplicate(
            specialPrice.getHotelId(),
            specialPrice.getRoomTypeId(),
            specialPrice.getRoomId(),
            specialPrice.getDate(),
            excludeId
        );

        if (duplicateCount > 0) {
            throw new BusinessException("该房间在指定日期已存在特殊价格");
        }
    }
}