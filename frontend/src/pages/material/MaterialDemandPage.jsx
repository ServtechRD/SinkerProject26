import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import { getMaterialDemand } from '../../api/materialDemand'
import WeekPicker from '../../components/WeekPicker'
import './MaterialDemand.css'

const FACTORIES = ['F1', 'F2', 'F3']

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
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

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr
}

export default function MaterialDemandPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [weekStart, setWeekStart] = useState(getCurrentWeekMonday())
  const [factory, setFactory] = useState('F1')
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)

  const canView = hasPermission(user, 'material_demand.view')

  const fetchData = useCallback(async () => {
    if (!weekStart || !factory) return

    setLoading(true)
    try {
      const result = await getMaterialDemand(weekStart, factory)
      setData(result)
    } catch (err) {
      if (err.response?.status === 403) {
        toast.error('您沒有權限檢視物料需求資料')
      } else {
        toast.error('無法載入物料需求資料')
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

  if (!canView) {
    return (
      <div className="material-demand-page">
        <h1>物料需求</h1>
        <div className="material-demand-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="material-demand-page">
      <h1>物料需求</h1>

      <div className="material-demand-filters">
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
        <div className="material-demand-loading" role="status">
          載入中...
        </div>
      ) : data.length === 0 ? (
        <div className="material-demand-empty">
          週次 {weekStart} 和工廠 {factory} 無物料需求資料
        </div>
      ) : (
        <div className="material-demand-table-container">
          <div className="material-demand-info">總計: {data.length} 筆物料需求</div>
          <div className="material-demand-table-wrapper">
            <table className="material-demand-table">
              <thead>
                <tr>
                  <th>物料代碼</th>
                  <th>物料名稱</th>
                  <th>單位</th>
                  <th>最近採購日期</th>
                  <th>需求日期</th>
                  <th className="numeric-col">預期交付</th>
                  <th className="numeric-col">需求數量</th>
                  <th className="numeric-col">預估庫存</th>
                </tr>
              </thead>
              <tbody>
                {data.map((row, index) => {
                  const isLowInventory =
                    row.estimatedInventory !== null &&
                    row.demandQuantity !== null &&
                    Number(row.estimatedInventory) < Number(row.demandQuantity)

                  return (
                    <tr key={index} className={isLowInventory ? 'low-inventory' : ''}>
                      <td>{row.materialCode}</td>
                      <td>{row.materialName}</td>
                      <td>{row.unit || '-'}</td>
                      <td>{formatDate(row.lastPurchaseDate)}</td>
                      <td>{formatDate(row.demandDate)}</td>
                      <td className="numeric-col">{formatNumber(row.expectedDelivery)}</td>
                      <td className="numeric-col">{formatNumber(row.demandQuantity)}</td>
                      <td className="numeric-col">{formatNumber(row.estimatedInventory)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
