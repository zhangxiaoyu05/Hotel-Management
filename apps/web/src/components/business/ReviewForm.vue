<template>
  <div class="review-form">
    <div class="form-header">
      <h3>评价您的入住体验</h3>
      <p class="form-subtitle">请分享您的真实体验，帮助其他用户做出更好的选择</p>
    </div>

    <form @submit.prevent="handleSubmit">
      <!-- 多维度评分 -->
      <div class="ratings-section">
        <h4>评分详情</h4>

        <RatingStars
          v-model="formData.overallRating"
          label="总体评价"
          :error="errors.overallRating"
        />

        <RatingStars
          v-model="formData.cleanlinessRating"
          label="清洁度"
          :error="errors.cleanlinessRating"
        />

        <RatingStars
          v-model="formData.serviceRating"
          label="服务态度"
          :error="errors.serviceRating"
        />

        <RatingStars
          v-model="formData.facilitiesRating"
          label="设施设备"
          :error="errors.facilitiesRating"
        />

        <RatingStars
          v-model="formData.locationRating"
          label="地理位置"
          :error="errors.locationRating"
        />
      </div>

      <!-- 文字评价 -->
      <div class="comment-section">
        <label for="comment" class="form-label">详细评价</label>
        <textarea
          id="comment"
          v-model="formData.comment"
          class="form-textarea"
          :class="{ error: errors.comment }"
          placeholder="请分享您的入住体验，例如房间清洁度、服务质量、设施状况等..."
          rows="5"
          maxlength="1000"
        ></textarea>
        <div class="character-count">
          {{ formData.comment.length }}/1000
        </div>
        <div v-if="errors.comment" class="error-message">{{ errors.comment }}</div>
      </div>

      <!-- 图片上传 -->
      <div class="images-section">
        <ImageUpload
          v-model="formData.images"
          :max-images="5"
          :error="errors.images"
        />
      </div>

      <!-- 匿名评价选项 -->
      <div class="anonymous-section">
        <label class="checkbox-label">
          <input
            type="checkbox"
            v-model="formData.isAnonymous"
            class="checkbox-input"
          />
          <span class="checkbox-text">匿名评价</span>
        </label>
        <p class="anonymous-hint">选择匿名后，您的姓名将不会显示在评价中</p>
      </div>

      <!-- 表单操作 -->
      <div class="form-actions">
        <button
          type="button"
          class="btn btn-secondary"
          @click="$emit('cancel')"
        >
          取消
        </button>

        <button
          type="submit"
          class="btn btn-primary"
          :disabled="isSubmitting"
        >
          <i v-if="isSubmitting" class="fas fa-spinner fa-spin"></i>
          {{ isSubmitting ? '提交中...' : '提交评价' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import RatingStars from './RatingStars.vue'
import ImageUpload from './ImageUpload.vue'

interface ReviewFormData {
  orderId: number
  overallRating: number
  cleanlinessRating: number
  serviceRating: number
  facilitiesRating: number
  locationRating: number
  comment: string
  images: string[]
  isAnonymous: boolean
}

interface Props {
  orderId: number
  initialData?: Partial<ReviewFormData>
}

const props = withDefaults(defineProps<Props>(), {
  initialData: () => ({})
})

const emit = defineEmits<{
  submit: [data: ReviewFormData]
  cancel: []
}>()

const isSubmitting = ref(false)

const formData = reactive<ReviewFormData>({
  orderId: props.orderId,
  overallRating: 0,
  cleanlinessRating: 0,
  serviceRating: 0,
  facilitiesRating: 0,
  locationRating: 0,
  comment: '',
  images: [],
  isAnonymous: false,
  ...props.initialData
})

const errors = reactive<Record<string, string>>({})

const validateForm = (): boolean => {
  const newErrors: Record<string, string> = {}

  // 验证评分
  if (formData.overallRating < 1 || formData.overallRating > 5) {
    newErrors.overallRating = '请选择总体评分'
  }

  if (formData.cleanlinessRating < 1 || formData.cleanlinessRating > 5) {
    newErrors.cleanlinessRating = '请选择清洁度评分'
  }

  if (formData.serviceRating < 1 || formData.serviceRating > 5) {
    newErrors.serviceRating = '请选择服务态度评分'
  }

  if (formData.facilitiesRating < 1 || formData.facilitiesRating > 5) {
    newErrors.facilitiesRating = '请选择设施设备评分'
  }

  if (formData.locationRating < 1 || formData.locationRating > 5) {
    newErrors.locationRating = '请选择地理位置评分'
  }

  // 验证评价内容
  if (!formData.comment.trim()) {
    newErrors.comment = '请填写评价内容'
  } else if (formData.comment.trim().length < 10) {
    newErrors.comment = '评价内容至少需要10个字符'
  }

  Object.assign(errors, newErrors)
  return Object.keys(newErrors).length === 0
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  isSubmitting.value = true

  try {
    emit('submit', { ...formData })
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  // 如果有初始数据，填充表单
  Object.assign(formData, props.initialData)
})
</script>

<style scoped>
.review-form {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
  background: white;
  border-radius: 0.75rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.form-header {
  text-align: center;
  margin-bottom: 2rem;
}

.form-header h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.5rem;
}

.form-subtitle {
  color: #6b7280;
  font-size: 0.875rem;
}

.ratings-section {
  margin-bottom: 2rem;
}

.ratings-section h4 {
  font-size: 1.125rem;
  font-weight: 500;
  color: #111827;
  margin-bottom: 1rem;
}

.comment-section {
  margin-bottom: 2rem;
}

.form-label {
  display: block;
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.5rem;
}

.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  line-height: 1.5;
  resize: vertical;
  transition: border-color 0.2s ease;
}

.form-textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-textarea.error {
  border-color: #ef4444;
}

.character-count {
  text-align: right;
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 0.25rem;
}

.error-message {
  color: #ef4444;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

.images-section {
  margin-bottom: 2rem;
}

.anonymous-section {
  margin-bottom: 2rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.checkbox-input {
  width: 1rem;
  height: 1rem;
  margin-right: 0.75rem;
  color: #3b82f6;
  border-radius: 0.25rem;
}

.checkbox-text {
  font-weight: 500;
  color: #374151;
}

.anonymous-hint {
  font-size: 0.875rem;
  color: #6b7280;
  margin-top: 0.5rem;
  margin-left: 1.75rem;
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  font-weight: 500;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .review-form {
    padding: 1rem;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>