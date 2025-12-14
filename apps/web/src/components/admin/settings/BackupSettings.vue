<template>
  <div class="backup-settings">
    <div class="settings-content">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="140px"
        label-position="left"
        class="settings-form"
      >
        <!-- 备份配置 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><FolderOpened /></el-icon>
            备份配置
          </h3>

          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="自动备份">
                <el-switch
                  v-model="formData.enableAutoBackup"
                  active-text="启用"
                  inactive-text="禁用"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="备份间隔(天)" prop="backupIntervalDays">
                <el-input-number
                  v-model="formData.backupIntervalDays"
                  :min="1"
                  :max="30"
                  :disabled="!formData.enableAutoBackup"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="保留天数" prop="backupRetentionDays">
                <el-input-number
                  v-model="formData.backupRetentionDays"
                  :min="1"
                  :max="365"
                  :disabled="!formData.enableAutoBackup"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="备份时间" prop="backupTime">
                <el-time-picker
                  v-model="backupTime"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="选择备份时间"
                  :disabled="!formData.enableAutoBackup"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="压缩类型">
                <el-select v-model="formData.backupCompressionType" placeholder="选择压缩类型">
                  <el-option label="无压缩" value="none" />
                  <el-option label="ZIP" value="zip" />
                  <el-option label="GZIP" value="gzip" />
                  <el-option label="TAR" value="tar" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 存储配置 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><Coin /></el-icon>
            存储配置
          </h3>

          <el-form-item label="存储类型" prop="backupStorageType">
            <el-radio-group v-model="formData.backupStorageType">
              <el-radio label="local">本地存储</el-radio>
              <el-radio label="cloud">云存储</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 本地存储配置 -->
          <div v-if="formData.backupStorageType === 'local'">
            <el-form-item label="存储路径">
              <el-input
                v-model="formData.localBackupPath"
                placeholder="/backups"
              />
            </el-form-item>
          </div>

          <!-- 云存储配置 -->
          <div v-if="formData.backupStorageType === 'cloud'">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="云服务商">
                  <el-select v-model="formData.cloudBackupProvider" placeholder="选择云服务商">
                    <el-option label="阿里云OSS" value="aliyun-oss" />
                    <el-option label="腾讯云COS" value="tencent-cos" />
                    <el-option label="亚马逊S3" value="aws-s3" />
                    <el-option label="华为云OBS" value="huawei-obs" />
                  </el-select>
                </el-form-item>
              </el-col>

              <el-col :span="12">
                <el-form-item label="存储桶">
                  <el-input
                    v-model="formData.cloudBackupBucket"
                    placeholder="backup-bucket"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="区域">
                  <el-input
                    v-model="formData.cloudBackupRegion"
                    placeholder="oss-cn-beijing"
                  />
                </el-form-item>
              </el-col>

              <el-col :span="12">
                <el-form-item label="AccessKey">
                  <el-input
                    v-model="formData.cloudAccessKey"
                    type="password"
                    show-password
                    placeholder="请输入AccessKey"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="SecretKey">
              <el-input
                v-model="formData.cloudSecretKey"
                type="password"
                show-password
                placeholder="请输入SecretKey"
              />
            </el-form-item>
          </div>
        </div>

        <!-- 备份内容 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><List /></el-icon>
            备份内容
          </h3>

          <el-form-item label="备份数据库">
            <el-switch
              v-model="formData.enableDatabaseBackup"
              active-text="启用"
              inactive-text="禁用"
            />
          </el-form-item>

          <el-form-item label="备份文件">
            <el-switch
              v-model="formData.enableFileBackup"
              active-text="启用"
              inactive-text="禁用"
            />
          </el-form-item>

          <el-form-item label="备份加密">
            <el-switch
              v-model="formData.enableBackupEncryption"
              active-text="启用"
              inactive-text="禁用"
            />
          </el-form-item>

          <el-form-item v-if="formData.enableBackupEncryption" label="加密密钥">
            <el-input
              v-model="formData.backupEncryptionKey"
              type="password"
              show-password
              placeholder="请输入备份加密密钥"
            />
          </el-form-item>
        </div>

        <!-- 备份状态 -->
        <div class="settings-section">
          <h3 class="section-title">
            <el-icon><Monitor /></el-icon>
            备份状态
          </h3>

          <div class="backup-status">
            <el-row :gutter="24">
              <el-col :span="8">
                <el-statistic title="最后备份时间" :value="lastBackupTime" />
              </el-col>
              <el-col :span="8">
                <el-statistic title="下次备份时间" :value="nextBackupTime" />
              </el-col>
              <el-col :span="8">
                <div class="backup-actions">
                  <el-button
                    type="primary"
                    @click="handleExecuteBackup"
                    :loading="backingUp"
                    :disabled="!formData.enableAutoBackup && formData.backupStorageType === 'cloud' && !formData.cloudAccessKey"
                  >
                    立即备份
                  </el-button>
                </div>
              </el-col>
            </el-row>
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
import { FolderOpened, Coin, List, Monitor } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { BackupSettings } from '@/stores/systemSettings'
import dayjs from 'dayjs'

