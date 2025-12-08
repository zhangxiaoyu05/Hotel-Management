<template>
  <div class="booking-wizard">
    <div class="wizard-header">
      <h2 class="title">预订房间</h2>

      <!-- 步骤指示器 -->
      <div class="steps">
        <div
          v-for="(step, index) in steps"
          :key="index"
          class="step"
          :class="{
            'active': index + 1 === bookingState.currentStep,
            'completed': index + 1 < bookingState.currentStep
          }"
        >
          <div class="step-number">{{ index + 1 }}</div>
          <div class="step-title">{{ step.title }}</div>
        </div>
      </div>
    </div>

    <!-- 步骤内容 -->
    <div class="wizard-content">
      <!-- 步骤1: 选择房间和日期 -->
      <div v-if="bookingState.currentStep === 1" class="step-content">
        <h3>选择房间和日期</h3>

        <div v-if="bookingState.selectedRoom" class="selected-room">
          <h4>已选择的房间</h4>
          <div class="room-card">
            <img
              :src="bookingState.selectedRoom.images?.[0] || '/default-room.jpg'"
              :alt="bookingState.selectedRoom.name"
              class="room-image"
            />
            <div class="room-info">
              <h5>{{ bookingState.selectedRoom.name }}</h5>
              <p>{{ bookingState.selectedRoom.hotelName }}</p>
              <p class="price">¥{{ bookingState.selectedRoom.price }}/晚</p>
            </div>
            <button @click="changeRoom" class="change-btn">更换房间</button>
          </div>
        </div>

        <div class="date-guest-section">
          <div class="form-group">
            <label>入住日期</label>
            <input
              v-model="bookingState.checkInDate"
              type="date"
              :min="today"
              @change="validateDates"
              class="form-input"
            />
          </div>

          <div class="form-group">
            <label>退房日期</label>
            <input
              v-model="bookingState.checkOutDate"
              type="date"
              :min="bookingState.checkInDate || today"
              @change="validateDates"
              class="form-input"
            />
          </div>

          <div class="form-group">
            <label>入住人数</label>
            <select
              v-model="bookingState.guestCount"
              class="form-input"
            >
              <option v-for="n in 6" :key="n" :value="n">{{ n }}人</option>
            </select>
          </div>
        </div>

        <div v-if="dateError" class="error-message">{{ dateError }}</div>
      </div>

      <!-- 步骤2: 填写入住信息 -->
      <div v-if="bookingState.currentStep === 2" class="step-content">
        <h3>填写入住信息</h3>

        <div class="guest-form">
          <div class="form-row">
            <div class="form-group">
              <label>入住人姓名 *</label>
              <input
                v-model="bookingState.guestInfo.guestName"
                type="text"
                placeholder="请输入入住人姓名"
                class="form-input"
                :class="{ 'error': errors.guestName }"
              />
              <span v-if="errors.guestName" class="error-text">{{ errors.guestName }}</span>
            </div>

            <div class="form-group">
              <label>联系电话 *</label>
              <input
                v-model="bookingState.guestInfo.guestPhone"
                type="tel"
                placeholder="请输入手机号码"
                class="form-input"
                :class="{ 'error': errors.guestPhone }"
              />
              <span v-if="errors.guestPhone" class="error-text">{{ errors.guestPhone }}</span>
            </div>
          </div>

          <div class="form-group">
            <label>邮箱地址</label>
            <input
              v-model="bookingState.guestInfo.guestEmail"
              type="email"
              placeholder="请输入邮箱地址（选填）"
              class="form-input"
              :class="{ 'error': errors.guestEmail }"
            />
            <span v-if="errors.guestEmail" class="error-text">{{ errors.guestEmail }}</span>
          </div>

          <div class="form-group">
            <label>特殊要求</label>
            <textarea
              v-model="bookingState.guestInfo.specialRequests"
              placeholder="如有特殊要求请在此填写（选填）"
              rows="3"
              class="form-textarea"
            ></textarea>
          </div>
        </div>
      </div>

      <!-- 步骤3: 确认预订 -->
      <div v-if="bookingState.currentStep === 3" class="step-content">
        <h3>确认预订信息</h3>

        <div class="booking-summary">
          <div class="summary-section">
            <h4>房间信息</h4>
            <div class="room-summary">
              <img
                :src="bookingState.selectedRoom?.images?.[0] || '/default-room.jpg'"
                :alt="bookingState.selectedRoom?.name"
                class="room-image-small"
              />
              <div class="room-details">
                <p><strong>{{ bookingState.selectedRoom?.name }}</strong></p>
                <p>{{ bookingState.selectedRoom?.hotelName }}</p>
                <p>{{ bookingState.selectedRoom?.roomNumber }}</p>
              </div>
            </div>
          </div>

          <div class="summary-section">
            <h4>预订信息</h4>
            <div class="booking-details">
              <p><strong>入住日期:</strong> {{ formatDate(bookingState.checkInDate) }}</p>
              <p><strong>退房日期:</strong> {{ formatDate(bookingState.checkOutDate) }}</p>
              <p><strong>入住天数:</strong> {{ nights }}晚</p>
              <p><strong>入住人数:</strong> {{ bookingState.guestCount }}人</p>
              <p><strong>入住人:</strong> {{ bookingState.guestInfo.guestName }}</p>
              <p><strong>联系电话:</strong> {{ bookingState.guestInfo.guestPhone }}</p>
              <div v-if="bookingState.guestInfo.specialRequests" class="special-requests">
                <strong>特殊要求:</strong> {{ bookingState.guestInfo.specialRequests }}
              </div>
            </div>
          </div>

          <!-- 价格明细 -->
          <PriceBreakdown
            v-if="priceData"
            :room="bookingState.selectedRoom"
            :check-in-date="bookingState.checkInDate"
            :check-out-date="bookingState.checkOutDate"
            :coupon-code="bookingState.couponCode"
            @update:price-breakdown="handlePriceUpdate"
          />

          <!-- 优惠券输入 -->
          <div class="coupon-section">
            <div class="form-group">
              <label>优惠券代码</label>
              <div class="coupon-input-group">
                <input
                  v-model="bookingState.couponCode"
                  type="text"
                  placeholder="请输入优惠券代码"
                  class="form-input"
                />
                <button
                  @click="applyCoupon"
                  class="apply-coupon-btn"
                  :disabled="!bookingState.couponCode"
                >
                  应用
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="wizard-footer">
      <button
        v-if="bookingState.currentStep > 1"
        @click="prevStep"
        class="btn btn-secondary"
        :disabled="loading"
      >
        上一步
      </button>

      <button
        v-if="bookingState.currentStep < 3"
        @click="nextStep"
        class="btn btn-primary"
        :disabled="!canProceed || loading"
      >
        下一步
      </button>

      <button
        v-if="bookingState.currentStep === 3"
        @click="confirmBooking"
        class="btn btn-primary btn-large"
        :disabled="loading"
      >
        <span v-if="loading" class="loading-spinner"></span>
        {{ loading ? '正在预订...' : '确认预订' }}
      </button>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-toast">
      {{ error }}
      <button @click="error = null" class="close-btn">×</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { useAuthStore } from '@/stores/auth'
