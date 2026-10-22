/**
 * 金额展示统一「¥x.xx」：后端金额一律整数「分」，此处分转元
 * @param {number|string} fen 金额（分）
 * @returns {string} ¥x.xx
 */
export function fen2yuan(fen) {
  if (fen === null || fen === undefined || fen === '') return '¥0.00'
  const n = Number(fen)
  if (Number.isNaN(n)) return '¥0.00'
  return '¥' + (n / 100).toFixed(2)
}

/**
 * 元转分（用于表单提交）
 */
export function yuan2fen(yuan) {
  return Math.round(Number(yuan) * 100)
}

/**
 * 分页响应包装统一：兼容 {total, rows}
 */
export function unwrapPage(page) {
  if (!page) return { rows: [], total: 0 }
  if (Array.isArray(page.rows)) {
    return { rows: page.rows, total: page.total || page.rows.length }
  }
  return { rows: page.content || [], total: page.totalElements || 0 }
}

/**
 * 下载 CSV 文本为文件
 */
export function downloadCsv(text, filename) {
  const blob = new Blob(['\uFEFF' + text], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = window.URL.createObjectURL(blob)
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(link.href)
}
