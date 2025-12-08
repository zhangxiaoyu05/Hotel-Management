import { apiClient } from '@/utils/apiClient'

export interface DetectConflictRequest {
  roomId: number
  userId: number
  checkInDate: string
  checkOutDate: string
}

export interface ConflictDetectionResult {
  hasConflict: boolean
  conflictId?: number
  conflictType?: string
  conflictingOrderId?: number
  message?: string
  alternativeRooms?: AlternativeRoom[]
  suggestions?: string[]
}

export interface AlternativeRoom {
  roomId: number
  roomNumber: string
  roomType: string
  description?: string
  price: number
  originalPrice?: number
  discount?: number
  available: boolean
  imageUrl?: string
}

export interface JoinWaitingListRequest {
  roomId: number
  userId?: number
  checkInDate: string
  checkOutDate: string
  guestCount: number
  specialRequests?: string
}

export interface WaitingListResponse {
  waitingListId: number
  roomId: number
  roomNumber?: string
  roomType?: string
  status: string
  priority: number
  requestedCheckInDate: string
  requestedCheckOutDate: string
  guestCount: number
  estimatedWaitTime?: number
  currentPosition?: number
  notifiedAt?: string
  expiresAt?: string
  createdAt: string
  specialRequests?: string
}

export interface ConfirmWaitingListRequest {
  orderId: number
  specialRequests?: string
  notifyUser?: boolean
}

export interface WaitingListQueryRequest {
  userId?: number
  roomId?: number
  status?: string
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: string
}

export interface ConflictStatisticsRequest {
  roomId?: number
  startDate?: string
  endDate?: string
  groupBy?: string
}

export interface ConflictStatisticsResponse {
  totalConflicts: number
  timeOverlapConflicts: number
  doubleBookingConflicts: number
  concurrentRequestConflicts: number
  resolvedConflicts: number
  waitingListSize: number
  resolutionRate: number
  conflictTrends?: ConflictTrend[]
  conflictsByRoom?: Record<string, number>
  conflictsByType?: Record<string, number>
  roomHotspots?: RoomConflictHotspot[]
}

export interface ConflictTrend {
  period: string
  conflictCount: number
  resolvedCount: number
  resolutionRate: number
}

export interface RoomConflictHotspot {
  roomId: number
  roomNumber: string
  conflictCount: number
  waitingListSize: number
  conflictRate: number
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  code?: number
}

class BookingConflictService {
  // 检测预订冲突
  async detectConflict(request: DetectConflictRequest): Promise<ApiResponse<ConflictDetectionResult>> {
    try {
      const response = await apiClient.post('/v1/booking-conflicts/detect', request)
      return response.data
    } catch (error) {
      console.error('Detect conflict error:', error)
      throw error
    }
  }

  // 加入等待列表
  async joinWaitingList(request: JoinWaitingListRequest): Promise<ApiResponse<WaitingListResponse>> {
    try {
      const response = await apiClient.post('/v1/booking-conflicts/waiting-list', request)
      return response.data
    } catch (error) {
      console.error('Join waiting list error:', error)
      throw error
    }
  }

  // 获取用户的等待列表
  async getWaitingList(query: WaitingListQueryRequest): Promise<ApiResponse<{
    records: WaitingListResponse[]
    total: number
    size: number
    current: number
    pages: number
  }>> {
    try {
      const response = await apiClient.get('/v1/booking-conflicts/waiting-list', { params: query })
      return response.data
    } catch (error) {
      console.error('Get waiting list error:', error)
      throw error
    }
  }

  // 确认等待列表预订
  async confirmWaitingListBooking(
    waitingListId: number,
    request: ConfirmWaitingListRequest
  ): Promise<ApiResponse<any>> {
    try {
      const response = await apiClient.put(`/v1/booking-conflicts/waiting-list/${waitingListId}/confirm`, request)
      return response.data
    } catch (error) {
      console.error('Confirm waiting list booking error:', error)
      throw error
    }
  }

  // 退出等待列表
  async leaveWaitingList(waitingListId: number): Promise<ApiResponse<string>> {
    try {
      const response = await apiClient.delete(`/v1/booking-conflicts/waiting-list/${waitingListId}`)
      return response.data
    } catch (error) {
      console.error('Leave waiting list error:', error)
      throw error
    }
  }

  // 获取冲突统计
  async getConflictStatistics(query: ConflictStatisticsRequest): Promise<ApiResponse<ConflictStatisticsResponse>> {
    try {
      const response = await apiClient.get('/v1/booking-conflicts/statistics', { params: query })
      return response.data
    } catch (error) {
      console.error('Get conflict statistics error:', error)
      throw error
    }
  }

  // 获取冲突列表
  async getConflicts(params: {
    roomId?: number
    status?: string
    conflictType?: string
    page?: number
    size?: number
  }): Promise<ApiResponse<{
    records: any[]
    total: number
    size: number
    current: number
    pages: number
  }>> {
    try {
      const response = await apiClient.get('/v1/booking-conflicts/conflicts', { params })
      return response.data
    } catch (error) {
      console.error('Get conflicts error:', error)
      throw error
    }
  }

  // 清理过期的等待列表
  async cleanupExpiredWaitingList(): Promise<ApiResponse<string>> {
    try {
      const response = await apiClient.post('/v1/booking-conflicts/cleanup-expired')
      return response.data
    } catch (error) {
      console.error('Cleanup expired waiting list error:', error)
      throw error
    }
  }

  // 通过订单API检查冲突
  async checkBookingConflict(roomId: number, checkInDate: string, checkOutDate: string): Promise<ApiResponse<ConflictDetectionResult>> {
    try {
      const response = await apiClient.get('/v1/orders/check-conflict', {
        params: { roomId, checkInDate, checkOutDate }
      })
      return response.data
    } catch (error) {
      console.error('Check booking conflict error:', error)
      throw error
    }
  }
}

export const bookingConflictService = new BookingConflictService()

export default bookingConflictService