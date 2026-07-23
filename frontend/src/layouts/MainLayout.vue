<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="24" height="24">
            <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
        </div>
        <span class="logo-text">会议室预约</span>
      </div>
      <el-menu :default-active="route.path" router background-color="transparent" text-color="rgba(255,255,255,0.6)" active-text-color="#fff" class="side-menu">
        <el-menu-item index="/home"><el-icon><HomeFilled /></el-icon><span>首页</span></el-menu-item>
        <el-menu-item index="/meeting/rooms"><el-icon><OfficeBuilding /></el-icon><span>会议室</span></el-menu-item>
        <el-menu-item index="/reservation/my"><el-icon><Calendar /></el-icon><span>我的预约</span></el-menu-item>
        <el-menu-item v-if="userStore.isAdmin()" index="/admin/users"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item v-if="userStore.isAdmin()" index="/admin/rooms"><el-icon><OfficeBuilding /></el-icon><span>会议室管理</span></el-menu-item>
        <el-menu-item v-if="userStore.isAdmin()" index="/admin/reservations"><el-icon><Calendar /></el-icon><span>预约管理</span></el-menu-item>
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
      <el-main class="layout-main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, OfficeBuilding, ArrowDown, User, Calendar } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute(); const router = useRouter(); const userStore = useUserStore()
function handleLogout() { userStore.logout(); router.push('/login') }
</script>

<style scoped>
.layout-container { height: 100vh; }
.layout-aside { background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%); display: flex; flex-direction: column; }
.logo { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 20px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.logo-icon { width: 32px; height: 32px; background: var(--primary-gradient); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; }
.logo-text { font-size: 15px; font-weight: 600; color: #fff; }
.side-menu { border-right: none; padding: 8px 0; }
.side-menu .el-menu-item { border-radius: 8px; margin: 2px 10px; height: 40px; line-height: 40px; font-size: 13px; }
.side-menu .el-menu-item:hover { background: rgba(102, 126, 234, 0.12) !important; }
.side-menu .el-menu-item.is-active { background: rgba(102, 126, 234, 0.2) !important; color: #fff !important; font-weight: 500; }
.layout-header { height: 56px; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; background: #fff; border-bottom: 1px solid var(--border); }
.greeting { font-size: 14px; color: var(--text-secondary); }
.header-right { display: flex; align-items: center; gap: 12px; }
.avatar-btn { display: flex; align-items: center; gap: 6px; cursor: pointer; padding: 4px 8px; border-radius: 8px; }
.avatar-btn:hover { background: #f8fafc; }
.avatar { width: 30px; height: 30px; border-radius: 50%; background: var(--primary-gradient); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; }
.arrow { font-size: 11px; color: var(--text-muted); }
.layout-main { background: var(--bg-page); padding: 20px; overflow-y: auto; }
</style>
