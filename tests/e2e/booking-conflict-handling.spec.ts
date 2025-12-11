import { test, expect } from '@playwright/test'

test.describe('预订冲突处理流程', () => {
  test.beforeEach(async ({ page }) => {
    // 登录用户
    await page.goto('/login')
    await page.fill('[data-testid="email-input"]', 'test@example.com')
    await page.fill('[data-testid="password-input"]', 'password123')
    await page.click('[data-testid="login-button"]')

    // 等待登录成功并跳转到首页
    await expect(page.locator('[data-testid="user-menu"]')).toBeVisible()
  })

  test('完整的预订冲突检测和处理流程', async ({ page }) => {
    // 1. 搜索房间
    await page.goto('/rooms')
    await page.fill('[data-testid="search-input"]', '101')
    await page.click('[data-testid="search-button"]')

    // 等待搜索结果
    await expect(page.locator('[data-testid="room-list"]')).toBeVisible()

    // 2. 选择已预订的房间（触发冲突）
    const roomCard = page.locator('[data-testid="room-card"]').first()
    await expect(roomCard).toBeVisible()

    // 检查房间状态为已预订
    await expect(roomCard.locator('[data-testid="room-status"]')).toContainText('已预订')

    // 3. 尝试预订该房间
    await roomCard.click()
    await expect(page.locator('[data-testid="room-detail"]')).toBeVisible()

    // 填写预订信息
    await page.fill('[data-testid="check-in-date"]', '2024-02-01')
    await page.fill('[data-testid="check-out-date"]', '2024-02-03')
    await page.fill('[data-testid="guest-count"]', '2')

    // 点击预订按钮
    await page.click('[data-testid="booking-button"]')

    // 4. 验证冲突检测弹窗显示
    await expect(page.locator('[data-testid="conflict-detection-modal"]')).toBeVisible()
    await expect(page.locator('[data-testid="conflict-title"]')).toContainText('时间冲突')
    await expect(page.locator('[data-testid="conflict-message"]')).toContainText('您选择的时间段已被其他用户预订')

    // 5. 查看冲突详情
    await expect(page.locator('[data-testid="conflict-details"]')).toBeVisible()
    await expect(page.locator('[data-testid="conflict-type"]')).toContainText('时间重叠')
    await expect(page.locator('[data-testid="conflicting-order"]')).toBeVisible()

    // 6. 查看替代房间建议
    await expect(page.locator('[data-testid="alternative-rooms"]')).toBeVisible()
    const alternativeRooms = page.locator('[data-testid="alternative-room-card"]')
    const altRoomCount = await alternativeRooms.count()
    expect(altRoomCount).toBeGreaterThan(0)

    // 7. 测试选择替代房间
    const firstAlternativeRoom = alternativeRooms.first()
    const altRoomNumber = await firstAlternativeRoom.locator('[data-testid="room-number"]').textContent()

    await firstAlternativeRoom.locator('[data-testid="select-room-button"]').click()

    // 验证选择了替代房间
    await expect(page.locator('[data-testid="selected-room-info"]')).toContainText(altRoomNumber || '')

    // 8. 测试加入等待列表
    await page.click('[data-testid="join-waiting-list-button"]')
    await expect(page.locator('[data-testid="waiting-list-modal"]')).toBeVisible()

    // 填写等待列表信息
    await expect(page.locator('[data-testid="waiting-room-info"]')).toContainText('101')
    await expect(page.locator('[data-testid="waiting-dates"]')).toContainText('2024-02-01')

    // 确认加入等待列表
    await page.click('[data-testid="confirm-waiting-list-button"]')

    // 9. 验证等待列表成功提示
    await expect(page.locator('[data-testid="success-message"]')).toContainText('已成功加入等待列表')

    // 10. 验证等待列表管理界面
    await page.goto('/profile/waiting-list')
    await expect(page.locator('[data-testid="waiting-list-manager"]')).toBeVisible()

    const waitingListItems = page.locator('[data-testid="waiting-list-item"]')
    await expect(waitingListItems).toHaveCount(1)

    // 验证等待列表项信息
    const waitingItem = waitingListItems.first()
    await expect(waitingItem.locator('[data-testid="room-number"]')).toContainText('101')
    await expect(waitingItem.locator('[data-testid="status"]')).toContainText('等待中')
    await expect(waitingItem.locator('[data-testid="position"]')).toContainText('第')

    // 11. 测试移除等待列表项
    await waitingItem.locator('[data-testid="remove-button"]').click()
    await expect(page.locator('[data-testid="confirm-remove-modal"]')).toBeVisible()

    await page.click('[data-testid="confirm-remove-button"]')
    await expect(page.locator('[data-testid="remove-success-message"]')).toBeVisible()

    // 12. 验证等待列表已更新
    await page.reload()
    await expect(page.locator('[data-testid="empty-waiting-list"]')).toBeVisible()
  })

  test('并發预订冲突处理', async ({ page }) => {
    // 模拟并发预订场景
    await page.goto('/rooms/101')

    // 快速连续点击预订按钮
    await page.fill('[data-testid="check-in-date"]', '2024-02-01')
    await page.fill('[data-testid="check-out-date"]', '2024-02-03')
    await page.fill('[data-testid="guest-count"]', '2')

    // 快速点击两次预订按钮
    await page.click('[data-testid="booking-button"]')
    await page.click('[data-testid="booking-button"]')

    // 应该只显示一个冲突检测弹窗
    await expect(page.locator('[data-testid="conflict-detection-modal"]')).toHaveCount(1)

    // 验证并发请求冲突提示
    await expect(page.locator('[data-testid="conflict-title"]')).toContainText('并发请求冲突')
  })

  test('等待列表状态变更通知', async ({ page }) => {
    // 先加入等待列表
    await page.goto('/rooms/101')
    await page.fill('[data-testid="check-in-date"]', '2024-02-01')
    await page.fill('[data-testid="check-out-date"]', '2024-02-03')
    await page.fill('[data-testid="guest-count"]', '2')
    await page.click('[data-testid="booking-button"]')

    // 模拟房间可用通知
    await page.click('[data-testid="join-waiting-list-button"]')
    await page.click('[data-testid="confirm-waiting-list-button"]')

    // 等待通知服务触发（模拟）
    await page.waitForTimeout(2000)

    // 验证通知显示
    await expect(page.locator('[data-testid="notification-badge"]')).toBeVisible()
    await page.click('[data-testid="notification-center"]')

    const notifications = page.locator('[data-testid="notification-item"]')
    const roomAvailableNotification = notifications.filter({
      hasText: '房间可用'
    }).first()

    await expect(roomAvailableNotification).toBeVisible()

    // 点击通知查看详情
    await roomAvailableNotification.click()

    // 验证确认预订界面
    await expect(page.locator('[data-testid="confirm-booking-modal"]')).toBeVisible()
    await expect(page.locator('[data-testid="countdown-timer"]')).toBeVisible()

    // 测试确认预订
    await page.fill('[data-testid="order-id-input"]', 'ORD123456')
    await page.click('[data-testid="confirm-booking-button"]')

    // 验证预订成功
    await expect(page.locator('[data-testid="booking-success-message"]')).toContainText('预订成功')
  })

  test('管理员查看冲突统计数据', async ({ page }) => {
    // 以管理员身份登录
    await page.goto('/admin/login')
    await page.fill('[data-testid="admin-email-input"]', 'admin@hotel.com')
    await page.fill('[data-testid="admin-password-input"]', 'admin123')
    await page.click('[data-testid="admin-login-button"]')

    // 导航到冲突统计页面
    await page.goto('/admin/conflict-statistics')
    await expect(page.locator('[data-testid="conflict-statistics-page"]')).toBeVisible()

    // 验证统计数据展示
    await expect(page.locator('[data-testid="total-conflicts"]')).toBeVisible()
    await expect(page.locator('[data-testid="resolved-conflicts"]')).toBeVisible()
    await expect(page.locator('[data-testid="waiting-list-count"]')).toBeVisible()

    // 测试日期范围过滤
    await page.fill('[data-testid="start-date"]', '2024-01-01')
    await page.fill('[data-testid="end-date"]', '2024-01-31')
    await page.click('[data-testid="filter-button"]')

    // 验证图表更新
    await expect(page.locator('[data-testid="conflict-trend-chart"]')).toBeVisible()

    // 测试导出功能
    await page.click('[data-testid="export-button"]')
    await expect(page.locator('[data-testid="export-success-message"]')).toBeVisible()

    // 验证冲突热点房间列表
    const hotRooms = page.locator('[data-testid="hot-room-item"]')
    await expect(hotRooms).toHaveCount.greaterThan(0)

    const firstHotRoom = hotRooms.first()
    await expect(firstHotRoom.locator('[data-testid="room-number"]')).toBeVisible()
    await expect(firstHotRoom.locator('[data-testid="conflict-count"]')).toBeVisible()
  })

  test('移动端响应式冲突处理', async ({ page }) => {
    // 模拟移动设备
    await page.setViewportSize({ width: 375, height: 667 })

    await page.goto('/rooms/101')
    await page.fill('[data-testid="check-in-date"]', '2024-02-01')
    await page.fill('[data-testid="check-out-date"]', '2024-02-03')
    await page.fill('[data-testid="guest-count"]', '2')
    await page.click('[data-testid="booking-button"]')

    // 验证移动端冲突检测弹窗
    await expect(page.locator('[data-testid="conflict-detection-modal"]')).toBeVisible()
    await expect(page.locator('[data-testid="mobile-conflict-title"]')).toBeVisible()

    // 验证移动端替代房间卡片布局
    const alternativeRooms = page.locator('[data-testid="alternative-room-card-mobile"]')
    await expect(alternativeRooms).toHaveCount.greaterThan(0)

    // 测试移动端房间选择
    const firstMobileRoom = alternativeRooms.first()
    await firstMobileRoom.click()

    // 验证选择后的移动端界面
    await expect(page.locator('[data-testid="mobile-selected-room"]')).toBeVisible()

    // 测试移动端等待列表加入
    await page.click('[data-testid="mobile-join-waiting-list"]')
    await expect(page.locator('[data-testid="mobile-waiting-list-modal"]')).toBeVisible()

    await page.click('[data-testid="mobile-confirm-waiting"]')
    await expect(page.locator('[data-testid="mobile-success-toast"]')).toBeVisible()
  })

  test('性能测试 - 大量并发冲突检测', async ({ page }) => {
    const startTime = Date.now()

    // 批量创建冲突检测请求
    const promises = []
    for (let i = 0; i < 10; i++) {
      promises.push(
        page.evaluate(() => {
          return fetch('/api/v1/orders/check-conflict', {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json'
            }
          }).then(response => response.json())
        })
      )
    }

    // 等待所有请求完成
    const results = await Promise.all(promises)
    const endTime = Date.now()

    // 验证响应时间（应该在合理范围内）
    const responseTime = endTime - startTime
    expect(responseTime).toBeLessThan(5000) // 5秒内完成

    // 验证所有请求都有响应
    expect(results).toHaveLength(10)
    results.forEach(result => {
      expect(result).toHaveProperty('success')
    })
  })
})