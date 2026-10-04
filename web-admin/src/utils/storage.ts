/**
 * 本地存储工具：统一加前缀，避免同域下与其他应用串键；
 * 读取时做 JSON 反序列化兜底，防止手改 localStorage 导致页面白屏。
 */
const PREFIX = 'bizmsg:'

export const STORAGE_KEYS = {
  token: 'token',
  userInfo: 'userInfo',
  sidebarCollapsed: 'sidebarCollapsed',
  reportHistory: 'reportHistory'
} as const

export function getStorage<T>(key: string, fallback: T): T {
  const raw = window.localStorage.getItem(PREFIX + key)
  if (!raw) {
    return fallback
  }
  try {
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

export function setStorage(key: string, value: unknown): void {
  window.localStorage.setItem(PREFIX + key, JSON.stringify(value))
}

export function removeStorage(key: string): void {
  window.localStorage.removeItem(PREFIX + key)
}
