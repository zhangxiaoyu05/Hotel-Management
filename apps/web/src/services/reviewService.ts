import { apiClient } from '@/utils/apiClient'

export interface ReviewRequest {
  orderId: number
  overallRating: number
  cleanlinessRating: number
  serviceRating: number
  facilitiesRating: number
  locationRating: number
  comment: string
  images?: string[]
  isAnonymous?: boolean
}

// 激励系统相关类型定义
export interface ReviewActivity {
  id: number
  title: string
  description: string
  activityType: string
  startDate: string
  endDate: string
  status: string
  rules: Record<string, any>
  isActive: boolean
  createdBy: number
  createdAt: string
}

export interface LeaderboardEntry {
  rank: number
  userId: number
  userName: string
  totalReviews: number
  qualityScore: number
  totalPoints: number
  qualityReviews?: number
}

export interface ReviewLeaderboard {
  periodType: string
  period: string
  updatedAt: string
  entries: LeaderboardEntry[]
}

export interface ActivityParticipation {
  id: number
  userId: number
  activityId: number
  joinedAt: string
  status: string
  activitySnapshot?: string
  rewardPoints?: number
  rewardAt?: string
  notes?: string
}

export interface HighQualityBadge {
  id: number
  reviewId: number
  badgeType: string
  displayName: string
  description: string
  iconUrl: string
  awardedAt: string
}

export interface ReviewResponse {
  id: number
  userId: number
  orderId: number
  roomId: number
  hotelId: number
  overallRating: number
  cleanlinessRating: number
  serviceRating: number
  facilitiesRating: number
  locationRating: number
  comment: string
  images: string[]
  isAnonymous: boolean
  status: string
  createdAt: string
}

export interface ReviewQueryRequest {
  hotelId: number
  roomId?: number
  minRating?: number
  maxRating?: number
  hasImages?: boolean
  sortBy?: 'date' | 'rating'
  sortOrder?: 'asc' | 'desc'
  page?: number
  size?: number
}

export interface ReviewListResponse {
  reviews: ReviewResponse[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface ReviewStatisticsResponse {
  hotelId: number
  totalReviews: number
  overallRating: number
  cleanlinessRating: number
  serviceRating: number
  facilitiesRating: number
  locationRating: number
  ratingDistribution: {
    rating5: number
    rating4: number
    rating3: number
    rating2: number
    rating1: number
  }
  reviewsWithImages: number
  averageCommentLength: number
}

// 管理功能相关接口
export interface ReviewModerationRequest {
  action: 'APPROVE' | 'REJECT' | 'MARK' | 'HIDE' | 'DELETE'
  reason: string
}

export interface ReviewReplyRequest {
  content: string
  status: 'DRAFT' | 'PUBLISHED'
}

export interface BatchModerationRequest {
  reviewIds: number[]
  action: 'APPROVE' | 'REJECT' | 'HIDE'
  reason: string
}

export interface ReviewAnalyticsRequest {
  startDate?: string
  endDate?: string
  hotelId?: number
  groupBy?: 'day' | 'week' | 'month'
  metricType?: 'count' | 'rating' | 'status'
}

export interface ReviewReplyResponse {
  id: number
  reviewId: number
  adminId: number
  adminName: string
  content: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface ReviewModerationLogResponse {
  id: number
  reviewId: number
  adminId: number
  adminName: string
  action: string
  reason: string
  oldStatus: string
  newStatus: string
  createdAt: string
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
}

class ReviewService {
  // 缓存
  private static cache = new Map<string, any>()
  private static cacheTimeout = 5 * 60 * 1000 // 5分钟缓存

  /**
   * 获取缓存数据
   */
  private static getCacheKey(endpoint: string, params?: any): string {
    return `${endpoint}${params ? JSON.stringify(params) : ''}`
  }

  /**
   * 检查缓存是否有效
   */
  private static isCacheValid(timestamp: number): boolean {
    return Date.now() - timestamp < this.cacheTimeout
  }

  /**
   * 设置缓存
   */
  private static setCache(key: string, data: any): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    })
  }

