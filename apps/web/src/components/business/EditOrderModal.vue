<template>
  <teleport to="body">
    <div class="modal-overlay" @click="handleClose">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>修改订单</h2>
          <button @click="handleClose" class="close-button">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="modal-body">
          <form @submit.prevent="handleSubmit">
            <!-- 订单基本信息 -->
            <div class="order-summary">
              <h3>订单信息</h3>
              <div class="summary-grid">
                <div class="summary-item">
                  <label>订单号：</label>
                  <span>{{ order?.orderNumber }}</span>
                </div>
                <div class="summary-item">
                  <label>酒店：</label>
                  <span>{{ order?.hotel?.name }}</span>
                </div>
                <div class="summary-item">
                  <label>房间：</label>
                  <span>{{ order?.room?.name }} ({{ order?.room?.roomNumber }})</span>
                </div>
                <div class="summary-item">
                  <label>入住时间：</label>
                  <span>{{ formatDate(order?.checkInDate) }}</span>
                </div>
                <div class="summary-item">
                  <label>退房时间：</label>
                  <span>{{ formatDate(order?.checkOutDate) }}</span>
                </div>
                <div class="summary-item">
                  <label>入住人数：</label>
                  <span>{{ order?.guestCount }}人</span>
                </div>
              </div>
            </div>

            <!-- 可修改的字段 -->
            <div class="edit-form">
              <h3>修改信息</h3>
              <p class="edit-notice">
                <i class="fas fa-info-circle"></i>
                注意：只能修改特殊要求，其他信息如房间、日期、人数等不可修改
              </p>

              <div class="form-group">
                <label for="specialRequests">特殊要求</label>
                <textarea
                  id="specialRequests"
                  v-model="formData.specialRequests"
                  rows="4"
                  placeholder="请输入您的特殊要求，如需要无烟房、高楼层、婴儿床等"
                  maxlength="500"
                ></textarea>
                <div class="char-count">
                  {{ (formData.specialRequests?.length || 0) }}/500
                </div>
              </div>
            </div>

            <!-- 修改限制说明 -->
            <div class="restrictions">
              <h4>修改限制</h4>
              <ul>
                <li>只能在入住前24小时以上修改订单</li>
                <li>只能修改待确认或已确认状态的订单</li>
                <li>房间、日期、人数等核心信息不可修改</li>
                <li>每次修改都会记录在订单历史中</li>
              </ul>
            </div>
          </form>
        </div>

        <div class="modal-footer">
          <button @click="handleClose" class="btn-cancel" :disabled="loading">
            取消
          </button>
          <button
            @click="handleSubmit"
            class="btn-submit"
            :disabled="loading || !hasChanges"
          >
            <i v-if="loading" class="fas fa-spinner fa-spin"></i>
            {{ loading ? '保存中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { orderService } from '@/services/orderService'
import type { OrderListResponse, OrderResponse, UpdateOrderRequest } from '@/types/order'

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
const formData = reactive<UpdateOrderRequest>({
  specialRequests: ''
})

const hasChanges = computed(() => {
  if (!props.order) return false
  return formData.specialRequests !== (props.order.specialRequests || '')
})

const formatDate = (dateString?: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const handleClose = () => {
  if (loading.value) return
  emit('close')
}

const handleSubmit = async () => {
  if (!props.order || !hasChanges.value) return

  loading.value = true

  try {
    // 构建更新请求数据，只包含有变化的字段
    const updateData: UpdateOrderRequest = {}

    if (formData.specialRequests !== props.order.specialRequests) {
      updateData.specialRequests = formData.specialRequests
    }

    // 如果没有实际需要更新的数据，直接关闭
    if (Object.keys(updateData).length === 0) {
      emit('close')
      return
    }

    await orderService.updateOrder(props.order.id, updateData)

    emit('success')
  } catch (error: any) {
    console.error('修改订单失败:', error)
    alert(error.message || '修改订单失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (props.order) {
    formData.specialRequests = props.order.specialRequests || ''
  }
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
  max-width: 700px;
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

.order-summary,
.edit-form,
.restrictions {
  margin-bottom: 30px;
}

.order-summary h3,
.edit-form h3,
.restrictions h4 {
  color: #2c3e50;
  margin-bottom: 15px;
  font-size: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
  background-color: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.summary-item label {
  font-weight: 500;
  color: #666;
  min-width: 80px;
}

.summary-item span {
  color: #2c3e50;
}

.edit-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #e3f2fd;
  color: #1976d2;
  padding: 15px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 20px;
}

.edit-notice i {
  color: #1976d2;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
  font-family: inherit;
}

.form-group textarea:focus {
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

.restrictions {
  background-color: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 20px;
}

.restrictions h4 {
  color: #856404;
  margin-bottom: 10px;
}

.restrictions ul {
  margin: 0;
  padding-left: 20px;
  color: #856404;
}

.restrictions li {
  margin-bottom: 5px;
  font-size: 14px;
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
  min-width: 100px;
}

.btn-cancel {
  background-color: #6c757d;
  color: white;
}

.btn-cancel:hover:not(:disabled) {
  background-color: #5a6268;
}

.btn-submit {
  background-color: #3498db;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-submit:hover:not(:disabled) {
  background-color: #2980b9;
}

.btn-cancel:disabled,
.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-submit:disabled {
  background-color: #3498db;
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

  .summary-grid {
    grid-template-columns: 1fr;
    gap: 10px;
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