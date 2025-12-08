<template>
  <div class="image-upload">
    <div class="upload-label">上传图片</div>
    <div class="upload-area">
      <div
        v-for="(image, index) in imageList"
        :key="index"
        class="image-item"
      >
        <img :src="image" alt="上传的图片" class="preview-image" />
        <div class="image-overlay">
          <button
            type="button"
            class="btn-remove"
            @click="removeImage(index)"
            title="删除图片"
          >
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>

      <div
        v-if="imageList.length < maxImages"
        class="upload-placeholder"
        @click="triggerFileInput"
        @dragover.prevent
        @drop.prevent="handleDrop"
      >
        <i class="fas fa-cloud-upload-alt"></i>
        <div class="upload-text">
          <div>点击或拖拽上传</div>
          <div class="upload-hint">支持 JPG、PNG，最大 5MB</div>
        </div>
      </div>
    </div>

    <input
      ref="fileInput"
      type="file"
      multiple
      accept="image/*"
      @change="handleFileSelect"
      style="display: none"
    />

    <div class="upload-info">
      <span>{{ imageList.length }}/{{ maxImages }}</span>
      <span v-if="error" class="error-text">{{ error }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  modelValue: string[]
  maxImages?: number
  maxSize?: number // KB
  error?: string
}

const props = withDefaults(defineProps<Props>(), {
  maxImages: 5,
  maxSize: 5 * 1024 // 5MB
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const fileInput = ref<HTMLInputElement>()

const imageList = computed({
  get: () => props.modelValue || [],
  set: (value: string[]) => emit('update:modelValue', value)
})

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  processFiles(files)
}

const handleDrop = (event: DragEvent) => {
  const files = Array.from(event.dataTransfer?.files || [])
  processFiles(files)
}

const processFiles = async (files: File[]) => {
  if (imageList.value.length + files.length > props.maxImages) {
    return
  }

  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      continue
    }

    if (file.size > props.maxSize * 1024) {
      continue
    }

    try {
      const imageUrl = await fileToUrl(file)
      imageList.value = [...imageList.value, imageUrl]
    } catch (error) {
      console.error('文件处理失败:', error)
    }
  }
}

const fileToUrl = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const removeImage = (index: number) => {
  const newImageList = [...imageList.value]
  newImageList.splice(index, 1)
  imageList.value = newImageList
}
</script>

<style scoped>
.image-upload {
  margin-bottom: 1.5rem;
}

.upload-label {
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.75rem;
}

.upload-area {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 0.5rem;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.btn-remove {
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 50%;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-remove:hover {
  background: #dc2626;
}

.upload-placeholder {
  width: 120px;
  height: 120px;
  border: 2px dashed #d1d5db;
  border-radius: 0.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #f9fafb;
}

.upload-placeholder:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.upload-placeholder i {
  font-size: 2rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.upload-text {
  text-align: center;
  font-size: 0.75rem;
  color: #6b7280;
}

.upload-hint {
  font-size: 0.625rem;
  color: #9ca3af;
  margin-top: 0.25rem;
}

.upload-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.875rem;
  color: #6b7280;
  margin-top: 0.5rem;
}

.error-text {
  color: #ef4444;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .image-item,
  .upload-placeholder {
    width: 100px;
    height: 100px;
  }

  .upload-area {
    gap: 0.75rem;
  }

  .upload-placeholder i {
    font-size: 1.5rem;
  }

  .upload-text {
    font-size: 0.625rem;
  }
}
</style>