<template>
  <el-dialog
    v-model="dialogVisible"
    title="评价详情"
    width="80%"
    :before-close="handleClose"
    destroy-on-close
  >
    <div v-if="review" class="review-detail">
      <!-- 评价头部信息 -->
      <div class="review-header mb-6">
        <div class="bg-white border rounded-lg p-6">
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center space-x-4">
              <div>
                <div class="text-lg font-medium text-gray-900">
                  评价 #{{ review.id }}
                </div>
                <div class="text-sm text-gray-600">
                  订单ID: {{ review.orderId }}
                </div>
              </div>
              <el-tag :type="getStatusType(review.status)" size="large">
                {{ getStatusText(review.status) }}
              </el-tag>
            </div>
            <div class="text-right">
              <div class="text-sm text-gray-500">
                提交时间
              </div>
              <div class="text-lg font-medium">
                {{ formatDate(review.createdAt) }}
              </div>
            </div>
          </div>

          <!-- 用户信息 -->
          <div class="flex items-center space-x-4 p-4 bg-gray-50 rounded-lg">
            <el-avatar :size="48" src="/default-avatar.png" />
            <div>
              <div class="font-medium text-gray-900">
                {{ review.isAnonymous ? '匿名用户' : `用户 #${review.userId}` }}
              </div>
              <div class="text-sm text-gray-600">
                {{ review.isAnonymous ? '用户选择匿名评价' : `用户ID: ${review.userId}` }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 评分详情 -->
      <div class="rating-detail mb-6">
        <h3 class="text-lg font-medium mb-4">详细评分</h3>
        <div class="bg-white border rounded-lg p-6">
          <div class="grid grid-cols-2 lg:grid-cols-5 gap-6">
            <div class="text-center">
              <div class="mb-2">
                <el-rate
                  v-model="review.overallRating"
                  disabled
                  show-score
                  text-color="#ff9900"
                  score-template="{value}"
                  size="large"
                />
              </div>
              <div class="text-sm font-medium text-gray-700">总体评价</div>
            </div>
            <div class="text-center">
              <div class="mb-2">
                <el-rate
                  v-model="review.cleanlinessRating"
                  disabled
                  size="large"
                />
              </div>
              <div class="text-sm font-medium text-gray-700">清洁度</div>
            </div>
            <div class="text-center">
              <div class="mb-2">
                <el-rate
                  v-model="review.serviceRating"
                  disabled
                  size="large"
                />
              </div>
              <div class="text-sm font-medium text-gray-700">服务态度</div>
            </div>
            <div class="text-center">
              <div class="mb-2">
                <el-rate
                  v-model="review.facilitiesRating"
                  disabled
                  size="large"
                />
              </div>
              <div class="text-sm font-medium text-gray-700">设施设备</div>
            </div>
            <div class="text-center">
              <div class="mb-2">
                <el-rate
                  v-model="review.locationRating"
                  disabled
                  size="large"
                />
              </div>
              <div class="text-sm font-medium text-gray-700">地理位置</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 评价内容 -->
      <div class="review-content mb-6">
        <h3 class="text-lg font-medium mb-4">评价内容</h3>
        <div class="bg-white border rounded-lg p-6">
          <div class="prose max-w-none">
            <p class="text-gray-800 leading-relaxed whitespace-pre-wrap">
              {{ review.comment }}
            </p>
          </div>

          <!-- 内容分析 -->
          <div class="mt-4 pt-4 border-t">
            <div class="grid grid-cols-3 gap-4 text-sm">
              <div class="text-center">
                <div class="text-2xl font-bold text-blue-600">
                  {{ review.comment.length }}
                </div>
                <div class="text-gray-600">字符数</div>
              </div>
              <div class="text-center">
                <div class="text-2xl font-bold text-green-600">
                  {{ calculateWordCount(review.comment) }}
                </div>
                <div class="text-gray-600">字数</div>
              </div>
              <div class="text-center">
                <div class="text-2xl font-bold text-purple-600">
                  {{ hasImages ? review.images.length : 0 }}
                </div>
                <div class="text-gray-600">图片数</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 评价图片 -->
      <div v-if="hasImages" class="review-images mb-6">
        <h3 class="text-lg font-medium mb-4">评价图片</h3>
        <div class="bg-white border rounded-lg p-6">
          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <div
              v-for="(image, index) in review.images"
              :key="index"
              class="relative group"
            >
              <el-image
                :src="image"
                :alt="`评价图片${index + 1}`"
                fit="cover"
                class="w-full h-32 rounded-lg cursor-pointer shadow-md hover:shadow-lg transition-shadow"
                @click="previewImage(image)"
              />
              <div class="absolute top-2 right-2 bg-black bg-opacity-50 text-white text-xs px-2 py-1 rounded">
                {{ index + 1 }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 管理操作 -->
      <div class="management-actions mb-6">
        <h3 class="text-lg font-medium mb-4">管理操作</h3>
        <div class="bg-white border rounded-lg p-6">
          <div class="flex flex-wrap gap-3">
            <el-button
              v-if="review.status === 'PENDING'"
              type="success"
              @click="$emit('moderate', review)"
            >
              <el-icon class="mr-1">
                <CircleCheck />
              </el-icon>
              审核评价
            </el-button>

            <el-button
              v-if="review.status === 'APPROVED'"
              type="primary"
              @click="$emit('reply', review)"
            >
              <el-icon class="mr-1">
                <ChatDotRound />
              </el-icon>
              回复评价
            </el-button>

            <el-button
              type="info"
              @click="viewModerationHistory"
            >
              <el-icon class="mr-1">
                <Clock />
              </el-icon>
              审核历史
            </el-button>

            <el-button
              type="warning"
              @click="exportReview"
            >
              <el-icon class="mr-1">
                <Download />
              </el-icon>
              导出数据
            </el-button>
          </div>
        </div>
      </div>

      <!-- 关联信息 -->
      <div class="related-info mb-6">
        <h3 class="text-lg font-medium mb-4">关联信息</h3>
        <div class="bg-white border rounded-lg p-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h4 class="font-medium text-gray-900 mb-2">酒店信息</h4>
              <div class="space-y-2 text-sm">
                <div class="flex justify-between">
                  <span class="text-gray-600">酒店ID:</span>
                  <span class="font-medium">{{ review.hotelId }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-600">房间ID:</span>
                  <span class="font-medium">{{ review.roomId }}</span>
                </div>
              </div>
            </div>

            <div>
              <h4 class="font-medium text-gray-900 mb-2">订单信息</h4>
              <div class="space-y-2 text-sm">
                <div class="flex justify-between">
                  <span class="text-gray-600">订单ID:</span>
                  <span class="font-medium">{{ review.orderId }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-600">评价状态:</span>
                  <el-tag :type="getStatusType(review.status)" size="small">
                    {{ getStatusText(review.status) }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  CircleCheck,
  ChatDotRound,
  Clock,
  Download
} from '@element-plus/icons-vue'
import type { ReviewResponse } from '@/services/reviewService'

interface Props {
  visible: boolean
  review: ReviewResponse | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  moderate: [review: ReviewResponse]
  reply: [review: ReviewResponse]
}>()

// 计算属性
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const hasImages = computed(() => {
  return props.review?.images && props.review.images.length > 0
})

// 方法定义
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
    minute: '2-digit',
    second: '2-digit'
  })
}

const calculateWordCount = (text: string): number => {
  if (!text) return 0
  // 简单的中文字数计算
  return text.replace(/\s/g, '').length
}

const previewImage = (imageUrl: string) => {
  // 图片预览功能
  console.log('预览图片:', imageUrl)
  ElMessage.info('图片预览功能开发中')
}

const viewModerationHistory = () => {
  // 查看审核历史
  ElMessage.info('审核历史功能开发中')
}

const exportReview = () => {
  // 导出评价数据
  ElMessage.info('导出功能开发中')
}

const handleClose = () => {
  dialogVisible.value = false
}
</script>

<style scoped>
.review-detail {
  max-height: 75vh;
  overflow-y: auto;
}

:deep(.el-rate) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-rate__text) {
  margin-left: 8px;
  font-size: 16px;
  color: #ff9900;
}

:deep(.prose) {
  color: #374151;
  line-height: 1.75;
}

:deep(.prose p) {
  margin: 0;
}

:deep(.el-image) {
  transition: all 0.3s;
}

:deep(.el-image:hover) {
  transform: scale(1.02);
}
</style>