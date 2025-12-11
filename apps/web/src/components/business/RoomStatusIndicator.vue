<template>
  <div class="room-status-indicator">
    <el-tag
      :type="statusColor"
      :effect="effect"
      :size="size"
      class="status-tag"
    >
      <el-icon v-if="showIcon" class="status-icon">
        <component :is="statusIcon" />
      </el-icon>
      {{ statusText }}
    </el-tag>

    <!-- 版本信息（开发环境显示） -->
    <span v-if="showVersion && version" class="version-info">
      v{{ version }}
    </span>

    <!-- 最后更新时间 -->
    <span v-if="showLastUpdated && lastStatusChangedAt" class="last-updated">
      {{ formatLastUpdated(lastStatusChangedAt) }}
    </span>

    <!-- 状态变更历史按钮 -->
    <el-button
      v-if="showHistoryButton"
      link
      type="primary"
      size="small"
      @click="$emit('show-history')"
    >
      <el-icon><Clock /></el-icon>
      历史记录
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ElIcon, ElTag, ElButton } from 'element-plus';
import {
  Check,
  Close,
  Warning,
  Loading,
  Tools,
  Clock
} from '@element-plus/icons-vue';
import { useRoomStore } from '@/stores/roomStore';
import type { Room } from '@/types/room';

interface Props {
  room?: Room;
  status?: string;
  version?: number;
  lastStatusChangedAt?: string;
  size?: 'large' | 'default' | 'small';
  effect?: 'dark' | 'light' | 'plain';
  showIcon?: boolean;
  showVersion?: boolean;
  showLastUpdated?: boolean;
  showHistoryButton?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  size: 'default',
  effect: 'light',
  showIcon: true,
  showVersion: false,
  showLastUpdated: true,
  showHistoryButton: false
});

const emit = defineEmits<{
  'show-history': [];
}>();

const roomStore = useRoomStore();

// 计算当前状态
const currentStatus = computed(() => {
  return props.status || props.room?.status || 'UNKNOWN';
});

// 计算当前版本
const currentVersion = computed(() => {
  return props.version || props.room?.version || 0;
});

// 计算最后更新时间
const currentLastUpdated = computed(() => {
  return props.lastStatusChangedAt || (props.room as any)?.lastStatusChangedAt;
});

// 状态显示文本
const statusText = computed(() => {
  return roomStore.getStatusDisplayText(currentStatus.value);
});

// 状态颜色
const statusColor = computed(() => {
  return roomStore.getStatusColor(currentStatus.value);
});

// 状态图标
const statusIcon = computed(() => {
  switch (currentStatus.value) {
    case 'AVAILABLE':
      return Check;
    case 'OCCUPIED':
      return Close;
    case 'MAINTENANCE':
      return Tools;
    case 'CLEANING':
      return Loading;
    default:
      return Warning;
  }
});

// 格式化最后更新时间
const formatLastUpdated = (timestamp: string) => {
  try {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMins < 1) {
      return '刚刚';
    } else if (diffMins < 60) {
      return `${diffMins}分钟前`;
    } else if (diffHours < 24) {
      return `${diffHours}小时前`;
    } else if (diffDays < 7) {
      return `${diffDays}天前`;
    } else {
      return date.toLocaleDateString();
    }
  } catch (error) {
    return '';
  }
};
</script>

<style scoped>
.room-status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.status-icon {
  font-size: 14px;
}

.version-info {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
}

.last-updated {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 768px) {
  .room-status-indicator {
    gap: 4px;
  }

  .last-updated {
    display: none;
  }

  .version-info {
    font-size: 10px;
    padding: 1px 4px;
  }
}
</style>