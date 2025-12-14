export interface OrderReportDTO {
  startDate: string
  endDate: string
  totalOrders: number
  totalRevenue: number
  averageOrderValue: number
  ordersByStatus: Record<string, number>
  ordersByRoomType: Record<string, number>
  ordersByDate: Record<string, number>
  revenueByMonth: Record<string, number>
  ordersByChannel: Record<string, number>
  completionRate: number
  cancellationRate: number
  orderTrends: OrderTrendData[]
  roomTypePreferences: RoomTypePreference[]
}

export interface OrderTrendData {
  date: string
  orderCount: number
  revenue: number
}

export interface RoomTypePreference {
  roomTypeName: string
  orderCount: number
  revenue: number
  percentage: number
}

export interface RevenueReportDTO {
  startDate: string
  endDate: string
  totalRevenue: number
  monthlyRevenue: Record<string, number>
  revenueByRoomType: Record<string, number>
  averageDailyRate: number
  revenuePerAvailableRoom: number
  occupancyRate: number
  revenueGrowthRate: number
  dailyRevenueTrends: DailyRevenueData[]
  roomTypeRevenueContributions: RoomTypeRevenueContribution[]
  revenueForecasts: RevenueForecast[]
}

export interface DailyRevenueData {
  date: string
  revenue: number
  orderCount: number
  averageOrderValue: number
}

export interface RoomTypeRevenueContribution {
  roomTypeName: string
  revenue: number
  percentage: number
  orderCount: number
  averageOrderValue: number
}

export interface RevenueForecast {
  period: string
  predictedRevenue: number
  confidenceLevel: number
  growthRate: number
}

export interface UserReportDTO {
  startDate: string
  endDate: string
  totalUsers: number
  newUsersByMonth: Record<string, number>
  activeUsers: number
  userRetentionRate: number
  userConversionRate: number
  usersByRole: Record<string, number>
  topUsersByOrders: UserOrderSummary[]
  topUsersBySpending: UserSpendingSummary[]
  userRegistrationTrends: UserRegistrationTrend[]
  userBehaviorAnalysis: UserBehaviorAnalysis
}

export interface UserOrderSummary {
  userId: number
  username: string
  email: string
  orderCount: number
  lastOrderDate: string
}

export interface UserSpendingSummary {
  userId: number
  username: string
  email: string
  totalSpending: number
  orderCount: number
  averageOrderValue: number
}

export interface UserRegistrationTrend {
  date: string
  newUserCount: number
  cumulativeUserCount: number
}

export interface UserBehaviorAnalysis {
  averageOrdersPerUser: number
  averageSpendingPerUser: number
  repeatPurchaseRate: number
  averageBookingTime: string
  bookingTimeDistribution: Record<string, number>
}

export interface RoomReportDTO {
  startDate: string
  endDate: string
  totalRooms: number
  occupancyRate: number
  averageRoomRate: number
  roomUtilization: Record<string, number>
  revenueByRoomType: Record<string, number>
  topPerformingRooms: RoomPerformance[]
  maintenanceRooms: number
  availableRooms: number
  roomUtilizationTrends: RoomUtilizationTrend[]
  roomTypePerformances: RoomTypePerformance[]
  maintenanceStats: RoomMaintenanceStats
}

export interface RoomPerformance {
  roomId: number
  roomNumber: string
  roomTypeName: string
  totalOrders: number
  totalRevenue: number
  averageRevenuePerOrder: number
  occupancyRate: number
  totalNights: number
  revenuePerNight: number
}

export interface RoomUtilizationTrend {
  date: string
  totalRooms: number
  occupiedRooms: number
  availableRooms: number
  occupancyRate: number
  dailyRevenue: number
}

export interface RoomTypePerformance {
  roomTypeName: string
  totalRooms: number
  totalOrders: number
  totalRevenue: number
  occupancyRate: number
  averageDailyRate: number
  revenuePerAvailableRoom: number
  revenueContribution: number
}

export interface RoomMaintenanceStats {
  currentlyUnderMaintenance: number
  totalMaintenanceDays: number
  maintenanceReasons: Record<string, number>
  recentMaintenanceRecords: MaintenanceRecord[]
}

export interface MaintenanceRecord {
  roomId: number
  roomNumber: string
  reason: string
  startDate: string
  endDate: string
  durationDays: number
}

export interface ReportOverviewDTO {
  lastUpdated: string
  todayOrders: number
  todayRevenue: number
  currentOccupancyRate: number
  monthlyNewUsers: number
  monthlyRevenue: number
  availableRooms: number
  maintenanceRooms: number
  activeUsers: number
  monthlyCompletionRate: number
  averageDailyRate: number
}

export interface ReportExportRequest {
  reportType: 'ORDER' | 'REVENUE' | 'USER' | 'ROOM'
  exportFormat: 'EXCEL' | 'PDF'
  startDate: string
  endDate: string
  roomTypeId?: number
  orderStatus?: string
  includeCharts?: boolean
  includeDetailedData?: boolean
}

export interface ReportFilters {
  startDate: string
  endDate: string
  roomTypeId?: number
  orderStatus?: string
}