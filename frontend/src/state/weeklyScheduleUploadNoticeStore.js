/** 僅供「生產週排程」上傳成功後顯示頂部通知用，與其他頁面狀態隔離 */

let state = {
  visible: false,
  weekStart: '',
  factory: '',
  /** @type {Date | string | null} */
  savedAt: null,
}

const listeners = new Set()

export function getWeeklyScheduleUploadNoticeState() {
  return state
}

function emit() {
  listeners.forEach((fn) => {
    try {
      fn(state)
    } catch {
      /* ignore */
    }
  })
}

export function subscribeWeeklyScheduleUploadNotice(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function showWeeklyScheduleUploadNotice({ weekStart, factory, savedAt = new Date() }) {
  state = {
    visible: true,
    weekStart: weekStart != null ? String(weekStart) : '',
    factory: factory != null ? String(factory) : '',
    savedAt,
  }
  emit()
}

export function hideWeeklyScheduleUploadNotice() {
  state = { ...state, visible: false }
  emit()
}
