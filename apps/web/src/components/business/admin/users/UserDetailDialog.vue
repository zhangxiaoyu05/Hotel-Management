<template>
  <el-dialog
    v-model="dialogVisible"
    :title="`用户详情 - ${userDetail?.username || ''}`"
    width="800px"
    :close-on-click-modal="false"
    @open="handleOpen"
  >
    <div v-loading="loading" class="user-detail-content">
      <!-- 用户基本信息 -->
      <div class="detail-section" v-if="userDetail">
        <h3 class="section-title">基本信息</h3>

        <el-row :gutter="20">
          <el-col :span="8" class="avatar-col">
            <div class="avatar-section">
              <el-avatar
                :src="UserManagementService.getUserAvatarUrl(userDetail.avatar)"
                :size="120"
                class="user-avatar"
              >
                {{ userDetail.username.charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="avatar-info">
                <h4>{{ userDetail.username }}</h4>
                <p>{{ userDetail.nickname || '未设置昵称' }}</p>
              </div>
            </div>
          </el-col>

          <el-col :span="16">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="用户ID">
                {{ userDetail.id }}
              </el-descriptions-item>
              <el-descriptions-item label="用户名">
                {{ userDetail.username }}
              </el-descriptions-item>
              <el-descriptions-item label="邮箱">
                <el-tooltip :content="userDetail.email" placement="top">
                  <span>{{ UserManagementService.maskEmail(userDetail.email) }}</span>
                </el-tooltip>
              </el-descriptions-item>
              <el-descriptions-item label="手机号">
                {{ UserManagementService.maskPhone(userDetail.phone) }}
              </el-descriptions-item>
              <el-descriptions-item label="真实姓名">
                {{ userDetail.realName || '未填写' }}
              </el-descriptions-item>
              <el-descriptions-item label="性别">
                {{ formatGender(userDetail.gender) }}
              </el-descriptions-item>
              <el-descriptions-item label="出生日期">
                {{ userDetail.birthDate || '未填写' }}
              </el-descriptions-item>
              <el-descriptions-item label="会员等级">
                <el-tag :type="getMemberLevelType(userDetail.memberLevel)">
                  {{ userDetail.memberLevel }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="角色">
                <el-tag :type="UserManagementService.formatUserRole(userDetail.role).type">
                  {{ UserManagementService.formatUserRole(userDetail.role).text }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="UserManagementService.formatUserStatus(userDetail.status).type">
                  {{ UserManagementService.formatUserStatus(userDetail.status).text }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="注册时间">
                {{ formatDate(userDetail.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="最后登录">
                {{ userDetail.lastLoginAt ? formatDate(userDetail.lastLoginAt) : '从未登录' }}
              </el-descriptions-item>
              <el-descriptions-item label="登录IP">
                {{ userDetail.lastLoginIp || '未知' }}
              </el-descriptions-item>
              <el-descriptions-item label="更新时间">
                {{ formatDate(userDetail.updatedAt) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
      </div>

      <!-- 统计信息 -->
      <div class="detail-section" v-if="userDetail">
        <h3 class="section-title">统计信息</h3>

        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <div class="stat-number">{{ userDetail.totalOrders }}</div>
                <div class="stat-label">订单数量</div>
              </div>
              <el-icon class="stat-icon"><Document /></el-icon>
            </el-card>
          </el-col>

          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <div class="stat-number">{{ userDetail.totalReviews }}</div>
                <div class="stat-label">评价数量</div>
              </div>
              <el-icon class="stat-icon"><Star /></el-icon>
            </el-card>
          </el-col>

          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <div class="stat-number">¥{{ formatMoney(userDetail.totalSpent) }}</div>
                <div class="stat-label">累计消费</div>
              </div>
              <el-icon class="stat-icon"><Money /></el-icon>
            </el-card>
          </el-col>

          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <div class="stat-number">{{ formatRating(userDetail.averageRating) }}</div>
                <div class="stat-label">平均评分</div>
              </div>
              <el-icon class="stat-icon"><TrendCharts /></el-icon>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 操作历史 -->
      <div class="detail-section">
        <div class="section-header">
          <h3 class="section-title">操作历史</h3>
          <el-button
            type="text"
            size="small"
            @click="loadOperationHistory"
            :loading="historyLoading"
          >
            刷新
          </el-button>
        </div>

        <el-table
          v-loading="historyLoading"
          :data="operationHistory"
          style="width: 100%"
          size="small"
          max-height="300"
        >
          <el-table-column prop="operationTime" label="操作时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.operationTime) }}
            </template>
          </el-table-column>

          <el-table-column prop="operation" label="操作类型" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="getOperationType(row.operation)">
                {{ UserManagementService.formatOperationType(row.operation) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="operatorUsername" label="操作者" width="120" />

          <el-table-column prop="details" label="操作详情" min-width="200" />

          <el-table-column prop="ipAddress" label="IP地址" width="130" />
        </el-table>

        <div class="pagination-wrapper" v-if="operationHistoryTotal > 0">
          <el-pagination
            v-model:current-page="historyPage + 1"
            v-model:page-size="historySize"
            :total="operationHistoryTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="handleHistorySizeChange"
            @current-change="handleHistoryPageChange"
            small
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button
          v-if="userDetail && userDetail.status === 'ACTIVE'"
          type="warning"
          @click="handleDisableUser"
        >
          禁用用户
        </el-button>
        <el-button
          v-if="userDetail && userDetail.status === 'INACTIVE'"
          type="success"
          @click="handleEnableUser"
        >
          启用用户
        </el-button>
        <el-button
          type="danger"
          @click="handleDeleteUser"
        >
          删除用户
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Star,
  Money,
  TrendCharts
} from '@element-plus/icons-vue'
import { useUserManagementStore } from '@/stores/userManagement'
import UserManagementService from '@/services/userManagementService'
import type { UserDetailDTO, UserOperationHistoryDTO } from '@/types/userManagement'

// Props
const props = defineProps<{
  modelValue: boolean
  userId: number
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'refresh': []
}>()

// Store
const userManagementStore = useUserManagementStore()

// 解构store方法
const {
  userDetail,
  operationHistory,
  loading,
  historyLoading,
  fetchUserDetail,
  fetchUserOperationHistory,
  updateUserStatus,
  deleteUser
} = userManagementStore

// 响应式数据
const operationHistoryTotal = ref(0)
const historyPage = ref(0)
const historySize = ref(20)

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 监听器
watch(
  () => props.userId,
  (newUserId) => {
    if (newUserId > 0 && dialogVisible.value) {
      handleOpen()
    }
  }
)

// 方法

/**
 * 对话框打开时加载数据
 */
const handleOpen = async () => {
  if (props.userId > 0) {
    await Promise.all([
      loadUserDetail(),
      loadOperationHistory()
    ])
  }
}

/**
 * 加载用户详情
 */
const loadUserDetail = async () => {
  try {
    await fetchUserDetail(props.userId)
  } catch (error) {
    console.error('加载用户详情失败:', error)
    ElMessage.error('加载用户详情失败')
  }
}

/**
 * 加载操作历史
 */
const loadOperationHistory = async (reset = true) => {
  try {
    if (reset) {
      historyPage.value = 0
    }

    const response = await fetchUserOperationHistory(
      props.userId,
      historyPage.value,
      reset
    )

    operationHistoryTotal.value = response.totalElements
  } catch (error) {
    console.error('加载操作历史失败:', error)
    ElMessage.error('加载操作历史失败')
  }
}

/**
 * 处理历史分页大小变化
 */
const handleHistorySizeChange = (newSize: number) => {
  historySize.value = newSize
  historyPage.value = 0
  loadOperationHistory()
}

/**
 * 处理历史页码变化
 */
const handleHistoryPageChange = (newPage: number) => {
  historyPage.value = newPage - 1
  loadOperationHistory(false)
}

/**
 * 启用用户
 */
const handleEnableUser = async () => {
  if (!userDetail.value) return

  try {
    await ElMessageBox.confirm(
      `确定要启用用户 "${userDetail.value.username}" 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await updateUserStatus(props.userId, 'ACTIVE', '管理员启用用户')
    ElMessage.success('用户启用成功')
    emit('refresh')

  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('启用用户失败')
    }
  }
}

/**
 * 禁用用户
 */
const handleDisableUser = async () => {
  if (!userDetail.value) return

  try {
    await ElMessageBox.confirm(
      `确定要禁用用户 "${userDetail.value.username}" 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await updateUserStatus(props.userId, 'INACTIVE', '管理员禁用用户')
    ElMessage.success('用户禁用成功')
    emit('refresh')

  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('禁用用户失败')
    }
  }
}

/**
 * 删除用户
 */
const handleDeleteUser = async () => {
  if (!userDetail.value) return

  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${userDetail.value.username}" 吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )

    await deleteUser(props.userId, '管理员删除用户')
    ElMessage.success('用户删除成功')
    handleClose()
    emit('refresh')

  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除用户失败')
    }
  }
}

/**
 * 关闭对话框
 */
const handleClose = () => {
  dialogVisible.value = false
}

// 辅助方法

/**
 * 格式化日期
 */
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

/**
 * 格式化金额
 */
const formatMoney = (amount: number) => {
  return (amount || 0).toFixed(2)
}

/**
 * 格式化评分
 */
const formatRating = (rating: number) => {
  return (rating || 0).toFixed(1)
}

/**
 * 格式化性别
 */
const formatGender = (gender?: string) => {
  const genderMap: Record<string, string> = {
    'MALE': '男',
    'FEMALE': '女',
    'OTHER': '其他'
  }
  return genderMap[gender || ''] || '未填写'
}

/**
 * 获取会员等级类型
 */
const getMemberLevelType = (level: string) => {
  const levelTypeMap: Record<string, string> = {
    '钻石会员': 'danger',
    '金牌会员': 'warning',
    '银牌会员': 'success',
    '铜牌会员': 'primary',
    '普通会员': 'info'
  }
  return levelTypeMap[level] || 'info'
}

/**
 * 获取操作类型标签样式
 */
const getOperationType = (operation: string) => {
  const typeMap: Record<string, string> = {
    'LOGIN': 'success',
    'LOGOUT': 'info',
    'STATUS_CHANGE': 'warning',
    'DELETE': 'danger',
    'PASSWORD_CHANGE': 'primary',
    'PROFILE_UPDATE': 'info'
  }
  return typeMap[operation] || 'info'
}
</script>

<style scoped>
.user-detail-content {
  padding: 0;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
}

.section-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background: #409eff;
  margin-right: 8px;
  border-radius: 2px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.avatar-col {
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 16px;
}

.avatar-section h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.avatar-section p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.stat-card {
  position: relative;
  overflow: hidden;
}

.stat-card .el-card__body {
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-icon {
  font-size: 28px;
  color: #ddd;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-descriptions__content) {
  color: #303133;
}

/* 统计卡片颜色 */
.stat-card:nth-child(1) .stat-number {
  color: #409eff;
}

.stat-card:nth-child(1) .stat-icon {
  color: #409eff;
}

.stat-card:nth-child(2) .stat-number {
  color: #f7ba2a;
}

.stat-card:nth-child(2) .stat-icon {
  color: #f7ba2a;
}

.stat-card:nth-child(3) .stat-number {
  color: #67c23a;
}

.stat-card:nth-child(3) .stat-icon {
  color: #67c23a;
}

.stat-card:nth-child(4) .stat-number {
  color: #e6a23c;
}

.stat-card:nth-child(4) .stat-icon {
  color: #e6a23c;
}
</style>