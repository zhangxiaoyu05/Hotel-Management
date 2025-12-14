<template>
  <div class="report-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>数据统计报表</h1>
      <p class="page-description">查看各种业务报表，分析业务趋势和制定策略</p>
    </div>

    <!-- 报表概览卡片 -->
    <div v-if="reportOverview" class="overview-cards">
      <div class="overview-grid">
        <div class="overview-card">
          <div class="card-icon orders">
            <el-icon><Document /></el-icon>
          </div>
          <div class="card-content">
            <h3>{{ reportOverview.todayOrders }}</h3>
            <p>今日订单</p>
            <span class="update-time">更新时间: {{ formatDate(reportOverview.lastUpdated) }}</span>
          </div>
        </div>

        <div class="overview-card">
          <div class="card-icon revenue">
            <el-icon><Money /></el-icon>
          </div>
          <div class="card-content">
            <h3>¥{{ formatNumber(reportOverview.todayRevenue) }}</h3>
            <p>今日收入</p>
            <span class="update-time">本月总收入: ¥{{ formatNumber(reportOverview.monthlyRevenue) }}</span>
          </div>
        </div>

        <div class="overview-card">
          <div class="card-icon occupancy">
            <el-icon><House /></el-icon>
          </div>
          <div class="card-content">
            <h3>{{ formatPercent(reportOverview.currentOccupancyRate) }}</h3>
            <p>当前入住率</p>
            <span class="update-time">可用房间: {{ reportOverview.availableRooms }}</span>
          </div>
        </div>

        <div class="overview-card">
          <div class="card-icon users">
            <el-icon><User /></el-icon>
          </div>
          <div class="card-content">
            <h3>{{ reportOverview.activeUsers }}</h3>
            <p>活跃用户</p>
            <span class="update-time">本月新增: {{ reportOverview.monthlyNewUsers }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>筛选条件</span>
            <div class="header-actions">
              <el-button @click="refreshAllData" :loading="loading">
                <el-icon><Refresh /></el-icon>
                刷新数据
              </el-button>
              <el-button type="primary" @click="exportReport" :loading="exporting">
                <el-icon><Download /></el-icon>
                导出报表
              </el-button>
            </div>
          </div>
        </template>

        <div class="filter-form">
          <el-form :model="filters" :inline="true" label-width="80px">
            <el-form-item label="开始日期">
              <el-date-picker
                v-model="filters.startDate"
                type="date"
                placeholder="选择开始日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledStartDate"
              />
            </el-form-item>

            <el-form-item label="结束日期">
              <el-date-picker
                v-model="filters.endDate"
                type="date"
                placeholder="选择结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledEndDate"
              />
            </el-form-item>

            <el-form-item label="房型">
              <el-select
                v-model="filters.roomTypeId"
                placeholder="选择房型"
                clearable
              >
                <el-option
                  v-for="roomType in roomTypes"
                  :key="roomType.id"
                  :label="roomType.name"
                  :value="roomType.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="订单状态">
              <el-select
                v-model="filters.orderStatus"
                placeholder="选择订单状态"
                clearable
              >
                <el-option label="待确认" value="PENDING" />
                <el-option label="已确认" value="CONFIRMED" />
                <el-option label="已完成" value="COMPLETED" />
                <el-option label="已取消" value="CANCELLED" />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="applyFilters">应用筛选</el-button>
              <el-button @click="resetFilters">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>
    </div>

    <!-- 报表选项卡 -->
    <div class="report-tabs">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="订单报表" name="orders">
          <OrderReport :filters="filters" />
        </el-tab-pane>

        <el-tab-pane label="收入报表" name="revenue">
          <RevenueReport :filters="filters" />
        </el-tab-pane>

        <el-tab-pane label="用户报表" name="users">
          <UserReport :filters="filters" />
        </el-tab-pane>

        <el-tab-pane label="房间报表" name="rooms">
          <RoomReport :filters="filters" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 导出对话框 -->
    <el-dialog v-model="exportDialogVisible" title="导出报表" width="500px">
      <el-form :model="exportForm" label-width="100px">
        <el-form-item label="报表类型" required>
          <el-select v-model="exportForm.reportType" placeholder="选择报表类型">
            <el-option label="订单报表" value="ORDER" />
            <el-option label="收入报表" value="REVENUE" />
            <el-option label="用户报表" value="USER" />
            <el-option label="房间报表" value="ROOM" />
          </el-select>
        </el-form-item>

        <el-form-item label="导出格式" required>
          <el-radio-group v-model="exportForm.exportFormat">
            <el-radio label="EXCEL">Excel</el-radio>
            <el-radio label="PDF">PDF</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="包含图表">
          <el-switch v-model="exportForm.includeCharts" />
        </el-form-item>

        <el-form-item label="详细数据">
          <el-switch v-model="exportForm.includeDetailedData" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="exportDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmExport" :loading="exporting">
            导出
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Money, House, User, Refresh, Download } from '@element-plus/icons-vue'
import type { ReportFilters, ReportExportRequest, ReportOverviewDTO } from '@/types/report'
import { useReportStore } from '@/stores/report'
import OrderReport from '@/components/admin/reports/OrderReport.vue'
import RevenueReport from '@/components/admin/reports/RevenueReport.vue'
import UserReport from '@/components/admin/reports/UserReport.vue'
import RoomReport from '@/components/admin/reports/RoomReport.vue'

