<template>
  <div class="review-statistics">
    <div v-if="loading" class="statistics-loading">
      <div class="loading-skeleton">
        <div class="skeleton-header">
          <div class="skeleton-rating"></div>
          <div class="skeleton-count"></div>
        </div>
        <div class="skeleton-content">
          <div v-for="i in 5" :key="i" class="skeleton-bar">
            <div class="skeleton-label"></div>
            <div class="skeleton-progress">
              <div class="skeleton-fill"></div>
            </div>
            <div class="skeleton-value"></div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="statistics.totalReviews === 0" class="statistics-empty">
      <div class="empty-icon">📊</div>
      <p class="empty-text">暂无评价数据</p>
    </div>

    <div v-else class="statistics-content">
      <!-- 总体评分概览 -->
      <div class="rating-overview">
        <div class="overall-score">
          <div class="score-number">
            {{ statistics.overallRating.toFixed(1) }}
          </div>
          <RatingStars :rating="Math.round(statistics.overallRating)" size="large" readonly />
          <div class="score-label">总体评分</div>
        </div>

        <div class="rating-summary">
          <div class="total-reviews">
            <span class="count">{{ statistics.totalReviews }}</span>
            <span class="label">条评价</span>
          </div>
          <div class="rating-distribution-text">
            {{
              statistics.overallRating >= 4.5 ? '优秀' :
              statistics.overallRating >= 4.0 ? '良好' :
              statistics.overallRating >= 3.0 ? '一般' :
              statistics.overallRating >= 2.0 ? '较差' : '很差'
            }}
          </div>
        </div>

        <div class="image-count" v-if="statistics.reviewsWithImages > 0">
          <div class="image-stat">
            <span class="icon">📸</span>
            <span class="count">{{ statistics.reviewsWithImages }}</span>
            <span class="label">带图片评价</span>
          </div>
        </div>
      </div>

      <!-- 评分分布 -->
      <div class="rating-distribution">
        <h3 class="distribution-title">评分分布</h3>
        <div class="distribution-bars">
          <div
            v-for="rating in [5, 4, 3, 2, 1]"
            :key="rating"
            class="rating-bar"
          >
            <div class="bar-label">{{ rating }}星</div>
            <div class="bar-container">
              <div
                class="bar-fill"
                :style="{
                  width: `${getRatingPercentage(rating)}%`,
                  backgroundColor: getRatingColor(rating)
                }"
              ></div>
            </div>
            <div class="bar-value">
              {{ getRatingCount(rating) }}
            </div>
            <div class="bar-percentage">
              {{ getRatingPercentage(rating).toFixed(1) }}%
            </div>
          </div>
        </div>
      </div>

      <!-- 多维度评分 -->
      <div class="dimensional-ratings">
        <h3 class="dimensional-title">详细评分</h3>
        <div class="dimensional-grid">
          <div class="dimensional-item">
            <div class="dimensional-icon">🧹</div>
            <div class="dimensional-info">
              <div class="dimensional-label">清洁度</div>
              <RatingStars :rating="Math.round(statistics.cleanlinessRating)" size="small" readonly />
              <div class="dimensional-score">{{ statistics.cleanlinessRating.toFixed(1) }}</div>
            </div>
          </div>

          <div class="dimensional-item">
            <div class="dimensional-icon">🤝</div>
            <div class="dimensional-info">
              <div class="dimensional-label">服务态度</div>
              <RatingStars :rating="Math.round(statistics.serviceRating)" size="small" readonly />
              <div class="dimensional-score">{{ statistics.serviceRating.toFixed(1) }}</div>
            </div>
          </div>

          <div class="dimensional-item">
            <div class="dimensional-icon">🏨</div>
            <div class="dimensional-info">
              <div class="dimensional-label">设施设备</div>
              <RatingStars :rating="Math.round(statistics.facilitiesRating)" size="small" readonly />
              <div class="dimensional-score">{{ statistics.facilitiesRating.toFixed(1) }}</div>
            </div>
          </div>

          <div class="dimensional-item">
            <div class="dimensional-icon">📍</div>
            <div class="dimensional-info">
              <div class="dimensional-label">地理位置</div>
              <RatingStars :rating="Math.round(statistics.locationRating)" size="small" readonly />
              <div class="dimensional-score">{{ statistics.locationRating.toFixed(1) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 统计摘要 -->
      <div class="statistics-summary">
        <div class="summary-item">
          <span class="summary-label">平均评价长度</span>
          <span class="summary-value">{{ statistics.averageCommentLength }} 字</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">图片评价占比</span>
          <span class="summary-value">
            {{ ((statistics.reviewsWithImages / statistics.totalReviews) * 100).toFixed(1) }}%
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import RatingStars from './RatingStars.vue'
import type { ReviewStatisticsResponse } from '@/services/reviewService'
import reviewService from '@/services/reviewService'

interface Props {
  hotelId: number
  autoLoad?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  autoLoad: true
})

// 响应式数据
const loading = ref(false)
const statistics = ref<ReviewStatisticsResponse>({
  hotelId: 0,
  totalReviews: 0,
  overallRating: 0,
  cleanlinessRating: 0,
  serviceRating: 0,
  facilitiesRating: 0,
  locationRating: 0,
  ratingDistribution: {
    rating5: 0,
    rating4: 0,
    rating3: 0,
    rating2: 0,
    rating1: 0
  },
  reviewsWithImages: 0,
  averageCommentLength: 0
})

// 计算属性
const getRatingCount = (rating: number) => {
  const distribution = statistics.value.ratingDistribution
  switch (rating) {
    case 5: return distribution.rating5
    case 4: return distribution.rating4
    case 3: return distribution.rating3
    case 2: return distribution.rating2
    case 1: return distribution.rating1
    default: return 0
  }
}

const getRatingPercentage = (rating: number) => {
  if (statistics.value.totalReviews === 0) return 0
  return (getRatingCount(rating) / statistics.value.totalReviews) * 100
}

const getRatingColor = (rating: number) => {
  const colors = {
    5: '#22c55e', // 绿色
    4: '#84cc16', // 浅绿色
    3: '#eab308', // 黄色
    2: '#f97316', // 橙色
    1: '#ef4444'  // 红色
  }
  return colors[rating as keyof typeof colors] || '#6b7280'
}

// 方法
const loadStatistics = async () => {
  if (!props.hotelId) return

  loading.value = true
  try {
    const response = await reviewService.getHotelStatistics(props.hotelId)
    if (response.success) {
      statistics.value = response.data
    }
  } catch (error) {
    console.error('加载评价统计失败:', error)
  } finally {
    loading.value = false
  }
}

const refresh = () => {
  loadStatistics()
}

// 生命周期
onMounted(() => {
  if (props.autoLoad) {
    loadStatistics()
  }
})

// 暴露方法
defineExpose({
  refresh,
  loadStatistics
})
</script>

<style scoped>
.review-statistics {
  background: white;
  border-radius: 8px;
  border: 1px solid #eee;
  overflow: hidden;
}

.statistics-loading {
  padding: 24px;
}

.loading-skeleton {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.skeleton-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.skeleton-rating {
  width: 120px;
  height: 32px;
  background: #f0f0f0;
  border-radius: 4px;
}

.skeleton-count {
  width: 80px;
  height: 24px;
  background: #f0f0f0;
  border-radius: 4px;
}

.skeleton-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.skeleton-label {
  width: 24px;
  height: 16px;
  background: #f0f0f0;
  border-radius: 4px;
}

.skeleton-progress {
  flex: 1;
  height: 16px;
  background: #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.skeleton-fill {
  width: 60%;
  height: 100%;
  background: #e0e0e0;
}

.skeleton-value {
  width: 32px;
  height: 16px;
  background: #f0f0f0;
  border-radius: 4px;
}

.statistics-empty {
  padding: 48px 24px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  color: #666;
  font-size: 16px;
}

.statistics-content {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.rating-overview {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.overall-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.score-number {
  font-size: 48px;
  font-weight: 700;
  line-height: 1;
}

.score-label {
  font-size: 14px;
  opacity: 0.9;
}

.rating-summary {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.total-reviews {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.total-reviews .count {
  font-size: 24px;
  font-weight: 600;
}

.total-reviews .label {
  font-size: 14px;
  opacity: 0.9;
}

.rating-distribution-text {
  font-size: 18px;
  font-weight: 500;
  opacity: 0.95;
}

.image-count {
  display: flex;
  align-items: center;
}

.image-stat {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
}

.image-stat .icon {
  font-size: 16px;
}

.image-stat .count {
  font-weight: 600;
}

.image-stat .label {
  font-size: 12px;
  opacity: 0.9;
}

.rating-distribution,
.dimensional-ratings {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.distribution-title,
.dimensional-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.distribution-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rating-bar {
  display: grid;
  grid-template-columns: 40px 1fr 40px 60px;
  align-items: center;
  gap: 12px;
}

.bar-label {
  font-size: 14px;
  color: #666;
  text-align: right;
}

.bar-container {
  height: 20px;
  background: #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
}

.bar-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 1s ease-out;
}

.bar-value {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  text-align: center;
}

.bar-percentage {
  font-size: 12px;
  color: #666;
  text-align: right;
}

.dimensional-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.dimensional-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.dimensional-icon {
  font-size: 24px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 50%;
  flex-shrink: 0;
}

.dimensional-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dimensional-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.dimensional-score {
  font-size: 16px;
  font-weight: 600;
  color: #ff9800;
}

.statistics-summary {
  display: flex;
  gap: 32px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  text-align: center;
}

.summary-label {
  font-size: 12px;
  color: #666;
}

.summary-value {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .statistics-content {
    padding: 16px;
    gap: 24px;
  }

  .rating-overview {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 16px;
    padding: 20px;
  }

  .rating-bar {
    grid-template-columns: 30px 1fr 30px 50px;
    gap: 8px;
  }

  .dimensional-grid {
    grid-template-columns: 1fr;
  }

  .statistics-summary {
    flex-direction: column;
    gap: 16px;
  }

  .summary-item {
    flex-direction: row;
    justify-content: space-between;
    text-align: left;
  }
}
</style>