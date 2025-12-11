/**
 * 仪表板状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
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
  HistoricalDataQuery
} from '@/types/dashboard'
import {
  getDashboardMetrics,
  getRealTimeData,
  getTrendData,
  getRevenueStatistics,
  getRoomStatusStatistics,
  refreshDashboardCache,
  getSystemHealth,
  getOrderTrendsChart,
  getRevenueAnalysisChart,
  getOccupancyAnalysisChart,
  getPieChartData,
  getDashboardChartData,
  queryHistoricalOrders,
  aggregateHistoricalData,
  generateHistoricalReport,
  exportHistoricalData,
  getDataSnapshot
} from '@/services/dashboardService'

export const useDashboardStore = defineStore('dashboard', () => {
  // 状态
  const metrics = ref<DashboardMetrics | null>(null)
  const realTimeData = ref<RealTimeData | null>(null)
  const trendData = ref<TrendData | null>(null)
  const revenueStats = ref<RevenueStatistics | null>(null)
  const roomStatusStats = ref<Record<string, number> | null>(null)
  const systemHealth = ref<Record<string, string> | null>(null)

  // 图表数据
  const orderTrendChart = ref<OrderTrendChart | null>(null)
  const revenueChart = ref<RevenueChart | null>(null)
  const occupancyChart = ref<OccupancyChart | null>(null)
  const roomStatusPieChart = ref<PieChart | null>(null)
  const orderStatusPieChart = ref<PieChart | null>(null)
  const revenuePieChart = ref<PieChart | null>(null)
  const dashboardChartData = ref<DashboardChartData | null>(null)

  // 加载状态
  const loading = ref(false)
  const metricsLoading = ref(false)
  const realtimeLoading = ref(false)
  const chartLoading = ref(false)

  // 错误状态
  const error = ref<string | null>(null)

  // 自动刷新定时器
  let refreshTimer: NodeJS.Timeout | null = null
  let websocket: WebSocket | null = null
  const autoRefresh = ref(true)
  const refreshInterval = ref(30000) // 30秒
  const lastUpdateTime = ref<string>(new Date().toISOString())
  const isWebSocketConnected = ref(false)
  const reconnectAttempts = ref(0)
  const maxReconnectAttempts = 5
  const reconnectDelay = 1000 // 1秒

  // 计算属性
  const totalRevenue = computed(() => metrics.value?.totalRevenue || 0)
  const totalOrders = computed(() => metrics.value?.todayOrdersCount || 0)
  const occupancyRate = computed(() => metrics.value?.occupancyRate || 0)
  const averageRating = computed(() => metrics.value?.averageRating || 0)

  // 获取仪表板核心指标
  const fetchMetrics = async () => {
    try {
      metricsLoading.value = true
      const response = await getDashboardMetrics()
      if (response.success) {
        metrics.value = response.data
      }
    } catch (err) {
      console.error('获取仪表板指标失败:', err)
      error.value = '获取仪表板指标失败'
    } finally {
      metricsLoading.value = false
    }
  }

  // 获取实时数据
  const fetchRealTimeData = async () => {
    try {
      realtimeLoading.value = true
      const response = await getRealTimeData()
      if (response.success) {
        realTimeData.value = response.data
      }
    } catch (err) {
      console.error('获取实时数据失败:', err)
      error.value = '获取实时数据失败'
    } finally {
      realtimeLoading.value = false
    }
  }

  // 获取趋势数据
  const fetchTrendData = async (period: string = 'daily', days: number = 30) => {
    try {
      const response = await getTrendData(period, days)
      if (response.success) {
        trendData.value = response.data
      }
    } catch (err) {
      console.error('获取趋势数据失败:', err)
      error.value = '获取趋势数据失败'
    }
  }

  // 获取收入统计
  const fetchRevenueStatistics = async () => {
    try {
      const response = await getRevenueStatistics()
      if (response.success) {
        revenueStats.value = response.data
      }
    } catch (err) {
      console.error('获取收入统计失败:', err)
      error.value = '获取收入统计失败'
    }
  }

  // 获取房间状态统计
  const fetchRoomStatusStatistics = async () => {
    try {
      const response = await getRoomStatusStatistics()
      if (response.success) {
        roomStatusStats.value = response.data
      }
    } catch (err) {
      console.error('获取房间状态统计失败:', err)
      error.value = '获取房间状态统计失败'
    }
  }

  // 获取系统健康状态
  const fetchSystemHealth = async () => {
    try {
      const response = await getSystemHealth()
      if (response.success) {
        systemHealth.value = response.data
      }
    } catch (err) {
      console.error('获取系统健康状态失败:', err)
      error.value = '获取系统健康状态失败'
    }
  }

  // 获取订单趋势图表数据
  const fetchOrderTrendChart = async (startDate: string, endDate: string, period: string = 'daily') => {
    try {
      chartLoading.value = true
      const response = await getOrderTrendsChart(startDate, endDate, period)
      if (response.success) {
        orderTrendChart.value = response.data
      }
    } catch (err) {
      console.error('获取订单趋势图表数据失败:', err)
      error.value = '获取订单趋势图表数据失败'
    } finally {
      chartLoading.value = false
    }
  }

  // 获取收入分析图表数据
  const fetchRevenueChart = async (startDate: string, endDate: string, type: string = 'trends') => {
    try {
      chartLoading.value = true
      const response = await getRevenueAnalysisChart(startDate, endDate, type)
      if (response.success) {
        revenueChart.value = response.data
      }
    } catch (err) {
      console.error('获取收入分析图表数据失败:', err)
      error.value = '获取收入分析图表数据失败'
    } finally {
      chartLoading.value = false
    }
  }

  // 获取入住率图表数据
  const fetchOccupancyChart = async (startDate: string, endDate: string, type: string = 'trends') => {
    try {
      chartLoading.value = true
      const response = await getOccupancyAnalysisChart(startDate, endDate, type)
      if (response.success) {
        occupancyChart.value = response.data
      }
    } catch (err) {
      console.error('获取入住率图表数据失败:', err)
      error.value = '获取入住率图表数据失败'
    } finally {
      chartLoading.value = false
    }
  }

  // 获取饼图数据
  const fetchPieChart = async (dataType: string, startDate: string, endDate: string) => {
    try {
      const response = await getPieChartData(dataType, startDate, endDate)
      if (response.success) {
        switch (dataType) {
          case 'room_status':
            roomStatusPieChart.value = response.data
            break
          case 'order_status':
            orderStatusPieChart.value = response.data
            break
          case 'revenue_by_room_type':
            revenuePieChart.value = response.data
            break
        }
      }
    } catch (err) {
      console.error('获取饼图数据失败:', err)
      error.value = '获取饼图数据失败'
    }
  }

  // 获取仪表板综合图表数据
  const fetchDashboardChartData = async (startDate: string, endDate: string) => {
    try {
      chartLoading.value = true
      const response = await getDashboardChartData(startDate, endDate)
      if (response.success) {
        dashboardChartData.value = response.data
        // 分别设置各个图表数据
        orderTrendChart.value = response.data.orderTrendChart
        revenueChart.value = response.data.revenueChart
        occupancyChart.value = response.data.occupancyChart
        roomStatusPieChart.value = response.data.roomStatusPieChart
        orderStatusPieChart.value = response.data.orderStatusPieChart
        revenuePieChart.value = response.data.revenuePieChart
      }
    } catch (err) {
      console.error('获取仪表板综合图表数据失败:', err)
      error.value = '获取仪表板综合图表数据失败'
    } finally {
      chartLoading.value = false
    }
  }

  // 刷新缓存
  const refreshCache = async () => {
    try {
      const response = await refreshDashboardCache()
      if (response.success) {
        // 重新获取所有数据
        await fetchAllData()
      }
    } catch (err) {
      console.error('刷新缓存失败:', err)
      error.value = '刷新缓存失败'
    }
  }

  // 获取所有数据
  const fetchAllData = async () => {
    loading.value = true
    error.value = null

    try {
      await Promise.all([
        fetchMetrics(),
        fetchRealTimeData(),
        fetchRevenueStatistics(),
        fetchRoomStatusStatistics(),
        fetchSystemHealth()
      ])

      // 获取默认时间段（最近30天）的图表数据
      const endDate = new Date().toISOString().split('T')[0]
      const startDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]

      await Promise.all([
        fetchDashboardChartData(startDate, endDate),
        fetchTrendData('daily', 30)
      ])
    } catch (err) {
      console.error('获取仪表板数据失败:', err)
    } finally {
      loading.value = false
    }
  }

  // WebSocket连接管理
  const connectWebSocket = () => {
    if (websocket && websocket.readyState === WebSocket.OPEN) {
      return
    }

    try {
      // 根据当前环境构建WebSocket URL
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = `${protocol}//${window.location.host}/ws/dashboard`

      websocket = new WebSocket(wsUrl)

      websocket.onopen = () => {
        console.log('WebSocket连接已建立')
        isWebSocketConnected.value = true
        reconnectAttempts.value = 0

        // 发送心跳
        const heartbeatInterval = setInterval(() => {
          if (websocket?.readyState === WebSocket.OPEN) {
            websocket.send(JSON.stringify({ type: 'heartbeat' }))
          } else {
            clearInterval(heartbeatInterval)
          }
        }, 30000) // 30秒心跳
      }

      websocket.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          handleWebSocketMessage(data)
        } catch (error) {
          console.error('解析WebSocket消息失败:', error)
        }
      }

      websocket.onclose = () => {
        console.log('WebSocket连接已关闭')
        isWebSocketConnected.value = false
        attemptReconnect()
      }

      websocket.onerror = (error) => {
        console.error('WebSocket错误:', error)
        isWebSocketConnected.value = false
      }
    } catch (error) {
      console.error('建立WebSocket连接失败:', error)
      // WebSocket连接失败时，回退到定时刷新
      startAutoRefresh()
    }
  }

  // WebSocket重连机制
  const attemptReconnect = () => {
    if (reconnectAttempts.value < maxReconnectAttempts) {
      reconnectAttempts.value++
      const delay = reconnectDelay * Math.pow(2, reconnectAttempts.value - 1) // 指数退避

      console.log(`${delay}ms后尝试第${reconnectAttempts.value}次重连`)
      setTimeout(connectWebSocket, delay)
    } else {
      console.error('WebSocket重连失败，回退到定时刷新')
      startAutoRefresh()
    }
  }

  // 处理WebSocket消息
  const handleWebSocketMessage = (data: any) => {
    lastUpdateTime.value = new Date().toISOString()

    switch (data.type) {
      case 'realtime_update':
        if (data.realTimeData) {
          realTimeData.value = { ...realTimeData.value, ...data.realTimeData }
        }
        break

      case 'metrics_update':
        if (data.metrics) {
          metrics.value = { ...metrics.value, ...data.metrics }
        }
        break

      case 'new_order':
        // 新订单通知
        if (data.order) {
          realTimeData.value?.recentOrders?.unshift(data.order)
          if (realTimeData.value?.recentOrders?.length > 10) {
            realTimeData.value.recentOrders = realTimeData.value.recentOrders.slice(0, 10)
          }
        }
        break

      case 'room_status_change':
        // 房间状态变化
        if (data.roomStatus) {
          if (realTimeData.value) {
            realTimeData.value.roomStatusCounts = {
              ...realTimeData.value.roomStatusCounts,
              ...data.roomStatus
            }
          }
        }
        break

      case 'system_alert':
        // 系统告警
        if (data.message) {
          error.value = data.message
        }
        break
    }
  }

  // 关闭WebSocket连接
  const disconnectWebSocket = () => {
    if (websocket) {
      websocket.close()
      websocket = null
    }
    isWebSocketConnected.value = false
  }

  // 启动自动刷新
  const startAutoRefresh = () => {
    // 如果WebSocket已连接，优先使用WebSocket
    if (isWebSocketConnected.value) {
      return
    }

    if (refreshTimer) {
      clearInterval(refreshTimer)
    }

    if (autoRefresh.value) {
      refreshTimer = setInterval(async () => {
        try {
          await fetchRealTimeData()
          lastUpdateTime.value = new Date().toISOString()

          // 每5分钟刷新一次核心指标
          const now = Date.now()
          const lastMetricsUpdate = metrics.value ? new Date(lastUpdateTime.value).getTime() : 0
          if (now - lastMetricsUpdate > 5 * 60 * 1000) {
            await fetchMetrics()
          }
        } catch (error) {
          console.error('自动刷新失败:', error)
        }
      }, refreshInterval.value)
    }
  }

  // 停止自动刷新
  const stopAutoRefresh = () => {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
    disconnectWebSocket()
  }

  // 设置自动刷新
  const setAutoRefresh = (enabled: boolean, interval?: number) => {
    autoRefresh.value = enabled
    if (interval) {
      refreshInterval.value = interval
    }

    if (enabled) {
      // 优先尝试WebSocket连接
      connectWebSocket()
      // 如果WebSocket连接失败，startAutoRefresh会自动启用定时刷新
    } else {
      stopAutoRefresh()
    }
  }

  // 手动刷新
  const manualRefresh = async (dataType?: string) => {
    try {
      error.value = null

      switch (dataType) {
        case 'realtime':
          await fetchRealTimeData()
          break
        case 'metrics':
          await fetchMetrics()
          break
        case 'charts':
          const endDate = new Date().toISOString().split('T')[0]
          const startDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
          await fetchDashboardChartData(startDate, endDate)
          break
        default:
          await fetchAllData()
      }

      lastUpdateTime.value = new Date().toISOString()
    } catch (error) {
      console.error('手动刷新失败:', error)
      error.value = '刷新失败，请稍后重试'
    }
  }

  // 检查连接状态
  const getConnectionStatus = () => {
    return {
      isWebSocketConnected: isWebSocketConnected.value,
      lastUpdateTime: lastUpdateTime.value,
      autoRefreshEnabled: autoRefresh.value,
      refreshInterval: refreshInterval.value
    }
  }

  // 清除错误
  const clearError = () => {
    error.value = null
  }

  return {
    // 状态
    metrics,
    realTimeData,
    trendData,
    revenueStats,
    roomStatusStats,
    systemHealth,
    orderTrendChart,
    revenueChart,
    occupancyChart,
    roomStatusPieChart,
    orderStatusPieChart,
    revenuePieChart,
    dashboardChartData,
    loading,
    metricsLoading,
    realtimeLoading,
    chartLoading,
    error,
    autoRefresh,
    refreshInterval,
    lastUpdateTime,
    isWebSocketConnected,

    // 计算属性
    totalRevenue,
    totalOrders,
    occupancyRate,
    averageRating,

    // 方法
    fetchMetrics,
    fetchRealTimeData,
    fetchTrendData,
    fetchRevenueStatistics,
    fetchRoomStatusStatistics,
    fetchSystemHealth,
    fetchOrderTrendChart,
    fetchRevenueChart,
    fetchOccupancyChart,
    fetchPieChart,
    fetchDashboardChartData,
    fetchAllData,
    refreshCache,
    startAutoRefresh,
    stopAutoRefresh,
    setAutoRefresh,
    manualRefresh,
    getConnectionStatus,
    connectWebSocket,
    disconnectWebSocket,
    clearError
  }
})