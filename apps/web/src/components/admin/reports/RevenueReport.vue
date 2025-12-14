<template>
  <div class="revenue-report">
    <div v-loading="loading" class="report-content">
      <!-- 收入概览指标 -->
      <div class="metrics-section">
        <div class="metrics-grid">
          <div class="metric-card">
            <div class="metric-title">总收入</div>
            <div class="metric-value">¥{{ formatNumber(reportData?.totalRevenue || 0) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-title">平均每日房价</div>
            <div class="metric-value">¥{{ formatNumber(reportData?.averageDailyRate || 0) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-title">RevPAR</div>
            <div class="metric-value">¥{{ formatNumber(reportData?.revenuePerAvailableRoom || 0) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-title">入住率</div>
            <div class="metric-value">{{ formatPercent(reportData?.occupancyRate || 0) }}</div>
          </div>
        </div>
      </div>

      <!-- 收入趋势图表 -->
      <div class="chart-section">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>收入趋势分析</span>
            </div>
          </template>
          <div ref="revenueChart" class="chart-container"></div>
        </el-card>
      </div>

      <!-- 收入详细数据 -->
      <div class="data-section">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card>
              <template #header>
                <span>房型收入贡献</span>
              </template>
              <el-table :data="reportData?.roomTypeRevenueContributions" height="300">
                <el-table-column prop="roomTypeName" label="房型" />
                <el-table-column prop="revenue" label="收入">
                  <template #default="scope">
                    ¥{{ formatNumber(scope.row.revenue) }}
                  </template>
                </el-table-column>
                <el-table-column prop="percentage" label="占比">
                  <template #default="scope">
                    {{ formatPercent(scope.row.percentage) }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header>
                <span>收入预测</span>
              </template>
              <el-table :data="reportData?.revenueForecasts" height="300">
                <el-table-column prop="period" label="预测期间" />
                <el-table-column prop="predictedRevenue" label="预测收入">
                  <template #default="scope">
                    ¥{{ formatNumber(scope.row.predictedRevenue) }}
                  </template>
                </el-table-column>
                <el-table-column prop="growthRate" label="增长率">
                  <template #default="scope">
                    {{ formatPercent(scope.row.growthRate) }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { RevenueReportDTO, ReportFilters } from '@/types/report'
import { useReportStore } from '@/stores/report'

interface Props {
  filters: ReportFilters
}

const props = defineProps<Props>()
const reportStore = useReportStore()

const loading = ref(false)
const reportData = ref<RevenueReportDTO | null>(null)
const revenueChart = ref<HTMLElement>()

let revenueChartInstance: echarts.ECharts | null = null

watch(() => props.filters, async () => {
  await loadData()
}, { deep: true })

onMounted(async () => {
  await loadData()
  await nextTick()
  initRevenueChart()
})

const loadData = async () => {
  loading.value = true
  try {
    reportData.value = await reportStore.fetchRevenueReport(props.filters)
  } finally {
    loading.value = false
  }
}

const initRevenueChart = () => {
  if (!revenueChart.value || !reportData.value) return

  if (revenueChartInstance) revenueChartInstance.dispose()
  revenueChartInstance = echarts.init(revenueChart.value)

  const option = {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: reportData.value.dailyRevenueTrends.map(item => item.date)
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: '¥{value}' }
    },
    series: [{
      type: 'line',
      data: reportData.value.dailyRevenueTrends.map(item => item.revenue),
      smooth: true
    }]
  }

  revenueChartInstance.setOption(option)
}

const formatNumber = (num: number) => new Intl.NumberFormat('zh-CN').format(num)
const formatPercent = (num: number) => `${num.toFixed(1)}%`
</script>

<style scoped lang="scss">
.revenue-report {
  padding: 20px;

  .metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;
    margin-bottom: 24px;
  }

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

  .chart-container {
    height: 400px;
    width: 100%;
  }
}
</style>