<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import {
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElSelect,
  ElOption,
  ElSwitch,
  ElButton,
  ElRow,
  ElCol,
  ElIcon
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import RoomTypeIcon from './RoomTypeIcon.vue'
import type { RoomType, CreateRoomTypeRequest, UpdateRoomTypeRequest } from '../../../types/roomType'

interface Props {
  roomType?: RoomType | null
  loading?: boolean
  hotelId?: number
}

interface Emits {
  (e: 'submit', data: CreateRoomTypeRequest | UpdateRoomTypeRequest): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const form = ref<CreateRoomTypeRequest>({
  name: '',
  capacity: 1,
  basePrice: 0,
  facilities: [],
  description: '',
  iconUrl: ''
})

const facilityInput = ref('')
const status = ref<'ACTIVE' | 'INACTIVE'>('ACTIVE')

const isEdit = computed(() => !!props.roomType?.id)

const rules = computed<FormRules>(() => ({
  name: [
    { required: true, message: '请输入房间类型名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  capacity: [
    { required: true, message: '请输入房间容量', trigger: 'blur' },
    { type: 'number', min: 1, max: 20, message: '容量必须在 1-20 人之间', trigger: 'blur' }
  ],
  basePrice: [
    { required: true, message: '请输入基础价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格不能小于 0', trigger: 'blur' }
  ]
}))

// 监听 roomType 变化，填充表单
watch(() => props.roomType, (newRoomType) => {
  if (newRoomType) {
    nextTick(() => {
      form.value = {
        name: newRoomType.name,
        capacity: newRoomType.capacity,
        basePrice: newRoomType.basePrice,
        facilities: [...(newRoomType.facilities || [])],
        description: newRoomType.description || '',
        iconUrl: newRoomType.iconUrl || ''
      }
      status.value = newRoomType.status
    })
  } else {
    resetForm()
  }
}, { immediate: true })

function resetForm() {
  form.value = {
    name: '',
    capacity: 1,
    basePrice: 0,
    facilities: [],
    description: '',
    iconUrl: ''
  }
  status.value = 'ACTIVE'
  facilityInput.value = ''
  formRef.value?.clearValidate()
}

function handleIconChange(url: string) {
  form.value.iconUrl = url
}

function addFacility() {
  const facility = facilityInput.value.trim()
  if (facility && !form.value.facilities?.includes(facility)) {
    form.value.facilities?.push(facility)
    facilityInput.value = ''
  }
}

function removeFacility(index: number) {
  form.value.facilities?.splice(index, 1)
}

function handleFacilityInputKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    event.preventDefault()
    addFacility()
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    const submitData = {
      ...form.value,
      ...(isEdit.value ? { status: status.value } : {})
    }

    emit('submit', submitData)
  } catch (error) {
    // 表单验证失败
  }
}

function handleCancel() {
  emit('cancel')
}
</script>

<template>
  <ElForm
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="120px"
    label-position="right"
  >
    <!-- 基本信息 -->
    <ElRow :gutter="20">
      <ElCol :xs="24" :sm="12">
        <ElFormItem label="房间类型名称" prop="name">
          <ElInput
            v-model="form.name"
            placeholder="请输入房间类型名称，如：标准间、豪华套房"
            maxlength="50"
            show-word-limit
          />
        </ElFormItem>
      </ElCol>
      <ElCol :xs="24" :sm="12">
        <ElFormItem label="房间容量" prop="capacity">
          <ElInputNumber
            v-model="form.capacity"
            :min="1"
            :max="20"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
          <div class="form-help">可入住人数</div>
        </ElFormItem>
      </ElCol>
    </ElRow>

    <ElRow :gutter="20">
      <ElCol :xs="24" :sm="12">
        <ElFormItem label="基础价格" prop="basePrice">
          <ElInputNumber
            v-model="form.basePrice"
            :min="0"
            :precision="2"
            :step="10"
            controls-position="right"
            style="width: 100%"
          />
          <div class="form-help">元/晚</div>
        </ElFormItem>
      </ElCol>
      <ElCol v-if="isEdit" :xs="24" :sm="12">
        <ElFormItem label="状态">
          <ElSelect v-model="status" style="width: 100%">
            <ElOption label="营业中" value="ACTIVE" />
            <ElOption label="已停用" value="INACTIVE" />
          </ElSelect>
        </ElFormItem>
      </ElCol>
    </ElRow>

    <!-- 房间类型图标 -->
    <ElFormItem label="类型图标">
      <RoomTypeIcon
        :icon-url="form.iconUrl"
        @change="handleIconChange"
      />
    </ElFormItem>

    <!-- 设施 -->
    <ElFormItem label="房间设施">
      <div class="facilities-input">
        <ElInput
          v-model="facilityInput"
          placeholder="输入设施名称，按回车添加"
          @keydown="handleFacilityInputKeydown"
          style="margin-bottom: 12px"
        >
          <template #append>
            <ElButton @click="addFacility">
              <i class="el-icon-plus"></i>
              添加
            </ElButton>
          </template>
        </ElInput>

        <div v-if="form.facilities && form.facilities.length > 0" class="facilities-list">
          <ElTag
            v-for="(facility, index) in form.facilities"
            :key="index"
            closable
            @close="removeFacility(index)"
            style="margin-right: 8px; margin-bottom: 8px"
          >
            {{ facility }}
          </ElTag>
        </div>
      </div>
    </ElFormItem>

    <!-- 描述 -->
    <ElFormItem label="房间描述">
      <ElInput
        v-model="form.description"
        type="textarea"
        :rows="4"
        placeholder="请输入房间类型描述信息"
        maxlength="500"
        show-word-limit
      />
    </ElFormItem>

    <!-- 表单操作 -->
    <ElFormItem>
      <div class="form-actions">
        <ElButton @click="handleCancel">
          取消
        </ElButton>
        <ElButton
          type="primary"
          :loading="loading"
          @click="handleSubmit"
        >
          {{ isEdit ? '更新' : '创建' }}
        </ElButton>
      </div>
    </ElFormItem>
  </ElForm>
</template>

<style scoped>
.form-help {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.facilities-input {
  width: 100%;
}

.facilities-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .form-actions {
    justify-content: center;
  }
}
</style>