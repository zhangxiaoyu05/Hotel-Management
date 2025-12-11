<template>
  <div class="review-list">
    <!-- 筛选器和排序 -->
    <div class="review-controls">
      <ReviewFilter
        v-model="filters"
        @filter-change="handleFilterChange"
      />
      <SortSelector
        v-model="sortOptions"
        @sort-change="handleSortChange"
      />
    </div>

    <!-- 评价列表 -->
    <div v-if="loading" class="review-list__loading">
      <div v-for="i in 4" :key="i" class="review-card__skeleton">
        <div class="skeleton-header">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-info">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
        <div class="skeleton-content">
          <div class="skeleton-line"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line long"></div>
        </div>
      </div>
    </div>

    <div v-else-if="reviews.length === 0" class="review-list__empty">
      <div class="empty-icon">📝</div>
      <p class="empty-text">暂无评价</p>
      <p class="empty-subtitle">期待您的宝贵意见</p>
    </div>

    <div v-else class="review-list__content">
      <ReviewCard
        v-for="review in reviews"
        :key="review.id"
        :review="review"
        @click="handleReviewClick"
      />
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="review-list__pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, jumper, total"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore && !loadingMore" class="review-list__loadmore">
      <button class="loadmore-btn" @click="loadMore">
        加载更多评价
      </button>
    </div>

    <div v-if="loadingMore" class="review-list__loading-more">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import ReviewCard from './ReviewCard.vue'
import ReviewFilter from './ReviewFilter.vue'
import SortSelector from './SortSelector.vue'
import reviewService, { type ReviewResponse, type ReviewQueryRequest } from '@/services/reviewService'

interface Props {
  hotelId: number
  pageSize?: number
  showPagination?: boolean
  showLoadMore?: boolean
  infiniteScroll?: boolean
}

interface Emits {
  (e: 'review-click', review: ReviewResponse): void
  (e: 'total-change', total: number): void
}

const props = withDefaults(defineProps<Props>(), {
  pageSize: 10,
  showPagination: true,
  showLoadMore: false,
  infiniteScroll: false
})

const emit = defineEmits<Emits>()

// 响应式数据
const reviews = ref<ReviewResponse[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const total = ref(0)
const totalPages = ref(0)
const currentPage = ref(1)
const hasMore = ref(false)

// 筛选条件
const filters = reactive({
  minRating: null as number | null,
  maxRating: null as number | null,
  hasImages: null as boolean | null,
  roomId: null as number | null
})

// 排序选项
const sortOptions = reactive({
  sortBy: 'date' as 'date' | 'rating',
  sortOrder: 'desc' as 'asc' | 'desc'
})

// 加载评价列表
const loadReviews = async (page: number = 1, append: boolean = false) => {
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    const queryRequest: ReviewQueryRequest = {
      hotelId: props.hotelId,
      page,
      size: props.pageSize,
      sortBy: sortOptions.sortBy,
      sortOrder: sortOptions.sortOrder,
      ...filters
    }

    const response = await reviewService.queryReviews(queryRequest)

    if (response.success) {
      const newReviews = response.data.reviews

      if (append) {
        reviews.value.push(...newReviews)
      } else {
        reviews.value = newReviews
        currentPage.value = page
      }

      total.value = response.data.total
      totalPages.value = response.data.totalPages
      hasMore.value = page < response.data.totalPages

      emit('total-change', total.value)
    } else {
      ElMessage.error(response.message || '加载评价失败')
    }
  } catch (error: any) {
    console.error('加载评价失败:', error)
    ElMessage.error('加载评价失败，请稍后重试')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 刷新评价列表
const refresh = () => {
  reviews.value = []
  currentPage.value = 1
  loadReviews(1)
}

// 处理筛选变化
const handleFilterChange = () => {
  reviews.value = []
  currentPage.value = 1
  loadReviews(1)
}

// 处理排序变化
const handleSortChange = () => {
  reviews.value = []
  currentPage.value = 1
  loadReviews(1)
}

// 处理分页变化
const handlePageChange = (page: number) => {
  loadReviews(page)
}

// 加载更多
const loadMore = () => {
  if (hasMore.value && !loadingMore.value) {
    loadReviews(currentPage.value + 1, true)
  }
}

// 处理评价点击
const handleReviewClick = (review: ReviewResponse) => {
  emit('review-click', review)
}

// 无限滚动处理
const handleScroll = () => {
  if (!props.infiniteScroll || loadingMore.value || !hasMore.value) return

  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  const scrollThreshold = 100 // 距离底部100px时开始加载

  if (scrollTop + clientHeight >= scrollHeight - scrollThreshold) {
    loadMore()
  }
}

// 监听酒店ID变化
watch(() => props.hotelId, (newHotelId) => {
  if (newHotelId) {
    refresh()
  }
})

// 监听无限滚动
onMounted(() => {
  if (props.infiniteScroll) {
    window.addEventListener('scroll', handleScroll, { passive: true })
  }
  loadReviews()
})

// 清理事件监听
import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (props.infiniteScroll) {
    window.removeEventListener('scroll', handleScroll)
  }
})

// 暴露方法给父组件
defineExpose({
  refresh,
  loadMore
})
</script>

<style scoped>
.review-list {
  width: 100%;
}

.review-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  gap: 16px;
}

.review-list__loading {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card__skeleton {
  padding: 20px;
  background: white;
  border-radius: 8px;
  border: 1px solid #eee;
}

.skeleton-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

.skeleton-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
}

.skeleton-line.short {
  width: 60%;
}

.skeleton-line.long {
  width: 80%;
}

.review-list__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.empty-subtitle {
  font-size: 14px;
  color: #666;
}

.review-list__content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-list__pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  border: 1px solid #eee;
}

.review-list__loadmore {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.loadmore-btn {
  padding: 12px 32px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.loadmore-btn:hover {
  background: #0056b3;
}

.review-list__loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding: 16px;
  color: #666;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .review-controls {
    flex-direction: column;
    gap: 12px;
  }

  .review-list__pagination {
    :deep(.el-pagination) {
      justify-content: center;
      flex-wrap: wrap;
    }
  }
}
</style>