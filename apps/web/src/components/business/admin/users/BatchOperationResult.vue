<template>
  <el-dialog
    v-model="dialogVisible"
    title="批量操作结果"
    width="600px"
    :close-on-click-modal="false"
  >
    <div class="result-content" v-if="result">
      <!-- 操作概览 -->
      <div class="result-overview">
        <el-result
          :icon="result.successCount === result.totalCount ? 'success' : result.failureCount === result.totalCount ? 'error' : 'warning'"
          :title="getResultTitle()"
          :sub-title="getResultSubtitle()"
        >
          <template #extra>
            <div class="result-stats">
              <el-row :gutter="20">
                <el-col :span="8">
                  <div class="stat-item total">
                    <div class="stat-number">{{ result.totalCount }}</div>
                    <div class="stat-label">总数量</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="stat-item success">
                    <div class="stat-number">{{ result.successCount }}</div>
                    <div class="stat-label">成功</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="stat-item failure">
                    <div class="stat-number">{{ result.failureCount }}</div>
                    <div class="stat-label">失败</div>
                  </div>
                </el-col>
              </el-row>
            </div>
          </template>
        </el-result>
      </div>

      <!-- 操作详情 -->
      <div class="result-details" v-if="result.failureCount > 0">
        <div class="details-header">
          <h4>失败详情</h4>
          <el-button
            type="text"
            size="small"
            @click="toggleDetailsExpanded"
          >
            {{ detailsExpanded ? '收起' : '展开' }}
            <el-icon>
              <component :is="detailsExpanded ? 'ArrowUp' : 'ArrowDown'" />
            </el-icon>
          </el-button>
        </div>

        <el-collapse-transition>
          <div v-show="detailsExpanded" class="failure-list">
            <el-table
              :data="failureList"
              style="width: 100%"
              size="small"
              max-height="300"
            >
              <el-table-column prop="userId" label="用户ID" width="100" />
              <el-table-column prop="reason" label="失败原因" min-width="200" />
            </el-table>
          </div>
        </el-collapse-transition>
      </div>

      <!-- 操作信息 -->
      <div class="operation-info">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="操作类型">
            <el-tag :type="getOperationType(result.operation)">
              {{ formatOperationType(result.operation) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作状态">
            <el-tag :type="result.isCompleted ? 'success' : 'warning'">
              {{ result.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作时间">
            {{ formatDate(result.operatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="操作者">
            {{ result.operatorUsername || '系统' }}
          </el-descriptions-item>
          <el-descriptions-item label="操作原因" :span="2" v-if="result.reason">
            {{ result.reason }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 成功用户列表（仅显示部分） -->
      <div class="success-list" v-if="result.successCount > 0 && result.successCount <= 10">
        <div class="list-header">
          <h4>成功用户 ({{ result.successCount }})</h4>
        </div>
        <div class="success-users">
          <el-tag
            v-for="userId in result.successUserIds"
            :key="userId"
            type="success"
            size="small"
            class="user-tag"
          >
            用户ID: {{ userId }}
          </el-tag>
        </div>
      </div>

      <div class="success-list" v-if="result.successCount > 10">
        <div class="list-header">
          <h4>成功用户 ({{ result.successCount }})</h4>
          <span class="more-info">仅显示前10个，共{{ result.successCount }}个用户操作成功</span>
        </div>
        <div class="success-users">
          <el-tag
            v-for="userId in result.successUserIds.slice(0, 10)"
            :key="userId"
            type="success"
            size="small"
            class="user-tag"
          >
            用户ID: {{ userId }}
          </el-tag>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button
          v-if="result?.failureCount > 0"
          type="primary"
          @click="handleRetryFailed"
        >
          重试失败项
        </el-button>
        <el-button
          v-if="result?.successCount > 0"
          type="success"
          @click="handleViewSuccess"
        >
          查看成功项
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import type { BatchOperationResultDTO } from '@/types/userManagement'

// Props
const props = defineProps<{
  modelValue: boolean
  result: BatchOperationResultDTO | null
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'retry-failed': [result: BatchOperationResultDTO]
  'view-success': [userIds: number[]]
}>()

// 响应式数据
const detailsExpanded = ref(false)

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const failureList = computed(() => {
  if (!props.result?.failureReasons) return []

  return Object.entries(props.result.failureReasons).map(([userId, reason]) => ({
    userId: parseInt(userId),
    reason
  }))
})

// 监听器
watch(
  () => props.result,
  () => {
    detailsExpanded.value = false
  }
)

// 方法

/**
 * 获取结果标题
 */
const getResultTitle = () => {
  if (!props.result) return ''

  const { successCount, totalCount } = props.result

  if (successCount === totalCount) {
    return '操作全部成功'
  } else if (successCount === 0) {
    return '操作全部失败'
  } else {
    return '操作部分成功'
  }
}

/**
 * 获取结果副标题
 */
const getResultSubtitle = () => {
  if (!props.result) return ''

  const { successCount, failureCount, totalCount } = props.result

  if (successCount === totalCount) {
    return `共处理 ${totalCount} 个用户，全部操作成功`
  } else if (successCount === 0) {
    return `共处理 ${totalCount} 个用户，全部操作失败`
  } else {
    return `共处理 ${totalCount} 个用户，成功 ${successCount} 个，失败 ${failureCount} 个`
  }
}

/**
 * 获取操作类型标签样式
 */
const getOperationType = (operation: string) => {
  const typeMap: Record<string, string> = {
    'ENABLE': 'success',
    'DISABLE': 'warning',
    'DELETE': 'danger'
  }
  return typeMap[operation] || 'info'
}

/**
 * 格式化操作类型
 */
const formatOperationType = (operation: string) => {
  const operationMap: Record<string, string> = {
    'ENABLE': '批量启用',
    'DISABLE': '批量禁用',
    'DELETE': '批量删除'
  }
  return operationMap[operation] || operation
}

/**
 * 格式化日期
 */
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

/**
 * 切换详情展开状态
 */
const toggleDetailsExpanded = () => {
  detailsExpanded.value = !detailsExpanded.value
}

/**
 * 关闭对话框
 */
const handleClose = () => {
  dialogVisible.value = false
}

/**
 * 重试失败项
 */
const handleRetryFailed = () => {
  if (props.result) {
    emit('retry-failed', props.result)
    handleClose()
  }
}

/**
 * 查看成功项
 */
const handleViewSuccess = () => {
  if (props.result?.successUserIds) {
    emit('view-success', props.result.successUserIds)
    handleClose()
  }
}
</script>

<style scoped>
.result-content {
  padding: 0;
}

.result-overview {
  margin-bottom: 24px;
}

.result-stats {
  margin-top: 20px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  border-radius: 8px;
  background: #f8f9fa;
}

.stat-item.total {
  background: #e3f2fd;
}

.stat-item.success {
  background: #e8f5e8;
}

.stat-item.failure {
  background: #ffebee;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-item.total .stat-number {
  color: #1976d2;
}

.stat-item.success .stat-number {
  color: #388e3c;
}

.stat-item.failure .stat-number {
  color: #d32f2f;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.result-details {
  margin-bottom: 24px;
}

.details-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.details-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.failure-list {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.operation-info {
  margin-bottom: 24px;
}

.success-list {
  margin-bottom: 16px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.list-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.more-info {
  font-size: 12px;
  color: #999;
}

.success-users {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-tag {
  margin: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-result__icon) {
  font-size: 48px;
}

:deep(.el-result__title) {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

:deep(.el-result__subtitle) {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-descriptions__content) {
  color: #303133;
}
</style>