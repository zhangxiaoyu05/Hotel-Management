import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth-helper'

test.describe('酒店管理端到端测试', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/hotels')
  })

  test('完整的酒店创建、编辑、删除流程', async ({ page }) => {
    // 验证页面加载
    await expect(page.locator('h1')).toContainText('酒店管理')
    await expect(page.locator('.hotel-list')).toBeVisible()

    // 点击创建酒店按钮
    await page.click('[data-testid="create-hotel-btn"]')
    await expect(page).toHaveURL(/\/admin\/hotels\/create/)

    // 填写酒店基本信息
    await page.fill('[data-testid="hotel-name-input"]', '测试端到端酒店')
    await page.fill('[data-testid="hotel-address-input"]', '测试地址123号')
    await page.fill('[data-testid="hotel-phone-input"]', '13800138001')
    await page.fill('[data-testid="hotel-description-input"]', '这是一个端到端测试创建的酒店，包含所有必要的信息。')

    // 选择设施
    await page.click('[data-testid="facility-wifi"]')
    await page.click('[data-testid="facility-parking"]')
    await page.click('[data-testid="facility-pool"]')

    // 上传图片
    const fileInput = page.locator('[data-testid="hotel-image-upload"] input[type="file"]')
    await fileInput.setInputFiles('tests/fixtures/hotel-test-image.jpg')

    // 等待图片上传完成
    await expect(page.locator('[data-testid="image-preview"]')).toBeVisible()
    await expect(page.locator('[data-testid="upload-progress"]')).not.toBeVisible()

    // 提交表单
    await page.click('[data-testid="submit-hotel-btn"]')

    // 验证创建成功消息
    await expect(page.locator('.el-message--success')).toContainText('酒店创建成功')
    await expect(page).toHaveURL(/\/admin\/hotels$/)

    // 搜索刚创建的酒店
    await page.fill('[data-testid="search-input"]', '测试端到端酒店')
    await page.click('[data-testid="search-btn"]')

    // 验证酒店出现在列表中
    await expect(page.locator('[data-testid="hotel-card"]')).toContainText('测试端到端酒店')

    // 点击编辑按钮
    await page.click('[data-testid="edit-hotel-btn"]')
    await expect(page).toHaveURL(/\/admin\/hotels\/\d+\/edit/)

    // 修改酒店信息
    await page.fill('[data-testid="hotel-name-input"]', '测试端到端酒店-已更新')
    await page.fill('[data-testid="hotel-description-input"]', '这是更新后的酒店描述。')

    // 添加更多设施
    await page.click('[data-testid="facility-gym"]')
    await page.click('[data-testid="facility-restaurant"]')

    // 提交更新
    await page.click('[data-testid="submit-hotel-btn"]')

    // 验证更新成功消息
    await expect(page.locator('.el-message--success')).toContainText('酒店更新成功')
    await expect(page).toHaveURL(/\/admin\/hotels$/)

    // 验证更新后的信息
    await expect(page.locator('[data-testid="hotel-card"]')).toContainText('测试端到端酒店-已更新')
    await expect(page.locator('[data-testid="hotel-card"]')).toContainText('这是更新后的酒店描述。')

    // 测试状态切换
    await page.click('[data-testid="status-toggle-btn"]')
    await expect(page.locator('.el-message--success')).toContainText('酒店状态更新成功')
    await expect(page.locator('[data-testid="hotel-status"]')).toContainText('已停业')

    // 再次切换状态
    await page.click('[data-testid="status-toggle-btn"]')
    await expect(page.locator('.el-message--success')).toContainText('酒店状态更新成功')
    await expect(page.locator('[data-testid="hotel-status"]')).toContainText('运营中')

    // 查看酒店详情
    await page.click('[data-testid="view-hotel-btn"]')
    await expect(page).toHaveURL(/\/admin\/hotels\/\d+$/)

    // 验证详情页面的所有信息
    await expect(page.locator('[data-testid="hotel-detail-name"]')).toContainText('测试端到端酒店-已更新')
    await expect(page.locator('[data-testid="hotel-detail-address"]')).toContainText('测试地址123号')
    await expect(page.locator('[data-testid="hotel-detail-phone"]')).toContainText('13800138001')
    await expect(page.locator('[data-testid="hotel-detail-description"]')).toContainText('这是更新后的酒店描述。')

    // 验证设施标签
    await expect(page.locator('[data-testid="facility-wifi"]')).toBeVisible()
    await expect(page.locator('[data-testid="facility-parking"]')).toBeVisible()
    await expect(page.locator('[data-testid="facility-pool"]')).toBeVisible()
    await expect(page.locator('[data-testid="facility-gym"]')).toBeVisible()
    await expect(page.locator('[data-testid="facility-restaurant"]')).toBeVisible()

    // 验证图片展示
    await expect(page.locator('[data-testid="hotel-image-gallery"]')).toBeVisible()
    await expect(page.locator('[data-testid="hotel-image"]')).toHaveCount(1)

    // 返回列表页面
    await page.click('[data-testid="back-to-list-btn"]')
    await expect(page).toHaveURL(/\/admin\/hotels$/)

    // 删除酒店
    await page.click('[data-testid="delete-hotel-btn"]')

    // 确认删除对话框
    await expect(page.locator('.el-message-box')).toBeVisible()
    await expect(page.locator('.el-message-box__message')).toContainText('确定要删除这个酒店吗？此操作不可撤销。')

    await page.click('.el-message-box__btns .el-button--primary')

    // 验证删除成功消息
    await expect(page.locator('.el-message--success')).toContainText('酒店删除成功')

    // 验证酒店已从列表中移除
    await expect(page.locator('[data-testid="hotel-card"]')).not.toContainText('测试端到端酒店-已更新')
  })

  test('酒店搜索和筛选功能', async ({ page }) => {
    // 等待页面加载
    await expect(page.locator('.hotel-list')).toBeVisible()

    // 按名称搜索
    await page.fill('[data-testid="search-input"]', '测试')
    await page.click('[data-testid="search-btn"]')

    // 验证搜索结果
    await expect(page.locator('[data-testid="hotel-card"]')).toBeVisible()

    // 清空搜索
    await page.fill('[data-testid="search-input"]', '')
    await page.click('[data-testid="search-btn"]')

    // 按状态筛选
    await page.click('[data-testid="status-filter"]')
    await page.click('[data-testid="status-active"]')
    await expect(page.locator('[data-testid="hotel-card"]')).toBeVisible()

    await page.click('[data-testid="status-filter"]')
    await page.click('[data-testid="status-inactive"]')
    await expect(page.locator('[data-testid="hotel-card"]')).toBeVisible()

    // 重置筛选
    await page.click('[data-testid="reset-filters-btn"]')
    await expect(page.locator('[data-testid="search-input"]')).toHaveValue('')
    await expect(page.locator('[data-testid="status-filter"]')).toHaveText('全部状态')
  })

  test('分页功能测试', async ({ page }) => {
    // 等待页面加载
    await expect(page.locator('.hotel-list')).toBeVisible()

    // 检查分页组件
    await expect(page.locator('.el-pagination')).toBeVisible()

    // 测试页面大小选择
    await page.click('.el-pagination__sizes .el-select')
    await page.click('.el-select-dropdown__item:has-text("10")')

    // 验证页面大小改变
    await expect(page.locator('[data-testid="hotel-card"]')).toHaveCount.lessThan(20)

    // 测试页面导航
    const totalElements = await page.locator('.el-pagination__total').textContent()
    if (totalElements && parseInt(totalElements.match(/\d+/)?.[0] || '0') > 10) {
      await page.click('.el-pagination__btn-next')
      await expect(page.locator('.el-pagination__current')).toContainText('2')
    }
  })

  test('表单验证测试', async ({ page }) => {
    // 导航到创建页面
    await page.click('[data-testid="create-hotel-btn"]')
    await expect(page).toHaveURL(/\/admin\/hotels\/create/)

    // 测试必填字段验证
    await page.click('[data-testid="submit-hotel-btn"]')

    await expect(page.locator('[data-testid="hotel-name-error"]')).toContainText('请输入酒店名称')
    await expect(page.locator('[data-testid="hotel-address-error"]')).toContainText('请输入酒店地址')

    // 测试长度限制
    await page.fill('[data-testid="hotel-name-input"]', 'a'.repeat(101))
    await page.blur('[data-testid="hotel-name-input"]')
    await expect(page.locator('[data-testid="hotel-name-error"]')).toContainText('酒店名称长度不能超过100个字符')

    // 测试电话号码格式
    await page.fill('[data-testid="hotel-phone-input"]', 'invalid-phone')
    await page.blur('[data-testid="hotel-phone-input"]')
    await expect(page.locator('[data-testid="hotel-phone-error"]')).toContainText('请输入有效的手机号码')

    // 修复所有错误
    await page.fill('[data-testid="hotel-name-input"]', '有效的酒店名称')
    await page.fill('[data-testid="hotel-address-input"]', '有效的酒店地址')
    await page.fill('[data-testid="hotel-phone-input"]', '13800138001')

    // 验证错误消息消失
    await expect(page.locator('[data-testid="hotel-name-error"]')).not.toBeVisible()
    await expect(page.locator('[data-testid="hotel-address-error"]')).not.toBeVisible()
    await expect(page.locator('[data-testid="hotel-phone-error"]')).not.toBeVisible()
  })

  test('图片上传功能测试', async ({ page }) => {
    // 导航到创建页面
    await page.click('[data-testid="create-hotel-btn"]')

    // 测试有效图片上传
    const validFileInput = page.locator('[data-testid="hotel-image-upload"] input[type="file"]')
    await validFileInput.setInputFiles('tests/fixtures/hotel-test-image.jpg')

    await expect(page.locator('[data-testid="image-preview"]')).toBeVisible()
    await expect(page.locator('[data-testid="upload-success"]')).toContainText('上传成功')

    // 测试删除上传的图片
    await page.click('[data-testid="remove-image-btn"]')
    await expect(page.locator('[data-testid="image-preview"]')).not.toBeVisible()

    // 测试无效文件格式
    const invalidFileInput = page.locator('[data-testid="hotel-image-upload"] input[type="file"]')
    await invalidFileInput.setInputFiles('tests/fixtures/invalid-file.pdf')

    await expect(page.locator('.el-message--error')).toContainText('不支持的文件格式')
    await expect(page.locator('[data-testid="image-preview"]')).not.toBeVisible()

    // 测试文件过大
    // 注意：这个测试可能需要根据实际文件大小限制调整
    await invalidFileInput.setInputFiles('tests/fixtures/large-image.jpg')
    await expect(page.locator('.el-message--error')).toContainText('文件大小不能超过')
  })

  test('响应式设计测试', async ({ page }) => {
    // 测试桌面视图
    await page.setViewportSize({ width: 1200, height: 800 })
    await expect(page.locator('.hotel-list')).toBeVisible()
    await expect(page.locator('[data-testid="hotel-card"]')).toHaveCount.greaterThan(0)

    // 测试平板视图
    await page.setViewportSize({ width: 768, height: 1024 })
    await expect(page.locator('.hotel-list')).toBeVisible()
    await expect(page.locator('[data-testid="hotel-card"]')).toHaveCount.greaterThan(0)

    // 测试移动端视图
    await page.setViewportSize({ width: 375, height: 667 })
    await expect(page.locator('.hotel-list')).toBeVisible()
    await expect(page.locator('[data-testid="hotel-card"]')).toHaveCount.greaterThan(0)

    // 验证移动端导航菜单
    await expect(page.locator('.mobile-nav')).toBeVisible()
  })

  test('权限测试', async ({ page }) => {
    // 退出管理员账户
    await page.click('[data-testid="user-dropdown"]')
    await page.click('[data-testid="logout-btn"]')

    // 尝试访问酒店管理页面
    await page.goto('/admin/hotels')

    // 应该重定向到登录页面
    await expect(page).toHaveURL(/\/login/)

    // 普通用户登录
    await page.fill('[data-testid="username-input"]', 'regularuser')
    await page.fill('[data-testid="password-input"]', 'password123')
    await page.click('[data-testid="login-btn"]')

    // 尝试访问酒店管理页面
    await page.goto('/admin/hotels')

    // 应该显示权限不足
    await expect(page.locator('.el-message--error')).toContainText('权限不足')
  })
})