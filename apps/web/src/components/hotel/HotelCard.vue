<script setup lang="ts">
import { computed } from 'vue'
import { ElCard, ElTag, ElButton, ElDropdown, ElDropdownMenu, ElDropdownItem } from 'element-plus'
import type { Hotel } from '../../types/hotel'

interface Props {
  hotel: Hotel
  adminMode?: boolean
}

interface Emits {
  (e: 'edit', hotel: Hotel): void
  (e: 'delete', hotel: Hotel): void
  (e: 'toggle-status', hotel: Hotel): void
  (e: 'view', hotel: Hotel): void
}

const props = withDefaults(defineProps<Props>(), {
  adminMode: false
})

const emit = defineEmits<Emits>()

const statusColor = computed(() => {
  return props.hotel.status === 'ACTIVE' ? 'success' : 'danger'
})

const statusText = computed(() => {
  return props.hotel.status === 'ACTIVE' ? '营业中' : '已停业'
})

const mainImage = computed(() => {
  return props.hotel.images && props.hotel.images.length > 0
    ? props.hotel.images[0]
    : '/api/placeholder/300/200'
})

const facilitiesList = computed(() => {
  return props.hotel.facilities ? props.hotel.facilities.slice(0, 3) : []
})

const hasMoreFacilities = computed(() => {
  return props.hotel.facilities && props.hotel.facilities.length > 3
})

function handleEdit() {
  emit('edit', props.hotel)
}

function handleDelete() {
  emit('delete', props.hotel)
}

function handleToggleStatus() {
  emit('toggle-status', props.hotel)
}

function handleView() {
  emit('view', props.hotel)
}
</script>

<template>
  <ElCard class="hotel-card" :body-style="{ padding: '0px' }">
    <!-- Hotel Image -->
    <div class="hotel-image">
      <img :src="mainImage" :alt="hotel.name" />
      <div v-if="hotel.status === 'INACTIVE'" class="status-overlay">
        <ElTag type="danger" size="large">已停业</ElTag>
      </div>
    </div>

    <!-- Hotel Content -->
    <div class="hotel-content">
      <div class="hotel-header">
        <h3 class="hotel-name">{{ hotel.name }}</h3>
        <ElTag :type="statusColor" size="small">
          {{ statusText }}
        </ElTag>
      </div>

      <div class="hotel-info">
        <p class="hotel-address">
          <i class="el-icon-location"></i>
          {{ hotel.address }}
        </p>
        <p v-if="hotel.phone" class="hotel-phone">
          <i class="el-icon-phone"></i>
          {{ hotel.phone }}
        </p>
        <p v-if="hotel.description" class="hotel-description">
          {{ hotel.description }}
        </p>
      </div>

      <!-- Facilities -->
      <div v-if="facilitiesList.length > 0" class="hotel-facilities">
        <ElTag
          v-for="facility in facilitiesList"
          :key="facility"
          class="facility-tag"
          size="small"
          type="info"
        >
          {{ facility }}
        </ElTag>
        <span v-if="hasMoreFacilities" class="facility-more">
          +{{ hotel.facilities.length - 3 }}
        </span>
      </div>

      <!-- Action Buttons -->
      <div class="hotel-actions">
        <ElButton
          type="text"
          @click="handleView"
        >
          查看详情
        </ElButton>

        <template v-if="adminMode">
          <ElDropdown @command="handleEdit">
            <ElButton type="text">
              管理 <i class="el-icon-arrow-down"></i>
            </ElButton>
            <template #dropdown>
              <ElDropdownMenu>
                <ElDropdownItem @click="handleEdit">
                  <i class="el-icon-edit"></i>
                  编辑酒店
                </ElDropdownItem>
                <ElDropdownItem @click="handleToggleStatus">
                  <i class="el-icon-video-pause" v-if="hotel.status === 'ACTIVE'"></i>
                  <i class="el-icon-video-play" v-else></i>
                  {{ hotel.status === 'ACTIVE' ? '停业' : '营业' }}
                </ElDropdownItem>
                <ElDropdownItem
                  @click="handleDelete"
                  divided
                  class="danger-item"
                >
                  <i class="el-icon-delete"></i>
                  删除酒店
                </ElDropdownItem>
              </ElDropdownMenu>
            </template>
          </ElDropdown>
        </template>
      </div>
    </div>
  </ElCard>
</template>

<style scoped>
.hotel-card {
  margin-bottom: 20px;
  transition: transform 0.3s, box-shadow 0.3s;
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.hotel-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.hotel-image {
  position: relative;
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

.status-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.7);
  padding: 10px 20px;
  border-radius: 4px;
}

.hotel-content {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.hotel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.hotel-name {
  margin: 0;
  font-size: 1.2em;
  font-weight: 600;
  color: #303133;
  flex: 1;
  margin-right: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hotel-info {
  margin-bottom: 15px;
  flex: 1;
}

.hotel-address,
.hotel-phone {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 0.9em;
  display: flex;
  align-items: center;
  gap: 6px;
}

.hotel-description {
  margin: 0;
  color: #909399;
  line-height: 1.5;
  font-size: 0.9em;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hotel-facilities {
  margin-bottom: 20px;
}

.facility-tag {
  margin-right: 8px;
  margin-bottom: 5px;
}

.facility-more {
  font-size: 0.85em;
  color: #909399;
  margin-left: 5px;
}

.hotel-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
}

.hotel-actions .el-button {
  padding: 8px 12px;
  font-size: 0.9em;
}

:deep(.danger-item) {
  color: #f56c6c !important;
}

:deep(.danger-item:hover) {
  background-color: #fef0f0 !important;
  color: #f56c6c !important;
}

@media (max-width: 768px) {
  .hotel-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }

  .hotel-actions {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }

  .hotel-actions .el-button {
    width: 100%;
  }
}
</style>