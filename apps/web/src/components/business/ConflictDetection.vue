<template>
  <div class="conflict-detection" v-if="show">
    <div class="conflict-detection__alert" :class="conflictTypeClass">
      <div class="alert__icon">
        <i class="bi" :class="conflictIcon"></i>
      </div>
      <div class="alert__content">
        <h4 class="alert__title">{{ conflictTitle }}</h4>
        <p class="alert__message">{{ conflictMessage }}</p>

        <!-- 冲突详情 -->
        <div v-if="conflictDetails" class="conflict-details">
          <div class="conflict-details__item">
            <span class="label">冲突类型：</span>
            <span class="value">{{ conflictDetails.conflictType }}</span>
          </div>
          <div class="conflict-details__item" v-if="conflictDetails.conflictingOrderId">
            <span class="label">冲突订单号：</span>
            <span class="value">#{{ conflictDetails.conflictingOrderId }}</span>
          </div>
        </div>

        <!-- 替代房间建议 -->
        <div v-if="alternativeRooms && alternativeRooms.length > 0" class="alternative-rooms">
          <h5 class="alternative-rooms__title">推荐替代房间</h5>
          <div class="room-suggestions">
            <div
              v-for="room in alternativeRooms"
              :key="room.roomId"
              class="room-suggestion"
              @click="selectAlternativeRoom(room)"
            >
              <div class="room-suggestion__number">{{ room.roomNumber }}</div>
              <div class="room-suggestion__type">{{ room.roomType }}</div>
              <div class="room-suggestion__price">
                <span class="current-price">¥{{ room.price }}</span>
                <span v-if="room.originalPrice && room.originalPrice > room.price"
                      class="original-price">¥{{ room.originalPrice }}</span>
                <span v-if="room.discount" class="discount-badge">-{{ room.discount }}%</span>
              </div>
              <div class="room-suggestion__status" :class="{ available: room.available }">
                {{ room.available ? '可预订' : '已占用' }}
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="conflict-actions">
          <button
            class="btn btn-outline-primary"
            @click="joinWaitingList"
            :disabled="joiningWaitingList"
          >
            <i class="bi bi-clock-history"></i>
            <span v-if="joiningWaitingList">加入中...</span>
            <span v-else>加入等待列表</span>
          </button>

          <button
            class="btn btn-secondary"
            @click="changeDates"
          >
            <i class="bi bi-calendar3"></i>
            更改日期
          </button>

          <button
            class="btn btn-link"
            @click="closeConflictAlert"
          >
            稍后再说
          </button>
        </div>
      </div>
    </div>

    <!-- 等待列表确认弹窗 -->
    <div v-if="showWaitingListModal" class="modal-backdrop">
      <div class="modal">
        <div class="modal__header">
          <h3 class="modal__title">加入等待列表确认</h3>
          <button class="modal__close" @click="closeWaitingListModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal__body">
          <div class="waiting-list-info">
            <p>您确定要加入此房间的等待列表吗？</p>
            <div class="info-item">
              <span class="label">房间：</span>
              <span class="value">房间 {{ roomId }}</span>
            </div>
            <div class="info-item">
              <span class="label">入住日期：</span>
              <span class="value">{{ formatDate(checkInDate) }}</span>
            </div>
            <div class="info-item">
              <span class="label">退房日期：</span>
              <span class="value">{{ formatDate(checkOutDate) }}</span>
            </div>
            <div class="info-item">
              <span class="label">客人数量：</span>
              <span class="value">{{ guestCount }}人</span>
            </div>
            <div class="info-item highlight">
              <span class="label">预计等待时间：</span>
              <span class="value">约{{ estimatedWaitTime }}天</span>
            </div>
          </div>
        </div>
        <div class="modal__footer">
          <button class="btn btn-secondary" @click="closeWaitingListModal">
            取消
          </button>
          <button
            class="btn btn-primary"
            @click="confirmJoinWaitingList"
            :disabled="joiningWaitingList"
          >
            <i class="bi bi-check-circle"></i>
            <span v-if="joiningWaitingList">加入中...</span>
            <span v-else>确认加入</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 成功提示 -->
    <div v-if="showSuccessMessage" class="success-toast">
      <i class="bi bi-check-circle-fill"></i>
      <span>{{ successMessage }}</span>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { bookingConflictService } from '@/services/bookingConflictService'
import { formatDate } from '@/utils/dateUtils'
import { useUserStore } from '@/stores/user'

