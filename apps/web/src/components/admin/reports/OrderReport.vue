<template>
  <div class="order-report">
    <div v-loading="loading" class="report-content">
      <!-- 概览指标 -->
      <div class="metrics-section">
        <div class="metrics-grid">
          <div class="metric-card">
            <div class="metric-title">总订单数</div>
            <div class="metric-value">{{ formatNumber(reportData?.totalOrders || 0) }}</div>
          </div>

          <div class="metric-card">
            <div class="metric-title">总收入</div>
            <div class="metric-value">¥{{ formatNumber(reportData?.totalRevenue || 0) }}</div>
          </div>

          <div class="metric-card">
            <div class="metric-title">平均订单价值</div>
            <div class="metric-value">¥{{ formatNumber(reportData?.averageOrderValue || 0) }}</div>
          </div>

          <div class="metric-card">
            <div class="metric-title">完成率</div>
            <div class="metric-value">{{ formatPercent(reportData?.completionRate || 0) }}</div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="charts-section">
        <el-row :gutter="20">
          <!-- 订单趋势图 -->
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>订单趋势</span>
                </div>
              </template>
              <div ref="orderTrendChart" class="chart-container"></div>
            </el-card>
          </el-col>

          <!-- 订单状态分布 -->
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>订单状态分布</span>
                </div>
              </template>
              <div ref="statusChart" class="chart-container"></div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <!-- 房型偏好 -->
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>房型偏好</span>
                </div>
              </template>
              <div ref="roomTypeChart" class="chart-container"></div>
            </el-card>
          </el-col>

          <!-- 渠道分布 -->
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>渠道分布</span>
                </div>
              </template>
              <div ref="channelChart" class="chart-container"></div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 详细数据表格 -->
      <div class="tables-section">
        <el-row :gutter="20">
          <!-- 按日期统计 -->
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>按日期统计</span>
                </div>
              </template>
              <el-table
                :data="orderDateData"
                height="300"
                stripe
              >
                <el-table-column prop="date" label="日期" width="120" />
                <el-table-column prop="count" label="订单数" width="80" />
                <el-table-column prop="revenue" label="预估收入" width="100">
                  <template #default="scope">
                    ¥{{ formatNumber(scope.row.revenue) }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <!-- 房型偏好排行 -->
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>房型偏好排行</span>
                </div>
              </template>
              <el-table
                :data="reportData?.roomTypePreferences || []"
                height="300"
                stripe
              >
                <el-table-column prop="roomTypeName" label="房型" width="100" />
                <el-table-column prop="orderCount" label="订单数" width="80" />
                <el-table-column prop="revenue" label="收入" width="100">
                  <template #default="scope">
                    ¥{{ formatNumber(scope.row.revenue) }}
                  </template>
                </el-table-column>
                <el-table-column prop="percentage" label="占比" width="80">
                  <template #default="scope">
                    {{ formatPercent(scope.row.percentage) }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <!-- 月度收入统计 -->
        <el-card style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>月度收入统计</span>
            </div>
          </template>
          <div ref="monthlyRevenueChart" class="chart-container-large"></div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { OrderReportDTO, ReportFilters } from '@/types/report'
import { useReportStore } from '@/stores/report'

interface Props {
  filters: ReportFilters
}

const props = defineProps<Props>()
const reportStore = useReportStore()

// 响应式数据
const loading = ref(false)
const reportData = ref<OrderReportDTO | null>(null)

// 图表实例
const orderTrendChart = ref<HTMLElement>()
const statusChart = ref<HTMLElement>()
const roomTypeChart = ref<HTMLElement>()
const channelChart = ref<HTMLElement>()
const monthlyRevenueChart = ref<HTMLElement>()

let orderTrendChartInstance: echarts.ECharts | null = null
let statusChartInstance: echarts.ECharts | null = null
let roomTypeChartInstance: echarts.ECharts | null = null
let channelChartInstance: echarts.ECharts | null = null
let monthlyRevenueChartInstance: echarts.ECharts | null = null

// 计算属性
const orderDateData = computed(() => {
  if (!reportData.value?.ordersByDate) return []

  return Object.entries(reportData.value.ordersByDate)
    .slice(-7) // 显示最近7天
    .map(([date, count]) => ({
      date,
      count,
      revenue: count * (reportData.value?.averageOrderValue || 0)
    }))
    .reverse()
})

// 监听筛选条件变化
watch(
  () => props.filters,
  async () => {
    await loadData()
  },
  { deep: true }
)

// 生命周期
onMounted(async () => {
  await loadData()
  await nextTick()
  initCharts()
})

// 方法
const loadData = async () => {
  loading.value = true
  try {
    reportData.value = await reportStore.fetchOrderReport(props.filters)
  } catch (error) {
    console.error('加载订单报表失败:', error)
  } finally {
    loading.value = false
  }
}

const initCharts = () => {
  if (!reportData.value) return

  initOrderTrendChart()
  initStatusChart()
  initRoomTypeChart()
  initChannelChart()
  initMonthlyRevenueChart()
}

const initOrderTrendChart = () => {
  if (!orderTrendChart.value || !reportData.value) return

  if (orderTrendChartInstance) {
    orderTrendChartInstance.dispose()
  }

  orderTrendChartInstance = echarts.init(orderTrendChart.value)

  const dates = reportData.value.orderTrends.map(item => item.date)
  const orderCounts = reportData.value.orderTrends.map(item => item.orderCount)
  const revenues = reportData.value.orderTrends.map(item => item.revenue)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['订单数', '收入']
    },
    xAxis: {
      type: 'category',
      data: dates
    },
    yAxis: [
      {
        type: 'value',
        name: '订单数',
        position: 'left'
      },
      {
        type: 'value',
        name: '收入',
        position: 'right'
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'line',
        data: orderCounts,
        smooth: true,
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '收入',
        type: 'bar',
        yAxisIndex: 1,
        data: revenues,
        itemStyle: { color: '#67C23A' }
      }
    ]
  }

  orderTrendChartInstance.setOption(option)
}

