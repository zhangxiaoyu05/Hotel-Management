import type {
  OrderReportDTO,
  RevenueReportDTO,
  UserReportDTO,
  RoomReportDTO,
  ReportOverviewDTO,
  ReportExportRequest
} from '@/types/report'
import { http } from '@/utils/http'

export const reportService = {
  // 获取订单报表
  async getOrderReport(params: {
    startDate: string
    endDate: string
    roomTypeId?: number
    orderStatus?: string
  }): Promise<OrderReportDTO> {
    const response = await http.get('/v1/admin/reports/orders', { params })
    return response.data.data
  },

  // 获取收入报表
  async getRevenueReport(params: {
    startDate: string
    endDate: string
    roomTypeId?: number
  }): Promise<RevenueReportDTO> {
    const response = await http.get('/v1/admin/reports/revenue', { params })
    return response.data.data
  },

  // 获取用户报表
  async getUserReport(params: {
    startDate: string
    endDate: string
  }): Promise<UserReportDTO> {
    const response = await http.get('/v1/admin/reports/users', { params })
    return response.data.data
  },

  // 获取房间报表
  async getRoomReport(params: {
    startDate: string
    endDate: string
    roomTypeId?: number
  }): Promise<RoomReportDTO> {
    const response = await http.get('/v1/admin/reports/rooms', { params })
    return response.data.data
  },

  // 获取报表概览
  async getReportOverview(): Promise<ReportOverviewDTO> {
    const response = await http.get('/v1/admin/reports/overview')
    return response.data.data
  },

  // 导出报表
  async exportReport(request: ReportExportRequest): Promise<string> {
    const response = await http.post('/v1/admin/reports/export', request)
    return response.data.data
  },

  // 刷新报表缓存
  async refreshReportCache(): Promise<void> {
    await http.post('/v1/admin/reports/refresh-cache')
  }
}