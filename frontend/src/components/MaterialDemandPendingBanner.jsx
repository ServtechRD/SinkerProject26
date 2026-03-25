import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { getMaterialDemandPendingConfirm } from '../api/materialDemand'
import { formatMaterialDemandSavedAt } from '../utils/materialDemandDateTime'
import './MaterialDemandPendingBanner.css'

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

export default function MaterialDemandPendingBanner() {
  const { user } = useAuth()
  const [pending, setPending] = useState([])

  useEffect(() => {
    if (!hasPermission(user, 'confirm_data_send_erp')) return
    getMaterialDemandPendingConfirm()
      .then((list) => setPending(Array.isArray(list) ? list : []))
      .catch(() => setPending([]))
  }, [user])

  if (!pending.length) return null

  return (
    <div className="material-demand-pending-banner" role="alert">
      <span className="material-demand-pending-banner__text">物料需求數量表單 有變動</span>
      <span className="material-demand-pending-banner__links">
        {pending.map((item, i) => {
          const ws = item.weekStart != null ? String(item.weekStart) : ''
          const fac = item.factory != null ? String(item.factory) : ''
          if (!ws || !fac) return null
          const to = `/material-demand/form?week_start=${encodeURIComponent(ws)}&factory=${encodeURIComponent(fac)}`
          const savedLabel = formatMaterialDemandSavedAt(item.updatedAt ?? item.updated_at)
          return (
            <span key={`${ws}-${fac}-${i}`} className="material-demand-pending-banner__item">
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
        })}
      </span>
    </div>
  )
}
