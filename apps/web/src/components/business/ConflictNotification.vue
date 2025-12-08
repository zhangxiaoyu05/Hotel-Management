<template>
  <div class="conflict-notification" v-if="notification">
    <div class="notification-container" :class="notificationClass">
      <div class="notification-icon">
        <i class="bi" :class="notificationIcon"></i>
      </div>
      <div class="notification-content">
        <h4 class="notification-title">{{ notification.title }}</h4>
        <p class="notification-message">{{ notification.content }}</p>

        <!-- 通知详情 -->
        <div v-if="notificationDetails" class="notification-details">
          <div class="detail-item" v-for="(value, key) in notificationDetails" :key="key">
            <span class="detail-label">{{ formatDetailKey(key) }}：</span>
            <span class="detail-value">{{ formatDetailValue(key, value) }}</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="notification-actions">
          <button
            v-if="notification.type === 'ROOM_AVAILABLE'"
            class="btn btn-primary btn-sm"
            @click="handleRoomAvailable"
          >
            <i class="bi bi-check-circle"></i>
            立即预订
          </button>

          <button
            v-if="notification.type === 'BOOKING_CONFLICT'"
            class="btn btn-outline-primary btn-sm"
            @click="handleBookingConflict"
          >
            <i class="bi bi-eye"></i>
            查看详情
          </button>

          <button
            v-if="notification.type === 'WAITING_LIST'"
            class="btn btn-outline-secondary btn-sm"
            @click="viewWaitingList"
          >
            <i class="bi bi-list-ul"></i>
            查看等待列表
          </button>

          <button
            class="btn btn-link btn-sm"
            @click="markAsRead"
          >
            标记已读
          </button>
        </div>
      </div>

      <button class="notification-close" @click="closeNotification">
        <i class="bi bi-x-lg"></i>
      </button>
    </div>

    <!-- 成功确认提示 -->
    <div v-if="showConfirmSuccess" class="confirmation-toast">
      <i class="bi bi-check-circle-fill text-success"></i>
      <span>{{ confirmMessage }}</span>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { bookingConflictService } from '@/services/bookingConflictService'
import { formatDate, formatDateTime } from '@/utils/dateUtils'
import { useRouter } from 'vue-router'

export default {
  name: 'ConflictNotification',
  props: {
    notification: {
      type: Object,
      default: null
    },
    autoClose: {
      type: Boolean,
      default: true
    },
    duration: {
      type: Number,
      default: 5000
    }
  },
  emits: ['close', 'mark-read', 'action'],
  setup(props, { emit }) {
    const router = useRouter()
    const showConfirmSuccess = ref(false)
    const confirmMessage = ref('')

    // 计算属性
    const notificationClass = computed(() => {
      if (!props.notification) return ''

      const baseClass = 'notification-container'
      const typeClass = `notification--${props.notification.type?.toLowerCase()}`
      const unreadClass = !props.notification.isRead ? 'notification--unread' : ''

      return [baseClass, typeClass, unreadClass].filter(Boolean).join(' ')
    })

    const notificationIcon = computed(() => {
      if (!props.notification) return 'bi-info-circle'

      const iconMap = {
        'ROOM_AVAILABLE': 'bi-calendar-check',
        'BOOKING_CONFLICT': 'bi-exclamation-triangle',
        'WAITING_LIST': 'bi-clock-history',
        'WAITING_LIST_EXPIRED': 'bi-x-circle',
        'default': 'bi-info-circle'
      }

      return iconMap[props.notification.type] || iconMap['default']
    })

    const notificationDetails = computed(() => {
      if (!props.notification) return null

      // 根据通知内容解析详细信息
      const details = {}
      const content = props.notification.content || ''

      // 提取房间ID
      const roomIdMatch = content.match(/房间[：:]?\s*(\d+)/)
      if (roomIdMatch) {
        details.roomId = roomIdMatch[1]
      }

      // 提取日期信息
      const dateMatches = content.match(/(\d{4}-\d{2}-\d{2})/g)
      if (dateMatches) {
        if (dateMatches.length >= 1) details.checkInDate = dateMatches[0]
        if (dateMatches.length >= 2) details.checkOutDate = dateMatches[1]
      }

      // 提取客人数量
      const guestMatch = content.match(/客人[：:]?\s*(\d+)/)
      if (guestMatch) {
        details.guestCount = guestMatch[1]
      }

      // 提取等待优先级
      const priorityMatch = content.match(/优先级[：:]?\s*(\d+)/)
      if (priorityMatch) {
        details.priority = priorityMatch[1]
      }

      return Object.keys(details).length > 0 ? details : null
    })

    // 监听通知变化，自动关闭
    watch(() => props.notification, (newNotification) => {
      if (newNotification && props.autoClose) {
        setTimeout(() => {
          closeNotification()
        }, props.duration)
      }
    })

    // 方法
    const formatDetailKey = (key) => {
      const keyMap = {
        'roomId': '房间ID',
        'checkInDate': '入住日期',
        'checkOutDate': '退房日期',
        'guestCount': '客人数量',
        'priority': '优先级'
      }
      return keyMap[key] || key
    }

    const formatDetailValue = (key, value) => {
      if (key.includes('Date')) {
        return formatDate(value)
      }
      return value
    }

    const handleRoomAvailable = async () => {
      try {
        if (notificationDetails.value?.roomId) {
          // 跳转到预订页面或显示确认弹窗
          const roomId = notificationDetails.value.roomId
          router.push({
            name: 'RoomDetail',
            params: { id: roomId },
            query: {
              checkIn: notificationDetails.value.checkInDate,
              checkOut: notificationDetails.value.checkOutDate,
              guests: notificationDetails.value.guestCount || 1,
              waitingListId: props.notification.relatedEntityId
            }
          })
        }
      } catch (error) {
        console.error('处理房间可用通知失败:', error)
      }
    }

    const handleBookingConflict = () => {
      // 跳转到冲突处理页面
      if (notificationDetails.value?.roomId) {
        router.push({
          name: 'BookingConflict',
          params: { roomId: notificationDetails.value.roomId }
        })
      }
    }

    const viewWaitingList = () => {
      // 跳转到等待列表页面
      router.push({
        name: 'WaitingList'
      })
    }

    const markAsRead = async () => {
      try {
        if (props.notification.id) {
          // 调用API标记为已读
          // await notificationService.markAsRead(props.notification.id)
        }
        emit('mark-read', props.notification)
        closeNotification()
      } catch (error) {
        console.error('标记已读失败:', error)
      }
    }

    const closeNotification = () => {
      emit('close')
    }

    const showSuccessMessage = (message) => {
      confirmMessage.value = message
      showConfirmSuccess.value = true

      setTimeout(() => {
        showConfirmSuccess.value = false
      }, 3000)
    }

    return {
      notificationClass,
      notificationIcon,
      notificationDetails,
      showConfirmSuccess,
      confirmMessage,
      handleRoomAvailable,
      handleBookingConflict,
      viewWaitingList,
      markAsRead,
      closeNotification,
      formatDetailKey,
      formatDetailValue
    }
  }
}
</script>