export default {
  name: 'ConflictDetection',
  props: {
    show: {
      type: Boolean,
      default: false
    },
    roomId: {
      type: [String, Number],
      required: true
    },
    checkInDate: {
      type: String,
      required: true
    },
    checkOutDate: {
      type: String,
      required: true
    },
    guestCount: {
      type: Number,
      default: 1
    },
    conflictData: {
      type: Object,
      default: null
    }
  },
  emits: ['close', 'room-selected', 'dates-changed', 'waiting-list-joined'],
  setup(props, { emit }) {
    const userStore = useUserStore()
    const conflictDetails = ref(null)
    const alternativeRooms = ref([])
    const showWaitingListModal = ref(false)
    const joiningWaitingList = ref(false)
    const showSuccessMessage = ref(false)
    const successMessage = ref('')
    const estimatedWaitTime = ref(0)

    // 获取当前用户ID的辅助函数
    const getCurrentUserId = async () => {
      try {
        if (userStore.currentUser?.id) {
          return userStore.currentUser.id
        }
        // 如果用户store中没有，尝试从API获取
        await userStore.fetchCurrentUser()
        return userStore.currentUser?.id
      } catch (error) {
        console.error('获取用户信息失败:', error)
        return null
      }
    }

    // 计算属性
    const conflictTitle = computed(() => {
      if (!conflictDetails.value) return ''
      switch (conflictDetails.value.conflictType) {
        case 'TIME_OVERLAP':
          return '时间冲突'
        case 'DOUBLE_BOOKING':
          return '重复预订'
        case 'CONCURRENT_REQUEST':
          return '并发请求冲突'
        default:
          return '预订冲突'
      }
    })

    const conflictMessage = computed(() => {
      if (!conflictDetails.value) return ''
      switch (conflictDetails.value.conflictType) {
        case 'TIME_OVERLAP':
          return '您选择的时间段已被其他用户预订，请选择其他时间或加入等待列表。'
        case 'DOUBLE_BOOKING':
          return '检测到重复的预订请求，系统已为您保护账户安全。'
        case 'CONCURRENT_REQUEST':
          return '其他用户正在预订此房间，请稍后重试或选择其他房间。'
        default:
          return '预订过程中遇到冲突，请选择替代方案。'
      }
    })

    const conflictTypeClass = computed(() => {
      if (!conflictDetails.value) return 'alert--warning'
      switch (conflictDetails.value.conflictType) {
        case 'TIME_OVERLAP':
          return 'alert--warning'
        case 'DOUBLE_BOOKING':
          return 'alert--danger'
        case 'CONCURRENT_REQUEST':
          return 'alert--info'
        default:
          return 'alert--warning'
      }
    })

    const conflictIcon = computed(() => {
      if (!conflictDetails.value) return 'bi-exclamation-triangle'
      switch (conflictDetails.value.conflictType) {
        case 'TIME_OVERLAP':
          return 'bi-calendar-x'
        case 'DOUBLE_BOOKING':
          return 'bi-shield-exclamation'
        case 'CONCURRENT_REQUEST':
          return 'bi-arrow-repeat'
        default:
          return 'bi-exclamation-triangle'
      }
    })

    // 监听冲突数据变化
    watch(() => props.conflictData, (newData) => {
      if (newData) {
        conflictDetails.value = newData
        alternativeRooms.value = newData.alternativeRooms || []
      }
    }, { immediate: true })

    // 方法
    const selectAlternativeRoom = (room) => {
      emit('room-selected', room)
      closeConflictAlert()
    }

    const changeDates = () => {
      emit('dates-changed')
    }

    const closeConflictAlert = () => {
      emit('close')
    }

    const joinWaitingList = () => {
      // 估算等待时间
      estimatedWaitTime.value = Math.max(1, Math.floor(Math.random() * 7) + 1)
      showWaitingListModal.value = true
    }

    const closeWaitingListModal = () => {
      showWaitingListModal.value = false
    }

    const confirmJoinWaitingList = async () => {
      try {
        joiningWaitingList.value = true

        // 获取当前用户ID - 安全修复：不应传递null
        const currentUserId = await getCurrentUserId()
        if (!currentUserId) {
          throw new Error('用户未登录，无法加入等待列表')
        }

        const response = await bookingConflictService.joinWaitingList({
          roomId: props.roomId,
          userId: currentUserId,
          checkInDate: new Date(props.checkInDate).toISOString(),
          checkOutDate: new Date(props.checkOutDate).toISOString(),
          guestCount: props.guestCount,
          specialRequests: ''
        })

        if (response.success) {
          showSuccessMessage.value = true
          successMessage.value = '已成功加入等待列表！房间可用时我们会第一时间通知您。'

          setTimeout(() => {
            showSuccessMessage.value = false
            emit('waiting-list-joined', response.data)
            closeWaitingListModal()
            closeConflictAlert()
          }, 3000)
        }
      } catch (error) {
        console.error('加入等待列表失败:', error)
        // 可以显示错误提示
      } finally {
        joiningWaitingList.value = false
      }
    }

    return {
      conflictDetails,
      alternativeRooms,
      showWaitingListModal,
      joiningWaitingList,
      showSuccessMessage,
      successMessage,
      estimatedWaitTime,
      conflictTitle,
      conflictMessage,
      conflictTypeClass,
      conflictIcon,
      selectAlternativeRoom,
      changeDates,
      closeConflictAlert,
      joinWaitingList,
      closeWaitingListModal,
      confirmJoinWaitingList,
      formatDate
    }
  }
}
</script>

