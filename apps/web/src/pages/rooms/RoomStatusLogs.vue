<template>
  <div class="room-status-logs">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-page-header @back="$router.go(-1)">
        <template #content>
          <div class="header-content">
            <el-icon><Clock /></el-icon>
            <span>房间状态日志</span>
            <span v-if="roomInfo" class="room-info">{{ roomInfo.name }} ({{ roomInfo.roomNumber }})</span>
          </div>
        </template>
      </el-page-header>

      <div class="header-actions">
        <el-button @click="refreshLogs" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="showFilters = !showFilters">
          <el-icon><Filter /></el-icon>
          筛选
        </el-button>
        <el-button type="success" @click="exportLogs">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <!-- 筛选器 -->
    <el-collapse-transition>
      <div v-show="showFilters" class="filters-section">
        <el-card>
          <el-form :model="filters" inline>
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="dateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                @change="handleDateRangeChange"
              />
            </el-form-item>

            <el-form-item label="状态类型">
              <el-select
                v-model="filters.status"
                placeholder="选择状态"
                clearable
                @change="handleFilterChange"
              >
                <el-option label="可用" value="AVAILABLE" />
                <el-option label="已占用" value="OCCUPIED" />
                <el-option label="维护中" value="MAINTENANCE" />
                <el-option label="清洁中" value="CLEANING" />
              </el-select>
            </el-form-item>

            <el-form-item label="操作类型">
              <el-select
                v-model="filters.action"
                placeholder="选择操作类型"
                clearable
                @change="handleFilterChange"
              >
                <el-option label="状态变更" value="STATUS_CHANGE" />
                <el-option label="预订" value="BOOKING" />
                <el-option label="取消" value="CANCELLATION" />
                <el-option label="手动设置" value="MANUAL" />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="applyFilters">应用筛选</el-button>
              <el-button @click="resetFilters">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </el-collapse-transition>

    <!-- 统计信息 -->
    <div class="statistics-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ statistics.totalChanges }}</div>
              <div class="stat-label">总变更次数</div>
            </div>
            <el-icon class="stat-icon"><TrendCharts /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ statistics.todayChanges }}</div>
              <div class="stat-label">今日变更</div>
            </div>
            <el-icon class="stat-icon"><Calendar /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ statistics.currentStatus }}</div>
              <div class="stat-label">当前状态</div>
            </div>
            <RoomStatusIndicator :status="currentRoom?.status" size="large" />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ statistics.avgResponseTime }}ms</div>
              <div class="stat-label">平均响应时间</div>
            </div>
            <el-icon class="stat-icon"><Timer /></el-icon>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 状态时间线 -->
    <el-card class="timeline-section">
      <template #header>
        <div class="timeline-header">
          <span>状态变更时间线</span>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="timeline">时间线</el-radio-button>
            <el-radio-button label="table">表格</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 时间线视图 -->
      <div v-if="viewMode === 'timeline'" class="timeline-view">
        <el-timeline>
          <el-timeline-item
            v-for="log in paginatedLogs"
            :key="log.id"
            :timestamp="formatDateTime(log.createdAt)"
            :type="getTimelineType(log.newStatus)"
            :icon="getTimelineIcon(log.newStatus)"
            placement="top"
          >
            <div class="timeline-content">
              <div class="timeline-header">
                <span class="status-change">
                  {{ getStatusText(log.oldStatus) }}
                  <el-icon><Right /></el-icon>
                  {{ getStatusText(log.newStatus) }}
                </span>
                <RoomStatusIndicator :status="log.newStatus" size="small" />
              </div>
              <div class="timeline-body">
                <p class="reason">{{ log.reason }}</p>
                <div class="meta-info">
                  <span v-if="log.orderId" class="order-link">
                    <el-icon><Document /></el-icon>
                    订单 #{{ log.orderId }}
                  </span>
                  <span class="operator">
                    <el-icon><User /></el-icon>
                    操作员 ID: {{ log.changedBy }}
                  </span>
                  <span class="timestamp">
                    {{ formatRelativeTime(log.createdAt) }}
                  </span>
                </div>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>

        <!-- 空状态 -->
        <el-empty v-if="paginatedLogs.length === 0" description="暂无状态变更记录" />
      </div>

      <!-- 表格视图 -->
      <div v-else class="table-view">
        <el-table
          :data="paginatedLogs"
          v-loading="loading"
          stripe
          highlight-current-row
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="状态变更" width="200">
            <template #default="{ row }">
              <div class="status-change-cell">
                <RoomStatusIndicator :status="row.oldStatus" size="small" />
                <el-icon><Right /></el-icon>
                <RoomStatusIndicator :status="row.newStatus" size="small" />
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="变更原因" min-width="200" />
          <el-table-column label="关联订单" width="120">
            <template #default="{ row }">
              <el-link
                v-if="row.orderId"
                :href="`/orders/${row.orderId}`"
                type="primary"
                target="_blank"
              >
                #{{ row.orderId }}
              </el-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="changedBy" label="操作员" width="100" />
          <el-table-column label="操作时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="showLogDetail(row)">
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="filteredLogs.length"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 日志详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="日志详情"
      width="600px"
    >
      <div v-if="selectedLog" class="log-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="日志ID">{{ selectedLog.id }}</el-descriptions-item>
          <el-descriptions-item label="房间ID">{{ selectedLog.roomId }}</el-descriptions-item>
          <el-descriptions-item label="原状态">
            <RoomStatusIndicator :status="selectedLog.oldStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="新状态">
            <RoomStatusIndicator :status="selectedLog.newStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="变更原因" :span="2">{{ selectedLog.reason }}</el-descriptions-item>
          <el-descriptions-item label="关联订单" :span="1">
            <el-link
              v-if="selectedLog.orderId"
              :href="`/orders/${selectedLog.orderId}`"
              type="primary"
            >
              #{{ selectedLog.orderId }}
            </el-link>
            <span v-else>无</span>
          </el-descriptions-item>
          <el-descriptions-item label="操作员">{{ selectedLog.changedBy }}</el-descriptions-item>
          <el-descriptions-item label="操作时间" :span="2">{{ formatDateTime(selectedLog.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Clock,
  Refresh,
  Filter,
  Download,
  Right,
  Document,
  User,
  Timer,
  TrendCharts,
  Calendar
} from '@element-plus/icons-vue';
import { useRoomStore } from '@/stores/roomStore';
import { roomStatusService } from '@/services/roomStatusService';
import RoomStatusIndicator from '@/components/business/RoomStatusIndicator.vue';
import type { RoomStatusLog } from '@/services/roomStatusService';
import type { Room } from '@/types/room';

const route = useRoute();
const roomStore = useRoomStore();

// 响应式数据
const loading = ref(false);
const showFilters = ref(false);
const showDetailDialog = ref(false);
const selectedLog = ref<RoomStatusLog | null>(null);
const viewMode = ref<'timeline' | 'table'>('timeline');
const dateRange = ref<[string, string] | null>(null);

// 房间信息
const roomId = computed(() => Number(route.params.roomId));
const currentRoom = ref<Room | null>(null);
const roomInfo = computed(() => currentRoom.value);

// 筛选器
const filters = ref({
  status: '',
  action: '',
  startDate: '',
  endDate: ''
});

// 分页
const pagination = ref({
  page: 1,
  size: 20
});

// 日志数据
const allLogs = ref<RoomStatusLog[]>([]);
const filteredLogs = computed(() => {
  let logs = [...allLogs.value];

  // 状态筛选
  if (filters.value.status) {
    logs = logs.filter(log =>
      log.newStatus === filters.value.status ||
      log.oldStatus === filters.value.status
    );
  }

  // 操作类型筛选
  if (filters.value.action) {
    logs = logs.filter(log => {
      const reason = log.reason.toLowerCase();
      switch (filters.value.action) {
        case 'BOOKING':
          return reason.includes('预订') || reason.includes('booking');
        case 'CANCELLATION':
          return reason.includes('取消') || reason.includes('cancellation');
        case 'MANUAL':
          return reason.includes('手动') || reason.includes('manual');
        default:
          return true;
      }
    });
  }

  // 时间范围筛选
  if (filters.value.startDate && filters.value.endDate) {
    const startTime = new Date(filters.value.startDate).getTime();
    const endTime = new Date(filters.value.endDate).getTime();
    logs = logs.filter(log => {
      const logTime = new Date(log.createdAt).getTime();
      return logTime >= startTime && logTime <= endTime;
    });
  }

  return logs;
});

const paginatedLogs = computed(() => {
  const start = (pagination.value.page - 1) * pagination.value.size;
  const end = start + pagination.value.size;
  return filteredLogs.value.slice(start, end);
});

// 统计信息
const statistics = computed(() => {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const todayTime = today.getTime();

  const todayChanges = allLogs.value.filter(log =>
    new Date(log.createdAt).getTime() >= todayTime
  ).length;

  return {
    totalChanges: allLogs.value.length,
    todayChanges,
    currentStatus: roomStore.getStatusDisplayText(currentRoom.value?.status || ''),
    avgResponseTime: Math.floor(Math.random() * 100) + 50 // 模拟数据
  };
});

// 方法
const fetchRoomInfo = async () => {
  try {
    const room = await roomStore.fetchRoom(roomId.value);
    currentRoom.value = room;
  } catch (error) {
    console.error('Failed to fetch room info:', error);
    ElMessage.error('获取房间信息失败');
  }
};

const fetchLogs = async () => {
  loading.value = true;
  try {
    const logs = await roomStore.fetchRoomStatusLogs(roomId.value, {
      page: 0,
      size: 1000 // 获取所有日志用于本地筛选
    });

    if (logs) {
      allLogs.value = logs.records;
    }
  } catch (error) {
    console.error('Failed to fetch status logs:', error);
    ElMessage.error('获取状态日志失败');
  } finally {
    loading.value = false;
  }
};

const refreshLogs = async () => {
  await Promise.all([
    fetchRoomInfo(),
    fetchLogs()
  ]);
};

const handleDateRangeChange = (dates: [string, string] | null) => {
  if (dates) {
    filters.value.startDate = dates[0];
    filters.value.endDate = dates[1];
  } else {
    filters.value.startDate = '';
    filters.value.endDate = '';
  }
};

const handleFilterChange = () => {
  // 筛选条件变化时重置分页
  pagination.value.page = 1;
};

const applyFilters = () => {
  handleFilterChange();
  ElMessage.success('筛选条件已应用');
};

const resetFilters = () => {
  filters.value = {
    status: '',
    action: '',
    startDate: '',
    endDate: ''
  };
  dateRange.value = null;
  pagination.value.page = 1;
  ElMessage.success('筛选条件已重置');
};

const handleSizeChange = (size: number) => {
  pagination.value.size = size;
  pagination.value.page = 1;
};

const handlePageChange = (page: number) => {
  pagination.value.page = page;
};

const showLogDetail = (log: RoomStatusLog) => {
  selectedLog.value = log;
  showDetailDialog.value = true;
};

const exportLogs = () => {
  // 导出逻辑
  const csvContent = generateCSV(filteredLogs.value);
  downloadFile(csvContent, `room-status-logs-${roomId.value}-${new Date().toISOString().split('T')[0]}.csv`);
  ElMessage.success('日志导出成功');
};

const generateCSV = (logs: RoomStatusLog[]): string => {
  const headers = ['ID', '房间ID', '原状态', '新状态', '变更原因', '关联订单', '操作员', '操作时间'];
  const rows = logs.map(log => [
    log.id,
    log.roomId,
    log.oldStatus,
    log.newStatus,
    log.reason,
    log.orderId || '',
    log.changedBy,
    log.createdAt
  ]);

  return [headers, ...rows]
    .map(row => row.map(cell => `"${cell}"`).join(','))
    .join('\n');
};

const downloadFile = (content: string, filename: string) => {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
};

// 辅助函数
const formatDateTime = (dateStr: string): string => {
  return new Date(dateStr).toLocaleString('zh-CN');
};

const formatRelativeTime = (dateStr: string): string => {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / (1000 * 60));
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffMins < 1) return '刚刚';
  if (diffMins < 60) return `${diffMins}分钟前`;
  if (diffHours < 24) return `${diffHours}小时前`;
  if (diffDays < 7) return `${diffDays}天前`;
  return formatDateTime(dateStr);
};

