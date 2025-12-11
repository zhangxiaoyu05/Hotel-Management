<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useHotelStore } from '../../../stores/hotelStore'
import type { Hotel, HotelListQuery } from '../../../types/hotel'
import HotelCard from '../../../components/hotel/HotelCard.vue'

const router = useRouter()
const hotelStore = useHotelStore()

const searchQuery = ref('')
const selectedStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const sortBy = ref('createdAt')
const sortDirection = ref<'ASC' | 'DESC'>('DESC')

const query = computed<HotelListQuery>(() => ({
  page: currentPage.value - 1,
  size: pageSize.value,
  search: searchQuery.value || undefined,
  status: selectedStatus.value || undefined,
  sortBy: sortBy.value,
  sortDir: sortDirection.value
}))

onMounted(() => {
  loadHotels()
})

async function loadHotels() {
  try {
    await hotelStore.fetchHotels(query.value)
  } catch (error) {
    ElMessage.error('加载酒店列表失败')
  }
}

function handleSearch() {
  currentPage.value = 1
  loadHotels()
}

function handleStatusFilter() {
  currentPage.value = 1
  loadHotels()
}

function handleSort() {
  loadHotels()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadHotels()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadHotels()
}

function handleCreateHotel() {
  router.push('/admin/hotels/create')
}

function handleEditHotel(hotel: Hotel) {
  router.push(`/admin/hotels/${hotel.id}/edit`)
}

async function handleDeleteHotel(hotel: Hotel) {
  try {
    await ElMessageBox.confirm(
      `确定要删除酒店"${hotel.name}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await hotelStore.deleteHotel(hotel.id)
    ElMessage.success('酒店删除成功')
    loadHotels()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除酒店失败')
    }
  }
}

async function handleToggleHotelStatus(hotel: Hotel) {
  try {
    const newStatus = hotel.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    const statusText = newStatus === 'ACTIVE' ? '激活' : '停用'

    await ElMessageBox.confirm(
      `确定要${statusText}酒店"${hotel.name}"吗？`,
      `确认${statusText}`,
      {
        confirmButtonText: statusText,
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await hotelStore.updateHotelStatus(hotel.id, newStatus)
    ElMessage.success(`酒店${statusText}成功`)
    loadHotels()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `更新酒店状态失败`)
    }
  }
}

function handleViewHotel(hotel: Hotel) {
  router.push(`/admin/hotels/${hotel.id}`)
}
</script>

<template>
  <div class="hotel-management">
    <div class="page-header">
      <h1>酒店管理</h1>
      <el-button type="primary" @click="handleCreateHotel">
        <i class="el-icon-plus"></i>
        添加酒店
      </el-button>
    </div>

    <!-- Search and Filter Section -->
    <div class="search-section">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-input
            v-model="searchQuery"
            placeholder="搜索酒店名称或地址"
            @keyup.enter="handleSearch"
            clearable
          >
            <template #append>
              <el-button @click="handleSearch">
                <i class="el-icon-search"></i>
              </el-button>
            </template>
          </el-input>
        </el-col>
        <el-col :xs="24" :sm="12" :md="4">
          <el-select
            v-model="selectedStatus"
            placeholder="酒店状态"
            @change="handleStatusFilter"
            clearable
          >
            <el-option label="全部" value="" />
            <el-option label="营业中" value="ACTIVE" />
            <el-option label="已停业" value="INACTIVE" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="12" :md="4">
          <el-select v-model="sortBy" @change="handleSort">
            <el-option label="创建时间" value="createdAt" />
            <el-option label="酒店名称" value="name" />
            <el-option label="更新时间" value="updatedAt" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="12" :md="4">
          <el-select v-model="sortDirection" @change="handleSort">
            <el-option label="降序" value="DESC" />
            <el-option label="升序" value="ASC" />
          </el-select>
        </el-col>
      </el-row>
    </div>

    <!-- Loading State -->
    <div v-if="hotelStore.loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- Error State -->
    <div v-else-if="hotelStore.error" class="error-container">
      <el-alert
        :title="hotelStore.error"
        type="error"
        show-icon
        :closable="false"
      />
    </div>

    <!-- Hotel List -->
    <div v-else class="hotel-list">
      <template v-if="hotelStore.hotels.length > 0">
        <el-row :gutter="20">
          <el-col
            v-for="hotel in hotelStore.hotels"
            :key="hotel.id"
            :xs="24"
            :sm="12"
            :lg="8"
          >
            <HotelCard
              :hotel="hotel"
              @edit="handleEditHotel"
              @delete="handleDeleteHotel"
              @toggle-status="handleToggleHotelStatus"
              @view="handleViewHotel"
              admin-mode
            />
          </el-col>
        </el-row>
      </template>

      <el-empty
        v-else
        description="暂无酒店数据"
        :image-size="200"
      >
        <el-button type="primary" @click="handleCreateHotel">
          添加第一个酒店
        </el-button>
      </el-empty>
    </div>

    <!-- Pagination -->
    <div v-if="hotelStore.hotels.length > 0" class="pagination-section">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="hotelStore.pagination.totalElements"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.hotel-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.search-section {
  margin-bottom: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.loading-container {
  margin: 20px 0;
}

.error-container {
  margin: 20px 0;
}

.hotel-list {
  margin-bottom: 30px;
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .search-section .el-col {
    margin-bottom: 10px;
  }
}
</style>