<template>
  <div class="review-management-list">
    <!-- 表头选择控制 -->
    <div class="px-6 py-4 border-b border-gray-200">
      <el-checkbox
        v-model="selectAll"
        :indeterminate="isIndeterminate"
        @change="handleSelectAll"
      >
        全选
      </el-checkbox>
      <span class="ml-4 text-sm text-gray-600">
        已选择 {{ selectedCount }} 条
      </span>
    </div>

    <!-- 评价列表 -->
    <div v-if="loading" class="p-8 text-center">
      <el-icon class="animate-spin text-4xl text-gray-400">
        <Loading />
      </el-icon>
      <p class="mt-2 text-gray-600">加载中...</p>
    </div>

    <div v-else-if="reviews.length === 0" class="p-8 text-center text-gray-500">
      <el-icon class="text-4xl mb-2">
        <Document />
      </el-icon>
      <p>暂无评价数据</p>
    </div>

    <div v-else>
      <div
        v-for="review in reviews"
        :key="review.id"
        class="review-item border-b border-gray-200 hover:bg-gray-50"
      >
        <div class="px-6 py-4">
          <div class="flex items-start space-x-4">
            <!-- 选择框 -->
            <div class="mt-1">
              <el-checkbox
                :model-value="selectedReviewIds.includes(review.id)"
                @change="(checked) => handleItemSelect(review.id, checked)"
              />
            </div>

            <!-- 状态标签 -->
            <div class="mt-1">
              <el-tag :type="getStatusType(review.status)" size="small">
                {{ getStatusText(review.status) }}
              </el-tag>
            </div>

            <!-- 评分显示 -->
            <div class="flex-1">
              <div class="flex items-center space-x-4 mb-2">
                <div class="flex items-center">
                  <span class="text-sm text-gray-600 mr-2">总体评分:</span>
                  <div class="flex items-center">
                    <el-rate
                      v-model="review.overallRating"
                      disabled
                      show-score
                      text-color="#ff9900"
                      score-template="{value}"
                    />
                  </div>
                </div>

                <div class="text-sm text-gray-500">
                  {{ formatDate(review.createdAt) }}
                </div>
              </div>

              <!-- 评价内容 -->
              <div class="mb-3">
                <p class="text-gray-800 line-clamp-3">
                  {{ review.comment }}
                </p>
                <div v-if="review.images && review.images.length > 0" class="mt-2">
                  <el-tag size="small" type="info">
                    {{ review.images.length }} 张图片
                  </el-tag>
                </div>
              </div>

              <!-- 详细评分 -->
              <div class="grid grid-cols-4 gap-4 mb-3 text-sm">
                <div class="flex items-center">
                  <span class="text-gray-600">清洁度:</span>
                  <el-rate
                    v-model="review.cleanlinessRating"
                    disabled
                    size="small"
                  />
                </div>
                <div class="flex items-center">
                  <span class="text-gray-600">服务态度:</span>
                  <el-rate
                    v-model="review.serviceRating"
                    disabled
                    size="small"
                  />
                </div>
                <div class="flex items-center">
                  <span class="text-gray-600">设施设备:</span>
                  <el-rate
                    v-model="review.facilitiesRating"
                    disabled
                    size="small"
                  />
                </div>
                <div class="flex items-center">
                  <span class="text-gray-600">地理位置:</span>
                  <el-rate
                    v-model="review.locationRating"
                    disabled
                    size="small"
                  />
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="flex space-x-2">
                <el-button
                  size="small"
                  type="primary"
                  @click="handleModerate(review)"
                  v-if="review.status === 'PENDING'"
                >
                  审核
                </el-button>
                <el-button
                  size="small"
                  type="success"
                  @click="handleReply(review)"
                  v-if="review.status === 'APPROVED'"
                >
                  回复
                </el-button>
                <el-button
                  size="small"
                  @click="handleViewDetails(review)"
                >
                  查看详情
                </el-button>
                <el-dropdown @command="(command) => handleDropdownCommand(command, review)">
                  <el-button size="small">
                    更多操作
                    <el-icon class="ml-1">
                      <ArrowDown />
                    </el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        command="approve"
                        v-if="review.status !== 'APPROVED'"
                      >
                        通过
                      </el-dropdown-item>
                      <el-dropdown-item
                        command="reject"
                        v-if="review.status !== 'REJECTED'"
                      >
                        拒绝
                      </el-dropdown-item>
                      <el-dropdown-item
                        command="hide"
                        v-if="review.status !== 'HIDDEN'"
                      >
                        隐藏
                      </el-dropdown-item>
                      <el-dropdown-item
                        command="delete"
                        divided
                      >
                        删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Loading,
  Document,
  ArrowDown
} from '@element-plus/icons-vue'
import type { ReviewResponse } from '@/services/reviewService'