const initStatusChart = () => {
  if (!statusChart.value || !reportData.value) return

  if (statusChartInstance) {
    statusChartInstance.dispose()
  }

  statusChartInstance = echarts.init(statusChart.value)

  const statusData = Object.entries(reportData.value.ordersByStatus).map(([key, value]) => ({
    name: getStatusName(key),
    value
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        type: 'pie',
        radius: '60%',
        data: statusData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  statusChartInstance.setOption(option)
}

const initRoomTypeChart = () => {
  if (!roomTypeChart.value || !reportData.value) return

  if (roomTypeChartInstance) {
    roomTypeChartInstance.dispose()
  }

  roomTypeChartInstance = echarts.init(roomTypeChart.value)

  const roomTypeData = reportData.value.roomTypePreferences.map(item => ({
    name: item.roomTypeName,
    value: item.orderCount
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 单 ({d}%)'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        data: roomTypeData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  roomTypeChartInstance.setOption(option)
}

const initChannelChart = () => {
  if (!channelChart.value || !reportData.value) return

  if (channelChartInstance) {
    channelChartInstance.dispose()
  }

  channelChartInstance = echarts.init(channelChart.value)

  const channelData = Object.entries(reportData.value.ordersByChannel).map(([key, value]) => ({
    name: key,
    value
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 单 ({d}%)'
    },
    series: [
      {
        type: 'pie',
        radius: '50%',
        data: channelData,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {d}%'
        }
      }
    ]
  }

  channelChartInstance.setOption(option)
}

const initMonthlyRevenueChart = () => {
  if (!monthlyRevenueChart.value || !reportData.value) return

  if (monthlyRevenueChartInstance) {
    monthlyRevenueChartInstance.dispose()
  }

  monthlyRevenueChartInstance = echarts.init(monthlyRevenueChart.value)

  const months = Object.keys(reportData.value.revenueByMonth)
  const revenues = Object.values(reportData.value.revenueByMonth)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>收入: ¥{c}'
    },
    xAxis: {
      type: 'category',
      data: months
    },
    yAxis: {
      type: 'value',
      name: '收入',
      axisLabel: {
        formatter: '¥{value}'
      }
    },
    series: [
      {
        type: 'bar',
        data: revenues,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ])
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#2378f7' },
              { offset: 0.7, color: '#2378f7' },
              { offset: 1, color: '#83bff6' }
            ])
          }
        }
      }
    ]
  }

  monthlyRevenueChartInstance.setOption(option)
}

// 工具函数
const getStatusName = (status: string): string => {
  const statusMap: Record<string, string> = {
    PENDING: '待确认',
    CONFIRMED: '已确认',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return statusMap[status] || status
}

const formatNumber = (num: number): string => {
  return new Intl.NumberFormat('zh-CN').format(num)
}

const formatPercent = (num: number): string => {
  return `${num.toFixed(1)}%`
}

// 监听窗口大小变化，重新调整图表大小
const handleResize = () => {
  orderTrendChartInstance?.resize()
  statusChartInstance?.resize()
  roomTypeChartInstance?.resize()
  channelChartInstance?.resize()
  monthlyRevenueChartInstance?.resize()
}

window.addEventListener('resize', handleResize)

// 组件卸载时清理资源
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)

  // 销毁所有图表实例
  if (orderTrendChartInstance) {
    orderTrendChartInstance.dispose()
    orderTrendChartInstance = null
  }
  if (statusChartInstance) {
    statusChartInstance.dispose()
    statusChartInstance = null
  }
  if (roomTypeChartInstance) {
    roomTypeChartInstance.dispose()
    roomTypeChartInstance = null
  }
  if (channelChartInstance) {
    channelChartInstance.dispose()
    channelChartInstance = null
  }
  if (monthlyRevenueChartInstance) {
    monthlyRevenueChartInstance.dispose()
    monthlyRevenueChartInstance = null
  }
})
</script>

<style scoped lang="scss">
.order-report {
  padding: 20px;

  .report-content {
    min-height: 400px;
  }

  .metrics-section {
    margin-bottom: 24px;

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;

      .metric-card {
        padding: 20px;
        background: white;
        border-radius: 8px;
        border: 1px solid #e5e7eb;
        text-align: center;

        .metric-title {
          font-size: 14px;
          color: #6b7280;
          margin-bottom: 8px;
        }

        .metric-value {
          font-size: 24px;
          font-weight: 600;
          color: #1f2937;
        }
      }
    }
  }

  .charts-section {
    margin-bottom: 24px;

    .card-header {
      font-weight: 600;
    }

    .chart-container {
      height: 300px;
      width: 100%;
    }

    .chart-container-large {
      height: 400px;
      width: 100%;
    }
  }

  .tables-section {
    .card-header {
      font-weight: 600;
    }
  }
}

:deep(.el-card) {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);

  .el-card__header {
    background-color: #f9fafb;
    border-bottom: 1px solid #e5e7eb;
  }
}
</style>