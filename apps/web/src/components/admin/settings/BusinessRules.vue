<template>
  <div class="business-rules">
    <div class="rules-content">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="140px"
        label-position="left"
        class="rules-form"
      >
        <!-- 预订规则 -->
        <div class="rules-section">
          <h3 class="section-title">
            <el-icon><Calendar /></el-icon>
            预订规则
          </h3>

          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="最少预订天数" prop="minBookingDays">
                <el-input-number
                  v-model="formData.minBookingDays"
                  :min="1"
                  :max="365"
                  :step="1"
                  controls-position="right"
                />
                <div class="field-tip">最少需要提前几天预订</div>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="最多预订天数" prop="maxBookingDays">
                <el-input-number
                  v-model="formData.maxBookingDays"
                  :min="1"
                  :max="365"
                  :step="1"
                  controls-position="right"
                />
                <div class="field-tip">最多可以提前几天预订</div>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="预订限制" prop="advanceBookingLimitDays">
                <el-input-number
                  v-model="formData.advanceBookingLimitDays"
                  :min="0"
                  :max="365"
                  :step="1"
                  controls-position="right"
                />
                <div class="field-tip">提前预订限制天数</div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 取消规则 -->
        <div class="rules-section">
          <h3 class="section-title">
            <el-icon><CloseBold /></el-icon>
            取消规则
          </h3>

          <el-form-item label="免费取消">
            <el-switch
              v-model="formData.enableFreeCancel"
              active-text="启用"
              inactive-text="禁用"
            />
            <div class="field-tip">允许用户在规定时间内免费取消预订</div>
          </el-form-item>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="免费取消时限" prop="freeCancelHours">
                <el-input-number
                  v-model="formData.freeCancelHours"
                  :min="0"
                  :max="720"
                  :step="1"
                  controls-position="right"
                  :disabled="!formData.enableFreeCancel"
                />
                <div class="field-tip">入住前几小时内可免费取消</div>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="取消时限" prop="cancelBeforeHours">
                <el-input-number
                  v-model="formData.cancelBeforeHours"
                  :min="0"
                  :max="720"
                  :step="1"
                  controls-position="right"
                />
                <div class="field-tip">提前几小时可取消预订</div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="取消费用" prop="cancelFeePercentage">
            <div class="fee-input">
              <el-input-number
                v-model="formData.cancelFeePercentage"
                :min="0"
                :max="100"
                :step="1"
                controls-position="right"
                :precision="2"
              />
              <span class="fee-unit">%</span>
              <span class="fee-desc">房费比例</span>
            </div>
            <div class="field-tip">取消预订时收取的费用比例</div>
          </el-form-item>
        </div>

        <!-- 价格策略 -->
        <div class="rules-section">
          <h3 class="section-title">
            <el-icon><Money /></el-icon>
            价格策略
          </h3>

          <el-form-item label="动态定价">
            <el-switch
              v-model="formData.enableDynamicPricing"
              active-text="启用"
              inactive-text="禁用"
            />
            <div class="field-tip">根据时间和季节自动调整价格</div>
          </el-form-item>

          <el-form-item label="默认房价" prop="defaultRoomPrice">
            <div class="price-input">
              <el-input-number
                v-model="formData.defaultRoomPrice"
                :min="0"
                :step="10"
                controls-position="right"
                :precision="2"
              />
              <span class="price-unit">元/晚</span>
            </div>
            <div class="field-tip">标准房型的默认价格</div>
          </el-form-item>

          <el-row :gutter="24" v-if="formData.enableDynamicPricing">
            <el-col :span="8">
              <el-form-item label="旺季价格倍数" prop="peakSeasonPriceMultiplier">
                <div class="multiplier-input">
                  <el-input-number
                    v-model="formData.peakSeasonPriceMultiplier"
                    :min="1.0"
                    :max="3.0"
                    :step="0.1"
                    controls-position="right"
                    :precision="2"
                  />
                  <span class="multiplier-desc">倍</span>
                </div>
                <div class="field-tip">旺季价格上调倍数</div>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="淡季价格倍数" prop="offSeasonPriceMultiplier">
                <div class="multiplier-input">
                  <el-input-number
                    v-model="formData.offSeasonPriceMultiplier"
                    :min="0.5"
                    :max="1.0"
                    :step="0.1"
                    controls-position="right"
                    :precision="2"
                  />
                  <span class="multiplier-desc">倍</span>
                </div>
                <div class="field-tip">淡季价格下调倍数</div>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="周末价格倍数" prop="weekendPriceMultiplier">
                <div class="multiplier-input">
                  <el-input-number
                    v-model="formData.weekendPriceMultiplier"
                    :min="1.0"
                    :max="3.0"
                    :step="0.1"
                    controls-position="right"
                    :precision="2"
                  />
                  <span class="multiplier-desc">倍</span>
                </div>
                <div class="field-tip">周末价格上调倍数</div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 预览区域 -->
        <div class="rules-section">
          <h3 class="section-title">
            <el-icon><View /></el-icon>
            规则预览
          </h3>

          <div class="preview-grid">
            <div class="preview-card">
              <h4>预订规则</h4>
              <ul>
                <li>最少预订 {{ formData.minBookingDays }} 天前</li>
                <li>最多预订 {{ formData.maxBookingDays }} 天前</li>
                <li>预订限制 {{ formData.advanceBookingLimitDays }} 天</li>
              </ul>
            </div>

            <div class="preview-card">
              <h4>取消规则</h4>
              <ul>
                <li v-if="formData.enableFreeCancel">
                  {{ formData.freeCancelHours }} 小时内免费取消
                </li>
                <li>
                  提前 {{ formData.cancelBeforeHours }} 小时可取消
                </li>
                <li v-if="formData.cancelFeePercentage > 0">
                  取消费用 {{ formData.cancelFeePercentage }}%
                </li>
              </ul>
            </div>

            <div class="preview-card">
              <h4>价格策略</h4>
              <ul>
                <li>默认房价: ¥{{ formData.defaultRoomPrice }}/晚</li>
                <li v-if="formData.enableDynamicPricing">启用动态定价</li>
                <li v-if="formData.enableDynamicPricing">
                  旺季: {{ formData.peakSeasonPriceMultiplier }}倍,
                  淡季: {{ formData.offSeasonPriceMultiplier }}倍,
                  周末: {{ formData.weekendPriceMultiplier }}倍
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="rules-actions">
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
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, CloseBold, Money, View } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { BusinessRules } from '@/stores/systemSettings'

