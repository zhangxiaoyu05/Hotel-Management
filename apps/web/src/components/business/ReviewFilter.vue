<template>
  <div class="review-filter">
    <div class="filter-group">
      <!-- 评分筛选 -->
      <div class="filter-item">
        <label class="filter-label">评分</label>
        <div class="rating-filter">
          <button
            v-for="rating in [5, 4, 3, 2, 1]"
            :key="rating"
            :class="['rating-btn', { active: isRatingSelected(rating) }]"
            @click="toggleRating(rating)"
          >
            {{ rating }}星
          </button>
        </div>
      </div>

      <!-- 图片筛选 -->
      <div class="filter-item">
        <label class="filter-label">图片</label>
        <el-checkbox v-model="localFilters.hasImages" @change="handleFilterChange">
          有图片评价
        </el-checkbox>
      </div>

      <!-- 清空筛选 -->
      <div class="filter-actions">
        <button class="clear-btn" @click="clearFilters">
          清空筛选
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'

interface Props {
  modelValue: {
    minRating?: number | null
    maxRating?: number | null
    hasImages?: boolean | null
    roomId?: number | null
  }
}

interface Emits {
  (e: 'update:modelValue', value: any): void
  (e: 'filter-change', filters: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 本地筛选状态
const localFilters = reactive({
  minRating: null as number | null,
  maxRating: null as number | null,
  hasImages: null as boolean | null,
  roomId: null as number | null
})

// 评分筛选按钮状态
const ratingButtons = ref([
  { rating: 5, selected: false },
  { rating: 4, selected: false },
  { rating: 3, selected: false },
  { rating: 2, selected: false },
  { rating: 1, selected: false }
])

// 同步props到localFilters
watch(() => props.modelValue, (newValue) => {
  Object.assign(localFilters, newValue)
  updateRatingButtons()
}, { immediate: true, deep: true })

// 计算选中了哪些评分
const selectedRatings = computed(() => {
  return ratingButtons.value.filter(btn => btn.selected).map(btn => btn.rating)
})

// 检查评分是否被选中
const isRatingSelected = (rating: number) => {
  return ratingButtons.value.find(btn => btn.rating === rating)?.selected || false
}

// 切换评分选择
const toggleRating = (rating: number) => {
  const button = ratingButtons.value.find(btn => btn.rating === rating)
  if (button) {
    button.selected = !button.selected
    updateRatingFilter()
  }
}

// 更新评分筛选条件
const updateRatingFilter = () => {
  const selected = selectedRatings.value
  if (selected.length === 0) {
    localFilters.minRating = null
    localFilters.maxRating = null
  } else {
    localFilters.maxRating = Math.max(...selected)
    localFilters.minRating = Math.min(...selected)
  }
  handleFilterChange()
}

// 更新评分按钮状态
const updateRatingButtons = () => {
  const { minRating, maxRating } = localFilters
  ratingButtons.value.forEach(button => {
    button.selected = minRating !== null && maxRating !== null &&
      button.rating >= minRating && button.rating <= maxRating
  })
}

// 处理筛选变化
const handleFilterChange = () => {
  emit('update:modelValue', { ...localFilters })
  emit('filter-change', { ...localFilters })
}

// 清空所有筛选
const clearFilters = () => {
  ratingButtons.value.forEach(button => {
    button.selected = false
  })
  localFilters.minRating = null
  localFilters.maxRating = null
  localFilters.hasImages = null
  localFilters.roomId = null
  handleFilterChange()
}
</script>

<style scoped>
.review-filter {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}

.rating-filter {
  display: flex;
  gap: 8px;
}

.rating-btn {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  color: #666;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.rating-btn:hover {
  border-color: #007bff;
  color: #007bff;
}

.rating-btn.active {
  background: #007bff;
  border-color: #007bff;
  color: white;
}

.filter-actions {
  margin-left: auto;
}

.clear-btn {
  padding: 6px 16px;
  background: none;
  border: 1px solid #ddd;
  border-radius: 4px;
  color: #666;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.clear-btn:hover {
  border-color: #007bff;
  color: #007bff;
  background: #f8f9ff;
}

/* 复选框样式调整 */
:deep(.el-checkbox) {
  font-size: 12px;
}

:deep(.el-checkbox__label) {
  color: #666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-group {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .filter-actions {
    margin-left: 0;
    align-self: flex-end;
  }

  .rating-filter {
    flex-wrap: wrap;
    gap: 6px;
  }

  .rating-btn {
    padding: 4px 8px;
    font-size: 11px;
  }
}
</style>