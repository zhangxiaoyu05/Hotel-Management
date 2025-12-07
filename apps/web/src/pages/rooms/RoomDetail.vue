<template>
  <div v-if="room" class="room-detail-modal">
    <div class="modal-overlay" @click="$emit('close')"></div>

    <div class="modal-content">
      <!-- 关闭按钮 -->
      <button type="button" class="close-button" @click="$emit('close')">
        <i class="bi bi-x-lg"></i>
      </button>

      <!-- 房间图片轮播 -->
      <div class="room-gallery">
        <div v-if="room.images && room.images.length > 0" class="gallery-carousel">
          <div
            v-for="(image, index) in room.images"
            :key="index"
            class="gallery-item"
            :class="{ active: currentImageIndex === index }"
          >
            <img
              :src="image"
              :alt="`${room.roomNumber} 图片${index + 1}`"
              @error="handleImageError"
            />
          </div>

          <!-- 轮播控制 -->
          <button
            v-if="room.images.length > 1"
            type="button"
            class="gallery-control prev"
            @click="prevImage"
          >
            <i class="bi bi-chevron-left"></i>
          </button>
          <button
            v-if="room.images.length > 1"
            type="button"
            class="gallery-control next"
            @click="nextImage"
          >
            <i class="bi bi-chevron-right"></i>
          </button>

          <!-- 轮播指示器 -->
          <div v-if="room.images.length > 1" class="gallery-indicators">
            <button
              v-for="(image, index) in room.images"
              :key="index"
              type="button"
              class="indicator"
              :class="{ active: currentImageIndex === index }"
              @click="currentImageIndex = index"
            ></button>
          </div>
        </div>

        <!-- 默认图片 -->
        <div v-else class="default-gallery">
          <i class="bi bi-image display-1 text-muted"></i>
          <p class="text-muted">暂无图片</p>
        </div>
      </div>

      <!-- 房间信息 -->
      <div class="room-info">
        <div class="room-header">
          <div>
            <h2 class="room-number">{{ room.roomNumber }}</h2>
            <p class="room-type">{{ room.roomTypeName }}</p>
          </div>
          <div class="room-price-info">
            <div class="price-total">
              <span class="price-label">总价</span>
              <span class="price-value">¥{{ formatPrice(room.totalPrice || room.price) }}</span>
            </div>
            <div class="price-daily">
              <span class="price-unit">¥{{ formatPrice(room.averagePricePerNight || room.price) }}/晚</span>
            </div>
          </div>
        </div>

        <!-- 住宿信息 -->
        <div class="stay-info">
          <div class="info-item">
            <i class="bi bi-calendar-check"></i>
            <span>入住：{{ formatDate(checkInDate) }}</span>
          </div>
          <div class="info-item">
            <i class="bi bi-calendar-x"></i>
            <span>退房：{{ formatDate(checkOutDate) }}</span>
          </div>
          <div class="info-item">
            <i class="bi bi-people"></i>
            <span>{{ guestCount }} 位客人</span>
          </div>
          <div class="info-item">
            <i class="bi bi-moon"></i>
            <span>{{ nights }} 晚</span>
          </div>
        </div>

        <!-- 房间详情 -->
        <div class="room-details">
          <h3>房间详情</h3>
          <div class="details-grid">
            <div class="detail-item">
              <i class="bi bi-layers"></i>
              <div>
                <span class="detail-label">楼层</span>
                <span class="detail-value">{{ room.floor }} 楼</span>
              </div>
            </div>
            <div class="detail-item">
              <i class="bi bi-rulers"></i>
              <div>
                <span class="detail-label">面积</span>
                <span class="detail-value">{{ room.area }} ㎡</span>
              </div>
            </div>
            <div class="detail-item">
              <i class="bi bi-people"></i>
              <div>
                <span class="detail-label">容量</span>
                <span class="detail-value">最多 {{ room.roomTypeCapacity }} 人</span>
              </div>
            </div>
            <div class="detail-item">
              <i class="bi bi-check-circle"></i>
              <div>
                <span class="detail-label">状态</span>
                <span class="detail-value status-available">可预订</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 房间设施 -->
        <div v-if="room.roomTypeFacilities && room.roomTypeFacilities.length > 0" class="room-facilities">
          <h3>房间设施</h3>
          <div class="facilities-grid">
            <div
              v-for="facility in room.roomTypeFacilities"
              :key="facility"
              class="facility-item"
            >
              <i class="bi bi-check-circle-fill text-success"></i>
              <span>{{ facility }}</span>
            </div>
          </div>
        </div>

        <!-- 酒店信息 -->
        <div class="hotel-info">
          <h3>酒店信息</h3>
          <div class="hotel-details">
            <div class="hotel-name">
              <i class="bi bi-building"></i>
              {{ room.hotelName }}
            </div>
            <div class="hotel-address">
              <i class="bi bi-geo-alt"></i>
              {{ room.hotelAddress }}
            </div>
            <div class="hotel-phone">
              <i class="bi bi-telephone"></i>
              {{ room.hotelPhone }}
            </div>
            <div v-if="room.hotelDescription" class="hotel-description">
              <p>{{ room.hotelDescription }}</p>
            </div>
          </div>

          <!-- 酒店设施 -->
          <div v-if="room.hotelFacilities && room.hotelFacilities.length > 0" class="hotel-facilities">
            <h4>酒店设施</h4>
            <div class="facilities-grid">
              <div
                v-for="facility in room.hotelFacilities"
                :key="facility"
                class="facility-item"
              >
                <i class="bi bi-check-circle-fill text-success"></i>
                <span>{{ facility }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 预订须知 -->
        <div class="booking-info">
          <h3>预订须知</h3>
          <div class="info-list">
            <div class="info-item">
              <i class="bi bi-clock text-warning"></i>
              <span>入住时间：14:00 后</span>
            </div>
            <div class="info-item">
              <i class="bi bi-clock text-info"></i>
              <span>退房时间：12:00 前</span>
            </div>
            <div class="info-item">
              <i class="bi bi-credit-card text-primary"></i>
              <span>预订时需提供有效的信用卡信息</span>
            </div>
            <div class="info-item">
              <i class="bi bi-x-circle text-danger"></i>
              <span>免费取消：入住前24小时可免费取消</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 房间可用性日历 -->
      <RoomAvailabilityCalendar
        :room-id="room.id"
        :initial-start-date="checkInDate"
        :initial-end-date="checkOutDate"
        @date-selected="handleDateSelected"
        class="mt-4"
      />

      <!-- 预订按钮 -->
      <div class="booking-actions">
        <button
          type="button"
          class="btn btn-secondary"
          @click="$emit('close')"
        >
          返回搜索
        </button>
        <button
          type="button"
          class="btn btn-primary"
          @click="handleBookNow"
        >
          <i class="bi bi-calendar-check"></i>
          立即预订
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import RoomAvailabilityCalendar from '@/components/business/RoomAvailabilityCalendar.vue'

export default {
  name: 'RoomDetail',
  props: {
    room: {
      type: Object,
      required: true
    },
    checkInDate: {
      type: String,
      required: true
    },
    checkOutDate: {
      type: String,
      required: true
    },
    guestCount: {
      type: Number,
      required: true
    }
  },
  emits: ['close', 'book-now'],
  setup(props, { emit }) {
    const router = useRouter()
    const currentImageIndex = ref(0)

    // 计算住宿天数
    const nights = computed(() => {
      const checkIn = new Date(props.checkInDate)
      const checkOut = new Date(props.checkOutDate)
      const diffTime = Math.abs(checkOut - checkIn)
      return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
    })

    const prevImage = () => {
      if (currentImageIndex.value > 0) {
        currentImageIndex.value--
      } else {
        currentImageIndex.value = (props.room.images?.length || 1) - 1
      }
    }

    const nextImage = () => {
      if (currentImageIndex.value < (props.room.images?.length || 1) - 1) {
        currentImageIndex.value++
      } else {
        currentImageIndex.value = 0
      }
    }

    const formatPrice = (price) => {
      if (!price) return '0'
      return parseFloat(price).toFixed(2)
    }

    const formatDate = (dateString) => {
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    }

    const handleImageError = (event) => {
      event.target.src = '/images/room-placeholder.jpg'
    }

    const handleBookNow = () => {
      emit('book-now', props.room)

      // 导航到预订页面
      router.push({
        name: 'Booking',
        query: {
          roomId: props.room.id,
          checkInDate: props.checkInDate,
          checkOutDate: props.checkOutDate,
          guestCount: props.guestCount
        }
      })
    }

    const handleDateSelected = ({ start, end }) => {
      // 可以在这里处理日期选择的逻辑
      // 例如更新查询参数或重新计算价格
      console.log('选择的日期范围:', { start, end })

      // 触发重新搜索以获取新价格
      // 这里可以添加与父组件的通信逻辑
    }

    // 键盘事件处理
    const handleKeydown = (event) => {
      if (event.key === 'Escape') {
        emit('close')
      } else if (event.key === 'ArrowLeft') {
        prevImage()
      } else if (event.key === 'ArrowRight') {
        nextImage()
      }
    }

    onMounted(() => {
      document.addEventListener('keydown', handleKeydown)
      // 防止背景滚动
      document.body.style.overflow = 'hidden'
    })

    onUnmounted(() => {
      document.removeEventListener('keydown', handleKeydown)
      document.body.style.overflow = ''
    })

    return {
      currentImageIndex,
      nights,
      prevImage,
      nextImage,
      formatPrice,
      formatDate,
      handleImageError,
      handleBookNow,
      handleDateSelected
    }
  }
}
</script>

<style scoped>
.room-detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
}

