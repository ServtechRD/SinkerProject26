import { useSyncExternalStore } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
  subscribeWeeklyScheduleUploadNotice,
  getWeeklyScheduleUploadNoticeState,
  hideWeeklyScheduleUploadNotice,
} from '../state/weeklyScheduleUploadNoticeStore'
import { formatMaterialDemandSavedAt } from '../utils/materialDemandDateTime'
import './WeeklyScheduleUploadBanner.css'

function subscribe(callback) {
  return subscribeWeeklyScheduleUploadNotice(callback)
}

function getSnapshot() {
  return getWeeklyScheduleUploadNoticeState()
}

function getServerSnapshot() {
  return {
    visible: false,
    weekStart: '',
    factory: '',
    savedAt: null,
  }
}

/**
 * 生產週排程：檔案上傳成功後由週排程頁呼叫 showWeeklyScheduleUploadNotice。
 * 掛在 MainLayout；連結導向週排程頁錨點。
 */
export default function WeeklyScheduleUploadBanner() {
  const location = useLocation()
  const notice = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)

  if (!notice.visible || !notice.weekStart || !notice.factory) {
    return null
  }

  const savedLabel =
    notice.savedAt != null ? formatMaterialDemandSavedAt(notice.savedAt) : null
  const linkText = `${notice.weekStart} ${notice.factory}`
  const weeklyPath = '/weekly-schedule'
  const hashLink = `${weeklyPath}#weekly-schedule-upload-anchor`
  const onWeeklySchedulePage = location.pathname === weeklyPath

  return (
    <div className="weekly-schedule-upload-banner" role="status" aria-live="polite">
      <span className="weekly-schedule-upload-banner__title">生產週排程表單 有變動</span>
      <span className="weekly-schedule-upload-banner__row">
        <Link
          to={hashLink}
          className="weekly-schedule-upload-banner__link"
          onClick={(e) => {
            if (!onWeeklySchedulePage) return
            e.preventDefault()
            document.getElementById('weekly-schedule-upload-anchor')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
          }}
        >
          {linkText}
        </Link>
        {savedLabel && (
          <span className="weekly-schedule-upload-banner__saved-at">
            （最後編輯儲存：{savedLabel}）
          </span>
        )}
      </span>
      <button
        type="button"
        className="weekly-schedule-upload-banner__dismiss"
        onClick={() => hideWeeklyScheduleUploadNotice()}
        aria-label="關閉通知"
      >
        ×
      </button>
    </div>
  )
}
