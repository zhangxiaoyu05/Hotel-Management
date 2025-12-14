<template>
  <div class="security-settings">
    <div class="settings-content">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="160px"
        label-position="left"
        class="settings-form"
      >
        <!-- 密码策略 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><Lock /></el-icon>
            密码策略
          </h3>

          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="最小长度" prop="passwordMinLength">
                <el-input-number
                  v-model="formData.passwordMinLength"
                  :min="6"
                  :max="50"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="过期天数" prop="passwordExpiryDays">
                <el-input-number
                  v-model="formData.passwordExpiryDays"
                  :min="0"
                  :max="365"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="历史记录数" prop="passwordHistoryCount">
                <el-input-number
                  v-model="formData.passwordHistoryCount"
                  :min="0"
                  :max="20"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="密码复杂度要求">
            <el-checkbox-group v-model="passwordRequirements">
              <el-checkbox value="uppercase">大写字母</el-checkbox>
              <el-checkbox value="lowercase">小写字母</el-checkbox>
              <el-checkbox value="numbers">数字</el-checkbox>
              <el-checkbox value="special">特殊字符</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </div>

        <!-- 登录安全 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><User /></el-icon>
            登录安全
          </h3>

          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="最大尝试次数" prop="maxLoginAttempts">
                <el-input-number
                  v-model="formData.maxLoginAttempts"
                  :min="3"
                  :max="10"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="锁定时间(分钟)" prop="accountLockoutMinutes">
                <el-input-number
                  v-model="formData.accountLockoutMinutes"
                  :min="1"
                  :max="1440"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="会话超时(分钟)" prop="sessionTimeoutMinutes">
                <el-input-number
                  v-model="formData.sessionTimeoutMinutes"
                  :min="15"
                  :max="1440"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 高级安全 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><Shield /></el-icon>
            高级安全
          </h3>

          <el-row :gutter="24">
            <el-col :span="6">
              <el-form-item label="双因素认证">
                <el-switch
                  v-model="formData.enableTwoFactorAuth"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>

            <el-col :span="6">
              <el-form-item label="强制双因素认证">
                <el-switch
                  v-model="formData.forceTwoFactorAuth"
                  :disabled="!formData.enableTwoFactorAuth"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>

            <el-col :span="6">
              <el-form-item label="验证码">
                <el-switch
                  v-model="formData.enableCaptcha"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>

            <el-col :span="6">
              <el-form-item label="审计日志">
                <el-switch
                  v-model="formData.enableAuditLog"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="IP白名单">
            <el-switch
              v-model="formData.enableIpWhitelist"
              active-text="启用"
              inactive-text="禁用"
            />
          </el-form-item>

          <el-form-item v-if="formData.enableIpWhitelist" label="允许的IP范围">
            <el-input
              v-model="formData.allowedIpRanges"
              type="textarea"
              :rows="3"
              placeholder="请输入IP范围，每行一个，例如：&#10;192.168.1.0/24&#10;10.0.0.1&#10;172.16.0.0/16"
            />
          </el-form-item>
        </div>

        <!-- 安全预览 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><View /></el-icon>
            安全预览
          </h3>

          <div class="security-preview">
            <el-alert
              :title="getSecurityLevel()"
              :type="getSecurityType()"
              :description="getSecurityDescription()"
              show-icon
              :closable="false"
            />
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="settings-actions">
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
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock, User, Shield, View } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { SecuritySettings } from '@/stores/systemSettings'

// Props
interface Props {
  modelValue: SecuritySettings
}

