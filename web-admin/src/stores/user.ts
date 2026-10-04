import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { fetchUserInfo, login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest, UserInfo } from '@/types/api'
import { STORAGE_KEYS, getStorage, removeStorage, setStorage } from '@/utils/storage'

/** 登录态：token 与用户信息同步落 localStorage，刷新页面不掉登录。 */
export const useUserStore = defineStore('user', () => {
  const token = ref(getStorage<string>(STORAGE_KEYS.token, ''))
  const userInfo = ref<UserInfo | null>(getStorage<UserInfo | null>(STORAGE_KEYS.userInfo, null))

  const isLoggedIn = computed(() => Boolean(token.value))
  const nickname = computed(() => userInfo.value?.nickname ?? '未登录')

  async function login(payload: LoginRequest): Promise<UserInfo> {
    const result = await loginApi(payload)
    token.value = result.token
    userInfo.value = result.userInfo
    setStorage(STORAGE_KEYS.token, result.token)
    setStorage(STORAGE_KEYS.userInfo, result.userInfo)
    return result.userInfo
  }

  /** 有 token 但本地没有用户信息（例如手工清理过缓存）时补拉一次。 */
  async function loadUserInfo(): Promise<void> {
    if (!token.value || userInfo.value) {
      return
    }
    userInfo.value = await fetchUserInfo()
    setStorage(STORAGE_KEYS.userInfo, userInfo.value)
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      userInfo.value = null
      removeStorage(STORAGE_KEYS.token)
      removeStorage(STORAGE_KEYS.userInfo)
    }
  }

  return { token, userInfo, isLoggedIn, nickname, login, loadUserInfo, logout }
})
