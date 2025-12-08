<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElSelect, ElOption } from 'element-plus'
import { useRoomTypeStore } from '../../../stores/roomTypeStore'
import { useHotelStore } from '../../../stores/hotelStore'
import type { RoomType, RoomTypeListQuery } from '../../../types/roomType'
import RoomTypeCard from '../../../components/room-type/RoomTypeCard.vue'

const router = useRouter()
const roomTypeStore = useRoomTypeStore()
const hotelStore = useHotelStore()

const searchQuery = ref('')
const selectedStatus = ref('')
const selectedHotel = ref<number | undefined>()
const currentPage = ref(1)
const pageSize = ref(20)
const sortBy = ref('createdAt')
const sortDirection = ref<'ASC' | 'DESC'>('DESC')

const query = computed<RoomTypeListQuery>(() => ({
  page: currentPage.value - 1,
  size: pageSize.value,
  search: searchQuery.value || undefined,
  hotelId: selectedHotel.value,
  status: selectedStatus.value || undefined,
  sortBy: sortBy.value,
  sortDir: sortDirection.value
}))

onMounted(async () => {
  await Promise.all([
    loadHotels(),
    loadRoomTypes()
  ])
})

async function loadHotels() {
  try {
    await hotelStore.fetchHotels({ page: 0, size: 1000 })
  } catch (error) {
    console.error('Failed to load hotels:', error)
  }
}

async function loadRoomTypes() {
  try {
    await roomTypeStore.fetchRoomTypes(query.value)
  } catch (error) {
    ElMessage.error('加载房间类型列表失败')
  }
}

function handleSearch() {
  currentPage.value = 1
  loadRoomTypes()
}

function handleStatusFilter() {
  currentPage.value = 1
  loadRoomTypes()
}

function handleHotelFilter() {
  currentPage.value = 1
  loadRoomTypes()
}

function handleSort() {
  loadRoomTypes()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadRoomTypes()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadRoomTypes()
}

function handleCreateRoomType() {
  router.push('/admin/room-types/create')
}

function handleEditRoomType(roomType: RoomType) {
  router.push(`/admin/room-types/${roomType.id}/edit`)
}

async function handleDeleteRoomType(roomType: RoomType) {
  try {
    await ElMessageBox.confirm(
      `确定要删除房间类型"${roomType.name}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await roomTypeStore.deleteRoomType(roomType.id)
    ElMessage.success('房间类型删除成功')
    loadRoomTypes()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除房间类型失败')
    }
  }
}

async function handleToggleRoomTypeStatus(roomType: RoomType) {
  try {
    const newStatus = roomType.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    const statusText = newStatus === 'ACTIVE' ? '激活' : '停用'

    await ElMessageBox.confirm(
      `确定要${statusText}房间类型"${roomType.name}"吗？`,
      `确认${statusText}`,
      {
        confirmButtonText: statusText,
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await roomTypeStore.updateRoomTypeStatus(roomType.id, newStatus)
    ElMessage.success(`房间类型${statusText}成功`)
    loadRoomTypes()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `更新房间类型状态失败`)
    }
  }
}

function handleViewRoomType(roomType: RoomType) {
  router.push(`/admin/room-types/${roomType.id}`)
}
</script>

<template>
  <div class="room-type-management">
    <div class="page-header">
      <h1>房间类型管理</h1>
      <el-button type="primary" @click="handleCreateRoomType">
        <i class="el-icon-plus"></i>
        添加房间类型
      </el-button>
    </div>

    <!-- Search and Filter Section -->
    <div class="search-section">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-input
            v-model="searchQuery"
            placeholder="搜索房间类型名称或描述"
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
            v-model="selectedHotel"
            placeholder="选择酒店"
            @change="handleHotelFilter"
            clearable
            filterable
          >
            <el-option label="全部酒店" :value="undefined" />
            <el-option
              v-for="hotel in hotelStore.hotels"
              :key="hotel.id"
              :label="hotel.name"
              :value="hotel.id"
            />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="12" :md="4">
          <el-select
            v-model="selectedStatus"
            placeholder="房间状态"
            @change="handleStatusFilter"
            clearable
          >
            <el-option label="全部" value="" />
            <el-option label="营业中" value="ACTIVE" />
            <el-option label="已停用" value="INACTIVE" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="12" :md="4">
          <el-select v-model="sortBy" @change="handleSort">
            <el-option label="创建时间" value="createdAt" />
            <el-option label="房间类型名称" value="name" />
            <el-option label="基础价格" value="basePrice" />
            <el-option label="容量" value="capacity" />
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
    <div v-if="roomTypeStore.loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- Error State -->
    <div v-else-if="roomTypeStore.error" class="error-container">
      <el-alert
        :title="roomTypeStore.error"
        type="error"
        show-icon
        :closable="false"
      />
    </div>

    <!-- Room Type List -->
    <div v-else class="room-type-list">
      <template v-if="roomTypeStore.roomTypes.length > 0">
        <el-row :gutter="20">
          <el-col
            v-for="roomType in roomTypeStore.roomTypes"
            :key="roomType.id"
            :xs="24"
            :sm="12"
            :lg="8"
          >
            <RoomTypeCard
              :room-type="roomType"
              @edit="handleEditRoomType"
              @delete="handleDeleteRoomType"
              @toggle-status="handleToggleRoomTypeStatus"
              @view="handleViewRoomType"
              admin-mode
            />
          </el-col>
        </el-row>
      </template>

      <el-empty
        v-else
        description="暂无房间类型数据"
        :image-size="200"
      >
        <el-button type="primary" @click="handleCreateRoomType">
          添加第一个房间类型
        </el-button>
      </el-empty>
    </div>

    <!-- Pagination -->
    <div v-if="roomTypeStore.roomTypes.length > 0" class="pagination-section">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="roomTypeStore.pagination.totalElements"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.room-type-management {
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

.room-type-list {
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