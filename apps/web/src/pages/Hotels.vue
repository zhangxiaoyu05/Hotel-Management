<script setup lang="ts">
import { ref } from 'vue'
import { ElRow, ElCol, ElCard, ElInput, ElSelect, ElOption, ElButton, ElPagination } from 'element-plus'

const hotels = ref([
  {
    id: 1,
    name: '成都望江宾馆',
    address: '成都市武侯区望江路300号',
    phone: '028-88888888',
    description: '位于成都市中心，交通便利，设施齐全的五星级酒店',
    image: '/api/placeholder/300/200',
    price: 288,
    rating: 4.5,
    facilities: ['免费WiFi', '停车场', '游泳池', '健身房']
  },
  {
    id: 2,
    name: '天府酒店',
    address: '成都市高新区天府大道1000号',
    phone: '028-99999999',
    description: '现代化商务酒店，服务优质的商务人士首选',
    image: '/api/placeholder/300/200',
    price: 368,
    rating: 4.7,
    facilities: ['免费WiFi', '停车场', '会议室', '餐厅']
  },
  {
    id: 3,
    name: '锦江宾馆',
    address: '成都市锦江区人民南路一段66号',
    phone: '028-77777777',
    description: '历史悠久的知名酒店，环境优雅，服务周到',
    image: '/api/placeholder/300/200',
    price: 458,
    rating: 4.8,
    facilities: ['免费WiFi', '停车场', 'SPA', '酒吧']
  }
])

const searchKeyword = ref('')
const sortBy = ref('price')
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(30)

const filteredHotels = ref(hotels.value)

const handleSearch = () => {
  // TODO: 实际搜索逻辑
  console.log('搜索关键词:', searchKeyword.value)
}

const handleSort = () => {
  // TODO: 实际排序逻辑
  console.log('排序方式:', sortBy.value)
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  // TODO: 加载对应页面的数据
}

const viewHotelDetail = (hotelId: number) => {
  // TODO: 跳转到酒店详情页
  console.log('查看酒店详情:', hotelId)
}
</script>

<template>
  <div class="hotels-page">
    <div class="container">
      <!-- Search and Filter Section -->
      <div class="search-section">
        <ElRow :gutter="20" align="middle">
          <ElCol :xs="24" :sm="12" :md="8">
            <ElInput
              v-model="searchKeyword"
              placeholder="搜索酒店名称或地址"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <ElButton @click="handleSearch">搜索</ElButton>
              </template>
            </ElInput>
          </ElCol>
          <ElCol :xs="24" :sm="12" :md="4">
            <ElSelect v-model="sortBy" placeholder="排序方式" @change="handleSort">
              <ElOption label="价格从低到高" value="price" />
              <ElOption label="价格从高到低" value="price-desc" />
              <ElOption label="评分从高到低" value="rating" />
              <ElOption label="距离最近" value="distance" />
            </ElSelect>
          </ElCol>
        </ElRow>
      </div>

      <!-- Hotels Grid -->
      <div class="hotels-grid">
        <ElRow :gutter="20">
          <ElCol v-for="hotel in filteredHotels" :key="hotel.id" :xs="24" :sm="12" :lg="8">
            <ElCard class="hotel-card" :body-style="{ padding: '0px' }">
              <div class="hotel-image">
                <img :src="hotel.image" :alt="hotel.name" />
              </div>
              <div class="hotel-content">
                <div class="hotel-header">
                  <h3>{{ hotel.name }}</h3>
                  <div class="hotel-rating">
                    <span class="rating-score">{{ hotel.rating }}</span>
                    <span class="rating-text">分</span>
                  </div>
                </div>

                <div class="hotel-info">
                  <p class="hotel-address">
                    <i class="el-icon-location"></i>
                    {{ hotel.address }}
                  </p>
                  <p class="hotel-description">{{ hotel.description }}</p>
                </div>

                <div class="hotel-facilities">
                  <span v-for="facility in hotel.facilities.slice(0, 3)" :key="facility" class="facility-tag">
                    {{ facility }}
                  </span>
                  <span v-if="hotel.facilities.length > 3" class="facility-more">
                    +{{ hotel.facilities.length - 3 }}
                  </span>
                </div>

                <div class="hotel-footer">
                  <div class="hotel-price">
                    <span class="price-value">¥{{ hotel.price }}</span>
                    <span class="price-unit">/晚</span>
                  </div>
                  <ElButton type="primary" @click="viewHotelDetail(hotel.id)">
                    查看详情
                  </ElButton>
                </div>
              </div>
            </ElCard>
          </ElCol>
        </ElRow>
      </div>

      <!-- Pagination -->
      <div class="pagination-section">
        <ElPagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[9, 18, 27, 36]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.hotels-page {
  min-height: calc(100vh - 60px);
  padding: 40px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.search-section {
  margin-bottom: 40px;
}

.hotels-grid {
  margin-bottom: 40px;
}

.hotel-card {
  margin-bottom: 20px;
  transition: transform 0.3s, box-shadow 0.3s;
  overflow: hidden;
}

.hotel-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.hotel-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.hotel-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.hotel-card:hover .hotel-image img {
  transform: scale(1.05);
}

.hotel-content {
  padding: 20px;
}

.hotel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.hotel-header h3 {
  margin: 0;
  font-size: 1.3em;
  color: #303133;
}

.hotel-rating {
  display: flex;
  align-items: center;
  color: #e6a23c;
}

.rating-score {
  font-size: 1.2em;
  font-weight: bold;
}

.rating-text {
  font-size: 0.9em;
  margin-left: 2px;
}

.hotel-info {
  margin-bottom: 15px;
}

.hotel-address {
  margin: 0 0 10px 0;
  color: #606266;
  font-size: 0.9em;
  display: flex;
  align-items: center;
  gap: 5px;
}

.hotel-description {
  margin: 0;
  color: #909399;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hotel-facilities {
  margin-bottom: 20px;
}

.facility-tag {
  display: inline-block;
  background-color: #f0f9ff;
  color: #409eff;
  font-size: 0.8em;
  padding: 2px 8px;
  border-radius: 4px;
  margin-right: 8px;
  margin-bottom: 5px;
}

.facility-more {
  display: inline-block;
  font-size: 0.8em;
  color: #909399;
}

.hotel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hotel-price {
  display: flex;
  align-items: baseline;
}

.price-value {
  font-size: 1.5em;
  font-weight: bold;
  color: #f56c6c;
}

.price-unit {
  font-size: 0.9em;
  color: #909399;
  margin-left: 2px;
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

@media (max-width: 768px) {
  .search-section .el-col {
    margin-bottom: 10px;
  }

  .hotel-footer {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }

  .hotel-footer .el-button {
    width: 100%;
  }
}
</style>