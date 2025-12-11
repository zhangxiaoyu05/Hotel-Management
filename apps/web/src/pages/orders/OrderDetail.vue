<template>
  <div class="order-detail">
    <div v-if="loading" class="loading">
      <i class="fas fa-spinner fa-spin"></i>
      加载中...
    </div>

    <div v-else-if="error" class="error">
      <i class="fas fa-exclamation-triangle"></i>
      <p>{{ error }}</p>
      <button @click="loadOrder" class="btn-retry">重试</button>
    </div>

    <div v-else-if="order" class="order-content">
      <!-- 返回按钮 -->
      <div class="back-button">
        <button @click="goBack">
          <i class="fas fa-arrow-left"></i>
          返回订单列表
        </button>
      </div>

      <!-- 订单头部 -->
      <div class="order-header">
        <div class="order-info">
          <h1>订单详情</h1>
          <div class="order-number">订单号：{{ order.orderNumber }}</div>
        </div>
        <OrderStatus :status="order.status" size="large" />
      </div>

      <!-- 订单状态时间线 -->
      <div class="timeline-section">
        <h2>订单状态</h2>
        <OrderTimeline :order="order" />
      </div>

      <!-- 酒店和房间信息 -->
      <div class="hotel-section">
        <h2>酒店信息</h2>
        <div class="hotel-card">
          <div class="hotel-image">
            <img
              v-if="order.hotel.images && order.hotel.images.length > 0"
              :src="order.hotel.images[0]"
              :alt="order.hotel.name"
            />
            <div v-else class="no-image">
              <i class="fas fa-hotel"></i>
            </div>
          </div>
          <div class="hotel-details">
            <h3>{{ order.hotel.name }}</h3>
            <div class="hotel-address">
              <i class="fas fa-map-marker-alt"></i>
              <span>{{ order.hotel.address }}</span>
            </div>
            <div class="hotel-rating">
              <i class="fas fa-star"></i>
              <span>{{ order.hotel.rating || '暂无评分' }}</span>
            </div>
            <div class="hotel-contact">
              <i class="fas fa-phone"></i>
              <span>{{ order.hotel.contactPhone || '暂无联系方式' }}</span>
            </div>
          </div>
        </div>

        <div class="room-info">
          <h4>房间信息</h4>
          <div class="room-details">
            <div class="room-name">{{ order.room.name }}</div>
            <div class="room-number">房间号：{{ order.room.roomNumber }}</div>
            <div class="room-type">{{ order.room.type }}</div>
            <div class="room-features">
              <span v-for="feature in order.room.features" :key="feature" class="feature-tag">
                {{ feature }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 入住信息 -->
      <div class="booking-section">
        <h2>入住信息</h2>
        <div class="booking-details">
          <div class="detail-row">
            <div class="detail-item">
              <i class="fas fa-calendar-check"></i>
              <div>
                <div class="label">入住时间</div>
                <div class="value">{{ formatDate(order.checkInDate) }} 14:00后</div>
              </div>
            </div>
            <div class="detail-item">
              <i class="fas fa-calendar-times"></i>
              <div>
                <div class="label">退房时间</div>
                <div class="value">{{ formatDate(order.checkOutDate) }} 12:00前</div>
              </div>
            </div>
          </div>
          <div class="detail-row">
            <div class="detail-item">
              <i class="fas fa-users"></i>
              <div>
                <div class="label">入住人数</div>
                <div class="value">{{ order.guestCount }}人</div>
              </div>
            </div>
            <div class="detail-item">
              <i class="fas fa-bed"></i>
              <div>
                <div class="label">入住天数</div>
                <div class="value">{{ calculateNights(order.checkInDate, order.checkOutDate) }}晚</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 特殊要求 -->
      <div v-if="order.specialRequests" class="requests-section">
        <h2>特殊要求</h2>
        <div class="special-requests">
          {{ order.specialRequests }}
        </div>
      </div>

      <!-- 价格明细 -->
      <div class="price-section">
        <h2>价格明细</h2>
        <div v-if="order.priceBreakdown" class="price-breakdown">
          <div class="price-item">
            <span>房费 (¥{{ order.priceBreakdown.roomRate }}/晚 × {{ order.priceBreakdown.nights }}晚)</span>
            <span>¥{{ order.priceBreakdown.roomFee.toFixed(2) }}</span>
          </div>
          <div class="price-item">
            <span>服务费</span>
            <span>¥{{ order.priceBreakdown.serviceFee?.toFixed(2) || '0.00' }}</span>
          </div>
          <div v-if="order.priceBreakdown.taxAmount" class="price-item">
            <span>税费</span>
            <span>¥{{ order.priceBreakdown.taxAmount.toFixed(2) }}</span>
          </div>
          <div v-if="order.priceBreakdown.discountAmount && order.priceBreakdown.discountAmount > 0" class="price-item discount">
            <span>优惠金额</span>
            <span>-¥{{ order.priceBreakdown.discountAmount.toFixed(2) }}</span>
          </div>
          <div v-if="order.priceBreakdown.couponCode" class="price-item discount">
            <span>优惠券 ({{ order.priceBreakdown.couponCode }})</span>
            <span>-¥{{ order.priceBreakdown.discountAmount?.toFixed(2) || '0.00' }}</span>
          </div>
          <div class="price-divider"></div>
          <div class="price-total">
            <span>总计</span>
            <span class="total-amount">¥{{ order.totalPrice.toFixed(2) }}</span>
          </div>
        </div>
        <div v-else class="price-total">
          <span>总计</span>
          <span class="total-amount">¥{{ order.totalPrice.toFixed(2) }}</span>
        </div>
      </div>

      <!-- 退款信息 -->
      <div v-if="order.refundInfo" class="refund-section">
        <h2>退款信息</h2>
        <div class="refund-info">
          <div class="refund-item">
            <span>退款金额</span>
            <span class="refund-amount">¥{{ order.refundInfo.refundAmount.toFixed(2) }}</span>
          </div>
          <div class="refund-item">
            <span>取消原因</span>
            <span>{{ order.refundInfo.cancelReason }}</span>
          </div>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="order-meta">
        <div class="meta-item">
          <span class="label">下单时间：</span>
          <span>{{ formatDateTime(order.createdAt) }}</span>
        </div>
        <div v-if="order.modifiedAt" class="meta-item">
          <span class="label">最后修改：</span>
          <span>{{ formatDateTime(order.modifiedAt) }}</span>
        </div>
        <div class="meta-item">
          <span class="label">更新时间：</span>
          <span>{{ formatDateTime(order.updatedAt) }}</span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button
          v-if="order.status === 'CONFIRMED'"
          @click="showCancelModal"
          class="btn-cancel"
        >
          取消订单
        </button>
        <button
          v-if="order.status === 'PENDING' || order.status === 'CONFIRMED'"
          @click="showEditModal"
          class="btn-edit"
        >
          修改订单
        </button>
        <button @click="goBack" class="btn-back">
          返回列表
        </button>
      </div>
    </div>

    <!-- 修改订单弹窗 -->
    <EditOrderModal
      v-if="showEdit"
      :order="order"
      @close="showEdit = false"
      @success="handleEditSuccess"
    />

    <!-- 取消订单弹窗 -->
    <CancelOrderModal
      v-if="showCancel"
      :order="order"
      @close="showCancel = false"
      @success="handleCancelSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderService } from '@/services/orderService'
import type { OrderResponse } from '@/types/order'
import OrderStatus from '@/components/business/OrderStatus.vue'
import OrderTimeline from '@/components/business/OrderTimeline.vue'
import EditOrderModal from '@/components/business/EditOrderModal.vue'
import CancelOrderModal from '@/components/business/CancelOrderModal.vue'

const route = useRoute()
const router = useRouter()

const order = ref<OrderResponse | null>(null)
const loading = ref(false)
const error = ref('')

const showEdit = ref(false)
const showCancel = ref(false)

const loadOrder = async () => {
  loading.value = true
  error.value = ''

  try {
    const orderId = Number(route.params.id)
    if (isNaN(orderId)) {
      error.value = '无效的订单ID'
      return
    }

    order.value = await orderService.getOrder(orderId)
  } catch (err: any) {
    console.error('加载订单详情失败:', err)
    error.value = err.message || '加载订单详情失败'
  } finally {
    loading.value = false
  }
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}

const formatDateTime = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const calculateNights = (checkInDate: string, checkOutDate: string) => {
  const checkIn = new Date(checkInDate)
  const checkOut = new Date(checkOutDate)
  const nights = Math.ceil((checkOut.getTime() - checkIn.getTime()) / (1000 * 60 * 60 * 24))
  return nights
}

const goBack = () => {
  router.push('/orders')
}

const showEditModal = () => {
  showEdit.value = true
}

const showCancelModal = () => {
  showCancel.value = true
}

const handleEditSuccess = () => {
  showEdit.value = false
  loadOrder()
}

const handleCancelSuccess = () => {
  showCancel.value = false
  loadOrder()
}

onMounted(() => {
  loadOrder()
})
</script>

<style scoped>
.order-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.loading, .error {
  text-align: center;
  padding: 100px 20px;
  color: #666;
}

.error {
  color: #e74c3c;
}

.btn-retry {
  margin-top: 20px;
  padding: 10px 20px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.back-button {
  margin-bottom: 30px;
}

.back-button button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background-color: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  color: #495057;
  cursor: pointer;
  transition: background-color 0.3s;
}

.back-button button:hover {
  background-color: #e9ecef;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  padding-bottom: 20px;
  border-bottom: 2px solid #eee;
}

.order-info h1 {
  margin: 0 0 10px 0;
  color: #2c3e50;
  font-size: 28px;
}

.order-number {
  color: #666;
  font-size: 16px;
}

.timeline-section,
.hotel-section,
.booking-section,
.requests-section,
.price-section,
.refund-section {
  margin-bottom: 40px;
}

.timeline-section h2,
.hotel-section h2,
.booking-section h2,
.requests-section h2,
.price-section h2,
.refund-section h2 {
  color: #2c3e50;
  margin-bottom: 20px;
  font-size: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.hotel-card {
  display: flex;
  gap: 20px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.hotel-image {
  width: 200px;
  height: 150px;
  flex-shrink: 0;
}

.hotel-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  width: 100%;
  height: 100%;
  background-color: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 24px;
}

.hotel-details {
  flex: 1;
  padding: 20px;
}

.hotel-details h3 {
  margin: 0 0 10px 0;
  color: #2c3e50;
  font-size: 18px;
}

.hotel-address,
.hotel-rating,
.hotel-contact {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #666;
  font-size: 14px;
}

.room-info {
  margin-top: 20px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.room-info h4 {
  margin: 0 0 15px 0;
  color: #2c3e50;
}

.room-name {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.room-number,
.room-type {
  color: #666;
  margin-bottom: 5px;
  font-size: 14px;
}

.room-features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.feature-tag {
  background-color: #e3f2fd;
  color: #1976d2;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.booking-details {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.detail-row {
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
}

.detail-row:last-child {
  margin-bottom: 0;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.detail-item i {
  font-size: 20px;
  color: #3498db;
  width: 20px;
}

.detail-item .label {
  color: #666;
  font-size: 14px;
  margin-bottom: 2px;
}

.detail-item .value {
  color: #2c3e50;
  font-weight: 500;
}

.special-requests {
  background-color: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 20px;
  color: #856404;
}

.price-breakdown,
.price-total {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.price-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  color: #666;
}

.price-item.discount {
  color: #e74c3c;
}

.price-divider {
  height: 1px;
  background-color: #eee;
  margin: 20px 0;
}

.price-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: 600;
}

.total-amount {
  color: #e74c3c;
  font-size: 24px;
  font-weight: 700;
}

.refund-info {
  background-color: #d4edda;
  border: 1px solid #c3e6cb;
  border-radius: 8px;
  padding: 20px;
}

.refund-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.refund-item:last-child {
  margin-bottom: 0;
}

.refund-amount {
  color: #155724;
  font-weight: 600;
  font-size: 18px;
}

.order-meta {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
}

.meta-item {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.meta-item:last-child {
  margin-bottom: 0;
}

.meta-item .label {
  color: #666;
  min-width: 80px;
}

.action-buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.btn-cancel,
.btn-edit,
.btn-back {
  padding: 12px 30px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-cancel {
  background-color: #e74c3c;
  color: white;
}

.btn-cancel:hover {
  background-color: #c0392b;
}

.btn-edit {
  background-color: #3498db;
  color: white;
}

.btn-edit:hover {
  background-color: #2980b9;
}

.btn-back {
  background-color: #6c757d;
  color: white;
}

.btn-back:hover {
  background-color: #5a6268;
}

@media (max-width: 768px) {
  .order-detail {
    padding: 10px;
  }

  .order-header {
    flex-direction: column;
    align-items: stretch;
    gap: 20px;
  }

  .hotel-card {
    flex-direction: column;
  }

  .hotel-image {
    width: 100%;
    height: 200px;
  }

  .detail-row {
    flex-direction: column;
    gap: 20px;
  }

  .action-buttons {
    flex-direction: column;
  }

  .btn-cancel,
  .btn-edit,
  .btn-back {
    width: 100%;
  }
}
</style>