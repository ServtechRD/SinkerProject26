import { useState, useEffect, useRef } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import { syncErpProducts, getErpProductSyncStatus } from '../../api/integrations'

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

function formatDateTime(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('zh-TW', { hour12: false })
  } catch {
    return iso
  }
}

export default function ErpProductSyncPage() {
  const { user } = useAuth()
  const { showToast } = useToast()
  const [status, setStatus] = useState(null)
  const [triggering, setTriggering] = useState(false)
  const pollRef = useRef(null)

  async function fetchStatus() {
    try {
      const data = await getErpProductSyncStatus()
      setStatus(data)
      return data
    } catch {
      // 靜默失敗，不干擾使用者
    }
  }

  useEffect(() => {
    fetchStatus()
  }, [])

  useEffect(() => {
    if (!status?.running) {
      clearInterval(pollRef.current)
      return
    }
    pollRef.current = setInterval(async () => {
      const latest = await fetchStatus()
      if (!latest?.running) clearInterval(pollRef.current)
    }, 3000)
    return () => clearInterval(pollRef.current)
  }, [status?.running])

  async function handleSync() {
    setTriggering(true)
    try {
      await syncErpProducts()
      showToast('同步已開始，可離開此頁面', 'success')
      await fetchStatus()
    } catch (err) {
      if (err.response?.status === 409) {
        showToast('同步已在執行中，請稍後', 'error')
      } else {
        showToast(err.response?.data?.message || '觸發同步失敗', 'error')
      }
    } finally {
      setTriggering(false)
    }
  }

  if (!hasPermission(user, 'user.view')) {
    return (
      <div className="page-container">
        <h1>同步商品主檔</h1>
        <p>您沒有權限執行此操作。</p>
      </div>
    )
  }

  const isRunning = status?.running === true
  const lastResult = status?.lastResult
  const lastError = status?.lastError

  return (
    <div className="page-container">
      <h1>同步商品主檔</h1>
      <p>從 ERP 系統拉取最新商品資料並更新本機資料庫。觸發後可離開此頁面，同步在背景執行。</p>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
        <button
          className="btn btn--primary"
          onClick={handleSync}
          disabled={triggering || isRunning}
        >
          {isRunning ? '同步中...' : triggering ? '觸發中...' : '開始同步'}
        </button>
        <button
          className="btn btn--outline"
          onClick={fetchStatus}
          disabled={triggering}
        >
          重新整理狀態
        </button>
      </div>

      {status && (
        <table className="data-table" style={{ maxWidth: '440px' }}>
          <tbody>
            <tr>
              <th>執行狀態</th>
              <td>{isRunning ? '⏳ 執行中（每 3 秒自動更新）' : '閒置'}</td>
            </tr>
            <tr>
              <th>上次開始時間</th>
              <td>{formatDateTime(status.lastStartedAt)}</td>
            </tr>
            <tr>
              <th>上次完成時間</th>
              <td>{formatDateTime(status.lastFinishedAt)}</td>
            </tr>
            {lastError && (
              <tr>
                <th>錯誤</th>
                <td style={{ color: '#c62828' }}>{lastError}</td>
              </tr>
            )}
            {lastResult && !lastError && (
              <>
                <tr><th>拉取筆數</th><td>{lastResult.totalFetched}</td></tr>
                <tr><th>寫入筆數</th><td>{lastResult.totalSaved}</td></tr>
                <tr><th>頁數</th><td>{lastResult.totalPages}</td></tr>
                <tr><th>耗時</th><td>{lastResult.elapsedMs} ms</td></tr>
              </>
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}
