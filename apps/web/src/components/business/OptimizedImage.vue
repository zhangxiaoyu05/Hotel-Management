<template>
  <div class="optimized-image" :class="{ 'with-placeholder': showPlaceholder }">
    <!-- 占位符 -->
    <div v-if="showPlaceholder && !isLoaded" class="image-placeholder">
      <span class="placeholder-icon">📷</span>
      <span class="placeholder-text">图片加载中...</span>
    </div>

    <!-- 实际图片 -->
    <img
      v-show="isLoaded || !lazy"
      ref="imageRef"
      :src="lazy ? undefined : optimizedSrc"
      :data-src="lazy ? optimizedSrc : undefined"
      :alt="alt"
      :class="imageClasses"
      :style="imageStyles"
      @load="handleLoad"
      @error="handleError"
    />

    <!-- 错误状态 -->
    <div v-if="hasError" class="image-error">
      <span class="error-icon">⚠️</span>
      <span class="error-text">图片加载失败</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, toRefs } from 'vue'

interface Props {
  src: string
  alt?: string
  width?: number | string
  height?: number | string
  lazy?: boolean
  objectFit?: 'cover' | 'contain' | 'fill' | 'none' | 'scale-down'
  quality?: number
  format?: 'webp' | 'jpeg' | 'png'
  showPlaceholder?: boolean
  placeholderColor?: string
  borderRadius?: string
  enableHover?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  alt: '',
  lazy: true,
  objectFit: 'cover',
  quality: 80,
  format: 'webp',
  showPlaceholder: true,
  placeholderColor: '#f0f0f0',
  borderRadius: '4px',
  enableHover: true
})

// 响应式数据
const imageRef = ref<HTMLImageElement>()
const isLoaded = ref(false)
const hasError = ref(false)
const retryCount = ref(0)
const maxRetries = 3

// 计算属性
const optimizedSrc = computed(() => {
  if (!props.src) return ''

  try {
    const url = new URL(props.src, window.location.origin)

    // 如果是外部URL，直接返回
    if (url.origin !== window.location.origin) {
      return props.src
    }

    // 添加优化参数
    const searchParams = new URLSearchParams(url.search)

    // 质量参数
    searchParams.set('quality', props.quality.toString())

    // 格式参数（如果支持WebP）
    if (props.format === 'webp' && supportsWebP()) {
      searchParams.set('format', 'webp')
    }

    // 添加版本号防止缓存问题
    searchParams.set('v', Date.now().toString())

    url.search = searchParams.toString()
    return url.toString()
  } catch (error) {
    console.warn('Invalid image URL:', props.src)
    return props.src
  }
})

const imageClasses = computed(() => ({
  'lazy-loading': props.lazy && !isLoaded.value,
  'lazy-loaded': isLoaded.value,
  'lazy-error': hasError.value,
  'hover-effect': props.enableHover,
  'optimized-img': true
}))

const imageStyles = computed(() => ({
  width: typeof props.width === 'number' ? `${props.width}px` : props.width,
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
  objectFit: props.objectFit,
  borderRadius: props.borderRadius,
  backgroundColor: props.placeholderColor
}))

// 方法
const supportsWebP = (): boolean => {
  const canvas = document.createElement('canvas')
  canvas.width = 1
  canvas.height = 1
  return canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0
}

const handleLoad = () => {
  isLoaded.value = true
  hasError.value = false
  retryCount.value = 0
}

const handleError = () => {
  if (retryCount.value < maxRetries) {
    retryCount.value++
    // 延迟重试
    setTimeout(() => {
      if (imageRef.value) {
        imageRef.value.src = optimizedSrc.value
      }
    }, 1000 * retryCount.value)
  } else {
    hasError.value = true
    console.error('Failed to load image after retries:', props.src)
  }
}

const retry = () => {
  hasError.value = false
  retryCount.value = 0
  if (imageRef.value) {
    imageRef.value.src = optimizedSrc.value
  }
}

// 监听图片src变化
const { src } = toRefs(props)
watch(src, (newSrc) => {
  if (newSrc) {
    isLoaded.value = false
    hasError.value = false
    retryCount.value = 0
  }
})

// 懒加载处理
onMounted(() => {
  if (props.lazy && imageRef.value) {
    // 如果没有全局注册lazy指令，手动实现
    if (!imageRef.value.hasAttribute('data-lazy-loaded')) {
      setupIntersectionObserver()
    }
  }
})

const setupIntersectionObserver = () => {
  if (!imageRef.value) return

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          const img = entry.target as HTMLImageElement
          if (img.dataset.src) {
            img.src = img.dataset.src
            img.removeAttribute('data-src')
            img.setAttribute('data-lazy-loaded', 'true')
          }
          observer.unobserve(img)
        }
      })
    },
    {
      rootMargin: '50px',
      threshold: 0.1
    }
  )

  observer.observe(imageRef.value)
  onUnmounted(() => {
    observer.disconnect()
  })
}

// 暴露方法给父组件
defineExpose({
  retry,
  reload: () => {
    isLoaded.value = false
    hasError.value = false
    retryCount.value = 0
    if (imageRef.value) {
      imageRef.value.src = optimizedSrc.value
    }
  }
})
</script>

<style scoped>
.optimized-image {
  position: relative;
  overflow: hidden;
  display: inline-block;
}

.image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  color: #999;
  font-size: 12px;
  padding: 8px;
  border-radius: inherit;
}

.placeholder-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.placeholder-text {
  font-size: 12px;
  opacity: 0.8;
}

.optimized-img {
  transition: all 0.3s ease;
  display: block;
}

.optimized-img.lazy-loading {
  opacity: 0;
  transform: scale(0.95);
}

.optimized-img.lazy-loaded {
  opacity: 1;
  transform: scale(1);
}

.optimized-img.lazy-error {
  opacity: 0.6;
  filter: grayscale(1);
}

.optimized-img.hover-effect:hover {
  transform: scale(1.05);
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #666;
  font-size: 12px;
  padding: 16px;
  border-radius: inherit;
}

.error-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.error-text {
  font-size: 12px;
  text-align: center;
}

.with-placeholder {
  min-height: 100px;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 响应式优化 */
@media (max-width: 768px) {
  .image-placeholder,
  .image-error {
    padding: 12px;
    font-size: 11px;
  }

  .placeholder-icon,
  .error-icon {
    font-size: 20px;
  }

  .optimized-img.hover-effect:hover {
    transform: scale(1.02);
  }
}
</style>