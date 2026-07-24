<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="28" height="28">
            <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
        </div>
        <h2>会议室预约</h2>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,0.7)"
        active-text-color="#ffffff"
        class="side-menu"
      >
        <template v-for="item in menuItems" :key="item.id">
          <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path || String(item.id)">
            <template #title><el-icon><component :is="item.icon || 'Document'" /></el-icon><span>{{ item.name }}</span></template>
            <el-menu-item v-for="child in item.children" :key="child.id" :index="child.path">{{ child.name }}</el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon || 'Document'" /></el-icon>
            <span>{{ item.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <span class="greeting">你好，{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</span>
        </div>
        <div class="header-right">
          <el-tag v-if="userStore.isAdmin()" type="danger" size="small" effect="dark" round>管理员</el-tag>
          <el-dropdown trigger="click">
            <div class="avatar-btn">
              <div class="avatar">{{ (userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}</div>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { OfficeBuilding, ArrowDown, User, Calendar, Menu, HomeFilled, Document } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMyMenus } from '@/api/menu'
import type { MenuItem } from '@/types/menu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const menuItems = ref<MenuItem[]>([])

async function loadMenus() {
  try {
    const res = await getMyMenus()
    menuItems.value = res.data
  } catch { /* */ }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(loadMenus)
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background: linear-gradient(180deg, #1a1640 0%, #302b63 50%, #24243e 100%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo h2 {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin: 0;
  white-space: nowrap;
}

.side-menu {
  border-right: none;
  padding: 8px 0;
}

.side-menu .el-menu-item {
  border-radius: 8px;
  margin: 4px 12px;
  height: 44px;
  line-height: 44px;
  transition: all 0.2s;
}

.side-menu .el-menu-item:hover {
  background: rgba(102, 126, 234, 0.15) !important;
}

.side-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3), rgba(118, 75, 162, 0.3)) !important;
  color: #fff !important;
  font-weight: 500;
}

.layout-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.greeting {
  font-size: 14px;
  color: #64748b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.avatar-btn:hover {
  background: #f8fafc;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.arrow {
  font-size: 12px;
  color: #9ca3af;
}

.layout-main {
  background: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
