<template>
  <div class="pricing-management">
    <div class="page-header">
      <h1>价格策略管理</h1>
      <p class="page-description">管理酒店的定价策略、动态价格规则和特殊价格设置</p>
    </div>

    <el-tabs v-model="activeTab" class="pricing-tabs">
      <!-- 价格规则管理 -->
      <el-tab-pane label="价格规则" name="rules">
        <PricingRulesManager />
      </el-tab-pane>

      <!-- 特殊价格管理 -->
      <el-tab-pane label="特殊价格" name="special-prices">
        <SpecialPricesManager />
      </el-tab-pane>

      <!-- 价格历史 -->
      <el-tab-pane label="价格历史" name="history">
        <PriceHistoryManager />
      </el-tab-pane>

      <!-- 价格计算器 -->
      <el-tab-pane label="价格计算器" name="calculator">
        <PriceCalculator />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import PricingRulesManager from '../../../components/admin/pricing/PricingRulesManager.vue'
import SpecialPricesManager from '../../../components/admin/pricing/SpecialPricesManager.vue'
import PriceHistoryManager from '../../../components/admin/pricing/PriceHistoryManager.vue'
import PriceCalculator from '../../../components/admin/pricing/PriceCalculator.vue'

const activeTab = ref('rules')
</script>

<style scoped>
.pricing-management {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.page-description {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.pricing-tabs {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

:deep(.el-tabs__header) {
  margin: 0;
  border-bottom: 1px solid #e5e7eb;
  background: #f9fafb;
  border-radius: 8px 8px 0 0;
}

:deep(.el-tabs__nav-wrap) {
  padding: 0 24px;
}

:deep(.el-tabs__item) {
  padding: 0 20px;
  height: 48px;
  line-height: 48px;
  font-weight: 500;
  color: #6b7280;
  border-bottom: none;
}

:deep(.el-tabs__item.is-active) {
  color: #3b82f6;
  background: white;
}

:deep(.el-tabs__content) {
  padding: 24px;
}
</style>