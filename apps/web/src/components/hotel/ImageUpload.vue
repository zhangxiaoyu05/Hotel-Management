<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hotelService } from '../../services/hotelService'

interface Props {
  modelValue?: string[]
  maxCount?: number
  accept?: string
  fileSize?: number // MB
  placeholder?: string
}

interface Emits {
  (e: 'update:modelValue', images: string[]): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  maxCount: 10,
  accept: 'image/*',
  fileSize: 5,
  placeholder: '点击或拖拽文件到此区域上传'
})

const emit = defineEmits<Emits>()

const uploadRef = ref()
const uploading = ref(false)
const dragOver = ref(false)

const images = ref<string[]>([...props.modelValue])

function emitChange() {
  emit('update:modelValue', [...images.value])
}

function handleFileChange(file: File) {
  validateAndUpload(file)
}

async function validateAndUpload(file: File) {
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }

  // 检查文件大小
  const maxSizeMB = props.fileSize
  const maxSizeBytes = maxSizeMB * 1024 * 1024
  if (file.size > maxSizeBytes) {
    ElMessage.error(`文件大小不能超过 ${maxSizeMB}MB`)
    return
  }

  // 检查数量限制
  if (images.value.length >= props.maxCount) {
    ElMessage.error(`最多只能上传 ${props.maxCount} 张图片`)
    return
  }

  uploadFile(file)
}

async function uploadFile(file: File) {
  uploading.value = true

  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', 'hotel')

    const response = await fetch('/api/files/upload', {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error('上传失败')
    }

    const result = await response.json()
    if (result.success) {
      images.value.push(result.data.url)
      emitChange()
      ElMessage.success('图片上传成功')
    } else {
      throw new Error(result.error?.message || '上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    uploading.value = false
  }
}

function removeImage(index: number) {
  ElMessageBox.confirm('确定要删除这张图片吗？', '确认删除', {
    type: 'warning'
  }).then(() => {
    images.value.splice(index, 1)
    emitChange()
    ElMessage.success('图片删除成功')
  }).catch(() => {
    // 用户取消删除
  })
}

function setMainImage(index: number) {
  if (index === 0) return

  const [mainImage] = images.value.splice(index, 1)
  images.value.unshift(mainImage)
  emitChange()
  ElMessage.success('已设置为主图')
}

function handleDragOver(event: DragEvent) {
  event.preventDefault()
  dragOver.value = true
}

function handleDragLeave(event: DragEvent) {
  event.preventDefault()
  dragOver.value = false
}

function handleDrop(event: DragEvent) {
  event.preventDefault()
  dragOver.value = false

  const files = event.dataTransfer?.files
  if (!files) return

  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    if (images.value.length >= props.maxCount) {
      ElMessage.warning(`最多只能上传 ${props.maxCount} 张图片`)
      break
    }
    validateAndUpload(file)
  }
}

function openFileDialog() {
  uploadRef.value?.click()
}
</script>

<template>
  <div class="image-upload">
    <input
      ref="uploadRef"
      type="file"
      :accept="accept"
      :multiple="maxCount > 1"
      style="display: none"
      @change="handleFileChange($event.target.files[0])"
    />

    <!-- Upload Area -->
    <div
      v-if="images.length < maxCount"
      class="upload-area"
      :class="{ 'drag-over': dragOver, 'uploading': uploading }"
      @click="openFileDialog"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
    >
      <div class="upload-content">
        <i class="el-icon-plus upload-icon"></i>
        <div class="upload-text">
          <div>{{ placeholder }}</div>
          <div class="upload-hint">
            支持 JPG、PNG、GIF 格式，单个文件不超过 {{ fileSize }}MB
          </div>
        </div>
      </div>
      <div v-if="uploading" class="upload-loading">
        <el-icon class="is-loading">
          <i class="el-icon-loading"></i>
        </el-icon>
        <span>上传中...</span>
      </div>
    </div>

    <!-- Image Preview -->
    <div class="image-preview">
      <div
        v-for="(image, index) in images"
        :key="index"
        class="image-item"
      >
        <img :src="image" :alt="`酒店图片 ${index + 1}`" />
        <div class="image-overlay">
          <div class="image-actions">
            <el-button
              v-if="index > 0"
              type="text"
              size="small"
              @click="setMainImage(index)"
            >
              <i class="el-icon-star-on"></i>
              设为主图
            </el-button>
            <el-button
              type="text"
              size="small"
              class="delete-btn"
              @click="removeImage(index)"
            >
              <i class="el-icon-delete"></i>
              删除
            </el-button>
          </div>
          <div v-if="index === 0" class="main-image-badge">
            主图
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.image-upload {
  width: 100%;
}

.upload-area {
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.3s, background-color 0.3s;
  position: relative;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-area:hover {
  border-color: #409eff;
  background-color: #f5f7fa;
}

.upload-area.drag-over {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.upload-area.uploading {
  pointer-events: none;
  opacity: 0.7;
}

.upload-content {
  color: #606266;
}

.upload-icon {
  font-size: 24px;
  margin-bottom: 10px;
  display: block;
}

.upload-text {
  font-size: 14px;
  line-height: 1.6;
}

.upload-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.upload-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #409eff;
  font-size: 14px;
}

.image-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
  margin-top: 20px;
}

.image-item {
  position: relative;
  border-radius: 6px;
  overflow: hidden;
  aspect-ratio: 16/9;
  border: 1px solid #e4e7ed;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  opacity: 0;
  transition: opacity 0.3s;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.image-actions {
  display: flex;
  gap: 10px;
}

.image-actions .el-button {
  color: white;
  border-color: white;
}

.image-actions .el-button:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.delete-btn {
  color: #f56c6c !important;
  border-color: #f56c6c !important;
}

.delete-btn:hover {
  background-color: rgba(245, 108, 108, 0.2) !important;
}

.main-image-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #409eff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

@media (max-width: 768px) {
  .image-preview {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 10px;
  }

  .upload-area {
    padding: 30px 15px;
    min-height: 100px;
  }
}
</style>