<style scoped>
.conflict-detection {
  margin: 16px 0;
}

.conflict-detection__alert {
  display: flex;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid;
  gap: 12px;
}

.alert--warning {
  background-color: #fff8e6;
  border-color: #ffc107;
  color: #856404;
}

.alert--danger {
  background-color: #f8d7da;
  border-color: #f5c6cb;
  color: #721c24;
}

.alert--info {
  background-color: #d1ecf1;
  border-color: #bee5eb;
  color: #0c5460;
}

.alert__icon {
  font-size: 24px;
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}

.alert__content {
  flex: 1;
}

.alert__title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
}

.alert__message {
  margin: 0 0 16px 0;
  font-size: 14px;
  line-height: 1.5;
}

.conflict-details {
  background-color: rgba(255, 255, 255, 0.5);
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.conflict-details__item {
  display: flex;
  margin-bottom: 4px;
  font-size: 14px;
}

.conflict-details__item:last-child {
  margin-bottom: 0;
}

.label {
  font-weight: 500;
  margin-right: 8px;
  min-width: 80px;
}

.value {
  color: inherit;
  opacity: 0.8;
}

.alternative-rooms {
  margin-bottom: 16px;
}

.alternative-rooms__title {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
}

.room-suggestions {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.room-suggestion {
  flex: 0 0 200px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background-color: rgba(255, 255, 255, 0.7);
}

.room-suggestion:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.room-suggestion__number {
  font-weight: 600;
  margin-bottom: 4px;
}

.room-suggestion__type {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.room-suggestion__price {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.current-price {
  font-weight: 600;
  color: #e91e63;
}

.original-price {
  font-size: 12px;
  text-decoration: line-through;
  color: #999;
}

.discount-badge {
  font-size: 10px;
  background-color: #4caf50;
  color: white;
  padding: 2px 4px;
  border-radius: 3px;
}

.room-suggestion__status {
  font-size: 12px;
  color: #999;
}

.room-suggestion__status.available {
  color: #4caf50;
  font-weight: 500;
}

.conflict-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background-color: #1976d2;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #1565c0;
}

.btn-outline-primary {
  background-color: transparent;
  color: #1976d2;
  border: 1px solid #1976d2;
}

.btn-outline-primary:hover:not(:disabled) {
  background-color: #1976d2;
  color: white;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #5a6268;
}

.btn-link {
  background-color: transparent;
  color: inherit;
  border: none;
  text-decoration: underline;
}

/* Modal styles */
.modal-backdrop {
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
}

.modal {
  background-color: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.modal__close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.modal__close:hover {
  background-color: #f5f5f5;
}

.modal__body {
  padding: 20px;
}

.modal__footer {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #eee;
}

.waiting-list-info {
  font-size: 14px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item.highlight {
  background-color: #e8f5e8;
  padding: 12px;
  border-radius: 6px;
  margin-top: 12px;
  border-bottom: none;
}

.success-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background-color: #4caf50;
  color: white;
  padding: 12px 20px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 1001;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* Responsive */
@media (max-width: 768px) {
  .conflict-detection__alert {
    flex-direction: column;
    gap: 8px;
  }

  .conflict-actions {
    flex-direction: column;
  }

  .room-suggestions {
    flex-direction: column;
  }

  .room-suggestion {
    flex: 1;
  }
}
</style>