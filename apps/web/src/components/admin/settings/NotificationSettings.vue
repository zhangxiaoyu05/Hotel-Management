<template>
  <div class="notification-settings">
    <div class="settings-content">
      <el-form
        ref="formRef"
        :model="formData"
        label-width="140px"
        label-position="left"
        class="settings-form"
      >
        <!-- 通知开关 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><Bell /></el-icon>
            通知设置
          </h3>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="邮件通知">
                <el-switch
                  v-model="formData.enableEmailNotifications"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="短信通知">
                <el-switch
                  v-model="formData.enableSmsNotifications"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 邮件配置 -->
        <div class="settings-section" v-if="formData.enableEmailNotifications">
          <h3 class="section-title">
            <el-icon><Message /></el-icon>
            邮件配置
          </h3>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="发件人邮箱">
                <el-input
                  v-model="formData.emailFromAddress"
                  placeholder="noreply@hotel.com"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="发件人名称">
                <el-input
                  v-model="formData.emailFromName"
                  placeholder="酒店管理系统"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="SMTP服务器">
                <el-input
                  v-model="formData.smtpHost"
                  placeholder="smtp.gmail.com"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="端口">
                <el-input-number
                  v-model="formData.smtpPort"
                  :min="1"
                  :max="65535"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="用户名">
                <el-input
                  v-model="formData.smtpUsername"
                  placeholder="邮箱地址"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="启用SSL">
                <el-switch
                  v-model="formData.smtpSslEnabled"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 短信配置 -->
        <div class="settings-section" v-if="formData.enableSmsNotifications">
          <h3 class="section-title">
            <el-icon><Iphone /></el-icon>
            短信配置
          </h3>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="服务商">
                <el-select v-model="formData.smsProvider" placeholder="选择短信服务商">
                  <el-option label="阿里云" value="aliyun" />
                  <el-option label="腾讯云" value="tencent" />
                  <el-option label="华为云" value="huawei" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="签名">
                <el-input
                  v-model="formData.smsSignName"
                  placeholder="【酒店管理】"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="API Key">
                <el-input
                  v-model="formData.smsApiKey"
                  type="password"
                  show-password
                  placeholder="请输入API Key"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="API Secret">
                <el-input
                  v-model="formData.smsApiSecret"
                  type="password"
                  show-password
                  placeholder="请输入API Secret"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 通知规则 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><Setting /></el-icon>
            通知规则
          </h3>

          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="预订通知">
                <el-switch
                  v-model="formData.enableBookingNotifications"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="取消通知">
                <el-switch
                  v-model="formData.enableCancelNotifications"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="支付通知">
                <el-switch
                  v-model="formData.enablePaymentNotifications"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="入住通知">
                <el-switch
                  v-model="formData.enableCheckInNotifications"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="退房通知">
                <el-switch
                  v-model="formData.enableCheckOutNotifications"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 操作按钮 -->
        <div class="settings-actions">
          <el-button @click="handleTestNotification" :loading="testing">
            测试通知
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">
            保存设置
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Message, Iphone, Setting } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import type { NotificationSettings } from '@/stores/systemSettings'

// Props
interface Props {
  modelValue: NotificationSettings
}

interface Emits {
  (e: 'update:modelValue', value: NotificationSettings): void
  (e: 'save', value: NotificationSettings): void
  (e: 'loading', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const formRef = ref<FormInstance>()
const saving = ref(false)
const testing = ref(false)
const originalData = ref<NotificationSettings>({} as NotificationSettings)

const formData = reactive<NotificationSettings>({
  enableEmailNotifications: false,
  enableSmsNotifications: false,
  enableBookingNotifications: true,
  enableCancelNotifications: true,
  enablePaymentNotifications: true,
  enableCheckInNotifications: true,
  enableCheckOutNotifications: true
})

// 监听父组件数据变化
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    Object.assign(formData, newValue)
    originalData.value = { ...newValue }
  }
}, { immediate: true, deep: true })

// 监听表单数据变化
watch(formData, (newValue) => {
  emit('update:modelValue', { ...newValue })
}, { deep: true })

// 生命周期
onMounted(() => {
  if (props.modelValue) {
    Object.assign(formData, props.modelValue)
    originalData.value = { ...props.modelValue }
  }
})

// 方法
const handleSave = async () => {
  try {
    saving.value = true
    emit('loading', true)

    emit('save', { ...formData })
  } catch (error) {
    ElMessage.error('保存设置失败')
  } finally {
    saving.value = false
    emit('loading', false)
  }
}

const handleReset = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重置通知设置吗？未保存的更改将丢失。',
      '确认重置',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    Object.assign(formData, originalData.value)
    ElMessage.success('已重置到上次保存的状态')
  } catch (error) {
    // 用户取消操作
  }
}

const handleTestNotification = async () => {
  try {
    testing.value = true

    // TODO: 实现测试通知API
    await new Promise(resolve => setTimeout(resolve, 2000))

    ElMessage.success('测试通知已发送')
  } catch (error) {
    ElMessage.error('发送测试通知失败')
  } finally {
    testing.value = false
  }
}
</script>

<style scoped>
.notification-settings {
  padding: 24px;
}

.settings-content {
  max-width: 1200px;
  margin: 0 auto;
}

.settings-form {
  padding: 0;
}

.settings-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.settings-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.section-title .el-icon {
  margin-right: 8px;
  color: #409eff;
}

.settings-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  margin-top: 32px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-switch) {
  height: 22px;
}

:deep(.el-switch__label) {
  font-size: 12px;
  color: #606266;
}
</style>