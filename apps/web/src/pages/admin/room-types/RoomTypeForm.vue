<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElCard, ElPageHeader } from 'element-plus'
import { useRoomTypeStore } from '../../../stores/roomTypeStore'
import { useHotelStore } from '../../../stores/hotelStore'
import type { RoomType, CreateRoomTypeRequest, UpdateRoomTypeRequest } from '../../../types/roomType'
import RoomTypeForm from '../../../components/room-type/RoomTypeForm.vue'

const router = useRouter()
const route = useRoute()
const roomTypeStore = useRoomTypeStore()
const hotelStore = useHotelStore()

const loading = ref(false)
const isEdit = computed(() => !!route.params.id)
const title = computed(() => isEdit.value ? '编辑房间类型' : '创建房间类型')

// 获取酒店ID
const hotelId = computed(() => {
  const hotelIdParam = route.query.hotelId
  return hotelIdParam ? Number(hotelIdParam) : undefined
})

onMounted(async () => {
  await Promise.all([
    loadHotels(),
    isEdit.value && loadRoomType()
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

  try {
    await roomTypeStore.fetchRoomType(Number(route.params.id))
  } catch (error) {
    ElMessage.error('加载房间类型信息失败')
    router.push('/admin/room-types')
  }
}

async function handleSubmit(data: CreateRoomTypeRequest | UpdateRoomTypeRequest) {
  loading.value = true

  try {
    if (isEdit.value) {
      // 编辑模式
      await roomTypeStore.updateRoomType(Number(route.params.id), data as UpdateRoomTypeRequest)
      ElMessage.success('房间类型更新成功')
    } else {
      // 创建模式
      const createData = {
        ...data,
        hotelId: hotelId.value || hotelStore.hotels[0]?.id
      } as CreateRoomTypeRequest

      if (!createData.hotelId) {
        ElMessage.error('请选择酒店')
        return
      }

      await roomTypeStore.createRoomType(createData)
      ElMessage.success('房间类型创建成功')
    }

    router.push('/admin/room-types')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  router.push('/admin/room-types')
}

function handleBack() {
  router.back()
}
</script>

<template>
  <div class="room-type-form-page">
    <div class="page-header">
      <ElPageHeader
        :title="title"
        @back="handleBack"
      />
    </div>

    <div class="form-container">
      <ElCard>
        <template #header>
          <span>{{ title }}</span>
        </template>

        <RoomTypeForm
          :room-type="roomTypeStore.currentRoomType"
          :loading="loading"
          :hotel-id="hotelId"
          @submit="handleSubmit"
          @cancel="handleCancel"
        />
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.room-type-form-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.form-container {
  background: #fff;
}

@media (max-width: 768px) {
  .room-type-form-page {
    padding: 10px;
  }
}
</style>