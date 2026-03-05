import { useState, useEffect, useCallback } from 'react'
import { listConfigs } from '../../api/forecastConfig'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import CreateMonthsDialog from './CreateMonthsDialog'
import EditConfigDialog from './EditConfigDialog'
import './ForecastConfig.css'

const PAGE_SIZE = 10

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

function formatClosedAt(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export default function ForecastConfigPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(true)
  const [accessDenied, setAccessDenied] = useState(false)
  const [page, setPage] = useState(1)

  const [showCreate, setShowCreate] = useState(false)
  const [editTarget, setEditTarget] = useState(null)

  const canView = hasPermission(user, 'sales_forecast_config.view')
  const canEdit = hasPermission(user, 'sales_forecast_config.edit')

  const totalPages = Math.max(1, Math.ceil(configs.length / PAGE_SIZE))
  const startIndex = (page - 1) * PAGE_SIZE
  const pageConfigs = configs.slice(startIndex, startIndex + PAGE_SIZE)

  const fetchConfigs = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listConfigs()
      const sorted = [...data].sort((a, b) => (b.month > a.month ? 1 : b.month < a.month ? -1 : 0))
      setConfigs(sorted)
      setAccessDenied(false)
    } catch (err) {
      if (err.response?.status === 403) {
        setAccessDenied(true)
      } else {
        toast.error('無法載入設定資料')
      }
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    fetchConfigs()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (page > totalPages && totalPages >= 1) setPage(totalPages)
  }, [configs.length, totalPages, page])

  if (accessDenied || (!loading && !canView)) {
    return (
      <div className="forecast-page">
        <h1>銷售預估量-表單設定</h1>
        <div className="forecast-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="forecast-page">
      <div className="forecast-page-header">
        <h1>銷售預估量-表單設定</h1>
        {canEdit && (
          <button
            className="btn btn--primary"
            onClick={() => setShowCreate(true)}
          >
            新增填寫月份
          </button>
        )}
      </div>

      {loading ? (
        <div className="forecast-loading" role="status">載入中...</div>
      ) : configs.length === 0 ? (
        <div className="forecast-empty">
          尚無設定資料，請建立第一個月份。
        </div>
      ) : (
        <>
          <div className="forecast-table-wrap">
            <table className="forecast-table">
            <thead>
              <tr>
                <th>開放填寫月份</th>
                <th>自動結束新增設定日期</th>
                <th>結束新增設定</th>
                <th>最後編輯時間</th>
                {canEdit && <th className="actions-cell">編輯</th>}
              </tr>
            </thead>
            <tbody>
              {pageConfigs.map((c) => (
                <tr key={c.id}>
                  <td>{c.month}</td>
                  <td>{c.autoCloseDay}</td>
                  <td>
                    <span className={c.isClosed ? 'badge--closed' : 'badge--open'}>
                      {c.isClosed ? '已結束新增設定' : '開放中'}
                    </span>
                  </td>
                  <td>{formatClosedAt(c.closedAt)}</td>
                  {canEdit && (
                    <td className="actions-cell">
                      <button
                        className="btn btn--small btn--outline"
                        onClick={() => setEditTarget(c)}
                      >
                        編輯
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {configs.length > PAGE_SIZE && (
          <div className="forecast-pagination">
            <span className="forecast-pagination-info">
              第 {startIndex + 1}–{Math.min(startIndex + PAGE_SIZE, configs.length)} 筆，共 {configs.length} 筆
            </span>
            <div className="forecast-pagination-buttons">
              <button
                type="button"
                className="btn btn--small btn--outline"
                disabled={page <= 1}
                onClick={() => setPage((p) => Math.max(1, p - 1))}
              >
                上一頁
              </button>
              <span className="forecast-pagination-page">
                第 {page} / {totalPages} 頁
              </span>
              <button
                type="button"
                className="btn btn--small btn--outline"
                disabled={page >= totalPages}
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              >
                下一頁
              </button>
            </div>
          </div>
        )}
        </>
      )}

      <CreateMonthsDialog
        open={showCreate}
        onClose={() => setShowCreate(false)}
        onSuccess={() => {
          setShowCreate(false)
          fetchConfigs()
        }}
      />

      <EditConfigDialog
        open={!!editTarget}
        config={editTarget}
        onClose={() => setEditTarget(null)}
        onSuccess={() => {
          setEditTarget(null)
          fetchConfigs()
        }}
      />
    </div>
  )
}
