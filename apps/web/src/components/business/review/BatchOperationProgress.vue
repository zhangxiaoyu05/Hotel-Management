<template>
  <el-dialog
    v-model="visible"
    title="批量操作进度"
    width="500px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <div class="progress-container">
      <!-- 进度条 -->
      <div class="progress-section">
        <div class="progress-info">
          <span class="progress-text">{{ operationText }}</span>
          <span class="progress-count">{{ processedCount }} / {{ totalCount }}</span>
        </div>
        <el-progress
          :percentage="progressPercentage"
          :status="progressStatus"
          :stroke-width="8"
        />
      </div>

      <!-- 操作统计 -->
      <div class="stats-section" v-if="showStats">
        <div class="stat-item">
          <div class="stat-label">成功</div>
          <div class="stat-value success">{{ successCount }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">失败</div>
          <div class="stat-value error">{{ errorCount }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">跳过</div>
          <div class="stat-value warning">{{ skippedCount }}</div>
        </div>
      </div>

      <!-- 错误信息 -->
      <div class="error-section" v-if="errors.length > 0">
        <el-collapse>
          <el-collapse-item title="查看错误详情" name="errors">
            <div class="error-list">
              <div
                v-for="(error, index) in errors"
                :key="index"
                class="error-item"
              >
                <el-icon class="error-icon"><Warning /></el-icon>
                <span class="error-message">{{ error.message }}</span>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 时间信息 -->
      <div class="time-section">
        <div class="time-item">
          <span class="time-label">开始时间：</span>
          <span class="time-value">{{ formatTime(startTime) }}</span>
        </div>
        <div class="time-item" v-if="duration">
          <span class="time-label">用时：</span>
          <span class="time-value">{{ duration }}秒</span>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button
          v-if="isCompleted"
          @click="handleRetry"
          :disabled="errorCount === 0"
        >
          重试失败项
        </el-button>
        <el-button
          v-if="isCompleted"
          @click="handleExport"
          :disabled="processedCount === 0"
        >
          导出结果
        </el-button>
        <el-button
          :type="isCompleted ? 'primary' : 'default'"
          @click="handleClose"
          :disabled="!isCompleted"
        >
          {{ isCompleted ? '确定' : '后台运行' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'

interface BatchOperationError {
  reviewId: number
  message: string
  action?: string
}

interface BatchOperationResult {
  totalCount: number
  processedCount: number
  successCount: number
  errorCount: number
  skippedCount: number
  errors: BatchOperationError[]
  startTime: Date
  endTime?: Date
}

const props = defineProps<{
  visible: boolean
  operationType: string // 'approve', 'reject', 'hide', 'reply'等
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'retry': [errors: BatchOperationError[]]
  'export': [result: BatchOperationResult]
}>()

// 响应式数据
const result = ref<BatchOperationResult>({
  totalCount: 0,
  processedCount: 0,
  successCount: 0,
  errorCount: 0,
  skippedCount: 0,
  errors: [],
  startTime: new Date()
})

const isRunning = ref(true)
const isCompleted = ref(false)

// 计算属性
const progressPercentage = computed(() => {
  if (result.value.totalCount === 0) return 0
  return Math.round((result.value.processedCount / result.value.totalCount) * 100)
})

const progressStatus = computed(() => {
  if (isCompleted.value) {
    return result.value.errorCount > 0 ? 'exception' : 'success'
  }
  return undefined
})

const showStats = computed(() => {
  return result.value.successCount > 0 || result.value.errorCount > 0 || result.value.skippedCount > 0
})

const operationText = computed(() => {
  const operationMap: Record<string, string> = {
    approve: '审核通过',
    reject: '审核拒绝',
    hide: '隐藏评价',
    delete: '删除评价',
    reply: '回复评价'
  }
  return operationMap[props.operationType] || '处理中'
})

const errors = computed(() => result.value.errors)

const duration = computed(() => {
  if (!result.value.endTime) return null
  return Math.round((result.value.endTime.getTime() - result.value.startTime.getTime()) / 1000)
})

// 方法
const updateProgress = (update: Partial<BatchOperationResult>) => {
  Object.assign(result.value, update)
}

const complete = () => {
  isRunning.value = false
  isCompleted.value = true
  result.value.endTime = new Date()

  // 显示完成消息
  if (result.value.errorCount === 0) {
    ElMessage.success(`批量${operationText.value}完成`)
  } else {
    ElMessage.warning(
      `批量${operationText.value}完成，成功 ${result.value.successCount} 条，失败 ${result.value.errorCount} 条`
    )
  }
}

const handleRetry = () => {
  if (errors.value.length > 0) {
    emit('retry', errors.value)
  }
}

const handleExport = () => {
  emit('export', result.value)
}

const handleClose = () => {
  if (!isCompleted.value) {
    ElMessage.info('操作将在后台继续运行')
  }
  emit('update:visible', false)
}

const formatTime = (date: Date) => {
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 模拟进度更新（实际使用时通过外部调用updateProgress方法）
const simulateProgress = () => {
  let count = 0
  const total = 50 // 模拟总数

  updateProgress({ totalCount: total })

  const interval = setInterval(() => {
    if (count >= total) {
      clearInterval(interval)
      complete()
      return
    }

    count++
    const success = Math.random() > 0.1 // 90%成功率

    if (success) {
      updateProgress({
        processedCount: count,
        successCount: result.value.successCount + 1
      })
    } else {
      updateProgress({
        processedCount: count,
        errorCount: result.value.errorCount + 1,
        errors: [
          ...result.value.errors,
          {
            reviewId: count,
            message: `处理失败：网络错误`,
            action: props.operationType
          }
        ]
      })
    }
  }, 200)
}

// 生命周期
onMounted(() => {
  // 仅用于演示，实际使用时删除
  if (props.visible) {
    simulateProgress()
  }
})

// 暴露方法供外部调用
defineExpose({
  updateProgress,
  complete,
  setTotalCount: (count: number) => updateProgress({ totalCount: count })
})
</script>

<style scoped>
.progress-container {
  padding: 20px 0;
}

.progress-section {
  margin-bottom: 24px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.progress-text {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.progress-count {
  font-size: 14px;
  color: #909399;
}

.stats-section {
  display: flex;
  justify-content: space-around;
  margin-bottom: 24px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.error {
  color: #f56c6c;
}

.stat-value.warning {
  color: #e6a23c;
}

.error-section {
  margin-bottom: 20px;
}

.error-list {
  max-height: 200px;
  overflow-y: auto;
}

.error-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.error-item:last-child {
  border-bottom: none;
}

.error-icon {
  color: #f56c6c;
  margin-right: 8px;
  flex-shrink: 0;
}

.error-message {
  font-size: 14px;
  color: #606266;
  word-break: break-all;
}

.time-section {
  padding: 12px 16px;
  background-color: #fafafa;
  border-radius: 4px;
}

.time-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.time-item:last-child {
  margin-bottom: 0;
}

.time-label {
  font-size: 14px;
  color: #909399;
}

.time-value {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.dialog-footer {
  text-align: right;
}
</style>