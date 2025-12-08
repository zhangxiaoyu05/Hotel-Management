<template>
  <div class="review-analytics">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="flex justify-between items-center">
        <h1 class="text-2xl font-bold text-gray-900">评价数据分析</h1>
        <div class="flex space-x-4">
          <el-button @click="refreshData" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
          <el-button @click="exportData">
            <el-icon><Download /></el-icon>
            导出报告
          </el-button>
          <el-button type="primary" @click="goToManagement">
            <el-icon><Back /></el-icon>
            返回管理
          </el-button>
        </div>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section mt-6">
      <div class="bg-white rounded-lg shadow p-6">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">酒店</label>
            <el-select v-model="filters.hotelId" placeholder="选择酒店" clearable>
              <el-option label="全部酒店" value="" />
              <!-- 酒店列表需要从API获取 -->
            </el-select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">时间范围</label>
            <el-select v-model="filters.timeRange" placeholder="选择时间范围">
              <el-option label="最近7天" value="7d" />
              <el-option label="最近30天" value="30d" />
              <el-option label="最近90天" value="90d" />
              <el-option label="自定义" value="custom" />
            </el-select>
          </div>

          <div v-if="filters.timeRange === 'custom'">
            <label class="block text-sm font-medium text-gray-700 mb-2">开始日期</label>
            <el-date-picker
              v-model="filters.startDate"
              type="date"
              placeholder="选择开始日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </div>

          <div v-if="filters.timeRange === 'custom'">
            <label class="block text-sm font-medium text-gray-700 mb-2">结束日期</label>
            <el-date-picker
              v-model="filters.endDate"
              type="date"
              placeholder="选择结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </div>
        </div>

        <div class="flex justify-end mt-4 space-x-4">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="applyFilters">应用筛选</el-button>
        </div>
      </div>
    </div>

    <!-- 仪表板概览 -->
    <div class="dashboard-overview mt-6">
      <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <!-- 总体评分趋势 -->
        <div class="lg:col-span-2">
          <ReviewTrendChart
            title="总体评分趋势"
            :data="trendData.ratingTrends"
            :loading="loading"
          />
        </div>

        <!-- 评价数量趋势 -->
        <div class="lg:col-span-2">
          <ReviewCountChart
            title="评价数量趋势"
            :data="trendData.countTrends"
            :loading="loading"
          />
        </div>

        <!-- 状态分布 -->
        <div>
          <ReviewStatusChart
            title="评价状态分布"
            :data="qualityData.statusCounts"
            :loading="loading"
          />
        </div>

        <!-- 评分分布 -->
        <div>
          <ReviewRatingChart
            title="评分分布"
            :data="statistics.ratingDistribution"
            :loading="loading"
          />
        </div>

        <!-- 质量指标 -->
        <div class="lg:col-span-2">
          <ReviewQualityMetrics
            title="质量分析指标"
            :data="qualityData"
            :loading="loading"
          />
        </div>
      </div>
    </div>

    <!-- 详细分析 -->
    <div class="detailed-analysis mt-6">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 趋势分析 -->
        <el-tab-pane label="趋势分析" name="trends">
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <ReviewTrendChart
              title="详细评分趋势"
              :data="trendData.ratingTrends"
              :loading="loading"
              height="400px"
            />
            <ReviewCountChart
              title="详细数量趋势"
              :data="trendData.countTrends"
              :loading="loading"
              height="400px"
            />
          </div>
        </el-tab-pane>

        <!-- 质量分析 -->
        <el-tab-pane label="质量分析" name="quality">
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <ReviewQualityMetrics
              title="详细质量指标"
              :data="qualityData"
              :loading="loading"
            />
            <ReviewStatusChart
              title="状态分布详细"
              :data="qualityData.statusCounts"
              :loading="loading"
              height="300px"
            />
            <ReviewViolationChart
              title="违规类型分析"
              :data="violationData"
              :loading="loading"
              height="300px"
            />
          </div>
        </el-tab-pane>

        <!-- 审核统计 -->
        <el-tab-pane label="审核统计" name="moderation">
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <ModerationStatsChart
              title="审核操作统计"
              :data="moderationStats.actionCounts"
              :loading="loading"
            />
            <ModerationEfficiencyChart
              title="审核效率分析"
              :data="moderationStats"
              :loading="loading"
            />
          </div>
        </el-tab-pane>

        <!-- 对比分析 -->
        <el-tab-pane label="对比分析" name="comparison">
          <ReviewComparisonChart
            title="时期对比分析"
            :data="comparisonData"
            :loading="loading"
            height="400px"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 数据表格 -->
    <div class="data-table mt-6">
      <el-card>
        <template #header>
          <div class="flex justify-between items-center">
            <h3 class="text-lg font-medium">详细数据</h3>
            <el-button size="small" @click="refreshTableData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </template>

        <el-table
          :data="tableData"
          :loading="tableLoading"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="date" label="日期" width="120" />
          <el-table-column prop="totalCount" label="总评价数" width="100" />
          <el-table-column prop="approvedCount" label="通过数" width="100" />
          <el-table-column prop="rejectedCount" label="拒绝数" width="100" />
          <el-table-column prop="pendingCount" label="待审核" width="100" />
          <el-table-column prop="averageRating" label="平均评分" width="120">
            <template #default="scope">
              <el-rate
                v-model="scope.row.averageRating"
                disabled
                show-score
                text-color="#ff9900"
                score-template="{value}"
              />
            </template>
          </el-table-column>
          <el-table-column prop="replyRate" label="回复率" width="100">
            <template #default="scope">
              {{ (scope.row.replyRate * 100).toFixed(1) }}%
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button
                size="small"
                type="primary"
                @click="viewDayDetails(scope.row.date)"
              >
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="mt-4 flex justify-end">
          <el-pagination
            v-model:current-page="tablePagination.page"
            v-model:page-size="tablePagination.size"
            :total="tablePagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="handleTableSizeChange"
            @current-change="handleTablePageChange"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Download,
  Back
} from '@element-plus/icons-vue'

