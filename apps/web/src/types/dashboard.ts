/**
 * 仪表板相关类型定义
 */

// 仪表板核心指标
export interface DashboardMetrics {
  todayOrdersCount: number
  todayRevenue: number
  occupancyRate: number
  totalActiveUsers: number
  pendingReviewsCount: number
  averageRating: number
  totalRooms: number
  reservedRooms: number
  availableRooms: number
  maintenanceRooms: number
  monthlyRevenue: number
  monthlyOrdersCount: number
  todayNewUsers: number
}

// 实时数据
export interface RealTimeData {
  recentOrders: OrderSummary[]
  roomStatusCounts: Record<string, number>
  onlineUsersCount: number
  activeUsersCount: number
  todayNewUsersCount: number
  lastUpdateTime: string
  pendingOrdersCount: number
  pendingCheckInCount: number
  pendingCheckOutCount: number
  activeOrdersCount: number
  systemStatus: string
  databaseStatus: string
  cacheStatus: string
}

// 订单摘要
export interface OrderSummary {
  id: number
  orderNumber: string
  userId: number
  username: string
  roomId: number
  roomNumber: string
  roomTypeName: string
  hotelId: number
  hotelName: string
  checkInDate: string
  checkOutDate: string
  totalPrice: number
  status: string
  statusDesc: string
  createdAt: string
  updatedAt: string
  paymentMethod: string
  paymentStatus: string
  guestCount: number
  contactName: string
  contactPhone: string
}

// 趋势数据
export interface TrendData {
  period: string
  dates: string[]
  orderCounts: number[]
  revenues: number[]
  occupancies: number[]
  newUsersCounts: number[]
  totalOrders: number
  totalRevenue: number
  averageOccupancy: number
  averageDailyOrders: number
  averageDailyRevenue: number
  ordersGrowthRate: number
  revenueGrowthRate: number
  occupancyGrowthRate: number
}

// 收入统计
export interface RevenueStatistics {
  totalRevenue: number
  todayRevenue: number
  weeklyRevenue: number
  monthlyRevenue: number
  yearlyRevenue: number
  yesterdayRevenue: number
  lastWeekRevenue: number
  lastMonthRevenue: number
  lastYearRevenue: number
  dailyGrowthRate: number
  weeklyGrowthRate: number
  monthlyGrowthRate: number
  yearlyGrowthRate: number
  revenueByRoomType: Record<string, number>
  revenueByPaymentMethod: Record<string, number>
  revenueBySource: Record<string, number>
  averageOrderValue: number
  projectedMonthlyRevenue: number
  projectedYearlyRevenue: number
  revenueTargetCompletion: number
}

// 图表系列数据
export interface ChartSeries {
  name: string
  data: number[]
  color: string
  type: string
  stacked?: boolean
  yAxis?: number
  smooth?: boolean
}

// 饼图数据项
export interface PieDataItem {
  name: string
  value: number
  color: string
}

// 热力图数据项
export interface HeatmapDataItem {
  x: string
  y: string
  value: number
}

// 订单趋势图表数据
export interface OrderTrendChart {
  title: string
  subtitle: string
  categories: string[]
  legend: string[]
  series: ChartSeries[]
  tooltip: boolean
  showLegend: boolean
  theme: string
}

// 收入图表数据
export interface RevenueChart {
  title: string
  subtitle: string
  categories: string[]
  legend: string[]
  series: ChartSeries[]
  tooltip: boolean
  showLegend: boolean
  yAxisTitle: string
  dataFormat: string
}

// 入住率图表数据
export interface OccupancyChart {
  title: string
  subtitle: string
  categories: string[]
  legend: string[]
  series: ChartSeries[]
  heatmapData: number[][][][]
  weekDays: string[]
  tooltip: boolean
  showLegend: boolean
  yAxisTitle: string
  yAxisMax: number
  dataFormat: string
}

// 饼图数据
export interface PieChart {
  title: string
  subtitle: string
  data: PieDataItem[]
  showLabels: boolean
  showLegend: boolean
  tooltip: boolean
  radius: string
  innerRadius: string
  donut: boolean
  labelFormat: string
}

// 仪表板图表数据汇总
export interface DashboardChartData {
  title: string
  period: string
  orderTrendChart: OrderTrendChart
  revenueChart: RevenueChart
  occupancyChart: OccupancyChart
  roomStatusPieChart: PieChart
  orderStatusPieChart: PieChart
  revenuePieChart: PieChart
  lastUpdateTime: string
  cacheStatus: string
}

// 历史数据查询条件
export interface HistoricalDataQuery {
  startDate?: string
  endDate?: string
  statuses?: string[]
  minAmount?: number
  maxAmount?: number
  userId?: number
  roomId?: number
  roomTypeId?: number
  hotelId?: number
  page: number
  size: number
  sortBy: string
  sortOrder: string
  includeDeleted: boolean
}

// 历史数据聚合结果
export interface HistoricalDataAggregation {
  totalOrders: number
  totalRevenue: number
  totalRooms: number
  totalUsers: number
  orderStatusDistribution: Record<string, number>
  roomStatusDistribution: Record<string, number>
  userRoleDistribution: Record<string, number>
  userStatusDistribution: Record<string, number>
  orderAmountDistribution: Record<string, number>
  averageOrderAmount: number
  averageOccupancyRate: number
  timeRange: string
  lastUpdateTime: string
}

// API响应通用格式
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  success: boolean
  timestamp: number
}