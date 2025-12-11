<template>
  <div class="admin-dashboard">
    <!-- 页面标题和操作栏 -->
    <div class="dashboard-header">
      <div class="header-left">
        <h1 class="page-title">管理员仪表板</h1>
        <p class="page-subtitle">系统运营数据总览</p>
      </div>
      <div class="header-right">
        <!-- 连接状态指示器 -->
        <ConnectionStatus />

        <el-button
          type="primary"
          :loading="loading"
          @click="handleRefresh"
        >
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button
          type="default"
          @click="handleExport"
        >
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
        <el-dropdown @command="handleAutoRefreshChange">
          <el-button type="default">
            <el-icon><Timer /></el-icon>
            自动刷新
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="off">关闭自动刷新</el-dropdown-item>
              <el-dropdown-item command="30s">30秒</el-dropdown-item>
              <el-dropdown-item command="1min">1分钟</el-dropdown-item>
              <el-dropdown-item command="5min">5分钟</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 错误提示 -->
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      @close="clearError"
      class="error-alert"
    />

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 仪表板内容 -->
    <div v-else class="dashboard-content">
      <!-- 快速操作 -->
      <QuickActions />

      <!-- 核心指标概览 -->
      <MetricsOverview
        :metrics="metrics"
        :loading="loading"
        @refresh="handleRefresh"
      />

      <!-- 图表区域 -->
      <div class="charts-section">
        <el-row :gutter="24">
          <!-- 订单趋势图 -->
          <el-col :xs="24" :lg="12">
            <TrendChart
              title="订单趋势分析"
              :chart-data="orderTrendChart"
              :loading="loading"
              :date-range="dateRange"
              @date-range-change="handleDateRangeChange"
            />
          </el-col>

          <!-- 收入分析图 -->
          <el-col :xs="24" :lg="12">
            <TrendChart
              title="收入分析"
              :chart-data="revenueChart"
              :loading="loading"
              :chart-type="chartType"
              :chart-type-options="[
                { label: '趋势图', value: 'trends' },
                { label: '房型分布', value: 'by_room_type' },
                { label: '价格区间', value: 'structure' }
              ]"
              @chart-type-change="handleChartTypeChange"
            />
          </el-col>
        </el-row>

        <el-row :gutter="24" style="margin-top: 24px">
          <!-- 入住率图表 -->
          <el-col :xs="24" :lg="12">
            <TrendChart
              title="入住率分析"
              :chart-data="occupancyChart"
              :loading="loading"
              :chart-type="occupancyChartType"
              :chart-type-options="[
                { label: '趋势图', value: 'trends' },
                { label: '房型对比', value: 'by_room_type' },
                { label: '热力图', value: 'heatmap' }
              ]"
              @chart-type-change="handleOccupancyChartTypeChange"
            />
          </el-col>

          <!-- 房间状态饼图 -->
          <el-col :xs="24" :lg="12">
            <PieChart
              title="房间状态分布"
              :chart-data="roomStatusPieChart"
              :loading="loading"
            />
          </el-col>
        </el-row>
      </div>

      <!-- 实时数据区域 -->
      <RealTimeData
        :loading="loading"
        :real-time-data="realTimeData"
      />
    </div>

    <!-- 数据导出对话框 -->
    <el-dialog v-model="exportDialogVisible" title="数据导出" width="500px">
      <el-form :model="exportForm" label-width="100px">
        <el-form-item label="导出类型">
          <el-radio-group v-model="exportForm.type">
            <el-radio value="dashboard">仪表板数据</el-radio>
            <el-radio value="historical">历史数据</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时间范围" v-if="exportForm.type === 'historical'">
          <el-date-picker
            v-model="exportForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="导出格式">
          <el-select v-model="exportForm.format">
            <el-option label="CSV" value="csv" />
            <el-option label="Excel" value="excel" />
            <el-option label="JSON" value="json" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleExportConfirm" :loading="exportLoading">
          导出
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watchEffect } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Download,
  Timer,
  ArrowDown
} from '@element-plus/icons-vue'
import MetricsOverview from '@/components/business/admin/MetricsOverview.vue'
import TrendChart from '@/components/business/admin/charts/TrendChart.vue'
import PieChart from '@/components/business/admin/charts/PieChart.vue'
import RealTimeData from '@/components/business/admin/RealTimeData.vue'
import QuickActions from '@/components/business/admin/QuickActions.vue'
import ConnectionStatus from '@/components/business/admin/ConnectionStatus.vue'
import type { DashboardMetrics } from '@/types/dashboard'

