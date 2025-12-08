<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElUpload, ElImage, ElButton, ElMessage, ElIcon } from 'element-plus'
import { Plus, ZoomIn, Delete } from '@element-plus/icons-vue'
import type { UploadFile, UploadProps } from 'element-plus'

interface Props {
  iconUrl?: string
  maxSize?: number // MB
}

interface Emits {
  (e: 'change', url: string): void
}

const props = withDefaults(defineProps<Props>(), {
  iconUrl: '',
  maxSize: 5
})

const emit = defineEmits<Emits>()

const uploadRef = ref()
const uploading = ref(false)
const previewVisible = ref(false)
const currentIconUrl = computed(() => props.iconUrl)

// 允许的文件类型
const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/svg+xml', 'image/webp']

// 验证文件
const validateFile = (file: File): boolean => {
  // 检查文件类型
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('只支持 JPG、PNG、SVG、WebP 格式的图片')
    return false
  }

  // 检查文件大小
  const isLtMaxSize = file.size / 1024 / 1024 < props.maxSize
  if (!isLtMaxSize) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`)
    return false
  }

  return true
}

// 处理文件上传
const handleUpload: UploadProps['httpRequest'] = async(options) => {
  const { file, onSuccess, onError } = options

  if (!validateFile(file as File)) {
    onError(new Error('文件验证失败'))
    return
  }

  uploading.value = true

  try {
    // 调用实际的文件上传 API
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch('/api/files/upload?type=roomtype', {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error('上传失败')
    }

    const result = await response.json()

    if (result.success) {
      emit('change', result.data.url)
      onSuccess?.(result.data)
      ElMessage.success('图标上传成功')
    } else {
      throw new Error(result.error?.message || '上传失败')
    }
  } catch (error) {
    onError?.(error as Error)
    ElMessage.error(error instanceof Error ? error.message : '图标上传失败')
  } finally {
    uploading.value = false
  }
}

// 处理上传错误
const handleError = () => {
  uploading.value = false
  ElMessage.error('图标上传失败')
}

// 删除图标
const handleRemove = () => {
  emit('change', '')
  ElMessage.success('图标已删除')
}

// 预览图标
const handlePreview = () => {
  previewVisible.value = true
}

// 手动触发上传
const triggerUpload = () => {
  uploadRef.value?.handleClick()
}
</script>

<template>
  <div class="room-type-icon">
    <div v-if="currentIconUrl" class="icon-preview">
      <div class="icon-image-wrapper">
        <ElImage
          :src="currentIconUrl"
          :preview-src-list="[currentIconUrl]"
          fit="cover"
          class="icon-image"
          lazy
        >
          <template #error>
            <div class="image-error">
              <i class="el-icon-picture-outline"></i>
              <span>加载失败</span>
            </div>
          </template>
        </ElImage>
      </div>

      <div class="icon-actions">
        <ElButton size="small" @click="triggerUpload">
          <i class="el-icon-edit"></i>
          更换
        </ElButton>
        <ElButton size="small" type="danger" @click="handleRemove">
          <i class="el-icon-delete"></i>
          删除
        </ElButton>
      </div>
    </div>

    <div v-else class="icon-upload">
      <ElUpload
        ref="uploadRef"
        class="upload-component"
        :auto-upload="true"
        :show-file-list="false"
        :http-request="handleUpload"
        :on-error="handleError"
        :accept="allowedTypes.join(',')"
        :disabled="uploading"
      >
        <div class="upload-area" :class="{ uploading }">
          <ElIcon v-if="!uploading" class="upload-icon">
            <Plus />
          </ElIcon>
          <div v-else class="uploading-spinner">
            <i class="el-icon-loading"></i>
          </div>

          <div class="upload-text">
            <div v-if="!uploading">点击上传图标</div>
            <div v-else>上传中...</div>
          </div>
        </div>
      </ElUpload>
    </div>

    <div class="upload-tips">
      <p>支持 JPG、PNG、SVG、WebP 格式</p>
      <p>建议尺寸：64x64 像素，文件大小不超过 {{ maxSize }}MB</p>
    </div>
  </div>
</template>

<style scoped>
.room-type-icon {
  width: 100%;
}

.icon-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.icon-image-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}

.icon-image {
  width: 100%;
  height: 100%;
  display: block;
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  color: #909399;
  font-size: 12px;
  gap: 4px;
}

.icon-actions {
  display: flex;
  gap: 8px;
}

.icon-upload {
  width: 100%;
}

.upload-component {
  width: 100%;
}

.upload-area {
  width: 80px;
  height: 80px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fafafa;
}

.upload-area:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.upload-area.uploading {
  border-color: #409eff;
  background-color: #f0f9ff;
  cursor: not-allowed;
}

.upload-icon {
  font-size: 24px;
  color: #909399;
  margin-bottom: 8px;
}

.uploading-spinner {
  font-size: 24px;
  color: #409eff;
  margin-bottom: 8px;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.upload-text {
  font-size: 12px;
  color: #606266;
  text-align: center;
  line-height: 1.4;
}

.upload-tips {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.upload-tips p {
  margin: 2px 0;
}

@media (max-width: 768px) {
  .icon-preview {
    align-items: flex-start;
  }

  .icon-actions {
    width: 100%;
  }

  .upload-area {
    margin: 0 auto;
  }
}
</style>