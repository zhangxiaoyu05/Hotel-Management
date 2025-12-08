<template>
  <div v-if="visible" class="review-detail-overlay" @click="close">
    <div class="review-detail" @click.stop>
      <!-- 关闭按钮 -->
      <button class="close-btn" @click="close">
        <span class="close-icon">×</span>
      </button>

      <!-- 评价内容 -->
      <div class="review-detail-content">
        <!-- 用户信息和总体评分 -->
        <div class="review-header">
          <div class="user-section">
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
            <div class="user-info">
              <div class="user-name">
                {{ review.isAnonymous ? '匿名用户' : `用户${review.userId}` }}
              </div>
              <div class="review-meta">
                <span class="review-date">{{ formatDate(review.createdAt) }}</span>
                <span class="review-id">评价ID: {{ review.id }}</span>
              </div>
            </div>
          </div>

          <div class="rating-section">
            <div class="overall-rating">
              <RatingStars :rating="review.overallRating" size="large" readonly />
              <span class="rating-number">{{ review.overallRating }}.0</span>
            </div>
            <div class="rating-summary">
              {{
                review.overallRating === 5 ? '非常满意' :
                review.overallRating === 4 ? '满意' :
                review.overallRating === 3 ? '一般' :
                review.overallRating === 2 ? '不满意' : '非常不满意'
              }}
            </div>
          </div>
        </div>

        <!-- 评价内容 -->
        <div class="review-body">
          <div class="comment-section">
            <h3 class="section-title">评价内容</h3>
            <div class="comment-content">
              {{ review.comment }}
            </div>
          </div>

          <!-- 评价图片 -->
          <div v-if="review.images && review.images.length > 0" class="images-section">
            <h3 class="section-title">评价图片 ({{ review.images.length }}张)</h3>
            <div class="images-grid">
              <div
                v-for="(image, index) in review.images"
                :key="index"
                class="image-item"
                @click="openImageGallery(index)"
              >
                <img :src="image" :alt="`评价图片${index + 1}`" />
              </div>
            </div>
          </div>

          <!-- 多维度评分 -->
          <div class="rating-breakdown-section">
            <h3 class="section-title">详细评分</h3>
            <div class="rating-grid">
              <div class="rating-item">
                <div class="rating-info">
                  <span class="rating-label">清洁度</span>
                  <span class="rating-description">房间卫生程度</span>
                </div>
                <div class="rating-display">
                  <RatingStars :rating="review.cleanlinessRating" size="medium" readonly />
                  <span class="rating-value">{{ review.cleanlinessRating }}.0</span>
                </div>
              </div>

              <div class="rating-item">
                <div class="rating-info">
                  <span class="rating-label">服务态度</span>
                  <span class="rating-description">员工服务质量</span>
                </div>
                <div class="rating-display">
                  <RatingStars :rating="review.serviceRating" size="medium" readonly />
                  <span class="rating-value">{{ review.serviceRating }}.0</span>
                </div>
              </div>

              <div class="rating-item">
                <div class="rating-info">
                  <span class="rating-label">设施设备</span>
                  <span class="rating-description">酒店设施完善度</span>
                </div>
                <div class="rating-display">
                  <RatingStars :rating="review.facilitiesRating" size="medium" readonly />
                  <span class="rating-value">{{ review.facilitiesRating }}.0</span>
                </div>
              </div>

              <div class="rating-item">
                <div class="rating-info">
                  <span class="rating-label">地理位置</span>
                  <span class="rating-description">交通便利程度</span>
                </div>
                <div class="rating-display">
                  <RatingStars :rating="review.locationRating" size="medium" readonly />
                  <span class="rating-value">{{ review.locationRating }}.0</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 评价操作 -->
        <div class="review-actions">
          <button class="action-btn helpful-btn" @click="toggleHelpful">
            <span class="icon">👍</span>
            <span>有帮助 ({{ helpfulCount }})</span>
          </button>
          <button class="action-btn share-btn" @click="shareReview">
            <span class="icon">📤</span>
            <span>分享</span>
          </button>
          <button class="action-btn report-btn" @click="reportReview">
            <span class="icon">🚩</span>
            <span>举报</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 图片画廊 -->
    <ImageGallery
      v-model:visible="galleryVisible"
      :images="review.images || []"
      :initial-index="galleryInitialIndex"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import RatingStars from './RatingStars.vue'
