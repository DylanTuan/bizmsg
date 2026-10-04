/** 报文类型候选值，与后端 ReportMessage.messageType 对应；默认 ACCEPT（受理）。 */
export const MESSAGE_TYPE_OPTIONS = [
  { label: 'ACCEPT（受理）', value: 'ACCEPT' },
  { label: 'RECEIPT（回执）', value: 'RECEIPT' },
  { label: 'CANCEL（撤销）', value: 'CANCEL' }
] as const

export const DEFAULT_MESSAGE_TYPE = 'ACCEPT'

/** 顶部进度条：关掉自带的旋转圈，避免与按钮 loading 视觉打架。 */
export const NPROGRESS_OPTIONS = {
  showSpinner: false,
  minimum: 0.15
} as const

export const APP_TITLE = import.meta.env.VITE_APP_TITLE || '报文管理平台'
