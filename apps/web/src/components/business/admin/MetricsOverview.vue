<template>
  <div class="metrics-overview">
    <el-row :gutter="24">
      <el-col
        :xs="24"
        :sm="12"
        :md="6"
        v-for="(metric, index) in metrics"
        :key="index"
      >
        <el-card class="metric-card" shadow="hover">
          <div class="metric-content">
            <div class="metric-icon" :style="{ backgroundColor: metric.color + '20' }">
              <el-icon :size="24" :color="metric.color">
                <component :is="metric.icon" />
              </el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metric.value }}</div>
              <div class="metric-label">{{ metric.label }}</div>
              <div class="metric-trend" v-if="metric.trend">
                <el-tag
                  :type="metric.trend.type"
                  size="small"
                  effect="plain"
                  :icon="metric.trend.up ? ArrowUp : ArrowDown"
                >
                  {{ metric.trend.value }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import { useDashboardStore } from '@/stores/dashboard'

const dashboardStore = useDashboardStore()

const metrics = computed(() => {
  const metricsData = dashboardStore.metrics
  if (!metricsData) return []

  return [
    {
      label: '今日订单',
      value: metricsData.todayOrdersCount,
      icon: 'Document',
      color: '#1890ff',
      trend: {
        up: true,
        value: '+12.5%',
        type: 'success'
      }
    },
    {
      label: '今日收入',
      value: `¥${metricsData.todayRevenue}`,
      icon: 'Money',
      color: '#52c41a',
      trend: {
        up: true,
        value: '+8.3%',
        type: 'success'
      }
    },
    {
      label: '入住率',
      value: `${metricsData.occupancyRate}%`,
      icon: 'House',
      color: '#faad14',
      trend: {
        up: false,
        value: '-2.1%',
        type: 'danger'
      }
    },
    {
      label: '活跃用户',
      value: metricsData.totalActiveUsers,
      icon: 'User',
      color: '#722ed1',
      trend: {
        up: true,
        value: '+5.7%',
        type: 'success'
      }
    },
    {
      label: '总房间数',
      value: metricsData.totalRooms,
      icon: 'HomeFilled',
      color: '#13c2c2',
      trend: null
    },
    {
      label: '平均评分',
      value: metricsData.averageRating.toFixed(1),
      icon: 'Star',
      color: '#fadb14',
      trend: {
        up: true,
        value: '+0.2',
        type: 'success'
      }
    },
    {
      label: '待审核评价',
      value: metricsData.pendingReviewsCount,
      icon: 'ChatDotRound',
      color: '#ff4d4f',
      trend: null
    },
    {
      label: '今日新增用户',
      value: metricsData.todayNewUsers,
      icon: 'UserFilled',
      color: '#52c41a',
      trend: {
        up: true,
        value: '+18.2%',
        type: 'success'
      }
    }
  ]
})
</script>

<style scoped>
.metrics-overview {
  margin-bottom: 24px;
}

.metric-card {
  height: 120px;
  margin-bottom: 24px;
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.metric-content {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 16px;
  padding: 8px;
}

.metric-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.metric-value {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-label {
  font-size: 14px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-trend {
  margin-top: auto;
  align-self: flex-start;
}

@media (max-width: 576px) {
  .metric-value {
    font-size: 24px;
  }

  .metric-icon {
    width: 56px;
    height: 56px;
  }

  .metric-icon .el-icon {
    font-size: 20px;
  }
}
</style>