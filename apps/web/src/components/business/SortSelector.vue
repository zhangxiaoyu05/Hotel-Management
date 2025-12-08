<template>
  <div class="sort-selector">
    <label class="sort-label">排序</label>
    <div class="sort-controls">
      <!-- 排序方式选择 -->
      <el-select
        v-model="localSort.sortBy"
        placeholder="排序方式"
        size="small"
        @change="handleSortChange"
      >
        <el-option
          v-for="option in sortOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>

      <!-- 排序顺序切换 -->
      <button
        :class="['sort-order-btn', { 'desc': localSort.sortOrder === 'desc' }]"
        @click="toggleSortOrder"
      >
        <span class="sort-icon">↓</span>
        <span class="sort-text">{{ localSort.sortOrder === 'desc' ? '降序' : '升序' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'

interface Props {
  modelValue: {
    sortBy?: 'date' | 'rating'
    sortOrder?: 'asc' | 'desc'
  }
}

interface Emits {
  (e: 'update:modelValue', value: any): void
  (e: 'sort-change', sort: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 排序选项
const sortOptions = [
  { label: '最新评价', value: 'date' },
  { label: '评分最高', value: 'rating' }
]

// 本地排序状态
const localSort = reactive({
  sortBy: 'date' as 'date' | 'rating',
  sortOrder: 'desc' as 'asc' | 'desc'
})

// 同步props到localSort
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    localSort.sortBy = newValue.sortBy || 'date'
    localSort.sortOrder = newValue.sortOrder || 'desc'
  }
}, { immediate: true, deep: true })

// 处理排序变化
const handleSortChange = () => {
  emit('update:modelValue', { ...localSort })
  emit('sort-change', { ...localSort })
}

// 切换排序顺序
const toggleSortOrder = () => {
  localSort.sortOrder = localSort.sortOrder === 'desc' ? 'asc' : 'desc'
  handleSortChange()
}
</script>

<style scoped>
.sort-selector {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sort-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}

.sort-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-order-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  color: #666;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.sort-order-btn:hover {
  border-color: #007bff;
  color: #007bff;
  background: #f8f9ff;
}

.sort-order-btn.desc {
  background: #007bff;
  border-color: #007bff;
  color: white;
}

.sort-icon {
  font-size: 12px;
  line-height: 1;
  transition: transform 0.3s;
}

.sort-order-btn:not(.desc) .sort-icon {
  transform: rotate(180deg);
}

.sort-text {
  font-size: 12px;
}

/* Select组件样式调整 */
:deep(.el-select) {
  width: 120px;
}

:deep(.el-input--small) {
  font-size: 12px;
}

:deep(.el-input__inner) {
  font-size: 12px;
  padding: 0 8px;
}

:deep(.el-select .el-input .el-select__caret) {
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sort-selector {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .sort-controls {
    width: 100%;
    justify-content: space-between;
  }

  :deep(.el-select) {
    flex: 1;
    max-width: 120px;
  }
}
</style>