<template>
  <div class="connection-status">
    <el-tooltip
      :content="tooltipContent"
      placement="bottom"
      :effect="dark"
    >
      <div class="status-indicator" :class="statusClass">
        <div class="status-dot"></div>
        <div class="status-text">{{ statusText }}</div>
      </div>
    </el-tooltip>

    <!-- 连接详情面板 -->
    <el-drawer
      v-model="showDetails"
      title="连接状态详情"
      direction="rtl"
      size="400px"
    >
      <div class="connection-details">
        <div class="detail-item">
          <div class="detail-label">连接方式</div>
          <div class="detail-value">
            <el-tag :type="connectionType.tag" size="small">
              {{ connectionType.text }}
            </el-tag>
          </div>
        </div>

        <div class="detail-item">
          <div class="detail-label">自动刷新</div>
          <div class="detail-value">
            <el-switch
              v-model="autoRefreshEnabled"
              @change="handleAutoRefreshChange"
              size="small"
            />
          </div>
        </div>

        <div class="detail-item" v-if="autoRefreshEnabled">
          <div class="detail-label">刷新间隔</div>
          <div class="detail-value">
            <el-select
              v-model="refreshInterval"
              @change="handleIntervalChange"
              size="small"
              style="width: 120px"
            >
              <el-option label="30秒" :value="30000" />
              <el-option label="1分钟" :value="60000" />
              <el-option label="5分钟" :value="300000" />
              <el-option label="10分钟" :value="600000" />
            </el-select>
          </div>
        </div>

        <div class="detail-item">
          <div class="detail-label">最后更新</div>
          <div class="detail-value">
            {{ formatTime(lastUpdateTime) }}
          </div>
        </div>

        <div class="detail-item">
          <div class="detail-label">数据版本</div>
          <div class="detail-value">
            {{ dataVersion }}
          </div>
        </div>

        <div class="detail-section">
          <h4>操作</h4>
          <div class="detail-actions">
            <el-button
              type="primary"
              size="small"
              @click="handleManualRefresh"
              :loading="refreshing"
            >
              <el-icon><Refresh /></el-icon>
              立即刷新
            </el-button>
            <el-button
              v-if="!isWebSocketConnected"
              type="success"
              size="small"
              @click="handleReconnect"
            >
              <el-icon><Connection /></el-icon>
              重新连接
            </el-button>
          </div>
        </div>

        <div class="detail-section">
          <h4>性能统计</h4>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-label">刷新次数</div>
              <div class="stat-value">{{ refreshCount }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">平均延迟</div>
              <div class="stat-value">{{ averageLatency }}ms</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">错误次数</div>
              <div class="stat-value">{{ errorCount }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { ElMessage } from 'element-plus'
import { Refresh, Connection } from '@element-plus/icons-vue'

const dashboardStore = useDashboardStore()

// 响应式数据
const showDetails = ref(false)
const refreshing = ref(false)
const refreshCount = ref(0)
const averageLatency = ref(0)
const errorCount = ref(0)
const dataVersion = ref('1.0.0')

// 计算属性
const isWebSocketConnected = computed(() => dashboardStore.isWebSocketConnected)
const lastUpdateTime = computed(() => dashboardStore.lastUpdateTime)
const autoRefreshEnabled = computed({
  get: () => dashboardStore.autoRefresh,
  set: (value: boolean) => {
    dashboardStore.setAutoRefresh(value)
  }
})
const refreshInterval = computed({
  get: () => dashboardStore.refreshInterval,
  set: (value: number) => {
    dashboardStore.setAutoRefresh(true, value)
  }
})

const statusClass = computed(() => {
  return {
    'status-connected': isWebSocketConnected.value,
    'status-disconnected': !isWebSocketConnected.value,
    'status-loading': refreshing.value
  }
})

const statusText = computed(() => {
  if (refreshing.value) return '刷新中'
  return isWebSocketConnected.value ? '实时' : '定时'
})

const connectionType = computed(() => {
  return isWebSocketConnected.value
    ? { text: 'WebSocket', tag: 'success' }
    : { text: 'HTTP轮询', tag: 'info' }
})

const tooltipContent = computed(() => {
  const base = `连接方式: ${connectionType.value.text}\n最后更新: ${formatTime(lastUpdateTime.value)}`
  if (autoRefreshEnabled.value) {
    return `${base}\n刷新间隔: ${formatInterval(refreshInterval.value)}`
  }
  return base
})

// 方法
const formatTime = (time: string) => {
  if (!time) return '从未'
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else {
    return date.toLocaleString()
  }
}

const formatInterval = (interval: number) => {
  if (interval < 60000) {
    return `${interval / 1000}秒`
  } else {
    return `${interval / 60000}分钟`
  }
}

const handleAutoRefreshChange = (enabled: boolean) => {
  dashboardStore.setAutoRefresh(enabled)
  ElMessage.success(enabled ? '自动刷新已开启' : '自动刷新已关闭')
}

const handleIntervalChange = (interval: number) => {
  dashboardStore.setAutoRefresh(true, interval)
  ElMessage.success(`刷新间隔已设置为 ${formatInterval(interval)}`)
}

const handleManualRefresh = async () => {
  try {
    refreshing.value = true
    const startTime = Date.now()

    await dashboardStore.manualRefresh()

    const latency = Date.now() - startTime
    updateLatency(latency)
    refreshCount.value++

    ElMessage.success('数据刷新成功')
  } catch (error) {
    errorCount.value++
    ElMessage.error('数据刷新失败')
  } finally {
    refreshing.value = false
  }
}

const handleReconnect = () => {
  dashboardStore.connectWebSocket()
  ElMessage.info('正在尝试重新连接...')
}

const updateLatency = (latency: number) => {
  // 计算移动平均延迟
  const alpha = 0.3 // 平滑因子
  averageLatency.value = Math.round(averageLatency.value * (1 - alpha) + latency * alpha)
}

// 监听连接状态变化
const updateConnectionStatus = () => {
  if (!isWebSocketConnected.value) {
    // WebSocket断开连接时显示警告
    ElMessage.warning('实时连接已断开，已切换到定时刷新模式')
  }
}

// 生命周期
onMounted(() => {
  // 监听WebSocket连接状态变化
  const checkInterval = setInterval(() => {
    if (dashboardStore.isWebSocketConnected !== isWebSocketConnected.value) {
      updateConnectionStatus()
    }
  }, 1000)

  onUnmounted(() => {
    clearInterval(checkInterval)
  })
})
</script>

<style scoped>
.connection-status {
  display: inline-block;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 12px;
}

.status-indicator:hover {
  background: rgba(0, 0, 0, 0.05);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.status-connected .status-dot {
  background: #52c41a;
  box-shadow: 0 0 6px rgba(82, 196, 26, 0.5);
}

.status-connected .status-text {
  color: #52c41a;
}

.status-disconnected .status-dot {
  background: #ff4d4f;
  box-shadow: 0 0 6px rgba(255, 77, 79, 0.5);
}

.status-disconnected .status-text {
  color: #ff4d4f;
}

.status-loading .status-dot {
  background: #faad14;
  animation: pulse 1.5s infinite;
}

.status-loading .status-text {
  color: #faad14;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.connection-details {
  padding: 20px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 14px;
  color: #666;
}

.detail-value {
  font-size: 14px;
  color: #333;
}

.detail-section {
  margin-top: 24px;
}

.detail-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 12px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

@media (max-width: 768px) {
  .status-indicator {
    padding: 6px 10px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>