<template>
  <teleport to="body">
    <div class="modal-overlay" @click="handleClose">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>取消订单</h2>
          <button @click="handleClose" class="close-button">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="modal-body">
          <!-- 订单信息 -->
          <div class="order-info">
            <h3>订单信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <label>订单号：</label>
                <span>{{ order?.orderNumber }}</span>
              </div>
              <div class="info-item">
                <label>酒店：</label>
                <span>{{ order?.hotel?.name }}</span>
              </div>
              <div class="info-item">
                <label>房间：</label>
                <span>{{ order?.room?.name }}</span>
              </div>
              <div class="info-item">
                <label>入住时间：</label>
                <span>{{ formatDate(order?.checkInDate) }}</span>
              </div>
              <div class="info-item">
                <label>退房时间：</label>
                <span>{{ formatDate(order?.checkOutDate) }}</span>
              </div>
              <div class="info-item">
                <label>订单金额：</label>
                <span class="price">¥{{ order?.totalPrice?.toFixed(2) }}</span>
              </div>
            </div>
          </div>

          <!-- 取消原因 -->
          <div class="cancel-reason">
            <h3>取消原因</h3>
            <div class="reason-options">
              <label class="reason-option">
                <input
                  type="radio"
                  v-model="selectedReason"
                  value="行程变更"
                  name="cancelReason"
                />
                <span class="radio-text">行程变更</span>
              </label>
              <label class="reason-option">
                <input
                  type="radio"
                  v-model="selectedReason"
                  value="找到更好的选择"
                  name="cancelReason"
                />
                <span class="radio-text">找到更好的选择</span>
              </label>
              <label class="reason-option">
                <input
                  type="radio"
                  v-model="selectedReason"
                  value="个人原因"
                  name="cancelReason"
                />
                <span class="radio-text">个人原因</span>
              </label>
              <label class="reason-option">
                <input
                  type="radio"
                  v-model="selectedReason"
                  value="酒店服务问题"
                  name="cancelReason"
                />
                <span class="radio-text">酒店服务问题</span>
              </label>
              <label class="reason-option">
                <input
                  type="radio"
                  v-model="selectedReason"
                  value="其他"
                  name="cancelReason"
                />
                <span class="radio-text">其他</span>
              </label>
            </div>

            <div v-if="selectedReason === '其他'" class="custom-reason">
              <label for="customReason">请说明具体原因</label>
              <textarea
                id="customReason"
                v-model="customReason"
                rows="3"
                placeholder="请详细描述取消订单的原因"
                maxlength="200"
              ></textarea>
              <div class="char-count">
                {{ customReason.length }}/200
              </div>
            </div>
          </div>

          <!-- 退款政策 -->
          <div class="refund-policy">
            <h3>退款政策</h3>
            <div class="policy-content">
              <div class="policy-item">
                <div class="policy-time">提前7天以上</div>
                <div class="policy-detail">
                  <div class="policy-rate">全额退款</div>
                  <div class="policy-desc">退还订单全额款项</div>
                </div>
              </div>
              <div class="policy-item">
                <div class="policy-time">提前3-7天</div>
                <div class="policy-detail">
                  <div class="policy-rate">退款80%</div>
                  <div class="policy-desc">扣除订单金额的20%作为手续费</div>
                </div>
              </div>
              <div class="policy-item">
                <div class="policy-time">提前1-3天</div>
                <div class="policy-detail">
                  <div class="policy-rate">退款50%</div>
                  <div class="policy-desc">扣除订单金额的50%作为手续费</div>
                </div>
              </div>
              <div class="policy-item">
                <div class="policy-time">当天或之后</div>
                <div class="policy-detail">
                  <div class="policy-rate">不可退款</div>
                  <div class="policy-desc">入住当天或之后取消不退款</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 退款金额计算 -->
          <div v-if="refundInfo" class="refund-calculation">
            <h3>退款金额</h3>
            <div class="refund-breakdown">
              <div class="refund-item">
                <span>订单金额：</span>
                <span>¥{{ order?.totalPrice?.toFixed(2) }}</span>
              </div>
              <div class="refund-item">
                <span>退款比例：</span>
                <span>{{ refundPercentage }}%</span>
              </div>
              <div class="refund-item">
                <span>退款金额：</span>
                <span class="refund-amount">¥{{ refundInfo.refundAmount.toFixed(2) }}</span>
              </div>
            </div>
          </div>

          <!-- 确认提示 -->
          <div class="warning">
            <i class="fas fa-exclamation-triangle"></i>
            <div>
              <strong>重要提示：</strong>
              <ul>
                <li>订单取消后将无法恢复</li>
                <li>退款将在3-7个工作日内原路返还</li>
                <li>如有疑问请联系客服</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button @click="handleClose" class="btn-cancel" :disabled="loading">
            再考虑一下
          </button>
          <button
            @click="handleSubmit"
            class="btn-submit"
            :disabled="loading || !canSubmit"
          >
            <i v-if="loading" class="fas fa-spinner fa-spin"></i>
            {{ loading ? '取消中...' : '确认取消订单' }}
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { orderService } from '@/services/orderService'
import type { OrderListResponse, OrderResponse } from '@/types/order'

