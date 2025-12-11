/**
 * 仪表板API服务
 */
import axios from '@/api/request'
import type {
  DashboardMetrics,
  RealTimeData,
  TrendData,
  RevenueStatistics,
  OrderTrendChart,
  RevenueChart,
  OccupancyChart,
  PieChart,
  DashboardChartData,
  HistoricalDataQuery,
  HistoricalDataAggregation
} from '@/types/dashboard'
import type { ApiResponse } from '@/types/dashboard'

/**
 * 获取仪表板核心指标
 */
export function getDashboardMetrics(): Promise<ApiResponse<DashboardMetrics>> {
  return axios.get('/v1/admin/dashboard/metrics')
}

/**
 * 获取实时数据
 */
export function getRealTimeData(): Promise<ApiResponse<RealTimeData>> {
  return axios.get('/v1/admin/dashboard/realtime')
}

/**
 * 获取趋势数据
 */
export function getTrendData(period: string, days: number): Promise<ApiResponse<TrendData>> {
  return axios.get('/v1/admin/dashboard/trends', {
    params: { period, days }
  })
}

/**
 * 获取收入统计
 */
export function getRevenueStatistics(): Promise<ApiResponse<RevenueStatistics>> {
  return axios.get('/v1/admin/dashboard/revenue')
}

/**
 * 获取房间状态统计
 */
export function getRoomStatusStatistics(): Promise<ApiResponse<Record<string, number>>> {
  return axios.get('/v1/admin/dashboard/room-status')
}

/**
 * 刷新仪表板缓存
 */
export function refreshDashboardCache(): Promise<ApiResponse<string>> {
  return axios.post('/v1/admin/dashboard/refresh-cache')
}

/**
 * 获取系统健康状态
 */
export function getSystemHealth(): Promise<ApiResponse<Record<string, string>>> {
  return axios.get('/v1/admin/dashboard/health')
}

// ========== 图表数据API ==========

/**
 * 获取订单趋势图表数据
 */
export function getOrderTrendsChart(
  startDate: string,
  endDate: string,
  period: string = 'daily'
): Promise<ApiResponse<OrderTrendChart>> {
  return axios.get('/v1/admin/dashboard/charts/orders/trends', {
    params: { startDate, endDate, period }
  })
}

/**
 * 获取收入分析图表数据
 */
export function getRevenueAnalysisChart(
  startDate: string,
  endDate: string,
  type: string = 'trends'
): Promise<ApiResponse<RevenueChart>> {
  return axios.get('/v1/admin/dashboard/charts/revenue/analysis', {
    params: { startDate, endDate, type }
  })
}

/**
 * 获取入住率图表数据
 */
export function getOccupancyAnalysisChart(
  startDate: string,
  endDate: string,
  type: string = 'trends'
): Promise<ApiResponse<OccupancyChart>> {
  return axios.get('/v1/admin/dashboard/charts/occupancy/analysis', {
    params: { startDate, endDate, type }
  })
}

/**
 * 获取饼图数据
 */
export function getPieChartData(
  dataType: string,
  startDate: string,
  endDate: string
): Promise<ApiResponse<PieChart>> {
  return axios.get('/v1/admin/dashboard/charts/pie', {
    params: { dataType, startDate, endDate }
  })
}

/**
 * 获取仪表板综合图表数据
 */
export function getDashboardChartData(
  startDate: string,
  endDate: string
): Promise<ApiResponse<DashboardChartData>> {
  return axios.get('/v1/admin/dashboard/charts/dashboard', {
    params: { startDate, endDate }
  })
}

// ========== 历史数据API ==========

/**
 * 查询历史订单数据
 */
export function queryHistoricalOrders(query: HistoricalDataQuery): Promise<ApiResponse<any>> {
  return axios.post('/v1/admin/dashboard/historical/orders', query)
}

/**
 * 聚合历史数据
 */
export function aggregateHistoricalData(query: HistoricalDataQuery): Promise<ApiResponse<HistoricalDataAggregation>> {
  return axios.post('/v1/admin/dashboard/historical/aggregation', query)
}

/**
 * 生成历史数据统计报告
 */
export function generateHistoricalReport(
  startDate: string,
  endDate: string,
  groupBy: string = 'daily'
): Promise<ApiResponse<any>> {
  return axios.get('/v1/admin/dashboard/historical/report', {
    params: { startDate, endDate, groupBy }
  })
}

/**
 * 导出历史数据
 */
export function exportHistoricalData(query: HistoricalDataQuery, format: string = 'csv'): Promise<Blob> {
  return axios.post('/v1/admin/dashboard/historical/export', query, {
    params: { format },
    responseType: 'blob'
  })
}

/**
 * 获取数据快照
 */
export function getDataSnapshot(date: string): Promise<ApiResponse<any>> {
  return axios.get('/v1/admin/dashboard/historical/snapshot', {
    params: { date }
  })
}