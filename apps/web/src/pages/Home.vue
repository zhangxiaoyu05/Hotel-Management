<script setup lang="ts">
import { ElRow, ElCol, ElCard, ElButton, ElImage } from 'element-plus'

const featuredHotels = [
  {
    id: 1,
    name: '成都望江宾馆',
    description: '位于成都市中心，交通便利，设施齐全',
    image: '/api/placeholder/300/200',
    price: 288
  },
  {
    id: 2,
    name: '天府酒店',
    description: '现代化商务酒店，服务优质',
    image: '/api/placeholder/300/200',
    price: 368
  },
  {
    id: 3,
    name: '锦江宾馆',
    description: '历史悠久的知名酒店，环境优雅',
    image: '/api/placeholder/300/200',
    price: 458
  }
]
</script>

<template>
  <div class="home">
    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-content">
        <h1>欢迎来到成都酒店管理系统</h1>
        <p>发现成都最美的酒店，享受舒适的住宿体验</p>
        <ElButton type="primary" size="large" @click="$router.push('/hotels')">
          浏览酒店
        </ElButton>
      </div>
    </section>

    <!-- Featured Hotels -->
    <section class="featured-hotels">
      <div class="container">
        <h2>推荐酒店</h2>
        <ElRow :gutter="20">
          <ElCol v-for="hotel in featuredHotels" :key="hotel.id" :xs="24" :sm="12" :md="8">
            <ElCard class="hotel-card" :body-style="{ padding: '0px' }">
              <ElImage
                :src="hotel.image"
                fit="cover"
                class="hotel-image"
                :alt="hotel.name"
              />
              <div class="hotel-info">
                <h3>{{ hotel.name }}</h3>
                <p>{{ hotel.description }}</p>
                <div class="hotel-footer">
                  <span class="price">¥{{ hotel.price }}/晚</span>
                  <ElButton type="primary" size="small">查看详情</ElButton>
                </div>
              </div>
            </ElCard>
          </ElCol>
        </ElRow>
      </div>
    </section>

    <!-- Features Section -->
    <section class="features">
      <div class="container">
        <h2>我们的优势</h2>
        <ElRow :gutter="20">
          <ElCol :xs="24" :sm="12" :md="6">
            <div class="feature">
              <div class="feature-icon">🏨</div>
              <h3>优质酒店</h3>
              <p>精选成都地区优质酒店，确保住宿品质</p>
            </div>
          </ElCol>
          <ElCol :xs="24" :sm="12" :md="6">
            <div class="feature">
              <div class="feature-icon">💰</div>
              <h3>优惠价格</h3>
              <p>提供最具竞争力的价格和优惠活动</p>
            </div>
          </ElCol>
          <ElCol :xs="24" :sm="12" :md="6">
            <div class="feature">
              <div class="feature-icon">🔒</div>
              <h3>安全支付</h3>
              <p>支持多种安全的支付方式，保障交易安全</p>
            </div>
          </ElCol>
          <ElCol :xs="24" :sm="12" :md="6">
            <div class="feature">
              <div class="feature-icon">🎧</div>
              <h3>24/7客服</h3>
              <p>全天候客服支持，随时为您解答疑问</p>
            </div>
          </ElCol>
        </ElRow>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
}

.hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 100px 0;
  text-align: center;
}

.hero-content h1 {
  font-size: 3em;
  margin-bottom: 20px;
  font-weight: 300;
}

.hero-content p {
  font-size: 1.2em;
  margin-bottom: 30px;
  opacity: 0.9;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.featured-hotels {
  padding: 80px 0;
}

.featured-hotels h2 {
  text-align: center;
  font-size: 2.5em;
  margin-bottom: 50px;
  color: #303133;
}

.hotel-card {
  margin-bottom: 20px;
  transition: transform 0.3s, box-shadow 0.3s;
}

.hotel-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.hotel-image {
  width: 100%;
  height: 200px;
}

.hotel-info {
  padding: 20px;
}

.hotel-info h3 {
  margin: 0 0 10px 0;
  font-size: 1.2em;
  color: #303133;
}

.hotel-info p {
  margin: 0 0 15px 0;
  color: #606266;
  line-height: 1.5;
}

.hotel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  font-size: 1.2em;
  font-weight: bold;
  color: #f56c6c;
}

.features {
  background-color: #f8f9fa;
  padding: 80px 0;
}

.features h2 {
  text-align: center;
  font-size: 2.5em;
  margin-bottom: 50px;
  color: #303133;
}

.feature {
  text-align: center;
  padding: 30px 20px;
}

.feature-icon {
  font-size: 3em;
  margin-bottom: 20px;
}

.feature h3 {
  font-size: 1.5em;
  margin-bottom: 15px;
  color: #303133;
}

.feature p {
  color: #606266;
  line-height: 1.6;
}
</style>