<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type UploadProps } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { profileService } from '@/services/profileService'
import type { User, UpdateProfileRequest, ChangePasswordRequest } from '@/types/user'

const authStore = useAuthStore()
const activeTab = ref('basic')
const profileLoading = ref(false)
const passwordLoading = ref(false)
const defaultAvatar = '/src/assets/default-avatar.png'

// 表单引用
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

// 上传配置
const uploadUrl = `${import.meta.env.VITE_API_BASE_URL}/files/upload`
const uploadHeaders = {
  Authorization: `Bearer ${authStore.token}`
}

// 基本信息表单
const profileForm = reactive<UpdateProfileRequest>({
  nickname: '',
  realName: '',
  gender: undefined,
  birthDate: '',
  avatar: '',
  email: '',
  phone: ''
})

// 密码修改表单
const passwordForm = reactive<ChangePasswordRequest>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 基本信息表单验证规则
const profileRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  nickname: [
    { max: 50, message: '昵称长度不能超过 50 个字符', trigger: 'blur' }
  ],
  realName: [
    { max: 50, message: '真实姓名长度不能超过 50 个字符', trigger: 'blur' }
  ]
}

// 密码修改表单验证规则
const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 加载用户信息
const loadUserProfile = async () => {
  try {
    const userInfo = await profileService.getCurrentUser()

    // 更新表单数据
    Object.assign(profileForm, {
      nickname: userInfo.nickname || '',
      realName: userInfo.realName || '',
      gender: userInfo.gender || undefined,
      birthDate: userInfo.birthDate || '',
      avatar: userInfo.avatar || '',
      email: userInfo.email || '',
      phone: userInfo.phone || ''
    })

    // 更新 authStore 中的用户信息
    authStore.setUser(userInfo)
  } catch (error: any) {
    ElMessage.error(error.message || '加载用户信息失败')
  }
}

// 更新基本信息
const updateProfile = async () => {
  if (!profileFormRef.value) return

  try {
    await profileFormRef.value.validate()
    profileLoading.value = true

    const updatedUser = await profileService.updateProfile(profileForm)

    // 更新 authStore 中的用户信息
    authStore.setUser(updatedUser)

    ElMessage.success('基本信息更新成功')
  } catch (error: any) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    profileLoading.value = false
  }
}

// 修改密码
const changePassword = async () => {
  if (!passwordFormRef.value) return

  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true

    await profileService.changePassword(passwordForm)

    ElMessage.success('密码修改成功')
    resetPasswordForm()
  } catch (error: any) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    passwordLoading.value = false
  }
}

// 重置基本信息表单
const resetProfileForm = () => {
  loadUserProfile()
}

