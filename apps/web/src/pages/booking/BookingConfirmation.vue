<template>
  <div class="booking-confirmation">
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在加载预订信息...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <div class="error-icon">❌</div>
      <h2>预订信息加载失败</h2>
      <p>{{ error }}</p>
      <button @click="retryLoad" class="btn btn-primary">重新加载</button>
    </div>

    <div v-else-if="orderData" class="confirmation-container">
      <!-- 成功状态头部 -->
      <div class="success-header">
        <div class="success-icon">✅</div>
        <h1>预订成功！</h1>
        <p class="success-message">您的房间已成功预订，请保存好订单信息</p>
      </div>

      <!-- 订单号显示 -->
      <div class="order-number-card">
        <h3>订单号</h3>
        <div class="order-number">{{ orderData.order.orderNumber }}</div>
        <div class="copy-section">
          <button @click="copyOrderNumber" class="copy-btn">
            {{ copied ? '已复制' : '复制订单号' }}
          </button>
        </div>
      </div>

      <!-- 酒店和房间信息 -->
      <div class="info-section">
        <div class="info-card">
          <h3>酒店信息</h3>
          <div class="hotel-info">
            <img
              :src="orderData.hotel.images?.[0] || '/default-hotel.jpg'"
              :alt="orderData.hotel.name"
              class="hotel-image"
            />
            <div class="hotel-details">
              <h4>{{ orderData.hotel.name }}</h4>
              <p class="address">{{ orderData.hotel.address }}</p>
              <p class="phone">📞 {{ orderData.hotel.phone || '暂无电话' }}</p>
            </div>
          </div>
        </div>

        <div class="info-card">
          <h3>房间信息</h3>
          <div class="room-info">
            <img
              :src="orderData.room.images?.[0] || '/default-room.jpg'"
              :alt="orderData.room.name"
              class="room-image"
            />
            <div class="room-details">
              <h4>{{ orderData.room.name }}</h4>
              <p>房间号: {{ orderData.room.roomNumber }}</p>
              <p>容纳人数: {{ orderData.room.maxGuests }}人</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 预订详情 -->
      <div class="booking-details">
        <h3>预订详情</h3>
        <div class="details-grid">
          <div class="detail-item">
            <span class="label">入住人</span>
            <span class="value">{{ guestInfo.guestName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">联系电话</span>
            <span class="value">{{ guestInfo.guestPhone }}</span>
          </div>
          <div v-if="guestInfo.guestEmail" class="detail-item">
            <span class="label">邮箱地址</span>
            <span class="value">{{ guestInfo.guestEmail }}</span>
          </div>
          <div class="detail-item">
            <span class="label">入住日期</span>
            <span class="value">{{ formatDate(orderData.order.checkInDate) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">退房日期</span>
            <span class="value">{{ formatDate(orderData.order.checkOutDate) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">入住天数</span>
            <span class="value">{{ orderData.priceBreakdown.nights }}晚</span>
          </div>
          <div class="detail-item">
            <span class="label">入住人数</span>
            <span class="value">{{ orderData.order.guestCount }}人</span>
          </div>
          <div class="detail-item">
            <span class="label">订单状态</span>
            <span class="value status" :class="statusClass">
              {{ getStatusText(orderData.order.status) }}
            </span>
          </div>
          <div class="detail-item">
            <span class="label">预订时间</span>
            <span class="value">{{ formatDateTime(orderData.order.createdAt) }}</span>
          </div>
          <div v-if="orderData.order.specialRequests" class="detail-item full-width">
            <span class="label">特殊要求</span>
            <span class="value">{{ orderData.order.specialRequests }}</span>
          </div>
        </div>
      </div>

      <!-- 费用明细 -->
      <div class="price-breakdown">
        <h3>费用明细</h3>
        <div class="price-details">
          <div class="price-row">
            <span class="label">
              房费 (¥{{ orderData.room.price }}/晚 × {{ orderData.priceBreakdown.nights }}晚)
            </span>
            <span class="value">¥{{ orderData.priceBreakdown.roomFee.toFixed(2) }}</span>
          </div>
          <div class="price-row">
            <span class="label">服务费 (10%)</span>
            <span class="value">¥{{ orderData.priceBreakdown.serviceFee.toFixed(2) }}</span>
          </div>
          <div v-if="orderData.priceBreakdown.discountAmount > 0" class="price-row discount">
            <span class="label">优惠减免</span>
            <span class="value">-¥{{ orderData.priceBreakdown.discountAmount.toFixed(2) }}</span>
          </div>
          <div class="price-row total">
            <span class="label">总计</span>
            <span class="value">¥{{ orderData.order.totalPrice.toFixed(2) }}</span>
          </div>
        </div>
      </div>

      <!-- 重要提示 -->
      <div class="important-notice">
        <h3>重要提示</h3>
        <ul>
          <li>请在入住当天携带有效身份证件办理入住手续</li>
          <li>入住时间通常为下午2点后，退房时间为中午12点前</li>
          <li>如需取消预订，请提前24小时联系酒店</li>
          <li>预订确认邮件已发送至您的邮箱，请注意查收</li>
        </ul>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button @click="shareBooking" class="btn btn-secondary">
          <span class="icon">📤</span>
          分享预订信息
        </button>
        <button @click="goToMyOrders" class="btn btn-primary">
          <span class="icon">📋</span>
          查看我的订单
        </button>
        <button @click="goToHotel" class="btn btn-outline">
          <span class="icon">🏨</span>
          查看酒店详情
        </button>
      </div>

      <!-- 取消预订 -->
      <div class="cancel-section">
        <button
          @click="showCancelDialog = true"
          class="cancel-btn"
          :disabled="orderData.order.status !== 'CONFIRMED' || canCancelOrder"
        >
          取消预订
        </button>
        <p v-if="!canCancelOrder" class="cancel-notice">
          入住当天或之后无法取消预订
        </p>
      </div>
    </div>

    <!-- 取消确认对话框 -->
    <div v-if="showCancelDialog" class="modal-overlay" @click="closeCancelDialog">
      <div class="modal" @click.stop>
        <h3>确认取消预订</h3>
        <p>您确定要取消这个预订吗？取消后无法恢复。</p>
        <div class="modal-actions">
          <button @click="closeCancelDialog" class="btn btn-secondary">
            取消
          </button>
          <button
            @click="confirmCancel"
            class="btn btn-danger"
            :disabled="canceling"
          >
            {{ canceling ? '正在取消...' : '确认取消' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderService } from '@/services/orderService'
import { useOrderStore } from '@/stores/order'
import type { OrderResponse } from '@/types/order'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()

// 响应式数据
const orderData = ref<OrderResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const copied = ref(false)
const showCancelDialog = ref(false)
const canceling = ref(false)

// 计算属性
const orderNumber = computed(() => route.params.orderNumber as string)

const guestInfo = computed(() => {
  if (!orderData.value) return { guestName: '', guestPhone: '', guestEmail: '' }

  // 从订单信息或存储中获取客人信息
  return {
    guestName: '张三', // 实际应用中应该从订单数据获取
    guestPhone: '13800138000', // 实际应用中应该从订单数据获取
    guestEmail: 'zhang@example.com' // 实际应用中应该从订单数据获取
  }
})

const statusClass = computed(() => {
  if (!orderData.value) return ''
  return `status-${orderData.value.order.status.toLowerCase()}`
})

const canCancelOrder = computed(() => {
  if (!orderData.value) return false
  const checkInDate = new Date(orderData.value.order.checkInDate)
  const today = new Date()
  return checkInDate <= today || orderData.value.order.status !== 'CONFIRMED'
})

// 方法
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const formatDateTime = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待确认',
    'CONFIRMED': '已确认',
    'CANCELLED': '已取消',
    'COMPLETED': '已完成'
  }
  return statusMap[status] || status
}

const copyOrderNumber = async () => {
  if (orderData.value) {
    try {
      await navigator.clipboard.writeText(orderData.value.order.orderNumber)
      copied.value = true
      setTimeout(() => {
        copied.value = false
      }, 2000)
    } catch (err) {
      console.error('复制失败:', err)
    }
  }
}

const shareBooking = async () => {
  if (!orderData.value) return

  const shareText = `我在${orderData.value.hotel.name}预订了${orderData.value.room.name}，订单号：${orderData.value.order.orderNumber}`
  const shareUrl = window.location.href

  if (navigator.share) {
    try {
      await navigator.share({
        title: '酒店预订成功',
        text: shareText,
        url: shareUrl
      })
    } catch (err) {
      console.log('分享取消:', err)
    }
  } else {
    // 降级处理：复制到剪贴板
    try {
      await navigator.clipboard.writeText(`${shareText} ${shareUrl}`)
      alert('分享链接已复制到剪贴板')
    } catch (err) {
      console.error('复制失败:', err)
    }
  }
}

const goToMyOrders = () => {
  router.push({ name: 'MyOrders' })
}

const goToHotel = () => {
  if (orderData.value) {
    router.push({
      name: 'HotelDetail',
      params: { id: orderData.value.hotel.id }
    })
  }
}

const closeCancelDialog = () => {
  showCancelDialog.value = false
}

const confirmCancel = async () => {
  if (!orderData.value) return

  canceling.value = true
  try {
    const success = await orderStore.cancelOrder(orderData.value.order.id)
    if (success) {
      // 重新加载订单数据
      await loadOrder()
    }
  } catch (err: any) {
    error.value = err.response?.data?.message || '取消失败，请重试'
  } finally {
    canceling.value = false
    closeCancelDialog()
  }
}

const loadOrder = async () => {
  loading.value = true
  error.value = null

  try {
    const data = await orderService.getOrderByNumber(orderNumber.value)
    orderData.value = data
  } catch (err: any) {
    error.value = err.response?.data?.message || '加载订单信息失败'
  } finally {
    loading.value = false
  }
}

const retryLoad = () => {
  loadOrder()
}

// 生命周期
onMounted(() => {
  loadOrder()
})
</script>

<style scoped>
.booking-confirmation {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1976d2;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-container {
  text-align: center;
  padding: 3rem 1rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.error-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.confirmation-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.success-header {
  background: linear-gradient(135deg, #4caf50, #45a049);
  color: white;
  text-align: center;
  padding: 3rem 2rem;
}

.success-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.success-header h1 {
  font-size: 2rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.success-message {
  font-size: 1.125rem;
  opacity: 0.9;
  margin: 0;
}

.order-number-card {
  background: #f8f9fa;
  padding: 1.5rem;
  text-align: center;
  border-bottom: 1px solid #e5e5e5;
}

.order-number-card h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #666;
  margin-bottom: 0.5rem;
}

.order-number {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1976d2;
  font-family: monospace;
  margin-bottom: 1rem;
}

.copy-btn {
  background: #1976d2;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
}

.copy-btn:hover {
  background: #1565c0;
}

.info-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin: 1.5rem;
}

.info-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
}

.info-card h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 1rem;
}

.hotel-info,
.room-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.hotel-image,
.room-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.hotel-details,
.room-details h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.5rem;
}

.hotel-details p,
.room-details p {
  font-size: 0.875rem;
  color: #666;
  margin-bottom: 0.25rem;
}

.booking-details,
.price-breakdown,
.important-notice {
  margin: 1.5rem;
  padding: 1.5rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.booking-details h3,
.price-breakdown h3,
.important-notice h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 1rem;
}

.details-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid #e5e5e5;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-item:last-child {
  border-bottom: none;
}

.label {
  font-size: 0.875rem;
  color: #666;
  font-weight: 500;
}

.value {
  font-size: 0.875rem;
  color: #333;
  font-weight: 600;
}

.status {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-confirmed {
  background: #e8f5e8;
  color: #4caf50;
}

.status-pending {
  background: #fff3e0;
  color: #ff9800;
}

.status-cancelled {
  background: #ffebee;
  color: #f44336;
}

.status-completed {
  background: #e3f2fd;
  color: #1976d2;
}

.price-details {
  space-y: 0.75rem;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  font-size: 0.875rem;
}

.price-row:not(:last-child) {
  border-bottom: 1px solid #e5e5e5;
}

.price-row.total {
  font-weight: 600;
  font-size: 1rem;
  color: #1976d2;
  margin-top: 0.5rem;
  padding-top: 1rem;
  border-top: 2px solid #1976d2;
}

.price-row.discount {
  color: #4caf50;
}

.important-notice ul {
  margin: 0;
  padding-left: 1.5rem;
}

.important-notice li {
  font-size: 0.875rem;
  color: #666;
  margin-bottom: 0.5rem;
  line-height: 1.5;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  margin: 1.5rem;
  padding: 0 1.5rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-primary {
  background: #1976d2;
  color: white;
}

.btn-primary:hover {
  background: #1565c0;
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
}

.btn-secondary:hover {
  background: #eeeeee;
}

.btn-outline {
  background: white;
  color: #1976d2;
  border: 1px solid #1976d2;
}

.btn-outline:hover {
  background: #f0f8ff;
}

.btn-danger {
  background: #f44336;
  color: white;
}

.btn-danger:hover {
  background: #d32f2f;
}

.cancel-section {
  text-align: center;
  margin: 1.5rem;
  padding: 1.5rem;
  border-top: 1px solid #e5e5e5;
}

.cancel-btn {
  background: transparent;
  color: #f44336;
  border: 1px solid #f44336;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
}

.cancel-btn:hover:not(:disabled) {
  background: #ffebee;
}

.cancel-btn:disabled {
  color: #ccc;
  border-color: #ccc;
  cursor: not-allowed;
}

.cancel-notice {
  font-size: 0.875rem;
  color: #666;
  margin-top: 0.5rem;
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

.modal {
  background: white;
  border-radius: 8px;
  padding: 2rem;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: #333;
}

.modal p {
  font-size: 0.875rem;
  color: #666;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .booking-confirmation {
    padding: 1rem;
  }

  .info-section {
    grid-template-columns: 1fr;
    margin: 1rem;
  }

  .details-grid {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
    margin: 1rem;
  }

  .success-header {
    padding: 2rem 1rem;
  }

  .success-header h1 {
    font-size: 1.5rem;
  }
}
</style>