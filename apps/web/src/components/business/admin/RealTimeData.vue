<template>
  <div class="realtime-data">
    <!-- 最新订单 -->
    <el-card class="recent-orders-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><Tickets /></el-icon>
            最新订单
          </span>
          <span class="update-time">
            <el-icon><Clock /></el-icon>
            {{ formatTime(realTimeData?.lastUpdateTime) }}
          </span>
        </div>
      </template>
      <div class="orders-list" v-loading="loading">
        <div
          v-for="order in recentOrders"
          :key="order.id"
          class="order-item"
        >
          <div class="order-main">
            <div class="order-info">
              <div class="order-number">{{ order.orderNumber }}</div>
              <div class="order-detail">
                {{ order.roomNumber }} · {{ order.contactName }}
              </div>
              <div class="order-time">
                {{ formatDateTime(order.createdAt) }}
              </div>
            </div>
            <div class="order-right">
              <div class="order-price">¥{{ order.totalPrice }}</div>
              <el-tag :type="getOrderStatusType(order.status)" size="small">
                {{ getOrderStatusText(order.status) }}
              </el-tag>
            </div>
          </div>
        </div>

        <div v-if="!recentOrders || recentOrders.length === 0" class="empty-state">
          <el-empty description="暂无订单数据" />
        </div>
      </div>
    </el-card>

    <!-- 系统状态 -->
    <el-card class="system-status-card" shadow="hover">
      <template #header>
        <span class="card-title">
          <el-icon><Monitor /></el-icon>
          系统状态
        </span>
      </template>
      <div class="status-grid" v-loading="loading">
        <div class="status-item" v-for="(status, key) in systemStatus" :key="key">
          <div class="status-icon">
            <el-icon :size="16" :color="getStatusColor(status)">
              <component :is="getStatusIcon(key)" />
            </el-icon>
          </div>
          <div class="status-info">
            <div class="status-name">{{ getStatusName(key) }}</div>
            <div class="status-value">
              <el-tag :type="getStatusType(status)" size="small">
                {{ status }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 快速统计 -->
    <el-card class="quick-stats-card" shadow="hover">
      <template #header>
        <span class="card-title">
          <el-icon><DataLine /></el-icon>
          快速统计
        </span>
      </template>
      <div class="stats-grid" v-loading="loading">
        <div class="stat-item">
          <div class="stat-icon pending">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ realTimeData?.pendingOrdersCount || 0 }}</div>
            <div class="stat-label">待处理订单</div>
          </div>
        </div>

        <div class="stat-item">
          <div class="stat-icon checkin">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ realTimeData?.pendingCheckInCount || 0 }}</div>
            <div class="stat-label">待入住</div>
          </div>
        </div>

        <div class="stat-item">
          <div class="stat-icon checkout">
            <el-icon><Switch /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ realTimeData?.pendingCheckOutCount || 0 }}</div>
            <div class="stat-label">待退房</div>
          </div>
        </div>

        <div class="stat-item">
          <div class="stat-icon online">
            <el-icon><Connection /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ realTimeData?.onlineUsersCount || 0 }}</div>
            <div class="stat-label">在线用户</div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Tickets,
  Clock,
  Monitor,
  DataLine,
  User,
  Switch,
  Connection,
  Setting,
  HardDrive,
  MemoryStick
} from '@element-plus/icons-vue'
import type { RealTimeData, OrderSummary } from '@/types/dashboard'
import { useDashboardStore } from '@/stores/dashboard'

interface Props {
  loading?: boolean
  realTimeData?: RealTimeData
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const recentOrders = computed(() => {
  return props.realTimeData?.recentOrders?.slice(0, 8) || []
})

const systemStatus = computed(() => {
  return {
    database: props.realTimeData?.databaseStatus || 'UNKNOWN',
    cache: props.realTimeData?.cacheStatus || 'UNKNOWN',
    memory: props.realTimeData?.systemHealth?.memory || 'UNKNOWN',
    disk: props.realTimeData?.systemHealth?.diskSpace || 'UNKNOWN'
  }
})

const formatTime = (time: string | undefined) => {
  if (!time) return '--'
  return new Date(time).toLocaleTimeString()
}

const formatDateTime = (time: string | undefined) => {
  if (!time) return '--'
  return new Date(time).toLocaleString()
}

const getOrderStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    PENDING: 'warning',
    CONFIRMED: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }
  return statusMap[status] || 'info'
}

const getOrderStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    PENDING: '待处理',
    CONFIRMED: '已确认',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return statusMap[status] || status
}

const getStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    UP: '#52c41a',
    OK: '#52c41a',
    DOWN: '#ff4d4f',
    ERROR: '#ff4d4f',
    WARNING: '#faad14',
    UNKNOWN: '#d9d9d9'
  }
  return colorMap[status] || '#d9d9d9'
}

const getStatusIcon = (key: string) => {
  const iconMap: Record<string, any> = {
    database: HardDrive,
    cache: MemoryStick,
    memory: MemoryStick,
    disk: HardDrive
  }
  return iconMap[key] || Setting
}

const getStatusName = (key: string) => {
  const nameMap: Record<string, string> = {
    database: '数据库',
    cache: '缓存',
    memory: '内存',
    disk: '磁盘空间'
  }
  return nameMap[key] || key
}

const getStatusType = (status: string) => {
  if (!status) return 'info'
  return status === 'UP' || status === 'OK' ? 'success' : 'danger'
}
</script>

<style scoped>
.realtime-data {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.recent-orders-card,
.system-status-card,
.quick-stats-card {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.update-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
}

.orders-list {
  max-height: 400px;
  overflow-y: auto;
}

.order-item {
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.order-item:last-child {
  border-bottom: none;
}

.order-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-info {
  flex: 1;
  margin-right: 16px;
}

.order-number {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.order-detail {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 4px;
}

.order-time {
  font-size: 12px;
  color: #9ca3af;
}

.order-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.order-price {
  font-weight: 600;
  color: #10b981;
  font-size: 14px;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.status-icon {
  flex-shrink: 0;
}

.status-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-name {
  font-size: 14px;
  color: #1f2937;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.stat-item:hover {
  border-color: #1890ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.pending {
  background: #fff7e6;
  color: #faad14;
}

.stat-icon.checkin {
  background: #e6f7ff;
  color: #1890ff;
}

.stat-icon.checkout {
  background: #f6ffed;
  color: #52c41a;
}

.stat-icon.online {
  background: #f9f0ff;
  color: #722ed1;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

@media (max-width: 768px) {
  .realtime-data {
    gap: 16px;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .order-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-info {
    margin-right: 0;
    margin-bottom: 8px;
  }

  .order-right {
    flex-direction: row;
    align-items: center;
    gap: 8px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>