// Props
interface Props {
  modelValue: BusinessRules
}

interface Emits {
  (e: 'update:modelValue', value: BusinessRules): void
  (e: 'save', value: BusinessRules): void
  (e: 'loading', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const formRef = ref<FormInstance>()
const saving = ref(false)
const originalData = ref<BusinessRules>({} as BusinessRules)

const formData = reactive<BusinessRules>({
  minBookingDays: 1,
  maxBookingDays: 30,
  advanceBookingLimitDays: 365,
  cancelBeforeHours: 24,
  cancelFeePercentage: 0,
  enableFreeCancel: true,
  freeCancelHours: 24,
  defaultRoomPrice: 299,
  enableDynamicPricing: false,
  peakSeasonPriceMultiplier: 1.5,
  offSeasonPriceMultiplier: 0.8,
  weekendPriceMultiplier: 1.2
})

// 表单验证规则
const rules: FormRules = {
  minBookingDays: [
    { required: true, message: '请输入最少预订天数', trigger: 'blur' },
    { type: 'number', min: 1, max: 365, message: '最少预订天数必须在1-365之间', trigger: 'blur' }
  ],
  maxBookingDays: [
    { required: true, message: '请输入最多预订天数', trigger: 'blur' },
    { type: 'number', min: 1, max: 365, message: '最多预订天数必须在1-365之间', trigger: 'blur' }
  ],
  cancelBeforeHours: [
    { required: true, message: '请输入取消时限', trigger: 'blur' },
    { type: 'number', min: 0, max: 720, message: '取消时限必须在0-720小时之间', trigger: 'blur' }
  ],
  cancelFeePercentage: [
    { required: true, message: '请输入取消费用比例', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: '取消费用比例必须在0-100之间', trigger: 'blur' }
  ],
  defaultRoomPrice: [
    { required: true, message: '请输入默认房价', trigger: 'blur' },
    { type: 'number', min: 0, message: '默认房价必须大于等于0', trigger: 'blur' }
  ]
}

// 监听父组件数据变化
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    Object.assign(formData, newValue)
    originalData.value = { ...newValue }
  }
}, { immediate: true, deep: true })

// 监听表单数据变化
watch(formData, (newValue) => {
  emit('update:modelValue', { ...newValue })
}, { deep: true })

// 生命周期
onMounted(() => {
  if (props.modelValue) {
    Object.assign(formData, props.modelValue)
    originalData.value = { ...props.modelValue }
  }
})

// 方法
const handleSave = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    // 验证业务逻辑
    if (formData.minBookingDays > formData.maxBookingDays) {
      ElMessage.error('最少预订天数不能大于最多预订天数')
      return
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
      '确定要重置所有业务规则设置吗？未保存的更改将丢失。',
      '确认重置',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    Object.assign(formData, originalData.value)
    ElMessage.success('已重置到上次保存的状态')
  } catch (error) {
    // 用户取消操作
  }
}
</script>

<style scoped>
.business-rules {
  padding: 24px;
}

.rules-content {
  max-width: 1200px;
  margin: 0 auto;
}

.rules-form {
  padding: 0;
}

.rules-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.rules-section:last-child {
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

.field-tip {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.fee-input, .price-input, .multiplier-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.fee-input .el-input-number,
.price-input .el-input-number,
.multiplier-input .el-input-number {
  flex: 1;
}

.fee-unit, .price-unit, .multiplier-desc {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.fee-desc {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.preview-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.preview-card h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.preview-card ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.preview-card li {
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

.preview-card li:last-child {
  margin-bottom: 0;
}

.rules-actions {
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

:deep(.el-switch) {
  height: 22px;
}

:deep(.el-switch__label) {
  font-size: 12px;
  color: #606266;
}
</style>