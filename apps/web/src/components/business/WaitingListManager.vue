<template>
  <div class="waiting-list-manager">
    <div class="manager-header">
      <h3 class="manager-title">
        <i class="bi bi-clock-history"></i>
        我的等待列表
      </h3>
      <div class="manager-actions">
        <button
          class="btn btn-outline-primary btn-sm"
          @click="refreshWaitingList"
          :disabled="loading"
        >
          <i class="bi bi-arrow-clockwise" :class="{ rotating: loading }"></i>
          刷新
        </button>
      </div>
    </div>

    <!-- 等待列表统计 -->
    <div class="waiting-stats">
      <div class="stat-card">
        <div class="stat-value">{{ totalWaiting }}</div>
        <div class="stat-label">等待中</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ totalNotified }}</div>
        <div class="stat-label">待确认</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ totalConfirmed }}</div>
        <div class="stat-label">已确认</div>
      </div>
    </div>

    <!-- 过滤器 -->
    <div class="filter-bar">
      <div class="filter-group">
        <label class="filter-label">状态：</label>
        <select v-model="selectedStatus" @change="loadWaitingList" class="form-select">
          <option value="">全部</option>
          <option value="WAITING">等待中</option>
          <option value="NOTIFIED">待确认</option>
          <option value="EXPIRED">已过期</option>
          <option value="CONFIRMED">已确认</option>
        </select>
      </div>
    </div>

    <!-- 等待列表内容 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载等待列表...</p>
    </div>

    <div v-else-if="waitingList.length === 0" class="empty-state">
      <i class="bi bi-clock"></i>
      <p>{{ emptyMessage }}</p>
    </div>

    <div v-else class="waiting-list-content">
      <div
        v-for="item in waitingList"
        :key="item.waitingListId"
        class="waiting-item"
        :class="getItemClass(item)"
      >
        <div class="item-header">
          <div class="room-info">
            <h4 class="room-number">房间 {{ item.roomNumber || item.roomId }}</h4>
            <span class="room-type">{{ item.roomType || '标准房间' }}</span>
          </div>
          <div class="item-status">
            <span class="status-badge" :class="getStatusClass(item.status)">
              {{ getStatusText(item.status) }}
            </span>
          </div>
        </div>

        <div class="item-details">
          <div class="detail-row">
            <i class="bi bi-calendar3"></i>
            <span class="detail-label">入住：</span>
            <span class="detail-value">{{ formatDate(item.requestedCheckInDate) }}</span>
          </div>
          <div class="detail-row">
            <i class="bi bi-calendar-x"></i>
            <span class="detail-label">退房：</span>
            <span class="detail-value">{{ formatDate(item.requestedCheckOutDate) }}</span>
          </div>
          <div class="detail-row">
            <i class="bi bi-people"></i>
            <span class="detail-label">客人：</span>
            <span class="detail-value">{{ item.guestCount }}人</span>
          </div>
          <div class="detail-row">
            <i class="bi bi-flag"></i>
            <span class="detail-label">优先级：</span>
            <span class="detail-value">{{ item.priority }}</span>
          </div>
        </div>

        <!-- 时间信息 -->
        <div class="time-info" v-if="item.notifiedAt || item.expiresAt">
          <div v-if="item.notifiedAt" class="time-row">
            <i class="bi bi-bell"></i>
            <span>通知时间：{{ formatDateTime(item.notifiedAt) }}</span>
          </div>
          <div v-if="item.expiresAt" class="time-row urgent">
            <i class="bi bi-hourglass-split"></i>
            <span>过期时间：{{ formatDateTime(item.expiresAt) }}</span>
            <span v-if="isExpired(item.expiresAt)" class="expired-badge">已过期</span>
            <span v-else-if="isExpiringSoon(item.expiresAt)" class="expiring-badge">即将过期</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="item-actions">
          <button
            v-if="item.status === 'NOTIFIED' && !isExpired(item.expiresAt)"
            class="btn btn-primary btn-sm"
            @click="confirmBooking(item)"
            :disabled="confirming"
          >
            <i class="bi bi-check-circle"></i>
            <span v-if="confirming">确认中...</span>
            <span v-else>确认预订</span>
          </button>

          <button
            v-if="item.status === 'WAITING'"
            class="btn btn-outline-danger btn-sm"
            @click="leaveWaitingList(item)"
            :disabled="leaving"
          >
            <i class="bi bi-x-circle"></i>
            <span v-if="leaving">退出中...</span>
            <span v-else>退出等待</span>
          </button>

          <button
            v-if="item.status === 'EXPIRED'"
            class="btn btn-outline-secondary btn-sm"
            @click="rejoinWaitingList(item)"
            :disabled="rejoining"
          >
            <i class="bi bi-arrow-clockwise"></i>
            <span v-if="rejoining">重新加入...</span>
            <span v-else>重新加入</span>
          </button>

          <button
            class="btn btn-outline-secondary btn-sm"
            @click="viewRoomDetails(item)"
          >
            <i class="bi bi-eye"></i>
            查看房间
          </button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <button
        class="btn btn-outline-secondary btn-sm"
        @click="prevPage"
        :disabled="currentPage === 1"
      >
        <i class="bi bi-chevron-left"></i>
        上一页
      </button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <button
        class="btn btn-outline-secondary btn-sm"
        @click="nextPage"
        :disabled="currentPage === totalPages"
      >
        下一页
        <i class="bi bi-chevron-right"></i>
      </button>
    </div>

    <!-- 确认预订弹窗 -->
    <div v-if="showConfirmModal" class="modal-backdrop">
      <div class="modal">
        <div class="modal__header">
          <h3 class="modal__title">确认预订</h3>
          <button class="modal__close" @click="closeConfirmModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal__body">
          <div class="booking-summary">
            <h4>预订详情</h4>
            <div class="summary-item">
              <span class="label">房间：</span>
              <span class="value">房间 {{ selectedItem?.roomId }}</span>
            </div>
            <div class="summary-item">
              <span class="label">入住日期：</span>
              <span class="value">{{ formatDate(selectedItem?.requestedCheckInDate) }}</span>
            </div>
            <div class="summary-item">
              <span class="label">退房日期：</span>
              <span class="value">{{ formatDate(selectedItem?.requestedCheckOutDate) }}</span>
            </div>
            <div class="summary-item">
              <span class="label">客人数量：</span>
              <span class="value">{{ selectedItem?.guestCount }}人</span>
            </div>
            <div class="summary-item urgent">
              <span class="label">⚠️ 重要提醒：</span>
              <span class="value">确认后不可更改，请仔细核对信息</span>
            </div>
          </div>
        </div>
        <div class="modal__footer">
          <button class="btn btn-secondary" @click="closeConfirmModal">
            取消
          </button>
          <button
            class="btn btn-primary"
            @click="submitConfirmBooking"
            :disabled="confirming"
          >
            <i class="bi bi-check-circle"></i>
            <span v-if="confirming">确认中...</span>
            <span v-else>确认预订</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { bookingConflictService } from '@/services/bookingConflictService'
