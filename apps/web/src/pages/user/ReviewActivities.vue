<template>
  <div class="review-activities">
    <div class="page-header">
      <h1>评价活动</h1>
      <el-button type="primary" @click="fetchActivities">
        <i class="el-icon-refresh"></i> 刷新
      </el-button>
    </div>

    <div class="activities-tabs">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="进行中" name="active">
          <div class="activities-grid" v-loading="loading">
            <activity-card
              v-for="activity in activeActivities"
              :key="activity.id"
              :activity="activity"
              @join="handleJoinActivity"
            />
          </div>
          <div class="empty-state" v-if="!loading && activeActivities.length === 0">
            <i class="el-icon-time"></i>
            <p>暂无进行中的活动</p>
          </div>
        </el-tab-pane>

        <el-tab-pane label="即将开始" name="upcoming">
          <div class="activities-grid" v-loading="loading">
            <activity-card
              v-for="activity in upcomingActivities"
              :key="activity.id"
              :activity="activity"
              :can-join="false"
            />
          </div>
          <div class="empty-state" v-if="!loading && upcomingActivities.length === 0">
            <i class="el-icon-date"></i>
            <p>暂无即将开始的活动</p>
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的参与" name="my-participations">
          <div class="participations-list" v-loading="loading">
            <el-timeline>
              <el-timeline-item
                v-for="participation in myParticipations"
                :key="participation.id"
                :timestamp="formatDateTime(participation.joinedAt)"
                :type="getTimelineType(participation.status)"
              >
                <div class="participation-item">
                  <h4>{{ getActivityTitle(participation.activityId) }}</h4>
                  <p class="status" :class="participation.status">
                    {{ getStatusText(participation.status) }}
                  </p>
                  <div class="rewards" v-if="participation.rewardPoints">
                    <el-tag type="success">获得 {{ participation.rewardPoints }} 积分</el-tag>
                  </div>
                  <div class="notes" v-if="participation.notes">
                    <small>{{ participation.notes }}</small>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>

            <div class="empty-state" v-if="!loading && myParticipations.length === 0">
              <i class="el-icon-user"></i>
              <p>您还没有参与任何活动</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import reviewService from '@/services/reviewService'
import ActivityCard from '@/components/business/review/ActivityCard.vue'

export default {
  name: 'ReviewActivities',
  components: {
    ActivityCard
  },
  setup() {
    const loading = ref(false)
    const activeTab = ref('active')
    const activeActivities = ref([])
    const upcomingActivities = ref([])
    const myParticipations = ref([])
    const allActivities = ref([]) // 缓存所有活动信息

    const fetchActivities = async () => {
      try {
        loading.value = true

        // 获取活跃和即将开始的活动
        const [activeRes, upcomingRes] = await Promise.all([
          reviewService.getActiveActivities(true),
          reviewService.getActiveActivities(false)
        ])

        if (activeRes.success) {
          activeActivities.value = activeRes.data
        }
        if (upcomingRes.success) {
          upcomingActivities.value = upcomingRes.data
        }

        // 缓存活动信息
        allActivities.value = [...activeActivities.value, ...upcomingActivities.value]

      } catch (error) {
        console.error('获取活动失败:', error)
        ElMessage.error('获取活动失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }

    const fetchMyParticipations = async () => {
      try {
        loading.value = true
        const response = await reviewService.getUserParticipations()

        if (response.success) {
          myParticipations.value = response.data
        } else {
          ElMessage.error(response.message || '获取参与记录失败')
        }
      } catch (error) {
        console.error('获取参与记录失败:', error)
        ElMessage.error('获取参与记录失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }

    const handleTabClick = (tab) => {
      if (tab.name === 'my-participations' && myParticipations.value.length === 0) {
        fetchMyParticipations()
      }
    }

    const handleJoinActivity = async (activity) => {
      try {
        await ElMessageBox.confirm(
          `确定要参与活动"${activity.title}"吗？`,
          '确认参与',
          {
            type: 'warning',
            confirmButtonText: '确定',
            cancelButtonText: '取消'
          }
        )

        const response = await reviewService.joinActivity(activity.id)

        if (response.success) {
          ElMessage.success(response.data.message || '参与成功')
          // 重新获取活动列表
          await fetchActivities()
        } else {
          ElMessage.error(response.message || '参与失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('参与活动失败:', error)
          ElMessage.error('参与失败，请稍后重试')
        }
      }
    }

    const getActivityTitle = (activityId) => {
      const activity = allActivities.value.find(a => a.id === activityId)
      return activity ? activity.title : '未知活动'
    }

    const getStatusText = (status) => {
      const statusMap = {
        'ACTIVE': '参与中',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消'
      }
      return statusMap[status] || status
    }

    const getTimelineType = (status) => {
      const typeMap = {
        'ACTIVE': 'primary',
        'COMPLETED': 'success',
        'CANCELLED': 'danger'
      }
      return typeMap[status] || 'info'
    }

    const formatDateTime = (dateTime) => {
      if (!dateTime) return ''
      return new Date(dateTime).toLocaleString('zh-CN')
    }

    onMounted(() => {
      fetchActivities()
    })

    return {
      loading,
      activeTab,
      activeActivities,
      upcomingActivities,
      myParticipations,
      fetchActivities,
      handleTabClick,
      handleJoinActivity,
      getActivityTitle,
      getStatusText,
      getTimelineType,
      formatDateTime
    }
  }
}
</script>

<style scoped>
.review-activities {
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

.activities-tabs {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
  padding: 20px;
}

.participations-list {
  padding: 20px;
}

.participation-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.participation-item h4 {
  margin: 0 0 8px 0;
  color: #303133;
}

.participation-item .status {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 8px;
}

.participation-item .status.ACTIVE {
  background: #e6f7ff;
  color: #1890ff;
}

.participation-item .status.COMPLETED {
  background: #f6ffed;
  color: #52c41a;
}

.participation-item .status.CANCELLED {
  background: #fff2f0;
  color: #ff4d4f;
}

.rewards {
  margin: 8px 0;
}

.notes {
  color: #909399;
  margin-top: 8px;
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

@media (max-width: 768px) {
  .activities-grid {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .participations-list {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
  }
}
</style>