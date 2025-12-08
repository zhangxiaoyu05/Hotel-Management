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
}

export default new ReviewService()