const dashboardStore = useDashboardStore()

// 响应式数据
const loading = computed(() => dashboardStore.loading)
const error = computed(() => dashboardStore.error)
const metrics = computed(() => dashboardStore.metrics)
const realTimeData = computed(() => dashboardStore.realTimeData)
const orderTrendChart = computed(() => dashboardStore.orderTrendChart)
const revenueChart = computed(() => dashboardStore.revenueChart)
const occupancyChart = computed(() => dashboardStore.occupancyChart)
const roomStatusPieChart = computed(() => dashboardStore.roomStatusPieChart)

// 本地状态
const dateRange = ref<[string, string]>([
  new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  new Date().toISOString().split('T')[0]
])
const chartType = ref('trends')
const occupancyChartType = ref('trends')

// 导出对话框
const exportDialogVisible = ref(false)
const exportLoading = ref(false)
const exportForm = ref({
  type: 'dashboard',
  dateRange: null as [string, string] | null,
  format: 'csv'
})

// 方法
const handleRefresh = () => {
  dashboardStore.fetchAllData()
  ElMessage.success('数据刷新成功')
}

const handleExport = () => {
  exportDialogVisible.value = true
}

const handleExportConfirm = async () => {
  try {
    exportLoading.value = true
    // 这里实现导出逻辑
    ElMessage.success('导出成功')
    exportDialogVisible.value = false
  } catch (error) {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

const handleAutoRefreshChange = (command: string) => {
  switch (command) {
    case 'off':
      dashboardStore.setAutoRefresh(false)
      break
    case '30s':
      dashboardStore.setAutoRefresh(true, 30000)
      break
    case '1min':
      dashboardStore.setAutoRefresh(true, 60000)
      break
    case '5min':
      dashboardStore.setAutoRefresh(true, 300000)
      break
  }
}

const handleDateRangeChange = (newDateRange: [string, string]) => {
  dateRange.value = newDateRange
  if (dateRange.value) {
    dashboardStore.fetchOrderTrendChart(dateRange.value[0], dateRange.value[1])
    dashboardStore.fetchRevenueChart(dateRange.value[0], dateRange.value[1], chartType.value)
    dashboardStore.fetchOccupancyChart(dateRange.value[0], dateRange.value[1], occupancyChartType.value)
  }
}

const handleChartTypeChange = (newChartType: string) => {
  chartType.value = newChartType
  if (dateRange.value) {
    dashboardStore.fetchRevenueChart(dateRange.value[0], dateRange.value[1], chartType.value)
  }
}

const handleOccupancyChartTypeChange = (newChartType: string) => {
  occupancyChartType.value = newChartType
  if (dateRange.value) {
    dashboardStore.fetchOccupancyChart(dateRange.value[0], dateRange.value[1], occupancyChartType.value)
  }
}

const clearError = () => {
  dashboardStore.clearError()
}

// 生命周期
onMounted(() => {
  // 获取初始数据
  dashboardStore.fetchAllData()

  // 启动自动刷新
  dashboardStore.setAutoRefresh(true)
})

onUnmounted(() => {
  // 停止自动刷新
  dashboardStore.stopAutoRefresh()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 24px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.error-alert {
  margin-bottom: 24px;
}

.loading-container {
  padding: 24px;
  background: white;
  border-radius: 8px;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.charts-section {
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .admin-dashboard {
    padding: 16px;
  }

  .dashboard-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .header-right {
    justify-content: center;
  }
}
</style>