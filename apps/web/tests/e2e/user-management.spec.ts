import { test, expect, type Page } from '@playwright/test'

test.describe('用户管理功能端到端测试', () => {
  let page: Page

  test.beforeEach(async ({ browser }) => {
    // 创建新的页面上下文
    page = await browser.newPage()

    // 设置localStorage来模拟已登录的管理员
    await page.goto('/')
    await page.evaluate(() => {
      localStorage.setItem('token', 'mock-admin-token')
      localStorage.setItem('userRole', 'ADMIN')
    })

    // 导航到用户管理页面
    await page.goto('/admin/users')

    // 等待页面加载完成
    await page.waitForLoadState('networkidle')
  })

  test.afterEach(async () => {
    await page.close()
  })

  test('应该显示用户管理页面标题和统计卡片', async () => {
    // 验证页面标题
    await expect(page.locator('h1')).toContainText('用户管理')

    // 验证统计卡片
    await expect(page.locator('[data-testid="total-users-stat"]')).toContainText('总用户数')
    await expect(page.locator('[data-testid="active-users-stat"]')).toContainText('活跃用户')
    await expect(page.locator('[data-testid="inactive-users-stat"]')).toContainText('非活跃用户')
    await expect(page.locator('[data-testid="new-users-stat"]')).toContainText('本月新增')
  })

  test('应该显示用户搜索表单', async () => {
    // 验证搜索表单元素
    await expect(page.locator('input[placeholder*="用户名"]')).toBeVisible()
    await expect(page.locator('input[placeholder*="邮箱"]')).toBeVisible()
    await expect(page.locator('select')).toBeVisible() // 状态选择
    await expect(page.locator('button:has-text("搜索")')).toBeVisible()
    await expect(page.locator('button:has-text("重置")')).toBeVisible()
  })

  test('应该显示用户列表表格', async () => {
    // 等待数据加载
    await page.waitForSelector('[data-testid="user-table"]')

    // 验证表格列头
    await expect(page.locator('th:has-text("ID")')).toBeVisible()
    await expect(page.locator('th:has-text("用户名")')).toBeVisible()
    await expect(page.locator('th:has-text("邮箱")')).toBeVisible()
    await expect(page.locator('th:has-text("手机号")')).toBeVisible()
    await expect(page.locator('th:has-text("状态")')).toBeVisible()
    await expect(page.locator('th:has-text("注册时间")')).toBeVisible()
    await expect(page.locator('th:has-text("最后登录")')).toBeVisible()
    await expect(page.locator('th:has-text("操作")')).toBeVisible()
  })

  test('应该能够搜索用户', async () => {
    // 等待页面加载完成
    await page.waitForSelector('[data-testid="user-table"]')

    // 输入搜索条件
    await page.fill('input[placeholder*="用户名"]', 'test')
    await page.fill('input[placeholder*="邮箱"]', 'example.com')
    await page.selectOption('select', 'ACTIVE')

    // 点击搜索按钮
    await page.click('button:has-text("搜索")')

    // 等待搜索结果
    await page.waitForTimeout(1000)

    // 验证搜索结果（假设有测试数据）
    await expect(page.locator('[data-testid="user-table"] tbody tr')).toHaveCount.greaterThan(0)
  })

  test('应该能够重置搜索条件', async () => {
    // 输入搜索条件
    await page.fill('input[placeholder*="用户名"]', 'test')
    await page.fill('input[placeholder*="邮箱"]', 'example.com')
    await page.selectOption('select', 'ACTIVE')

    // 点击重置按钮
    await page.click('button:has-text("重置")')

    // 验证搜索条件已清空
    await expect(page.locator('input[placeholder*="用户名"]')).toHaveValue('')
    await expect(page.locator('input[placeholder*="邮箱"]')).toHaveValue('')
    await expect(page.locator('select')).toHaveValue('')
  })

  test('应该能够分页浏览用户列表', async () => {
    // 等待用户列表加载
    await page.waitForSelector('[data-testid="user-table"]')

    // 检查分页控件是否存在
    const pagination = page.locator('[data-testid="pagination"]')
    if (await pagination.isVisible()) {
      // 点击下一页
      await page.click('[data-testid="pagination"] button:has-text("下一页")')
      await page.waitForTimeout(1000)

      // 验证页面内容更新（可以通过检查不同的用户数据来验证）
      await expect(page.locator('[data-testid="user-table"]')).toBeVisible()
    }
  })

  test('应该能够更新单个用户状态', async () => {
    // 等待用户列表加载
    await page.waitForSelector('[data-testid="user-table"]')

    // 获取第一行数据
    const firstRow = page.locator('[data-testid="user-table"] tbody tr').first()
    await expect(firstRow).toBeVisible()

    // 查找状态切换按钮
    const statusButton = firstRow.locator('[data-testid="status-toggle-btn"]')
    if (await statusButton.isVisible()) {
      // 点击状态切换按钮
      await statusButton.click()

      // 等待确认对话框
      await page.waitForSelector('[role="dialog"]')

      // 点击确认按钮
      await page.click('[data-testid="confirm-btn"]')

      // 等待操作完成
      await page.waitForTimeout(1000)

      // 验证成功消息
      await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
    }
  })

  test('应该能够批量更新用户状态', async () => {
    // 等待用户列表加载
    await page.waitForSelector('[data-testid="user-table"]')

    // 选择多个用户
    const checkboxes = page.locator('[data-testid="user-checkbox"]')
    const checkboxCount = await checkboxes.count()

    if (checkboxCount >= 2) {
      // 选择前两个用户
      await checkboxes.nth(0).check()
      await checkboxes.nth(1).check()

      // 验证批量操作按钮已启用
      await expect(page.locator('[data-testid="batch-status-btn"]')).toBeEnabled()

      // 点击批量状态更新按钮
      await page.click('[data-testid="batch-status-btn"]')

      // 等待下拉菜单出现
      await page.waitForSelector('[data-testid="batch-dropdown"]')

      // 选择新状态
      await page.click('[data-testid="set-inactive-btn"]')

      // 等待确认对话框
      await page.waitForSelector('[role="dialog"]')

      // 点击确认按钮
      await page.click('[data-testid="confirm-batch-btn"]')

      // 等待操作完成
      await page.waitForTimeout(2000)

      // 验证成功消息
      await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
    }
  })

  test('应该能够查看用户详情', async () => {
    // 等待用户列表加载
    await page.waitForSelector('[data-testid="user-table"]')

    // 查找详情按钮
    const detailButton = page.locator('[data-testid="user-detail-btn"]').first()
    if (await detailButton.isVisible()) {
      // 点击详情按钮
      await detailButton.click()

      // 等待详情弹窗
      await page.waitForSelector('[data-testid="user-detail-modal"]')

      // 验证弹窗内容
      await expect(page.locator('[data-testid="user-detail-modal"] h2')).toContainText('用户详情')
      await expect(page.locator('[data-testid="detail-username"]')).toBeVisible()
      await expect(page.locator('[data-testid="detail-email"]')).toBeVisible()
      await expect(page.locator('[data-testid="detail-phone"]')).toBeVisible()

      // 关闭弹窗
      await page.click('[data-testid="close-modal-btn"]')

      // 验证弹窗已关闭
      await expect(page.locator('[data-testid="user-detail-modal"]')).not.toBeVisible()
    }
  })

  test('应该处理加载状态', async () => {
    // 触发搜索操作
    await page.fill('input[placeholder*="用户名"]', 'loading-test')
    await page.click('button:has-text("搜索")')

    // 验证加载状态
    await expect(page.locator('[data-testid="loading-spinner"]')).toBeVisible()

    // 等待加载完成
    await page.waitForSelector('[data-testid="user-table"]', { state: 'visible' })
    await expect(page.locator('[data-testid="loading-spinner"]')).not.toBeVisible()
  })

  test('应该处理错误状态', async () => {
    // 模拟API错误（通过拦截网络请求）
    await page.route('**/api/admin/users/search', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Internal Server Error' })
      })
    })

    // 触发搜索操作
    await page.fill('input[placeholder*="用户名"]', 'error-test')
    await page.click('button:has-text("搜索")')

    // 等待错误消息显示
    await page.waitForTimeout(1000)

    // 验证错误消息
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
  })

  test('应该验证数据脱敏', async () => {
    // 等待用户列表加载
    await page.waitForSelector('[data-testid="user-table"]')

    // 验证手机号脱敏格式
    const phoneCells = page.locator('[data-testid="user-table"] tbody tr td:nth-child(4)')
    const phoneCount = await phoneCells.count()

    if (phoneCount > 0) {
      const firstPhone = await phoneCells.first().textContent()
      expect(firstPhone).toMatch(/^\d{3}\*\*\*\*\d{4}$/) // 格式：138****8000
    }

    // 验证状态标签显示正确
    const statusCells = page.locator('[data-testid="user-table"] tbody tr td:nth-child(5)')
    const statusCount = await statusCells.count()

    if (statusCount > 0) {
      const statusText = await statusCells.first().textContent()
      expect(['ACTIVE', 'INACTIVE']).toContain(statusText)
    }
  })

  test('应该响应式设计在不同屏幕尺寸下正常工作', async () => {
    // 测试桌面视图
    await page.setViewportSize({ width: 1200, height: 800 })
    await expect(page.locator('[data-testid="user-table"]')).toBeVisible()

    // 测试平板视图
    await page.setViewportSize({ width: 768, height: 1024 })
    await expect(page.locator('[data-testid="user-table"]')).toBeVisible()

    // 测试移动视图
    await page.setViewportSize({ width: 375, height: 667 })

    // 在移动视图中，表格可能变为卡片形式
    const tableOrCards = page.locator('[data-testid="user-table"], [data-testid="user-cards"]')
    await expect(tableOrCards).toBeVisible()
  })
})