interface Props {
  reviews: ReviewResponse[]
  loading: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [reviewIds: number[]]
  moderate: [review: ReviewResponse]
  reply: [review: ReviewResponse]
  viewDetails: [review: ReviewResponse]
}>()

const selectedReviewIds = ref<number[]>([])

// 计算属性
const selectAll = computed({
  get: () => props.reviews.length > 0 && selectedReviewIds.value.length === props.reviews.length,
  set: (value: boolean) => handleSelectAll(value)
})

const isIndeterminate = computed(() =>
  selectedReviewIds.value.length > 0 && selectedReviewIds.value.length < props.reviews.length
)

const selectedCount = computed(() => selectedReviewIds.value.length)

// 方法定义
const handleSelectAll = (checked: boolean) => {
  if (checked) {
    selectedReviewIds.value = props.reviews.map(review => review.id)
  } else {
    selectedReviewIds.value = []
  }
  emitSelect()
}

const handleItemSelect = (reviewId: number, checked: boolean) => {
  if (checked) {
    if (!selectedReviewIds.value.includes(reviewId)) {
      selectedReviewIds.value.push(reviewId)
    }
  } else {
    const index = selectedReviewIds.value.indexOf(reviewId)
    if (index > -1) {
      selectedReviewIds.value.splice(index, 1)
    }
  }
  emitSelect()
}

const emitSelect = () => {
  emit('select', [...selectedReviewIds.value])
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'PENDING':
      return 'warning'
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'HIDDEN':
      return 'info'
    default:
      return ''
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING':
      return '待审核'
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已拒绝'
    case 'HIDDEN':
      return '已隐藏'
    default:
      return status
  }
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleModerate = (review: ReviewResponse) => {
  emit('moderate', review)
}

const handleReply = (review: ReviewResponse) => {
  emit('reply', review)
}

const handleViewDetails = (review: ReviewResponse) => {
  emit('viewDetails', review)
}

const handleDropdownCommand = async (command: string, review: ReviewResponse) => {
  switch (command) {
    case 'approve':
      await handleQuickApprove(review)
      break
    case 'reject':
      await handleQuickReject(review)
      break
    case 'hide':
      await handleQuickHide(review)
      break
    case 'delete':
      await handleQuickDelete(review)
      break
  }
}

const handleQuickApprove = async (review: ReviewResponse) => {
  try {
    await ElMessageBox.confirm(
      `确定要通过这条评价吗？`,
      '确认通过',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    // 这里应该调用快速审核API
    ElMessage.success('审核通过')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审核失败:', error)
      ElMessage.error('审核失败')
    }
  }
}

const handleQuickReject = async (review: ReviewResponse) => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请输入拒绝理由:',
      '确认拒绝',
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
    // 这里应该调用快速拒绝API
    ElMessage.success('拒绝成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拒绝失败:', error)
      ElMessage.error('拒绝失败')
    }
  }
}

const handleQuickHide = async (review: ReviewResponse) => {
  try {
    await ElMessageBox.confirm(
      '确定要隐藏这条评价吗？',
      '确认隐藏',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    // 这里应该调用快速隐藏API
    ElMessage.success('隐藏成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('隐藏失败:', error)
      ElMessage.error('隐藏失败')
    }
  }
}

const handleQuickDelete = async (review: ReviewResponse) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条评价吗？此操作不可恢复！',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }
    )
    // 这里应该调用删除API
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped>
.review-management-list {
  background: white;
}

.review-item {
  transition: background-color 0.2s;
}

.review-item:hover {
  background-color: #f9fafb;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

:deep(.el-rate) {
  display: flex;
  align-items: center;
}

:deep(.el-rate__text) {
  margin-left: 8px;
  font-size: 14px;
  color: #ff9900;
}
</style>