<template>
  <div class="quick-actions">
    <el-card class="actions-card" shadow="hover">
      <template #header>
        <span class="card-title">
          <el-icon><Operation /></el-icon>
          快速操作
        </span>
      </template>
      <div class="actions-grid">
        <div
          v-for="action in quickActions"
          :key="action.key"
          class="action-item"
          :class="{ 'action-disabled': action.disabled }"
          @click="handleActionClick(action)"
        >
          <div class="action-icon" :style="{ backgroundColor: action.color + '20' }">
            <el-icon :size="20" :color="action.color">
              <component :is="action.icon" />
            </el-icon>
          </div>
          <div class="action-content">
            <div class="action-title">{{ action.title }}</div>
            <div class="action-desc">{{ action.description }}</div>
          </div>
          <div class="action-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
          <!-- 徽章显示 -->
          <div v-if="action.badge && action.badge > 0" class="action-badge">
            {{ action.badge > 99 ? '99+' : action.badge }}
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Operation,
  User,
  House,
  Tickets,
  Money,
  Document,
  DataLine,
  Setting,
  Upload,
  Download,
  ArrowRight,
  Bell,
  Calendar,
  Tools,
  Plus,
  Edit,
  Search
} from '@element-plus/icons-vue'

interface QuickAction {
  key: string
  title: string
  description: string
  icon: any
  color: string
  route?: string
  disabled?: boolean
  badge?: number
  onClick?: () => void
  type?: 'navigation' | 'action' | 'modal'
}

const router = useRouter()

// 响应式数据
const pendingOrdersCount = ref(0)
const pendingReviewsCount = ref(0)
const systemNotifications = ref(0)

const quickActions = computed<QuickAction[]>(() => [
  {
    key: 'add-order',
    title: '创建订单',
    description: '手动创建新订单',
    icon: Plus,
    color: '#1890ff',
    onClick: () => handleCreateOrder(),
    type: 'action'
  },
  {
    key: 'orders',
    title: '订单管理',
    description: '查看和管理订单',
    icon: Tickets,
    color: '#faad14',
    route: '/admin/orders',
    badge: pendingOrdersCount.value,
    type: 'navigation'
  },
  {
    key: 'rooms',
    title: '房间管理',
    description: '管理房间信息',
    icon: House,
    color: '#52c41a',
    route: '/admin/rooms',
    type: 'navigation'
  },
  {
    key: 'checkin',
    title: '快速入住',
    description: '快速办理入住',
    icon: Calendar,
    color: '#722ed1',
    onClick: () => handleQuickCheckIn(),
    type: 'action'
  },
  {
    key: 'users',
    title: '用户管理',
    description: '管理系统用户',
    icon: User,
    color: '#1890ff',
    route: '/admin/users',
    type: 'navigation'
  },
  {
    key: 'reviews',
    title: '评价管理',
    description: '管理用户评价',
    icon: Document,
    color: '#f5222d',
    route: '/admin/reviews',
    badge: pendingReviewsCount.value,
    type: 'navigation'
  },
  {
    key: 'pricing',
    title: '定价策略',
    description: '设置房间价格',
    icon: Money,
    color: '#722ed1',
    route: '/admin/pricing',
    type: 'navigation'
  },
  {
    key: 'maintenance',
    title: '维护模式',
    description: '设置维护状态',
    icon: Tools,
    color: '#6b7280',
    onClick: () => handleMaintenanceMode(),
    type: 'action'
  },
  {
    key: 'notifications',
    title: '系统通知',
    description: '查看系统通知',
    icon: Bell,
    color: '#ff7a45',
    badge: systemNotifications.value,
    onClick: () => handleNotifications(),
    type: 'modal'
  },
  {
    key: 'export',
    title: '数据导出',
    description: '导出业务数据',
    icon: Download,
    color: '#13c2c2',
    onClick: () => handleExport(),
    type: 'modal'
  }
])

const handleActionClick = (action: QuickAction) => {
  if (action.disabled) return

  if (action.onClick) {
    action.onClick()
  } else if (action.route) {
    router.push(action.route)
  }
}

// 方法实现
const handleCreateOrder = () => {
  ElMessage.info('正在开发创建订单功能...')
}

const handleQuickCheckIn = () => {
  ElMessage.info('正在开发快速入住功能...')
}

const handleMaintenanceMode = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要进入维护模式吗？这将影响用户访问。',
      '维护模式',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    ElMessage.success('维护模式已启用')
  } catch {
    // 用户取消
  }
}

const handleNotifications = () => {
  ElMessage.info('正在开发系统通知功能...')
}

const handleExport = async () => {
  try {
    const { value } = await ElMessageBox.prompt(
      '请选择导出格式',
      '数据导出',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'select',
        inputOptions: [
          { value: 'csv', label: 'CSV格式' },
          { value: 'excel', label: 'Excel格式' },
          { value: 'json', label: 'JSON格式' }
        ],
        inputValue: 'csv'
      }
    )

    if (value) {
      // 模拟导出过程
      ElMessage.info(`正在导出${value.toUpperCase()}格式数据...`)
      setTimeout(() => {
        ElMessage.success('数据导出成功！')
      }, 2000)
    }
  } catch {
    // 用户取消
  }
}

// 获取徽章数据
const fetchBadgeData = async () => {
  try {
    // 这里应该调用API获取实际数据
    // const response = await api.getDashboardBadges()
    // pendingOrdersCount.value = response.pendingOrders
    // pendingReviewsCount.value = response.pendingReviews
    // systemNotifications.value = response.notifications

    // 模拟数据
    pendingOrdersCount.value = Math.floor(Math.random() * 10)
    pendingReviewsCount.value = Math.floor(Math.random() * 5)
    systemNotifications.value = Math.floor(Math.random() * 3)
  } catch (error) {
    console.error('获取徽章数据失败:', error)
  }
}

// 生命周期
onMounted(() => {
  fetchBadgeData()
})
</script>

<style scoped>
.quick-actions {
  margin-bottom: 24px;
}

.actions-card {
  margin-bottom: 0;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.action-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.action-item:hover {
  border-color: #1890ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-item.action-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-item.action-disabled:hover {
  border-color: #e5e7eb;
  transform: none;
  box-shadow: none;
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-content {
  flex: 1;
  margin-left: 12px;
  margin-right: 8px;
}

.action-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 2px;
}

.action-desc {
  font-size: 12px;
  color: #6b7280;
}

.action-arrow {
  flex-shrink: 0;
  color: #9ca3af;
  transition: transform 0.3s ease;
}

.action-item:hover .action-arrow {
  transform: translateX(4px);
  color: #1890ff;
}

/* 徽章样式 */
.action-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #ff4d4f;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 11px;
  font-weight: 600;
  min-width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  box-shadow: 0 2px 4px rgba(255, 77, 79, 0.3);
  z-index: 1;
}

@media (max-width: 768px) {
  .actions-grid {
    grid-template-columns: 1fr;
  }

  .action-item {
    padding: 12px;
  }

  .action-icon {
    width: 40px;
    height: 40px;
  }

  .action-icon .el-icon {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .action-content {
    margin-left: 8px;
    margin-right: 4px;
  }

  .action-title {
    font-size: 13px;
  }

  .action-desc {
    font-size: 11px;
  }
}
</style>