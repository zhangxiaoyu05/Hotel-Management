<template>
  <div class="basic-settings">
    <div class="settings-content">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        label-position="left"
        class="settings-form"
      >
        <!-- 系统信息 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><Setting /></el-icon>
            系统信息
          </h3>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="系统名称" prop="systemName">
                <el-input
                  v-model="formData.systemName"
                  placeholder="请输入系统名称"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="营业时间">
                <el-input
                  v-model="formData.businessHours"
                  placeholder="例如：24小时服务"
                  maxlength="50"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="系统Logo">
            <div class="logo-upload">
              <el-upload
                class="logo-uploader"
                action="#"
                :show-file-list="false"
                :before-upload="beforeLogoUpload"
                :http-request="handleLogoUpload"
              >
                <img v-if="formData.systemLogo" :src="formData.systemLogo" class="logo-image" />
                <div v-else class="logo-placeholder">
                  <el-icon class="logo-icon"><Plus /></el-icon>
                  <div class="logo-text">上传Logo</div>
                </div>
              </el-upload>
              <div class="logo-tips">
                <p>支持JPG、PNG格式，文件大小不超过2MB</p>
                <p>建议尺寸：200x50像素</p>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="系统描述">
            <el-input
              v-model="formData.systemDescription"
              type="textarea"
              :rows="3"
              placeholder="请输入系统描述"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </div>

        <!-- 联系信息 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><Phone /></el-icon>
            联系信息
          </h3>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="联系电话" prop="contactPhone">
                <el-input
                  v-model="formData.contactPhone"
                  placeholder="请输入联系电话"
                  maxlength="20"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="联系邮箱" prop="contactEmail">
                <el-input
                  v-model="formData.contactEmail"
                  placeholder="请输入联系邮箱"
                  maxlength="100"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="联系地址">
            <el-input
              v-model="formData.contactAddress"
              placeholder="请输入联系地址"
              maxlength="200"
            />
          </el-form-item>
        </div>

        <!-- 预览区域 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><View /></el-icon>
            效果预览
          </h3>

          <div class="preview-card">
            <div class="preview-header">
              <img v-if="formData.systemLogo" :src="formData.systemLogo" class="preview-logo" />
              <h3 class="preview-title">{{ formData.systemName || '酒店管理系统' }}</h3>
            </div>
            <div class="preview-content">
              <p v-if="formData.systemDescription" class="preview-desc">
                {{ formData.systemDescription }}
              </p>
              <div class="preview-contact">
                <p v-if="formData.contactPhone">
                  <el-icon><Phone /></el-icon>
                  {{ formData.contactPhone }}
                </p>
                <p v-if="formData.contactEmail">
                  <el-icon><Message /></el-icon>
                  {{ formData.contactEmail }}
                </p>
                <p v-if="formData.businessHours">
                  <el-icon><Clock /></el-icon>
                  {{ formData.businessHours }}
                </p>
              </div>
            </div>
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
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Phone, View, Plus, Message, Clock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadProps } from 'element-plus'
import type { BasicSettings } from '@/stores/systemSettings'

// Props
interface Props {
  modelValue: BasicSettings
}

interface Emits {
  (e: 'update:modelValue', value: BasicSettings): void
  (e: 'save', value: BasicSettings): void
  (e: 'loading', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const formRef = ref<FormInstance>()
const saving = ref(false)
const originalData = ref<BasicSettings>({} as BasicSettings)

const formData = reactive<BasicSettings>({
  systemName: '',
  systemLogo: '',
  contactPhone: '',
  contactEmail: '',
  contactAddress: '',
  systemDescription: '',
  businessHours: ''
})

// 表单验证规则
const rules: FormRules = {
  systemName: [
    { required: true, message: '请输入系统名称', trigger: 'blur' },
    { min: 2, max: 100, message: '系统名称长度在2-100个字符之间', trigger: 'blur' }
  ],
  contactPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  contactEmail: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

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
const beforeLogoUpload: UploadProps['beforeUpload'] = (file) => {
  const isJPGorPNG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPGorPNG) {
    ElMessage.error('Logo只能是JPG或PNG格式!')
  }
  if (!isLt2M) {
    ElMessage.error('Logo大小不能超过2MB!')
  }

  return isJPGorPNG && isLt2M
}

const handleLogoUpload = async (options: any) => {
  try {
    emit('loading', true)

    // TODO: 这里应该调用文件上传API
    // const response = await fileUploadApi(options.file)
    // formData.systemLogo = response.data.url

    // 暂时使用本地预览
    const reader = new FileReader()
    reader.onload = (e) => {
      formData.systemLogo = e.target?.result as string
    }
    reader.readAsDataURL(options.file)

    ElMessage.success('Logo上传成功')
  } catch (error) {
    ElMessage.error('Logo上传失败')
  } finally {
    emit('loading', false)
  }
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
      '确定要重置所有设置吗？未保存的更改将丢失。',
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
.basic-settings {
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

.logo-upload {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.logo-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}

.logo-uploader:hover {
  border-color: #409eff;
}

.logo-uploader :deep(.el-upload) {
  border: none;
  width: 200px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-image {
  width: 200px;
  height: 50px;
  object-fit: contain;
}

.logo-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #8c939d;
  background: #fafafa;
}

.logo-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.logo-text {
  font-size: 12px;
}

.logo-tips {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.logo-tips p {
  margin: 0 0 4px 0;
}

.logo-tips p:last-child {
  margin-bottom: 0;
}

.preview-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  background: #fafafa;
  max-width: 600px;
}

.preview-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.preview-logo {
  height: 30px;
  margin-right: 12px;
  object-fit: contain;
}

.preview-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.preview-content {
  color: #606266;
}

.preview-desc {
  margin: 0 0 16px 0;
  line-height: 1.6;
}

.preview-contact p {
  display: flex;
  align-items: center;
  margin: 0 0 8px 0;
  font-size: 14px;
}

.preview-contact p:last-child {
  margin-bottom: 0;
}

.preview-contact .el-icon {
  margin-right: 8px;
  font-size: 14px;
  color: #909399;
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

:deep(.el-textarea__inner) {
  border-radius: 6px;
}
</style>