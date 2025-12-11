<template>
  <div class="batch-operations-toolbar">
    <div class="bg-blue-50 border-l-4 border-blue-400 p-4 mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center">
          <el-icon class="text-blue-400 mr-2">
            <InfoFilled />
          </el-icon>
          <span class="text-blue-700">
            已选择 <strong>{{ selectedCount }}</strong> 条评价
          </span>
        </div>
        <el-button
          type="text"
          size="small"
          @click="$emit('clearSelection')"
        >
          清空选择
        </el-button>
      </div>
    </div>

    <div class="flex flex-wrap gap-3 mb-6">
      <el-button
        type="success"
        :disabled="selectedCount === 0"
        @click="handleBatchApprove"
      >
        <el-icon class="mr-1">
          <CircleCheck />
        </el-icon>
        批量通过
      </el-button>

      <el-button
        type="danger"
        :disabled="selectedCount === 0"
        @click="handleBatchReject"
      >
        <el-icon class="mr-1">
          <CircleClose />
        </el-icon>
        批量拒绝
      </el-button>

      <el-button
        type="warning"
        :disabled="selectedCount === 0"
        @click="handleBatchHide"
      >
        <el-icon class="mr-1">
          <Hide />
        </el-icon>
        批量隐藏
      </el-button>

      <el-button
        type="info"
        :disabled="selectedCount === 0"
        @click="handleExport"
      >
        <el-icon class="mr-1">
          <Download />
        </el-icon>
        导出选中
      </el-button>
    </div>

    <!-- 快速操作提示 -->
    <div class="text-sm text-gray-600 bg-gray-50 p-3 rounded-lg">
      <p class="font-medium mb-1">批量操作说明：</p>
      <ul class="list-disc list-inside space-y-1 text-xs">
        <li>批量操作最多处理100条评价</li>
        <li>删除等不可逆操作需要二次确认</li>
        <li>所有批量操作都会记录详细日志</li>
        <li>批量删除的操作可以在30分钟内撤销</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import {
  InfoFilled,
  CircleCheck,
  CircleClose,
  Hide,
  Download
} from '@element-plus/icons-vue'

interface Props {
  selectedCount: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  batchApprove: []
  batchReject: []
  batchHide: []
  clearSelection: []
}>()

const handleBatchApprove = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要通过选中的 ${props.selectedCount} 条评价吗？`,
      '批量通过确认',
      {
        confirmButtonText: '确定通过',
        cancelButtonText: '取消',
        type: 'success',
        dangerouslyUseHTMLString: true,
        message: `
          <div>
            <p>即将通过 <strong>${props.selectedCount}</strong> 条评价</p>
            <p class="text-sm text-gray-600 mt-2">通过后评价将在用户端显示</p>
          </div>
        `
      }
    )
    emit('batchApprove')
  } catch (error) {
    // 用户取消操作
  }
}

const handleBatchReject = async () => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `请输入拒绝理由（将拒绝选中的 ${props.selectedCount} 条评价）：`,
      '批量拒绝',
      {
        confirmButtonText: '确定拒绝',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请输入拒绝理由...',
        inputValidator: (value) => {
          if (!value || !value.trim()) {
            return '拒绝理由不能为空'
          }
          if (value.trim().length < 5) {
            return '拒绝理由至少需要5个字符'
          }
          return true
        }
      }
    )
    // 这里应该传递拒绝理由
    emit('batchReject')
  } catch (error) {
    // 用户取消操作
  }
}

const handleBatchHide = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要隐藏选中的 ${props.selectedCount} 条评价吗？`,
      '批量隐藏确认',
      {
        confirmButtonText: '确定隐藏',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true,
        message: `
          <div>
            <p>即将隐藏 <strong>${props.selectedCount}</strong> 条评价</p>
            <p class="text-sm text-gray-600 mt-2">隐藏后评价将不会在用户端显示，但数据仍然保留</p>
          </div>
        `
      }
    )
    emit('batchHide')
  } catch (error) {
    // 用户取消操作
  }
}

const handleExport = () => {
  // 导出功能实现
  console.log('导出选中的评价')
}
</script>

<style scoped>
.batch-operations-toolbar {
  margin-bottom: 24px;
}

:deep(.el-button.is-disabled) {
  cursor: not-allowed;
}
</style>