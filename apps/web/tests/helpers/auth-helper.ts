import { Page } from '@playwright/test'

export async function loginAsAdmin(page: Page): Promise<void> {
  await page.goto('/login')

  // 等待登录页面加载
  await page.waitForSelector('[data-testid="username-input"]')
  await page.waitForSelector('[data-testid="password-input"]')

  // 填写管理员凭据
  await page.fill('[data-testid="username-input"]', 'admin')
  await page.fill('[data-testid="password-input"]', 'Admin@123456')

  // 点击登录按钮
  await page.click('[data-testid="login-btn"]')

  // 等待登录成功
  await page.waitForURL('/dashboard')

  // 验证登录成功
  await expect(page.locator('[data-testid="user-avatar"]')).toBeVisible()
}

export async function loginAsRegularUser(page: Page): Promise<void> {
  await page.goto('/login')

  // 等待登录页面加载
  await page.waitForSelector('[data-testid="username-input"]')
  await page.waitForSelector('[data-testid="password-input"]')

  // 填写普通用户凭据
  await page.fill('[data-testid="username-input"]', 'testuser')
  await page.fill('[data-testid="password-input"]', 'Test@123456')

  // 点击登录按钮
  await page.click('[data-testid="login-btn"]')

  // 等待登录成功
  await page.waitForURL('/dashboard')

  // 验证登录成功
  await expect(page.locator('[data-testid="user-avatar"]')).toBeVisible()
}

export async function logout(page: Page): Promise<void> {
  // 点击用户下拉菜单
  await page.click('[data-testid="user-dropdown"]')

  // 点击退出登录按钮
  await page.click('[data-testid="logout-btn"]')

  // 等待重定向到登录页面
  await page.waitForURL('/login')

  // 验证已退出登录
  await expect(page.locator('[data-testid="username-input"]')).toBeVisible()
}

export async function createTestHotel(page: Page, hotelName: string): Promise<number> {
  // 导航到酒店管理页面
  await page.goto('/admin/hotels')

  // 点击创建酒店按钮
  await page.click('[data-testid="create-hotel-btn"]')

  // 填写基本信息
  await page.fill('[data-testid="hotel-name-input"]', hotelName)
  await page.fill('[data-testid="hotel-address-input"]', `测试地址${Date.now()}号`)
  await page.fill('[data-testid="hotel-phone-input"]', '13800138000')
  await page.fill('[data-testid="hotel-description-input"]', '这是一个测试酒店')

  // 选择设施
  await page.click('[data-testid="facility-wifi"]')
  await page.click('[data-testid="facility-parking"]')

  // 提交表单
  await page.click('[data-testid="submit-hotel-btn"]')

  // 等待创建成功
  await expect(page.locator('.el-message--success')).toContainText('酒店创建成功')

  // 返回列表页面，获取酒店ID
  await page.goto('/admin/hotels')
  await page.fill('[data-testid="search-input"]', hotelName)
  await page.click('[data-testid="search-btn"]')

  // 获取酒店卡片的ID
  const hotelCard = page.locator('[data-testid="hotel-card"]').first()
  const hotelId = await hotelCard.getAttribute('data-hotel-id')

  return parseInt(hotelId || '0')
}