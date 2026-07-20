<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <h2>会议室预约</h2>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/meeting/rooms">
          <el-icon><OfficeBuilding /></el-icon>
          <span>会议室</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin()" index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-right">
          <el-tag v-if="userStore.isAdmin()" type="danger" size="small" style="margin-right: 12px">
            管理员
          </el-tag>
          <el-dropdown>
            <span class="el-dropdown-link">
              {{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
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
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, OfficeBuilding, ArrowDown, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-aside {
  background-color: #304156;
  overflow: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.logo h2 {
  font-size: 18px;
  margin: 0;
}
.layout-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  border-bottom: 1px solid #eee;
  background: #fff;
}
.header-right {
  display: flex;
  align-items: center;
}
.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
.layout-main {
  background-color: #f5f7fa;
}
</style>