import ImageGallery from './ImageGallery.vue'
import type { ReviewResponse } from '@/services/reviewService'

interface Props {
  visible: boolean
  review: ReviewResponse
}

interface Emits {
  (e: 'update:visible', visible: boolean): void
  (e: 'helpful', reviewId: number): void
  (e: 'report', reviewId: number): void
  (e: 'share', review: ReviewResponse): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const helpfulCount = ref(Math.floor(Math.random() * 50) + 10) // 模拟数据
const galleryVisible = ref(false)
const galleryInitialIndex = ref(0)

// 方法
const close = () => {
  emit('update:visible', false)
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getUserAvatar = (userId: number) => {
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=${userId}`
}

const handleAvatarError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}

const toggleHelpful = () => {
  helpfulCount.value++
  emit('helpful', props.review.id)
  ElMessage.success('感谢您的反馈')
}

const shareReview = () => {
  emit('share', props.review)

  // 复制分享链接到剪贴板
  const shareUrl = `${window.location.origin}/reviews/${props.review.id}`

  if (navigator.clipboard) {
    navigator.clipboard.writeText(shareUrl).then(() => {
      ElMessage.success('分享链接已复制到剪贴板')
    })
  } else {
    // 兼容性处理
    const textArea = document.createElement('textarea')
    textArea.value = shareUrl
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success('分享链接已复制到剪贴板')
  }
}

const reportReview = () => {
  emit('report', props.review.id)
  ElMessage.info('举报已提交，我们会尽快处理')
}

const openImageGallery = (index: number) => {
  galleryInitialIndex.value = index
  galleryVisible.value = true
}
</script>

<style scoped>
.review-detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.review-detail {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 800px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  width: 40px;
  height: 40px;
  background: rgba(0, 0, 0, 0.1);
  border: none;
  border-radius: 50%;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.2);
  color: #333;
}

.close-icon {
  font-size: 24px;
  line-height: 1;
}

.review-detail-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.user-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #666;
  flex-shrink: 0;
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
  font-size: 24px;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.review-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.review-date {
  font-size: 14px;
  color: #666;
}

.review-id {
  font-size: 12px;
  color: #999;
}

.rating-section {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.overall-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.rating-number {
  font-size: 24px;
  font-weight: 700;
  color: #ff9800;
}

.rating-summary {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.review-body {
  flex: 1;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 20px;
  background: #007bff;
  border-radius: 2px;
}

.comment-section {
  display: flex;
  flex-direction: column;
}

.comment-content {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
  font-size: 15px;
}

.images-section {
  display: flex;
  flex-direction: column;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.image-item {
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s;
}

.image-item:hover {
  transform: scale(1.05);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rating-breakdown-section {
  display: flex;
  flex-direction: column;
}

.rating-grid {
  display: grid;
  gap: 20px;
}

.rating-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.rating-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rating-label {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.rating-description {
  font-size: 12px;
  color: #666;
}

.rating-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-value {
  font-weight: 600;
  color: #ff9800;
  font-size: 16px;
}

.review-actions {
  display: flex;
  gap: 16px;
  padding: 24px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  justify-content: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 6px;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.action-btn:hover {
  border-color: #007bff;
  color: #007bff;
  background: #f8f9ff;
}

.action-btn .icon {
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .review-detail-overlay {
    padding: 0;
  }

  .review-detail {
    border-radius: 0;
    max-height: 100vh;
  }

  .review-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .rating-section {
    align-items: flex-start;
    text-align: left;
  }

  .rating-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .images-grid {
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
    gap: 8px;
  }

  .review-actions {
    flex-wrap: wrap;
  }
}
</style>