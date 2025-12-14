package com.hotel.service;

import com.hotel.dto.report.*;

/**
 * 报表服务接口
 * 提供各种报表的生成和导出功能
 *
 * @author Hotel System
 * @version 1.0
 */
public interface ReportService {

    /**
     * 生成订单报表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param roomTypeId 房型ID（可选）
     * @param orderStatus 订单状态（可选）
     * @return 订单报表DTO
     */
    OrderReportDTO generateOrderReport(java.time.LocalDate startDate,
                                      java.time.LocalDate endDate,
                                      Long roomTypeId,
                                      String orderStatus);

    /**
     * 生成收入报表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param roomTypeId 房型ID（可选）
     * @return 收入报表DTO
     */
    RevenueReportDTO generateRevenueReport(java.time.LocalDate startDate,
                                         java.time.LocalDate endDate,
                                         Long roomTypeId);

    /**
     * 生成用户报表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 用户报表DTO
     */
    UserReportDTO generateUserReport(java.time.LocalDate startDate,
                                   java.time.LocalDate endDate);

    /**
     * 生成房间报表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param roomTypeId 房型ID（可选）
     * @return 房间报表DTO
     */
    RoomReportDTO generateRoomReport(java.time.LocalDate startDate,
                                   java.time.LocalDate endDate,
                                   Long roomTypeId);

    /**
     * 导出报表
     *
     * @param request 导出请求
     * @return 导出文件URL
     */
    String exportReport(ReportExportRequest request);

    /**
     * 获取报表概览数据
     *
     * @return 报表概览DTO
     */
    ReportOverviewDTO getReportOverview();

    /**
     * 刷新报表缓存
     */
    void refreshReportCache();
}