<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑价格规则' : '新建价格规则'"
    width="600px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="规则名称" prop="name">
        <el-input
          v-model="formData.name"
          placeholder="请输入规则名称"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="规则类型" prop="ruleType">
        <el-select
          v-model="formData.ruleType"
          placeholder="请选择规则类型"
          style="width: 100%"
          @change="handleRuleTypeChange"
        >
          <el-option label="周末价格" value="WEEKEND" />
          <el-option label="节假日价格" value="HOLIDAY" />
          <el-option label="季节性价格" value="SEASONAL" />
          <el-option label="自定义价格" value="CUSTOM" />
        </el-select>
      </el-form-item>

      <el-form-item label="适用房间类型" prop="roomTypeId">
        <el-select
          v-model="formData.roomTypeId"
          placeholder="请选择房间类型，不选择表示适用于所有房间类型"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="roomType in roomTypes"
            :key="roomType.id"
            :label="roomType.name"
            :value="roomType.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="调整方式" prop="adjustmentType">
        <el-radio-group v-model="formData.adjustmentType">
          <el-radio value="PERCENTAGE">百分比调整</el-radio>
          <el-radio value="FIXED_AMOUNT">固定金额调整</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="调整值" prop="adjustmentValue">
        <div class="adjustment-input">
          <el-input-number
            v-model="formData.adjustmentValue"
            :precision="formData.adjustmentType === 'PERCENTAGE' ? 2 : 0"
            :min="0.01"
            :max="formData.adjustmentType === 'PERCENTAGE' ? 1000 : 99999"
            style="width: 200px"
          />
          <span class="adjustment-unit">
            {{ formData.adjustmentType === 'PERCENTAGE' ? '%' : '元' }}
          </span>
          <span class="adjustment-preview">
            {{ formData.adjustmentType === 'PERCENTAGE' ? '上浮' : '增加' }}
            {{ formData.adjustmentValue }}{{ formData.adjustmentType === 'PERCENTAGE' ? '%' : '元' }}
          </span>
        </div>
      </el-form-item>

      <el-form-item label="时间条件">
        <div class="time-conditions">
          <div class="condition-row">
            <span class="condition-label">适用日期范围：</span>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
              @change="handleDateRangeChange"
            />
          </div>

          <div class="condition-row">
            <span class="condition-label">适用星期：</span>
            <el-checkbox-group v-model="selectedDays">
              <el-checkbox :label="1">周一</el-checkbox>
              <el-checkbox :label="2">周二</el-checkbox>
              <el-checkbox :label="3">周三</el-checkbox>
              <el-checkbox :label="4">周四</el-checkbox>
              <el-checkbox :label="5">周五</el-checkbox>
              <el-checkbox :label="6">周六</el-checkbox>
              <el-checkbox :label="7">周日</el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="优先级" prop="priority">
        <el-input-number
          v-model="formData.priority"
          :min="0"
          :max="999"
          placeholder="数值越大优先级越高"
          style="width: 200px"
        />
        <span class="priority-hint">
          提示：特殊价格 > 动态规则(按优先级) > 基础价格
        </span>
      </el-form-item>

      <el-form-item label="状态">
        <el-switch
          v-model="formData.isActive"
          active-text="激活"
          inactive-text="停用"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :loading="submitting"
        >
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { usePricingStore } from '../../../stores/pricingStore'
import { useHotelStore } from '../../../stores/hotelStore'
import type { PricingRule, CreatePricingRuleRequest } from '../../../types/pricing'

interface Props {
  modelValue: boolean
  rule?: PricingRule | null
  roomTypes: any[]
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const pricingStore = usePricingStore()
const hotelStore = useHotelStore()

// 响应式数据
const formRef = ref<FormInstance>()
const submitting = ref(false)
const dateRange = ref<[string, string] | null>(null)
const selectedDays = ref<number[]>([])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isEdit = computed(() => !!props.rule)

// 表单数据
const formData = ref<CreatePricingRuleRequest>({
  hotelId: hotelStore.currentHotel?.id || 0,
  roomTypeId: undefined,
  name: '',
  ruleType: 'WEEKEND',
  adjustmentType: 'PERCENTAGE',
  adjustmentValue: 0,
  startDate: undefined,
  endDate: undefined,
  daysOfWeek: [],
  isActive: true,
  priority: 0
})

// 表单验证规则
const formRules: FormRules = {
  name: [
    { required: true, message: '请输入规则名称', trigger: 'blur' },
    { max: 100, message: '规则名称不能超过100个字符', trigger: 'blur' }
  ],
  ruleType: [
    { required: true, message: '请选择规则类型', trigger: 'change' }
  ],
  adjustmentType: [
    { required: true, message: '请选择调整方式', trigger: 'change' }
  ],
  adjustmentValue: [
    { required: true, message: '请输入调整值', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '调整值必须大于0', trigger: 'blur' }
  ]
}

// 监听器
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    initFormData()
  }
})

