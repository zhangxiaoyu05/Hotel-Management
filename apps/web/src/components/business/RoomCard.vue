<template>
  <div class="room-card" @click="$emit('view-details', room)">
    <!-- 房间图片轮播 -->
    <div class="room-card__images">
      <div v-if="room.images && room.images.length > 0" class="image-carousel">
        <div
          v-for="(image, index) in room.images"
          :key="index"
          class="carousel-item"
          :class="{ active: currentImageIndex === index }"
        >
          <img
            :src="image"
            :alt="`${room.roomNumber} 图片${index + 1}`"
            @error="handleImageError"
          />
        </div>

        <!-- 轮播指示器 -->
        <div v-if="room.images.length > 1" class="carousel-indicators">
          <button
            v-for="(image, index) in room.images"
            :key="index"
            type="button"
            class="indicator"
            :class="{ active: currentImageIndex === index }"
            @click.stop="currentImageIndex = index"
          ></button>
        </div>

        <!-- 轮播控制按钮 -->
        <button
          v-if="room.images.length > 1"
          type="button"
          class="carousel-control prev"
          @click.stop="prevImage"
        >
          <i class="bi bi-chevron-left"></i>
        </button>
        <button
          v-if="room.images.length > 1"
          type="button"
          class="carousel-control next"
          @click.stop="nextImage"
        >
          <i class="bi bi-chevron-right"></i>
        </button>
      </div>

      <!-- 默认图片 -->
      <div v-else class="default-image">
        <i class="bi bi-image display-4 text-muted"></i>
        <p class="text-muted mb-0">暂无图片</p>
      </div>
    </div>

    <!-- 房间信息 -->
    <div class="room-card__content">
      <div class="room-card__header">
        <div class="room-title">
          <h3 class="room-number">{{ room.roomNumber }}</h3>
          <span class="room-type">{{ room.roomTypeName }}</span>
        </div>
        <div class="room-price">
          <span class="price-label">总价</span>
          <span class="price-value">¥{{ formatPrice(room.totalPrice || room.price) }}</span>
          <span class="price-unit">{{ room.averagePricePerNight ? `/晚 ¥${formatPrice(room.averagePricePerNight)}` : '/晚' }}</span>
        </div>
      </div>

      <!-- 酒店信息 -->
      <div class="hotel-info">
        <div class="hotel-name">
          <i class="bi bi-building"></i>
          {{ room.hotelName }}
        </div>
        <div class="hotel-address">
          <i class="bi bi-geo-alt"></i>
          {{ room.hotelAddress }}
        </div>
      </div>

      <!-- 房间详情 -->
      <div class="room-details">
        <div class="detail-item">
          <i class="bi bi-people"></i>
          <span>最多 {{ room.roomTypeCapacity }} 人</span>
        </div>
        <div class="detail-item">
          <i class="bi bi-layers"></i>
          <span>{{ room.floor }} 楼</span>
        </div>
        <div class="detail-item">
          <i class="bi bi-rulers"></i>
          <span>{{ room.area }} ㎡</span>
        </div>
      </div>

      <!-- 设施标签 -->
      <div v-if="room.roomTypeFacilities && room.roomTypeFacilities.length > 0" class="facilities">
        <span
          v-for="facility in room.roomTypeFacilities.slice(0, 4)"
          :key="facility"
          class="facility-tag"
        >
          {{ facility }}
        </span>
        <span
          v-if="room.roomTypeFacilities.length > 4"
          class="facility-more"
        >
          +{{ room.roomTypeFacilities.length - 4 }}
        </span>
      </div>

      <!-- 房间状态 -->
      <div class="room-status">
        <div class="status-badge available">
          <i class="bi bi-check-circle"></i>
          可预订
        </div>
        <div v-if="room.hasSpecialPrice" class="special-price-badge">
          <i class="bi bi-star"></i>
          特价
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="room-actions">
        <button
          type="button"
          class="btn btn-outline-primary btn-sm"
          @click.stop="$emit('view-details', room)"
        >
          <i class="bi bi-eye"></i>
          查看详情
        </button>
        <button
          type="button"
          class="btn btn-primary btn-sm"
          @click.stop="$emit('book-now', room)"
        >
          <i class="bi bi-calendar-check"></i>
          立即预订
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'

