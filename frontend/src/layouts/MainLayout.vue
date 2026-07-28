<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo" :class="{ 'logo-collapsed': isCollapsed }">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="28" height="28">
            <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
        </div>
        <h2 v-if="!isCollapsed">会议室预约</h2>
      </div>
      <el-menu
        :default-active="route.path"
        router
        :collapse="isCollapsed"
        background-color="transparent"
        text-color="rgba(255,255,255,0.7)"
        active-text-color="#ffffff"
        class="side-menu"
      >
        <template v-for="item in menuItems" :key="item.id">
          <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path || String(item.id)">
            <template #title><el-icon><component :is="iconComponents[item.icon || 'Document']" /></el-icon><span>{{ item.name }}</span></template>
            <el-menu-item v-for="child in item.children" :key="child.id" :index="child.path">
              <el-icon><component :is="iconComponents[child.icon || 'Document']" /></el-icon>
              <span>{{ child.name }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="iconComponents[item.icon || 'Document']" /></el-icon>
            <span>{{ item.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button class="collapse-btn" @click="toggleCollapse">
            <el-icon><component :is="isCollapsed ? 'Expand' : 'Fold'" /></el-icon>
          </el-button>
          <h2 class="page-title">
            <span v-if="parentMeta" class="parent-link" @click="goParent">{{ parentMeta.title }}</span>
            <span v-if="parentMeta" class="title-separator">/</span>
            <span class="current-title">{{ currentTitle }}</span>
          </h2>
        </div>
        <div class="header-right">
          <div class="header-action-group">
            <NotificationBell />
            <el-tooltip content="AI 助手" placement="bottom">
              <el-button class="icon-btn chat-btn" @click="toggleChat">
                <el-icon><ChatDotRound /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="themeStore.isDark ? '切换到亮色模式' : '切换到暗色模式'" placement="bottom">
              <el-button class="icon-btn" @click="themeStore.toggle">
                <el-icon><component :is="themeStore.isDark ? 'Sunny' : 'Moon'" /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
          <el-dropdown trigger="hover" popper-class="user-dropdown-popper" placement="bottom-end">
            <div class="avatar-btn">
              <div class="avatar" :style="getAvatarStyle()">
                <template v-if="avatarIcon">
                  <el-icon :size="18"><component :is="avatarIcon" /></el-icon>
                </template>
                <template v-else>
                  {{ (userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
                </template>
              </div>
              <div v-if="!isCollapsed" class="user-info-brief">
                <span class="user-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</span>
                <span class="user-role-tag" v-if="userStore.isAdmin()">超级管理员</span>
              </div>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <div class="user-dropdown-panel">
                <div class="dropdown-header">
                  <div class="dropdown-avatar" :style="getAvatarStyle()">
                    <template v-if="avatarIcon">
                      <el-icon :size="22"><component :is="avatarIcon" /></el-icon>
                    </template>
                    <template v-else>
                      {{ (userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
                    </template>
                  </div>
                  <div class="dropdown-user-info">
                    <div class="dropdown-username">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</div>
                    <div class="dropdown-account">@{{ userStore.userInfo?.username || 'unknown' }}</div>
                  </div>
                  <span class="dropdown-role" v-if="userStore.isAdmin()">超级管理员</span>
                </div>
                <div class="dropdown-divider"></div>
                <div class="dropdown-menu-list">
                  <button type="button" class="dropdown-menu-item" @click="router.push('/profile')">
                    <el-icon class="menu-icon"><User /></el-icon>
                    <span class="menu-text">个人中心</span>
                    <el-icon class="menu-arrow"><Right /></el-icon>
                  </button>
                  <button type="button" class="dropdown-menu-item danger" @click="handleLogout">
                    <el-icon class="menu-icon"><SwitchButton /></el-icon>
                    <span class="menu-text">退出登录</span>
                    <el-icon class="menu-arrow"><Right /></el-icon>
                  </button>
                </div>
              </div>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
  <ChatPanel ref="chatPanelRef" />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, ChatDotRound, User, SwitchButton, Right } from '@element-plus/icons-vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useNotificationStore } from '@/stores/notification'
import { getMyMenus } from '@/api/menu'
import ChatPanel from '@/components/ChatPanel.vue'
import NotificationBell from '@/components/NotificationBell.vue'
import type { MenuItem } from '@/types/menu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const notificationStore = useNotificationStore()
const menuItems = ref<MenuItem[]>([])
const isCollapsed = ref(false)
const chatPanelRef = ref<InstanceType<typeof ChatPanel> | null>(null)

// 注册所有 Element Plus 图标供动态菜单使用
const icons = ElementPlusIconsVue
const iconComponents = Object.fromEntries(
  Object.entries(icons).map(([key, component]) => [key, component])
)

// 当前页面标题
const currentTitle = computed(() => {
  return (route.meta.title as string) || ''
})

// 父级面包屑（详情页可点击跳转父级列表）
const parentMeta = computed(() => (route.meta.parent as { path: string; title: string } | undefined))
function goParent() {
  if (parentMeta.value) router.push(parentMeta.value.path)
}

// 头像渐变色
const avatarGradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
  'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
  'linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%)',
  'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',
  'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)',
  'linear-gradient(135deg, #fddb92 0%, #d1fdff 100%)',
]

const avatarData = computed(() => {
  const avatar = userStore.userInfo?.avatar
  if (!avatar) return { icon: '', gradient: 0 }
  try {
    const data = JSON.parse(avatar)
    return { icon: data.icon || '', gradient: data.gradient ?? 0 }
  } catch {
    return { icon: '', gradient: 0 }
  }
})

const avatarIcon = computed(() => {
  const iconName = avatarData.value.icon
  if (!iconName) return null
  return iconComponents[iconName] || null
})

function getAvatarStyle(): Record<string, string> {
  const gradient = avatarGradients[avatarData.value.gradient] || avatarGradients[0]
  return {
    background: gradient,
    color: '#fff',
  }
}

// 切换侧边栏折叠
function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

// 切换聊天面板
function toggleChat() {
  chatPanelRef.value?.togglePanel()
}

async function loadMenus() {
  try {
    const res = await getMyMenus()
    menuItems.value = res.data
  } catch { /* */ }
}

function handleLogout() {
  notificationStore.stop()
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadMenus()
  notificationStore.start()
})

onUnmounted(() => {
  notificationStore.stop()
})

// WebSocket 实时通知到达时弹出 toast
watch(() => notificationStore.latestNotification, (n) => {
  if (n) {
    ElMessage({ message: n.title, type: 'info', duration: 4000, showClose: true })
  }
})
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
  transition: width 0.3s ease;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
}

.logo-collapsed {
  justify-content: center;
  padding: 0;
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
  flex-shrink: 0;
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

.side-menu > .el-menu-item,
.side-menu > .el-sub-menu :deep(.el-sub-menu__title) {
  border-radius: 8px;
  margin: 4px 12px;
  height: 44px;
  line-height: 44px;
  transition: all 0.2s;
}

/* 折叠模式：取消边距，图标居中 */
.side-menu.el-menu--collapse > .el-menu-item,
.side-menu.el-menu--collapse > .el-sub-menu :deep(.el-sub-menu__title) {
  margin: 4px auto;
  padding: 0 !important;
  width: 44px;
  min-width: 44px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.side-menu.el-menu--collapse > .el-menu-item .el-icon,
.side-menu.el-menu--collapse > .el-sub-menu :deep(.el-sub-menu__title .el-icon) {
  margin: 0 !important;
  font-size: 18px;
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
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.2s;
}

.collapse-btn:hover {
  background: var(--border-light);
  color: var(--primary);
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.parent-link {
  color: var(--text-secondary, #909399);
  font-weight: 400;
  cursor: pointer;
  transition: color 0.2s;
}

.parent-link:hover {
  color: var(--primary, #409eff);
}

.title-separator {
  color: var(--text-secondary, #c0c4cc);
  font-weight: 400;
}

.current-title {
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-action-group {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 2px;
  background: var(--bg-page);
  border-radius: 10px;
}

.header-action-group .icon-btn {
  width: 32px;
  height: 32px;
}

.header-action-group .icon-btn .el-icon {
  font-size: 18px;
}

.header-action-group .chat-btn::after {
  top: 4px;
  right: 4px;
  width: 7px;
  height: 7px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--border-light);
  color: var(--primary);
}

/* 聊天入口：在线状态圆点 */
.chat-btn {
  position: relative;
}

.chat-btn::after {
  content: '';
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--success);
  border: 2px solid var(--bg-card);
  animation: online-pulse 2s ease-in-out infinite;
}

@keyframes online-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.5); }
  50% { box-shadow: 0 0 0 4px rgba(16, 185, 129, 0); }
}

.avatar-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px 4px 4px;
  border-radius: 20px;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.avatar-btn:hover {
  background: var(--bg-card);
  border-color: var(--border-light);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.user-info-brief {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: #fff;
  font-weight: 500;
}

.arrow {
  font-size: 12px;
  color: var(--text-muted);
  transition: transform 0.2s;
}

.avatar-btn:hover .arrow {
  transform: rotate(180deg);
}

/* 用户下拉面板样式（通过 popper-class 全局生效） */
:global(.user-dropdown-popper) {
  width: 260px !important;
  padding: 0 !important;
  border: 1px solid var(--border-light) !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
  background: var(--bg-card) !important;
  overflow: hidden;
}

:global(.user-dropdown-popper .user-dropdown-panel) {
  width: 100%;
}

:global(.user-dropdown-popper .dropdown-header) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, var(--primary-light, #f5f7ff), transparent);
}

:global(.user-dropdown-popper .dropdown-avatar) {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

:global(.user-dropdown-popper .dropdown-user-info) {
  flex: 1;
  min-width: 0;
}

:global(.user-dropdown-popper .dropdown-username) {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.user-dropdown-popper .dropdown-account) {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

:global(.user-dropdown-popper .dropdown-role) {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: #fff;
  font-weight: 500;
  flex-shrink: 0;
}

:global(.user-dropdown-popper .dropdown-divider) {
  height: 1px;
  background: var(--border-light);
  margin: 0;
}

:global(.user-dropdown-popper .dropdown-menu-list) {
  padding: 6px;
}

:global(.user-dropdown-popper .dropdown-menu-item) {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-primary);
  transition: all 0.15s;
  text-align: left;
}

:global(.user-dropdown-popper .dropdown-menu-item:hover) {
  background: var(--primary-light, #f5f7ff);
  color: var(--primary);
}

:global(.user-dropdown-popper .dropdown-menu-item.danger:hover) {
  background: #fef0f0;
  color: #f56c6c;
}

:global(.user-dropdown-popper .dropdown-menu-item .menu-icon) {
  font-size: 16px;
  flex-shrink: 0;
}

:global(.user-dropdown-popper .dropdown-menu-item .menu-text) {
  flex: 1;
  font-weight: 500;
}

:global(.user-dropdown-popper .dropdown-menu-item .menu-arrow) {
  font-size: 12px;
  color: var(--text-muted);
  opacity: 0;
  transition: opacity 0.15s;
}

:global(.user-dropdown-popper .dropdown-menu-item:hover .menu-arrow) {
  opacity: 1;
}

.layout-main {
  background: var(--bg-page);
  padding: 20px;
  overflow-y: auto;
}

/* 菜单折叠模式：确保折叠时图标居中 */
.side-menu:not(.el-menu--collapse) {
  width: 100%;
}
.side-menu.el-menu--collapse {
  width: 64px;
}

/* 一级菜单项：统一左侧对齐 + 边距 */
.side-menu > .el-menu-item {
  padding-left: 20px !important;
}

/* 一级 sub-menu 标题：与一级 menu-item 对齐 */
.side-menu > .el-sub-menu :deep(.el-sub-menu__title) {
  padding-left: 20px !important;
}

/* 二级菜单项：比一级多缩进 */
.side-menu :deep(.el-sub-menu .el-menu-item) {
  padding-left: 48px !important;
}
</style>
