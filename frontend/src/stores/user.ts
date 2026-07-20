import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { logout as logoutApi } from '@/api/auth'
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

  async function logout() {
    try {
      if (token.value) {
        await logoutApi()
      }
    } catch {
      // ignore
    }
    token.value = ''
    userInfo.value = null
    removeToken()
  }

  return { token, userInfo, setUserToken, setUserInfo, isAdmin, logout }
})
