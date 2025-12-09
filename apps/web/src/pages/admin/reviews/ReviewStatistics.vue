<template>
  <div class="review-statistics">
    <div class="page-header">
      <h1>评价统计分析</h1>
      <div class="header-actions">
        <el-select v-model="selectedHotel" placeholder="选择酒店" @change="onHotelChange" style="width: 200px; margin-right: 16px;">
          <el-option
            v-for="hotel in hotels"
            :key="hotel.id"
            :label="hotel.name"
            :value="hotel.id"
          />
        </el-select>
        <el-select v-model="selectedPeriod" placeholder="选择周期" @change="onPeriodChange" style="width: 120px; margin-right: 16px;">
          <el-option label="日报" value="daily" />
          <el-option label="周报" value="weekly" />
          <el-option label="月报" value="monthly" />
          <el-option label="季报" value="quarterly" />
          <el-option label="年报" value="yearly" />
        </el-select>
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button @click="exportData">
          <el-icon><Download /></el-icon>
          导出报告
        </el-button>
      </div>
    </div>

    <!-- 综合评分概览 -->
    <div class="statistics-overview" v-loading="overviewLoading">
      <el-row :gutter="24">
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon">
              <el-icon size="40" color="#409EFF"><Star /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">综合评分</div>
              <div class="card-value">{{ overviewData.overallRating || '0.0' }}</div>
              <div class="card-subtitle">共 {{ overviewData.totalReviews || 0 }} 条评价</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon">
              <el-icon size="40" color="#67C23A"><TrendCharts /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">评分趋势</div>
              <div class="card-value">{{ getRatingChange() }}</div>
              <div class="card-subtitle">较上期{{ getRatingChangeText() }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon">
              <el-icon size="40" color="#E6A23C"><User /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">净推荐值</div>
              <div class="card-value">{{ getNPSValue() }}</div>
              <div class="card-subtitle">客户推荐意愿</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon">
              <el-icon size="40" color="#F56C6C"><Timer /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">平均响应</div>
              <div class="card-value">{{ getResponseTime() }}</div>
              <div class="card-subtitle">客户反馈响应时间</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 维度评分分析 -->
    <div class="dimension-ratings" v-loading="overviewLoading">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>维度评分分析</span>
            <el-radio-group v-model="dimensionView" size="small">
              <el-radio-button label="radar">雷达图</el-radio-button>
              <el-radio-button label="bar">柱状图</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <StatisticsOverview
          :data="overviewData"
          :view-type="dimensionView"
          v-if="!overviewLoading"
        />
      </el-card>
    </div>

    <!-- 评分趋势图表 -->
    <div class="rating-trends">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>评分趋势分析</span>
            <div class="trend-controls">
              <el-date-picker
                v-model="trendDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                @change="onDateRangeChange"
                size="small"
              />
              <el-select v-model="trendGroupBy" @change="onTrendGroupByChange" size="small" style="width: 100px; margin-left: 12px;">
                <el-option label="按天" value="day" />
                <el-option label="按周" value="week" />
                <el-option label="按月" value="month" />
              </el-select>
            </div>
          </div>
        </template>
        <RatingTrendChart
          :data="trendData"
          :loading="trendLoading"
          v-if="!trendLoading"
        />
      </el-card>
    </div>

    <!-- 词云和建议 -->
    <el-row :gutter="24" class="wordcloud-suggestions">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>评价关键词云</span>
              <el-button type="text" @click="refreshWordCloud">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>
          <ReviewWordCloud
            :data="wordCloudData"
            :loading="wordCloudLoading"
            v-if="!wordCloudLoading"
          />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>改进建议</span>
              <el-select v-model="suggestionCategory" @change="onSuggestionCategoryChange" size="small">
                <el-option label="全部" value="" />
                <el-option label="服务" value="service" />
                <el-option label="卫生" value="cleanliness" />
                <el-option label="设施" value="facilities" />
                <el-option label="位置" value="location" />
                <el-option label="其他" value="general" />
              </el-select>
            </div>
          </template>
          <ImprovementSuggestions
            :data="suggestions"
            :loading="suggestionsLoading"
            v-if="!suggestionsLoading"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 酒店对比分析 -->
    <div class="hotel-comparison" v-if="showComparison">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>酒店对比分析</span>
            <div class="comparison-controls">
              <el-input
                v-model="competitorInput"
                placeholder="输入竞品酒店ID，多个用逗号分隔"
                @blur="onCompetitorChange"
                size="small"
                style="width: 250px; margin-right: 12px;"
              />
              <el-button type="primary" @click="updateComparison" size="small">更新对比</el-button>
            </div>
          </div>
        </template>
        <HotelComparisonChart
          :data="comparisonData"
          :loading="comparisonLoading"
          v-if="!comparisonLoading"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Star,
  TrendCharts,
  User,
  Timer,
  Refresh,
  Download
} from '@element-plus/icons-vue'

