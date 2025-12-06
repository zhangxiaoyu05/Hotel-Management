<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElCard, ElForm, ElFormItem, ElInput, ElButton, ElMessage, ElTabs, ElTabPane, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

const profileForm = ref({
  username: '',
  email: '',
  phone: ''
})

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

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

const loading = ref(false)

onMounted(() => {
  userStore.fetchProfile()
  if (userStore.user) {
    profileForm.value = {
      username: userStore.user.username,
      email: userStore.user.email,
      phone: userStore.user.phone
    }
  }
})

const updateProfile = async () => {
  try {
    loading.value = true
    // TODO: 实现更新个人信息的API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('个人信息更新成功')
  } catch (error) {
    ElMessage.error('更新失败，请重试')
  } finally {
    loading.value = false
  }
}

const changePassword = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  try {
    loading.value = true
    // TODO: 实现修改密码的API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('密码修改成功')
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (error) {
    ElMessage.error('修改失败，请重试')
  } finally {
    loading.value = false
  }
}

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

        <ElTabs>
          <!-- 个人信息 -->
          <ElTabPane label="个人信息" name="profile">
            <ElForm
              :model="profileForm"
              label-width="100px"
              style="max-width: 500px"
            >
              <ElFormItem label="用户名">
                <ElInput v-model="profileForm.username" disabled />
              </ElFormItem>
              <ElFormItem label="邮箱地址">
                <ElInput v-model="profileForm.email" />
              </ElFormItem>
              <ElFormItem label="手机号码">
                <ElInput v-model="profileForm.phone" />
              </ElFormItem>
              <ElFormItem>
                <ElButton type="primary" :loading="loading" @click="updateProfile">
                  更新信息
                </ElButton>
              </ElFormItem>
            </ElForm>
          </ElTabPane>

          <!-- 修改密码 -->
          <ElTabPane label="修改密码" name="password">
            <ElForm
              :model="passwordForm"
              label-width="100px"
              style="max-width: 500px"
            >
              <ElFormItem label="当前密码">
                <ElInput
                  v-model="passwordForm.currentPassword"
                  type="password"
                  show-password
                />
              </ElFormItem>
              <ElFormItem label="新密码">
                <ElInput
                  v-model="passwordForm.newPassword"
                  type="password"
                  show-password
                />
              </ElFormItem>
              <ElFormItem label="确认密码">
                <ElInput
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  show-password
                />
              </ElFormItem>
              <ElFormItem>
                <ElButton type="primary" :loading="loading" @click="changePassword">
                  修改密码
                </ElButton>
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

  :deep(.el-form) {
    max-width: 100% !important;
  }

  :deep(.el-table) {
    font-size: 12px;
  }
}
</style>