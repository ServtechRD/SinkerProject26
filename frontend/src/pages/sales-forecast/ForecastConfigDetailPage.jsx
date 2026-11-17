import { useState, useEffect, useCallback } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getFormDetail, getFormLogs, endReopen } from '../../api/forecastConfig'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import { formatTaipeiDateTime, formatLocalDateTime, remainingText } from '../../utils/dateTime'
import ConfirmDialog from '../../components/ConfirmDialog'
import ReopenDialog from './ReopenDialog'
import './ForecastConfig.css'

const ACTION_LABELS = {
  AUTO_CLOSE: '自動截止',
  MANUAL_CLOSE: '手動結束新增',
  REOPEN: '開放補登',
  REOPEN_END: '補登結束',
  CORRECTION: '生管直接修正',
}

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

/** 表單明細：月份狀態、各通路上傳／補登狀態、開放補登、異動紀錄 */
export default function ForecastConfigDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const toast = useToast()
  const [detail, setDetail] = useState(null)
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [accessDenied, setAccessDenied] = useState(false)
  const [reopenOpen, setReopenOpen] = useState(false)
  const [endingId, setEndingId] = useState(null)
  const [endTarget, setEndTarget] = useState(null)

  const canReopen = hasPermission(user, 'sales_forecast_config.reopen')

  const fetchAll = useCallback(async () => {
    setLoading(true)
    try {
      const [d, l] = await Promise.all([getFormDetail(id), getFormLogs(id)])
      setDetail(d)
      setLogs(Array.isArray(l) ? l : [])
    } catch (err) {
      if (err.response?.status === 403) setAccessDenied(true)
      else toast.error(err.response?.data?.message || '無法載入表單明細')
    } finally {
      setLoading(false)
    }
  }, [id, toast])

  useEffect(() => {
    fetchAll()
  }, [fetchAll])

  async function handleEnd(windowId) {
    setEndTarget(null)
    setEndingId(windowId)
    try {
      await endReopen(id, windowId)
      toast.success('已結束補登並產生新版本')
      await fetchAll()
    } catch (err) {
      toast.error(err.response?.data?.message || '結束補登失敗')
    } finally {
      setEndingId(null)
    }
  }

  const config = detail?.config

  return (
    <div className="forecast-page">
      <div className="forecast-page-header">
        <h1>
          <Link to="/sales-forecast/config">銷售預估量-表單設定</Link>
          {config && ` > ${config.month}`}
        </h1>
        {config?.isClosed && canReopen && (
          <button type="button" className="btn btn--primary" onClick={() => setReopenOpen(true)}>
            開放補登
          </button>
        )}
      </div>

      {accessDenied ? (
        <div className="forecast-access-denied" role="alert">您沒有權限檢視此頁面</div>
      ) : loading && !detail ? (
        <div className="forecast-loading" role="status">載入中...</div>
      ) : !detail ? (
        <div className="forecast-empty">找不到此表單</div>
      ) : (
        <>
          <p>
            <span className={config.isClosed ? 'badge--closed' : 'badge--open'}>
              {config.isClosed ? '已結束新增設定' : '開放中'}
            </span>{' '}
            截止時間 {formatLocalDateTime(config.autoCloseAt)}
            {config.isClosed &&
              `（${detail.closeType === 'MANUAL' ? '生管手動結束' : '系統自動截止'}，結束於 ${formatTaipeiDateTime(config.closedAt)}）`}
          </p>

          <section className="fc-detail-section">
            <h2>各通路狀態</h2>
            <div className="forecast-table-wrap">
              <table className="forecast-table" aria-label="各通路狀態">
                <thead>
                  <tr>
                    <th>通路</th>
                    <th>業務最後上傳（銷售）</th>
                    <th>業務最後上傳（禮品）</th>
                    <th>補登</th>
                    <th>生管修正</th>
                    {canReopen && <th className="actions-cell">操作</th>}
                  </tr>
                </thead>
                <tbody>
                  {detail.channels.map((c) => (
                    <tr key={c.channel}>
                      <td>{c.channel}</td>
                      <UploadCell value={c.salesLastUploadAt} />
                      <UploadCell value={c.giftLastUploadAt} />
                      <td>
                        {c.activeWindow
                          ? `補登中，至 ${formatLocalDateTime(c.activeWindow.expiresAt)}（${remainingText(c.activeWindow.expiresAt)}）`
                          : '—'}
                      </td>
                      <td>{c.correctionCount > 0 ? `${c.correctionCount} 筆` : '—'}</td>
                      {canReopen && (
                        <td className="actions-cell">
                          {c.activeWindow && (
                            <button
                              type="button"
                              className="btn btn--small btn--outline"
                              disabled={endingId === c.activeWindow.id}
                              onClick={() => setEndTarget({ windowId: c.activeWindow.id, channel: c.channel })}
                            >
                              提前結束
                            </button>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="fc-detail-section">
            <h2>異動紀錄</h2>
            {logs.length === 0 ? (
              <p className="forecast-empty">尚無異動紀錄</p>
            ) : (
              <div className="forecast-table-wrap">
                <table className="forecast-table" aria-label="異動紀錄">
                  <thead>
                    <tr>
                      <th>時間</th>
                      <th>操作者</th>
                      <th>動作</th>
                      <th>通路</th>
                      <th>內容</th>
                    </tr>
                  </thead>
                  <tbody>
                    {logs.map((l) => (
                      <tr key={l.id}>
                        <td>{formatLocalDateTime(l.createdAt)}</td>
                        <td>{l.operatorName}</td>
                        <td>{ACTION_LABELS[l.action] ?? l.action}</td>
                        <td>{l.channel ?? '全部'}</td>
                        <td>{l.detail ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <ReopenDialog
            open={reopenOpen}
            configId={id}
            month={config.month}
            channels={detail.channels}
            maxExpiresAt={detail.reopenMaxExpiresAt}
            onClose={() => setReopenOpen(false)}
            onSuccess={() => {
              setReopenOpen(false)
              fetchAll()
            }}
          />

          <ConfirmDialog
            open={endTarget !== null}
            title={`確定要提前結束『${endTarget?.channel}』的補登嗎？`}
            message="結束後會以業務補登的數字產生新的生管版本，該通路先前的生管修正將被覆蓋。"
            onConfirm={() => handleEnd(endTarget.windowId)}
            onCancel={() => setEndTarget(null)}
            confirmText="確定結束"
            confirmButtonClass="btn--primary"
          />
        </>
      )}
    </div>
  )
}

function UploadCell({ value }) {
  return value
    ? <td>{formatTaipeiDateTime(value)}</td>
    : <td style={{ color: '#c62828', fontWeight: 600 }}>未上傳</td>
}
