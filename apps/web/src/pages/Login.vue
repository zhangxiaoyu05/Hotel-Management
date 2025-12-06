<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage, ElCard, ElCheckbox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest } from '@/services/authService'

const router = useRouter()
const authStore = useAuthStore()

const loginForm = reactive({
  login: '', // 支持用户名、邮箱或手机号
  password: '',
  rememberMe: false
})

const loading = ref(false)
const formRef = ref()

const validateLoginInput = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入用户名、邮箱或手机号'))
  } else if (value.includes('@')) {
    // 简单的邮箱格式验证
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(value)) {
      callback(new Error('请输入有效的邮箱地址'))
    } else {
      callback()
    }
  } else if (/^\d+$/.test(value)) {
    // 纯数字，验证手机号
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(value)) {
      callback(new Error('请输入有效的手机号'))
    } else {
      callback()
    }
  } else {
    // 用户名
    if (value.length < 3 || value.length > 20) {
      callback(new Error('用户名长度应在 3 到 20 个字符'))
    } else {
      callback()
    }
  }
}

const rules = {
  login: [
    { validator: validateLoginInput, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    const loginData: LoginRequest = {
      identifier: loginForm.login,
      password: loginForm.password
    }

    await authStore.login(loginData)

    ElMessage.success('登录成功')

    // 如果选择了记住我，保存登录状态到 localStorage
    if (loginForm.rememberMe) {
      localStorage.setItem('remember_me', 'true')
      localStorage.setItem('last_login', loginForm.login)
    } else {
      localStorage.removeItem('remember_me')
      localStorage.removeItem('last_login')
    }

    const redirectPath = router.currentRoute.value.query.redirect as string
    router.push(redirectPath || '/')
  } catch (error: any) {
    console.error('Login error:', error)
    ElMessage.error(error.message || '登录失败，请重试')
  } finally {
    loading.value = false
  }
}

// 组件挂载时检查是否需要恢复登录信息
onMounted(() => {
  const rememberMe = localStorage.getItem('remember_me')
  const lastLogin = localStorage.getItem('last_login')

  if (rememberMe === 'true' && lastLogin) {
    loginForm.login = lastLogin
    loginForm.rememberMe = true
  }
})
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <ElCard class="login-card">
        <template #header>
          <div class="login-header">
            <h2>登录</h2>
            <p>欢迎回到成都酒店管理系统</p>
          </div>
        </template>

        <ElForm
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          label-width="0"
          size="large"
        >
          <ElFormItem prop="login">
            <ElInput
              v-model="loginForm.login"
              placeholder="请输入用户名、邮箱或手机号"
              prefix-icon="User"
              @keyup.enter="handleLogin"
            />
          </ElFormItem>

          <ElFormItem prop="password">
            <ElInput
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </ElFormItem>

          <ElFormItem>
            <ElCheckbox v-model="loginForm.rememberMe">
              记住我
            </ElCheckbox>
          </ElFormItem>

          <ElFormItem>
            <ElButton
              type="primary"
              style="width: 100%"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </ElButton>
          </ElFormItem>

          <div class="login-footer">
            <span>还没有账号？</span>
            <router-link to="/register" class="register-link">
              立即注册
            </router-link>
          </div>
        </ElForm>
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 400px;
}

.login-card {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  overflow: hidden;
}

.login-header {
  text-align: center;
  margin-bottom: 20px;
}

.login-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.login-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #909399;
}

.register-link {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
  margin-left: 5px;
}

.register-link:hover {
  text-decoration: underline;
}

:deep(.el-card__header) {
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
}

@media (max-width: 480px) {
  .login-container {
    padding: 0 10px;
  }

  .login-header h2 {
    font-size: 20px;
  }
}
</style>