import { formatDate, formatDateTime } from '@/utils/dateUtils'

export default {
  name: 'WaitingListManager',
  setup() {
    const waitingList = ref([])
    const loading = ref(false)
    const selectedStatus = ref('')
    const currentPage = ref(1)
    const pageSize = ref(10)
    const totalItems = ref(0)
    const totalPages = ref(1)

    const showConfirmModal = ref(false)
    const selectedItem = ref(null)
    const confirming = ref(false)
    const leaving = ref(false)
    const rejoining = ref(false)

    // 计算属性
    const totalWaiting = computed(() =>
      waitingList.value.filter(item => item.status === 'WAITING').length
    )

    const totalNotified = computed(() =>
      waitingList.value.filter(item => item.status === 'NOTIFIED').length
    )

    const totalConfirmed = computed(() =>
      waitingList.value.filter(item => item.status === 'CONFIRMED').length
    )

    const emptyMessage = computed(() => {
      if (selectedStatus.value) {
        switch (selectedStatus.value) {
          case 'WAITING':
            return '当前没有等待中的项目'
          case 'NOTIFIED':
            return '当前没有待确认的通知'
          case 'EXPIRED':
            return '当前没有过期的项目'
          case 'CONFIRMED':
            return '当前没有已确认的预订'
          default:
            return '没有找到相关的等待列表项目'
        }
      }
      return '您还没有加入任何等待列表'
    })

    // 方法
    const loadWaitingList = async () => {
      try {
        loading.value = true
        const response = await bookingConflictService.getWaitingList({
          status: selectedStatus.value,
          page: currentPage.value,
          size: pageSize.value
        })

        if (response.success) {
          waitingList.value = response.data.records || []
          totalItems.value = response.data.total || 0
          totalPages.value = Math.ceil(totalItems.value / pageSize.value)
        }
      } catch (error) {
        console.error('加载等待列表失败:', error)
      } finally {
        loading.value = false
      }
    }

    const refreshWaitingList = () => {
      loadWaitingList()
    }

    const getItemClass = (item) => {
      return {
        'item--notified': item.status === 'NOTIFIED',
        'item--expired': item.status === 'EXPIRED',
        'item--confirmed': item.status === 'CONFIRMED'
      }
    }

    const getStatusClass = (status) => {
      return `status--${status.toLowerCase()}`
    }

    const getStatusText = (status) => {
      const statusMap = {
        'WAITING': '等待中',
        'NOTIFIED': '待确认',
        'EXPIRED': '已过期',
        'CONFIRMED': '已确认'
      }
      return statusMap[status] || status
    }

    const isExpired = (expiresAt) => {
      return new Date(expiresAt) < new Date()
    }

    const isExpiringSoon = (expiresAt) => {
      const now = new Date()
      const expires = new Date(expiresAt)
      const hoursUntilExpiry = (expires - now) / (1000 * 60 * 60)
      return hoursUntilExpiry > 0 && hoursUntilExpiry <= 2
    }

    const confirmBooking = (item) => {
      selectedItem.value = item
      showConfirmModal.value = true
    }

    const closeConfirmModal = () => {
      showConfirmModal.value = false
      selectedItem.value = null
    }

    const submitConfirmBooking = async () => {
      try {
        confirming.value = true
        const response = await bookingConflictService.confirmWaitingListBooking(
          selectedItem.value.waitingListId,
          {
            specialRequests: ''
          }
        )

        if (response.success) {
          // 显示成功消息
          showSuccessMessage('预订确认成功！')
          closeConfirmModal()
          loadWaitingList()
        }
      } catch (error) {
        console.error('确认预订失败:', error)
        showErrorMessage('确认预订失败，请稍后重试')
      } finally {
        confirming.value = false
      }
    }

    const leaveWaitingList = async (item) => {
      if (!confirm('确定要退出此项目的等待列表吗？')) {
        return
      }

      try {
        leaving.value = true
        const response = await bookingConflictService.leaveWaitingList(item.waitingListId)

        if (response.success) {
          showSuccessMessage('已成功退出等待列表')
          loadWaitingList()
        }
      } catch (error) {
        console.error('退出等待列表失败:', error)
        showErrorMessage('退出失败，请稍后重试')
      } finally {
        leaving.value = false
      }
    }

    const rejoinWaitingList = async (item) => {
      try {
        rejoining.value = true
        const response = await bookingConflictService.joinWaitingList({
          roomId: item.roomId,
          checkInDate: item.requestedCheckInDate,
          checkOutDate: item.requestedCheckOutDate,
          guestCount: item.guestCount
        })

        if (response.success) {
          showSuccessMessage('已重新加入等待列表')
          loadWaitingList()
        }
      } catch (error) {
        console.error('重新加入等待列表失败:', error)
        showErrorMessage('重新加入失败，请稍后重试')
      } finally {
        rejoining.value = false
      }
    }

    const viewRoomDetails = (item) => {
      // 跳转到房间详情页
      window.location.href = `/rooms/${item.roomId}`
    }

    const prevPage = () => {
      if (currentPage.value > 1) {
        currentPage.value--
        loadWaitingList()
      }
    }

    const nextPage = () => {
      if (currentPage.value < totalPages.value) {
        currentPage.value++
        loadWaitingList()
      }
    }

    const showSuccessMessage = (message) => {
      // 实现成功提示
      console.log('Success:', message)
    }

    const showErrorMessage = (message) => {
      // 实现错误提示
      console.error('Error:', message)
    }

    // 生命周期
    onMounted(() => {
      loadWaitingList()
    })

    return {
      waitingList,
      loading,
      selectedStatus,
      currentPage,
      totalPages,
      showConfirmModal,
      selectedItem,
      confirming,
      leaving,
      rejoining,
      totalWaiting,
      totalNotified,
      totalConfirmed,
      emptyMessage,
      loadWaitingList,
      refreshWaitingList,
      getItemClass,
      getStatusClass,
      getStatusText,
      isExpired,
      isExpiringSoon,
      confirmBooking,
      closeConfirmModal,
      submitConfirmBooking,
      leaveWaitingList,
      rejoinWaitingList,
      viewRoomDetails,
      prevPage,
      nextPage,
      formatDate,
      formatDateTime
    }
  }
}
</script>