<style scoped>
.conflict-notification {
  position: relative;
  margin-bottom: 12px;
}

.notification-container {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  gap: 12px;
  transition: all 0.2s;
}

.notification-container:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.notification--unread {
  border-left: 4px solid #1976d2;
  background-color: #f8f9ff;
}

.notification--room_available {
  border-color: #4caf50;
  background-color: #f1f8e9;
}

.notification--booking_conflict {
  border-color: #ff9800;
  background-color: #fff8e6;
}

.notification--waiting_list {
  border-color: #2196f3;
  background-color: #e3f2fd;
}

.notification--waiting_list_expired {
  border-color: #f44336;
  background-color: #ffebee;
}

.notification-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
}

.notification--room_available .notification-icon {
  background-color: #4caf50;
}

.notification--booking_conflict .notification-icon {
  background-color: #ff9800;
}

.notification--waiting_list .notification-icon {
  background-color: #2196f3;
}

.notification--waiting_list_expired .notification-icon {
  background-color: #f44336;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
}

.notification-message {
  margin: 0 0 12px 0;
  font-size: 14px;
  line-height: 1.5;
  color: #666;
  white-space: pre-wrap;
}

.notification-details {
  background-color: rgba(255, 255, 255, 0.7);
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 12px;
}

.detail-item {
  display: flex;
  font-size: 13px;
  margin-bottom: 4px;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-label {
  font-weight: 500;
  color: #495057;
  min-width: 80px;
  margin-right: 8px;
}

.detail-value {
  color: #333;
  font-weight: 400;
}

.notification-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.notification-close {
  flex-shrink: 0;
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  color: #999;
  transition: all 0.2s;
}

.notification-close:hover {
  background-color: rgba(0, 0, 0, 0.1);
  color: #666;
}

.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
  text-decoration: none;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

.btn-primary {
  background-color: #1976d2;
  color: white;
}

.btn-primary:hover {
  background-color: #1565c0;
}

.btn-outline-primary {
  background-color: transparent;
  color: #1976d2;
  border: 1px solid #1976d2;
}

.btn-outline-primary:hover {
  background-color: #1976d2;
  color: white;
}

.btn-outline-secondary {
  background-color: transparent;
  color: #6c757d;
  border: 1px solid #6c757d;
}

.btn-outline-secondary:hover {
  background-color: #6c757d;
  color: white;
}

.btn-link {
  background-color: transparent;
  color: inherit;
  border: none;
  text-decoration: underline;
  padding: 4px 8px;
}

.btn-link:hover {
  color: #1976d2;
}

.confirmation-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background-color: white;
  border: 1px solid #4caf50;
  border-radius: 8px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 1001;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: slideIn 0.3s ease;
}

.text-success {
  color: #4caf50;
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
  .notification-container {
    flex-direction: column;
    gap: 8px;
  }

  .notification-icon {
    align-self: flex-start;
  }

  .notification-actions {
    flex-direction: column;
  }

  .btn {
    width: 100%;
    justify-content: center;
  }

  .confirmation-toast {
    right: 10px;
    left: 10px;
    top: 10px;
  }
}

/* Animation for notification entry */
.conflict-notification {
  animation: notificationSlide 0.3s ease;
}

@keyframes notificationSlide {
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
</style>