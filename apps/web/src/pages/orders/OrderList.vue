<template>
  <div class="order-list">
    <div class="header">
      <h1>我的订单</h1>
      <div class="actions">
        <div class="search-bar">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索订单号或酒店名"
            @keyup.enter="loadOrders"
          />
          <button @click="loadOrders" :disabled="loading">
            <i class="fas fa-search"></i>
          </button>
        </div>
      </div>
    </div>

    <div class="filters">
      <div class="status-filter">
        <label>订单状态：</label>
        <select v-model="selectedStatus" @change="loadOrders">
          <option value="">全部</option>
          <option value="PENDING">待确认</option>
          <option value="CONFIRMED">已确认</option>
          <option value="COMPLETED">已完成</option>
          <option value="CANCELLED">已取消</option>
        </select>
      </div>

      <div class="sort-options">
        <label>排序：</label>
        <select v-model="sortBy" @change="loadOrders">
          <option value="createdAt">预订时间</option>
          <option value="checkInDate">入住时间</option>
          <option value="totalPrice">价格</option>
        </select>
        <button
          @click="sortOrder = sortOrder === 'desc' ? 'asc' : 'desc'"
          @change="loadOrders"
          class="sort-direction"
        >
          <i :class="sortOrder === 'desc' ? 'fas fa-sort-down' : 'fas fa-sort-up'"></i>
        </button>
      </div>
    </div>

    <div class="orders-container">
      <div v-if="loading" class="loading">
        <i class="fas fa-spinner fa-spin"></i>
        加载中...
      </div>

      <div v-else-if="orders.length === 0" class="empty-state">
        <i class="fas fa-file-invoice"></i>
        <p>暂无订单</p>
      </div>

      <div v-else class="orders-grid">
        <div
          v-for="order in orders"
          :key="order.id"
          class="order-card"
          @click="goToOrderDetail(order.id)"
        >
          <div class="order-header">
            <div class="order-number">{{ order.orderNumber }}</div>
            <div :class="['order-status', statusClass(order.status)]">
              {{ statusText(order.status) }}
            </div>
          </div>

          <div class="hotel-info">
            <h3>{{ order.hotelName }}</h3>
            <p>{{ order.roomName }} ({{ order.roomNumber }})</p>
          </div>

          <div class="order-details">
            <div class="detail-item">
              <i class="fas fa-calendar-check"></i>
              <span>入住：{{ formatDate(order.checkInDate) }}</span>
            </div>
            <div class="detail-item">
              <i class="fas fa-calendar-times"></i>
              <span>退房：{{ formatDate(order.checkOutDate) }}</span>
            </div>
            <div class="detail-item">
              <i class="fas fa-users"></i>
              <span>入住人数：{{ order.guestCount }}人</span>
            </div>
          </div>

          <div class="order-footer">
            <div class="price">
              <span class="amount">¥{{ order.totalPrice.toFixed(2) }}</span>
            </div>
            <div class="booking-time">
              {{ formatDateTime(order.createdAt) }}
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="order-actions" @click.stop>
            <button
              v-if="order.status === 'CONFIRMED'"
              @click="showCancelModal(order)"
              class="btn-cancel"
            >
              取消订单
            </button>
            <button
              v-if="order.status === 'PENDING' || order.status === 'CONFIRMED'"
              @click="showEditModal(order)"
              class="btn-edit"
            >
              修改订单
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <button
        @click="currentPage = 1; loadOrders()"
        :disabled="currentPage === 1"
      >
        首页
      </button>
      <button
        @click="currentPage--; loadOrders()"
        :disabled="currentPage === 1"
      >
        上一页
      </button>
      <span class="page-info">
        第 {{ currentPage }} 页，共 {{ totalPages }} 页
      </span>
      <button
        @click="currentPage++; loadOrders()"
        :disabled="currentPage === totalPages"
      >
        下一页
      </button>
      <button
        @click="currentPage = totalPages; loadOrders()"
        :disabled="currentPage === totalPages"
      >
        末页
      </button>
    </div>

    <!-- 修改订单弹窗 -->
    <EditOrderModal
      v-if="showEdit"
      :order="selectedOrder"
      @close="showEdit = false"
      @success="handleEditSuccess"
    />

    <!-- 取消订单弹窗 -->
    <CancelOrderModal
      v-if="showCancel"
      :order="selectedOrder"
      @close="showCancel = false"
      @success="handleCancelSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderService } from '@/services/orderService'
