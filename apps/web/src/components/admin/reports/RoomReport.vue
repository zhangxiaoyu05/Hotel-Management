<template>
  <div class="room-report">
    <div v-loading="loading" class="report-content">
      <!-- 房间概览指标 -->
      <div class="metrics-section">
        <div class="metrics-grid">
          <div class="metric-card">
            <div class="metric-title">总房间数</div>
            <div class="metric-value">{{ formatNumber(reportData?.totalRooms || 0) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-title">可用房间</div>
            <div class="metric-value">{{ formatNumber(reportData?.availableRooms || 0) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-title">入住率</div>
            <div class="metric-value">{{ formatPercent(reportData?.occupancyRate || 0) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-title">平均房价</div>
            <div class="metric-value">¥{{ formatNumber(reportData?.averageRoomRate || 0) }}</div>
          </div>
        </div>
      </div>

      <!-- 房间绩效数据 -->
      <div class="data-section">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card>
              <template #header>
                <span>房型绩效对比</span>
              </template>
              <el-table :data="reportData?.roomTypePerformances" height="300">
                <el-table-column prop="roomTypeName" label="房型" />
                <el-table-column prop="totalOrders" label="订单数" />
                <el-table-column prop="occupancyRate" label="入住率">
                  <template #default="scope">
                    {{ formatPercent(scope.row.occupancyRate) }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header>
                <span>房间绩效排行</span>
              </template>
              <el-table :data="reportData?.topPerformingRooms" height="300">
                <el-table-column prop="roomNumber" label="房间号" />
                <el-table-column prop="totalOrders" label="订单数" />
                <el-table-column prop="totalRevenue" label="总收入">
                  <template #default="scope">
                    ¥{{ formatNumber(scope.row.totalRevenue) }}
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
import type { RoomReportDTO, ReportFilters } from '@/types/report'
import { useReportStore } from '@/stores/report'

interface Props {
  filters: ReportFilters
}

const props = defineProps<Props>()
const reportStore = useReportStore()

const loading = ref(false)
const reportData = ref<RoomReportDTO | null>(null)

watch(() => props.filters, async () => {
  await loadData()
}, { deep: true })

onMounted(async () => {
  await loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    reportData.value = await reportStore.fetchRoomReport(props.filters)
  } finally {
    loading.value = false
  }
}

const formatNumber = (num: number) => new Intl.NumberFormat('zh-CN').format(num)
const formatPercent = (num: number) => `${num.toFixed(1)}%`
</script>

<style scoped lang="scss">
.room-report {
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