<template>
  <div class="hotel-reviews">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">{{ hotelName || '酒店' }}评价</h1>
        <div class="header-actions">
          <button class="back-btn" @click="goBack">
            <span class="icon">←</span>
            返回酒店详情
          </button>
          <button class="write-review-btn" @click="goToWriteReview">
            <span class="icon">✍️</span>
            写评价
          </button>
        </div>
      </div>
    </div>

    <!-- 评价统计概览 -->
    <div class="statistics-section">
      <ReviewStatistics
        :hotel-id="hotelId"
        ref="statisticsRef"
      />
    </div>

    <!-- 评价列表和筛选器 -->
    <div class="reviews-section">
      <div class="reviews-container">
        <!-- 筛选和排序控制 -->
        <div class="reviews-controls">
          <div class="controls-left">
            <div class="results-count">
              共找到 {{ totalReviews }} 条评价
            </div>
          </div>
          <div class="controls-right">
            <ReviewFilter
              v-model="filters"
              @filter-change="handleFilterChange"
            />
            <SortSelector
              v-model="sortOptions"
              @sort-change="handleSortChange"
            />
          </div>
        </div>

        <!-- 评价列表 -->
        <ReviewList
          ref="reviewListRef"
          :hotel-id="hotelId"
          :page-size="pageSize"
          :show-pagination="showPagination"
          :show-load-more="showLoadMore"
          @review-click="handleReviewClick"
          @total-change="handleTotalChange"
        />
      </div>

      <!-- 侧边栏 -->
      <div class="reviews-sidebar">
        <!-- 评分分布图表 -->
        <div class="sidebar-section">
          <RatingDistribution
            title="评分分布"
            :data="ratingDistribution"
            mode="bar"
            :show-total="true"
          />
        </div>

        <!-- 多维度评分 -->
        <div class="sidebar-section">
          <DimensionalRatings
            title="详细评分"
            :data="dimensionalData"
            mode="grid"
            :show-average="true"
          />
        </div>

        <!-- 快速筛选 -->
        <div class="sidebar-section">
          <h3 class="section-title">快速筛选</h3>
          <div class="quick-filters">
            <button
              :class="['filter-btn', { active: quickFilter === 'recent' }]"
              @click="applyQuickFilter('recent')"
            >
              最新评价
            </button>
            <button
              :class="['filter-btn', { active: quickFilter === 'high-rating' }]"
              @click="applyQuickFilter('high-rating')"
            >
              高评分
            </button>
            <button
              :class="['filter-btn', { active: quickFilter === 'with-images' }]"
              @click="applyQuickFilter('with-images')"
            >
              有图片
            </button>
          </div>
        </div>

        <!-- 带图片评价预览 -->
        <div class="sidebar-section" v-if="recentImageReviews.length > 0">
          <h3 class="section-title">图片评价</h3>
          <div class="image-reviews-preview">
            <div
              v-for="review in recentImageReviews"
              :key="review.id"
              class="image-review-item"
              @click="handleReviewClick(review)"
            >
              <img
                v-if="review.images && review.images.length > 0"
                :src="review.images[0]"
                :alt="`评价图片`"
                class="review-thumbnail"
              />
              <div class="review-info">
                <div class="review-rating">
                  <RatingStars :rating="review.overallRating" size="small" readonly />
                  <span class="rating-score">{{ review.overallRating }}.0</span>
                </div>
                <div class="review-excerpt">
                  {{ truncateText(review.comment, 50) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 评价详情弹窗 -->
    <ReviewDetail
      v-model:visible="reviewDetailVisible"
      :review="selectedReview"
      @helpful="handleHelpful"
      @report="handleReport"
      @share="handleShare"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import ReviewList from '@/components/business/ReviewList.vue'
import ReviewStatistics from '@/components/business/ReviewStatistics.vue'
import ReviewFilter from '@/components/business/ReviewFilter.vue'
import SortSelector from '@/components/business/SortSelector.vue'
import ReviewDetail from '@/components/business/ReviewDetail.vue'
import RatingDistribution from '@/components/business/RatingDistribution.vue'
import DimensionalRatings from '@/components/business/DimensionalRatings.vue'
import RatingStars from '@/components/business/RatingStars.vue'
import reviewService, { type ReviewResponse, type ReviewStatisticsResponse } from '@/services/reviewService'

// 路由
const router = useRouter()
const route = useRoute()

// 响应式数据
const hotelId = ref(Number(route.params.hotelId))
const hotelName = ref(route.query.hotelName as string || '')
const totalReviews = ref(0)
const reviewDetailVisible = ref(false)
const selectedReview = ref<ReviewResponse | null>(null)
const recentImageReviews = ref<ReviewResponse[]>([])

// 组件引用
const reviewListRef = ref()
const statisticsRef = ref()

// 筛选和排序
const filters = reactive({
  minRating: null as number | null,
  maxRating: null as number | null,
  hasImages: null as boolean | null,
  roomId: null as number | null
})

const sortOptions = reactive({
  sortBy: 'date' as 'date' | 'rating',
  sortOrder: 'desc' as 'asc' | 'desc'
})

const quickFilter = ref<string | null>(null)

// 分页设置
const pageSize = ref(10)
const showPagination = ref(true)
const showLoadMore = ref(false)

// 统计数据
const ratingDistribution = ref({
  rating5: 0,
  rating4: 0,
  rating3: 0,
  rating2: 0,
  rating1: 0
})

const dimensionalData = ref({
  cleanlinessRating: 0,
  serviceRating: 0,
  facilitiesRating: 0,
  locationRating: 0,
  overallRating: 0
})

// 计算属性
const layoutMode = computed(() => {
  return window.innerWidth > 1024 ? 'desktop' : 'mobile'
})

// 方法
const goBack = () => {
  router.push(`/hotels/${hotelId.value}`)
}

const goToWriteReview = () => {
  router.push(`/reviews/submit?hotelId=${hotelId.value}`)
}

const handleFilterChange = () => {
  quickFilter.value = null
  reviewListRef.value?.refresh()
}

const handleSortChange = () => {
  reviewListRef.value?.refresh()
}

const handleTotalChange = (total: number) => {
  totalReviews.value = total
}

const handleReviewClick = (review: ReviewResponse) => {
  selectedReview.value = review
  reviewDetailVisible.value = true
}

const handleHelpful = (reviewId: number) => {
  console.log('评价有帮助:', reviewId)
}

const handleReport = (reviewId: number) => {
  console.log('举报评价:', reviewId)
}

const handleShare = (review: ReviewResponse) => {
  const shareUrl = `${window.location.origin}/reviews/${review.id}`

  if (navigator.clipboard) {
    navigator.clipboard.writeText(shareUrl).then(() => {
      ElMessage.success('分享链接已复制到剪贴板')
    })
  }
}

const truncateText = (text: string, maxLength: number): string => {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

const applyQuickFilter = (filter: string) => {
  quickFilter.value = quickFilter.value === filter ? null : filter

  switch (filter) {
    case 'recent':
      sortOptions.sortBy = 'date'
      sortOptions.sortOrder = 'desc'
      Object.assign(filters, {
        minRating: null,
        maxRating: null,
        hasImages: null,
        roomId: null
      })
      break
    case 'high-rating':
      Object.assign(filters, {
        minRating: 4,
        maxRating: 5,
        hasImages: null,
        roomId: null
      })
      break
    case 'with-images':
      Object.assign(filters, {
        minRating: null,
        maxRating: null,
        hasImages: true,
        roomId: null
      })
      break
  }

  reviewListRef.value?.refresh()
}

const loadRecentImageReviews = async () => {
  try {
    const response = await reviewService.getReviewsWithImages(hotelId.value, 6)
    if (response.success) {
      recentImageReviews.value = response.data
    }
  } catch (error) {
    console.error('加载图片评价失败:', error)
  }
}

const loadStatistics = async () => {
  try {
    const response = await reviewService.getHotelStatistics(hotelId.value)
    if (response.success) {
      const data = response.data

      // 更新评分分布
      ratingDistribution.value = {
        rating5: data.ratingDistribution.rating5,
        rating4: data.ratingDistribution.rating4,
        rating3: data.ratingDistribution.rating3,
        rating2: data.ratingDistribution.rating2,
        rating1: data.ratingDistribution.rating1
      }

      // 更新多维度评分
      dimensionalData.value = {
        cleanlinessRating: data.cleanlinessRating,
        serviceRating: data.serviceRating,
        facilitiesRating: data.facilitiesRating,
        locationRating: data.locationRating,
        overallRating: data.overallRating
      }
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadData = async () => {
  await Promise.all([
    loadStatistics(),
    loadRecentImageReviews()
  ])
}

// 生命周期
onMounted(() => {
  if (!hotelId.value) {
    ElMessage.error('酒店ID不能为空')
    router.push('/hotels')
    return
  }

  loadData()
})
</script>

<style scoped>
.hotel-reviews {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
  background: white;
  border-bottom: 1px solid #eee;
  padding: 24px 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.back-btn,
.write-review-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.back-btn:hover {
  border-color: #007bff;
  color: #007bff;
}

.write-review-btn {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.write-review-btn:hover {
  background: #0056b3;
  border-color: #0056b3;
}

.statistics-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.reviews-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 24px;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
}

.reviews-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reviews-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #eee;
}

.controls-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.results-count {
  font-size: 14px;
  color: #666;
}

.controls-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.reviews-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sidebar-section {
  background: white;
  border-radius: 8px;
  border: 1px solid #eee;
  padding: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
}

.quick-filters {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-btn {
  padding: 10px 16px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 6px;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  text-align: left;
  transition: all 0.3s;
}

.filter-btn:hover {
  border-color: #007bff;
  color: #007bff;
}

.filter-btn.active {
  background: #007bff;
  border-color: #007bff;
  color: white;
}

.image-reviews-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.image-review-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.image-review-item:hover {
  background: #e9ecef;
}

.review-thumbnail {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.review-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.review-rating {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rating-score {
  font-size: 12px;
  font-weight: 600;
  color: #ff9800;
}

.review-excerpt {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .reviews-section {
    grid-template-columns: 1fr;
  }

  .reviews-sidebar {
    order: -1;
  }

  .sidebar-section {
    padding: 16px;
  }

  .quick-filters {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .filter-btn {
    flex: 1;
    min-width: 100px;
    text-align: center;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .page-title {
    font-size: 24px;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .reviews-controls {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .controls-right {
    width: 100%;
    justify-content: space-between;
  }

  .image-reviews-preview {
    gap: 8px;
  }

  .image-review-item {
    padding: 8px;
  }

  .review-thumbnail {
    width: 50px;
    height: 50px;
  }
}
</style>