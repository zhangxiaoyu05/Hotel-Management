package com.hotel.controller.pricing;

import com.hotel.controller.BaseController;
import com.hotel.entity.pricing.PricingRule;
import com.hotel.entity.pricing.SpecialPrice;
import com.hotel.entity.pricing.PriceHistory;
import com.hotel.entity.pricing.Holiday;
import com.hotel.enums.PricingRuleType;
import com.hotel.enums.PriceChangeType;
import com.hotel.service.PricingService;
import com.hotel.service.pricing.PricingRuleService;
import com.hotel.service.pricing.SpecialPriceService;
import com.hotel.service.pricing.PriceHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 价格策略控制器
 * 处理价格规则、特殊价格和价格计算相关的API
 */
@RestController
@RequestMapping("/v1/pricing")
@Validated
@Slf4j
@Tag(name = "价格策略接口", description = "价格规则管理、特殊价格设置和价格计算相关接口")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class PricingController extends BaseController {

    private final PricingService pricingService;
    private final PricingRuleService pricingRuleService;
    private final SpecialPriceService specialPriceService;
    private final PriceHistoryService priceHistoryService;

    // ==================== 价格计算相关接口 ====================

    /**
     * 计算房间在指定日期的价格
     */
    @GetMapping("/calculate/room/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "计算房间价格", description = "计算指定房间在指定日期的价格")
    public ResponseEntity<BaseController.ApiResponse<BigDecimal>> calculateRoomPrice(
            @Parameter(description = "房间ID") @PathVariable Long roomId,
            @Parameter(description = "日期") @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        BigDecimal price = pricingService.calculateRoomPrice(roomId, date);
        return ResponseEntity.ok(success(price, "价格计算成功"));
    }

    /**
     * 计算房间类型在指定日期的价格
     */
    @GetMapping("/calculate/room-type/{roomTypeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "计算房间类型价格", description = "计算指定房间类型在指定日期的价格")
    public ResponseEntity<BaseController.ApiResponse<BigDecimal>> calculateRoomTypePrice(
            @Parameter(description = "房间类型ID") @PathVariable Long roomTypeId,
            @Parameter(description = "酒店ID") @RequestParam @NotNull Long hotelId,
            @Parameter(description = "日期") @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        BigDecimal price = pricingService.calculateRoomTypePrice(roomTypeId, hotelId, date);
        return ResponseEntity.ok(success(price, "价格计算成功"));
    }

    /**
     * 批量计算房间在日期范围内的价格
     */
    @GetMapping("/calculate/room/{roomId}/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "批量计算房间价格", description = "计算指定房间在日期范围内的价格")
    public ResponseEntity<BaseController.ApiResponse<Map<LocalDate, BigDecimal>>> calculateRoomPricesBatch(
            @Parameter(description = "房间ID") @PathVariable Long roomId,
            @Parameter(description = "开始日期") @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<LocalDate, BigDecimal> priceMap = pricingService.calculateRoomPricesForDateRange(
            roomId, startDate, endDate);
        return ResponseEntity.ok(success(priceMap, "批量价格计算成功"));
    }

    // ==================== 价格规则相关接口 ====================

    /**
     * 创建价格规则
     */
    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "创建价格规则", description = "创建新的价格调整规则")
    public ResponseEntity<BaseController.ApiResponse<PricingRule>> createPricingRule(
            @Valid @RequestBody PricingRule rule) {

        PricingRule createdRule = pricingRuleService.createRule(rule);
        return ResponseEntity.ok(success(createdRule, "价格规则创建成功"));
    }

    /**
     * 更新价格规则
     */
    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "更新价格规则", description = "更新现有的价格规则")
    public ResponseEntity<BaseController.ApiResponse<PricingRule>> updatePricingRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Valid @RequestBody PricingRule rule) {

        PricingRule updatedRule = pricingRuleService.updateRule(id, rule);
        return ResponseEntity.ok(success(updatedRule, "价格规则更新成功"));
    }

    /**
     * 删除价格规则
     */
    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "删除价格规则", description = "删除指定的价格规则")
    public ResponseEntity<BaseController.ApiResponse<Void>> deletePricingRule(
            @Parameter(description = "规则ID") @PathVariable Long id) {

        pricingRuleService.deleteRule(id);
        return ResponseEntity.ok(success("价格规则删除成功"));
    }

    /**
     * 激活/停用价格规则
     */
    @PatchMapping("/rules/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "切换规则状态", description = "激活或停用价格规则")
    public ResponseEntity<BaseController.ApiResponse<Void>> toggleRuleStatus(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Parameter(description = "是否激活") @RequestParam Boolean active) {

        pricingRuleService.toggleRuleStatus(id, active);
        String message = active ? "价格规则已激活" : "价格规则已停用";
        return ResponseEntity.ok(success(message));
    }

    /**
     * 获取价格规则详情
     */
    @GetMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "获取价格规则详情", description = "获取指定价格规则的详细信息")
    public ResponseEntity<BaseController.ApiResponse<PricingRule>> getPricingRule(
            @Parameter(description = "规则ID") @PathVariable Long id) {

        PricingRule rule = pricingRuleService.getRuleById(id);
        return ResponseEntity.ok(success(rule, "获取价格规则成功"));
    }

    /**
     * 获取酒店的价格规则列表
     */
    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "获取价格规则列表", description = "获取指定酒店的价格规则列表")
    public ResponseEntity<BaseController.ApiResponse<List<PricingRule>>> getPricingRules(
            @Parameter(description = "酒店ID") @RequestParam @NotNull Long hotelId,
            @Parameter(description = "是否只查询激活的规则") @RequestParam(required = false) Boolean activeOnly,
            @Parameter(description = "规则类型过滤") @RequestParam(required = false) PricingRuleType ruleType,
            @Parameter(description = "房间类型过滤") @RequestParam(required = false) Long roomTypeId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "20") @Min(1) Integer pageSize,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {

        if (pageNum > 1 || pageSize != 20 || keyword != null || ruleType != null || roomTypeId != null) {
            // 需要分页或过滤查询
            var page = pricingRuleService.getRulesWithPagination(
                hotelId, pageNum, pageSize, keyword, ruleType, activeOnly);
            return ResponseEntity.ok(success(page.getRecords(), "获取价格规则列表成功"));
        } else {
            // 简单列表查询
            List<PricingRule> rules = pricingRuleService.getRulesByHotelId(hotelId, activeOnly);
            return ResponseEntity.ok(success(rules, "获取价格规则列表成功"));
        }
    }

    /**
     * 批量删除价格规则
     */
    @DeleteMapping("/rules/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "批量删除价格规则", description = "批量删除指定的价格规则")
    public ResponseEntity<BaseController.ApiResponse<Void>> batchDeletePricingRules(
            @Parameter(description = "规则ID列表") @RequestBody List<Long> ids) {

        pricingRuleService.batchDeleteRules(ids);
        return ResponseEntity.ok(success("批量删除价格规则成功"));
    }

    /**
     * 批量切换规则状态
     */
    @PatchMapping("/rules/batch/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "批量切换规则状态", description = "批量激活或停用价格规则")
    public ResponseEntity<BaseController.ApiResponse<Void>> batchToggleRules(
            @Parameter(description = "规则ID列表") @RequestBody List<Long> ids,
            @Parameter(description = "是否激活") @RequestParam Boolean active) {

        pricingRuleService.batchToggleRules(ids, active);
        String message = active ? "批量激活规则成功" : "批量停用规则成功";
        return ResponseEntity.ok(success(message));
    }

    // ==================== 特殊价格相关接口 ====================

    /**
     * 创建特殊价格
     */
    @PostMapping("/special-prices")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "创建特殊价格", description = "为指定日期设置特殊价格")
    public ResponseEntity<BaseController.ApiResponse<SpecialPrice>> createSpecialPrice(
            @Valid @RequestBody SpecialPrice specialPrice) {

        // TODO: 从安全上下文获取当前用户ID
        Long currentUserId = getCurrentUserId();
        SpecialPrice createdPrice = specialPriceService.createSpecialPrice(specialPrice, currentUserId);
        return ResponseEntity.ok(success(createdPrice, "特殊价格创建成功"));
    }

    /**
     * 更新特殊价格
     */
    @PutMapping("/special-prices/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "更新特殊价格", description = "更新现有的特殊价格")
    public ResponseEntity<BaseController.ApiResponse<SpecialPrice>> updateSpecialPrice(
            @Parameter(description = "特殊价格ID") @PathVariable Long id,
            @Valid @RequestBody SpecialPrice specialPrice) {

        Long currentUserId = getCurrentUserId();
        SpecialPrice updatedPrice = specialPriceService.updateSpecialPrice(id, specialPrice, currentUserId);
        return ResponseEntity.ok(success(updatedPrice, "特殊价格更新成功"));
    }

    /**
     * 删除特殊价格
     */
    @DeleteMapping("/special-prices/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "删除特殊价格", description = "删除指定的特殊价格")
    public ResponseEntity<BaseController.ApiResponse<Void>> deleteSpecialPrice(
            @Parameter(description = "特殊价格ID") @PathVariable Long id) {

        specialPriceService.deleteSpecialPrice(id);
        return ResponseEntity.ok(success("特殊价格删除成功"));
    }

    /**
     * 获取特殊价格详情
     */
    @GetMapping("/special-prices/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "获取特殊价格详情", description = "获取指定特殊价格的详细信息")
    public ResponseEntity<BaseController.ApiResponse<SpecialPrice>> getSpecialPrice(
            @Parameter(description = "特殊价格ID") @PathVariable Long id) {

        SpecialPrice specialPrice = specialPriceService.getSpecialPriceById(id);
        return ResponseEntity.ok(success(specialPrice, "获取特殊价格成功"));
    }

    /**
     * 获取酒店的特殊价格列表
     */
    @GetMapping("/special-prices")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "获取特殊价格列表", description = "获取指定酒店的特殊价格列表")
    public ResponseEntity<BaseController.ApiResponse<List<SpecialPrice>>> getSpecialPrices(
            @Parameter(description = "酒店ID") @RequestParam @NotNull Long hotelId,
            @Parameter(description = "开始日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "房间类型过滤") @RequestParam(required = false) Long roomTypeId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "20") @Min(1) Integer pageSize) {

        if (pageNum > 1 || pageSize != 20 || startDate != null || endDate != null || roomTypeId != null) {
            // 需要分页或过滤查询
            var page = specialPriceService.getSpecialPricesWithPagination(
                hotelId, pageNum, pageSize, startDate, endDate, roomTypeId);
            return ResponseEntity.ok(success(page.getRecords(), "获取特殊价格列表成功"));
        } else {
            // 简单列表查询
            List<SpecialPrice> specialPrices = specialPriceService.getSpecialPricesByHotelId(hotelId, null, null);
            return ResponseEntity.ok(success(specialPrices, "获取特殊价格列表成功"));
        }
    }

    /**
     * 获取未来的特殊价格
     */
    @GetMapping("/special-prices/future")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "获取未来特殊价格", description = "获取指定日期之后的特殊价格")
    public ResponseEntity<BaseController.ApiResponse<List<SpecialPrice>>> getFutureSpecialPrices(
            @Parameter(description = "酒店ID") @RequestParam @NotNull Long hotelId,
            @Parameter(description = "起始日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {

        if (fromDate == null) {
            fromDate = LocalDate.now();
        }
        List<SpecialPrice> futurePrices = specialPriceService.getFutureSpecialPrices(hotelId, fromDate);
        return ResponseEntity.ok(success(futurePrices, "获取未来特殊价格成功"));
    }

    /**
     * 复制特殊价格到其他日期
     */
    @PostMapping("/special-prices/{id}/copy")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "复制特殊价格", description = "将指定特殊价格复制到其他日期")
    public ResponseEntity<BaseController.ApiResponse<List<SpecialPrice>>> copySpecialPrice(
            @Parameter(description = "源特殊价格ID") @PathVariable Long id,
            @Parameter(description = "目标日期列表") @RequestBody List<LocalDate> targetDates) {

        Long currentUserId = getCurrentUserId();
        List<SpecialPrice> copiedPrices = specialPriceService.copySpecialPriceToDateRange(
            id, targetDates, currentUserId);
        return ResponseEntity.ok(success(copiedPrices, "复制特殊价格成功"));
    }

    // ==================== 价格历史相关接口 ====================

    /**
     * 获取价格历史记录
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "获取价格历史记录", description = "获取指定条件的价格历史记录")
    public ResponseEntity<BaseController.ApiResponse<List<PriceHistory>>> getPriceHistory(
            @Parameter(description = "酒店ID") @RequestParam @NotNull Long hotelId,
            @Parameter(description = "房间ID") @RequestParam(required = false) Long roomId,
            @Parameter(description = "房间类型ID") @RequestParam(required = false) Long roomTypeId,
            @Parameter(description = "变更类型") @RequestParam(required = false) PriceChangeType changeType,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "100") @Min(1) Integer limit) {

        List<PriceHistory> history;
        if (roomId != null) {
            history = priceHistoryService.getPriceHistoryByRoomId(roomId, limit);
        } else if (roomTypeId != null) {
            history = priceHistoryService.getPriceHistoryByRoomTypeId(roomTypeId, limit);
        } else if (startTime != null && endTime != null) {
            history = priceHistoryService.getPriceHistoryByTimeRange(hotelId, startTime, endTime);
        } else if (changeType != null) {
            history = priceHistoryService.getPriceHistoryByChangeType(hotelId, changeType, limit);
        } else {
            history = priceHistoryService.getPriceHistoryByHotelId(hotelId, limit);
        }

        return ResponseEntity.ok(success(history, "获取价格历史记录成功"));
    }

    /**
     * 获取价格变更统计
     */
    @GetMapping("/history/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "获取价格变更统计", description = "获取指定时间范围内的价格变更统计信息")
    public ResponseEntity<BaseController.ApiResponse<Map<String, Object>>> getPriceChangeStatistics(
            @Parameter(description = "酒店ID") @RequestParam @NotNull Long hotelId,
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "30") @Min(1) Integer days) {

        Map<String, Object> statistics = priceHistoryService.getPriceChangeStatistics(hotelId, days);
        return ResponseEntity.ok(success(statistics, "获取价格变更统计成功"));
    }

    /**
     * 获取价格变更趋势
     */
    @GetMapping("/history/trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "获取价格变更趋势", description = "获取指定天数内的价格变更趋势数据")
    public ResponseEntity<BaseController.ApiResponse<Map<String, Object>>> getPriceChangeTrend(
            @Parameter(description = "酒店ID") @RequestParam @NotNull Long hotelId,
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "30") @Min(1) Integer days) {

        Map<String, Object> trend = priceHistoryService.getPriceChangeTrend(hotelId, days);
        return ResponseEntity.ok(success(trend, "获取价格变更趋势成功"));
    }

    // ==================== 节假日相关接口 ====================

    /**
     * 获取节假日信息
     */
    @GetMapping("/holidays")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "获取节假日信息", description = "获取指定年份或日期范围内的节假日信息")
    public ResponseEntity<BaseController.ApiResponse<List<Holiday>>> getHolidays(
            @Parameter(description = "年份") @RequestParam(required = false) Integer year,
            @Parameter(description = "开始日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Holiday> holidays;
        if (year != null) {
            holidays = pricingService.getHolidaysByYear(year);
        } else if (startDate != null && endDate != null) {
            holidays = pricingService.getHolidaysByYear(startDate.getYear())
                .stream()
                .filter(holiday -> !holiday.getDate().isBefore(startDate) && !holiday.getDate().isAfter(endDate))
                .collect(java.util.stream.Collectors.toList());
        } else {
            holidays = pricingService.getHolidaysByYear(LocalDate.now().getYear());
        }

        return ResponseEntity.ok(success(holidays, "获取节假日信息成功"));
    }

    /**
     * 检查指定日期是否为节假日
     */
    @GetMapping("/holidays/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "检查是否为节假日", description = "检查指定日期是否为节假日")
    public ResponseEntity<BaseController.ApiResponse<Map<String, Object>>> checkHoliday(
            @Parameter(description = "日期") @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        boolean isHoliday = pricingService.isHoliday(date);
        Holiday holiday = null;
        if (isHoliday) {
            holiday = pricingService.getHoliday(date);
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("date", date);
        result.put("isHoliday", isHoliday);
        result.put("holidayInfo", holiday);

        return ResponseEntity.ok(success(result, "节假日检查完成"));
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前用户ID
     * TODO: 从Spring Security上下文获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        // 临时实现，应该从安全上下文获取
        return 1L;
    }
}