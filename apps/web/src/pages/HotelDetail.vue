<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElCard, ElRow, ElCol, ElButton, ElImage, ElRate, ElTag, ElMessage } from 'element-plus'
import RatingStars from '@/components/business/RatingStars.vue'
import ReviewStatistics from '@/components/business/ReviewStatistics.vue'
import reviewService, { type ReviewResponse, type ReviewStatisticsResponse } from '@/services/reviewService'

const route = useRoute()
const router = useRouter()
const hotelId = route.params.id

// 评价相关数据
const reviewStatistics = ref<ReviewStatisticsResponse | null>(null)
const recentReviews = ref<ReviewResponse[]>([])
const loadingReviews = ref(false)

const hotel = ref({
  id: 1,
  name: '成都望江宾馆',
  address: '成都市武侯区望江路300号',
  phone: '028-88888888',
  description: '位于成都市中心，交通便利，设施齐全的五星级酒店。酒店拥有各类客房200余间，配备完善的商务设施和休闲娱乐场所，是商务出行和旅游度假的理想选择。',
  images: ['/api/placeholder/800/400', '/api/placeholder/400/300'],
  price: 288,
  rating: 4.5,
  facilities: ['免费WiFi', '停车场', '游泳池', '健身房', '会议室', '餐厅'],
  rooms: [
    {
      id: 101,
      type: '标准间',
      price: 288,
      capacity: 2,
      area: 30,
      image: '/api/placeholder/300/200'
    },
    {
      id: 102,
      type: '豪华间',
      price: 388,
      capacity: 2,
      area: 35,
      image: '/api/placeholder/300/200'
    }
  ]
})

const currentImageIndex = ref(0)

const nextImage = () => {
  currentImageIndex.value = (currentImageIndex.value + 1) % hotel.value.images.length
}

const prevImage = () => {
  currentImageIndex.value = currentImageIndex.value === 0 ? hotel.value.images.length - 1 : currentImageIndex.value - 1
}

const bookRoom = (roomId: number) => {
  console.log('预订房间:', roomId)
  // TODO: 实现预订功能
}

const goToReviews = () => {
  router.push(`/hotels/${hotelId}/reviews?hotelName=${encodeURIComponent(hotel.value.name)}`)
}

const loadReviewStatistics = async () => {
  try {
    const response = await reviewService.getHotelStatistics(Number(hotelId))
    if (response.success) {
      reviewStatistics.value = response.data
    }
  } catch (error) {
    console.error('加载评价统计失败:', error)
  }
}

const loadRecentReviews = async () => {
  if (loadingReviews.value) return

  loadingReviews.value = true
  try {
    const response = await reviewService.getRecentReviews(Number(hotelId), 3)
    if (response.success) {
      recentReviews.value = response.data
    }
  } catch (error) {
    console.error('加载最新评价失败:', error)
  } finally {
    loadingReviews.value = false
  }
}

const truncateText = (text: string, maxLength: number): string => {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

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

const handleAvatarError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}

onMounted(() => {
  // TODO: 根据hotelId加载酒店详情
  console.log('加载酒店详情:', hotelId)

  // 加载评价相关数据
  loadReviewStatistics()
  loadRecentReviews()
})
</script>

