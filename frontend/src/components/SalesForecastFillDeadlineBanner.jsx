import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { listConfigs } from '../api/forecastConfig'
import { formatLocalDateTime } from '../utils/dateTime'
import './SalesForecastFillDeadlineBanner.css'

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

function startOfLocalDay(d) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

/** month: YYYYMM → YYYY-MM */
function formatYm(monthStr) {
  const s = String(monthStr ?? '')
  return /^\d{6}$/.test(s) ? `${s.slice(0, 4)}-${s.slice(4, 6)}` : s
}

/** 開放中且 7 天內截止的月份 */
function buildAlerts(configs) {
  const today = startOfLocalDay(new Date())
  const out = []
  for (const c of configs) {
    if (c.isClosed || !c.autoCloseAt) continue
    const deadline = new Date(`${String(c.autoCloseAt).slice(0, 19)}+08:00`)
    if (isNaN(deadline.getTime())) continue
    const diff = Math.round((startOfLocalDay(deadline).getTime() - today.getTime()) / 86400000)
    if (diff >= 0 && diff <= 7) {
      out.push({ monthLabel: formatYm(c.month), deadlineLabel: formatLocalDateTime(c.autoCloseAt) })
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
        if (!cancelled) setAlerts(buildAlerts(Array.isArray(data) ? data : []))
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
          開放填寫月份 <strong>{a.monthLabel}</strong> 表單填寫期限至 <strong>{a.deadlineLabel}</strong> 為止，截止後將無法上傳，若需延展請於到期前聯繫生管同仁。
        </p>
      ))}
    </div>
  )
}
