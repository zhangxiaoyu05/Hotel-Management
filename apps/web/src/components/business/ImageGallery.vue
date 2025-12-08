<template>
  <div v-if="visible" class="image-gallery-overlay" @click="close">
    <div class="image-gallery" @click.stop>
      <!-- 关闭按钮 -->
      <button class="close-btn" @click="close">
        <span class="close-icon">×</span>
      </button>

      <!-- 图片展示区域 -->
      <div class="gallery-content">
        <!-- 主图片 -->
        <div class="main-image-container">
          <img
            v-if="currentImage"
            :src="currentImage"
            :alt="`图片 ${currentIndex + 1}`"
            class="main-image"
            @load="handleImageLoad"
            @error="handleImageError"
          />
          <div v-if="loading" class="image-loading">
            <div class="loading-spinner"></div>
            <span>加载中...</span>
          </div>
          <div v-if="error" class="image-error">
            <span class="error-icon">⚠️</span>
            <span>图片加载失败</span>
          </div>
        </div>

        <!-- 图片导航 -->
        <div v-if="images.length > 1" class="image-navigation">
          <button
            class="nav-btn prev-btn"
            :disabled="currentIndex === 0"
            @click="prevImage"
          >
            <span class="nav-icon">‹</span>
          </button>
          <button
            class="nav-btn next-btn"
            :disabled="currentIndex === images.length - 1"
            @click="nextImage"
          >
            <span class="nav-icon">›</span>
          </button>
        </div>

        <!-- 图片缩略图 -->
        <div v-if="images.length > 1" class="thumbnail-container">
          <div class="thumbnail-list">
            <div
              v-for="(image, index) in images"
              :key="index"
              :class="['thumbnail-item', { active: index === currentIndex }]"
              @click="selectImage(index)"
            >
              <img
                :src="image"
                :alt="`缩略图 ${index + 1}`"
                class="thumbnail-image"
                @error="handleThumbnailError($event, index)"
              />
            </div>
          </div>
        </div>

        <!-- 图片信息 -->
        <div class="image-info">
          <span class="image-counter">
            {{ currentIndex + 1 }} / {{ images.length }}
          </span>
          <span class="image-title">评价图片</span>
        </div>
      </div>

      <!-- 键盘导航提示 -->
      <div class="keyboard-hint">
        <span>使用 ← → 键切换图片</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

interface Props {
  visible: boolean
  images: string[]
  initialIndex?: number
}

interface Emits {
  (e: 'update:visible', visible: boolean): void
  (e: 'image-change', index: number): void
}

const props = withDefaults(defineProps<Props>(), {
  initialIndex: 0
})

const emit = defineEmits<Emits>()

// 响应式数据
const currentIndex = ref(props.initialIndex)
const loading = ref(false)
const error = ref(false)

// 计算属性
const currentImage = computed(() => {
  return props.images[currentIndex.value]
})

// 监听visible变化
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    currentIndex.value = props.initialIndex
    addKeyboardListeners()
    document.body.style.overflow = 'hidden'
  } else {
    removeKeyboardListeners()
    document.body.style.overflow = ''
  }
})

// 监听currentIndex变化
watch(currentIndex, (newIndex) => {
  emit('image-change', newIndex)
})

// 图片加载处理
const handleImageLoad = () => {
  loading.value = false
  error.value = false
}

const handleImageError = () => {
  loading.value = false
  error.value = true
}

const handleThumbnailError = (event: Event, index: number) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
  img.parentElement?.classList.add('thumbnail-error')
}

// 图片导航
const prevImage = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
    loadImage()
  }
}

const nextImage = () => {
  if (currentIndex.value < props.images.length - 1) {
    currentIndex.value++
    loadImage()
  }
}

const selectImage = (index: number) => {
  currentIndex.value = index
  loadImage()
}

const loadImage = () => {
  loading.value = true
  error.value = false
}

// 关闭画廊
const close = () => {
  emit('update:visible', false)
}

// 键盘导航
const handleKeyDown = (event: KeyboardEvent) => {
  switch (event.key) {
    case 'ArrowLeft':
      prevImage()
      break
    case 'ArrowRight':
      nextImage()
      break
    case 'Escape':
      close()
      break
  }
}

const addKeyboardListeners = () => {
  document.addEventListener('keydown', handleKeyDown)
}

const removeKeyboardListeners = () => {
  document.removeEventListener('keydown', handleKeyDown)
}

// 生命周期
onMounted(() => {
  if (props.visible) {
    addKeyboardListeners()
    document.body.style.overflow = 'hidden'
  }
})

onUnmounted(() => {
  removeKeyboardListeners()
  document.body.style.overflow = ''
})
</script>

<style scoped>
.image-gallery-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.image-gallery {
  position: relative;
  width: 90%;
  max-width: 900px;
  height: 80vh;
  display: flex;
  flex-direction: column;
  background: black;
  border-radius: 8px;
  overflow: hidden;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 50%;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.close-icon {
  font-size: 24px;
  line-height: 1;
}

.gallery-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.main-image-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  min-height: 0;
}

.main-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  animation: imageLoad 0.3s ease;
}

@keyframes imageLoad {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.image-loading,
.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: white;
  font-size: 16px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top: 3px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.error-icon {
  font-size: 48px;
}

.image-navigation {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  padding: 0 16px;
  transform: translateY(-50%);
  pointer-events: none;
}

.nav-btn {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 50%;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  pointer-events: all;
}

.nav-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.nav-icon {
  font-size: 24px;
  line-height: 1;
}

.thumbnail-container {
  padding: 16px;
  background: rgba(0, 0, 0, 0.5);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.thumbnail-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 4px 0;
}

.thumbnail-list::-webkit-scrollbar {
  height: 4px;
}

.thumbnail-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.thumbnail-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

.thumbnail-item {
  flex: 0 0 60px;
  height: 60px;
  border: 2px solid transparent;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.thumbnail-item.active {
  border-color: #007bff;
}

.thumbnail-item:hover {
  border-color: rgba(0, 123, 255, 0.5);
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-error {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

.image-info {
  position: absolute;
  bottom: 16px;
  left: 16px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 14px;
  display: flex;
  gap: 12px;
}

.image-counter {
  font-weight: 600;
}

.keyboard-hint {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 12px;
  opacity: 0.7;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .image-gallery {
    width: 100%;
    height: 100%;
    border-radius: 0;
  }

  .close-btn {
    top: 12px;
    right: 12px;
    width: 36px;
    height: 36px;
  }

  .nav-btn {
    width: 40px;
    height: 40px;
  }

  .thumbnail-item {
    flex: 0 0 50px;
    height: 50px;
  }

  .image-info {
    bottom: 12px;
    left: 12px;
    font-size: 12px;
  }

  .keyboard-hint {
    display: none;
  }
}
</style>