<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElCard, ElPageHeader, ElButton, ElDescriptions, ElTag, ElRow, ElCol, ElIcon } from 'element-plus'
import { useRoomTypeStore } from '../../../stores/roomTypeStore'
import { useHotelStore } from '../../../stores/hotelStore'
import type { RoomType } from '../../../types/roomType'

const router = useRouter()
const route = useRoute()
const roomTypeStore = useRoomTypeStore()
const hotelStore = useHotelStore()

const loading = ref(false)
const roomType = computed(() => roomTypeStore.currentRoomType)
const hotel = computed(() => roomType.value ? hotelStore.hotels.find(h => h.id === roomType.value.hotelId) : null)

onMounted(async () => {
  await Promise.all([
    loadHotels(),
    loadRoomType()
  ])
})

async function loadHotels() {
  try {
    await hotelStore.fetchHotels({ page: 0, size: 1000 })
  } catch (error) {
    console.error('Failed to load hotels:', error)
  }
}

async function loadRoomType() {
  if (!route.params.id) return

  loading.value = true

  try {
    await roomTypeStore.fetchRoomType(Number(route.params.id))
  } catch (error) {
    ElMessage.error('加载房间类型详情失败')
    router.push('/admin/room-types')
  } finally {
    loading.value = false
  }
}

function handleEdit() {
  router.push(`/admin/room-types/${roomType.value?.id}/edit`)
}

function handleBack() {
  router.back()
}

function handleToList() {
  router.push('/admin/room-types')
}

function formatPrice(price: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY'
  }).format(price)
}

function formatDate(dateString: string) {
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const statusColor = computed(() => ({
  'ACTIVE': 'success',
  'INACTIVE': 'danger'
}[roomType.value?.status || '']))

const statusText = computed(() => ({
  'ACTIVE': '营业中',
  'INACTIVE': '已停用'
}[roomType.value?.status || '']))
</script>

<template>
  <div class="room-type-detail-page">
    <div class="page-header">
      <ElPageHeader
        title="房间类型详情"
        @back="handleBack"
      >
        <template #extra>
          <div class="header-actions">
            <ElButton @click="handleToList">
              <i class="el-icon-back"></i>
              返回列表
            </ElButton>
            <ElButton type="primary" @click="handleEdit">
              <i class="el-icon-edit"></i>
              编辑
            </ElButton>
          </div>
        </template>
      </ElPageHeader>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="roomType" class="detail-container">
      <ElRow :gutter="20">
        <!-- 基本信息 -->
        <ElCol :xs="24" :lg="16">
          <ElCard class="detail-card">
            <template #header>
              <div class="card-header">
                <h3>基本信息</h3>
                <ElTag :type="statusColor">
                  {{ statusText }}
                </ElTag>
              </div>
            </template>

            <ElDescriptions :column="2" border>
              <ElDescriptionsItem label="房间类型名称">
                {{ roomType.name }}
              </ElDescriptionsItem>
              <ElDescriptionsItem label="所属酒店">
                {{ hotel?.name || '未知酒店' }}
              </ElDescriptionsItem>
              <ElDescriptionsItem label="房间容量">
                {{ roomType.capacity }} 人
              </ElDescriptionsItem>
              <ElDescriptionsItem label="基础价格">
                <span class="price">{{ formatPrice(roomType.basePrice) }}</span>
              </ElDescriptionsItem>
              <ElDescriptionsItem label="状态" :span="2">
                <ElTag :type="statusColor">
                  {{ statusText }}
                </ElTag>
              </ElDescriptionsItem>
              <ElDescriptionsItem v-if="roomType.description" label="房间描述" :span="2">
                {{ roomType.description }}
              </ElDescriptionsItem>
            </ElDescriptions>
          </ElCard>
        </ElCol>

        <!-- 房间类型图标 -->
        <ElCol :xs="24" :lg="8">
          <ElCard class="detail-card">
            <template #header>
              <h3>类型图标</h3>
            </template>

            <div class="icon-display">
              <img
                v-if="roomType.iconUrl"
                :src="roomType.iconUrl"
                :alt="roomType.name"
                class="icon-image"
              />
              <div v-else class="icon-placeholder">
                <i class="el-icon-house"></i>
                <p>暂无图标</p>
              </div>
            </div>
          </ElCard>
        </ElCol>
      </ElRow>

      <!-- 房间设施 -->
      <ElCard v-if="roomType.facilities && roomType.facilities.length > 0" class="detail-card">
        <template #header>
          <h3>房间设施</h3>
        </template>

        <div class="facilities-list">
          <ElTag
            v-for="facility in roomType.facilities"
            :key="facility"
            type="info"
            class="facility-tag"
          >
            <i class="el-icon-check"></i>
            {{ facility }}
          </ElTag>
        </div>
      </ElCard>

      <!-- 时间信息 -->
      <ElCard class="detail-card">
        <template #header>
          <h3>时间信息</h3>
        </template>

        <ElDescriptions :column="2" border>
          <ElDescriptionsItem label="创建时间">
            {{ formatDate(roomType.createdAt) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem v-if="roomType.updatedAt" label="更新时间">
            {{ formatDate(roomType.updatedAt) }}
          </ElDescriptionsItem>
        </ElDescriptions>
      </ElCard>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <ElButton size="large" @click="handleBack">
          <i class="el-icon-back"></i>
          返回上页
        </ElButton>
        <ElButton type="primary" size="large" @click="handleEdit">
          <i class="el-icon-edit"></i>
          编辑房间类型
        </ElButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.room-type-detail-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.loading-container {
  margin: 20px 0;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-card {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.price {
  font-weight: 600;
  color: #e6a23c;
  font-size: 16px;
}

.icon-display {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.icon-image {
  max-width: 100%;
  max-height: 120px;
  object-fit: contain;
  border-radius: 8px;
}

.icon-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  background-color: #f5f7fa;
  border-radius: 8px;
  color: #909399;
  font-size: 48px;
  margin: 0 auto;
}

.icon-placeholder p {
  margin: 8px 0 0 0;
  font-size: 14px;
}

.facilities-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.facility-tag {
  padding: 8px 16px;
  font-size: 14px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

@media (max-width: 768px) {
  .room-type-detail-page {
    padding: 10px;
  }

  .header-actions {
    flex-direction: column;
    gap: 8px;
  }

  .action-buttons {
    flex-direction: column;
    align-items: stretch;
  }

  .facilities-list {
    gap: 8px;
  }

  .facility-tag {
    font-size: 12px;
    padding: 6px 12px;
  }
}
</style>