// 导入组件
import StatisticsOverview from '@/components/business/review/statistics/StatisticsOverview.vue'
import RatingTrendChart from '@/components/business/review/statistics/RatingTrendChart.vue'
import ReviewWordCloud from '@/components/business/review/statistics/ReviewWordCloud.vue'
import ImprovementSuggestions from '@/components/business/review/statistics/ImprovementSuggestions.vue'
import HotelComparisonChart from '@/components/business/review/statistics/HotelComparisonChart.vue'

// 导入服务
import { reviewService } from '@/services/reviewService'

// 响应式数据
const selectedHotel = ref(1)
const selectedPeriod = ref('monthly')
const dimensionView = ref('radar')
const trendDateRange = ref<[Date, Date]>([])
const trendGroupBy = ref('day')
const suggestionCategory = ref('')
const competitorInput = ref('')
const showComparison = ref(true)

// 加载状态
const overviewLoading = ref(false)
const trendLoading = ref(false)
const wordCloudLoading = ref(false)
const suggestionsLoading = ref(false)
const comparisonLoading = ref(false)

// 数据
const hotels = ref([
  { id: 1, name: '成都希尔顿酒店' },
  { id: 2, name: '成都香格里拉大酒店' },
  { id: 3, name: '成都万达瑞华酒店' }
])

const overviewData = ref<any>({})
const trendData = ref([])
const wordCloudData = ref([])
const suggestions = ref([])
const comparisonData = ref([])

// 计算属性
const getRatingChange = () => {
  if (!overviewData.value.monthOverMonth) return '0.0'
  const change = overviewData.value.monthOverMonth.ratingChange || 0
  return change >= 0 ? `+${change.toFixed(1)}` : change.toFixed(1)
}

const getRatingChangeText = () => {
  const change = getRatingChange()
  return change.startsWith('+') ? '上升' : change === '0.0' ? '持平' : '下降'
}

const getNPSValue = () => {
  if (!trendData.value || trendData.value.length === 0) return '0'
  const latestTrend = trendData.value[trendData.value.length - 1]
  return latestTrend.nps ? `${latestTrend.nps.toFixed(0)}%` : '0%'
}

const getResponseTime = () => {
  if (!trendData.value || trendData.value.length === 0) return '0小时'
  const latestTrend = trendData.value[trendData.value.length - 1]
  return latestTrend.averageResponseTime ? `${latestTrend.averageResponseTime.toFixed(1)}小时` : '0小时'
}

// 方法
const onHotelChange = () => {
  loadAllData()
}

const onPeriodChange = () => {
  loadOverviewData()
}

const onDateRangeChange = () => {
  loadTrendData()
}

const onTrendGroupByChange = () => {
  loadTrendData()
}

const onSuggestionCategoryChange = () => {
  loadSuggestions()
}

const onCompetitorChange = () => {
  if (competitorInput.value) {
    loadComparisonData()
  }
}

