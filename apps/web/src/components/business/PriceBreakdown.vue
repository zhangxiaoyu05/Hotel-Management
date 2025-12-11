<template>
  <div class="price-breakdown">
    <h4>价格明细</h4>

    <div class="price-details">
      <div class="price-row">
        <span class="label">房费 (¥{{ room?.price }}/晚 × {{ nights }}晚)</span>
        <span class="value">¥{{ roomFee.toFixed(2) }}</span>
      </div>

      <div class="price-row">
        <span class="label">服务费 (10%)</span>
        <span class="value">¥{{ serviceFee.toFixed(2) }}</span>
      </div>

      <div v-if="discountAmount > 0" class="price-row discount">
        <span class="label">
          优惠减免
          <span v-if="couponCode" class="coupon-tag">({{ couponCode }})</span>
        </span>
        <span class="value">-¥{{ discountAmount.toFixed(2) }}</span>
      </div>

      <div class="price-row total">
        <span class="label">总计</span>
        <span class="value">¥{{ totalPrice.toFixed(2) }}</span>
      </div>
    </div>

    <div v-if="savedAmount > 0" class="saved-amount">
      <span class="saved-icon">💰</span>
      您已节省 ¥{{ savedAmount.toFixed(2) }}
    </div>

    <div class="price-tips">
      <p class="tip">
        <span class="icon">ℹ️</span>
        预订成功后将立即扣款，支持多种支付方式
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { Room } from '@/types/room'

interface Props {
  room: Room
  checkInDate: string
  checkOutDate: string
  couponCode?: string
}

interface Emits {
  (e: 'update:priceBreakdown', breakdown: {
    roomFee: number
    serviceFee: number
    discountAmount: number
    totalPrice: number
    nights: number
  }): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const SERVICE_FEE_RATE = 0.10

const coupons: Record<string, number> = {
  'NEWYEAR2024': 0.15,
  'WELCOME10': 0.10,
  'SPECIAL20': 0.20,
  'FIRST5': 0.05
}

// 计算属性
const nights = computed(() => {
  if (!props.checkInDate || !props.checkOutDate) return 0

  const start = new Date(props.checkInDate)
  const end = new Date(props.checkOutDate)
  const diffTime = Math.abs(end.getTime() - start.getTime())
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
})

const roomFee = computed(() => {
  if (!props.room || nights.value === 0) return 0
  return props.room.price * nights.value
})

const serviceFee = computed(() => {
  return roomFee.value * SERVICE_FEE_RATE
})

const discountRate = computed(() => {
  if (!props.couponCode) return 0
  return coupons[props.couponCode.toUpperCase()] || 0
})

const discountAmount = computed(() => {
  return roomFee.value * discountRate.value
})

const totalPrice = computed(() => {
  return roomFee.value + serviceFee.value - discountAmount.value
})

const savedAmount = computed(() => {
  return discountAmount.value
})

const priceBreakdown = computed(() => ({
  roomFee: roomFee.value,
  serviceFee: serviceFee.value,
  discountAmount: discountAmount.value,
  totalPrice: totalPrice.value,
  nights: nights.value
}))

// 监听价格变化并发出事件
watch(
  priceBreakdown,
  (newBreakdown) => {
    emit('update:priceBreakdown', newBreakdown)
  },
  { immediate: true }
)

// 监听优惠券代码变化
watch(
  () => props.couponCode,
  () => {
    // 优惠券变化时会自动重新计算
  }
)
</script>

<style scoped>
.price-breakdown {
  background: #f9f9f9;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 1.5rem;
  margin: 1rem 0;
}

.price-breakdown h4 {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: #333;
}

.price-details {
  space-y: 0.75rem;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
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
  border-bottom: none;
}

.price-row.discount {
  color: #4caf50;
}

.label {
  color: #666;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.value {
  font-weight: 500;
  color: #333;
}

.coupon-tag {
  background: #4caf50;
  color: white;
  padding: 0.125rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
}

.saved-amount {
  background: linear-gradient(135deg, #4caf50, #45a049);
  color: white;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  text-align: center;
  font-weight: 600;
  margin-top: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.saved-icon {
  font-size: 1.25rem;
}

.price-tips {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #e3f2fd;
  border-radius: 6px;
  border-left: 4px solid #1976d2;
}

.tip {
  font-size: 0.875rem;
  color: #1976d2;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.icon {
  font-size: 1rem;
}

@media (max-width: 768px) {
  .price-breakdown {
    padding: 1rem;
  }

  .price-row {
    font-size: 0.8rem;
  }

  .price-row.total {
    font-size: 0.9rem;
  }
}
</style>