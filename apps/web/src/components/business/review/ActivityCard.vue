<template>
  <div class="activity-card" :class="{ 'disabled': !canJoin }">
    <div class="card-header">
      <h3>{{ activity.title }}</h3>
      <el-tag :type="getTagType(activity.status)" size="small">
        {{ getStatusText(activity.status) }}
      </el-tag>
    </div>

    <div class="card-body">
      <p class="description">{{ activity.description }}</p>

      <div class="activity-info">
        <div class="info-item">
          <i class="el-icon-collection-tag"></i>
          <span>{{ getActivityTypeText(activity.activityType) }}</span>
        </div>

        <div class="info-item">
          <i class="el-icon-time"></i>
          <span>{{ formatDateRange(activity.startDate, activity.endDate) }}</span>
        </div>
      </div>

      <div class="rewards" v-if="hasRewards">
        <h4>奖励说明</h4>
        <div class="reward-list">
          <div class="reward-item" v-for="(value, key) in activity.rules" :key="key">
            <span class="reward-key">{{ getRewardKeyText(key) }}:</span>
            <span class="reward-value">{{ formatRewardValue(key, value) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="card-footer">
      <el-button
        v-if="canJoin"
        type="primary"
        @click="$emit('join', activity)"
        :disabled="!canJoinActivity"
      >
        {{ getJoinButtonText() }}
      </el-button>
      <el-button v-else disabled>
        {{ getJoinButtonText() }}
      </el-button>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'ActivityCard',
  props: {
    activity: {
      type: Object,
      required: true
    },
    canJoin: {
      type: Boolean,
      default: true
    }
  },
  emits: ['join'],

  setup(props) {
    const hasRewards = computed(() => {
      return props.activity.rules && Object.keys(props.activity.rules).length > 0
    })

    const canJoinActivity = computed(() => {
      return props.activity.status === 'ACTIVE' && props.canJoin
    })

    const getTagType = (status) => {
      const typeMap = {
        'ACTIVE': 'success',
        'UPCOMING': 'warning',
        'ENDED': 'info'
      }
      return typeMap[status] || 'info'
    }

    const getStatusText = (status) => {
      const statusMap = {
        'ACTIVE': '进行中',
        'UPCOMING': '即将开始',
        'ENDED': '已结束'
      }
      return statusMap[status] || status
    }

    const getActivityTypeText = (type) => {
      const typeMap = {
        'DOUBLE_POINTS': '双倍积分',
        'REVIEW_CONTEST': '评价竞赛',
        'MONTHLY_CHAMPION': '月度冠军'
      }
      return typeMap[type] || type
    }

    const formatDateRange = (startDate, endDate) => {
      if (!startDate || !endDate) return ''

      const start = new Date(startDate)
      const end = new Date(endDate)

      const formatDate = (date) => {
        return date.toLocaleDateString('zh-CN', {
          month: 'long',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        })
      }

      return `${formatDate(start)} - ${formatDate(end)}`
    }

    const getRewardKeyText = (key) => {
      const keyMap = {
        'multiplier': '积分倍数',
        'maxReviews': '最大评价数',
        'participationReward': '参与奖励',
        'winnerReward': '优胜奖励',
        'championReward': '冠军奖励',
        'runnerUpReward': '亚军奖励'
      }
      return keyMap[key] || key
    }

    const formatRewardValue = (key, value) => {
      if (key.includes('Reward')) {
        return `${value} 积分`
      }
      if (key === 'multiplier') {
        return `${value} 倍`
      }
      if (key === 'maxReviews') {
        return `${value} 条`
      }
      return value
    }

    const getJoinButtonText = () => {
      if (!props.canJoin) {
        if (props.activity.status === 'UPCOMING') {
          return '即将开始'
        }
        if (props.activity.status === 'ENDED') {
          return '已结束'
        }
        return '不可参与'
      }

      if (props.activity.status === 'ACTIVE') {
        return '立即参与'
      }

      return '查看详情'
    }

    return {
      hasRewards,
      canJoinActivity,
      getTagType,
      getStatusText,
      getActivityTypeText,
      formatDateRange,
      getRewardKeyText,
      formatRewardValue,
      getJoinButtonText
    }
  }
}
</script>

<style scoped>
.activity-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.activity-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.activity-card.disabled {
  opacity: 0.8;
}

.card-header {
  padding: 20px 20px 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
  flex: 1;
  margin-right: 12px;
}

.card-body {
  padding: 16px 20px;
}

.description {
  color: #606266;
  margin: 8px 0 16px 0;
  line-height: 1.6;
}

.activity-info {
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  color: #909399;
  font-size: 14px;
}

.info-item i {
  margin-right: 8px;
  font-size: 16px;
}

.rewards {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
}

.rewards h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #606266;
}

.reward-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reward-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.reward-key {
  color: #909399;
}

.reward-value {
  color: #67c23a;
  font-weight: bold;
}

.card-footer {
  padding: 0 20px 20px;
}

.card-footer .el-button {
  width: 100%;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    gap: 8px;
  }

  .card-header h3 {
    margin-right: 0;
  }

  .reward-item {
    flex-direction: column;
    gap: 2px;
  }
}
</style>