import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import { getMaterialPurchase, triggerErp } from '../../api/materialPurchase'
import WeekPicker from '../../components/WeekPicker'
import ConfirmDialog from '../../components/ConfirmDialog'
import './MaterialPurchase.css'

const FACTORIES = ['F1', 'F2', 'F3']

function hasPermission(user, perm) {
  if (user?.permissions && Array.isArray(user.permissions)) {
    return user.permissions.includes(perm)
  }
  if (user?.roleCode === 'admin') return true
  return false
}

// Get Monday of current week
function getCurrentWeekMonday() {
  const today = new Date()
  const day = today.getDay()
  const diff = today.getDate() - day + (day === 0 ? -6 : 1)
  const monday = new Date(today.setDate(diff))
  const year = monday.getFullYear()
  const month = String(monday.getMonth() + 1).padStart(2, '0')
  const dayStr = String(monday.getDate()).padStart(2, '0')
  return `${year}-${month}-${dayStr}`
}

function formatNumber(value) {
  if (value === null || value === undefined) return '-'
  const num = Number(value)
  if (isNaN(num)) return '-'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export default function MaterialPurchasePage() {
  const { user } = useAuth()
  const toast = useToast()

  const [weekStart, setWeekStart] = useState(getCurrentWeekMonday())
  const [factory, setFactory] = useState('F1')
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [triggeringId, setTriggeringId] = useState(null)
  const [confirmDialog, setConfirmDialog] = useState({ open: false, item: null })

  const canView = hasPermission(user, 'material_purchase.view')
  const canTrigger = hasPermission(user, 'material_purchase.trigger_erp')

  const fetchData = useCallback(async () => {
    if (!weekStart || !factory) return

    setLoading(true)
    try {
      const result = await getMaterialPurchase(weekStart, factory)
      setData(result)
    } catch (err) {
      if (err.response?.status === 403) {
        toast.error('您沒有權限檢視物料採購資料')
      } else {
        toast.error('無法載入物料採購資料')
      }
      setData([])
    } finally {
      setLoading(false)
    }
  }, [weekStart, factory, toast])

  useEffect(() => {
    if (canView && weekStart && factory) {
      fetchData()
    }
  }, [weekStart, factory, canView, fetchData])

  const handleTriggerClick = (item) => {
    setConfirmDialog({ open: true, item })
  }

  const handleConfirmTrigger = async () => {
    const item = confirmDialog.item
    if (!item) return

    setTriggeringId(item.id)
    try {
      const result = await triggerErp(item.id)
      toast.success(`ERP 訂單建立成功: ${result.erpOrderNo}`)
      setConfirmDialog({ open: false, item: null })
      // Refresh table data
      await fetchData()
    } catch (err) {
      if (err.response?.status === 409) {
        const errorMsg = err.response?.data?.message || '訂單已觸發'
        toast.error(errorMsg)
      } else if (err.response?.status === 403) {
        toast.error('您沒有權限觸發 ERP 訂單')
      } else {
        const errorMsg = err.response?.data?.message || '觸發 ERP 訂單失敗'
        toast.error(errorMsg)
      }
      setConfirmDialog({ open: false, item: null })
    } finally {
      setTriggeringId(null)
    }
  }

  const handleCancelTrigger = () => {
    setConfirmDialog({ open: false, item: null })
  }

  if (!canView) {
    return (
      <div className="material-purchase-page">
        <h1>物料採購規劃</h1>
        <div className="material-purchase-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="material-purchase-page">
      <h1>物料採購規劃</h1>

      <div className="material-purchase-filters">
        <div className="filter-group">
          <label>週次</label>
          <WeekPicker value={weekStart} onChange={setWeekStart} disabled={loading} />
        </div>

        <div className="filter-group">
          <label htmlFor="factory-select">工廠</label>
          <select
            id="factory-select"
            className="form-select"
            value={factory}
            onChange={(e) => setFactory(e.target.value)}
            disabled={loading}
          >
            {FACTORIES.map((f) => (
              <option key={f} value={f}>
                {f}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="material-purchase-loading" role="status">
          載入中...
        </div>
      ) : data.length === 0 ? (
        <div className="material-purchase-empty">
          週次 {weekStart} 和工廠 {factory} 無物料採購資料
        </div>
      ) : (
        <div className="material-purchase-table-container">
          <div className="material-purchase-info">總計: {data.length} 筆物料採購</div>
          <div className="material-purchase-table-wrapper">
            <table className="material-purchase-table">
              <thead>
                <tr>
                  <th>產品代碼</th>
                  <th>產品名稱</th>
                  <th className="numeric-col">數量</th>
                  <th>半成品代碼</th>
                  <th>半成品名稱</th>
                  <th className="numeric-col">Kg/Box</th>
                  <th className="numeric-col">籃數量</th>
                  <th className="numeric-col">Boxes/Barrel</th>
                  <th className="numeric-col">所需桶數</th>
                  <th>狀態</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {data.map((row) => (
                  <tr key={row.id}>
                    <td>{row.productCode}</td>
                    <td>{row.productName}</td>
                    <td className="numeric-col">{formatNumber(row.quantity)}</td>
                    <td>{row.semiProductCode || '-'}</td>
                    <td>{row.semiProductName || '-'}</td>
                    <td className="numeric-col">{formatNumber(row.kgPerBox)}</td>
                    <td className="numeric-col">{formatNumber(row.basketQuantity)}</td>
                    <td className="numeric-col">{formatNumber(row.boxesPerBarrel)}</td>
                    <td className="numeric-col">{formatNumber(row.requiredBarrels)}</td>
                    <td>
                      {row.isErpTriggered ? (
                        <span className="status-badge status-badge--triggered">已觸發</span>
                      ) : (
                        <span className="status-badge status-badge--pending">待觸發</span>
                      )}
                    </td>
                    <td>
                      {row.isErpTriggered ? (
                        <span className="triggered-info">已觸發: {row.erpOrderNo}</span>
                      ) : (
                        <button
                          className="btn btn--primary btn--small"
                          onClick={() => handleTriggerClick(row)}
                          disabled={!canTrigger || triggeringId === row.id}
                        >
                          {triggeringId === row.id ? '處理中...' : '觸發 ERP'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={confirmDialog.open}
        title="確認觸發 ERP"
        message={
          confirmDialog.item
            ? `確定要為 ${confirmDialog.item.productName} (${confirmDialog.item.productCode}) 觸發 ERP 採購訂單嗎？`
            : ''
        }
        onConfirm={handleConfirmTrigger}
        onCancel={handleCancelTrigger}
        loading={triggeringId !== null}
        confirmText="確認"
        confirmButtonClass="btn--primary"
      />
    </div>
  )
}