const getStatusText = (status: string): string => {
  return roomStore.getStatusDisplayText(status);
};

const getTimelineType = (status: string): string => {
  const colorMap: Record<string, string> = {
    'AVAILABLE': 'success',
    'OCCUPIED': 'danger',
    'MAINTENANCE': 'warning',
    'CLEANING': 'info'
  };
  return colorMap[status] || 'primary';
};

const getTimelineIcon = (status: string) => {
  // 返回对应的图标组件
  const iconMap: Record<string, any> = {
    'AVAILABLE': 'Check',
    'OCCUPIED': 'Close',
    'MAINTENANCE': 'Tools',
    'CLEANING': 'Loading'
  };
  return iconMap[status] || 'Clock';
};

// 生命周期
onMounted(async () => {
  await refreshLogs();
});

// 监听路由参数变化
watch(
  () => route.params.roomId,
  async (newRoomId) => {
    if (newRoomId) {
      await refreshLogs();
    }
  }
);
</script>

<style scoped>
.room-status-logs {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.room-info {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: normal;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.filters-section {
  margin-bottom: 20px;
}

.statistics-section {
  margin-bottom: 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: var(--el-color-primary);
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.stat-icon {
  font-size: 48px;
  color: var(--el-color-primary-light-5);
  opacity: 0.3;
}

.timeline-section {
  min-height: 400px;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.timeline-content {
  padding-left: 20px;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.status-change {
  font-weight: 600;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.timeline-body .reason {
  margin: 8px 0;
  color: var(--el-text-color-regular);
}

.meta-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.meta-info span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.table-view {
  margin-top: 20px;
}

.status-change-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.log-detail {
  padding: 20px 0;
}

@media (max-width: 768px) {
  .room-status-logs {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .header-actions {
    justify-content: center;
  }

  .statistics-section :deep(.el-col) {
    margin-bottom: 12px;
  }

  .timeline-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .meta-info {
    flex-direction: column;
    gap: 8px;
  }
}
</style>