// Props
interface Props {
  modelValue: BackupSettings
}

interface Emits {
  (e: 'update:modelValue', value: BackupSettings): void
  (e: 'save', value: BackupSettings): void
  (e: 'execute-backup'): void
  (e: 'loading', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const formRef = ref<FormInstance>()
const saving = ref(false)
const backingUp = ref(false)
const originalData = ref<BackupSettings>({} as BackupSettings)

const formData = reactive<BackupSettings>({
  enableAutoBackup: true,
  backupIntervalDays: 7,
  backupRetentionDays: 30,
  backupTime: '02:00',
  backupStorageType: 'local',
  localBackupPath: '/backups',
  enableBackupEncryption: true,
  enableDatabaseBackup: true,
  enableFileBackup: true,
  backupCompressionType: 'gzip'
})

const backupTime = ref('02:00')

// 计算属性
const lastBackupTime = computed(() => {
  return formData.lastBackupTime ? dayjs(formData.lastBackupTime).format('YYYY-MM-DD HH:mm') : '暂无备份记录'
})

const nextBackupTime = computed(() => {
  if (!formData.enableAutoBackup) return '未启用自动备份'
  return formData.nextBackupTime ? dayjs(formData.nextBackupTime).format('YYYY-MM-DD HH:mm') : '计算中...'
})

// 表单验证规则
const rules: FormRules = {
  backupIntervalDays: [
    { required: true, message: '请输入备份间隔天数', trigger: 'blur' },
    { type: 'number', min: 1, max: 30, message: '备份间隔天数必须在1-30之间', trigger: 'blur' }
  ],
  backupRetentionDays: [
    { required: true, message: '请输入备份保留天数', trigger: 'blur' },
    { type: 'number', min: 1, max: 365, message: '备份保留天数必须在1-365之间', trigger: 'blur' }
  ],
  backupTime: [
    { required: true, message: '请选择备份时间', trigger: 'change' }
  ],
  backupStorageType: [
    { required: true, message: '请选择存储类型', trigger: 'change' }
  ]
}

// 监听备份时间变化
watch(backupTime, (newTime) => {
  formData.backupTime = newTime
})

// 监听表单数据变化
watch(formData, (newValue) => {
  backupTime.value = newValue.backupTime || '02:00'
  emit('update:modelValue', { ...newValue })
}, { deep: true })

// 监听父组件数据变化
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    Object.assign(formData, newValue)
    backupTime.value = newValue.backupTime || '02:00'
    originalData.value = { ...newValue }
  }
}, { immediate: true, deep: true })

// 生命周期
onMounted(() => {
  if (props.modelValue) {
    Object.assign(formData, props.modelValue)
    backupTime.value = props.modelValue.backupTime || '02:00'
    originalData.value = { ...props.modelValue }
  }
})

// 方法
const handleSave = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    // 验证云存储配置
    if (formData.backupStorageType === 'cloud') {
      if (!formData.cloudBackupProvider || !formData.cloudBackupBucket || !formData.cloudAccessKey || !formData.cloudSecretKey) {
        ElMessage.error('云存储配置不完整')
        return
      }
    }

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
      '确定要重置备份设置吗？未保存的更改将丢失。',
      '确认重置',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    Object.assign(formData, originalData.value)
    backupTime.value = originalData.value.backupTime || '02:00'
    ElMessage.success('已重置到上次保存的状态')
  } catch (error) {
    // 用户取消操作
  }
}

const handleExecuteBackup = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要立即执行系统备份吗？这可能需要一些时间。',
      '确认备份',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    backingUp.value = true
    emit('loading', true)

    emit('execute-backup')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '备份执行失败')
    }
  } finally {
    backingUp.value = false
    emit('loading', false)
  }
}
</script>

<style scoped>
.backup-settings {
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

.backup-status {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.backup-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
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

:deep(.el-time-picker) {
  width: 100%;
}

:deep(.el-switch) {
  height: 22px;
}

:deep(.el-switch__label) {
  font-size: 12px;
  color: #606266;
}

:deep(.el-radio-group) {
  display: flex;
  gap: 16px;
}

:deep(.el-statistic) {
  text-align: center;
}

:deep(.el-statistic .head) {
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
}

:deep(.el-statistic .content) {
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}
</style>