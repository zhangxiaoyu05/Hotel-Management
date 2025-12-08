<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hotelService } from '../../../services/hotelService'
import ImageGallery from '../../../components/hotel/ImageGallery.vue'
import type { Hotel } from '../../../types/hotel'

const router = useRouter()
const route = useRoute()

const hotelId = computed(() => Number(route.params.id))
const loading = ref(false)
const hotel = ref<Hotel | null>(null)

const mainImage = computed(() => {
  return hotel.value?.images && hotel.value.images.length > 0
    ? hotel.value.images[0]
    : '/api/placeholder/800/400'
})

onMounted(() => {
  loadHotel()
})

async function loadHotel() {
  if (!hotelId.value) return

  loading.value = true
  try {
    hotel.value = await hotelService.getHotelById(hotelId.value)
  } catch (error: any) {
    ElMessage.error(error.message || '加载酒店信息失败')
    router.push('/admin/hotels')
  } finally {
    loading.value = false
  }
}

function handleEdit() {
  router.push(`/admin/hotels/${hotelId.value}/edit`)
}

async function handleDelete() {
  if (!hotel.value) return

  try {
    await ElMessageBox.confirm(
      `确定要删除酒店"${hotel.value.name}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await hotelService.deleteHotel(hotelId.value)
    ElMessage.success('酒店删除成功')
    router.push('/admin/hotels')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除酒店失败')
    }
  }
}

async function handleToggleStatus() {
  if (!hotel.value) return

  try {
    const newStatus = hotel.value.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    const statusText = newStatus === 'ACTIVE' ? '激活' : '停用'

    await ElMessageBox.confirm(
      `确定要${statusText}酒店"${hotel.value.name}"吗？`,
      `确认${statusText}`,
      {
        confirmButtonText: statusText,
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await hotelService.updateHotelStatus(hotelId.value, newStatus)
    hotel.value.status = newStatus
    ElMessage.success(`酒店${statusText}成功`)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `更新酒店状态失败`)
    }
  }
}

function handleBack() {
  router.push('/admin/hotels')
}

const statusColor = computed(() => {
  return hotel.value?.status === 'ACTIVE' ? 'success' : 'danger'
})

const statusText = computed(() => {
  return hotel.value?.status === 'ACTIVE' ? '营业中' : '已停业'
})
</script>

<template>
  <div class="hotel-detail-page">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="handleBack" icon="el-icon-arrow-left">
          返回列表
        </el-button>
        <h1 v-if="hotel">{{ hotel.name }}</h1>
      </div>
      <div class="header-actions">
        <el-button @click="handleEdit" icon="el-icon-edit">
          编辑酒店
        </el-button>
        <el-button
          :type="hotel?.status === 'ACTIVE' ? 'warning' : 'success'"
          @click="handleToggleStatus"
          :icon="hotel?.status === 'ACTIVE' ? 'el-icon-video-pause' : 'el-icon-video-play'"
        >
          {{ hotel?.status === 'ACTIVE' ? '停业' : '营业' }}
        </el-button>
        <el-button
          type="danger"
          @click="handleDelete"
          icon="el-icon-delete"
        >
          删除酒店
        </el-button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- Hotel Detail Content -->
    <div v-else-if="hotel" class="hotel-content">
      <!-- Status Badge -->
      <div class="status-section">
        <el-tag :type="statusColor" size="large">
          {{ statusText }}
        </el-tag>
      </div>

      <!-- Images Gallery -->
      <div v-if="hotel.images && hotel.images.length > 0" class="images-section">
        <el-card>
          <template #header>
            <h3>酒店图片</h3>
          </template>
          <ImageGallery :images="hotel.images" />
        </el-card>
      </div>

      <!-- Basic Information -->
      <div class="info-section">
        <el-card>
          <template #header>
            <h3>基本信息</h3>
          </template>

          <el-row :gutter="30">
            <el-col :span="12">
              <div class="info-item">
                <label>酒店名称</label>
                <span>{{ hotel.name }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <label>联系电话</label>
                <span>{{ hotel.phone || '暂无' }}</span>
              </div>
            </el-col>
          </el-row>

          <div class="info-item">
            <label>酒店地址</label>
            <span>{{ hotel.address }}</span>
          </div>

          <div class="info-item">
            <label>酒店简介</label>
            <p>{{ hotel.description || '暂无简介' }}</p>
          </div>

          <el-row :gutter="30">
            <el-col :span="12">
              <div class="info-item">
                <label>创建时间</label>
                <span>{{ new Date(hotel.createdAt).toLocaleString() }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <label>更新时间</label>
                <span>{{ new Date(hotel.updatedAt).toLocaleString() }}</span>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </div>

      <!-- Facilities -->
      <div v-if="hotel.facilities && hotel.facilities.length > 0" class="facilities-section">
        <el-card>
          <template #header>
            <h3>设施服务</h3>
          </template>
          <div class="facilities-list">
            <el-tag
              v-for="facility in hotel.facilities"
              :key="facility"
              class="facility-tag"
              type="info"
            >
              {{ facility }}
            </el-tag>
          </div>
        </el-card>
      </div>

      <!-- Statistics -->
      <div class="stats-section">
        <el-card>
          <template #header>
            <h3>统计信息</h3>
          </template>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-value">{{ hotel.images.length }}</div>
                <div class="stat-label">图片数量</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-value">{{ hotel.facilities.length }}</div>
                <div class="stat-label">设施数量</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-value">
                  {{ new Date(hotel.createdAt).getFullYear() }}
                </div>
                <div class="stat-label">创建年份</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <el-empty description="酒店信息不存在" />
    </div>
  </div>
</template>

<style scoped>
.hotel-detail-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.loading-container {
  margin: 40px 0;
}

.hotel-content {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.status-section {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.images-section h3,
.info-section h3,
.facilities-section h3,
.stats-section h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.info-item {
  margin-bottom: 20px;
}

.info-item label {
  display: block;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-item span {
  color: #303133;
  font-size: 14px;
}

.info-item p {
  margin: 0;
  color: #303133;
  line-height: 1.6;
  font-size: 14px;
}

.facilities-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.facility-tag {
  margin: 0;
}

.stats-section .stat-item {
  text-align: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .header-left {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }

  .header-actions {
    flex-direction: column;
    gap: 10px;
  }

  .header-actions .el-button {
    width: 100%;
  }
}
</style>