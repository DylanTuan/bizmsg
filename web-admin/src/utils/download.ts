/** 把文本（XML 报文）保存为本地文件，供人工核对或留档。 */
export function downloadTextFile(filename: string, content: string, mime = 'application/xml;charset=utf-8'): void {
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  // 释放 Blob 占用的内存，避免连续导出后堆积
  URL.revokeObjectURL(url)
}