// 组件导入
import ReviewTrendChart from '@/components/business/review/ReviewTrendChart.vue'
import ReviewCountChart from '@/components/business/review/ReviewCountChart.vue'
import ReviewStatusChart from '@/components/business/review/ReviewStatusChart.vue'
import ReviewRatingChart from '@/components/business/review/ReviewRatingChart.vue'
import ReviewQualityMetrics from '@/components/business/review/ReviewQualityMetrics.vue'
import ReviewViolationChart from '@/components/business/review/ReviewViolationChart.vue'
import ModerationStatsChart from '@/components/business/review/ModerationStatsChart.vue'
import ModerationEfficiencyChart from '@/components/business/review/ModerationEfficiencyChart.vue'
import ReviewComparisonChart from '@/components/business/review/ReviewComparisonChart.vue'

// 服务导入
import reviewService, {
  type ReviewStatisticsResponse,
  type ReviewAnalyticsRequest
} from '@/services/reviewService'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const tableLoading = ref(false)
const activeTab = ref('trends')

// 筛选条件
const filters = reactive({
  hotelId: '',
  timeRange: '30d',
  startDate: '',
  endDate: ''
})

// 数据状态
const statistics = ref<ReviewStatisticsResponse>({
  hotelId: 0,
  totalReviews: 0,
  overallRating: 0,
  cleanlinessRating: 0,
  serviceRating: 0,
  facilitiesRating: 0,
  locationRating: 0,
  ratingDistribution: {
    rating5: 0,
    rating4: 0,
    rating3: 0,
    rating2: 0,
    rating1: 0
  },
  reviewsWithImages: 0,
  averageCommentLength: 0
})

const trendData = ref({
  countTrends: [] as Array<{ date: string; count: number }>,
  ratingTrends: [] as Array<{ date: string; rating: number }>,
  statusTrends: [] as Array<{ date: string; approved: number; rejected: number; pending: number }>
})

const qualityData = ref({
  statusCounts: {} as Record<string, number>,
  violationRate: 0,
  replyRate: 0,
  totalReplies: 0,
  averageModerationTime: 0
})

