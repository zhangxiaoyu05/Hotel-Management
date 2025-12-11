<template>
  <div class="rating-stars">
    <div class="rating-item">
      <label class="rating-label">{{ label }}</label>
      <div class="stars-container">
        <div
          v-for="star in 5"
          :key="star"
          class="star"
          :class="{ active: star <= modelValue, hover: star <= hoverValue }"
          @click="setRating(star)"
          @mouseenter="setHover(star)"
          @mouseleave="setHover(0)"
        >
          <i class="fas fa-star"></i>
        </div>
      </div>
      <span class="rating-text" v-if="showText">{{ ratingText }}</span>
    </div>
    <div class="rating-error" v-if="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  modelValue: number
  label: string
  error?: string
  showText?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showText: true
})

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

const hoverValue = ref(0)

const ratingText = computed(() => {
  if (!props.showText) return ''
  const rating = props.modelValue
  switch (rating) {
    case 1: return '非常差'
    case 2: return '差'
    case 3: return '一般'
    case 4: return '好'
    case 5: return '非常好'
    default: return ''
  }
})

const setRating = (rating: number) => {
  emit('update:modelValue', rating)
}

const setHover = (value: number) => {
  hoverValue.value = value
}
</script>

<style scoped>
.rating-stars {
  margin-bottom: 1.5rem;
}

.rating-item {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.rating-label {
  min-width: 100px;
  font-weight: 500;
  color: #374151;
}

.stars-container {
  display: flex;
  gap: 0.25rem;
}

.star {
  font-size: 1.5rem;
  color: #d1d5db;
  cursor: pointer;
  transition: color 0.2s ease;
}

.star:hover,
.star.hover {
  color: #fbbf24;
}

.star.active {
  color: #f59e0b;
}

.rating-text {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
}

.rating-error {
  color: #ef4444;
  font-size: 0.875rem;
  margin-top: 0.25rem;
  margin-left: 100px;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .rating-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .rating-label {
    min-width: auto;
  }

  .rating-error {
    margin-left: 0;
  }
}
</style>