<script setup lang="ts">
import { RouterView } from 'vue-router'
import { ElContainer, ElHeader, ElMain, ElMenu, ElMenuItem, ElButton } from 'element-plus'
import { useUserStore } from './stores/user'
import { computed } from 'vue'

const userStore = useUserStore()
const isAuthenticated = computed(() => userStore.isAuthenticated)

const handleLogout = () => {
  userStore.logout()
}
</script>

<template>
  <ElContainer class="app-container">
    <ElHeader class="header">
      <div class="header-content">
        <h1>成都酒店管理系统</h1>
        <ElMenu mode="horizontal" router :default-active="$route.path">
          <ElMenuItem index="/">首页</ElMenuItem>
          <ElMenuItem index="/hotels">酒店列表</ElMenuItem>
          <ElMenuItem v-if="isAuthenticated" index="/profile">个人中心</ElMenuItem>
        </ElMenu>
        <div class="user-actions">
          <template v-if="isAuthenticated">
            <ElButton @click="handleLogout">退出登录</ElButton>
          </template>
          <template v-else>
            <RouterLink to="/login">
              <ElButton type="primary">登录</ElButton>
            </RouterLink>
            <RouterLink to="/register">
              <ElButton>注册</ElButton>
            </RouterLink>
          </template>
        </div>
      </div>
    </ElHeader>
    <ElMain class="main">
      <RouterView />
    </ElMain>
  </ElContainer>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.header-content h1 {
  margin: 0;
  color: #303133;
  font-size: 20px;
}

.header-content .el-menu {
  flex: 1;
  margin: 0 40px;
  border-bottom: none;
}

.user-actions {
  display: flex;
  gap: 10px;
}

.user-actions a {
  text-decoration: none;
}

.main {
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}
</style>