const moderationStats = ref({
  actionCounts: {} as Record<string, number>,
  totalActions: 0,
  recentActions: 0
})

const violationData = ref({})
const comparisonData = ref({})

// 表格数据
const tableData = ref([])
const tablePagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 计算属性
const analyticsRequest = computed(() => {
  const request: ReviewAnalyticsRequest = {
    hotelId: filters.hotelId ? Number(filters.hotelId) : undefined,
    groupBy: 'day'
  }

  if (filters.timeRange === 'custom') {
    request.startDate = filters.startDate
    request.endDate = filters.endDate
  } else {
    // 计算日期范围
    const days = Number(filters.timeRange.replace('d', ''))
    const endDate = new Date()
    const startDate = new Date()
    startDate.setDate(endDate.getDate() - days)

    request.startDate = startDate.toISOString().split('T')[0]
    request.endDate = endDate.toISOString().split('T')[0]
  }

  return request
})

// 方法定义
const loadDashboardData = async () => {
  loading.value = true
  try {
    const [dashboardResponse, overallStatsResponse] = await Promise.all([
      reviewService.getDashboardData(analyticsRequest.value.hotelId),
      reviewService.getOverallStatistics(analyticsRequest.value.hotelId)
    ])

    if (dashboardResponse.success) {
      const dashboard = dashboardResponse.data
      statistics.value = dashboard.statistics
      qualityData.value = dashboard.quality
      moderationStats.value = dashboard.moderationStats
      trendData.value = dashboard.trends
    }

    if (overallStatsResponse.success) {
      statistics.value = overallStatsResponse.data
    }
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
    ElMessage.error('加载仪表板数据失败')
  } finally {
    loading.value = false
  }
}

const loadAnalyticsData = async () => {
  try {
    const [trendsResponse, qualityResponse, moderationResponse] = await Promise.all([
      reviewService.getReviewTrends(analyticsRequest.value),
      reviewService.getQualityAnalysis(analyticsRequest.value.hotelId),
      reviewService.getModerationStatistics()
    ])

    if (trendsResponse.success) {
      trendData.value = trendsResponse.data
    }

    if (qualityResponse.success) {
      qualityData.value = qualityResponse.data
    }

    if (moderationResponse.success) {
      moderationStats.value = moderationResponse.data
    }
  } catch (error) {
    console.error('加载分析数据失败:', error)
    ElMessage.error('加载分析数据失败')
  }
}

const loadTableData = async () => {
  tableLoading.value = true
  try {
    // 这里应该调用实际的表格数据API
    // 暂时使用模拟数据
    tableData.value = []
    tablePagination.total = 0
  } catch (error) {
    console.error('加载表格数据失败:', error)
    ElMessage.error('加载表格数据失败')
  } finally {
    tableLoading.value = false
  }
}

const refreshData = async () => {
  await Promise.all([
    loadDashboardData(),
    loadAnalyticsData(),
    loadTableData()
  ])
}

const refreshTableData = () => {
  loadTableData()
}

const applyFilters = () => {
  refreshData()
}

const resetFilters = () => {
  Object.assign(filters, {
    hotelId: '',
    timeRange: '30d',
    startDate: '',
    endDate: ''
  })
  refreshData()
}

const handleTableSizeChange = (size: number) => {
  tablePagination.size = size
  tablePagination.page = 1
  loadTableData()
}

const handleTablePageChange = (page: number) => {
  tablePagination.page = page
  loadTableData()
}

const viewDayDetails = (date: string) => {
  // 查看某天的详细数据
  console.log('查看详情:', date)
}

const exportData = () => {
  // 导出数据报告
  ElMessage.info('导出功能开发中')
}

const goToManagement = () => {
  router.push('/admin/reviews')
}

// 生命周期
onMounted(() => {
  refreshData()
})
</script>

<style scoped>
.review-analytics {
  padding: 24px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.filter-section {
  margin-bottom: 24px;
}

.dashboard-overview {
  margin-bottom: 24px;
}

.detailed-analysis {
  margin-bottom: 24px;
}

.data-table {
  margin-bottom: 24px;
}

:deep(.el-tabs__content) {
  padding: 20px 0;
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>