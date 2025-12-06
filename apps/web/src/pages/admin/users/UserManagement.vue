<template>
  <div class="user-management">
    <div class="page-header">
      <h1>用户管理</h1>
      <p>管理系统中的所有用户账户</p>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input
            v-model="searchForm.username"
            placeholder="搜索用户名"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="searchForm.role"
            placeholder="选择角色"
            clearable
            @clear="handleSearch"
            @change="handleSearch"
          >
            <el-option label="全部角色" value="" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="searchForm.status"
            placeholder="选择状态"
            clearable
            @clear="handleSearch"
            @change="handleSearch"
          >
            <el-option label="全部状态" value="" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 用户列表表格 -->
    <div class="table-section">
      <el-table
        v-loading="loading"
        :data="userList"
        style="width: 100%"
        stripe
        border
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="ACTIVE"
              inactive-value="INACTIVE"
              active-text="启用"
              inactive-text="禁用"
              :disabled="row.id === currentUser?.id"
              @change="(value) => handleStatusChange(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleEditRole(row)"
              :disabled="row.id === currentUser?.id"
            >
              编辑角色
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-section">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 编辑角色对话框 -->
    <el-dialog
      v-model="roleDialog.visible"
      title="编辑用户角色"
      width="400px"
    >
      <el-form :model="roleDialog.form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="roleDialog.form.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleDialog.form.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="roleDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveRole" :loading="roleDialog.loading">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { authStore } from '@/stores/auth'
import { adminApi } from '@/api/admin'
import type { User } from '@/types/user'

// 响应式数据
const loading = ref(false)
const userList = ref<User[]>([])
const currentUser = authStore.user

// 搜索表单
const searchForm = reactive({
  username: '',
  role: '',
  status: ''
})

// 分页数据
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 角色编辑对话框
const roleDialog = reactive({
  visible: false,
  loading: false,
  form: {
    id: 0,
    username: '',
    role: ''
  }
})

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1, // 后端分页从0开始
      size: pagination.size,
      username: searchForm.username || undefined,
      role: searchForm.role || undefined,
      status: searchForm.status || undefined
    }

    const response = await adminApi.getUsers(params)
    if (response.success) {
      userList.value = response.data.records
      pagination.total = response.data.total
    } else {
      ElMessage.error(response.message || '获取用户列表失败')
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  pagination.page = 1
  fetchUsers()
}

// 分页处理
const handleSizeChange = (newSize: number) => {
  pagination.size = newSize
  pagination.page = 1
  fetchUsers()
}

const handleCurrentChange = (newPage: number) => {
  pagination.page = newPage
  fetchUsers()
}

// 状态变更处理
const handleStatusChange = async (user: User, newStatus: string) => {
  try {
    await ElMessageBox.confirm(
      `确定要${newStatus === 'ACTIVE' ? '启用' : '禁用'}用户 "${user.username}" 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await adminApi.updateUserStatus(user.id, { status: newStatus })
    if (response.success) {
      ElMessage.success('用户状态更新成功')
      fetchUsers()
    } else {
      // 恢复原状态
      user.status = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
      ElMessage.error(response.message || '用户状态更新失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      // 恢复原状态
      user.status = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
      ElMessage.error('用户状态更新失败')
    } else {
      // 恢复原状态
      user.status = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    }
  }
}

// 编辑角色处理
const handleEditRole = (user: User) => {
  roleDialog.form.id = user.id
  roleDialog.form.username = user.username
  roleDialog.form.role = user.role
  roleDialog.visible = true
}

// 保存角色
const handleSaveRole = async () => {
  roleDialog.loading.value = true
  try {
    const response = await adminApi.updateUserRole(roleDialog.form.id, {
      role: roleDialog.form.role
    })

    if (response.success) {
      ElMessage.success('用户角色更新成功')
      roleDialog.visible = false
      fetchUsers()
    } else {
      ElMessage.error(response.message || '用户角色更新失败')
    }
  } catch (error) {
    console.error('更新用户角色失败:', error)
    ElMessage.error('用户角色更新失败')
  } finally {
    roleDialog.loading.value = false
  }
}

// 格式化日期
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
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
  margin: 0;
  color: #666;
  font-size: 14px;
}

.filter-section {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.table-section {
  background: white;
  border-radius: 6px;
  overflow: hidden;
}

.pagination-section {
  padding: 20px;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>