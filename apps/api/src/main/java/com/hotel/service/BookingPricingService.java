package com.hotel.service;

import com.hotel.dto.order.PriceBreakdown;
import com.hotel.dto.order.PricingRequest;
import com.hotel.entity.Coupon;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.repository.CouponRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 预订价格计算服务
 *
 * 提供预订流程中的价格计算逻辑，支持：
 * - 房费计算（使用动态定价）
 * - 服务费计算（固定百分比）
 * - 优惠券折扣
 * - 税费计算
 *
 * @author Hotel Development Team
 * @since 2024-12-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingPricingService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final CouponRepository couponRepository;
    private final PricingService pricingService;

    /**
     * 服务费率（10%）
     */
    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.10");

    /**
     * 税率（6%）
     */
    private static final BigDecimal TAX_RATE = new BigDecimal("0.06");

    /**
     * 计算预订价格
     *
     * @param request 价格计算请求
     * @return 价格明细
     */
    @Cacheable(value = "booking-pricing", key = "#request.hashCode()", unless = "#result == null")
    public PriceBreakdown calculatePrice(PricingRequest request) {
        log.debug("计算预订价格: roomId={}, checkIn={}, checkOut={}, guestCount={}, couponCode={}",
                request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate(),
                request.getGuestCount(), request.getCouponCode());

        // 1. 验证房间
        Room room = roomRepository.selectById(request.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("房间不存在: " + request.getRoomId());
        }

        RoomType roomType = roomTypeRepository.selectById(room.getRoomTypeId());
        if (roomType == null) {
            throw new IllegalArgumentException("房间类型不存在: " + room.getRoomTypeId());
        }

        // 2. 计算入住天数
        LocalDate checkIn = LocalDate.parse(request.getCheckInDate());
        LocalDate checkOut = LocalDate.parse(request.getCheckOutDate());
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        if (nights <= 0) {
            throw new IllegalArgumentException("入住天数必须大于0");
        }

        // 3. 使用动态定价计算基础房费
        BigDecimal baseRoomFee = pricingService.calculateTotalPrice(
            request.getRoomId(), checkIn, checkOut);

        // 4. 计算服务费
        BigDecimal serviceFee = baseRoomFee.multiply(SERVICE_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        // 5. 计算折扣
        BigDecimal discountAmount = calculateDiscount(request.getCouponCode(), baseRoomFee);

        // 6. 计算税费
        BigDecimal subtotal = baseRoomFee.add(serviceFee).subtract(discountAmount);
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        // 7. 计算总价
        BigDecimal totalPrice = subtotal.add(taxAmount);

        PriceBreakdown breakdown = new PriceBreakdown();
        breakdown.setRoomFee(baseRoomFee);
        breakdown.setServiceFee(serviceFee);
        breakdown.setTaxAmount(taxAmount);
        breakdown.setDiscountAmount(discountAmount);
        breakdown.setTotalPrice(totalPrice);
        breakdown.setNights(nights);
        breakdown.setRoomRate(roomType.getBasePrice());

        // 设置折扣信息
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0 && request.getCouponCode() != null) {
            breakdown.setCouponCode(request.getCouponCode());

            // 获取优惠券详情用于显示
            Optional<Coupon> coupon = couponRepository.findByCode(request.getCouponCode());
            coupon.ifPresent(c -> {
                breakdown.setDiscountType(c.getDiscountType());
                breakdown.setDiscountValue(c.getDiscountValue());
            });
        }

        log.debug("预订价格计算完成: {}", breakdown);
        return breakdown;
    }

    /**
     * 计算折扣金额
     */
    private BigDecimal calculateDiscount(String couponCode, BigDecimal baseRoomFee) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        Optional<Coupon> couponOpt = couponRepository.findByCode(couponCode);
        if (couponOpt.isEmpty()) {
            log.warn("优惠券不存在: {}", couponCode);
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponOpt.get();

        // 检查优惠券是否有效
        if (!isCouponValid(coupon)) {
            log.warn("优惠券已过期或不可用: {}", couponCode);
            return BigDecimal.ZERO;
        }

        return switch (coupon.getDiscountType()) {
            case "PERCENTAGE" -> {
                BigDecimal discount = baseRoomFee.multiply(
                    coupon.getDiscountValue().divide(new BigDecimal("100")));
                yield discount.setScale(2, RoundingMode.HALF_UP);
            }
            case "FIXED_AMOUNT" -> {
                // 固定金额不能超过房费
                BigDecimal discount = coupon.getDiscountValue().min(baseRoomFee);
                yield discount.setScale(2, RoundingMode.HALF_UP);
            }
            default -> {
                log.warn("未知的折扣类型: {}", coupon.getDiscountType());
                yield BigDecimal.ZERO;
            }
        };
    }

    /**
     * 检查优惠券是否有效
     */
    private boolean isCouponValid(Coupon coupon) {
        LocalDate now = LocalDate.now();
        return coupon.getStatus().equals("ACTIVE")
                && (coupon.getValidFrom() == null || !now.isBefore(coupon.getValidFrom()))
                && (coupon.getValidTo() == null || !now.isAfter(coupon.getValidTo()));
    }

    /**
     * 估算预订价格（不使用缓存）
     * 用于预订向导中的实时价格展示
     */
    public PriceBreakdown estimatePrice(PricingRequest request) {
        // 创建临时请求对象，避免使用缓存
        PricingRequest tempRequest = new PricingRequest();
        tempRequest.setRoomId(request.getRoomId());
        tempRequest.setCheckInDate(request.getCheckInDate());
        tempRequest.setCheckOutDate(request.getCheckOutDate());
        tempRequest.setGuestCount(request.getGuestCount());
        tempRequest.setCouponCode(request.getCouponCode());

        return calculatePrice(tempRequest);
    }

    /**
     * 验证价格计算的合理性
     */
    public boolean validatePriceCalculation(PriceBreakdown breakdown, PricingRequest request) {
        // 基本验证
        if (breakdown == null || breakdown.getTotalPrice() == null) {
            return false;
        }

        // 价格不能为负数
        if (breakdown.getTotalPrice().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        // 验证总价计算
        BigDecimal calculatedTotal = breakdown.getRoomFee()
                .add(breakdown.getServiceFee())
                .add(breakdown.getTaxAmount())
                .subtract(breakdown.getDiscountAmount());

        // 允许小数点后2位的误差
        return breakdown.getTotalPrice().subtract(calculatedTotal).abs()
                .compareTo(new BigDecimal("0.01")) <= 0;
    }

    /**
     * 验证优惠券代码（缓存20分钟）
     *
     * @param couponCode 优惠券代码
     * @return 优惠券信息，如果无效则返回null
     */
    @Cacheable(value = "coupons", key = "#couponCode", unless = "#result == null")
    public Coupon validateCoupon(String couponCode) {
        log.debug("从数据库验证优惠券: couponCode={}", couponCode);
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return null;
        }

        Optional<Coupon> couponOpt = couponRepository.findByCode(couponCode);
        if (couponOpt.isEmpty()) {
            return null;
        }

        Coupon coupon = couponOpt.get();
        return isCouponValid(coupon) ? coupon : null;
    }
}