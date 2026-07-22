import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/HomeView.vue'),
        meta: { title: '首页', requiresAuth: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
      {
        path: 'meeting/rooms',
        name: 'MeetingRooms',
        component: () => import('@/views/meeting/RoomListView.vue'),
        meta: { title: '会议室列表', requiresAuth: true },
      },
      {
        path: 'meeting/rooms/:id',
        name: 'MeetingRoomDetail',
        component: () => import('@/views/meeting/RoomDetailView.vue'),
        meta: { title: '会议室详情', requiresAuth: true },
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'admin/rooms',
        name: 'AdminRooms',
        component: () => import('@/views/admin/RoomManage.vue'),
        meta: { title: '会议室管理', requiresAuth: true, requiresAdmin: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  NProgress.start()
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin()) {
    ElMessage.error('无权访问')
    next('/home')
    return
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
