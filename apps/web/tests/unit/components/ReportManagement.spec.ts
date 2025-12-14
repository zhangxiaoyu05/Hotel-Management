import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import ReportManagement from '@/pages/admin/reports/ReportManagement.vue'
import { useReportStore } from '@/stores/report'

// Mock the report store
vi.mock('@/stores/report', () => ({
  useReportStore: vi.fn(() => ({
    fetchReportOverview: vi.fn().mockResolvedValue({
      todayOrders: 25,
      todayRevenue: 2500,
      currentOccupancyRate: 85.0,
      monthlyNewUsers: 15,
      availableRooms: 80,
      maintenanceRooms: 5,
      activeUsers: 150
    }),
    fetchOrderReport: vi.fn().mockResolvedValue({
      totalOrders: 100,
      totalRevenue: 10000,
      averageOrderValue: 100,
      completionRate: 85.0
    }),
    exportReport: vi.fn().mockResolvedValue('/exports/reports/test.xlsx'),
    refreshReportCache: vi.fn().mockResolvedValue(undefined),
    loading: ref(false)
  }))
}))

// Mock Element Plus components
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn()
    }
  }
})

describe('ReportManagement', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(ReportManagement, {
      global: {
        components: {
          'el-card': { template: '<div><slot name="header"></slot><slot></slot></div>' },
          'el-row': { template: '<div><slot></slot></div>' },
          'el-col': { template: '<div><slot></slot></div>' },
          'el-button': { template: '<button @click="$emit(\'click\')"><slot></slot></button>' },
          'el-tabs': { template: '<div><slot></slot></div>', props: ['modelValue'], emits: ['tab-change'] },
          'el-tab-pane': { template: '<div><slot></slot></div>', props: ['name', 'label'] },
          'el-form': { template: '<form><slot></slot></form>' },
          'el-form-item': { template: '<div><slot></slot></div>' },
          'el-date-picker': { template: '<input />' },
          'el-select': { template: '<select><slot></slot></select>' },
          'el-option': { template: '<option><slot></slot></option>' },
          'el-dialog': {
            template: '<div v-if="modelValue"><slot></slot></div>',
            props: ['modelValue'],
            emits: ['update:modelValue']
          }
        }
      }
    })
  })

  it('renders correctly', () => {
    expect(wrapper.find('.report-management').exists()).toBe(true)
    expect(wrapper.find('.page-header h1').text()).toBe('数据统计报表')
  })

  it('displays overview cards', () => {
    const overviewCards = wrapper.findAll('.overview-card')
    expect(overviewCards).toHaveLength(4)

    const ordersCard = overviewCards[0]
    expect(ordersCard.find('h3').text()).toBe('25')
    expect(ordersCard.find('p').text()).toBe('今日订单')
  })

  it('has working filter form', () => {
    const filterSection = wrapper.find('.filter-section')
    expect(filterSection.exists()).toBe(true)

    const applyButton = wrapper.find('[data-test="apply-filters"]')
    expect(applyButton.exists()).toBe(true)
  })

  it('has export functionality', () => {
    const exportButton = wrapper.find('[data-test="export-button"]')
    expect(exportButton.exists()).toBe(true)

    exportButton.trigger('click')

    // Check if export dialog is shown
    expect(wrapper.vm.exportDialogVisible).toBe(true)
  })

  it('has refresh functionality', async () => {
    const refreshButton = wrapper.find('[data-test="refresh-button"]')
    expect(refreshButton.exists()).toBe(true)

    await refreshButton.trigger('click')

    // Check if refresh methods are called
    expect(useReportStore().fetchReportOverview).toHaveBeenCalled()
    expect(useReportStore().refreshReportCache).toHaveBeenCalled()
  })

  it('has report tabs', () => {
    const tabs = wrapper.find('.report-tabs')
    expect(tabs.exists()).toBe(true)

    const tabPanes = wrapper.findAll('.el-tab-pane')
    expect(tabPanes.length).toBeGreaterThanOrEqual(4)
  })
})