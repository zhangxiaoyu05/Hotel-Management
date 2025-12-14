<template>
  <div class="user-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>用户管理</h1>
      <p>管理系统中的所有用户账户，支持搜索、筛选、批量操作等功能</p>

      <!-- 统计信息 -->
      <div class="statistics-cards" v-if="statistics">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <div class="stat-number">{{ statistics.totalUsers }}</div>
                <div class="stat-label">总用户数</div>
              </div>
              <el-icon class="stat-icon"><User /></el-icon>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card active">
              <div class="stat-content">
                <div class="stat-number">{{ statistics.activeUsers }}</div>
                <div class="stat-label">活跃用户</div>
              </div>
              <el-icon class="stat-icon"><UserFilled /></el-icon>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card inactive">
              <div class="stat-content">
                <div class="stat-number">{{ statistics.inactiveUsers }}</div>
                <div class="stat-label">禁用用户</div>
              </div>
              <el-icon class="stat-icon"><CircleClose /></el-icon>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card today">
              <div class="stat-content">
                <div class="stat-number">{{ statistics.newUsersToday }}</div>
                <div class="stat-label">今日新增</div>
              </div>
              <el-icon class="stat-icon"><Plus /></el-icon>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>

    <!-- 操作工具栏 -->
    <div class="toolbar-section">
      <div class="toolbar-left">
        <el-button
          type="primary"
          :icon="Search"
          @click="showSearchDialog = true"
        >
          高级搜索
        </el-button>
        <el-button
          :icon="Refresh"
          @click="handleRefresh"
          :loading="loading"
        >
          刷新
        </el-button>
      </div>

      <div class="toolbar-right" v-if="selectedUserIds.length > 0">
        <span class="selection-info">
          已选择 {{ selectedUserIds.length }} 个用户
        </span>
        <el-button
          type="success"
          size="small"
          @click="handleBatchEnable"
          :loading="batchOperationLoading"
        >
          批量启用
        </el-button>
        <el-button
          type="warning"
          size="small"
          @click="handleBatchDisable"
          :loading="batchOperationLoading"
        >
          批量禁用
        </el-button>
        <el-button
          type="danger"
          size="small"
          @click="handleBatchDelete"
          :loading="batchOperationLoading"
        >
          批量删除
        </el-button>
        <el-button
          size="small"
          @click="clearSelection"
        >
          取消选择
        </el-button>
      </div>
    </div>

    <!-- 用户列表表格 -->
    <div class="table-section">
      <el-table
        v-loading="loading"
        :data="userList"
        style="width: 100%"
        stripe
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />

        <el-table-column prop="id" label="ID" width="80" sortable />

        <el-table-column label="用户信息" min-width="200">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar
                :src="UserManagementService.getUserAvatarUrl(row.avatar)"
                :size="40"
                class="user-avatar"
              >
                {{ row.username.charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-details">
                <div class="username">{{ row.username }}</div>
                <div class="nickname">{{ row.nickname || '未设置昵称' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">
            <span :title="row.email">
              {{ UserManagementService.maskEmail(row.email) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="phone" label="手机号" min-width="130">
          <template #default="{ row }">
            {{ UserManagementService.maskPhone(row.phone) }}
          </template>
        </el-table-column>

        <el-table-column prop="role" label="角色" width="100" sortable>
          <template #default="{ row }">
            <el-tag :type="UserManagementService.formatUserRole(row.role).type">
              {{ UserManagementService.formatUserRole(row.role).text }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100" sortable>
          <template #default="{ row }">
            <el-tag :type="UserManagementService.formatUserStatus(row.status).type">
              {{ UserManagementService.formatUserStatus(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="统计信息" width="120">
          <template #default="{ row }">
            <div class="stats-info">
              <div class="stat-item">
                <el-icon><Document /></el-icon>
                <span>{{ row.totalOrders }}</span>
              </div>
              <div class="stat-item">
                <el-icon><Star /></el-icon>
                <span>{{ row.totalReviews }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="注册时间" width="160" sortable>
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              link
              @click="handleViewDetail(row)"
            >
              详情
            </el-button>
            <el-button
              type="warning"
              size="small"
              link
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button
              type="danger"
              size="small"
              link
              @click="handleDeleteUser(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-section">
        <el-pagination
          v-model:current-page="pagination.page + 1"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 高级搜索对话框 -->
    <UserSearch
      v-model="showSearchDialog"
      :filters="searchFilters"
      @search="handleAdvancedSearch"
      @reset="handleSearchReset"
    />

    <!-- 用户详情对话框 -->
    <UserDetailDialog
      v-model="showDetailDialog"
      :user-id="selectedUserId"
      @refresh="handleRefresh"
    />

    <!-- 批量操作结果对话框 -->
    <BatchOperationResult
      v-model="showBatchResultDialog"
      :result="batchOperationResult"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  User,
  UserFilled,
  CircleClose,
  Plus,
  Document,
  Star
} from '@element-plus/icons-vue'
import { useUserManagementStore } from '@/stores/userManagement'
import UserManagementService from '@/services/userManagementService'
import UserSearch from '@/components/business/admin/users/UserSearch.vue'
import UserDetailDialog from '@/components/business/admin/users/UserDetailDialog.vue'
import BatchOperationResult from '@/components/business/admin/users/BatchOperationResult.vue'
import type { UserListDTO } from '@/types/userManagement'

// Store
const userManagementStore = useUserManagementStore()

// 解构store状态和方法
const {
  userList,
  statistics,
  pagination,
  searchFilters,
  selectedUserIds,
  loading,
  batchOperationLoading,
  batchOperationResult,
  fetchUserList,
  fetchUserStatistics,
  updateUserStatus,
  deleteUser,
  batchUpdateUserStatus,
  batchDeleteUsers,
  toggleUserSelection,
  toggleAllSelection,
  clearSelection,
  updateSearchFilters,
  resetSearchFilters
} = userManagementStore

// 对话框状态
const showSearchDialog = ref(false)
const showDetailDialog = ref(false)
const showBatchResultDialog = ref(false)
const selectedUserId = ref<number>(0)

// 格式化日期
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

// ========== 事件处理 ==========

/**
 * 刷新列表
 */
const handleRefresh = async () => {
  await fetchUserList(true)
  await fetchUserStatistics()
}

/**
 * 表格选择变化
 */
const handleSelectionChange = (selection: UserListDTO[]) => {
  // 更新store中的选中状态
  selectedUserIds.splice(0, selectedUserIds.length, ...selection.map(user => user.id))
}

/**
 * 分页大小变化
 */
const handleSizeChange = (newSize: number) => {
  updatePagination(0, newSize)
  fetchUserList()
}

/**
 * 当前页变化
 */
const handleCurrentChange = (newPage: number) => {
  updatePagination(newPage - 1, pagination.size)
  fetchUserList()
}

/**
 * 高级搜索
 */
const handleAdvancedSearch = (filters: any) => {
  updateSearchFilters(filters)
  fetchUserList(true)
}

/**
 * 重置搜索
 */
const handleSearchReset = () => {
  resetSearchFilters()
  fetchUserList(true)
}

/**
 * 查看用户详情
 */
const handleViewDetail = (user: UserListDTO) => {
  selectedUserId.value = user.id
  showDetailDialog.value = true
}

/**
 * 切换用户状态
 */
const handleToggleStatus = async (user: UserListDTO) => {
  try {
    const action = user.status === 'ACTIVE' ? '禁用' : '启用'
    await ElMessageBox.confirm(
      `确定要${action}用户 "${user.username}" 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const newStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    await updateUserStatus(user.id, newStatus, `管理员${action}用户`)

    ElMessage.success(`用户${action}成功`)

  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

/**
 * 删除用户
 */
const handleDeleteUser = async (user: UserListDTO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.username}" 吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )

    await deleteUser(user.id, '管理员删除用户')

    ElMessage.success('用户删除成功')

  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 批量启用
 */
const handleBatchEnable = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要启用选中的 ${selectedUserIds.length} 个用户吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const result = await batchUpdateUserStatus('ENABLE', '批量启用用户')
    showBatchResultDialog.value = true

    if (result.failureCount === 0) {
      ElMessage.success('批量启用成功')
    } else {
      ElMessage.warning(`部分操作失败，成功: ${result.successCount}, 失败: ${result.failureCount}`)
    }

  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('批量启用失败')
    }
  }
}

/**
 * 批量禁用
 */
const handleBatchDisable = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要禁用选中的 ${selectedUserIds.length} 个用户吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const result = await batchUpdateUserStatus('DISABLE', '批量禁用用户')
    showBatchResultDialog.value = true

    if (result.failureCount === 0) {
      ElMessage.success('批量禁用成功')
    } else {
      ElMessage.warning(`部分操作失败，成功: ${result.successCount}, 失败: ${result.failureCount}`)
    }

  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('批量禁用失败')
    }
  }
}

/**
 * 批量删除
 */
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedUserIds.length} 个用户吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )

    const result = await batchDeleteUsers('批量删除用户')
    showBatchResultDialog.value = true

    if (result.failureCount === 0) {
      ElMessage.success('批量删除成功')
    } else {
      ElMessage.warning(`部分操作失败，成功: ${result.successCount}, 失败: ${result.failureCount}`)
    }

  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

/**
 * 更新分页
 */
const updatePagination = (page: number, size: number) => {
  pagination.page = page
  pagination.size = size
}

// 生命周期
onMounted(async () => {
  await Promise.all([
    fetchUserList(true),
    fetchUserStatistics()
  ])
})
</script>

<style scoped>
.user-management {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #333;
}

.page-header p {
  margin: 0 0 20px 0;
  color: #666;
  font-size: 14px;
}

.statistics-cards {
  margin-top: 20px;
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
  font-size: 28px;
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
  font-size: 32px;
  color: #ddd;
}

.stat-card.active .stat-number {
  color: #67c23a;
}

.stat-card.active .stat-icon {
  color: #67c23a;
}

.stat-card.inactive .stat-number {
  color: #f56c6c;
}

.stat-card.inactive .stat-icon {
  color: #f56c6c;
}

.stat-card.today .stat-number {
  color: #409eff;
}

.stat-card.today .stat-icon {
  color: #409eff;
}

.toolbar-section {
  background: white;
  padding: 16px 20px;
  margin-bottom: 20px;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selection-info {
  color: #666;
  font-size: 14px;
  margin-right: 12px;
}

.table-section {
  background: white;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.username {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.nickname {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stats-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

.stat-item .el-icon {
  font-size: 14px;
}

.pagination-section {
  padding: 20px;
  display: flex;
  justify-content: center;
}
</style>