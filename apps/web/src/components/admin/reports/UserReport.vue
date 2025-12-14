<template>
  <div class="user-report">
    <div v-loading="loading" class="report-content">
      <!-- 用户概览指标 -->
      <div class="metrics-section">
        <div class="metrics-grid">
          <div class="metric-card">
            <div class="metric-title">总用户数</div>
            <div class="metric-value">{{ formatNumber(reportData?.totalUsers || 0) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-title">活跃用户</div>
            <div class="metric-value">{{ formatNumber(reportData?.activeUsers || 0) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-title">用户留存率</div>
            <div class="metric-value">{{ formatPercent(reportData?.userRetentionRate || 0) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-title">转化率</div>
            <div class="metric-value">{{ formatPercent(reportData?.userConversionRate || 0) }}</div>
          </div>
        </div>
      </div>

      <!-- 用户数据表格 -->
      <div class="data-section">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card>
              <template #header>
                <span>消费金额排行</span>
              </template>
              <el-table :data="reportData?.topUsersBySpending" height="300">
                <el-table-column prop="username" label="用户名" />
                <el-table-column prop="totalSpending" label="总消费">
                  <template #default="scope">
                    ¥{{ formatNumber(scope.row.totalSpending) }}
                  </template>
                </el-table-column>
                <el-table-column prop="orderCount" label="订单数" />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header>
                <span>下单次数排行</span>
              </template>
              <el-table :data="reportData?.topUsersByOrders" height="300">
                <el-table-column prop="username" label="用户名" />
                <el-table-column prop="orderCount" label="订单数" />
                <el-table-column prop="lastOrderDate" label="最后下单">
                  <template #default="scope">
                    {{ formatDate(scope.row.lastOrderDate) }}
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
import { ref, onMounted, watch } from 'vue'
import type { UserReportDTO, ReportFilters } from '@/types/report'
import { useReportStore } from '@/stores/report'

interface Props {
  filters: ReportFilters
}

const props = defineProps<Props>()
const reportStore = useReportStore()

const loading = ref(false)
const reportData = ref<UserReportDTO | null>(null)

watch(() => props.filters, async () => {
  await loadData()
}, { deep: true })

onMounted(async () => {
  await loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    reportData.value = await reportStore.fetchUserReport(props.filters)
  } finally {
    loading.value = false
  }
}

const formatNumber = (num: number) => new Intl.NumberFormat('zh-CN').format(num)
const formatPercent = (num: number) => `${num.toFixed(1)}%`
const formatDate = (date: string) => date ? new Date(date).toLocaleDateString('zh-CN') : '-'
</script>

<style scoped lang="scss">
.user-report {
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
}
</style>