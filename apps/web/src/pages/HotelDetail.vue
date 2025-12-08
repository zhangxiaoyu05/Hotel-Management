<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElCard, ElRow, ElCol, ElButton, ElImage, ElRate, ElTag } from 'element-plus'

const route = useRoute()
const hotelId = route.params.id

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

onMounted(() => {
  // TODO: 根据hotelId加载酒店详情
  console.log('加载酒店详情:', hotelId)
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
                  <ElRate v-model="hotel.rating" disabled />
                  <span class="rating-text">{{ hotel.rating }} 分</span>
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
  gap: 10px;
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
}
</style>