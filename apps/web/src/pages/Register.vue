<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage, ElCard } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const registerForm = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)

const validatePass = (_rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email' as const, message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validatePass, trigger: 'blur' }
  ]
}

const formRef = ref()

const handleRegister = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    const { confirmPassword, ...userData } = registerForm
    const success = await userStore.register(userData)

    if (success) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error('注册失败，请重试')
    }
  } catch (error) {
    console.error('Register error:', error)
    ElMessage.error('注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-container">
      <ElCard class="register-card">
        <template #header>
          <div class="register-header">
            <h2>注册</h2>
            <p>创建您的成都酒店管理系统账号</p>
          </div>
        </template>

        <ElForm
          ref="formRef"
          :model="registerForm"
          :rules="rules"
          label-width="0"
          size="large"
        >
          <ElFormItem prop="username">
            <ElInput
              v-model="registerForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
            />
          </ElFormItem>

          <ElFormItem prop="email">
            <ElInput
              v-model="registerForm.email"
              type="email"
              placeholder="请输入邮箱地址"
              prefix-icon="Message"
            />
          </ElFormItem>

          <ElFormItem prop="phone">
            <ElInput
              v-model="registerForm.phone"
              placeholder="请输入手机号码"
              prefix-icon="Phone"
            />
          </ElFormItem>

          <ElFormItem prop="password">
            <ElInput
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
            />
          </ElFormItem>

          <ElFormItem prop="confirmPassword">
            <ElInput
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请确认密码"
              prefix-icon="Lock"
              show-password
              @keyup.enter="handleRegister"
            />
          </ElFormItem>

          <ElFormItem>
            <ElButton
              type="primary"
              style="width: 100%"
              :loading="loading"
              @click="handleRegister"
            >
              注册
            </ElButton>
          </ElFormItem>

          <div class="register-footer">
            <span>已有账号？</span>
            <router-link to="/login" class="login-link">
              立即登录
            </router-link>
          </div>
        </ElForm>
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-container {
  width: 100%;
  max-width: 450px;
}

.register-card {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  overflow: hidden;
}

.register-header {
  text-align: center;
  margin-bottom: 20px;
}

.register-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.register-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #909399;
}

.login-link {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
  margin-left: 5px;
}

.login-link:hover {
  text-decoration: underline;
}

:deep(.el-card__header) {
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
}

@media (max-width: 480px) {
  .register-container {
    padding: 0 10px;
  }

  .register-header h2 {
    font-size: 20px;
  }
}
</style>