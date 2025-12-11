<template>
  <div class="review-leaderboard">
    <div class="page-header">
      <h1>评价排行榜</h1>
      <div class="filters">
        <el-select v-model="periodType" @change="fetchLeaderboard">
          <el-option label="月度榜" value="monthly" />
          <el-option label="季度榜" value="quarterly" />
          <el-option label="年度榜" value="yearly" />
        </el-select>
        <el-date-picker
          v-model="period"
          type="month"
          placeholder="选择月份"
          @change="fetchLeaderboard"
        />
        <el-button @click="fetchLeaderboard" :loading="loading">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
      </div>
    </div>

    <div class="leaderboard-content" v-loading="loading">
      <div class="top-three" v-if="topThree.length">
        <div
          class="rank-item"
          v-for="(user, index) in topThree"
          :key="user.userId"
          :class="`rank-${index + 1}`"
        >
          <div class="medal">
            <i :class="medalIcons[index]"></i>
          </div>
          <div class="user-info">
            <div class="user-name">{{ user.userName }}</div>
            <div class="user-stats">
              <span>{{ user.totalReviews }} 条评价</span>
              <span>{{ user.totalPoints }} 分</span>
            </div>
          </div>
          <div class="rank-number">{{ user.rank }}</div>
        </div>
      </div>

      <div class="leaderboard-list">
        <el-table
          :data="leaderboardEntries"
          style="width: 100%"
          :row-class-name="getRowClassName"
        >
          <el-table-column prop="rank" label="排名" width="80" />
          <el-table-column prop="userName" label="用户" width="200" />
          <el-table-column prop="totalReviews" label="评价数量" width="120" />
          <el-table-column prop="qualityScore" label="质量分" width="100" />
          <el-table-column prop="totalPoints" label="总积分" width="100" />
          <el-table-column label="徽章">
            <template #default="{ row }">
              <el-tag
                v-if="row.qualityReviews > 0"
                type="success"
                size="small"
              >
                优质评价 x{{ row.qualityReviews }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="empty-state" v-if="!loading && leaderboardEntries.length === 0">
        <i class="el-icon-trophy"></i>
        <p>暂无排行数据</p>
      </div>
    </div>

    <div class="page-footer">
      <p class="update-time" v-if="lastUpdate">
        最后更新：{{ formatDateTime(lastUpdate) }}
      </p>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import reviewService from '@/services/reviewService'

export default {
  name: 'ReviewLeaderboard',
  setup() {
    const loading = ref(false)
    const periodType = ref('monthly')
    const period = ref(new Date())
    const leaderboardData = ref({})
    const lastUpdate = ref(null)

    const topThree = computed(() => leaderboardData.value.entries?.slice(0, 3) || [])
    const leaderboardEntries = computed(() => leaderboardData.value.entries || [])

    const medalIcons = ['el-icon-trophy', 'el-icon-medal', 'el-icon-medal']

    const fetchLeaderboard = async () => {
      try {
        loading.value = true
        const periodStr = formatPeriod(period.value, periodType.value)
        const response = await reviewService.getLeaderboard(periodType.value, periodStr)

        if (response.success) {
          leaderboardData.value = response.data
          lastUpdate.value = response.data.updatedAt
        } else {
          ElMessage.error(response.message || '获取排行榜失败')
        }
      } catch (error) {
        console.error('获取排行榜失败:', error)
        ElMessage.error('获取排行榜失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }

    const formatPeriod = (date, type) => {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')

      switch (type) {
        case 'monthly':
          return `${year}-${month}`
        case 'quarterly':
          const quarter = Math.floor(date.getMonth() / 3) + 1
          return `${year}-Q${quarter}`
        case 'yearly':
          return `${year}`
        default:
          return `${year}-${month}`
      }
    }

    const formatDateTime = (dateTime) => {
      if (!dateTime) return ''
      return new Date(dateTime).toLocaleString('zh-CN')
    }

    const getRowClassName = ({ row }) => {
      if (row.rank <= 3) return 'top-rank-row'
      return ''
    }

    onMounted(() => {
      fetchLeaderboard()
    })

    return {
      loading,
      periodType,
      period,
      leaderboardData,
      topThree,
      leaderboardEntries,
      medalIcons,
      lastUpdate,
      fetchLeaderboard,
      formatDateTime,
      getRowClassName
    }
  }
}
</script>

<style scoped>
.review-leaderboard {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
}

.filters {
  display: flex;
  gap: 12px;
}

.filters .el-select {
  width: 120px;
}

.top-three {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 40px;
}

.rank-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-width: 200px;
  position: relative;
}

.rank-1 {
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: white;
}

.rank-2 {
  background: linear-gradient(135deg, #C0C0C0, #B8B8B8);
  color: white;
}

.rank-3 {
  background: linear-gradient(135deg, #CD7F32, #B87333);
  color: white;
}

.medal {
  font-size: 48px;
  margin-bottom: 12px;
}

.user-info {
  text-align: center;
  flex: 1;
}

.user-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 8px;
}

.user-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  opacity: 0.9;
}

.rank-number {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 24px;
  font-weight: bold;
}

.leaderboard-list {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-state i {
  font-size: 64px;
  margin-bottom: 16px;
  display: block;
}

.page-footer {
  margin-top: 20px;
  text-align: center;
}

.update-time {
  color: #909399;
  font-size: 14px;
}

:deep(.top-rank-row) {
  background-color: #f5f7fa;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }

  .filters {
    flex-wrap: wrap;
  }

  .top-three {
    flex-direction: column;
    align-items: center;
    gap: 20px;
  }

  .rank-item {
    width: 100%;
    max-width: 300px;
  }
}
</style>