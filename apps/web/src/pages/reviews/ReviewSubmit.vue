<template>
  <div class="review-submit-page">
    <div class="page-header">
      <div class="header-content">
        <button class="back-btn" @click="goBack">
          <i class="fas fa-arrow-left"></i>
          返回
        </button>
        <h1>提交评价</h1>
      </div>
    </div>

    <div class="page-content">
      <!-- 订单信息卡片 -->
      <div v-if="orderInfo" class="order-info-card">
        <div class="order-header">
          <h3>订单信息</h3>
          <span class="order-status">已完成</span>
        </div>
        <div class="order-details">
          <div class="hotel-info">
            <img
              v-if="orderInfo.hotelImage"
              :src="orderInfo.hotelImage"
              :alt="orderInfo.hotelName"
              class="hotel-image"
            />
            <div class="hotel-details">
              <h4>{{ orderInfo.hotelName }}</h4>
              <p class="room-type">{{ orderInfo.roomTypeName }}</p>
              <p class="order-dates">
                入住: {{ formatDate(orderInfo.checkInDate) }} -
                退房: {{ formatDate(orderInfo.checkOutDate) }}
              </p>
            </div>
          </div>
          <div class="order-meta">
            <p>订单号: {{ orderInfo.orderNumber }}</p>
            <p>订单金额: ¥{{ orderInfo.totalAmount }}</p>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <i class="fas fa-spinner fa-spin"></i>
        <p>加载订单信息中...</p>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="error-container">
        <i class="fas fa-exclamation-circle"></i>
        <p>{{ error }}</p>
        <button class="btn btn-primary" @click="fetchOrderInfo">重新加载</button>
      </div>

      <!-- 评价表单 -->
      <ReviewForm
        v-else-if="orderInfo"
        :order-id="orderId"
        @submit="handleSubmitReview"
        @cancel="goBack"
      />
    </div>

    <!-- 成功提示模态框 -->
    <div v-if="showSuccessModal" class="modal-overlay" @click="closeSuccessModal">
      <div class="success-modal" @click.stop>
        <div class="success-icon">
          <i class="fas fa-check-circle"></i>
        </div>
        <h2>评价提交成功！</h2>
        <p>感谢您的评价，您的反馈对我们非常重要</p>
        <div class="modal-actions">
          <button class="btn btn-primary" @click="goToOrders">返回订单列表</button>
          <button class="btn btn-secondary" @click="goToHotel">查看酒店详情</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ReviewForm from '@/components/business/ReviewForm.vue'
import reviewService, { type ReviewRequest } from '@/services/reviewService'
import orderService from '@/services/orderService'
import { apiClient } from '@/utils/apiClient'
import { formatDate } from '@/utils/date'

interface OrderInfo {
  id: number
  orderNumber: string
  hotelId: number
  hotelName: string
  hotelImage?: string
  roomId: number
  roomTypeName: string
  checkInDate: string
  checkOutDate: string
  totalAmount: number
  status: string
}

const route = useRoute()
const router = useRouter()

const orderId = Number(route.params.orderId)
const loading = ref(false)
const error = ref('')
const orderInfo = ref<OrderInfo | null>(null)
const showSuccessModal = ref(false)

onMounted(() => {
  fetchOrderInfo()
})

const fetchOrderInfo = async () => {
  if (!orderId) {
    error.value = '订单ID无效'
    return
  }

  loading.value = true
  error.value = ''

  try {
    // 检查是否可以评价
    const canReviewResponse = await reviewService.canReviewOrder(orderId)
    if (!canReviewResponse.data.canReview) {
      error.value = canReviewResponse.data.reason || '该订单无法评价'
      return
    }

    // 获取订单信息
    const orderResponse = await orderService.getOrder(orderId)
    orderInfo.value = orderResponse

  } catch (err: any) {
    console.error('获取订单信息失败:', err)
    error.value = err.response?.data?.message || '获取订单信息失败'
  } finally {
    loading.value = false
  }
}

const handleSubmitReview = async (reviewData: ReviewRequest) => {
  try {
    loading.value = true

    // 上传图片（如果有）
    if (reviewData.images && reviewData.images.length > 0) {
      // 这里需要将base64转换为File对象并上传
      // 实际项目中，图片上传应该在ImageUpload组件中处理
      const uploadedImages = await uploadImages(reviewData.images)
      reviewData.images = uploadedImages
    }

    // 提交评价
    await reviewService.submitReview(reviewData)

    // 显示成功提示
    showSuccessModal.value = true

  } catch (err: any) {
    console.error('提交评价失败:', err)
    error.value = err.response?.data?.message || '提交评价失败，请重试'
  } finally {
    loading.value = false
  }
}

