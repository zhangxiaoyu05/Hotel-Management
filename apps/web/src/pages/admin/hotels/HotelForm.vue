<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useHotelStore } from '../../../stores/hotelStore'
import { hotelService } from '../../../services/hotelService'
import ImageUpload from '../../../components/hotel/ImageUpload.vue'
import type { Hotel, CreateHotelRequest, UpdateHotelRequest } from '../../../types/hotel'

const router = useRouter()
const route = useRoute()
const hotelStore = useHotelStore()

const isEdit = computed(() => !!route.params.id)
const hotelId = computed(() => Number(route.params.id))

const formRef = ref()
const loading = ref(false)
const submitting = ref(false)

const hotelForm = reactive({
  name: '',
  address: '',
  phone: '',
  description: '',
  facilities: [] as string[],
  images: [] as string[],
  status: 'ACTIVE' as 'ACTIVE' | 'INACTIVE'
})

const rules = {
  name: [
    { required: true, message: '请输入酒店名称', trigger: 'blur' },
    { min: 2, max: 100, message: '酒店名称长度为2-100个字符', trigger: 'blur' }
  ],
  address: [
    { required: true, message: '请输入酒店地址', trigger: 'blur' },
    { min: 5, max: 255, message: '酒店地址长度为5-255个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$|^\\d{7,8}$/, message: '请输入有效的电话号码', trigger: 'blur' }
  ],
  description: [
    { max: 1000, message: '酒店简介不能超过1000个字符', trigger: 'blur' }
  ]
}

const newFacility = ref('')

onMounted(() => {
  if (isEdit.value) {
    loadHotel()
  }
})

async function loadHotel() {
  if (!hotelId.value) return

  loading.value = true
  try {
    const hotel = await hotelService.getHotelById(hotelId.value)
    Object.assign(hotelForm, {
      name: hotel.name,
      address: hotel.address,
      phone: hotel.phone,
      description: hotel.description,
      facilities: [...(hotel.facilities || [])],
      images: [...(hotel.images || [])],
      status: hotel.status
    })
  } catch (error: any) {
    ElMessage.error(error.message || '加载酒店信息失败')
    router.push('/admin/hotels')
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true

  try {
    const hotelData = {
      name: hotelForm.name.trim(),
      address: hotelForm.address.trim(),
      phone: hotelForm.phone.trim() || undefined,
      description: hotelForm.description.trim() || undefined,
      facilities: hotelForm.facilities,
      images: hotelForm.images
    }

    if (isEdit.value) {
      await hotelService.updateHotel(hotelId.value, {
        ...hotelData,
        status: hotelForm.status
      })
      ElMessage.success('酒店更新成功')
    } else {
      await hotelService.createHotel(hotelData)
      ElMessage.success('酒店创建成功')
    }

    router.push('/admin/hotels')
  } catch (error: any) {
    ElMessage.error(error.message || `${isEdit.value ? '更新' : '创建'}酒店失败`)
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push('/admin/hotels')
}

function handleImagesChange(images: string[]) {
  hotelForm.images = images
}

function addFacility() {
  const facility = newFacility.value.trim()
  if (facility && !hotelForm.facilities.includes(facility)) {
    hotelForm.facilities.push(facility)
    newFacility.value = ''
  }
}

function removeFacility(index: number) {
  hotelForm.facilities.splice(index, 1)
}

function handleKeyPress(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    event.preventDefault()
    addFacility()
  }
}

const pageTitle = computed(() => isEdit.value ? '编辑酒店' : '添加酒店')
</script>

<template>
  <div class="hotel-form-page">
    <div class="page-header">
      <h1>{{ pageTitle }}</h1>
      <div class="header-actions">
        <el-button @click="handleCancel">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </div>
    </div>

    <div class="form-container">
      <el-form
        ref="formRef"
        :model="hotelForm"
        :rules="rules"
        label-width="120px"
        :disabled="loading"
      >
        <!-- 基本信息 -->
        <el-card class="form-section">
          <template #header>
            <h3>基本信息</h3>
          </template>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="酒店名称" prop="name">
                <el-input
                  v-model="hotelForm.name"
                  placeholder="请输入酒店名称"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="phone">
                <el-input
                  v-model="hotelForm.phone"
                  placeholder="请输入联系电话"
                  maxlength="20"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="酒店地址" prop="address">
            <el-input
              v-model="hotelForm.address"
              type="textarea"
              :rows="2"
              placeholder="请输入酒店详细地址"
              maxlength="255"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="酒店简介" prop="description">
            <el-input
              v-model="hotelForm.description"
              type="textarea"
              :rows="4"
              placeholder="请输入酒店简介"
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>

          <el-form-item v-if="isEdit" label="营业状态">
            <el-radio-group v-model="hotelForm.status">
              <el-radio label="ACTIVE">营业中</el-radio>
              <el-radio label="INACTIVE">已停业</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-card>

        <!-- 设施服务 -->
        <el-card class="form-section">
          <template #header>
            <h3>设施服务</h3>
          </template>

          <el-form-item label="添加设施">
            <div class="facility-input">
              <el-input
                v-model="newFacility"
                placeholder="输入设施名称，按回车添加"
                @keypress="handleKeyPress"
                style="flex: 1; margin-right: 10px;"
              />
              <el-button type="primary" @click="addFacility">添加</el-button>
            </div>
          </el-form-item>

          <el-form-item>
            <div class="facility-list">
              <el-tag
                v-for="(facility, index) in hotelForm.facilities"
                :key="index"
                closable
                @close="removeFacility(index)"
                class="facility-tag"
              >
                {{ facility }}
              </el-tag>
              <span v-if="hotelForm.facilities.length === 0" class="no-facilities">
                暂无设施，请添加酒店设施
              </span>
            </div>
          </el-form-item>
        </el-card>

        <!-- 酒店图片 -->
        <el-card class="form-section">
          <template #header>
            <h3>酒店图片</h3>
          </template>

          <el-form-item>
            <ImageUpload
              v-model:images="hotelForm.images"
              :max-count="10"
              accept="image/*"
              :file-size="5"
              placeholder="上传酒店图片，最多10张，每张不超过5MB"
            />
          </el-form-item>
        </el-card>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.hotel-form-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.form-container {
  background: #fff;
}

.form-section {
  margin-bottom: 20px;
}

.form-section h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.facility-input {
  display: flex;
  align-items: center;
}

.facility-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.facility-tag {
  margin: 0;
}

.no-facilities {
  color: #909399;
  font-size: 14px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .facility-input {
    flex-direction: column;
    gap: 10px;
  }

  .facility-input .el-input {
    margin-right: 0;
  }
}
</style>