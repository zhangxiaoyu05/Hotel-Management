import { ref, readonly } from 'vue'
import type {
  OrderReportDTO,
  RevenueReportDTO,
  UserReportDTO,
  RoomReportDTO,
  ReportOverviewDTO,
  ReportExportRequest,
  ReportFilters
} from '@/types/report'
import { reportService } from '@/services/reportService'

export const useReportStore = defineStore('report', () => {
  // 状态
  const loading = ref(false)
  const error = ref('')

  // 报表数据
  const orderReport = ref<OrderReportDTO | null>(null)
  const revenueReport = ref<RevenueReportDTO | null>(null)
  const userReport = ref<UserReportDTO | null>(null)
  const roomReport = ref<RoomReportDTO | null>(null)
  const reportOverview = ref<ReportOverviewDTO | null>(null)

  // 获取订单报表
  const fetchOrderReport = async (filters: ReportFilters) => {
    loading.value = true
    error.value = ''

    try {
      orderReport.value = await reportService.getOrderReport(filters)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取订单报表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 获取收入报表
  const fetchRevenueReport = async (filters: ReportFilters) => {
    loading.value = true
    error.value = ''

    try {
      revenueReport.value = await reportService.getRevenueReport(filters)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取收入报表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 获取用户报表
  const fetchUserReport = async (filters: ReportFilters) => {
    loading.value = true
    error.value = ''

    try {
      userReport.value = await reportService.getUserReport(filters)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取用户报表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 获取房间报表
  const fetchRoomReport = async (filters: ReportFilters) => {
    loading.value = true
    error.value = ''

    try {
      roomReport.value = await reportService.getRoomReport(filters)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取房间报表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 获取报表概览
  const fetchReportOverview = async () => {
    loading.value = true
    error.value = ''

    try {
      reportOverview.value = await reportService.getReportOverview()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取报表概览失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 导出报表
  const exportReport = async (request: ReportExportRequest) => {
    loading.value = true
    error.value = ''

    try {
      const fileUrl = await reportService.exportReport(request)
      return fileUrl
    } catch (err) {
      error.value = err instanceof Error ? err.message : '导出报表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 刷新报表缓存
  const refreshReportCache = async () => {
    loading.value = true
    error.value = ''

    try {
      await reportService.refreshReportCache()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '刷新报表缓存失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 清除所有报表数据
  const clearReports = () => {
    orderReport.value = null
    revenueReport.value = null
    userReport.value = null
    roomReport.value = null
    reportOverview.value = null
    error.value = ''
  }

  // 重置错误
  const clearError = () => {
    error.value = ''
  }

  return {
    // 状态 (只读)
    loading: readonly(loading),
    error: readonly(error),
    orderReport: readonly(orderReport),
    revenueReport: readonly(revenueReport),
    userReport: readonly(userReport),
    roomReport: readonly(roomReport),
    reportOverview: readonly(reportOverview),

    // 方法
    fetchOrderReport,
    fetchRevenueReport,
    fetchUserReport,
    fetchRoomReport,
    fetchReportOverview,
    exportReport,
    refreshReportCache,
    clearReports,
    clearError
  }
})