import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { listConfigs } from '../api/forecastConfig'
import './SalesForecastFillDeadlineBanner.css'

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

function startOfLocalDay(d) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

/** month: YYYYMM → { y, m } 1-12 */
function parseConfigMonth(monthStr) {
  if (!monthStr || String(monthStr).length !== 6) return null
  const s = String(monthStr)
  const y = parseInt(s.slice(0, 4), 10)
  const m = parseInt(s.slice(4, 6), 10)
  if (!Number.isFinite(y) || !Number.isFinite(m) || m < 1 || m > 12) return null
  return { y, m }
}

/** 該月最後一日與 autoCloseDay 取合理日 */
function autoCloseLocalDate(y, month1to12, autoCloseDay) {
  const dayNum = Number(autoCloseDay)
  if (!Number.isFinite(dayNum) || dayNum < 1) return null
  const lastDay = new Date(y, month1to12, 0).getDate()
  const day = Math.min(Math.floor(dayNum), lastDay)
  return new Date(y, month1to12 - 1, day)
}

function formatYm(y, m) {
  return `${y}-${String(m).padStart(2, '0')}`
}

function formatYmd(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 今日 0 點與截止日 0 點相差天數；截止日當天為 0 */
function calendarDaysUntil(todayStart, deadlineStart) {
  return Math.round((deadlineStart.getTime() - todayStart.getTime()) / 86400000)
}

function buildAlerts(configs) {
  const today = startOfLocalDay(new Date())
  const out = []
  for (const c of configs) {
    const closed = Boolean(c.isClosed ?? c.is_closed)
    if (closed) continue
    const ym = parseConfigMonth(c.month)
    if (!ym) continue
    const autoDay = c.autoCloseDay ?? c.auto_close_day
    const deadline = autoCloseLocalDate(ym.y, ym.m, autoDay)
    if (!deadline) continue
    const deadlineStart = startOfLocalDay(deadline)
    const diff = calendarDaysUntil(today, deadlineStart)
    if (diff >= 0 && diff <= 7) {
      out.push({
        monthLabel: formatYm(ym.y, ym.m),
        deadlineLabel: formatYmd(deadlineStart),
      })
    }
  }
  return out
}

export default function SalesForecastFillDeadlineBanner() {
  const { user } = useAuth()
  const [alerts, setAlerts] = useState([])

  useEffect(() => {
    if (!user || !hasPermission(user, 'sales_forecast.upload')) {
      setAlerts([])
      return
    }
    let cancelled = false
    listConfigs()
      .then((data) => {
        if (cancelled) return
        const list = Array.isArray(data) ? data : []
        setAlerts(buildAlerts(list))
      })
      .catch(() => {
        if (!cancelled) setAlerts([])
      })
    return () => {
      cancelled = true
    }
  }, [user])

  if (!alerts.length) return null

  return (
    <div className="sales-fill-deadline-banner" role="status">
      {alerts.map((a) => (
        <p key={`${a.monthLabel}-${a.deadlineLabel}`} className="sales-fill-deadline-banner__line">
          開放填寫月份 <strong>{a.monthLabel}</strong> 表單填寫期限至 <strong>{a.deadlineLabel}</strong> 為止，若需要延展，請於到期前聯繫生管同仁。
        </p>
      ))}
    </div>
  )
}