.modal-content {
  position: relative;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  background: white;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
}

.close-button {
  position: absolute;
  top: 1rem;
  right: 1rem;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.3s ease;
}

.close-button:hover {
  background: white;
  transform: scale(1.1);
}

.room-gallery {
  position: relative;
  height: 400px;
  background: #f8f9fa;
}

.gallery-carousel {
  position: relative;
  width: 100%;
  height: 100%;
}

.gallery-item {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.5s ease;
}

.gallery-item.active {
  opacity: 1;
}

.gallery-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery-control {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  border-radius: 50%;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.3s ease;
  z-index: 5;
}

.gallery-control:hover {
  background: rgba(0, 0, 0, 0.7);
}

.gallery-control.prev {
  left: 1rem;
}

.gallery-control.next {
  right: 1rem;
}

.gallery-indicators {
  position: absolute;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 5;
}

.indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background 0.3s ease;
}

.indicator.active {
  background: white;
}

.default-gallery {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}

.room-info {
  padding: 2rem;
  overflow-y: auto;
  flex: 1;
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.room-number {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  font-weight: 700;
  color: #333;
}

.room-type {
  color: #6c757d;
  font-size: 1.1rem;
  margin: 0;
}

.room-price-info {
  text-align: right;
  white-space: nowrap;
}

.price-total {
  margin-bottom: 0.25rem;
}

.price-label {
  display: block;
  font-size: 0.875rem;
  color: #6c757d;
}

.price-value {
  display: block;
  font-size: 2rem;
  font-weight: 700;
  color: #007bff;
}

.price-daily .price-unit {
  font-size: 1rem;
  color: #6c757d;
}

.stay-info {
  display: flex;
  gap: 2rem;
  margin-bottom: 2rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.stay-info .info-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #495057;
}

.stay-info .info-item i {
  color: #007bff;
}

.room-details,
.room-facilities,
.hotel-info,
.booking-info {
  margin-bottom: 2rem;
}

.room-details h3,
.room-facilities h3,
.hotel-info h3,
.booking-info h3 {
  margin: 0 0 1rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
}

.details-grid,
.facilities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.detail-item i {
  color: #007bff;
  font-size: 1.25rem;
}

.detail-item div {
  display: flex;
  flex-direction: column;
}

.detail-label {
  font-size: 0.875rem;
  color: #6c757d;
}

.detail-value {
  font-weight: 500;
  color: #333;
}

.status-available {
  color: #28a745;
}

.facility-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background: #f8f9fa;
  border-radius: 6px;
}

.hotel-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.hotel-address,
.hotel-phone {
  color: #6c757d;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.hotel-description {
  margin-top: 1rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.hotel-description p {
  margin: 0;
  color: #495057;
  line-height: 1.6;
}

.hotel-facilities h4 {
  margin: 1.5rem 0 1rem 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.info-list .info-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.booking-actions {
  padding: 1.5rem 2rem;
  border-top: 1px solid #e9ecef;
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.booking-actions .btn {
  min-width: 120px;
}

@media (max-width: 768px) {
  .modal-content {
    width: 95%;
    max-height: 95vh;
  }

  .room-gallery {
    height: 250px;
  }

  .room-info {
    padding: 1rem;
  }

  .room-header {
    flex-direction: column;
    gap: 1rem;
  }

  .room-price-info {
    text-align: left;
  }

  .stay-info {
    flex-direction: column;
    gap: 0.75rem;
  }

  .details-grid,
  .facilities-grid {
    grid-template-columns: 1fr;
  }

  .booking-actions {
    padding: 1rem;
    flex-direction: column;
  }

  .booking-actions .btn {
    width: 100%;
  }
}
</style>