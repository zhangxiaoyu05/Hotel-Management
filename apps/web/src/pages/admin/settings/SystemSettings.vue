<template>
  <div class="system-settings">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>系统设置</h1>
      <p class="page-description">配置系统参数和规则，灵活调整系统行为</p>
    </div>

    <!-- 设置选项卡 -->
    <div class="settings-container">
      <el-tabs v-model="activeTab" type="card" @tab-change="handleTabChange">
        <el-tab-pane label="基础设置" name="basic">
          <BasicSettings
            v-if="activeTab === 'basic'"
            v-model="systemSettingsStore.basicSettings"
            @save="handleSaveBasic"
            @loading="handleLoadingChange"
          />
        </el-tab-pane>

        <el-tab-pane label="业务规则" name="business">
          <BusinessRules
            v-if="activeTab === 'business'"
            v-model="systemSettingsStore.businessRules"
            @save="handleSaveBusiness"
            @loading="handleLoadingChange"
          />
        </el-tab-pane>

        <el-tab-pane label="通知设置" name="notification">
          <NotificationSettings
            v-if="activeTab === 'notification'"
            v-model="systemSettingsStore.notificationSettings"
            @save="handleSaveNotification"
            @loading="handleLoadingChange"
          />
        </el-tab-pane>

        <el-tab-pane label="安全设置" name="security">
          <SecuritySettings
            v-if="activeTab === 'security'"
            v-model="systemSettingsStore.securitySettings"
            @save="handleSaveSecurity"
            @loading="handleLoadingChange"
          />
        </el-tab-pane>

        <el-tab-pane label="备份设置" name="backup">
          <BackupSettings
            v-if="activeTab === 'backup'"
            v-model="systemSettingsStore.backupSettings"
            @save="handleSaveBackup"
            @execute-backup="handleExecuteBackup"
            @loading="handleLoadingChange"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSystemSettingsStore } from '@/stores/systemSettings'
import BasicSettings from '@/components/admin/settings/BasicSettings.vue'
import BusinessRules from '@/components/admin/settings/BusinessRules.vue'
import NotificationSettings from '@/components/admin/settings/NotificationSettings.vue'
import SecuritySettings from '@/components/admin/settings/SecuritySettings.vue'
import BackupSettings from '@/components/admin/settings/BackupSettings.vue'

const systemSettingsStore = useSystemSettingsStore()

// 响应式数据
const activeTab = ref('basic')
const loading = ref(false)

// 生命周期
onMounted(() => {
  loadTabData(activeTab.value)
})

// 方法
const handleTabChange = (tabName: string) => {
  activeTab.value = tabName
  loadTabData(tabName)
}

const loadTabData = async (tabName: string) => {
  try {
    loading.value = true
    switch (tabName) {
      case 'basic':
        await systemSettingsStore.fetchBasicSettings()
        break
      case 'business':
        await systemSettingsStore.fetchBusinessRules()
        break
      case 'notification':
        await systemSettingsStore.fetchNotificationSettings()
        break
      case 'security':
        await systemSettingsStore.fetchSecuritySettings()
        break
      case 'backup':
        await systemSettingsStore.fetchBackupSettings()
        break
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
    console.error('Failed to load tab data:', error)
  } finally {
    loading.value = false
  }
}

const handleSaveBasic = async (settings: any) => {
  try {
    await systemSettingsStore.updateBasicSettings(settings)
    ElMessage.success('基础设置保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '基础设置保存失败')
  }
}

const handleSaveBusiness = async (rules: any) => {
  try {
    await systemSettingsStore.updateBusinessRules(rules)
    ElMessage.success('业务规则保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '业务规则保存失败')
  }
}

const handleSaveNotification = async (settings: any) => {
  try {
    await systemSettingsStore.updateNotificationSettings(settings)
    ElMessage.success('通知设置保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '通知设置保存失败')
  }
}

const handleSaveSecurity = async (settings: any) => {
  try {
    await systemSettingsStore.updateSecuritySettings(settings)
    ElMessage.success('安全设置保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '安全设置保存失败')
  }
}

const handleSaveBackup = async (settings: any) => {
  try {
    await systemSettingsStore.updateBackupSettings(settings)
    ElMessage.success('备份设置保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '备份设置保存失败')
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

    await systemSettingsStore.executeBackup()
    ElMessage.success('备份执行成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '备份执行失败')
    }
  }
}

const handleLoadingChange = (isLoading: boolean) => {
  loading.value = isLoading
}
</script>

<style scoped>
.system-settings {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  margin-bottom: 24px;
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.page-description {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.settings-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.settings-container :deep(.el-tabs__header) {
  margin: 0;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

.settings-container :deep(.el-tabs__content) {
  padding: 0;
}

.settings-container :deep(.el-tab-pane) {
  background: white;
}
</style>