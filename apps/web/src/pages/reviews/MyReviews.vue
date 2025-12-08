<template>
  <div class="my-reviews-page">
    <div class="page-header">
      <h1>我的评价</h1>
      <p class="page-subtitle">查看您提交的所有评价</p>
    </div>

    <div class="page-content">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <i class="fas fa-spinner fa-spin"></i>
        <p>加载评价列表中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading && reviews.length === 0" class="empty-container">
        <i class="fas fa-star"></i>
        <h3>暂无评价</h3>
        <p>您还没有提交任何评价</p>
        <router-link to="/orders" class="btn btn-primary">
          查看我的订单
        </router-link>
      </div>

      <!-- 评价列表 -->
      <div v-else class="reviews-list">
        <div
          v-for="review in reviews"
          :key="review.id"
          class="review-card"
        >
          <div class="review-header">
            <div class="hotel-info">
              <h3>{{ review.hotelName }}</h3>
              <div class="review-rating">
                <i class="fas fa-star"></i>
                <span>{{ review.overallRating }}</span>
              </div>
            </div>
            <div class="review-meta">
              <span class="review-date">{{ formatDateTime(review.createdAt) }}</span>
              <span class="review-status" :class="review.status.toLowerCase()">
                {{ getStatusText(review.status) }}
              </span>
            </div>
          </div>

          <div class="review-content">
            <div class="rating-breakdown">
              <div class="rating-item">
                <span>清洁度</span>
                <div class="stars">
                  <i
                    v-for="star in 5"
                    :key="star"
                    class="fas fa-star"
                    :class="{ active: star <= review.cleanlinessRating }"
                  ></i>
                </div>
              </div>
              <div class="rating-item">
                <span>服务态度</span>
                <div class="stars">
                  <i
                    v-for="star in 5"
                    :key="star"
                    class="fas fa-star"
                    :class="{ active: star <= review.serviceRating }"
                  ></i>
                </div>
              </div>
              <div class="rating-item">
                <span>设施设备</span>
                <div class="stars">
                  <i
                    v-for="star in 5"
                    :key="star"
                    class="fas fa-star"
                    :class="{ active: star <= review.facilitiesRating }"
                  ></i>
                </div>
              </div>
              <div class="rating-item">
                <span>地理位置</span>
                <div class="stars">
                  <i
                    v-for="star in 5"
                    :key="star"
                    class="fas fa-star"
                    :class="{ active: star <= review.locationRating }"
                  ></i>
                </div>
              </div>
            </div>

            <div class="review-comment">
              <p>{{ review.comment }}</p>
            </div>

            <div v-if="review.images && review.images.length > 0" class="review-images">
              <img
                v-for="(image, index) in review.images.slice(0, 3)"
                :key="index"
                :src="image"
                :alt="`评价图片${index + 1}`"
                class="review-image"
                @click="previewImage(image)"
              />
              <div
                v-if="review.images.length > 3"
                class="more-images"
                @click="previewAllImages(review.images)"
              >
                +{{ review.images.length - 3 }}
              </div>
            </div>

            <div v-if="review.isAnonymous" class="anonymous-badge">
              <i class="fas fa-user-secret"></i>
              匿名评价
            </div>
          </div>

          <div class="review-actions">
            <router-link
              :to="`/hotels/${review.hotelId}`"
              class="btn btn-outline"
            >
              查看酒店
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览模态框 -->
    <div v-if="showImagePreview" class="image-preview-modal" @click="closeImagePreview">
      <div class="preview-content" @click.stop>
        <button class="close-btn" @click="closeImagePreview">
          <i class="fas fa-times"></i>
        </button>
        <img
          v-if="currentPreviewImage"
          :src="currentPreviewImage"
          alt="图片预览"
          class="preview-image"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import reviewService, { type ReviewResponse } from '@/services/reviewService'
import { formatDateTime } from '@/utils/date'

const loading = ref(false)
const reviews = ref<ReviewResponse[]>([])
const showImagePreview = ref(false)
const currentPreviewImage = ref('')

onMounted(() => {
  fetchReviews()
})

const fetchReviews = async () => {
  loading.value = true

  try {
    const response = await reviewService.getMyReviews()
    reviews.value = response.data
  } catch (err: any) {
    console.error('获取评价列表失败:', err)
    // 这里可以显示错误提示
  } finally {
    loading.value = false
  }
}

const getStatusText = (status: string): string => {
  switch (status) {
    case 'PENDING':
      return '待审核'
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已拒绝'
    default:
      return status
  }
}

const previewImage = (imageUrl: string) => {
  currentPreviewImage.value = imageUrl
  showImagePreview.value = true
}

const previewAllImages = (images: string[]) => {
  // 可以实现轮播预览功能
  if (images.length > 0) {
    previewImage(images[0])
  }
}

const closeImagePreview = () => {
  showImagePreview.value = false
  currentPreviewImage.value = ''
}
</script>

<style scoped>
.my-reviews-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.page-header {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  padding: 2rem 0;
  text-align: center;
}

.page-header h1 {
  font-size: 2rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.5rem;
}

.page-subtitle {
  color: #6b7280;
  font-size: 1rem;
}

.page-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.loading-container,
.empty-container {
  background: white;
  border-radius: 0.75rem;
  padding: 4rem 2rem;
  text-align: center;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.loading-container i,
.empty-container i {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.loading-container i {
  color: #3b82f6;
}

.empty-container i {
  color: #d1d5db;
}

.empty-container h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.5rem;
}

.empty-container p {
  color: #6b7280;
  margin-bottom: 2rem;
}

.reviews-list {
  display: grid;
  gap: 1.5rem;
}

.review-card {
  background: white;
  border-radius: 0.75rem;
  padding: 1.5rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.hotel-info {
  flex: 1;
}

.hotel-info h3 {
  font-size: 1.125rem;
  font-weight: 500;
  color: #111827;
  margin-bottom: 0.5rem;
}

.review-rating {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: #f59e0b;
  font-weight: 500;
}

.review-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.5rem;
}

.review-date {
  color: #6b7280;
  font-size: 0.875rem;
}

.review-status {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
}

.review-status.pending {
  background: #fef3c7;
  color: #92400e;
}

.review-status.approved {
  background: #dcfce7;
  color: #166534;
}

.review-status.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.rating-breakdown {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.rating-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.875rem;
}

.stars {
  display: flex;
  gap: 0.125rem;
}

.stars i {
  color: #d1d5db;
  font-size: 0.75rem;
}

.stars i.active {
  color: #f59e0b;
}

.review-comment {
  margin-bottom: 1rem;
}

.review-comment p {
  color: #374151;
  line-height: 1.6;
}

.review-images {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  position: relative;
}

.review-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.review-image:hover {
  transform: scale(1.05);
}

.more-images {
  width: 80px;
  height: 80px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
}

.anonymous-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #f3f4f6;
  color: #6b7280;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  margin-bottom: 1rem;
}

.review-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-weight: 500;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  font-size: 0.875rem;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-outline {
  background: transparent;
  color: #3b82f6;
  border: 1px solid #3b82f6;
}

.btn-outline:hover {
  background: #3b82f6;
  color: white;
}

.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.preview-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.close-btn {
  position: absolute;
  top: -40px;
  right: 0;
  background: none;
  border: none;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0.5rem;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .review-header {
    flex-direction: column;
    gap: 1rem;
  }

  .review-meta {
    align-items: flex-start;
  }

  .rating-breakdown {
    grid-template-columns: 1fr;
  }

  .review-images {
    flex-wrap: wrap;
  }

  .review-image,
  .more-images {
    width: 60px;
    height: 60px;
  }
}
</style>