const uploadImages = async (imageUrls: string[]): Promise<string[]> => {
  const uploadedUrls: string[] = []

  for (const imageUrl of imageUrls) {
    try {
      // 如果已经是HTTP URL，说明已经上传过，直接使用
      if (imageUrl.startsWith('http')) {
        uploadedUrls.push(imageUrl)
        continue
      }

      // 如果是base64，转换为File并上传
      if (imageUrl.startsWith('data:')) {
        const file = await base64ToFile(imageUrl)
        const uploadedUrl = await uploadSingleImage(file)
        uploadedUrls.push(uploadedUrl)
      } else {
        // 其他情况，假设已经是有效的URL
        uploadedUrls.push(imageUrl)
      }
    } catch (error: any) {
      console.error('图片上传失败:', error)
      ElMessage.error(`图片上传失败: ${error.message}`)
      // 可以选择继续上传其他图片，或者抛出错误停止
      throw error
    }
  }

  return uploadedUrls
}

/**
 * 将base64字符串转换为File对象
 */
async function base64ToFile(base64String: string): Promise<File> {
  return new Promise((resolve, reject) => {
    try {
      // 提取base64的MIME类型和纯数据
      const matches = base64String.match(/^data:([A-Za-z-+/]+);base64,(.+)$/)
      if (!matches || matches.length !== 3) {
        throw new Error('无效的base64格式')
      }

      const mimeType = matches[1]
      const byteString = atob(matches[2])
      const ab = new ArrayBuffer(byteString.length)
      const ia = new Uint8Array(ab)

      for (let i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i)
      }

      const blob = new Blob([ab], { type: mimeType })
      const fileName = `image_${Date.now()}.${mimeType.split('/')[1]}`
      const file = new File([blob], fileName, { type: mimeType })

      resolve(file)
    } catch (error) {
      reject(error)
    }
  })
}

/**
 * 上传单个图片文件
 */
async function uploadSingleImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', 'review')

  try {
    const response = await apiClient.post('/v1/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 30000
    })

    if (response.data && response.data.success) {
      return response.data.data.url
    } else {
      throw new Error(response.data?.message || '图片上传失败')
    }
  } catch (error: any) {
    console.error('图片上传失败:', error)
    throw new Error(error.response?.data?.message || error.message || '图片上传失败')
  }
}

const goBack = () => {
  router.go(-1)
}

const goToOrders = () => {
  router.push('/orders')
}

const goToHotel = () => {
  if (orderInfo.value) {
    router.push(`/hotels/${orderInfo.value.hotelId}`)
  }
}

const closeSuccessModal = () => {
  showSuccessModal.value = false
  goToOrders()
}
</script>

<style scoped>
.review-submit-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.page-header {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  padding: 1rem 0;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.back-btn {
  background: none;
  border: none;
  color: #374151;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border-radius: 0.375rem;
  transition: background-color 0.2s ease;
}

.back-btn:hover {
  background: #f3f4f6;
}

.page-header h1 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #111827;
}

.page-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.order-info-card {
  background: white;
  border-radius: 0.75rem;
  padding: 1.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.order-header h3 {
  font-size: 1.125rem;
  font-weight: 500;
  color: #111827;
}

.order-status {
  background: #dcfce7;
  color: #166534;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
}

.order-details {
  display: grid;
  gap: 1rem;
}

.hotel-info {
  display: flex;
  gap: 1rem;
}

.hotel-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 0.5rem;
}

.hotel-details h4 {
  font-size: 1rem;
  font-weight: 500;
  color: #111827;
  margin-bottom: 0.25rem;
}

.room-type {
  color: #6b7280;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.order-dates {
  color: #6b7280;
  font-size: 0.875rem;
}

.order-meta {
  display: flex;
  gap: 2rem;
  color: #6b7280;
  font-size: 0.875rem;
}

.loading-container,
.error-container {
  background: white;
  border-radius: 0.75rem;
  padding: 3rem;
  text-align: center;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.loading-container i,
.error-container i {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.loading-container i {
  color: #3b82f6;
}

.error-container i {
  color: #ef4444;
}

.loading-container p,
.error-container p {
  color: #6b7280;
  margin-bottom: 1.5rem;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.success-modal {
  background: white;
  border-radius: 1rem;
  padding: 2rem;
  text-align: center;
  max-width: 400px;
  width: 90%;
}

.success-icon {
  color: #10b981;
  font-size: 4rem;
  margin-bottom: 1rem;
}

.success-modal h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.5rem;
}

.success-modal p {
  color: #6b7280;
  margin-bottom: 2rem;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  font-size: 0.875rem;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hotel-info {
    flex-direction: column;
  }

  .hotel-image {
    width: 100%;
    height: 200px;
  }

  .order-meta {
    flex-direction: column;
    gap: 0.5rem;
  }

  .modal-actions {
    flex-direction: column;
  }

  .btn {
    width: 100%;
  }
}
</style>