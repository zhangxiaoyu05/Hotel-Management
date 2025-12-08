<template>
  <div class="review-card" @click="$emit('click', review)">
    <!-- 用户信息 -->
    <div class="review-header">
      <div class="user-info">
        <div class="user-avatar">
          <img
            v-if="!review.isAnonymous && review.userId"
            :src="getUserAvatar(review.userId)"
            :alt="review.isAnonymous ? '匿名用户' : `用户${review.userId}`"
            @error="handleAvatarError"
          />
          <span v-else class="anonymous-avatar">
            {{ review.isAnonymous ? '匿' : 'U' }}
          </span>
        </div>
        <div class="user-details">
          <div class="user-name">
            {{ review.isAnonymous ? '匿名用户' : `用户${review.userId}` }}
          </div>
          <div class="review-date">
            {{ formatDate(review.createdAt) }}
          </div>
        </div>
      </div>

      <!-- 总体评分 -->
      <div class="overall-rating">
        <RatingStars :rating="review.overallRating" size="small" readonly />
        <span class="rating-number">{{ review.overallRating }}.0</span>
      </div>
    </div>

    <!-- 评价内容 -->
    <div class="review-content">
      <div class="comment">
        <div :class="{ 'comment-collapsed': isExpanded === false && isLongComment }">
          {{ review.comment }}
        </div>
        <button
          v-if="isLongComment"
          class="expand-btn"
          @click.stop="toggleExpanded"
        >
          {{ isExpanded ? '收起' : '展开' }}
        </button>
      </div>

      <!-- 评价图片 -->
      <div v-if="review.images && review.images.length > 0" class="review-images">
        <div class="image-grid">
          <div
            v-for="(image, index) in displayImages"
            :key="index"
            class="image-item"
            @click.stop="openImageGallery(index)"
          >
            <img :src="image" :alt="`评价图片${index + 1}`" />
            <div v-if="review.images.length > 3 && index === 2" class="more-images-overlay">
              <span>+{{ review.images.length - 3 }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 多维度评分 -->
      <div class="rating-breakdown">
        <div class="rating-item">
          <span class="rating-label">清洁度</span>
          <RatingStars :rating="review.cleanlinessRating" size="mini" readonly />
          <span class="rating-value">{{ review.cleanlinessRating }}.0</span>
        </div>
        <div class="rating-item">
          <span class="rating-label">服务</span>
          <RatingStars :rating="review.serviceRating" size="mini" readonly />
          <span class="rating-value">{{ review.serviceRating }}.0</span>
        </div>
        <div class="rating-item">
          <span class="rating-label">设施</span>
          <RatingStars :rating="review.facilitiesRating" size="mini" readonly />
          <span class="rating-value">{{ review.facilitiesRating }}.0</span>
        </div>
        <div class="rating-item">
          <span class="rating-label">位置</span>
          <RatingStars :rating="review.locationRating" size="mini" readonly />
          <span class="rating-value">{{ review.locationRating }}.0</span>
        </div>
      </div>
    </div>

    <!-- 评价操作 -->
    <div class="review-actions">
      <button class="action-btn helpful-btn" @click.stop="toggleHelpful">
        <span class="icon">👍</span>
        <span>有帮助 ({{ helpfulCount }})</span>
      </button>
      <button class="action-btn report-btn" @click.stop="reportReview">
        <span class="icon">🚩</span>
        <span>举报</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import RatingStars from './RatingStars.vue'
import type { ReviewResponse } from '@/services/reviewService'

interface Props {
  review: ReviewResponse
  showActions?: boolean
  maxImages?: number
}

interface Emits {
  (e: 'click', review: ReviewResponse): void
  (e: 'image-click', images: string[], index: number): void
  (e: 'helpful', reviewId: number): void
  (e: 'report', reviewId: number): void
}

const props = withDefaults(defineProps<Props>(), {
  showActions: true,
  maxImages: 3
})

const emit = defineEmits<Emits>()

// 响应式数据
const isExpanded = ref(false)
const helpfulCount = ref(Math.floor(Math.random() * 20) + 1) // 模拟数据

// 计算属性
const isLongComment = computed(() => {
  return props.review.comment && props.review.comment.length > 200
})

const displayImages = computed(() => {
  if (!props.review.images || props.review.images.length === 0) {
    return []
  }
  return props.review.images.slice(0, props.maxImages)
})

// 方法
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes === 0 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days === 1) {
    return '昨天'
  } else if (days < 30) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  }
}

const getUserAvatar = (userId: number) => {
  // 这里应该调用真实的用户头像API
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=${userId}`
}

const handleAvatarError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}

const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

const openImageGallery = (index: number) => {
  if (props.review.images) {
    emit('image-click', props.review.images, index)
  }
}

const toggleHelpful = () => {
  helpfulCount.value++
  emit('helpful', props.review.id)
  ElMessage.success('感谢您的反馈')
}

const reportReview = () => {
  emit('report', props.review.id)
  ElMessage.info('举报已提交，我们会尽快处理')
}
</script>

<style scoped>
.review-card {
  padding: 20px;
  background: white;
  border-radius: 8px;
  border: 1px solid #eee;
  transition: all 0.3s;
  cursor: pointer;
}

.review-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #007bff;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #666;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.anonymous-avatar {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e8f4f8;
  color: #007bff;
  font-size: 18px;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-weight: 500;
  color: #333;
}

.review-date {
  font-size: 12px;
  color: #666;
}

.overall-rating {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rating-number {
  font-weight: 600;
  color: #ff9800;
}

.review-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment {
  color: #333;
  line-height: 1.6;
  position: relative;
}

.comment-collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.expand-btn {
  color: #007bff;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  margin-top: 8px;
}

.expand-btn:hover {
  text-decoration: underline;
}

.review-images {
  margin-top: 8px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 8px;
  max-width: 320px;
}

.image-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.image-item:hover img {
  transform: scale(1.1);
}

.more-images-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 18px;
}

.rating-breakdown {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.rating-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rating-label {
  font-size: 12px;
  color: #666;
  min-width: 40px;
}

.rating-value {
  font-size: 12px;
  color: #ff9800;
  font-weight: 500;
}

.review-actions {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: none;
  border: 1px solid #ddd;
  border-radius: 4px;
  color: #666;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s;
}

.action-btn:hover {
  border-color: #007bff;
  color: #007bff;
  background: #f8f9ff;
}

.action-btn .icon {
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .review-card {
    padding: 16px;
  }

  .review-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .overall-rating {
    align-self: flex-end;
  }

  .rating-breakdown {
    grid-template-columns: repeat(2, 1fr);
  }

  .review-actions {
    justify-content: center;
  }

  .image-grid {
    max-width: 100%;
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>