export default {
  name: 'RoomCard',
  props: {
    room: {
      type: Object,
      required: true
    }
  },
  emits: ['view-details', 'book-now'],
  setup() {
    const currentImageIndex = ref(0)

    const prevImage = () => {
      if (currentImageIndex.value > 0) {
        currentImageIndex.value--
      } else {
        currentImageIndex.value = (this.room.images?.length || 1) - 1
      }
    }

    const nextImage = () => {
      if (currentImageIndex.value < (this.room.images?.length || 1) - 1) {
        currentImageIndex.value++
      } else {
        currentImageIndex.value = 0
      }
    }

    const formatPrice = (price) => {
      if (!price) return '0'
      return parseFloat(price).toFixed(2)
    }

    const handleImageError = (event) => {
      event.target.src = '/images/room-placeholder.jpg'
    }

    // 自动轮播
    let autoPlayInterval = null

    const startAutoPlay = () => {
      if (this.room.images && this.room.images.length > 1) {
        autoPlayInterval = setInterval(() => {
          nextImage()
        }, 5000)
      }
    }

    const stopAutoPlay = () => {
      if (autoPlayInterval) {
        clearInterval(autoPlayInterval)
        autoPlayInterval = null
      }
    }

    return {
      currentImageIndex,
      prevImage,
      nextImage,
      formatPrice,
      handleImageError,
      startAutoPlay,
      stopAutoPlay
    }
  },
  mounted() {
    this.startAutoPlay()
  },
  beforeUnmount() {
    this.stopAutoPlay()
  }
}
</script>

<style scoped>
.room-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.room-card:hover {
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.room-card__images {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.image-carousel {
  position: relative;
  height: 100%;
  width: 100%;
}

.carousel-item {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.5s ease;
}

.carousel-item.active {
  opacity: 1;
}

.carousel-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-indicators {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
}

.indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background 0.3s ease;
}

.indicator.active {
  background: white;
}

.carousel-control {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.3s ease;
}

.carousel-control:hover {
  background: rgba(0, 0, 0, 0.7);
}

.carousel-control.prev {
  left: 10px;
}

.carousel-control.next {
  right: 10px;
}

.default-image {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.room-card__content {
  padding: 1.25rem;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.room-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.room-title {
  flex: 1;
}

.room-number {
  margin: 0 0 0.25rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
}

.room-type {
  display: inline-block;
  background: #e9ecef;
  color: #495057;
  padding: 0.25rem 0.75rem;
  border-radius: 16px;
  font-size: 0.875rem;
}

.room-price {
  text-align: right;
  white-space: nowrap;
}

.price-label {
  display: block;
  font-size: 0.75rem;
  color: #6c757d;
  margin-bottom: 0.25rem;
}

.price-value {
  display: block;
  font-size: 1.5rem;
  font-weight: 700;
  color: #007bff;
}

.price-unit {
  font-size: 0.875rem;
  color: #6c757d;
}

.hotel-info {
  border-top: 1px solid #e9ecef;
  border-bottom: 1px solid #e9ecef;
  padding: 0.75rem 0;
}

.hotel-name {
  font-weight: 600;
  color: #333;
  margin-bottom: 0.25rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.hotel-address {
  font-size: 0.875rem;
  color: #6c757d;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.room-details {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.875rem;
  color: #495057;
}

.detail-item i {
  color: #6c757d;
}

.facilities {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.facility-tag {
  background: #e3f2fd;
  color: #1976d2;
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
}

.facility-more {
  background: #f5f5f5;
  color: #666;
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
}

.room-status {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.75rem;
  border-radius: 16px;
  font-size: 0.75rem;
  font-weight: 500;
}

.status-badge.available {
  background: #d4edda;
  color: #155724;
}

.special-price-badge {
  background: #fff3cd;
  color: #856404;
}

.room-actions {
  margin-top: auto;
  display: flex;
  gap: 0.5rem;
}

.room-actions .btn {
  flex: 1;
}

@media (max-width: 768px) {
  .room-card {
    margin-bottom: 1rem;
  }

  .room-card__header {
    flex-direction: column;
    gap: 0.75rem;
  }

  .room-price {
    text-align: left;
  }

  .room-details {
    gap: 0.75rem;
  }

  .detail-item {
    font-size: 0.8rem;
  }

  .room-actions {
    flex-direction: column;
  }

  .room-actions .btn {
    width: 100%;
  }
}
</style>