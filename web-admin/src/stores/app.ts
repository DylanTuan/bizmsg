import { defineStore } from 'pinia'
import { ref } from 'vue'

import { STORAGE_KEYS, getStorage, setStorage } from '@/utils/storage'

/** 应用级 UI 状态：目前只有侧边栏折叠，刷新后保持用户习惯。 */
export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(getStorage<boolean>(STORAGE_KEYS.sidebarCollapsed, false))

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
    setStorage(STORAGE_KEYS.sidebarCollapsed, sidebarCollapsed.value)
  }

  function setSidebarCollapsed(collapsed: boolean): void {
    sidebarCollapsed.value = collapsed
    setStorage(STORAGE_KEYS.sidebarCollapsed, collapsed)
  }

  return { sidebarCollapsed, toggleSidebar, setSidebarCollapsed }
})