<template>
  <div class="hotel-detail">
    <div class="container">
      <!-- Hotel Images -->
      <div class="hotel-images">
        <ElCard>
          <div class="image-gallery">
            <div class="main-image">
              <ElImage
                :src="hotel.images[currentImageIndex]"
                :alt="hotel.name"
                fit="cover"
                class="gallery-image"
              />
              <div class="image-controls">
                <ElButton @click="prevImage" circle>
                  <i class="el-icon-arrow-left"></i>
                </ElButton>
                <ElButton @click="nextImage" circle>
                  <i class="el-icon-arrow-right"></i>
                </ElButton>
              </div>
            </div>
            <div class="image-thumbnails">
              <div
                v-for="(image, index) in hotel.images"
                :key="index"
                class="thumbnail"
                :class="{ active: currentImageIndex === index }"
                @click="currentImageIndex = index"
              >
                <ElImage :src="image" :alt="`${hotel.name} ${index + 1}`" fit="cover" />
              </div>
            </div>
          </div>
        </ElCard>
      </div>

      <!-- Hotel Info -->
      <div class="hotel-info">
        <ElRow :gutter="20">
          <ElCol :xs="24" :md="16">
            <ElCard>
              <div class="hotel-header">
                <h1>{{ hotel.name }}</h1>
                <div class="hotel-rating">
                  <RatingStars
                    :rating="reviewStatistics?.overallRating || hotel.rating"
                    size="large"
                    readonly
                  />
                  <div class="rating-info">
                    <span class="rating-score">
                      {{ (reviewStatistics?.overallRating || hotel.rating).toFixed(1) }}
                    </span>
                    <span class="rating-count" v-if="reviewStatistics?.totalReviews">
                      {{ reviewStatistics.totalReviews }}条评价
                    </span>
                  </div>
                </div>
              </div>

              <div class="hotel-details">
                <div class="detail-item">
                  <i class="el-icon-location"></i>
                  <span>{{ hotel.address }}</span>
                </div>
                <div class="detail-item">
                  <i class="el-icon-phone"></i>
                  <span>{{ hotel.phone }}</span>
                </div>
              </div>

              <div class="hotel-description">
                <h3>酒店介绍</h3>
                <p>{{ hotel.description }}</p>
              </div>

              <div class="hotel-facilities">
                <h3>设施服务</h3>
                <div class="facility-list">
                  <ElTag v-for="facility in hotel.facilities" :key="facility" class="facility-tag">
                    {{ facility }}
                  </ElTag>
                </div>
              </div>
            </ElCard>
          </ElCol>

          <ElCol :xs="24" :md="8">
            <ElCard class="booking-card">
              <div class="booking-info">
                <div class="price-info">
                  <span class="price-label">起价</span>
                  <div class="price-value">
                    <span class="price">¥{{ hotel.price }}</span>
                    <span class="price-unit">/晚</span>
                  </div>
                </div>
                <ElButton type="primary" size="large" style="width: 100%">
                  立即预订
                </ElButton>
              </div>
            </ElCard>
          </ElCol>
        </ElRow>
      </div>

      <!-- Reviews Preview -->
      <div class="reviews-preview" v-if="recentReviews.length > 0">
        <ElCard>
          <template #header>
            <div class="reviews-header">
              <h2>住客评价</h2>
              <ElButton type="primary" size="small" @click="goToReviews">
                查看全部评价
              </ElButton>
            </div>
          </template>

          <!-- 评价统计摘要 -->
          <div v-if="reviewStatistics" class="reviews-summary">
            <div class="summary-rating">
              <div class="summary-score">
                {{ reviewStatistics.overallRating.toFixed(1) }}
              </div>
              <div class="summary-stars">
                <RatingStars :rating="Math.round(reviewStatistics.overallRating)" size="small" readonly />
              </div>
              <div class="summary-text">
                {{ reviewStatistics.totalReviews }}条评价 · {{ reviewStatistics.reviewsWithImages }}条带图
              </div>
            </div>
            <div class="summary-dimensions">
              <div class="dimension-item">
                <span class="dimension-label">清洁度</span>
                <span class="dimension-score">{{ reviewStatistics.cleanlinessRating.toFixed(1) }}</span>
              </div>
              <div class="dimension-item">
                <span class="dimension-label">服务</span>
                <span class="dimension-score">{{ reviewStatistics.serviceRating.toFixed(1) }}</span>
              </div>
              <div class="dimension-item">
                <span class="dimension-label">设施</span>
                <span class="dimension-score">{{ reviewStatistics.facilitiesRating.toFixed(1) }}</span>
              </div>
              <div class="dimension-item">
                <span class="dimension-label">位置</span>
                <span class="dimension-score">{{ reviewStatistics.locationRating.toFixed(1) }}</span>
              </div>
            </div>
          </div>

          <!-- 最新评价列表 -->
          <div class="recent-reviews">
            <div
              v-for="review in recentReviews"
              :key="review.id"
              class="review-item"
              @click="goToReviews"
            >
              <div class="review-avatar">
                <img
                  v-if="!review.isAnonymous && review.userId"
                  :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${review.userId}`"
                  :alt="review.isAnonymous ? '匿名用户' : `用户${review.userId}`"
                  @error="handleAvatarError"
                />
                <span v-else class="anonymous-avatar">
                  {{ review.isAnonymous ? '匿' : 'U' }}
                </span>
              </div>
              <div class="review-content">
                <div class="review-header-info">
                  <span class="review-name">
                    {{ review.isAnonymous ? '匿名用户' : `用户${review.userId}` }}
                  </span>
                  <span class="review-date">{{ formatDate(review.createdAt) }}</span>
                </div>
                <div class="review-rating">
                  <RatingStars :rating="review.overallRating" size="small" readonly />
                </div>
                <div class="review-comment">
                  {{ truncateText(review.comment, 120) }}
                </div>
                <div class="review-images" v-if="review.images && review.images.length > 0">
                  <img
                    v-for="(image, index) in review.images.slice(0, 3)"
                    :key="index"
                    :src="image"
                    :alt="`评价图片${index + 1}`"
                    class="review-image"
                  />
                  <div v-if="review.images.length > 3" class="more-images">
                    +{{ review.images.length - 3 }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="loadingReviews" class="reviews-loading">
            <div class="loading-spinner"></div>
            <span>加载评价中...</span>
          </div>
        </ElCard>
      </div>

      <!-- Room Types -->
      <div class="room-types">
        <ElCard>
          <template #header>
            <h2>房型选择</h2>
          </template>

          <ElRow :gutter="20">
            <ElCol v-for="room in hotel.rooms" :key="room.id" :xs="24" :md="12">
              <div class="room-card">
                <ElRow :gutter="20">
                  <ElCol :span="8">
                    <ElImage
                      :src="room.image"
                      :alt="room.type"
                      fit="cover"
                      class="room-image"
                    />
                  </ElCol>
                  <ElCol :span="16">
                    <div class="room-info">
                      <h3>{{ room.type }}</h3>
                      <div class="room-details">
                        <span>容纳 {{ room.capacity }} 人</span>
                        <span>{{ room.area }}㎡</span>
                      </div>
                      <div class="room-price">
                        <span class="price">¥{{ room.price }}</span>
                        <span class="price-unit">/晚</span>
                      </div>
                      <ElButton type="primary" size="small" @click="bookRoom(room.id)">
                        预订
                      </ElButton>
                    </div>
                  </ElCol>
                </ElRow>
              </div>
            </ElCol>
          </ElRow>
        </ElCard>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hotel-detail {
  min-height: calc(100vh - 60px);
  padding: 20px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.hotel-images {
  margin-bottom: 30px;
}

.image-gallery {
  position: relative;
}

.main-image {
  position: relative;
  width: 100%;
  height: 400px;
  margin-bottom: 15px;
  border-radius: 8px;
  overflow: hidden;
}

.gallery-image {
  width: 100%;
  height: 100%;
}

.image-controls {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
  transform: translateY(-50%);
}

.image-thumbnails {
  display: flex;
  gap: 10px;
  overflow-x: auto;
}

.thumbnail {
  flex-shrink: 0;
  width: 80px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.3s;
}

.thumbnail.active {
  border-color: #409eff;
}

.hotel-info {
  margin-bottom: 30px;
}

.hotel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.hotel-header h1 {
  margin: 0;
  font-size: 2em;
  color: #303133;
}

.hotel-rating {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rating-score {
  font-size: 24px;
  font-weight: 600;
  color: #ff9800;
}

.rating-count {
  font-size: 14px;
  color: #909399;
}

.rating-text {
  color: #909399;
  font-size: 0.9em;
}

.hotel-details {
  margin-bottom: 25px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: #606266;
}

.hotel-description {
  margin-bottom: 25px;
}

.hotel-description h3 {
  margin: 0 0 15px 0;
  color: #303133;
}

.hotel-description p {
  line-height: 1.6;
  color: #606266;
}

.hotel-facilities h3 {
  margin: 0 0 15px 0;
  color: #303133;
}

.facility-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.facility-tag {
  margin: 0;
}

.booking-card {
  position: sticky;
  top: 80px;
}

.booking-info {
  text-align: center;
}

.price-info {
  margin-bottom: 20px;
}

.price-label {
  color: #909399;
  font-size: 0.9em;
}

.price-value {
  margin-top: 10px;
}

.price {
  font-size: 2em;
  font-weight: bold;
  color: #f56c6c;
}

.price-unit {
  color: #909399;
  margin-left: 5px;
}

.room-types h2 {
  margin: 0;
  color: #303133;
}

.room-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
  transition: box-shadow 0.3s;
}

.room-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.room-image {
  width: 100%;
  height: 120px;
  border-radius: 4px;
}

.room-info h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

.room-details {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
  color: #909399;
  font-size: 0.9em;
}

.room-price {
  margin-bottom: 15px;
}

.room-price .price {
  font-size: 1.5em;
  font-weight: bold;
  color: #f56c6c;
}

/* 评价预览样式 */
.reviews-preview {
  margin-bottom: 30px;
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reviews-header h2 {
  margin: 0;
  color: #303133;
}

.reviews-summary {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 24px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 24px;
}

.summary-rating {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.summary-score {
  font-size: 36px;
  font-weight: 700;
  color: #ff9800;
}

.summary-stars {
  display: flex;
}

.summary-text {
  font-size: 14px;
  color: #666;
  text-align: center;
}

.summary-dimensions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.dimension-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.dimension-label {
  font-size: 12px;
  color: #666;
}

.dimension-score {
  font-size: 14px;
  font-weight: 600;
  color: #ff9800;
}

.recent-reviews {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.review-item:hover {
  background: #e9ecef;
  transform: translateY(-2px);
}

.review-avatar {
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
  flex-shrink: 0;
}

.review-avatar img {
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

.review-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-name {
  font-weight: 500;
  color: #333;
}

.review-date {
  font-size: 12px;
  color: #666;
}

.review-rating {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-comment {
  color: #333;
  line-height: 1.6;
  font-size: 14px;
}

.review-images {
  display: flex;
  gap: 8px;
  align-items: center;
}

.review-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.3s;
}

.review-image:hover {
  transform: scale(1.05);
}

.more-images {
  width: 60px;
  height: 60px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.reviews-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
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

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .main-image {
    height: 250px;
  }

  .hotel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .booking-card {
    position: static;
    margin-top: 20px;
  }

  .room-card {
    padding: 10px;
  }

  .reviews-summary {
    flex-direction: column;
    gap: 20px;
    padding: 16px;
  }

  .summary-dimensions {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .review-item {
    padding: 16px;
  }

  .review-images {
    flex-wrap: wrap;
  }

  .review-image {
    width: 50px;
    height: 50px;
  }
}
</style>