<template>
  <div class="review-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="flex justify-between items-center">
        <h1 class="text-2xl font-bold text-gray-900">评价管理</h1>
        <div class="flex space-x-4">
          <el-button @click="refreshData" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
          <el-button type="primary" @click="goToAnalytics">
            <el-icon><DataAnalysis /></el-icon>
            数据分析
          </el-button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="statistics-cards mt-6">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="bg-white rounded-lg shadow p-6">
          <div class="flex items-center">
            <div class="p-3 bg-blue-100 rounded-full">
              <el-icon class="text-blue-600 text-xl"><Document /></el-icon>
            </div>
            <div class="ml-4">
              <p class="text-sm text-gray-600">总评价数</p>
              <p class="text-2xl font-semibold text-gray-900">{{ statistics.totalReviews }}</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-lg shadow p-6">
          <div class="flex items-center">
            <div class="p-3 bg-yellow-100 rounded-full">
              <el-icon class="text-yellow-600 text-xl"><Clock /></el-icon>
            </div>
            <div class="ml-4">
              <p class="text-sm text-gray-600">待审核</p>
              <p class="text-2xl font-semibold text-yellow-600">{{ statistics.pendingReviews }}</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-lg shadow p-6">
          <div class="flex items-center">
            <div class="p-3 bg-green-100 rounded-full">
              <el-icon class="text-green-600 text-xl"><CircleCheck /></el-icon>
            </div>
            <div class="ml-4">
              <p class="text-sm text-gray-600">已通过</p>
              <p class="text-2xl font-semibold text-green-600">{{ statistics.approvedReviews }}</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-lg shadow p-6">
          <div class="flex items-center">
            <div class="p-3 bg-red-100 rounded-full">
              <el-icon class="text-red-600 text-xl"><CircleClose /></el-icon>
            </div>
            <div class="ml-4">
              <p class="text-sm text-gray-600">已拒绝</p>
              <p class="text-2xl font-semibold text-red-600">{{ statistics.rejectedReviews }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选和搜索区域 -->
    <div class="filter-section mt-6">
      <div class="bg-white rounded-lg shadow p-6">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">状态</label>
            <el-select v-model="filters.status" placeholder="选择状态" clearable>
              <el-option label="全部" value="" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="已隐藏" value="HIDDEN" />
            </el-select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">酒店</label>
            <el-select v-model="filters.hotelId" placeholder="选择酒店" clearable>
              <el-option label="全部酒店" value="" />
              <!-- 酒店列表需要从API获取 -->
            </el-select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">用户ID</label>
            <el-input v-model="filters.userId" placeholder="输入用户ID" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">开始日期</label>
            <el-date-picker
              v-model="filters.startDate"
              type="date"
              placeholder="选择开始日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </div>

          <div>
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
          <el-button type="primary" @click="searchReviews">搜索</el-button>
        </div>
      </div>
    </div>

    <!-- 批量操作工具栏 -->
    <BatchOperationsToolbar
      v-if="selectedReviews.length > 0"
      :selected-count="selectedReviews.length"
      @batch-approve="handleBatchApprove"
      @batch-reject="handleBatchReject"
      @batch-hide="handleBatchHide"
      @clear-selection="clearSelection"
    />

    <!-- 评价列表 -->
    <div class="review-list mt-6">
      <div class="bg-white rounded-lg shadow">
        <ReviewManagementList
          :reviews="reviews"
          :loading="loading"
          @select="handleReviewSelect"
          @moderate="handleModerateReview"
          @reply="handleReplyReview"
          @view-details="handleViewDetails"
        />

        <!-- 分页 -->
        <div class="px-6 py-4 border-t border-gray-200">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 审核对话框 -->
    <ReviewModerationDialog
      v-model:visible="moderationDialog.visible"
      :review="moderationDialog.review"
      @confirm="handleModerationConfirm"
    />

    <!-- 回复对话框 -->
    <ReviewReplyDialog
      v-model:visible="replyDialog.visible"
      :review="replyDialog.review"
      :reply="replyDialog.reply"
      @confirm="handleReplyConfirm"
    />

    <!-- 详情对话框 -->
    <ReviewDetailDialog
      v-model:visible="detailDialog.visible"
      :review="detailDialog.review"
    />

    <!-- 批量操作进度 -->
    <BatchOperationProgress
      v-model:visible="progressDialog.visible"
      :operation-type="progressDialog.operationType"
      ref="progressDialogRef"
      @retry="handleRetryBatchOperation"
      @export="handleExportBatchResult"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  DataAnalysis,
  Document,
  Clock,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'

// 组件导入
import BatchOperationsToolbar from '@/components/business/review/BatchOperationsToolbar.vue'
import ReviewManagementList from '@/components/business/review/ReviewManagementList.vue'
import ReviewModerationDialog from '@/components/business/review/ReviewModerationDialog.vue'
import ReviewReplyDialog from '@/components/business/review/ReviewReplyDialog.vue'
import ReviewDetailDialog from '@/components/business/review/ReviewDetailDialog.vue'
import BatchOperationProgress from '@/components/business/review/BatchOperationProgress.vue'

// 服务导入
import reviewService, {
  type ReviewResponse,
  type ReviewModerationRequest,
  type ReviewReplyRequest,
  type ReviewStatisticsResponse
} from '@/services/reviewService'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const reviews = ref<ReviewResponse[]>([])
const selectedReviews = ref<number[]>([])
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

// 筛选条件
const filters = reactive({
  status: '',
  hotelId: '',
  userId: '',
  startDate: '',
  endDate: ''
})

// 分页信息
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 对话框状态
const moderationDialog = reactive({
  visible: false,
  review: null as ReviewResponse | null
})

const replyDialog = reactive({
  visible: false,
  review: null as ReviewResponse | null,
  reply: null as any | null
})

const detailDialog = reactive({
  visible: false,
  review: null as ReviewResponse | null
})

const progressDialog = reactive({
  visible: false,
  operationType: ''
})

const progressDialogRef = ref()

// 计算属性
const hasData = computed(() => reviews.value.length > 0)

// 方法定义
const loadStatistics = async () => {
  try {
    const response = await reviewService.getOverallStatistics()
    if (response.success) {
      statistics.value = response.data
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

const loadReviews = async () => {
  loading.value = true
  try {
    const response = await reviewService.getReviewsForManagement({
      ...filters,
      page: pagination.page - 1,
      size: pagination.size
    })

    if (response.success) {
      reviews.value = response.data.content
      pagination.total = response.data.totalElements
    }
  } catch (error) {
    console.error('加载评价列表失败:', error)
    ElMessage.error('加载评价列表失败')
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  await Promise.all([
    loadStatistics(),
    loadReviews()
  ])
}

const searchReviews = () => {
  pagination.page = 1
  loadReviews()
}

const resetFilters = () => {
  Object.assign(filters, {
    status: '',
    hotelId: '',
    userId: '',
    startDate: '',
    endDate: ''
  })
  pagination.page = 1
  loadReviews()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadReviews()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadReviews()
}

const handleReviewSelect = (reviewIds: number[]) => {
  selectedReviews.value = reviewIds
}

const clearSelection = () => {
  selectedReviews.value = []
}

const handleModerateReview = (review: ReviewResponse) => {
  moderationDialog.review = review
  moderationDialog.visible = true
}

const handleModerationConfirm = async (request: ReviewModerationRequest) => {
  if (!moderationDialog.review) return

  try {
    const response = await reviewService.moderateReview(
      moderationDialog.review.id,
      request
    )

    if (response.success) {
      ElMessage.success('审核操作成功')
      moderationDialog.visible = false
      await Promise.all([
        loadStatistics(),
        loadReviews()
      ])
    }
  } catch (error) {
    console.error('审核操作失败:', error)
    ElMessage.error('审核操作失败')
  }
}

const handleReplyReview = (review: ReviewResponse) => {
  replyDialog.review = review
  replyDialog.reply = null
  replyDialog.visible = true
}

const handleReplyConfirm = async (request: ReviewReplyRequest) => {
  if (!replyDialog.review) return

  try {
    const response = await reviewService.createReviewReply(
      replyDialog.review.id,
      request
    )

    if (response.success) {
      ElMessage.success('回复成功')
      replyDialog.visible = false
    }
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error('回复失败')
  }
}

const handleViewDetails = (review: ReviewResponse) => {
  detailDialog.review = review
  detailDialog.visible = true
}

const handleBatchApprove = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要通过选中的 ${selectedReviews.value.length} 条评价吗？`,
      '批量审核确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await reviewService.batchModerateReviews({
      reviewIds: selectedReviews.value,
      action: 'APPROVE',
      reason: '批量审核通过'
    })

    if (response.success) {
      ElMessage.success('批量审核成功')
      clearSelection()
      await Promise.all([
        loadStatistics(),
        loadReviews()
      ])
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量审核失败:', error)
      ElMessage.error('批量审核失败')
    }
  }
}

const handleBatchReject = async () => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `请输入拒绝理由（将拒绝选中的 ${selectedReviews.value.length} 条评价）：`,
      '批量拒绝',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputValidator: (value) => {
          if (!value || !value.trim()) {
            return '拒绝理由不能为空'
          }
          return true
        }
      }
    )

    const response = await reviewService.batchModerateReviews({
      reviewIds: selectedReviews.value,
      action: 'REJECT',
      reason: reason.trim()
    })

    if (response.success) {
      ElMessage.success('批量拒绝成功')
      clearSelection()
      await Promise.all([
        loadStatistics(),
        loadReviews()
      ])
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量拒绝失败:', error)
      ElMessage.error('批量拒绝失败')
    }
  }
}

const handleBatchHide = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要隐藏选中的 ${selectedReviews.value.length} 条评价吗？`,
      '批量隐藏确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await reviewService.batchModerateReviews({
      reviewIds: selectedReviews.value,
      action: 'HIDE',
      reason: '批量隐藏'
    })

    if (response.success) {
      ElMessage.success('批量隐藏成功')
      clearSelection()
      await Promise.all([
        loadStatistics(),
        loadReviews()
      ])
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量隐藏失败:', error)
      ElMessage.error('批量隐藏失败')
    }
  }
}

const goToAnalytics = () => {
  router.push('/admin/reviews/analytics')
}

const handleRetryBatchOperation = (errors: any[]) => {
  const failedReviewIds = errors.map(error => error.reviewId)
  selectedReviews.value = failedReviewIds
  // 可以根据错误类型重试相应的操作
  ElMessage.info(`已选择 ${failedReviewIds.length} 条失败的评价，请重新操作`)
}

const handleExportBatchResult = (result: any) => {
  // 导出批量操作结果
  const csvContent = [
    ['操作类型', progressDialog.operationType],
    ['总数', result.totalCount],
    ['成功', result.successCount],
    ['失败', result.errorCount],
    ['跳过', result.skippedCount],
    ['开始时间', result.startTime.toLocaleString()],
    ['结束时间', result.endTime?.toLocaleString() || ''],
    [],
    ['错误详情'],
    ...result.errors.map((error: any) => [
      error.reviewId,
      error.message,
      error.action || ''
    ])
  ].map(row => row.join(',')).join('\n')

  // 创建下载链接
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `batch_operation_${Date.now()}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  ElMessage.success('批量操作结果已导出')
}

// 生命周期
onMounted(() => {
  refreshData()
})
</script>

<style scoped>
.review-management {
  padding: 24px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.statistics-cards {
  margin-bottom: 24px;
}

.filter-section {
  margin-bottom: 24px;
}

.review-list {
  margin-bottom: 24px;
}
</style>