import type { OrderListResponse } from '@/types/order'
import EditOrderModal from '@/components/business/EditOrderModal.vue'
import CancelOrderModal from '@/components/business/CancelOrderModal.vue'

const router = useRouter()

const orders = ref<OrderListResponse[]>([])
const loading = ref(false)
const searchQuery = ref('')
const selectedStatus = ref('')
const sortBy = ref('createdAt')
const sortOrder = ref<'asc' | 'desc'>('desc')
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(0)

const showEdit = ref(false)
const showCancel = ref(false)
const selectedOrder = ref<OrderListResponse | null>(null)

const loadOrders = async () => {
  loading.value = true
  try {
    orders.value = await orderService.getOrderList(
      selectedStatus.value,
      currentPage.value,
      pageSize.value,
      sortBy.value,
      sortOrder.value,
      searchQuery.value
    )
  } catch (error) {
    console.error('加载订单失败:', error)
  } finally {
    loading.value = false
  }
}

const statusClass = (status: string) => {
  const classes = {
    'PENDING': 'status-pending',
    'CONFIRMED': 'status-confirmed',
    'COMPLETED': 'status-completed',
    'CANCELLED': 'status-cancelled'
  }
  return classes[status as keyof typeof classes] || ''
}

const statusText = (status: string) => {
  const texts = {
    'PENDING': '待确认',
    'CONFIRMED': '已确认',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[status as keyof typeof texts] || status
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('zh-CN')
}

const formatDateTime = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const goToOrderDetail = (orderId: number) => {
  router.push(`/orders/${orderId}`)
}

const showEditModal = (order: OrderListResponse) => {
  selectedOrder.value = order
  showEdit.value = true
}

const showCancelModal = (order: OrderListResponse) => {
  selectedOrder.value = order
  showCancel.value = true
}

const handleEditSuccess = () => {
  showEdit.value = false
  loadOrders()
}

const handleCancelSuccess = () => {
  showCancel.value = false
  loadOrders()
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.order-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h1 {
  color: #2c3e50;
  margin: 0;
}

.search-bar {
  display: flex;
  gap: 10px;
}

.search-bar input {
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  width: 300px;
}

.search-bar button {
  padding: 10px 15px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.search-bar button:hover {
  background-color: #2980b9;
}

.filters {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
  align-items: center;
}

.filters label {
  font-weight: 500;
  color: #555;
}

.filters select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background-color: white;
}

.sort-options {
  display: flex;
  gap: 10px;
  align-items: center;
}

.sort-direction {
  padding: 8px 10px;
  background-color: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
}

.loading {
  text-align: center;
  padding: 50px;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 50px;
  color: #666;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 20px;
  color: #ccc;
}

.orders-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.order-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #eee;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.order-number {
  font-weight: 600;
  color: #333;
}

.order-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending {
  background-color: #fff3cd;
  color: #856404;
}

.status-confirmed {
  background-color: #d4edda;
  color: #155724;
}

.status-completed {
  background-color: #cce5ff;
  color: #004085;
}

.status-cancelled {
  background-color: #f8d7da;
  color: #721c24;
}

.hotel-info h3 {
  margin: 0 0 5px 0;
  color: #2c3e50;
  font-size: 18px;
}

.hotel-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.order-details {
  margin: 15px 0;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  color: #666;
  font-size: 14px;
}

.detail-item i {
  width: 16px;
  color: #3498db;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.price .amount {
  font-size: 18px;
  font-weight: 600;
  color: #e74c3c;
}

.booking-time {
  color: #888;
  font-size: 12px;
}

.order-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.btn-edit, .btn-cancel {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-edit {
  background-color: #3498db;
  color: white;
}

.btn-edit:hover {
  background-color: #2980b9;
}

.btn-cancel {
  background-color: #e74c3c;
  color: white;
}

.btn-cancel:hover {
  background-color: #c0392b;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 30px;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background-color: white;
  border-radius: 6px;
  cursor: pointer;
}

.pagination button:hover:not(:disabled) {
  background-color: #f8f9fa;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #666;
  font-size: 14px;
}

@media (max-width: 768px) {
  .header {
    flex-direction: column;
    gap: 20px;
    align-items: stretch;
  }

  .search-bar {
    width: 100%;
  }

  .search-bar input {
    flex: 1;
    width: auto;
  }

  .filters {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .sort-options {
    flex-direction: column;
    align-items: stretch;
  }

  .orders-grid {
    grid-template-columns: 1fr;
  }
}
</style>