watch(() => props.rule, (newRule) => {
  if (newRule && props.modelValue) {
    initFormData()
  }
})

// 方法
function initFormData() {
  if (props.rule) {
    // 编辑模式
    formData.value = {
      hotelId: props.rule.hotelId,
      roomTypeId: props.rule.roomTypeId,
      name: props.rule.name,
      ruleType: props.rule.ruleType as any,
      adjustmentType: props.rule.adjustmentType as any,
      adjustmentValue: Number(props.rule.adjustmentValue),
      startDate: props.rule.startDate,
      endDate: props.rule.endDate,
      daysOfWeek: props.rule.daysOfWeek || [],
      isActive: props.rule.isActive,
      priority: props.rule.priority || 0
    }

    // 设置日期范围
    if (props.rule.startDate && props.rule.endDate) {
      dateRange.value = [props.rule.startDate, props.rule.endDate]
    } else {
      dateRange.value = null
    }

    // 设置选中的星期
    selectedDays.value = props.rule.daysOfWeek || []
  } else {
    // 新建模式
    formData.value = {
      hotelId: hotelStore.currentHotel?.id || 0,
      roomTypeId: undefined,
      name: '',
      ruleType: 'WEEKEND',
      adjustmentType: 'PERCENTAGE',
      adjustmentValue: 0,
      startDate: undefined,
      endDate: undefined,
      daysOfWeek: [],
      isActive: true,
      priority: 0
    }

    dateRange.value = null
    selectedDays.value = []
  }
}

function handleRuleTypeChange() {
  // 根据规则类型预设默认值
  switch (formData.value.ruleType) {
    case 'WEEKEND':
      formData.value.adjustmentValue = 20
      selectedDays.value = [6, 7] // 周六、周日
      break
    case 'HOLIDAY':
      formData.value.adjustmentValue = 30
      formData.value.priority = 200
      break
    case 'SEASONAL':
      formData.value.adjustmentValue = 15
      formData.value.priority = 50
      break
    case 'CUSTOM':
      formData.value.adjustmentValue = 10
      formData.value.priority = 100
      break
  }
}

function handleDateRangeChange(dates: [string, string] | null) {
  if (dates) {
    formData.value.startDate = dates[0]
    formData.value.endDate = dates[1]
  } else {
    formData.value.startDate = undefined
    formData.value.endDate = undefined
  }
}

watch(selectedDays, (days) => {
  formData.value.daysOfWeek = days
}, { deep: true })

function disabledDate(date: Date) {
  // 不能选择过去的日期
  return date < new Date(new Date().setHours(0, 0, 0, 0))
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    submitting.value = true

    if (isEdit.value) {
      await pricingStore.updatePricingRule(props.rule!.id!, formData.value)
      ElMessage.success('价格规则更新成功')
    } else {
      await pricingStore.createPricingRule(formData.value)
      ElMessage.success('价格规则创建成功')
    }

    emit('success')
  } catch (error) {
    console.error('Failed to save pricing rule:', error)
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

function handleClose() {
  visible.value = false
}
</script>

<style scoped>
.adjustment-input {
  display: flex;
  align-items: center;
  gap: 12px;
}

.adjustment-unit {
  color: #6b7280;
  font-weight: 500;
}

.adjustment-preview {
  color: #3b82f6;
  font-size: 14px;
}

.time-conditions {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 16px;
  background: #f9fafb;
}

.condition-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.condition-row:last-child {
  margin-bottom: 0;
}

.condition-label {
  min-width: 100px;
  color: #374151;
  font-weight: 500;
}

.priority-hint {
  margin-left: 12px;
  color: #6b7280;
  font-size: 12px;
}

.dialog-footer {
  text-align: right;
}
</style>