// 重置密码表单
const resetPasswordForm = () => {
  if (passwordFormRef.value) {
    passwordFormRef.value.resetFields()
  }
  Object.assign(passwordForm, {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
}

// 头像上传前验证
const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
  const isValidFormat = ['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)
  const isValidSize = file.size / 1024 / 1024 < 5

  if (!isValidFormat) {
    ElMessage.error('头像只能是 JPG、PNG 格式!')
    return false
  }
  if (!isValidSize) {
    ElMessage.error('头像大小不能超过 5MB!')
    return false
  }
  return true
}

// 头像上传成功
const onAvatarUploadSuccess = (response: any) => {
  if (response.success) {
    profileForm.avatar = response.data.url
    ElMessage.success('头像上传成功')
    // 自动保存头像
    updateProfile()
  } else {
    ElMessage.error(response.message || '头像上传失败')
  }
}

// 头像上传失败
const onAvatarUploadError = () => {
  ElMessage.error('头像上传失败，请重试')
}

// 订单数据（暂时保持静态数据）
const orders = ref([
  {
    id: 1,
    orderNumber: 'ORD202412060001',
    hotelName: '成都望江宾馆',
    roomType: '标准间',
    checkInDate: '2024-12-10',
    checkOutDate: '2024-12-12',
    totalPrice: 576,
    status: 'CONFIRMED'
  },
  {
    id: 2,
    orderNumber: 'ORD202412050001',
    hotelName: '天府酒店',
    roomType: '豪华间',
    checkInDate: '2024-11-20',
    checkOutDate: '2024-11-22',
    totalPrice: 736,
    status: 'COMPLETED'
  }
])

const getStatusType = (status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const statusMap: { [key: string]: 'primary' | 'success' | 'warning' | 'info' | 'danger' } = {
    PENDING: 'warning',
    CONFIRMED: 'success',
    CANCELLED: 'danger',
    COMPLETED: 'info'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const statusMap: { [key: string]: string } = {
    PENDING: '待确认',
    CONFIRMED: '已确认',
    CANCELLED: '已取消',
    COMPLETED: '已完成'
  }
  return statusMap[status] || status
}

// 页面挂载时加载用户信息
onMounted(() => {
  loadUserProfile()
})
</script>

<template>
  <div class="profile-page">
    <div class="container">
      <ElCard>
        <template #header>
          <div class="profile-header">
            <h2>个人中心</h2>
            <p>管理您的个人信息和订单</p>
          </div>
        </template>

        <ElTabs v-model="activeTab">
          <!-- 个人信息 -->
          <ElTabPane label="个人信息" name="basic">
            <ElForm
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-width="100px"
              class="profile-form"
            >
              <!-- 头像上传 -->
              <ElFormItem label="头像">
                <div class="avatar-upload">
                  <el-avatar
                    :size="100"
                    :src="profileForm.avatar || defaultAvatar"
                    class="avatar"
                  />
                  <div class="avatar-actions">
                    <el-upload
                      class="avatar-uploader"
                      :action="uploadUrl"
                      :headers="uploadHeaders"
                      :show-file-list="false"
                      :before-upload="beforeAvatarUpload"
                      :on-success="onAvatarUploadSuccess"
                      :on-error="onAvatarUploadError"
                    >
                      <el-button type="primary" size="small">
                        <el-icon><Upload /></el-icon>
                        上传头像
                      </el-button>
                    </el-upload>
                    <el-text size="small" type="info">支持 JPG、PNG 格式，最大 5MB</el-text>
                  </div>
                </div>
              </ElFormItem>

              <ElFormItem label="用户名">
                <ElInput :model-value="authStore.user?.username" disabled />
              </ElFormItem>

              <ElFormItem label="昵称" prop="nickname">
                <ElInput v-model="profileForm.nickname" placeholder="请输入昵称" />
              </ElFormItem>

              <ElFormItem label="真实姓名" prop="realName">
                <ElInput v-model="profileForm.realName" placeholder="请输入真实姓名" />
              </ElFormItem>

              <ElFormItem label="性别" prop="gender">
                <el-select v-model="profileForm.gender" placeholder="请选择性别" class="full-width">
                  <el-option label="男" value="MALE" />
                  <el-option label="女" value="FEMALE" />
                  <el-option label="其他" value="OTHER" />
                </el-select>
              </ElFormItem>

              <ElFormItem label="出生日期" prop="birthDate">
                <el-date-picker
                  v-model="profileForm.birthDate"
                  type="date"
                  placeholder="请选择出生日期"
                  class="full-width"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </ElFormItem>

              <ElFormItem label="邮箱地址" prop="email">
                <ElInput v-model="profileForm.email" placeholder="请输入邮箱" />
              </ElFormItem>

              <ElFormItem label="手机号码" prop="phone">
                <ElInput v-model="profileForm.phone" placeholder="请输入手机号" />
              </ElFormItem>

              <ElFormItem>
                <ElButton type="primary" :loading="profileLoading" @click="updateProfile">
                  保存基本信息
                </ElButton>
                <ElButton @click="resetProfileForm">重置</ElButton>
              </ElFormItem>
            </ElForm>
          </ElTabPane>

          <!-- 修改密码 -->
          <ElTabPane label="修改密码" name="password">
            <ElForm
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="100px"
              class="profile-form"
            >
              <ElFormItem label="当前密码" prop="currentPassword">
                <ElInput
                  v-model="passwordForm.currentPassword"
                  type="password"
                  placeholder="请输入当前密码"
                  show-password
                />
              </ElFormItem>

              <ElFormItem label="新密码" prop="newPassword">
                <ElInput
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                  show-password
                />
              </ElFormItem>

              <ElFormItem label="确认新密码" prop="confirmPassword">
                <ElInput
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                />
              </ElFormItem>

              <ElFormItem>
                <ElButton type="primary" :loading="passwordLoading" @click="changePassword">
                  修改密码
                </ElButton>
                <ElButton @click="resetPasswordForm">重置</ElButton>
              </ElFormItem>
            </ElForm>
          </ElTabPane>

          <!-- 我的订单 -->
          <ElTabPane label="我的订单" name="orders">
            <ElTable :data="orders" style="width: 100%">
              <ElTableColumn prop="orderNumber" label="订单号" width="150" />
              <ElTableColumn prop="hotelName" label="酒店名称" />
              <ElTableColumn prop="roomType" label="房型" />
              <ElTableColumn prop="checkInDate" label="入住日期" width="120" />
              <ElTableColumn prop="checkOutDate" label="退房日期" width="120" />
              <ElTableColumn prop="totalPrice" label="总价" width="100">
                <template #default="{ row }">
                  ¥{{ row.totalPrice }}
                </template>
              </ElTableColumn>
              <ElTableColumn prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <ElTag :type="getStatusType(row.status)">
                    {{ getStatusText(row.status) }}
                  </ElTag>
                </template>
              </ElTableColumn>
              <ElTableColumn label="操作" width="120">
                <template #default="{ row }">
                  <ElButton type="text" size="small">查看</ElButton>
                  <ElButton
                    v-if="row.status === 'PENDING'"
                    type="text"
                    size="small"
                    style="color: #f56c6c"
                  >
                    取消
                  </ElButton>
                </template>
              </ElTableColumn>
            </ElTable>
          </ElTabPane>
        </ElTabs>
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: calc(100vh - 60px);
  padding: 20px 0;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px;
}

.profile-header {
  margin-bottom: 20px;
}

.profile-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 24px;
}

.profile-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.profile-tabs {
  margin-top: 20px;
}

.profile-form {
  max-width: 500px;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar {
  border: 2px solid #e4e7ed;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.avatar-uploader {
  display: block;
}

.full-width {
  width: 100%;
}

:deep(.el-tabs__content) {
  padding: 20px 0;
}

:deep(.el-form-item) {
  margin-bottom: 25px;
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-button) {
  border-radius: 6px;
}

@media (max-width: 768px) {
  .container {
    padding: 0 10px;
  }

  .profile-form {
    max-width: 100%;
  }

  .avatar-upload {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  :deep(.el-table) {
    font-size: 12px;
  }
}
</style>