<script setup lang="ts">
import { computed } from 'vue'
import { ElCard, ElTag, ElButton, ElButtonGroup } from 'element-plus'
import type { RoomType } from '../../../types/roomType'

interface Props {
  roomType: RoomType
  adminMode?: boolean
}

interface Emits {
  (e: 'edit', roomType: RoomType): void
  (e: 'delete', roomType: RoomType): void
  (e: 'toggleStatus', roomType: RoomType): void
  (e: 'view', roomType: RoomType): void
}

const props = withDefaults(defineProps<Props>(), {
  adminMode: false
})

const emit = defineEmits<Emits>()

const statusColor = computed(() => ({
  'ACTIVE': 'success',
  'INACTIVE': 'danger'
}[props.roomType.status]))

const statusText = computed(() => ({
  'ACTIVE': '营业中',
  'INACTIVE': '已停用'
}[props.roomType.status]))

const formatPrice = (price: number) => {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY'
  }).format(price)
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function handleEdit() {
  emit('edit', props.roomType)
}

function handleDelete() {
  emit('delete', props.roomType)
}

function handleToggleStatus() {
  emit('toggleStatus', props.roomType)
}

function handleView() {
  emit('view', props.roomType)
}
</script>

<template>
  <ElCard class="room-type-card" :body-style="{ padding: '20px' }">
    <div class="room-type-header">
      <div class="room-type-icon">
        <img
          v-if="roomType.iconUrl"
          :src="roomType.iconUrl"
          :alt="roomType.name"
          class="icon-image"
        />
        <div v-else class="icon-placeholder">
          <i class="el-icon-house"></i>
        </div>
      </div>
      <div class="room-type-info">
        <h3 class="room-type-name">{{ roomType.name }}</h3>
        <ElTag :type="statusColor" size="small">
          {{ statusText }}
        </ElTag>
      </div>
    </div>

    <div class="room-type-details">
      <div class="detail-row">
        <span class="label">容量：</span>
        <span class="value">{{ roomType.capacity }} 人</span>
      </div>
      <div class="detail-row">
        <span class="label">基础价格：</span>
        <span class="value price">{{ formatPrice(roomType.basePrice) }}</span>
      </div>
      <div v-if="roomType.description" class="detail-row description">
        <span class="label">描述：</span>
        <span class="value">{{ roomType.description }}</span>
      </div>
      <div v-if="roomType.facilities && roomType.facilities.length > 0" class="facilities">
        <span class="label">设施：</span>
        <div class="facility-tags">
          <ElTag
            v-for="facility in roomType.facilities.slice(0, 3)"
            :key="facility"
            size="small"
            type="info"
            class="facility-tag"
          >
            {{ facility }}
          </ElTag>
          <span v-if="roomType.facilities.length > 3" class="more-facilities">
            +{{ roomType.facilities.length - 3 }}
          </span>
        </div>
      </div>
    </div>

    <div class="room-type-footer">
      <div class="created-time">
        创建于 {{ formatDate(roomType.createdAt) }}
      </div>

      <div v-if="adminMode" class="action-buttons">
        <ElButtonGroup size="small">
          <ElButton @click="handleView">
            <i class="el-icon-view"></i>
            查看
          </ElButton>
          <ElButton @click="handleEdit">
            <i class="el-icon-edit"></i>
            编辑
          </ElButton>
          <ElButton
            @click="handleToggleStatus"
            :type="roomType.status === 'ACTIVE' ? 'warning' : 'success'"
          >
            <i :class="roomType.status === 'ACTIVE' ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
            {{ roomType.status === 'ACTIVE' ? '停用' : '启用' }}
          </ElButton>
          <ElButton @click="handleDelete" type="danger">
            <i class="el-icon-delete"></i>
            删除
          </ElButton>
        </ElButtonGroup>
      </div>

      <div v-else class="action-buttons">
        <ElButton size="small" @click="handleView">
          查看详情
        </ElButton>
      </div>
    </div>
  </ElCard>
</template>

<style scoped>
.room-type-card {
  height: 100%;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.room-type-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.room-type-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.room-type-icon {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
}

.icon-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.icon-placeholder {
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 24px;
}

.room-type-info {
  flex: 1;
  min-width: 0;
}

.room-type-name {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.room-type-details {
  margin-bottom: 16px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
  font-size: 14px;
}

.detail-row.description {
  flex-direction: column;
  gap: 4px;
}

.detail-row .label {
  color: #909399;
  white-space: nowrap;
  margin-right: 8px;
}

.detail-row .value {
  color: #606266;
  flex: 1;
}

.detail-row .value.price {
  font-weight: 600;
  color: #e6a23c;
  font-size: 16px;
}

.facilities {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.facility-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.facility-tag {
  font-size: 12px;
}

.more-facilities {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.room-type-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
}

.created-time {
  font-size: 12px;
  color: #909399;
}

.action-buttons {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .room-type-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .room-type-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .action-buttons .el-button-group {
    display: flex;
    flex-wrap: wrap;
  }
}
</style>