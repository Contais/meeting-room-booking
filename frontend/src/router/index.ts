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
      { path: 'home', name: 'Home', component: () => import('@/views/home/HomeView.vue'), meta: { title: '首页', requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/user/ProfileView.vue'), meta: { title: '个人中心', requiresAuth: true } },
      { path: 'meeting/rooms', name: 'MeetingRooms', component: () => import('@/views/meeting/RoomListView.vue'), meta: { title: '会议室列表', requiresAuth: true } },      { path: 'meeting/rooms/:id', name: 'MeetingRoomDetail', component: () => import('@/views/meeting/RoomDetailView.vue'), meta: { title: '会议室详情', requiresAuth: true, parent: { path: '/meeting/rooms', title: '会议室列表' } } },
      { path: 'schedule', name: 'Schedule', component: () => import('@/views/schedule/ScheduleView.vue'), meta: { title: '日程视图', requiresAuth: true } },
      { path: 'schedule/v2', name: 'ScheduleV2', component: () => import('@/views/schedule/ScheduleViewV2.vue'), meta: { title: '日程视图 v2', requiresAuth: true } },
      { path: 'reservation/my', name: 'MyReservations', component: () => import('@/views/reservation/MyReservations.vue'), meta: { title: '我的预约', requiresAuth: true } },
      { path: 'reservation/my/:id', name: 'MyReservationDetail', component: () => import('@/views/reservation/MyReservationDetail.vue'), meta: { title: '预约详情', requiresAuth: true, parent: { path: '/reservation/my', title: '我的预约' } } },
      { path: 'my-meetings', name: 'MyMeetings', component: () => import('@/views/meeting/MyMeetingsView.vue'), meta: { title: '我的会议', requiresAuth: true } },
      { path: 'contacts', name: 'Contacts', component: () => import('@/views/contacts/ContactsView.vue'), meta: { title: '通讯录', requiresAuth: true } },
      { path: 'notifications', name: 'Notifications', component: () => import('@/views/notification/NotificationView.vue'), meta: { title: '消息通知', requiresAuth: true } },
      { path: 'admin/menus', name: 'AdminMenus', component: () => import('@/views/admin/MenuManage.vue'), meta: { title: '菜单管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/roles', name: 'AdminRoles', component: () => import('@/views/admin/RoleManage.vue'), meta: { title: '角色管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/departments', name: 'AdminDepartments', component: () => import('@/views/admin/DeptManage.vue'), meta: { title: '部门管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/users/:id', name: 'AdminUserDetail', component: () => import('@/views/admin/UserDetail.vue'), meta: { title: '用户详情', requiresAuth: true, requiresAdmin: true, parent: { path: '/admin/users', title: '用户管理' } } },
      { path: 'admin/reservations', name: 'AdminReservations', component: () => import('@/views/admin/ReservationManage.vue'), meta: { title: '预约管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/reservations/:id', name: 'AdminReservationDetail', component: () => import('@/views/reservation/MyReservationDetail.vue'), meta: { title: '预约详情', requiresAuth: true, requiresAdmin: true, parent: { path: '/admin/reservations', title: '预约管理' } } },
      { path: 'admin/rooms', name: 'AdminRooms', component: () => import('@/views/admin/RoomManage.vue'), meta: { title: '会议室管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/rooms/:id', name: 'AdminRoomDetail', component: () => import('@/views/admin/RoomDetail.vue'), meta: { title: '会议室详情', requiresAuth: true, requiresAdmin: true, parent: { path: '/admin/rooms', title: '会议室管理' } } },
      { path: 'admin/equipments', name: 'AdminEquipments', component: () => import('@/views/admin/EquipmentManage.vue'), meta: { title: '设备管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/equipments/:id', name: 'AdminEquipmentDetail', component: () => import('@/views/admin/EquipmentDetail.vue'), meta: { title: '设备详情', requiresAuth: true, requiresAdmin: true, parent: { path: '/admin/equipments', title: '设备管理' } } },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.token) { next({ name: 'Login', query: { redirect: to.fullPath } }); return }
  // 刷新页面后恢复 userInfo
  if (userStore.token && !userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin()) { ElMessage.error('无权访问'); next('/home'); return }
  next()
})

router.afterEach(() => { NProgress.done() })

export default router