  /**
   * 获取缓存
   */
  private static getCache(key: string): any {
    const cached = this.cache.get(key)
    if (cached && this.isCacheValid(cached.timestamp)) {
      return cached.data
    }
    this.cache.delete(key)
    return null
  }

  /**
   * 清除特定酒店的所有缓存
   */
  public static clearHotelCache(hotelId: number): void {
    const keysToDelete: string[] = []
    this.cache.forEach((_, key) => {
      if (key.includes(`hotelId":${hotelId}`)) {
        keysToDelete.push(key)
      }
    })
    keysToDelete.forEach(key => this.cache.delete(key))
  }

  /**
   * 提交评价
   */
  async submitReview(reviewData: ReviewRequest): Promise<ApiResponse<ReviewResponse>> {
    try {
      const response = await apiClient.post('/v1/reviews', reviewData)

      // 清除相关缓存
      if (reviewData.orderId) {
        // 需要获取hotelId来清除缓存，这里简化处理
        ReviewService.clearHotelCache(0) // 这里应该传入实际的hotelId
      }

      return response.data
    } catch (error: any) {
      console.error('提交评价失败:', error)
      throw error
    }
  }

  /**
   * 获取当前用户的评价列表
   */
  async getMyReviews(): Promise<ApiResponse<ReviewResponse[]>> {
    try {
      const response = await apiClient.get('/v1/reviews/my')
      return response.data
    } catch (error: any) {
      console.error('获取评价列表失败:', error)
      throw error
    }
  }

  /**
   * 获取酒店的评价列表
   */
  async getHotelReviews(hotelId: number): Promise<ApiResponse<ReviewResponse[]>> {
    try {
      const response = await apiClient.get(`/v1/reviews/hotel/${hotelId}`)
      return response.data
    } catch (error: any) {
      console.error('获取酒店评价失败:', error)
      throw error
    }
  }

  /**
   * 上传评价图片
   */
  async uploadReviewImages(files: File[]): Promise<string[]> {
    const uploadPromises = files.map(file => this.uploadImage(file))
    try {
      const results = await Promise.all(uploadPromises)
      return results.map(result => result.data.url)
    } catch (error: any) {
      console.error('图片上传失败:', error)
      throw error
    }
  }

  /**
   * 单个图片上传
   */
  private async uploadImage(file: File): Promise<ApiResponse<{ url: string }>> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', 'review')

    try {
      const response = await apiClient.post('/v1/files/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      return response.data
    } catch (error: any) {
      console.error('图片上传失败:', error)
      throw error
    }
  }

