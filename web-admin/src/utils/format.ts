/** 生成 yyyyMMddHHmmss 形式的时间戳，用于导出文件名。 */
export function timestampForFilename(date = new Date()): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return (
    `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}` +
    `${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
  )
}

/** 健康检查等场景只需要「状态 -> 颜色」，统一收敛在这里。 */
export function healthTagType(status: string): 'success' | 'danger' | 'warning' | 'info' {
  switch (status?.toUpperCase()) {
    case 'UP':
      return 'success'
    case 'DOWN':
      return 'danger'
    case 'OUT_OF_SERVICE':
    case 'UNKNOWN':
      return 'warning'
    default:
      return 'info'
  }
}