import PriceBreakdown from '@/components/business/PriceBreakdown.vue'
import type { Room } from '@/types/room'

const router = useRouter()
const orderStore = useOrderStore()
const authStore = useAuthStore()

// 响应式数据
const today = computed(() => new Date().toISOString().split('T')[0])

const steps = [
  { title: '选择房间', key: 'room' },
  { title: '填写信息', key: 'guest' },
  { title: '确认预订', key: 'confirm' }
]

const { bookingState, loading, error } = orderStore

const dateError = ref('')
const errors = ref<Record<string, string>>({})

// 计算属性
const canProceed = computed(() => {
  switch (bookingState.currentStep) {
    case 1:
      return orderStore.canProceedToStep2 && !dateError.value
    case 2:
      return orderStore.canProceedToStep3 && !hasErrors.value
    case 3:
      return true
    default:
      return false
  }
})

const nights = computed(() => {
  if (!bookingState.checkInDate || !bookingState.checkOutDate) return 0
  const start = new Date(bookingState.checkInDate)
  const end = new Date(bookingState.checkOutDate)
  const diffTime = Math.abs(end.getTime() - start.getTime())
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
})

const hasErrors = computed(() => {
  return Object.values(errors.value).some(error => error !== '')
})

const priceData = computed(() => {
  if (!bookingState.selectedRoom || !bookingState.checkInDate || !bookingState.checkOutDate) {
    return null
  }

  return {
    room: bookingState.selectedRoom,
    checkInDate: bookingState.checkInDate,
    checkOutDate: bookingState.checkOutDate,
    nights: nights.value
  }
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

const validateDates = () => {
  dateError.value = ''

  if (bookingState.checkInDate && bookingState.checkOutDate) {
    const checkIn = new Date(bookingState.checkInDate)
    const checkOut = new Date(bookingState.checkOutDate)

    if (checkIn >= checkOut) {
      dateError.value = '退房日期必须晚于入住日期'
    } else if (checkIn < new Date(today.value)) {
      dateError.value = '入住日期不能早于今天'
    }
  }
}

const validateGuestInfo = () => {
  errors.value = {}

  if (!bookingState.guestInfo.guestName.trim()) {
    errors.value.guestName = '请输入入住人姓名'
  }

  if (!bookingState.guestInfo.guestPhone.trim()) {
    errors.value.guestPhone = '请输入联系电话'
  } else if (!/^1[3-9]\d{9}$/.test(bookingState.guestInfo.guestPhone)) {
    errors.value.guestPhone = '请输入正确的手机号码'
  }

  if (bookingState.guestInfo.guestEmail &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(bookingState.guestInfo.guestEmail)) {
    errors.value.guestEmail = '请输入正确的邮箱地址'
  }
}

const nextStep = () => {
  if (bookingState.currentStep === 2) {
    validateGuestInfo()
    if (hasErrors.value) return
  }

  orderStore.nextStep()
  orderStore.saveBookingState()
}

const prevStep = () => {
  orderStore.prevStep()
  orderStore.saveBookingState()
}

const changeRoom = () => {
  router.push({
    name: 'RoomDetail',
    params: { id: bookingState.selectedRoom?.id },
    query: {
      checkInDate: bookingState.checkInDate,
      checkOutDate: bookingState.checkOutDate,
      guestCount: bookingState.guestCount.toString()
    }
  })
}

const handlePriceUpdate = (breakdown: any) => {
  orderStore.setPriceBreakdown(breakdown)
}

const applyCoupon = () => {
  // 触发价格重新计算
  orderStore.saveBookingState()
}

const confirmBooking = async () => {
  if (!authStore.isAuthenticated) {
    router.push({
      name: 'Login',
      query: { redirect: router.currentRoute.value.fullPath }
    })
    return
  }

  const result = await orderStore.createOrder()
  if (result) {
    orderStore.clearBookingState()
    router.push({
      name: 'BookingConfirmation',
      params: { orderNumber: result.order.orderNumber }
    })
  }
}

// 监听器
watch(() => bookingState.currentStep, (newStep) => {
  if (newStep === 2) {
    validateGuestInfo()
  }
})

// 生命周期
onMounted(() => {
  orderStore.restoreBookingState()

  // 检查是否有房间信息，如果没有则返回房间列表页
  if (!bookingState.selectedRoom) {
    router.push({ name: 'RoomSearch' })
  }
})

onUnmounted(() => {
  if (bookingState.currentStep < 3) {
    orderStore.saveBookingState()
  }
})
</script>

<style scoped>
.booking-wizard {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.wizard-header {
  margin-bottom: 2rem;
}

.title {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  color: #333;
}

.steps {
  display: flex;
  justify-content: space-between;
  margin-bottom: 2rem;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}

.step:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 20px;
  right: -50%;
  width: 100%;
  height: 2px;
  background: #e5e5e5;
  z-index: -1;
}

.step.completed:not(:last-child)::after {
  background: #4caf50;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e5e5e5;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.step.active .step-number {
  background: #1976d2;
  color: white;
}

.step.completed .step-number {
  background: #4caf50;
  color: white;
}

.step-title {
  font-size: 0.875rem;
  color: #666;
  text-align: center;
}

.step.active .step-title {
  color: #1976d2;
  font-weight: 600;
}

.step.completed .step-title {
  color: #4caf50;
}

.wizard-content {
  min-height: 400px;
}

.step-content h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  color: #333;
}

.selected-room {
  margin-bottom: 2rem;
}

.selected-room h4 {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: #666;
}

.room-card {
  display: flex;
  align-items: center;
  padding: 1rem;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #f9f9f9;
}

.room-image {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 1rem;
}

.room-info {
  flex: 1;
}

.room-info h5 {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.room-info p {
  font-size: 0.875rem;
  color: #666;
  margin-bottom: 0.25rem;
}

.price {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1976d2;
}

.change-btn {
  padding: 0.5rem 1rem;
  background: #f5f5f5;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
}

.change-btn:hover {
  background: #eeeeee;
}

.date-guest-section {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: #333;
  margin-bottom: 0.5rem;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  font-size: 0.875rem;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: #1976d2;
}

.form-input.error {
  border-color: #f44336;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.error-text {
  display: block;
  font-size: 0.75rem;
  color: #f44336;
  margin-top: 0.25rem;
}

.error-message {
  color: #f44336;
  font-size: 0.875rem;
  margin-top: 0.5rem;
}

.booking-summary {
  space-y: 2rem;
}

.summary-section {
  margin-bottom: 2rem;
}

.summary-section h4 {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: #333;
}

.room-summary {
  display: flex;
  align-items: center;
  padding: 1rem;
  background: #f9f9f9;
  border-radius: 8px;
}

.room-image-small {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 1rem;
}

.room-details p {
  font-size: 0.875rem;
  color: #666;
  margin-bottom: 0.25rem;
}

.booking-details {
  space-y: 0.5rem;
}

.booking-details p {
  font-size: 0.875rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.special-requests {
  margin-top: 0.5rem;
  padding: 0.5rem;
  background: #fff3cd;
  border-radius: 4px;
}

.coupon-section {
  margin-top: 1rem;
  padding: 1rem;
  background: #f0f8ff;
  border-radius: 8px;
}

.coupon-input-group {
  display: flex;
  gap: 0.5rem;
}

.apply-coupon-btn {
  padding: 0.75rem 1rem;
  background: #1976d2;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  white-space: nowrap;
}

.apply-coupon-btn:hover:not(:disabled) {
  background: #1565c0;
}

.apply-coupon-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.wizard-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 2rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e5e5;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
}

.btn-secondary:hover:not(:disabled) {
  background: #eeeeee;
}

.btn-primary {
  background: #1976d2;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #1565c0;
}

.btn-large {
  padding: 1rem 2rem;
  font-size: 1rem;
}

.btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.loading-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid #ffffff;
  border-radius: 50%;
  border-top-color: transparent;
  animation: spin 1s ease-in-out infinite;
  margin-right: 0.5rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background: #f44336;
  color: white;
  padding: 1rem 1.5rem;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 1.25rem;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

@media (max-width: 768px) {
  .booking-wizard {
    padding: 1rem;
  }

  .steps {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .step:not(:last-child)::after {
    display: none;
  }

  .date-guest-section {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .wizard-footer {
    flex-direction: column;
    gap: 1rem;
  }
}
</style>