<template>
  <el-card
    class="room-card"
    :class="{ 'room-card--selected': selected }"
    @click="$emit('click', room)"
  >
    <template #header>
      <div class="room-card__header">
        <div class="room-card__title">
          <h3>{{ room.roomNumber }}</h3>
          <RoomStatusBadge :status="room.status" />
        </div>
        <div class="room-card__actions" v-if="showActions">
          <el-dropdown trigger="click">
            <el-button type="text" size="small">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$emit('edit', room)">
                  <el-icon><Edit /></el-icon> 编辑
                </el-dropdown-item>
                <el-dropdown-item @click="$emit('view', room)">
                  <el-icon><View /></el-icon> 查看
                </el-dropdown-item>
                <el-dropdown-item
                  divided
                  @click="handleDelete"
                  class="danger-item"
                >
                  <el-icon><Delete /></el-icon> 删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </template>

    <div class="room-card__content">
      <!-- 房间图片 -->
      <div class="room-card__images" v-if="room.images && room.images.length > 0">
        <el-carousel
          height="160px"
          indicator-position="none"
          :autoplay="false"
          v-if="room.images.length > 1"
        >
          <el-carousel-item v-for="(image, index) in room.images" :key="index">
            <div class="room-card__image">
              <img :src="image" :alt="`${room.roomNumber} 图片${index + 1}`" />
            </div>
          </el-carousel-item>
        </el-carousel>
        <div class="room-card__image" v-else>
          <img :src="room.images[0]" :alt="`${room.roomNumber} 图片`" />
        </div>
      </div>

      <!-- 默认图片 -->
      <div class="room-card__image room-card__image--default" v-else>
        <el-icon size="48"><Picture /></el-icon>
        <span>暂无图片</span>
      </div>

      <!-- 房间信息 -->
      <div class="room-card__info">
        <div class="room-card__type" v-if="room.roomTypeName">
          {{ room.roomTypeName }}
        </div>

        <div class="room-card__details">
          <div class="detail-item">
            <el-icon><HomeFilled /></el-icon>
            <span>{{ room.floor }}楼</span>
          </div>
          <div class="detail-item">
            <el-icon><Grid /></el-icon>
            <span>{{ room.area }}㎡</span>
          </div>
        </div>

        <div class="room-card__price">
          <span class="price-label">价格</span>
          <span class="price-value">¥{{ room.price }}</span>
          <span class="price-unit">/晚</span>
        </div>
      </div>
    </div>

    <template #footer v-if="showFooter">
      <div class="room-card__footer">
        <el-button
          type="primary"
          size="small"
          @click.stop="$emit('edit', room)"
          v-if="showActions"
        >
          编辑
        </el-button>
        <el-button
          size="small"
          @click.stop="$emit('view', room)"
        >
          查看详情
        </el-button>
      </div>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { MoreFilled, Edit, View, Delete, Picture, HomeFilled, Grid } from '@element-plus/icons-vue';
import RoomStatusBadge from './RoomStatusBadge.vue';
import type { Room } from '@/types/room';

interface Props {
  room: Room;
  selected?: boolean;
  showActions?: boolean;
  showFooter?: boolean;
}

interface Emits {
  (e: 'click', room: Room): void;
  (e: 'edit', room: Room): void;
  (e: 'view', room: Room): void;
  (e: 'delete', room: Room): void;
}

const props = withDefaults(defineProps<Props>(), {
  selected: false,
  showActions: true,
  showFooter: false
});

const emit = defineEmits<Emits>();

const handleDelete = () => {
  ElMessageBox.confirm(
    `确定要删除房间 ${props.room.roomNumber} 吗？`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    emit('delete', props.room);
  }).catch(() => {
    // 用户取消删除
  });
};
</script>

<style scoped>
.room-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.room-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.room-card--selected {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

.room-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.room-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.room-card__title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.room-card__content {
  padding: 0;
}

.room-card__images {
  margin-bottom: 12px;
  border-radius: 6px;
  overflow: hidden;
}

.room-card__image {
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--el-fill-color-light);
  overflow: hidden;
}

.room-card__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.room-card__image--default {
  flex-direction: column;
  color: var(--el-text-color-secondary);
  gap: 8px;
}

.room-card__info {
  padding: 0 16px;
}

.room-card__type {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.room-card__details {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.room-card__price {
  display: flex;
  align-items: baseline;
  gap: 4px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.price-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.price-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.price-unit {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.room-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.danger-item {
  color: var(--el-color-danger);
}
</style>