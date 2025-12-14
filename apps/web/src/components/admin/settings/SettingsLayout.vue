<template>
  <div class="settings-layout">
    <div class="layout-header" v-if="title || description">
      <div class="header-content">
        <h2 v-if="title" class="layout-title">{{ title }}</h2>
        <p v-if="description" class="layout-description">{{ description }}</p>
      </div>
      <div class="header-actions" v-if="$slots.actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="layout-content">
      <slot />
    </div>

    <div class="layout-footer" v-if="showFooter">
      <slot name="footer">
        <div class="default-footer">
          <el-button @click="handleCancel" v-if="showCancel">取消</el-button>
          <el-button type="primary" @click="handleConfirm" :loading="loading" :disabled="disabled">
            {{ confirmText }}
          </el-button>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title?: string
  description?: string
  showFooter?: boolean
  showCancel?: boolean
  confirmText?: string
  loading?: boolean
  disabled?: boolean
}

interface Emits {
  (e: 'confirm'): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  showFooter: true,
  showCancel: true,
  confirmText: '保存',
  loading: false,
  disabled: false
})

const emit = defineEmits<Emits>()

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  emit('cancel')
}
</script>

<style scoped>
.settings-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.layout-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.header-content {
  flex: 1;
}

.layout-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.layout-description {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.header-actions {
  margin-left: 20px;
}

.layout-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.layout-footer {
  padding: 20px 24px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.default-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>