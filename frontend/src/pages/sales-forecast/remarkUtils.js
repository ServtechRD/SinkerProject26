/** 備註來源標示，如 [全聯/12月]；month 為 YYYYMM */
export function remarkLabel(r) {
  return `[${r.channel}/${Number(String(r.month ?? '').slice(4))}月]`
}

/** 多則備註串成匯出用文字（每則一行），無備註回 '-' */
export function remarksToText(remarks) {
  const lines = (remarks ?? [])
    .filter((r) => (r.text ?? '').trim())
    .map((r) => `${remarkLabel(r)} ${r.text.trim()}`)
  return lines.length ? lines.join('\n') : '-'
}

/** CSV 欄位跳脫：含逗號、引號或換行時以雙引號包住 */
export function csvCell(v) {
  const s = String(v ?? '')
  return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
}

/** 下載 CSV（含 BOM，Excel 開啟中文不亂碼）；rows 第一列為表頭 */
export function downloadCsv(filename, rows) {
  const csv = '﻿' + rows.map((r) => r.map(csvCell).join(',')).join('\r\n')
  const url = window.URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