const refreshData = () => {
  loadAllData()
  ElMessage.success('数据刷新成功')
}

const refreshWordCloud = () => {
  loadWordCloudData()
}

const exportData = () => {
  // 导出功能
  ElMessage.info('导出功能开发中...')
}

const updateComparison = () => {
  loadComparisonData()
}

const loadAllData = async () => {
  await Promise.all([
    loadOverviewData(),
    loadTrendData(),
    loadWordCloudData(),
    loadSuggestions(),
    loadComparisonData()
  ])
}

const loadOverviewData = async () => {
  try {
    overviewLoading.value = true
    const response = await reviewService.getOverviewStatistics(selectedHotel.value, selectedPeriod.value)
    overviewData.value = response.data
  } catch (error) {
    console.error('加载概览数据失败:', error)
    ElMessage.error('加载概览数据失败')
  } finally {
    overviewLoading.value = false
  }
}

const loadTrendData = async () => {
  try {
    trendLoading.value = true
    const [startDate, endDate] = trendDateRange.value || [new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), new Date()]
    const response = await reviewService.getRatingTrends(
      selectedHotel.value,
      startDate.toISOString(),
      endDate.toISOString(),
      trendGroupBy.value
    )
    trendData.value = response.data
  } catch (error) {
    console.error('加载趋势数据失败:', error)
    ElMessage.error('加载趋势数据失败')
  } finally {
    trendLoading.value = false
  }
}

const loadWordCloudData = async () => {
  try {
    wordCloudLoading.value = true
    const response = await reviewService.getWordCloud(selectedHotel.value, 50)
    wordCloudData.value = response.data
  } catch (error) {
    console.error('加载词云数据失败:', error)
    ElMessage.error('加载词云数据失败')
  } finally {
    wordCloudLoading.value = false
  }
}

const loadSuggestions = async () => {
  try {
    suggestionsLoading.value = true
    const response = await reviewService.getSuggestions(
      selectedHotel.value,
      suggestionCategory.value || undefined
    )
    suggestions.value = response.data
  } catch (error) {
    console.error('加载改进建议失败:', error)
    ElMessage.error('加载改进建议失败')
  } finally {
    suggestionsLoading.value = false
  }
}

const loadComparisonData = async () => {
  try {
    comparisonLoading.value = true
    const competitorIds = competitorInput.value
      ? competitorInput.value.split(',').map(id => parseInt(id.trim()))
      : []
    const response = await reviewService.getHotelComparison(selectedHotel.value, competitorIds)
    comparisonData.value = response.data
  } catch (error) {
    console.error('加载对比数据失败:', error)
    ElMessage.error('加载对比数据失败')
  } finally {
    comparisonLoading.value = false
  }
}

// 生命周期
onMounted(() => {
  // 设置默认时间范围为最近30天
  const endDate = new Date()
  const startDate = new Date(endDate.getTime() - 30 * 24 * 60 * 60 * 1000)
  trendDateRange.value = [startDate, endDate]

  // 加载所有数据
  loadAllData()
})
</script>

<style scoped lang="scss">
.review-statistics {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h1 {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }

  .header-actions {
    display: flex;
    align-items: center;
  }
}

.statistics-overview {
  margin-bottom: 24px;

  .overview-card {
    background: white;
    border-radius: 8px;
    padding: 24px;
    display: flex;
    align-items: center;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

    .card-icon {
      margin-right: 16px;
    }

    .card-content {
      flex: 1;

      .card-title {
        font-size: 14px;
        color: #909399;
        margin-bottom: 8px;
      }

      .card-value {
        font-size: 28px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 4px;
      }

      .card-subtitle {
        font-size: 12px;
        color: #C0C4CC;
      }
    }
  }
}

.dimension-ratings,
.rating-trends,
.wordcloud-suggestions,
.hotel-comparison {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.trend-controls,
.comparison-controls {
  display: flex;
  align-items: center;
}
</style>