const reportStore = useReportStore()

// 响应式数据
const loading = ref(false)
const exporting = ref(false)
const activeTab = ref('orders')
const exportDialogVisible = ref(false)
const reportOverview = ref<ReportOverviewDTO | null>(null)

// 筛选条件
const filters = reactive<ReportFilters>({
  startDate: new Date(new Date().setDate(new Date().getDate() - 30))
    .toISOString().split('T')[0],
  endDate: new Date().toISOString().split('T')[0],
  roomTypeId: undefined,
  orderStatus: undefined
})

// 导出表单
const exportForm = reactive<ReportExportRequest>({
  reportType: 'ORDER',
  exportFormat: 'EXCEL',
  startDate: filters.startDate,
  endDate: filters.endDate,
  roomTypeId: filters.roomTypeId,
  orderStatus: filters.orderStatus,
  includeCharts: false,
  includeDetailedData: true
})

// 房型数据（模拟数据，实际应该从API获取）
const roomTypes = ref([
  { id: 1, name: '标准间' },
  { id: 2, name: '豪华间' },
  { id: 3, name: '套房' },
  { id: 4, name: '总统套房' }
])

// 生命周期
onMounted(async () => {
  await loadReportOverview()
})

// 方法
const loadReportOverview = async () => {
  try {
    reportOverview.value = await reportStore.fetchReportOverview()
  } catch (error) {
    ElMessage.error('加载报表概览失败')
  }
}

const refreshAllData = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadReportOverview(),
      reportStore.refreshReportCache()
    ])
    ElMessage.success('数据刷新成功')
  } catch (error) {
    ElMessage.error('刷新数据失败')
  } finally {
    loading.value = false
  }
}

const applyFilters = async () => {
  // 根据当前激活的标签页刷新对应的数据
  switch (activeTab.value) {
    case 'orders':
      await reportStore.fetchOrderReport(filters)
      break
    case 'revenue':
      await reportStore.fetchRevenueReport(filters)
      break
    case 'users':
      await reportStore.fetchUserReport(filters)
      break
    case 'rooms':
      await reportStore.fetchRoomReport(filters)
      break
  }
  ElMessage.success('筛选条件已应用')
}

const resetFilters = () => {
  filters.startDate = new Date(new Date().setDate(new Date().getDate() - 30))
    .toISOString().split('T')[0]
  filters.endDate = new Date().toISOString().split('T')[0]
  filters.roomTypeId = undefined
  filters.orderStatus = undefined
}

const handleTabChange = (tabName: string) => {
  activeTab.value = tabName
  applyFilters()
}

const exportReport = () => {
  exportForm.startDate = filters.startDate
  exportForm.endDate = filters.endDate
  exportForm.roomTypeId = filters.roomTypeId
  exportForm.orderStatus = filters.orderStatus
  exportDialogVisible.value = true
}

const confirmExport = async () => {
  exporting.value = true
  try {
    const fileUrl = await reportStore.exportReport(exportForm)

    // 创建下载链接
    const link = document.createElement('a')
    link.href = fileUrl
    link.download = `report_${exportForm.reportType}_${new Date().toISOString().split('T')[0]}.${exportForm.exportFormat.toLowerCase()}`
    link.click()

    ElMessage.success('报表导出成功')
    exportDialogVisible.value = false
  } catch (error) {
    ElMessage.error('导出报表失败')
  } finally {
    exporting.value = false
  }
}

const disabledStartDate = (time: Date) => {
  if (!filters.endDate) return false
  return time.getTime() > new Date(filters.endDate).getTime()
}

const disabledEndDate = (time: Date) => {
  if (!filters.startDate) return time.getTime() > Date.now()
  return time.getTime() < new Date(filters.startDate).getTime() || time.getTime() > Date.now()
}

// 工具函数
const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatNumber = (num: number) => {
  return new Intl.NumberFormat('zh-CN').format(num)
}

const formatPercent = (num: number) => {
  return `${num.toFixed(1)}%`
}
</script>

<style scoped lang="scss">
.report-management {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;

  h1 {
    font-size: 24px;
    font-weight: 600;
    margin: 0 0 8px 0;
    color: #1f2937;
  }

  .page-description {
    color: #6b7280;
    margin: 0;
    font-size: 14px;
  }
}

.overview-cards {
  margin-bottom: 24px;

  .overview-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 20px;
  }

  .overview-card {
    display: flex;
    align-items: center;
    padding: 20px;
    background: white;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);

    .card-icon {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;

      .el-icon {
        font-size: 24px;
        color: white;
      }

      &.orders {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }

      &.revenue {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      }

      &.occupancy {
        background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
      }

      &.users {
        background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
      }
    }

    .card-content {
      flex: 1;

      h3 {
        font-size: 24px;
        font-weight: 600;
        margin: 0 0 4px 0;
        color: #1f2937;
      }

      p {
        margin: 0 0 4px 0;
        color: #6b7280;
        font-size: 14px;
      }

      .update-time {
        font-size: 12px;
        color: #9ca3af;
      }
    }
  }
}

.filter-section {
  margin-bottom: 24px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .filter-form {
    .el-form-item {
      margin-bottom: 0;
    }
  }
}

.report-tabs {
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;

  :deep(.el-tabs__header) {
    margin: 0;
    background-color: #f9fafb;
    border-bottom: 1px solid #e5e7eb;
  }

  :deep(.el-tabs__content) {
    padding: 0;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>