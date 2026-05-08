import { useState, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { getMaterialDemandPendingConfirm } from '../api/materialDemand'
import { formatMaterialDemandSavedAt } from '../utils/materialDemandDateTime'
import './MaterialDemandPendingBanner.css'

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

/** 單筆：週次+廠連結與最後編輯時間（與原「有變動」多項並列相同結構） */
function renderLinkItem(item, keySuffix) {
  const ws = item.weekStart != null ? String(item.weekStart) : ''
  const fac = item.factory != null ? String(item.factory) : ''
  if (!ws || !fac) return null
  const to = `/material-demand/form?week_start=${encodeURIComponent(ws)}&factory=${encodeURIComponent(fac)}`
  const savedLabel = formatMaterialDemandSavedAt(item.updatedAt ?? item.updated_at)
  return (
    <span key={`${ws}-${fac}-${keySuffix}`} className="material-demand-pending-banner__item material-demand-pending-banner__item--inline-tight">
      <Link to={to} className="material-demand-pending-banner__link">
        {ws} {fac}
      </Link>
      {savedLabel && (
        <span className="material-demand-pending-banner__saved-at">
          （最後編輯儲存：{savedLabel}）
        </span>
      )}
    </span>
  )
}

/** 同一狀態一條橫幅，右側多筆橫向排列 */
function StatusBannerBlock({ statusLabel, items, keyPrefix }) {
  if (!items?.length) return null
  const cells = items.map((item, i) => renderLinkItem(item, `${keyPrefix}-${i}`)).filter(Boolean)
  if (!cells.length) return null
  return (
    <div className="material-demand-pending-banner" role="alert">
      <span className="material-demand-pending-banner__text">
        物料需求數量表單 {statusLabel}：
      </span>
      <span className="material-demand-pending-banner__links">{cells}</span>
    </div>
  )
}

export default function MaterialDemandPendingBanner() {
  const { user } = useAuth()
  const [pending, setPending] = useState([])

  const procurementMode = user?.roleCode === 'procurement'
  const shouldFetch =
    procurementMode || hasPermission(user, 'confirm_data_send_erp')

  const { approved, rejected } = useMemo(() => {
    const a = []
    const r = []
    for (const item of pending) {
      const st = item.status ?? item.reviewStatus
      if (st === 1) a.push(item)
      else if (st === 2) r.push(item)
    }
    return { approved: a, rejected: r }
  }, [pending])

  useEffect(() => {
    if (!shouldFetch) return
    getMaterialDemandPendingConfirm()
      .then((list) => setPending(Array.isArray(list) ? list : []))
      .catch(() => setPending([]))
  }, [user, shouldFetch])

  if (!pending.length) return null

  if (procurementMode) {
    if (!approved.length && !rejected.length) return null
    return (
      <>
        <StatusBannerBlock statusLabel="審核完成" items={approved} keyPrefix="a" />
        <StatusBannerBlock statusLabel="退回" items={rejected} keyPrefix="r" />
      </>
    )
  }

  return <StatusBannerBlock statusLabel="待審核" items={pending} keyPrefix="s" />
}
