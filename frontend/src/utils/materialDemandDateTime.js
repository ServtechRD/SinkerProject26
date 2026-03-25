/** 物料需求：後端 LocalDateTime 序列化字串轉台灣時間顯示 */
export function formatMaterialDemandSavedAt(isoLike) {
  if (isoLike == null || isoLike === '') return null
  let s = String(isoLike).trim()
  if (Array.isArray(isoLike) && isoLike.length >= 6) {
    const [y, m, d, h = 0, min = 0, sec = 0] = isoLike
    s = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}T${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
  }
  const normalized = s.includes('T') ? s : s.replace(' ', 'T')
  const d = new Date(normalized)
  if (Number.isNaN(d.getTime())) return String(isoLike)
  return d.toLocaleString('zh-TW', {
    timeZone: 'Asia/Taipei',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}
