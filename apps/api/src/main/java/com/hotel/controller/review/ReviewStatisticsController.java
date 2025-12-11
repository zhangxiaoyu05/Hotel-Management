package com.hotel.controller.review;

import com.hotel.common.api.CommonResult;
import com.hotel.dto.review.statistics.*;
import com.hotel.service.ReviewAnalyticsService;
import com.hotel.service.TextAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 评价统计分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/admin/reviews/statistics")
@RequiredArgsConstructor
@Tag(name = "评价统计分析", description = "评价统计分析相关API")
public class ReviewStatisticsController {

    private final ReviewAnalyticsService reviewAnalyticsService;
    private final TextAnalysisService textAnalysisService;

    /**
     * 获取综合评分统计
     */
    @GetMapping("/overview")
    @Operation(summary = "获取综合评分统计", description = "获取指定酒店的综合评分统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResult<ReviewStatisticsDTO> getOverviewStatistics(
            @Parameter(description = "酒店ID") @RequestParam Long hotelId,
            @Parameter(description = "统计周期", example = "monthly") @RequestParam(defaultValue = "monthly") String period) {

        log.info("获取综合评分统计，酒店ID: {}, 周期: {}", hotelId, period);

        try {
            ReviewStatisticsDTO statistics = reviewAnalyticsService.getComprehensiveStatistics(hotelId, period);
            return CommonResult.success(statistics);
        } catch (Exception e) {
            log.error("获取综合评分统计失败", e);
            return CommonResult.failed("获取综合评分统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取评分趋势数据
     */
    @GetMapping("/trends")
    @Operation(summary = "获取评分趋势数据", description = "获取指定时间范围内的评分趋势数据")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResult<List<RatingTrendDTO>> getRatingTrends(
            @Parameter(description = "酒店ID") @RequestParam Long hotelId,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "分组方式", example = "day") @RequestParam(defaultValue = "day") String groupBy) {

        log.info("获取评分趋势数据，酒店ID: {}, 开始: {}, 结束: {}, 分组: {}", hotelId, startDate, endDate, groupBy);

        try {
            List<RatingTrendDTO> trends = reviewAnalyticsService.getRatingTrends(hotelId, startDate, endDate, groupBy);
            return CommonResult.success(trends);
        } catch (Exception e) {
            log.error("获取评分趋势数据失败", e);
            return CommonResult.failed("获取评分趋势数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取评价词云
     */
    @GetMapping("/wordcloud")
    @Operation(summary = "获取评价词云", description = "获取指定酒店的评价关键词云数据")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResult<List<WordCloudDTO>> getWordCloud(
            @Parameter(description = "酒店ID") @RequestParam Long hotelId,
            @Parameter(description = "关键词数量限制", example = "50") @RequestParam(defaultValue = "50") Integer limit) {

        log.info("获取评价词云，酒店ID: {}, 限制: {}", hotelId, limit);

        try {
            List<WordCloudDTO> wordCloud = reviewAnalyticsService.getWordCloudData(hotelId, limit);
            return CommonResult.success(wordCloud);
        } catch (Exception e) {
            log.error("获取评价词云失败", e);
            return CommonResult.failed("获取评价词云失败: " + e.getMessage());
        }
    }

    /**
     * 获取酒店对比分析
     */
    @GetMapping("/comparison")
    @Operation(summary = "获取酒店对比分析", description = "获取指定酒店与竞品酒店的对比分析数据")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResult<List<HotelComparisonDTO>> getHotelComparison(
            @Parameter(description = "主酒店ID") @RequestParam Long hotelId,
            @Parameter(description = "竞品酒店ID列表，多个用逗号分隔") @RequestParam(required = false) String competitorIds) {

        log.info("获取酒店对比分析，主酒店ID: {}, 竞品IDs: {}", hotelId, competitorIds);

        try {
            List<Long> competitorIdList = null;
            if (competitorIds != null && !competitorIds.trim().isEmpty()) {
                competitorIdList = Arrays.stream(competitorIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .toList();
            }

            List<HotelComparisonDTO> comparison = reviewAnalyticsService.getHotelComparison(hotelId, competitorIdList);
            return CommonResult.success(comparison);
        } catch (Exception e) {
            log.error("获取酒店对比分析失败", e);
            return CommonResult.failed("获取酒店对比分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取改进建议
     */
    @GetMapping("/suggestions")
    @Operation(summary = "获取改进建议", description = "基于评价数据获取改进建议")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResult<List<SuggestionDTO>> getSuggestions(
            @Parameter(description = "酒店ID") @RequestParam Long hotelId,
            @Parameter(description = "建议类别", example = "service") @RequestParam(required = false) String category) {

        log.info("获取改进建议，酒店ID: {}, 类别: {}", hotelId, category);

        try {
            List<SuggestionDTO> suggestions = reviewAnalyticsService.getSuggestions(hotelId, category);
            return CommonResult.success(suggestions);
        } catch (Exception e) {
            log.error("获取改进建议失败", e);
            return CommonResult.failed("获取改进建议失败: " + e.getMessage());
        }
    }

    /**
     * 导出统计数据
     */
    @GetMapping("/export")
    @Operation(summary = "导出统计数据", description = "导出指定格式的统计数据报告")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResult<String> exportStatistics(
            @Parameter(description = "酒店ID") @RequestParam Long hotelId,
            @Parameter(description = "导出格式", example = "excel") @RequestParam(defaultValue = "excel") String format,
            @Parameter(description = "统计周期", example = "monthly") @RequestParam(defaultValue = "monthly") String period) {

        log.info("导出统计数据，酒店ID: {}, 格式: {}, 周期: {}", hotelId, format, period);

        try {
            // 这里需要实现实际的导出功能
            // 暂时返回模拟结果
            String downloadUrl = "/api/downloads/statistics_" + hotelId + "_" + period + "." + format;
            return CommonResult.success(downloadUrl);
        } catch (Exception e) {
            log.error("导出统计数据失败", e);
            return CommonResult.failed("导出统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 清空统计缓存
     */
    @PostMapping("/cache/clear")
    @Operation(summary = "清空统计缓存", description = "清空评价统计相关的缓存数据")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResult<String> clearStatisticsCache() {
        log.info("清空统计缓存");

        try {
            reviewAnalyticsService.clearAnalyticsCache();
            return CommonResult.success("统计缓存清空成功");
        } catch (Exception e) {
            log.error("清空统计缓存失败", e);
            return CommonResult.failed("清空统计缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取统计概览（简化版）
     */
    @GetMapping("/summary")
    @Operation(summary = "获取统计概览", description = "获取评价统计的概览信息")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResult<Object> getStatisticsSummary(
            @Parameter(description = "酒店ID") @RequestParam Long hotelId) {

        log.info("获取统计概览，酒店ID: {}", hotelId);

        try {
            // 获取基础统计信息
            ReviewStatisticsDTO monthlyStats = reviewAnalyticsService.getComprehensiveStatistics(hotelId, "monthly");

            // 构建概览数据
            Object summary = new Object() {
                public final Long totalReviews = monthlyStats.getTotalReviews();
                public final Double overallRating = monthlyStats.getOverallRating();
                public final Map<String, Double> dimensionRatings = monthlyStats.getDimensionRatings();
                public final String lastUpdated = monthlyStats.getLastUpdated().toString();
            };

            return CommonResult.success(summary);
        } catch (Exception e) {
            log.error("获取统计概览失败", e);
            return CommonResult.failed("获取统计概览失败: " + e.getMessage());
        }
    }
}