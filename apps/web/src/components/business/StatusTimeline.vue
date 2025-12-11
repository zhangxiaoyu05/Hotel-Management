<template>
  <div class="status-timeline">
    <el-timeline
      :class="{ 'compact-mode': compact }"
      :reverse="reverse"
    >
      <el-timeline-item
        v-for="(item, index) in timelineItems"
        :key="item.id || index"
        :timestamp="formatTimestamp(item.timestamp)"
        :type="getItemType(item)"
        :color="getItemColor(item)"
        :icon="getItemIcon(item)"
        :size="compact ? 'small' : 'normal'"
        :placement="reverse ? 'top' : 'bottom'"
        :hollow="item.hollow"
      >
        <div class="timeline-item-content" @click="$emit('item-click', item)">
          <!-- 状态指示器 -->
          <div class="status-indicator" v-if="item.type === 'status_change'">
            <RoomStatusIndicator
              :status="item.newStatus"
              :show-icon="true"
              :size="compact ? 'small' : 'default'"
            />
            <el-icon class="arrow-icon"><Right /></el-icon>
            <RoomStatusIndicator
              :status="item.oldStatus"
              :show-icon="true"
              :size="compact ? 'small' : 'default'"
              :show-last-updated="false"
            />
          </div>

          <!-- 标题 -->
          <div class="item-title">
            <span class="title-text">{{ item.title }}</span>
            <el-tag
              v-if="item.tag"
              :type="item.tagType || 'info'"
              size="small"
              class="title-tag"
            >
              {{ item.tag }}
            </el-tag>
          </div>

          <!-- 描述 -->
          <div v-if="item.description" class="item-description">
            {{ item.description }}
          </div>

          <!-- 元数据 -->
          <div class="item-meta">
            <span v-if="item.operator" class="meta-item">
              <el-icon><User /></el-icon>
              {{ item.operator }}
            </span>
            <span v-if="item.duration" class="meta-item">
              <el-icon><Timer /></el-icon>
              {{ item.duration }}
            </span>
            <span v-if="item.relatedId" class="meta-item">
              <el-icon><Link /></el-icon>
              {{ item.relatedType }} #{{ item.relatedId }}
            </span>
            <span class="meta-item timestamp">
              {{ formatRelativeTime(item.timestamp) }}
            </span>
          </div>

          <!-- 操作按钮 -->
          <div v-if="item.actions && item.actions.length > 0" class="item-actions">
            <el-button
              v-for="action in item.actions"
              :key="action.key"
              link
              :type="action.type || 'primary'"
              size="small"
              @click.stop="handleAction(action, item)"
            >
              <el-icon v-if="action.icon">
                <component :is="action.icon" />
              </el-icon>
              {{ action.label }}
            </el-button>
          </div>

          <!-- 扩展内容 -->
          <div v-if="showExpanded(item.id)" class="item-expanded">
            <slot :name="`expanded-${item.id}`" :item="item">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item
                  v-for="(value, key) in item.expandedData"
                  :key="key"
                  :label="key"
                >
                  {{ value }}
                </el-descriptions-item>
              </el-descriptions>
            </slot>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>

    <!-- 加载更多 -->
    <div v-if="showLoadMore" class="load-more">
      <el-button
        :loading="loading"
        @click="$emit('load-more')"
      >
        加载更多
      </el-button>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-if="!loading && timelineItems.length === 0"
      :description="emptyDescription"
      :image-size="compact ? 100 : 120"
    >
      <template #image>
        <el-icon size="120" color="var(--el-color-info-light-5)">
          <Clock />
        </el-icon>
      </template>
      <el-button v-if="showRefresh" type="primary" @click="$emit('refresh')">
        刷新
      </el-button>
    </el-empty>

    <!-- 加载状态 -->
    <div v-if="loading && timelineItems.length === 0" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  Right,
  User,
  Timer,
  Link,
  Clock,
  Check,
  Close,
  Warning,
  Loading,
  Tools,
  Document,
  Plus,
  View,
  Edit
} from '@element-plus/icons-vue';
import RoomStatusIndicator from './RoomStatusIndicator.vue';
import type { RoomStatusLog } from '@/services/roomStatusService';

// 时间线项目接口
export interface TimelineItem {
  id?: string | number;
  type: 'status_change' | 'booking' | 'cancellation' | 'maintenance' | 'cleaning' | 'custom';
  title: string;
  description?: string;
  timestamp: string;
  oldStatus?: string;
  newStatus?: string;
  operator?: string;
  duration?: string;
  relatedId?: number;
  relatedType?: string;
  tag?: string;
  tagType?: string;
  hollow?: boolean;
  color?: string;
  icon?: any;
  actions?: Array<{
    key: string;
    label: string;
    type?: string;
    icon?: any;
    handler?: (item: TimelineItem) => void;
  }>;
  expandedData?: Record<string, any>;
}

interface Props {
  items: TimelineItem[];
  loading?: boolean;
  compact?: boolean;
  reverse?: boolean;
  showLoadMore?: boolean;
  showRefresh?: boolean;
  emptyDescription?: string;
  maxExpandedItems?: number;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  compact: false,
  reverse: false,
  showLoadMore: false,
  showRefresh: true,
  emptyDescription: '暂无时间线记录',
  maxExpandedItems: 3
});

const emit = defineEmits<{
  'item-click': [item: TimelineItem];
  'load-more': [];
  'refresh': [];
  'action': [action: any, item: TimelineItem];
}>();

// 展开状态管理
const expandedItems = ref<Set<string | number>>(new Set());