interface Emits {
  (e: 'update:modelValue', value: SecuritySettings): void
  (e: 'save', value: SecuritySettings): void
  (e: 'loading', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const formRef = ref<FormInstance>()
const saving = ref(false)
const originalData = ref<SecuritySettings>({} as SecuritySettings)

const formData = reactive<SecuritySettings>({
  passwordMinLength: 8,
  passwordRequireUppercase: true,
  passwordRequireLowercase: true,
  passwordRequireNumbers: true,
  passwordRequireSpecialChars: true,
  passwordExpiryDays: 90,
  passwordHistoryCount: 5,
  maxLoginAttempts: 5,
  accountLockoutMinutes: 30,
  sessionTimeoutMinutes: 120,
  enableTwoFactorAuth: false,
  forceTwoFactorAuth: false,
  enableCaptcha: false,
  enableIpWhitelist: false,
  enableAuditLog: true
})

const passwordRequirements = ref<string[]>([])

// 表单验证规则
const rules: FormRules = {
  passwordMinLength: [
    { required: true, message: '请输入密码最小长度', trigger: 'blur' },
    { type: 'number', min: 6, max: 50, message: '密码最小长度必须在6-50之间', trigger: 'blur' }
  ],
  maxLoginAttempts: [
    { required: true, message: '请输入最大登录尝试次数', trigger: 'blur' },
    { type: 'number', min: 3, max: 10, message: '最大登录尝试次数必须在3-10之间', trigger: 'blur' }
  ]
}

// 计算属性
const securityLevel = computed(() => {
  let score = 0

  // 密码策略评分
  if (formData.passwordMinLength >= 8) score++
  if (formData.passwordMinLength >= 12) score++
  if (passwordRequirements.value.length >= 3) score++
  if (passwordRequirements.value.length === 4) score++

  // 登录安全评分
  if (formData.maxLoginAttempts <= 5) score++
  if (formData.accountLockoutMinutes <= 30) score++
  if (formData.sessionTimeoutMinutes <= 120) score++

  // 高级安全评分
  if (formData.enableTwoFactorAuth) score += 2
  if (formData.enableCaptcha) score++
  if (formData.enableAuditLog) score++
  if (formData.enableIpWhitelist) score++

  return score
})

// 监听密码复杂度要求
watch(passwordRequirements, (newRequirements) => {
  formData.passwordRequireUppercase = newRequirements.includes('uppercase')
  formData.passwordRequireLowercase = newRequirements.includes('lowercase')
  formData.passwordRequireNumbers = newRequirements.includes('numbers')
  formData.passwordRequireSpecialChars = newRequirements.includes('special')
}, { immediate: true })

// 监听表单数据变化
watch(formData, (newValue) => {
  // 更新密码复杂度要求
  const requirements = []
  if (newValue.passwordRequireUppercase) requirements.push('uppercase')
  if (newValue.passwordRequireLowercase) requirements.push('lowercase')
  if (newValue.passwordRequireNumbers) requirements.push('numbers')
  if (newValue.passwordRequireSpecialChars) requirements.push('special')
  passwordRequirements.value = requirements

  emit('update:modelValue', { ...newValue })
}, { deep: true })

// 监听父组件数据变化
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    Object.assign(formData, newValue)
    originalData.value = { ...newValue }
  }
}, { immediate: true, deep: true })

// 生命周期
onMounted(() => {
  if (props.modelValue) {
    Object.assign(formData, props.modelValue)
    originalData.value = { ...props.modelValue }
  }
})

// 方法
const getSecurityLevel = () => {
  const score = securityLevel.value
  if (score >= 10) return '安全级别：高'
  if (score >= 6) return '安全级别：中'
  return '安全级别：低'
}

const getSecurityType = () => {
  const score = securityLevel.value
  if (score >= 10) return 'success'
  if (score >= 6) return 'warning'
  return 'error'
}

const getSecurityDescription = () => {
  const score = securityLevel.value
  const enabled = []
  const disabled = []

  if (formData.passwordMinLength >= 8) enabled.push('强密码策略')
  else disabled.push('密码长度不足8位')

  if (passwordRequirements.value.length >= 3) enabled.push('密码复杂度要求')
  else disabled.push('密码复杂度要求不足')

  if (formData.enableTwoFactorAuth) enabled.push('双因素认证')
  else disabled.push('未启用双因素认证')

  if (formData.enableCaptcha) enabled.push('验证码防护')
  else disabled.push('未启用验证码')

  if (enabled.length === 0) return '当前安全配置较弱，建议启用更多安全功能'

  return `已启用：${enabled.join('、')}。${disabled.length > 0 ? `建议开启：${disabled.join('、')}` : '安全配置完善'}`
}

const handleSave = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    saving.value = true
    emit('loading', true)

    emit('save', { ...formData })
  } catch (error) {
    ElMessage.error('请检查表单数据')
  } finally {
    saving.value = false
    emit('loading', false)
  }
}

const handleReset = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重置安全设置吗？未保存的更改将丢失。',
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
</script>

<style scoped>
.security-settings {
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

.security-preview {
  max-width: 800px;
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

:deep(.el-input-number) {
  width: 100%;
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

:deep(.el-checkbox-group) {
  display: flex;
  gap: 16px;
}
</style>