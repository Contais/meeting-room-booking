import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { logout as logoutApi } from '@/api/auth'
import { getCurrentUser } from '@/api/user'
import type { UserInfo } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<UserInfo | null>(null)

  function setUserToken(val: string) {
    token.value = val
    setToken(val)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  function isAdmin(): boolean {
    return userInfo.value?.role === 'admin'
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const res = await getCurrentUser()
      if (res.data) {
        userInfo.value = res.data
      }
    } catch {
      // token invalid, will be handled by interceptor
    }
  }

  async function logout() {
    try {
      if (token.value) {
        await logoutApi()
      }
    } catch { /* */ }
    token.value = ''
    userInfo.value = null
    removeToken()
  }

  return { token, userInfo, setUserToken, setUserInfo, isAdmin, fetchUserInfo, logout }
})
