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

export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
}

class ReviewService {
  /**
   * 提交评价
   */
  async submitReview(reviewData: ReviewRequest): Promise<ApiResponse<ReviewResponse>> {
    try {
      const response = await apiClient.post('/v1/reviews', reviewData)
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
}

export default new ReviewService()