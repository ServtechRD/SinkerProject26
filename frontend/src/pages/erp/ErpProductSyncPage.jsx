import { useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import { syncErpProducts } from '../../api/integrations'

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

export default function ErpProductSyncPage() {
  const { user } = useAuth()
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)

  if (!hasPermission(user, 'user.view')) {
    return (
      <div className="page-container">
        <h1>同步商品主檔</h1>
        <p>您沒有權限執行此操作。</p>
      </div>
    )
  }

  async function handleSync() {
    setLoading(true)
    setResult(null)
    try {
      const data = await syncErpProducts()
      setResult(data)
      showToast(`同步完成，共處理 ${data.totalFetched} 筆`, 'success')
    } catch (err) {
      showToast(err.response?.data?.message || '同步失敗，請稍後再試', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page-container">
      <h1>同步商品主檔</h1>
      <p>從 ERP 系統拉取最新商品資料並更新本機資料庫。</p>
      <button
        className="btn btn--primary"
        onClick={handleSync}
        disabled={loading}
      >
        {loading ? '同步中...' : '開始同步'}
      </button>

      {result && (
        <table className="data-table" style={{ marginTop: '1.5rem', maxWidth: '400px' }}>
          <tbody>
            <tr><th>拉取筆數</th><td>{result.totalFetched}</td></tr>
            <tr><th>寫入筆數</th><td>{result.totalSaved}</td></tr>
            <tr><th>頁數</th><td>{result.totalPages}</td></tr>
            <tr><th>耗時</th><td>{result.elapsedMs} ms</td></tr>
          </tbody>
        </table>
      )}
    </div>
  )
}