<style scoped>
.waiting-list-manager {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.manager-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.manager-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.manager-actions {
  display: flex;
  gap: 8px;
}

.waiting-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  text-align: center;
  border: 1px solid #e9ecef;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1976d2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #495057;
}

.form-select {
  padding: 6px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
  background-color: white;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1976d2;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.waiting-list-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.waiting-item {
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 20px;
  background-color: white;
  transition: all 0.2s;
}

.waiting-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.waiting-item.item--notified {
  border-left: 4px solid #ffc107;
  background-color: #fff8e6;
}

.waiting-item.item--expired {
  border-left: 4px solid #dc3545;
  background-color: #f8d7da;
}

.waiting-item.item--confirmed {
  border-left: 4px solid #28a745;
  background-color: #d4edda;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.room-info h4 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.room-type {
  font-size: 14px;
  color: #666;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
}

.status--waiting {
  background-color: #e3f2fd;
  color: #1976d2;
}

.status--notified {
  background-color: #fff8e6;
  color: #f57c00;
}

.status--expired {
  background-color: #ffebee;
  color: #d32f2f;
}

.status--confirmed {
  background-color: #e8f5e8;
  color: #388e3c;
}

.item-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.detail-row i {
  color: #666;
}

.detail-label {
  font-weight: 500;
  color: #495057;
}

.detail-value {
  color: #333;
}

.time-info {
  background-color: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.time-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 4px;
}

.time-row:last-child {
  margin-bottom: 0;
}

.time-row.urgent {
  color: #dc3545;
  font-weight: 500;
}

.expired-badge {
  background-color: #dc3545;
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
  margin-left: 8px;
}

.expiring-badge {
  background-color: #ffc107;
  color: #212529;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
  margin-left: 8px;
}

.item-actions {
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
  text-decoration: none;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.btn-primary {
  background-color: #1976d2;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #1565c0;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-outline-secondary {
  background-color: transparent;
  color: #6c757d;
  border: 1px solid #6c757d;
}

.btn-outline-secondary:hover:not(:disabled) {
  background-color: #6c757d;
  color: white;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
}

.btn-outline-danger {
  background-color: transparent;
  color: #dc3545;
  border: 1px solid #dc3545;
}

.btn-outline-danger:hover:not(:disabled) {
  background-color: #dc3545;
  color: white;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}

.page-info {
  font-size: 14px;
  color: #666;
}

.rotating {
  animation: spin 1s linear infinite;
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

.booking-summary h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 14px;
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-item.urgent {
  background-color: #fff3cd;
  padding: 12px;
  border-radius: 6px;
  margin-top: 12px;
  border-bottom: none;
  color: #856404;
}

/* Responsive */
@media (max-width: 768px) {
  .waiting-list-manager {
    padding: 16px;
  }

  .manager-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .waiting-stats {
    grid-template-columns: repeat(3, 1fr);
  }

  .item-details {
    grid-template-columns: 1fr;
  }

  .item-actions {
    flex-direction: column;
  }

  .filter-bar {
    flex-direction: column;
    gap: 12px;
  }
}
</style>