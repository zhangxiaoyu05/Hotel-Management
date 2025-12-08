<template>
  <el-dialog
    v-model="dialogVisible"
    title="评价审核"
    width="80%"
    :before-close="handleClose"
    destroy-on-close
  >
    <div v-if="review" class="moderation-content">
      <!-- 评价基本信息 -->
      <div class="review-basic-info mb-6">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center space-x-4">
            <span class="text-sm text-gray-600">评价ID: {{ review.id }}</span>
            <el-tag :type="getStatusType(review.status)" size="small">
              {{ getStatusText(review.status) }}
            </el-tag>
            <span class="text-sm text-gray-600">
              {{ formatDate(review.createdAt) }}
            </span>
          </div>
        </div>

        <!-- 评分展示 -->
        <div class="bg-gray-50 rounded-lg p-4 mb-4">
          <div class="grid grid-cols-5 gap-4">
            <div class="text-center">
              <div class="text-sm text-gray-600 mb-1">总体评价</div>
              <div class="flex justify-center items-center">
                <el-rate
                  v-model="review.overallRating"
                  disabled
                  show-score
                  text-color="#ff9900"
                  score-template="{value}"
                />
              </div>
            </div>
            <div class="text-center">
              <div class="text-sm text-gray-600 mb-1">清洁度</div>
              <div class="flex justify-center">
                <el-rate
                  v-model="review.cleanlinessRating"
                  disabled
                  size="small"
                />
              </div>
            </div>
            <div class="text-center">
              <div class="text-sm text-gray-600 mb-1">服务态度</div>
              <div class="flex justify-center">
                <el-rate
                  v-model="review.serviceRating"
                  disabled
                  size="small"
                />
              </div>
            </div>
            <div class="text-center">
              <div class="text-sm text-gray-600 mb-1">设施设备</div>
              <div class="flex justify-center">
                <el-rate
                  v-model="review.facilitiesRating"
                  disabled
                  size="small"
                />
              </div>
            </div>
            <div class="text-center">
              <div class="text-sm text-gray-600 mb-1">地理位置</div>
              <div class="flex justify-center">
                <el-rate
                  v-model="review.locationRating"
                  disabled
                  size="small"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 评价内容预览 -->
      <div class="review-content mb-6">
        <h3 class="text-lg font-medium mb-3">评价内容</h3>
        <div class="bg-white border rounded-lg p-4">
          <div class="mb-3">
            <div v-if="containsSensitiveContent" class="bg-red-50 border-l-4 border-red-400 p-3 mb-3">
              <div class="flex items-center">
                <el-icon class="text-red-400 mr-2">
                  <Warning />
                </el-icon>
                <span class="text-red-700 text-sm">
                  检测到敏感内容，请仔细审核
                </span>
              </div>
            </div>
            <p class="text-gray-800 leading-relaxed whitespace-pre-wrap">
              {{ review.comment }}
            </p>
          </div>

          <!-- 图片展示 -->
          <div v-if="review.images && review.images.length > 0" class="mt-4">
            <h4 class="text-sm font-medium text-gray-700 mb-2">评价图片 ({{ review.images.length }}张)</h4>
            <div class="grid grid-cols-4 gap-2">
              <div
                v-for="(image, index) in review.images"
                :key="index"
                class="relative group"
              >
                <el-image
                  :src="image"
                  :alt="`评价图片${index + 1}`"
                  fit="cover"
                  class="w-full h-20 rounded cursor-pointer"
                  @click="previewImage(image)"
                />
                <div class="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-opacity rounded flex items-center justify-center">
                  <el-icon class="text-white opacity-0 group-hover:opacity-100 transition-opacity">
                    <ZoomIn />
                  </el-icon>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 审核操作 -->
      <div class="moderation-actions">
        <h3 class="text-lg font-medium mb-3">审核操作</h3>
        <div class="bg-gray-50 rounded-lg p-4">
          <!-- 快速操作按钮 -->
          <div class="grid grid-cols-3 gap-4 mb-4">
            <el-button
              type="success"
              size="large"
              @click="handleApprove"
              :disabled="review.status === 'APPROVED'"
            >
              <el-icon class="mr-2">
                <CircleCheck />
              </el-icon>
              通过审核
            </el-button>

            <el-button
              type="danger"
              size="large"
              @click="handleReject"
              :disabled="review.status === 'REJECTED'"
            >
              <el-icon class="mr-2">
                <CircleClose />
              </el-icon>
              拒绝审核
            </el-button>

            <el-button
              type="warning"
              size="large"
              @click="handleMark"
            >
              <el-icon class="mr-2">
                <Warning />
              </el-icon>
              标记违规
            </el-button>
          </div>

          <!-- 详细操作表单 -->
          <el-form
            ref="formRef"
            :model="moderationForm"
            :rules="formRules"
            label-width="80px"
          >
            <el-form-item label="操作类型" prop="action">
              <el-select v-model="moderationForm.action" placeholder="请选择操作类型">
                <el-option label="通过" value="APPROVE" />
                <el-option label="拒绝" value="REJECT" />
                <el-option label="标记" value="MARK" />
                <el-option label="隐藏" value="HIDE" />
                <el-option label="删除" value="DELETE" />
              </el-select>
            </el-form-item>

            <el-form-item
              label="操作原因"
              prop="reason"
              :required="['REJECT', 'MARK', 'HIDE', 'DELETE'].includes(moderationForm.action)"
            >
              <el-input
                v-model="moderationForm.reason"
                type="textarea"
                :rows="3"
                placeholder="请输入操作原因..."
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-form>

          <div class="flex justify-end mt-4 space-x-3">
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">
              提交审核
            </el-button>
          </div>
        </div>
      </div>

      <!-- 审核历史 -->
      <div class="moderation-history mt-6" v-if="historyLogs.length > 0">
        <h3 class="text-lg font-medium mb-3">审核历史</h3>
        <div class="bg-gray-50 rounded-lg p-4">
          <el-timeline>
            <el-timeline-item
              v-for="log in historyLogs"
              :key="log.id"
              :timestamp="formatDate(log.createdAt)"
              :type="getLogType(log.action)"
            >
              <div class="flex items-center justify-between">
                <div>
                  <span class="font-medium">{{ getActionText(log.action) }}</span>
                  <span class="text-sm text-gray-600 ml-2">操作人: {{ log.adminName }}</span>
                </div>
                <el-tag size="small" :type="getStatusType(log.newStatus)">
                  {{ getStatusText(log.newStatus) }}
                </el-tag>
              </div>
              <p v-if="log.reason" class="text-sm text-gray-600 mt-1">{{ log.reason }}</p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          提交审核
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Warning,
  ZoomIn,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'