const timelineItems = computed(() => props.items);

const showExpanded = (itemId: string | number | undefined) => {
  return itemId && expandedItems.value.has(itemId);
};

const toggleExpanded = (itemId: string | number) => {
  if (expandedItems.value.has(itemId)) {
    expandedItems.value.delete(itemId);
  } else {
    // 限制展开数量
    if (expandedItems.value.size >= props.maxExpandedItems) {
      const firstItem = expandedItems.value.values().next().value;
      expandedItems.value.delete(firstItem);
    }
    expandedItems.value.add(itemId);
  }
};

// 格式化时间戳
const formatTimestamp = (timestamp: string): string => {
  return new Date(timestamp).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

// 格式化相对时间
const formatRelativeTime = (timestamp: string): string => {
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / (1000 * 60));
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffMins < 1) return '刚刚';
  if (diffMins < 60) return `${diffMins}分钟前`;
  if (diffHours < 24) return `${diffHours}小时前`;
  if (diffDays < 7) return `${diffDays}天前`;
  return formatTimestamp(timestamp);
};

// 获取项目类型
const getItemType = (item: TimelineItem): string => {
  const typeMap: Record<string, string> = {
    'status_change': 'primary',
    'booking': 'success',
    'cancellation': 'warning',
    'maintenance': 'danger',
    'cleaning': 'info'
  };
  return typeMap[item.type] || 'primary';
};

// 获取项目颜色
const getItemColor = (item: TimelineItem): string => {
  if (item.color) return item.color;

  const colorMap: Record<string, string> = {
    'AVAILABLE': '#67c23a',
    'OCCUPIED': '#f56c6c',
    'MAINTENANCE': '#e6a23c',
    'CLEANING': '#409eff'
  };

  if (item.type === 'status_change' && item.newStatus) {
    return colorMap[item.newStatus] || '#409eff';
  }

  return '';
};

// 获取项目图标
const getItemIcon = (item: TimelineItem) => {
  if (item.icon) return item.icon;

  const iconMap: Record<string, any> = {
    'status_change': item.newStatus === 'AVAILABLE' ? Check : (
                   item.newStatus === 'OCCUPIED' ? Close : (
                   item.newStatus === 'MAINTENANCE' ? Tools : (
                   item.newStatus === 'CLEANING' ? Loading : Warning
                 ))),
    'booking': Plus,
    'cancellation': Close,
    'maintenance': Tools,
    'cleaning': Loading,
    'custom': Document
  };

  return iconMap[item.type] || Clock;
};

// 处理操作按钮点击
const handleAction = (action: any, item: TimelineItem) => {
  if (action.handler) {
    action.handler(item);
  } else {
    emit('action', action, item);
  }

  // 如果是展开/收起操作
  if (action.key === 'expand') {
    toggleExpanded(item.id!);
  }
};

// 转换RoomStatusLog到TimelineItem的静态方法
export const fromRoomStatusLog = (log: RoomStatusLog): TimelineItem => {
  return {
    id: log.id,
    type: 'status_change',
    title: '房间状态变更',
    description: log.reason,
    timestamp: log.createdAt,
    oldStatus: log.oldStatus,
    newStatus: log.newStatus,
    operator: `操作员 ${log.changedBy}`,
    relatedId: log.orderId,
    relatedType: log.orderId ? '订单' : undefined,
    tag: log.orderId ? '关联订单' : undefined,
    tagType: log.orderId ? 'success' : undefined,
    expandedData: {
      '日志ID': log.id,
      '房间ID': log.roomId,
      '操作员ID': log.changedBy,
      '变更原因': log.reason,
      '关联订单': log.orderId || '无',
      '操作时间': new Date(log.createdAt).toLocaleString('zh-CN')
    },
    actions: [
      {
        key: 'expand',
        label: '详情',
        type: 'primary',
        icon: View
      }
    ]
  };
};
</script>

<style scoped>
.status-timeline {
  padding: 16px 0;
}

.timeline-item-content {
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 12px;
  border-radius: 8px;
}

.timeline-item-content:hover {
  background-color: var(--el-fill-color-light);
}

.timeline-item-content:active {
  transform: scale(0.98);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.arrow-icon {
  color: var(--el-text-color-secondary);
  transform: rotate(180deg);
}

.item-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.title-text {
  font-weight: 600;
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.title-tag {
  font-size: 12px;
}

.item-description {
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 8px;
}

.item-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.meta-item.timestamp {
  margin-left: auto;
}

.item-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.item-expanded {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

.loading-state {
  padding: 16px;
}

/* 紧凑模式样式 */
.compact-mode :deep(.el-timeline-item__timestamp) {
  font-size: 12px;
  margin-bottom: 4px;
}

.compact-mode .timeline-item-content {
  padding: 8px;
}

.compact-mode .status-indicator {
  margin-bottom: 4px;
}

.compact-mode .title-text {
  font-size: 13px;
}

.compact-mode .item-description {
  font-size: 12px;
  margin-bottom: 4px;
}

.compact-mode .item-meta {
  font-size: 11px;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .status-timeline {
    padding: 8px 0;
  }

  .timeline-item-content {
    padding: 8px;
  }

  .status-indicator {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .arrow-icon {
    transform: rotate(90deg);
    margin: 0 auto;
  }

  .item-title {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .item-meta {
    flex-direction: column;
    gap: 4px;
  }

  .meta-item.timestamp {
    margin-left: 0;
  }

  .item-actions {
    flex-wrap: wrap;
  }
}
</style>