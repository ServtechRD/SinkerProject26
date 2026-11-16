/** 既有時間欄位（closed_at、上傳時間；後端為 UTC 或無時區）轉台灣時間顯示 */
export function formatTaipeiDateTime(dateStr) {
  if (!dateStr) return '-'
  const utcStr = typeof dateStr === 'string' && !/Z|[+-]\d{2}:?\d{2}$/.test(dateStr) ? dateStr + 'Z' : dateStr
  const d = new Date(utcStr)
  if (isNaN(d.getTime())) return String(dateStr)
  // 不同 ICU 版本會用不間斷空白分隔日期與時間，統一換成一般空白
  return d.toLocaleString('zh-TW', {
    timeZone: 'Asia/Taipei',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).replace(/\s/g, ' ')
}

/** 後端以台灣時間儲存的欄位（截止時間、補登期限、異動紀錄）原樣顯示 */
export function formatLocalDateTime(value) {
  if (!value) return '-'
  const m = String(value).match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/)
  return m ? `${m[1]}/${m[2]}/${m[3]} ${m[4]}:${m[5]}` : String(value)
}

/** 轉成 <input type="datetime-local"> 的值 */
export function toDateTimeLocalValue(value) {
  return value ? String(value).slice(0, 16) : ''
}

/** 台灣時間的到期時間距離 now 還剩多久 */
export function remainingText(expiresAt, now = new Date()) {
  if (!expiresAt) return ''
  const end = new Date(`${String(expiresAt).slice(0, 19)}+08:00`)
  const diff = end.getTime() - now.getTime()
  if (isNaN(diff)) return ''
  if (diff <= 0) return '已到期'
  const totalMin = Math.floor(diff / 60000)
  const d = Math.floor(totalMin / 1440)
  const h = Math.floor((totalMin % 1440) / 60)
  const m = totalMin % 60
  return d > 0 ? `剩餘 ${d} 天 ${h} 小時` : `剩餘 ${h} 小時 ${m} 分`
}
