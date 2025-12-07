import { test, expect } from '@playwright/test';

test.describe('房间状态管理', () => {
  test.beforeEach(async ({ page }) => {
    // 登录管理员账号
    await page.goto('/login');
    await page.fill('[data-testid="username"]', 'admin');
    await page.fill('[data-testid="password"]', 'admin123');
    await page.click('[data-testid="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('房间状态自动更新 - 预订成功后状态变为已占用', async ({ page }) => {
    // 1. 导航到房间列表页面
    await page.goto('/rooms');
    await page.waitForLoadState('networkidle');

    // 2. 找到一个可用房间并记录其状态
    const availableRoom = page.locator('[data-testid="room-card"]:has-text("可用")').first();
    await expect(availableRoom).toBeVisible();

    const roomName = await availableRoom.locator('[data-testid="room-name"]').textContent();
    const roomNumber = await availableRoom.locator('[data-testid="room-number"]').textContent();

    // 3. 点击预订房间
    await availableRoom.locator('[data-testid="book-button"]').click();
    await page.waitForURL('/booking/*');

    // 4. 填写预订信息并提交
    await page.fill('[data-testid="check-in-date"]', '2024-12-20');
    await page.fill('[data-testid="check-out-date"]', '2024-12-22');
    await page.fill('[data-testid="guest-count"]', '2');
    await page.click('[data-testid="confirm-booking"]');

    // 5. 等待预订成功
    await page.waitForSelector('[data-testid="booking-success"]');
    await expect(page.locator('[data-testid="booking-success"]')).toContainText('预订成功');

    // 6. 验证房间状态已更新为已占用
    await page.goto('/rooms');
    await page.waitForLoadState('networkidle');

    const updatedRoom = page.locator(`[data-testid="room-card"]:has-text("${roomNumber}")`);
    await expect(updatedRoom.locator('[data-testid="room-status"]')).toContainText('已占用');
  });

  test('房间状态恢复 - 订单取消后状态恢复为可用', async ({ page }) => {
    // 1. 导航到订单列表
    await page.goto('/orders');
    await page.waitForLoadState('networkidle');

    // 2. 找到一个已确认的订单
    const confirmedOrder = page.locator('[data-testid="order-card"]:has-text("已确认")').first();
    await expect(confirmedOrder).toBeVisible();

    // 3. 点击取消订单
    await confirmedOrder.locator('[data-testid="cancel-order"]').click();
    await page.waitForSelector('[data-testid="cancel-modal"]');

    // 4. 填写取消原因并确认
    await page.fill('[data-testid="cancel-reason"]', '行程变更');
    await page.click('[data-testid="confirm-cancel"]');

    // 5. 等待取消成功
    await page.waitForSelector('[data-testid="cancel-success"]');
    await expect(page.locator('[data-testid="cancel-success"]')).toContainText('订单已取消');

    // 6. 验证相关房间状态已恢复为可用
    const orderDetails = await confirmedOrder.locator('[data-testid="order-details"]').textContent();
    const roomNumber = orderDetails?.match(/房间号：(\w+)/)?.[1];

    if (roomNumber) {
      await page.goto('/rooms');
      await page.waitForLoadState('networkidle');

      const room = page.locator(`[data-testid="room-card"]:has-text("${roomNumber}")`);
      await expect(room.locator('[data-testid="room-status"]')).toContainText('可用');
    }
  });

  test('状态冲突检测 - 并发修改同一房间状态', async ({ page, context }) => {
    // 1. 打开两个浏览器窗口模拟并发操作
    const page2 = await context.newPage();

    // 2. 两个页面都登录并导航到房间详情页
    for (const p of [page, page2]) {
      await p.goto('/login');
      await p.fill('[data-testid="username"]', 'admin');
      await p.fill('[data-testid="password"]', 'admin123');
      await p.click('[data-testid="login-button"]');
      await p.waitForURL('/dashboard');

      await p.goto('/rooms/1');
      await p.waitForLoadState('networkidle');
    }

    // 3. 两个页面同时尝试修改房间状态
    await page.click('[data-testid="edit-status-button"]');
    await page.selectOption('[data-testid="status-select"]', 'MAINTENANCE');
    await page.fill('[data-testid="status-reason"]', '维护设备');

    await page2.click('[data-testid="edit-status-button"]');
    await page2.selectOption('[data-testid="status-select"]', 'CLEANING');
    await page2.fill('[data-testid="status-reason"]', '清洁房间');

    // 4. 第一个页面提交
    await page.click('[data-testid="save-status"]');

    // 5. 第二个页面提交（应该失败并提示冲突）
    await page2.click('[data-testid="save-status"]');

    // 6. 验证冲突提示
    await expect(page2.locator('[data-testid="conflict-error"]')).toBeVisible();
    await expect(page2.locator('[data-testid="conflict-error"]')).toContainText('房间已被其他用户修改');

    // 7. 关闭第二个页面
    await page2.close();

    // 8. 验证第一个页面的修改成功
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="room-status"]')).toContainText('维护中');
  });

  test('状态日志查看 - 查看房间状态变更历史', async ({ page }) => {
    // 1. 导航到房间详情页
    await page.goto('/rooms/1');
    await page.waitForLoadState('networkidle');

    // 2. 点击查看状态日志
    await page.click('[data-testid="view-status-logs"]');
    await page.waitForURL('/rooms/1/status-logs');

    // 3. 验证日志页面显示
    await expect(page.locator('[data-testid="status-logs-page"]')).toBeVisible();
    await expect(page.locator('[data-testid="timeline-header"]')).toContainText('状态变更时间线');

    // 4. 验证日志列表显示
    const logItems = page.locator('[data-testid="log-item"]');
    if (await logItems.count() > 0) {
      const firstLog = logItems.first();
      await expect(firstLog.locator('[data-testid="log-timestamp"]')).toBeVisible();
      await expect(firstLog.locator('[data-testid="log-reason"]')).toBeVisible();
      await expect(firstLog.locator('[data-testid="log-operator"]')).toBeVisible();
    }

    // 5. 测试筛选功能
    await page.click('[data-testid="filter-button"]');
    await page.selectOption('[data-testid="status-filter"]', 'AVAILABLE');
    await page.click('[data-testid="apply-filter"]');

    // 6. 测试分页功能
    const pagination = page.locator('[data-testid="pagination"]');
    if (await pagination.isVisible()) {
      await pagination.locator('[data-testid="next-page"]').click();
      await page.waitForLoadState('networkidle');
    }
  });

  test('权限控制 - 不同用户角色状态修改权限', async ({ page }) => {
    // 测试普通用户权限
    await page.goto('/login');
    await page.fill('[data-testid="username"]', 'user');
    await page.fill('[data-testid="password"]', 'user123');
    await page.click('[data-testid="login-button"]');
    await page.waitForURL('/dashboard');

    await page.goto('/rooms/1');
    await page.waitForLoadState('networkidle');

    // 普通用户应该看不到状态修改按钮
    await expect(page.locator('[data-testid="edit-status-button"]')).not.toBeVisible();

    // 登出并测试员工权限
    await page.click('[data-testid="user-menu"]');
    await page.click('[data-testid="logout"]');
    await page.goto('/login');

    await page.fill('[data-testid="username"]', 'staff');
    await page.fill('[data-testid="password"]', 'staff123');
    await page.click('[data-testid="login-button"]');
    await page.waitForURL('/dashboard');

    await page.goto('/rooms/1');
    await page.waitForLoadState('networkidle');

    // 员工应该能看到状态修改按钮但只能修改为特定状态
    await expect(page.locator('[data-testid="edit-status-button"]')).toBeVisible();

    await page.click('[data-testid="edit-status-button"]');
    await page.waitForSelector('[data-testid="status-select"]');

    // 验证可用选项
    const statusOptions = page.locator('[data-testid="status-select"] option');
    await expect(statusOptions).toHaveCount(2); // 只有MAINTENANCE和CLEANING
  });

  test('实时状态同步 - WebSocket状态变更推送', async ({ page, context }) => {
    // 1. 打开两个页面
    const page2 = await context.newPage();

    // 2. 两个页面都登录并导航到同一个房间详情页
    for (const p of [page, page2]) {
      await p.goto('/login');
      await p.fill('[data-testid="username"]', 'admin');
      await p.fill('[data-testid="password"]', 'admin123');
      await p.click('[data-testid="login-button"]');
      await p.waitForURL('/dashboard');

      await p.goto('/rooms/1');
      await p.waitForLoadState('networkidle');
    }

    // 3. 在第一个页面修改房间状态
    await page.click('[data-testid="edit-status-button"]');
    await page.selectOption('[data-testid="status-select"]', 'MAINTENANCE');
    await page.fill('[data-testid="status-reason"]', '测试实时同步');
    await page.click('[data-testid="save-status"]');

    // 4. 等待第一个页面修改成功
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible();

    // 5. 验证第二个页面的状态自动更新
    await page2.waitForTimeout(2000); // 等待WebSocket推送
    await expect(page2.locator('[data-testid="room-status"]')).toContainText('维护中');

    // 6. 验证状态变更通知
    await expect(page2.locator('[data-testid="status-notification"]')).toBeVisible();
    await expect(page2.locator('[data-testid="status-notification"]'))
      .toContainText('房间状态已更新');

    // 7. 关闭第二个页面
    await page2.close();
  });

  test('性能测试 - 大量房间状态查询性能', async ({ page }) => {
    // 1. 导航到房间列表页面
    await page.goto('/rooms');
    await page.waitForLoadState('networkidle');

    // 2. 记录页面加载时间
    const startTime = Date.now();
    await page.waitForSelector('[data-testid="room-list"]');
    const loadTime = Date.now() - startTime;

    // 验证页面加载时间在合理范围内（小于3秒）
    expect(loadTime).toBeLessThan(3000);

    // 3. 测试筛选功能性能
    const filterStartTime = Date.now();
    await page.selectOption('[data-testid="status-filter"]', 'AVAILABLE');
    await page.waitForLoadState('networkidle');
    const filterTime = Date.now() - filterStartTime;

    // 验证筛选响应时间在合理范围内（小于1秒）
    expect(filterTime).toBeLessThan(1000);

    // 4. 测试批量检查可用性性能
    const batchCheckStartTime = Date.now();
    await page.click('[data-testid="batch-check-availability"]');
    await page.waitForSelector('[data-testid="availability-results"]');
    const batchCheckTime = Date.now() - batchCheckStartTime;

    // 验证批量检查响应时间在合理范围内（小于2秒）
    expect(batchCheckTime).toBeLessThan(2000);
  });
});