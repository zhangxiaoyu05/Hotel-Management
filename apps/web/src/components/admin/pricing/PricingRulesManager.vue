<template>
  <div class="pricing-rules-manager">
    <!-- 页面工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button
          type="primary"
          @click="showCreateDialog = true"
          :icon="Plus"
        >
          新建价格规则
        </el-button>

        <el-button
          @click="showBatchActions = !showBatchActions"
          :disabled="selectedRules.length === 0"
        >
          批量操作 ({{ selectedRules.length }})
        </el-button>
      </div>

      <div class="toolbar-right">
        <el-input
          v-model="searchQuery"
          placeholder="搜索规则名称..."
          prefix-icon="Search"
          clearable
          style="width: 250px"
          @input="handleSearch"
        />

        <el-select
          v-model="filterType"
          placeholder="规则类型"
          clearable
          style="width: 150px"
          @change="handleFilter"
        >
          <el-option label="周末价格" value="WEEKEND" />
          <el-option label="节假日价格" value="HOLIDAY" />
          <el-option label="季节性价格" value="SEASONAL" />
          <el-option label="自定义价格" value="CUSTOM" />
        </el-select>

        <el-select
          v-model="filterStatus"
          placeholder="状态"
          clearable
          style="width: 120px"
          @change="handleFilter"
        >
          <el-option label="激活" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <div v-if="showBatchActions && selectedRules.length > 0" class="batch-actions">
      <el-button @click="batchActivate" size="small">批量激活</el-button>
      <el-button @click="batchDeactivate" size="small">批量停用</el-button>
      <el-button @click="batchDelete" size="small" type="danger">批量删除</el-button>
      <el-button @click="clearSelection" size="small">取消选择</el-button>
    </div>

    <!-- 价格规则列表 -->
    <div class="rules-list">
      <el-table
        :data="pricingRules"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        row-key="id"
      >
        <el-table-column type="selection" width="55" />

        <el-table-column prop="name" label="规则名称" min-width="150">
          <template #default="{ row }">
            <div class="rule-name">
              <span class="name">{{ row.name }}</span>
              <el-tag v-if="!row.isActive" type="info" size="small">已停用</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="ruleType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getRuleTypeTag(row.ruleType)">
              {{ getRuleTypeName(row.ruleType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="调整方式" width="120">
          <template #default="{ row }">
            <span v-if="row.adjustmentType === 'PERCENTAGE'">
              {{ row.adjustmentValue > 0 ? '+' : '' }}{{ row.adjustmentValue }}%
            </span>
            <span v-else>
              {{ row.adjustmentValue > 0 ? '+' : '' }}¥{{ row.adjustmentValue }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="适用范围" width="150">
          <template #default="{ row }">
            <span v-if="row.roomTypeName">{{ row.roomTypeName }}</span>
            <span v-else class="text-gray-500">所有房间类型</span>
          </template>
        </el-table-column>

        <el-table-column label="时间条件" width="200">
          <template #default="{ row }">
            <div class="time-conditions">
              <div v-if="row.startDate || row.endDate" class="date-range">
                {{ formatDate(row.startDate) }} - {{ formatDate(row.endDate) }}
              </div>
              <div v-if="row.daysOfWeek && row.daysOfWeek.length > 0" class="days-of-week">
                {{ formatDaysOfWeek(row.daysOfWeek) }}
              </div>
              <div v-if="!row.startDate && !row.endDate && (!row.daysOfWeek || row.daysOfWeek.length === 0)"
                   class="text-gray-500">
                无时间限制
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="priority" label="优先级" width="80" sortable>
          <template #default="{ row }">
            <el-tag :type="getPriorityTag(row.priority)">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              @change="toggleRuleStatus(row)"
              :loading="row.updating"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="editRule(row)"
              :icon="Edit"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="deleteRule(row)"
              :icon="Delete"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadPricingRules"
          @current-change="loadPricingRules"
        />
      </div>
    </div>

    <!-- 创建/编辑规则对话框 -->
    <PricingRuleDialog
      v-model="showCreateDialog"
      :rule="editingRule"
      :room-types="roomTypes"
      @success="handleDialogSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { usePricingStore } from '../../../stores/pricingStore'
import { useHotelStore } from '../../../stores/hotelStore'
import { useRoomTypeStore } from '../../../stores/roomTypeStore'
import type { PricingRule } from '../../../types/pricing'
import PricingRuleDialog from './PricingRuleDialog.vue'

const pricingStore = usePricingStore()
const hotelStore = useHotelStore()
const roomTypeStore = useRoomTypeStore()

// 响应式数据
const loading = ref(false)
const searchQuery = ref('')
const filterType = ref('')
const filterStatus = ref<boolean | undefined>(undefined)
const currentPage = ref(1)
const pageSize = ref(20)
const selectedRules = ref<PricingRule[]>([])
const showBatchActions = ref(false)
const showCreateDialog = ref(false)
const editingRule = ref<PricingRule | null>(null)

// 计算属性
const pricingRules = computed(() => pricingStore.rules)
const total = computed(() => pricingStore.total)
const roomTypes = computed(() => roomTypeStore.roomTypes)

// 生命周期
onMounted(async () => {
  await Promise.all([
    loadRoomTypes(),
    loadPricingRules()
  ])
})

// 方法
async function loadRoomTypes() {
  try {
    await roomTypeStore.fetchRoomTypes({ page: 0, size: 1000 })
  } catch (error) {
    console.error('Failed to load room types:', error)
  }
}

async function loadPricingRules() {
  loading.value = true
  try {
    const hotelId = hotelStore.currentHotel?.id
    if (!hotelId) {
      ElMessage.warning('请先选择酒店')
      return
    }

    await pricingStore.fetchPricingRules({
      hotelId,
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: searchQuery.value,
      ruleType: filterType.value,
      activeOnly: filterStatus.value
    })
  } catch (error) {
    ElMessage.error('加载价格规则失败')
    console.error('Failed to load pricing rules:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadPricingRules()
}

function handleFilter() {
  currentPage.value = 1
  loadPricingRules()
}

function handleSelectionChange(selection: PricingRule[]) {
  selectedRules.value = selection
}

function clearSelection() {
  selectedRules.value = []
  showBatchActions.value = false
}

async function toggleRuleStatus(rule: PricingRule) {
  rule.updating = true
  try {
    await pricingStore.toggleRuleStatus(rule.id!, !rule.isActive)
    ElMessage.success(rule.isActive ? '规则已激活' : '规则已停用')
  } catch (error) {
    rule.isActive = !rule.isActive // 回滚状态
    ElMessage.error('操作失败')
  } finally {
    rule.updating = false
  }
}

function editRule(rule: PricingRule) {
  editingRule.value = { ...rule }
  showCreateDialog.value = true
}

async function deleteRule(rule: PricingRule) {
  try {
    await ElMessageBox.confirm(
      `确定要删除价格规则"${rule.name}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await pricingStore.deletePricingRule(rule.id!)
    ElMessage.success('删除成功')
    loadPricingRules()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function batchActivate() {
  try {
    const ids = selectedRules.value.map(rule => rule.id!)
    await pricingStore.batchToggleRules(ids, true)
    ElMessage.success('批量激活成功')
    clearSelection()
    loadPricingRules()
  } catch (error) {
    ElMessage.error('批量激活失败')
  }
}

async function batchDeactivate() {
  try {
    const ids = selectedRules.value.map(rule => rule.id!)
    await pricingStore.batchToggleRules(ids, false)
    ElMessage.success('批量停用成功')
    clearSelection()
    loadPricingRules()
  } catch (error) {
    ElMessage.error('批量停用失败')
  }
}

async function batchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRules.value.length} 个价格规则吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const ids = selectedRules.value.map(rule => rule.id!)
    await pricingStore.batchDeleteRules(ids)
    ElMessage.success('批量删除成功')
    clearSelection()
    loadPricingRules()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

function handleDialogSuccess() {
  showCreateDialog.value = false
  editingRule.value = null
  loadPricingRules()
}

// 辅助函数
function getRuleTypeTag(type: string) {
  const tagMap: Record<string, string> = {
    'WEEKEND': 'warning',
    'HOLIDAY': 'danger',
    'SEASONAL': 'success',
    'CUSTOM': 'info'
  }
  return tagMap[type] || 'info'
}

function getRuleTypeName(type: string) {
  const nameMap: Record<string, string> = {
    'WEEKEND': '周末价格',
    'HOLIDAY': '节假日价格',
    'SEASONAL': '季节性价格',
    'CUSTOM': '自定义价格'
  }
  return nameMap[type] || type
}

function getPriorityTag(priority: number) {
  if (priority >= 200) return 'danger'
  if (priority >= 100) return 'warning'
  return 'info'
}

function formatDate(date: string | null | undefined) {
  if (!date) return '不限'
  return new Date(date).toLocaleDateString('zh-CN')
}

function formatDaysOfWeek(days: number[]) {
  const dayNames = ['一', '二', '三', '四', '五', '六', '日']
  return days.map(day => dayNames[day - 1]).join('、')
}
</script>

<style scoped>
.pricing-rules-manager {
  height: 100%;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
}

.batch-actions {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  margin-bottom: 16px;
}

.rules-list {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.rule-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rule-name .name {
  font-weight: 500;
}

.time-conditions {
  font-size: 12px;
  line-height: 1.4;
}

.date-range,
.days-of-week {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 16px;
  border-top: 1px solid #e5e7eb;
}

.text-gray-500 {
  color: #6b7280;
}
</style>