interface Props {
  order: OrderListResponse | OrderResponse | null
}

interface Emits {
  (e: 'close'): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const selectedReason = ref('')
const customReason = ref('')

// 计算退款信息
const refundInfo = computed(() => {
  if (!props.order) return null

  const now = new Date()
  const checkInDate = new Date(props.order.checkInDate)
  const daysUntilCheckIn = Math.ceil((checkInDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))

  let refundPercentage = 0
  let refundAmount = 0

  if (daysUntilCheckIn >= 7) {
    refundPercentage = 100
    refundAmount = props.order.totalPrice
  } else if (daysUntilCheckIn >= 3) {
    refundPercentage = 80
    refundAmount = props.order.totalPrice * 0.8
  } else if (daysUntilCheckIn >= 1) {
    refundPercentage = 50
    refundAmount = props.order.totalPrice * 0.5
  } else {
    refundPercentage = 0
    refundAmount = 0
  }

  return {
    refundPercentage,
    refundAmount,
    daysUntilCheckIn
  }
})

const refundPercentage = computed(() => refundInfo.value?.refundPercentage || 0)

const canSubmit = computed(() => {
  if (!selectedReason.value) return false
  if (selectedReason.value === '其他' && !customReason.value.trim()) return false
  return true
})

const formatDate = (dateString?: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const getCancelReason = () => {
  if (selectedReason.value === '其他') {
    return customReason.value.trim()
  }
  return selectedReason.value
}

const handleClose = () => {
  if (loading.value) return
  emit('close')
}

const handleSubmit = async () => {
  if (!props.order || !canSubmit.value) return

  loading.value = true

  try {
    const cancelReason = getCancelReason()
    await orderService.cancelOrder(props.order.id, cancelReason)

    emit('success')
  } catch (error: any) {
    console.error('取消订单失败:', error)
    alert(error.message || '取消订单失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 默认选择第一个原因
  selectedReason.value = '行程变更'
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 12px;
  max-width: 800px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 24px;
}

.close-button {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.3s;
}

.close-button:hover {
  background-color: #f8f9fa;
  color: #666;
}

.modal-body {
  padding: 24px;
}

.order-info,
.cancel-reason,
.refund-policy,
.refund-calculation {
  margin-bottom: 30px;
}

.order-info h3,
.cancel-reason h3,
.refund-policy h3,
.refund-calculation h3 {
  color: #2c3e50;
  margin-bottom: 15px;
  font-size: 18px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  background-color: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-item label {
  font-weight: 500;
  color: #666;
  min-width: 80px;
}

.info-item span {
  color: #2c3e50;
}

.info-item .price {
  color: #e74c3c;
  font-weight: 600;
  font-size: 16px;
}

.reason-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 20px;
}

.reason-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.reason-option:hover {
  border-color: #3498db;
  background-color: #f8f9fa;
}

.reason-option input[type="radio"] {
  accent-color: #3498db;
}

.radio-text {
  color: #333;
  font-size: 14px;
}

.custom-reason {
  margin-top: 15px;
}

.custom-reason label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.custom-reason textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
  font-family: inherit;
}

.custom-reason textarea:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
}

.char-count {
  text-align: right;
  color: #999;
  font-size: 12px;
  margin-top: 5px;
}

.policy-content {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
}

.policy-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.policy-item:last-child {
  border-bottom: none;
}

.policy-time {
  width: 120px;
  font-weight: 500;
  color: #333;
}

.policy-detail {
  flex: 1;
}

.policy-rate {
  font-weight: 600;
  color: #28a745;
  margin-bottom: 4px;
}

.policy-desc {
  color: #666;
  font-size: 14px;
}

.refund-breakdown {
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
  font-weight: 600;
  font-size: 16px;
}

.refund-amount {
  color: #155724;
  font-size: 18px;
  font-weight: 700;
}

.warning {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background-color: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 20px;
  color: #856404;
}

.warning i {
  color: #f39c12;
  font-size: 20px;
  margin-top: 2px;
}

.warning strong {
  color: #856404;
  display: block;
  margin-bottom: 8px;
}

.warning ul {
  margin: 0;
  padding-left: 20px;
}

.warning li {
  margin-bottom: 4px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  padding: 20px 24px;
  border-top: 1px solid #eee;
  background-color: #f8f9fa;
}

.btn-cancel,
.btn-submit {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 120px;
}

.btn-cancel {
  background-color: #6c757d;
  color: white;
}

.btn-cancel:hover:not(:disabled) {
  background-color: #5a6268;
}

.btn-submit {
  background-color: #e74c3c;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-submit:hover:not(:disabled) {
  background-color: #c0392b;
}

.btn-cancel:disabled,
.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-submit:disabled {
  background-color: #e74c3c;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-overlay {
    padding: 10px;
  }

  .modal-content {
    max-height: 95vh;
  }

  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 20px;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .reason-options {
    grid-template-columns: 1fr;
  }

  .policy-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .policy-time {
    width: auto;
  }

  .modal-footer {
    flex-direction: column;
  }

  .btn-cancel,
  .btn-submit {
    width: 100%;
  }
}

/* 滚动条样式 */
.modal-content::-webkit-scrollbar {
  width: 8px;
}

.modal-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.modal-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.modal-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>