  /**
   * 验证评价数据
   */
  validateReviewData(data: ReviewRequest): { isValid: boolean; errors: string[] } {
    const errors: string[] = []

    // 验证订单ID
    if (!data.orderId || data.orderId <= 0) {
      errors.push('请选择有效的订单')
    }

    // 验证评分
    const ratings = [
      { name: '总体评价', value: data.overallRating },
      { name: '清洁度', value: data.cleanlinessRating },
      { name: '服务态度', value: data.serviceRating },
      { name: '设施设备', value: data.facilitiesRating },
      { name: '地理位置', value: data.locationRating }
    ]

    for (const rating of ratings) {
      if (!rating.value || rating.value < 1 || rating.value > 5) {
        errors.push(`${rating.name}评分必须在1-5星之间`)
      }
    }

    // 验证评价内容
    if (!data.comment || data.comment.trim().length === 0) {
      errors.push('请填写评价内容')
    } else if (data.comment.trim().length < 10) {
      errors.push('评价内容至少需要10个字符')
    } else if (data.comment.length > 1000) {
      errors.push('评价内容不能超过1000字符')
    }

    // 验证图片数量
    if (data.images && data.images.length > 5) {
      errors.push('最多只能上传5张图片')
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  }

  /**
   * 获取评价统计信息
   */
  async getHotelReviewStats(hotelId: number): Promise<ApiResponse<{
    averageRating: number
    totalReviews: number
    ratingDistribution: {
      1: number
      2: number
      3: number
      4: number
      5: number
    }
  }>> {
    try {
      const response = await apiClient.get(`/v1/reviews/hotel/${hotelId}/stats`)
      return response.data
    } catch (error: any) {
      console.error('获取评价统计失败:', error)
      throw error
    }
  }

  /**
   * 检查订单是否可以评价
   */
  async canReviewOrder(orderId: number): Promise<ApiResponse<{
    canReview: boolean
    reason?: string
  }>> {
    try {
      const response = await apiClient.get(`/v1/reviews/can-review/${orderId}`)
      return response.data
    } catch (error: any) {
      console.error('检查评价权限失败:', error)
      throw error
    }
  }

  /**
   * 分页查询评价列表（支持多种筛选条件）
   */
  async queryReviews(queryRequest: ReviewQueryRequest): Promise<ApiResponse<ReviewListResponse>> {
    const cacheKey = ReviewService.getCacheKey('/v1/reviews/query', queryRequest)
    const cached = ReviewService.getCache(cacheKey)

    if (cached) {
      return Promise.resolve(cached)
    }

    try {
      const response = await apiClient.post('/v1/reviews/query', queryRequest)
      ReviewService.setCache(cacheKey, response.data)
      return response.data
    } catch (error: any) {
      console.error('查询评价列表失败:', error)
      throw error
    }
  }

  /**
   * 获取酒店最新评价
   */
  async getRecentReviews(hotelId: number, limit: number = 5): Promise<ApiResponse<ReviewResponse[]>> {
    const cacheKey = ReviewService.getCacheKey('/v1/reviews/recent', { hotelId, limit })
    const cached = ReviewService.getCache(cacheKey)

    if (cached) {
      return Promise.resolve(cached)
    }

    try {
      const response = await apiClient.get(`/v1/reviews/recent/${hotelId}?limit=${limit}`)
      ReviewService.setCache(cacheKey, response.data)
      return response.data
    } catch (error: any) {
      console.error('获取最新评价失败:', error)
      throw error
    }
  }

  /**
   * 获取带图片的评价
   */
  async getReviewsWithImages(hotelId: number, limit: number = 10): Promise<ApiResponse<ReviewResponse[]>> {
    const cacheKey = ReviewService.getCacheKey('/v1/reviews/with-images', { hotelId, limit })
    const cached = ReviewService.getCache(cacheKey)

    if (cached) {
      return Promise.resolve(cached)
    }

    try {
      const response = await apiClient.get(`/v1/reviews/with-images/${hotelId}?limit=${limit}`)
      ReviewService.setCache(cacheKey, response.data)
      return response.data
    } catch (error: any) {
      console.error('获取带图片评价失败:', error)
      throw error
    }
  }

  /**
   * 获取酒店评价统计
   */
  async getHotelStatistics(hotelId: number): Promise<ApiResponse<ReviewStatisticsResponse>> {
    const cacheKey = ReviewService.getCacheKey('/v1/reviews/statistics', { hotelId })
    const cached = ReviewService.getCache(cacheKey)

    if (cached) {
      return Promise.resolve(cached)
    }

    try {
      const response = await apiClient.get(`/v1/reviews/statistics/${hotelId}`)
      ReviewService.setCache(cacheKey, response.data)
      return response.data
    } catch (error: any) {
      console.error('获取酒店评价统计失败:', error)
      throw error
    }
  }

  /**
   * 获取简单统计信息
   */
  async getSimpleStatistics(hotelId: number): Promise<ApiResponse<{
    totalReviews: number
    overallRating: number
  }>> {
    const cacheKey = ReviewService.getCacheKey('/v1/reviews/statistics/simple', { hotelId })
    const cached = ReviewService.getCache(cacheKey)

    if (cached) {
      return Promise.resolve(cached)
    }

    try {
      const response = await apiClient.get(`/v1/reviews/statistics/${hotelId}/simple`)
      ReviewService.setCache(cacheKey, response.data)
      return response.data
    } catch (error: any) {
      console.error('获取简单统计信息失败:', error)
      throw error
    }
  }

  /**
   * 批量获取酒店统计
   */
  async getBatchStatistics(hotelIds: number[]): Promise<ApiResponse<Record<number, {
    totalReviews: number
    overallRating: number
  }>>> {
    try {
      const response = await apiClient.post('/v1/reviews/statistics/batch', hotelIds)
      return response.data
    } catch (error: any) {
      console.error('获取批量统计失败:', error)
      throw error
    }
  }

  // ========== 管理功能 API ==========

  /**
   * 审核单个评价
   */
  async moderateReview(reviewId: number, request: ReviewModerationRequest): Promise<ApiResponse<ReviewResponse>> {
    try {
      const response = await apiClient.put(`/v1/admin/reviews/${reviewId}/moderate`, request)
      return response.data
    } catch (error: any) {
      console.error('审核评价失败:', error)
      throw error
    }
  }

  /**
   * 批量审核评价
   */
  async batchModerateReviews(request: BatchModerationRequest): Promise<ApiResponse<ReviewResponse[]>> {
    try {
      const response = await apiClient.put('/v1/admin/reviews/batch-moderate', request)
      return response.data
    } catch (error: any) {
      console.error('批量审核失败:', error)
      throw error
    }
  }

  /**
   * 获取待审核评价列表
   */
  async getPendingReviews(): Promise<ApiResponse<ReviewResponse[]>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/pending')
      return response.data
    } catch (error: any) {
      console.error('获取待审核评价失败:', error)
      throw error
    }
  }

  /**
   * 获取管理评价列表（支持筛选）
   */
  async getReviewsForManagement(params: {
    status?: string
    hotelId?: number
    userId?: number
    startDate?: string
    endDate?: string
    page?: number
    size?: number
    sortBy?: string
    sortDir?: string
  }): Promise<ApiResponse<{
    content: ReviewResponse[]
    totalElements: number
    totalPages: number
    size: number
    number: number
  }>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews', { params })
      return response.data
    } catch (error: any) {
      console.error('获取管理评价列表失败:', error)
      throw error
    }
  }

  /**
   * 创建评价回复
   */
  async createReviewReply(reviewId: number, request: ReviewReplyRequest): Promise<ApiResponse<ReviewReplyResponse>> {
    try {
      const response = await apiClient.post(`/v1/admin/reviews/${reviewId}/reply`, request)
      return response.data
    } catch (error: any) {
      console.error('创建回复失败:', error)
      throw error
    }
  }

  /**
   * 更新评价回复
   */
  async updateReviewReply(reviewId: number, replyId: number, request: ReviewReplyRequest): Promise<ApiResponse<ReviewReplyResponse>> {
    try {
      const response = await apiClient.put(`/v1/admin/reviews/${reviewId}/reply/${replyId}`, request)
      return response.data
    } catch (error: any) {
      console.error('更新回复失败:', error)
      throw error
    }
  }

  /**
   * 删除评价回复
   */
  async deleteReviewReply(reviewId: number, replyId: number): Promise<ApiResponse<void>> {
    try {
      const response = await apiClient.delete(`/v1/admin/reviews/${reviewId}/reply/${replyId}`)
      return response.data
    } catch (error: any) {
      console.error('删除回复失败:', error)
      throw error
    }
  }

  /**
   * 获取评价的回复列表
   */
  async getReviewReplies(reviewId: number): Promise<ApiResponse<ReviewReplyResponse[]>> {
    try {
      const response = await apiClient.get(`/v1/admin/reviews/${reviewId}/replies`)
      return response.data
    } catch (error: any) {
      console.error('获取回复列表失败:', error)
      throw error
    }
  }

  /**
   * 获取所有回复列表（管理员视图）
   */
  async getAllReplies(params: {
    status?: string
    adminId?: number
    startDate?: string
    endDate?: string
    page?: number
    size?: number
  }): Promise<ApiResponse<{
    content: ReviewReplyResponse[]
    totalElements: number
    totalPages: number
    size: number
    number: number
  }>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/replies', { params })
      return response.data
    } catch (error: any) {
      console.error('获取回复列表失败:', error)
      throw error
    }
  }

  /**
   * 获取审核日志
   */
  async getModerationLogs(reviewId: number): Promise<ApiResponse<ReviewModerationLogResponse[]>> {
    try {
      const response = await apiClient.get(`/v1/admin/reviews/${reviewId}/moderation-logs`)
      return response.data
    } catch (error: any) {
      console.error('获取审核日志失败:', error)
      throw error
    }
  }

  /**
   * 获取管理员的审核日志
   */
  async getModerationLogsForAdmin(params: {
    reviewId?: number
    action?: string
    startDate?: string
    endDate?: string
    page?: number
    size?: number
  }): Promise<ApiResponse<{
    content: ReviewModerationLogResponse[]
    totalElements: number
    totalPages: number
    size: number
    number: number
  }>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/moderation-logs', { params })
      return response.data
    } catch (error: any) {
      console.error('获取管理员审核日志失败:', error)
      throw error
    }
  }

  /**
   * 获取评价总体统计数据
   */
  async getOverallStatistics(hotelId?: number): Promise<ApiResponse<ReviewStatisticsResponse>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/analytics/statistics', {
        params: hotelId ? { hotelId } : {}
      })
      return response.data
    } catch (error: any) {
      console.error('获取统计数据失败:', error)
      throw error
    }
  }

  /**
   * 获取评价趋势分析数据
   */
  async getReviewTrends(request: ReviewAnalyticsRequest): Promise<ApiResponse<{
    countTrends: Array<{ date: string; count: number }>
    ratingTrends: Array<{ date: string; rating: number }>
    statusTrends: Array<{ date: string; approved: number; rejected: number; pending: number }>
  }>> {
    try {
      const response = await apiClient.post('/v1/admin/reviews/analytics/trends', request)
      return response.data
    } catch (error: any) {
      console.error('获取趋势数据失败:', error)
      throw error
    }
  }

  /**
   * 获取评价质量分析数据
   */
  async getQualityAnalysis(hotelId?: number): Promise<ApiResponse<{
    statusCounts: Record<string, number>
    violationRate: number
    replyRate: number
    totalReplies: number
    averageModerationTime: number
  }>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/analytics/quality', {
        params: hotelId ? { hotelId } : {}
      })
      return response.data
    } catch (error: any) {
      console.error('获取质量分析数据失败:', error)
      throw error
    }
  }

  /**
   * 获取管理员审核统计数据
   */
  async getModerationStatistics(): Promise<ApiResponse<{
    actionCounts: Record<string, number>
    totalActions: number
    recentActions: number
  }>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/analytics/moderation-stats')
      return response.data
    } catch (error: any) {
      console.error('获取审核统计数据失败:', error)
      throw error
    }
  }

  /**
   * 获取管理仪表板数据
   */
  async getDashboardData(hotelId?: number): Promise<ApiResponse<{
    statistics: ReviewStatisticsResponse
    quality: any
    moderationStats: any
    trends: any
  }>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/analytics/dashboard', {
        params: hotelId ? { hotelId } : {}
      })
      return response.data
    } catch (error: any) {
      console.error('获取仪表板数据失败:', error)
      throw error
    }
  }

  /**
   * 清空分析缓存
   */
  async clearAnalyticsCache(): Promise<ApiResponse<void>> {
    try {
      const response = await apiClient.post('/v1/admin/reviews/analytics/clear-cache')
      return response.data
    } catch (error: any) {
      console.error('清空缓存失败:', error)
      throw error
    }
  }

  // ========== 统计分析高级功能 API ==========

  /**
   * 获取综合评分统计
   */
  async getOverviewStatistics(hotelId: number, period: string = 'monthly'): Promise<ApiResponse<{
    hotelId: number
    totalReviews: number
    overallRating: number
    dimensionRatings: Record<string, number>
    ratingDistribution: Record<string, number>
    trendData: any[]
    lastUpdated: string
    period: string
    yearOverYear: Record<string, any>
    monthOverMonth: Record<string, any>
  }>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/statistics/overview', {
        params: { hotelId, period }
      })
      return response.data
    } catch (error: any) {
      console.error('获取综合评分统计失败:', error)
      throw error
    }
  }

  /**
   * 获取评分趋势数据
   */
  async getRatingTrends(
    hotelId: number,
    startDate: string,
    endDate: string,
    groupBy: string = 'day'
  ): Promise<ApiResponse<Array<{
    period: string
    overallRating: number
    dimensionRatings: Record<string, number>
    reviewCount: number
    nps: number
    recommendationRate: number
    averageResponseTime: number
  }>>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/statistics/trends', {
        params: { hotelId, startDate, endDate, groupBy }
      })
      return response.data
    } catch (error: any) {
      console.error('获取评分趋势数据失败:', error)
      throw error
    }
  }

  /**
   * 获取评价词云数据
   */
  async getWordCloud(hotelId: number, limit: number = 50): Promise<ApiResponse<Array<{
    word: string
    count: number
    weight: number
    sentiment: string
    category: string
    averageRating: number
  }>>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/statistics/wordcloud', {
        params: { hotelId, limit }
      })
      return response.data
    } catch (error: any) {
      console.error('获取词云数据失败:', error)
      throw error
    }
  }

  /**
   * 获取酒店对比分析数据
   */
  async getHotelComparison(hotelId: number, competitorIds?: number[]): Promise<ApiResponse<Array<{
    hotelId: number
    hotelName: string
    overallRating: number
    dimensionRatings: Record<string, number>
    reviewCount: number
    ranking: number
    isCurrentHotel: boolean
    deviationFromAverage: Record<string, number>
  }>>> {
    try {
      const params: any = { hotelId }
      if (competitorIds && competitorIds.length > 0) {
        params.competitorIds = competitorIds.join(',')
      }

      const response = await apiClient.get('/v1/admin/reviews/statistics/comparison', { params })
      return response.data
    } catch (error: any) {
      console.error('获取酒店对比分析数据失败:', error)
      throw error
    }
  }

  /**
   * 获取改进建议
   */
  async getSuggestions(hotelId: number, category?: string): Promise<ApiResponse<Array<{
    id: number
    category: string
    title: string
    description: string
    priority: string
    keywords: string[]
    relatedReviewCount: number
    expectedRatingImprovement: number
    difficulty: string
    estimatedCost: string
    implementationTime: string
    analysisResult: string
  }>>> {
    try {
      const params: any = { hotelId }
      if (category) {
        params.category = category
      }

      const response = await apiClient.get('/v1/admin/reviews/statistics/suggestions', { params })
      return response.data
    } catch (error: any) {
      console.error('获取改进建议失败:', error)
      throw error
    }
  }

  /**
   * 导出统计数据
   */
  async exportStatistics(hotelId: number, format: string = 'excel', period: string = 'monthly'): Promise<ApiResponse<string>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/statistics/export', {
        params: { hotelId, format, period }
      })
      return response.data
    } catch (error: any) {
      console.error('导出统计数据失败:', error)
      throw error
    }
  }

  /**
   * 清空统计缓存
   */
  async clearStatisticsCache(): Promise<ApiResponse<string>> {
    try {
      const response = await apiClient.post('/v1/admin/reviews/statistics/cache/clear')
      return response.data
    } catch (error: any) {
      console.error('清空统计缓存失败:', error)
      throw error
    }
  }

  /**
   * 获取统计概览
   */
  async getStatisticsSummary(hotelId: number): Promise<ApiResponse<{
    totalReviews: number
    overallRating: number
    dimensionRatings: Record<string, number>
    lastUpdated: string
  }>> {
    try {
      const response = await apiClient.get('/v1/admin/reviews/statistics/summary', {
        params: { hotelId }
      })
      return response.data
    } catch (error: any) {
      console.error('获取统计概览失败:', error)
      throw error
    }
  }
}

  // ========== 激励系统 API ==========

  /**
   * 获取活跃活动
   */
  async getActiveActivities(active: boolean = true): Promise<ApiResponse<ReviewActivity[]>> {
    try {
      const response = await apiClient.get('/v1/reviews/activities', {
        params: { active }
      })
      return response.data
    } catch (error: any) {
      console.error('获取活动失败:', error)
      throw error
    }
  }

  /**
   * 根据类型获取活动
   */
  async getActivitiesByType(activityType: string): Promise<ApiResponse<ReviewActivity[]>> {
    try {
      const response = await apiClient.get(`/v1/reviews/activities/type/${activityType}`)
      return response.data
    } catch (error: any) {
      console.error('获取活动失败:', error)
      throw error
    }
  }

  /**
   * 参与活动
   */
  async joinActivity(activityId: number): Promise<ApiResponse<any>> {
    try {
      const response = await apiClient.post(`/v1/reviews/activities/${activityId}/join`)
      return response.data
    } catch (error: any) {
      console.error('参与活动失败:', error)
      throw error
    }
  }

  /**
   * 获取评价排行榜
   */
  async getLeaderboard(type: string = 'monthly', period: string): Promise<ApiResponse<ReviewLeaderboard>> {
    try {
      const response = await apiClient.get('/v1/reviews/activities/leaderboard', {
        params: { type, period }
      })
      return response.data
    } catch (error: any) {
      console.error('获取排行榜失败:', error)
      throw error
    }
  }

  /**
   * 获取评价的优质标识
   */
  async getReviewBadge(reviewId: number): Promise<ApiResponse<HighQualityBadge | null>> {
    try {
      const response = await apiClient.get(`/v1/reviews/activities/${reviewId}/badge`)
      return response.data
    } catch (error: any) {
      console.error('获取评价标识失败:', error)
      throw error
    }
  }

  /**
   * 获取用户参与记录
   */
  async getUserParticipations(): Promise<ApiResponse<ActivityParticipation[]>> {
    try {
      const response = await apiClient.get('/v1/reviews/activities/my-participations')
      return response.data
    } catch (error: any) {
      console.error('获取参与记录失败:', error)
      throw error
    }
  }

  /**
   * 取消活动参与
   */
  async cancelParticipation(activityId: number, reason: string): Promise<ApiResponse<void>> {
    try {
      const response = await apiClient.post(`/v1/reviews/activities/${activityId}/cancel`, { reason })
      return response.data
    } catch (error: any) {
      console.error('取消参与失败:', error)
      throw error
    }
  }

  // 管理员活动管理API

  /**
   * 创建新活动（管理员）
   */
  async createActivity(activityData: Partial<ReviewActivity>): Promise<ApiResponse<ReviewActivity>> {
    try {
      const response = await apiClient.post('/v1/reviews/activities/admin', activityData)
      return response.data
    } catch (error: any) {
      console.error('创建活动失败:', error)
      throw error
    }
  }

  /**
   * 更新活动（管理员）
   */
  async updateActivity(activityId: number, activityData: Partial<ReviewActivity>): Promise<ApiResponse<ReviewActivity>> {
    try {
      const response = await apiClient.put(`/v1/reviews/activities/admin/${activityId}`, activityData)
      return response.data
    } catch (error: any) {
      console.error('更新活动失败:', error)
      throw error
    }
  }

  /**
   * 删除活动（管理员）
   */
  async deleteActivity(activityId: number): Promise<ApiResponse<string>> {
    try {
      const response = await apiClient.delete(`/v1/reviews/activities/admin/${activityId}`)
      return response.data
    } catch (error: any) {
      console.error('删除活动失败:', error)
      throw error
    }
  }

  /**
   * 清除排行榜缓存（管理员）
   */
  async clearLeaderboardCache(): Promise<ApiResponse<string>> {
    try {
      const response = await apiClient.post('/v1/reviews/activities/admin/clear-cache')
      return response.data
    } catch (error: any) {
      console.error('清除缓存失败:', error)
      throw error
    }
  }

  /**
   * 获取用户积分汇总
   */
  async getUserPointsSummary(): Promise<ApiResponse<{
    userId: number
    totalPoints: number
    earnedThisMonth: number
    recentHistory: any[]
  }>> {
    try {
      const response = await apiClient.get('/v1/users/me/points')
      return response.data
    } catch (error: any) {
      console.error('获取积分汇总失败:', error)
      throw error
    }
  }

  /**
   * 获取用户积分历史
   */
  async getUserPointsHistory(page: number = 0, size: number = 20): Promise<ApiResponse<{
    content: any[]
    totalElements: number
    totalPages: number
  }>> {
    try {
      const response = await apiClient.get('/v1/users/me/points/history', {
        params: { page, size }
      })
      return response.data
    } catch (error: any) {
      console.error('获取积分历史失败:', error)
      throw error
    }
  }
}

export default new ReviewService()

// 导出单例实例
export const reviewService = new ReviewService()