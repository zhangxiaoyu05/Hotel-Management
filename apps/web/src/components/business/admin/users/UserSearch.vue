<template>
  <el-dialog
    v-model="dialogVisible"
    title="高级搜索"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="searchFormRef"
      :model="searchForm"
      :rules="searchRules"
      label-width="100px"
      class="search-form"
    >
      <!-- 基本信息 -->
      <div class="form-section">
        <h4 class="section-title">基本信息</h4>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="searchForm.username"
                placeholder="请输入用户名"
                clearable
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input
                v-model="searchForm.email"
                placeholder="请输入邮箱"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input
                v-model="searchForm.phone"
                placeholder="请输入手机号"
                clearable
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="关键词" prop="keyword">
              <el-input
                v-model="searchForm.keyword"
                placeholder="用户名、邮箱或手机号"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- 筛选条件 -->
      <div class="form-section">
        <h4 class="section-title">筛选条件</h4>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-select
                v-model="searchForm.role"
                placeholder="请选择角色"
                clearable
                style="width: 100%"
              >
                <el-option label="管理员" value="ADMIN" />
                <el-option label="普通用户" value="USER" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="searchForm.status"
                placeholder="请选择状态"
                clearable
                style="width: 100%"
              >
                <el-option label="正常" value="ACTIVE" />
                <el-option label="禁用" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- 时间范围 -->
      <div class="form-section">
        <h4 class="section-title">时间范围</h4>

        <el-form-item label="注册时间" prop="registrationDateRange">
          <el-date-picker
            v-model="searchForm.registrationDateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="最后登录时间" prop="lastLoginDateRange">
          <el-date-picker
            v-model="searchForm.lastLoginDateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </div>

      <!-- 排序选项 -->
      <div class="form-section">
        <h4 class="section-title">排序选项</h4>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="排序字段" prop="sortBy">
              <el-select
                v-model="searchForm.sortBy"
                placeholder="请选择排序字段"
                style="width: 100%"
              >
                <el-option label="创建时间" value="createdAt" />
                <el-option label="用户名" value="username" />
                <el-option label="邮箱" value="email" />
                <el-option label="注册时间" value="registrationDate" />
                <el-option label="最后登录时间" value="lastLoginAt" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="排序方向" prop="sortDirection">
              <el-select
                v-model="searchForm.sortDirection"
                placeholder="请选择排序方向"
                style="width: 100%"
              >
                <el-option label="降序" value="desc" />
                <el-option label="升序" value="asc" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleReset">重置</el-button>
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          @click="handleSearch"
          :loading="searching"
        >
          搜索
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { SearchFilters } from '@/types/userManagement'

interface SearchFormData extends SearchFilters {
  sortBy: string
  sortDirection: string
}

// Props
const props = defineProps<{
  modelValue: boolean
  filters: SearchFilters
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'search': [filters: SearchFormData]
  'reset': []
}>()

// 响应式数据
const searchFormRef = ref<FormInstance>()
const searching = ref(false)

// 搜索表单数据
const searchForm = reactive<SearchFormData>({
  keyword: '',
  username: '',
  email: '',
  phone: '',
  role: '',
  status: '',
  registrationDateRange: null,
  lastLoginDateRange: null,
  sortBy: 'createdAt',
  sortDirection: 'desc'
})

// 表单验证规则
const searchRules: FormRules = {
  email: [
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: '请输入正确的邮箱格式',
      trigger: 'blur'
    }
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号格式',
      trigger: 'blur'
    }
  ]
}

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 监听器
watch(
  () => props.filters,
  (newFilters) => {
    // 同步外部filters到内部表单
    Object.assign(searchForm, newFilters)
  },
  { immediate: true, deep: true }
)

// 方法

/**
 * 处理搜索
 */
const handleSearch = async () => {
  try {
    // 验证表单
    const valid = await searchFormRef.value?.validate()
    if (!valid) {
      return
    }

    searching.value = true

    // 构建搜索参数
    const searchParams: SearchFormData = {
      ...searchForm,
      registrationDateStart: searchForm.registrationDateRange?.[0] || undefined,
      registrationDateEnd: searchForm.registrationDateRange?.[1] || undefined,
      lastLoginDateStart: searchForm.lastLoginDateRange?.[0] || undefined,
      lastLoginDateEnd: searchForm.lastLoginDateRange?.[1] || undefined
    }

    // 触发搜索事件
    emit('search', searchParams)

    // 关闭对话框
    handleClose()

    ElMessage.success('搜索条件已应用')

  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败，请检查输入条件')
  } finally {
    searching.value = false
  }
}

/**
 * 重置搜索条件
 */
const handleReset = () => {
  searchFormRef.value?.resetFields()

  // 重置为默认值
  Object.assign(searchForm, {
    keyword: '',
    username: '',
    email: '',
    phone: '',
    role: '',
    status: '',
    registrationDateRange: null,
    lastLoginDateRange: null,
    sortBy: 'createdAt',
    sortDirection: 'desc'
  })

  // 触发重置事件
  emit('reset')
}

/**
 * 关闭对话框
 */
const handleClose = () => {
  dialogVisible.value = false
}

/**
 * 快速设置常用搜索条件
 */
const setQuickSearch = (type: string) => {
  switch (type) {
    case 'todayUsers':
      const today = new Date()
      const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0)
      const todayEnd = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59)
      searchForm.registrationDateRange = [
        formatDate(todayStart),
        formatDate(todayEnd)
      ]
      break
    case 'activeUsers':
      searchForm.status = 'ACTIVE'
      break
    case 'inactiveUsers':
      searchForm.status = 'INACTIVE'
      break
    case 'adminUsers':
      searchForm.role = 'ADMIN'
      break
  }
}

/**
 * 格式化日期
 */
const formatDate = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 暴露方法给父组件
defineExpose({
  setQuickSearch,
  reset: handleReset
})
</script>

<style scoped>
.search-form {
  max-height: 60vh;
  overflow-y: auto;
}

.form-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.form-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 500;
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-date-editor.el-input__wrapper) {
  width: 100%;
}

/* 快速搜索按钮组 */
.quick-search-buttons {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.quick-search-btn {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
}

.quick-search-btn:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

.quick-search-btn.active {
  border-color: #409eff;
  color: #fff;
  background: #409eff;
}
</style>