import type { ReviewResponse, ReviewModerationRequest, ReviewModerationLogResponse } from '@/services/reviewService'
import reviewService from '@/services/reviewService'

interface Props {
  visible: boolean
  review: ReviewResponse | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: [request: ReviewModerationRequest]
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const historyLogs = ref<ReviewModerationLogResponse[]>([])

// 表单数据
const moderationForm = reactive({
  action: '',
  reason: ''
})

// 表单验证规则
const formRules: FormRules = {
  action: [
    { required: true, message: '请选择操作类型', trigger: 'change' }
  ],
  reason: [
    { required: true, message: '请输入操作原因', trigger: 'blur' },
    { min: 5, message: '操作原因至少需要5个字符', trigger: 'blur' }
  ]
}

// 计算属性
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const containsSensitiveContent = computed(() => {
  if (!props.review?.comment) return false

  const sensitiveWords = ['垃圾', '骗子', '差劲', '恶心', '投诉', '举报', '诈骗']
  return sensitiveWords.some(word => props.review!.comment.includes(word))
})

// 监听review变化
watch(() => props.review, async (newReview) => {
  if (newReview && props.visible) {
    await loadModerationHistory(newReview.id)
    moderationForm.action = ''
    moderationForm.reason = ''
  }
}, { immediate: true })

// 方法定义
const loadModerationHistory = async (reviewId: number) => {
  try {
    const response = await reviewService.getModerationLogs(reviewId)
    if (response.success) {
      historyLogs.value = response.data
    }
  } catch (error) {
    console.error('加载审核历史失败:', error)
  }
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

const getLogType = (action: string) => {
  switch (action) {
    case 'APPROVE':
      return 'success'
    case 'REJECT':
    case 'DELETE':
      return 'danger'
    case 'MARK':
      return 'warning'
    case 'HIDE':
      return 'info'
    default:
      return 'primary'
  }
}

const getActionText = (action: string) => {
  switch (action) {
    case 'APPROVE':
      return '通过审核'
    case 'REJECT':
      return '拒绝审核'
    case 'MARK':
      return '标记违规'
    case 'HIDE':
      return '隐藏评价'
    case 'DELETE':
      return '删除评价'
    default:
      return action
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

const handleApprove = () => {
  moderationForm.action = 'APPROVE'
  moderationForm.reason = '审核通过'
  handleSubmit()
}

const handleReject = async () => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请输入拒绝理由:',
      '拒绝审核',
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

    moderationForm.action = 'REJECT'
    moderationForm.reason = reason.trim()
    handleSubmit()
  } catch (error) {
    // 用户取消
  }
}

const handleMark = () => {
  moderationForm.action = 'MARK'
  moderationForm.reason = '标记为违规内容'
}

const previewImage = (imageUrl: string) => {
  // 图片预览功能
  console.log('预览图片:', imageUrl)
}

const handleClose = () => {
  dialogVisible.value = false
}

const handleSubmit = async () => {
  if (!props.review) return

  try {
    await formRef.value?.validate()

    submitting.value = true

    const request: ReviewModerationRequest = {
      action: moderationForm.action as any,
      reason: moderationForm.reason.trim()
    }

    // 如果是删除操作，需要二次确认
    if (request.action === 'DELETE') {
      await ElMessageBox.confirm(
        '确定要删除这条评价吗？此操作不可恢复！',
        '确认删除',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error'
        }
      )
    }

    emit('confirm', request)
    dialogVisible.value = false

    ElMessage.success('审核操作提交成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('表单验证失败:', error)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.moderation-content {
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.el-rate) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-rate__text) {
  margin-left: 8px;
  font-size: 14px;
  color: #ff9900;
}

:deep(.el-image) {
  border-radius: 4px;
}

:deep(.el-timeline-item__content) {
  padding-bottom: 20px;
}
</style>