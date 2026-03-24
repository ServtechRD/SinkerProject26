import React, { useState } from 'react'
import {
  getProductionPlanVersions,
  getProductionPlanByRange,
} from '../../api/productionPlan'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import './ProductionPlan.css'

const MAX_MONTH_RANGE = 4

function hasPermission(user, perm) {
  return Boolean(
    user?.permissions &&
      Array.isArray(user.permissions) &&
      user.permissions.includes(perm)
  )
}

function num(v) {
  if (v == null) return 0
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

/** 內部/API 使用 YYYYMM，input type="month" 使用 YYYY-MM */
function toMonthInputValue(ym) {
  if (!ym || ym.length !== 6) return ''
  return `${ym.slice(0, 4)}-${ym.slice(4, 6)}`
}
function fromMonthInputValue(v) {
  if (!v) return ''
  return v.replace(/-/g, '')
}

function getDefaultMonth() {
  const now = new Date()
  return `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`
}

function monthRangeCount(start, end) {
  if (!start || !end || start > end) return 0
  const [sy, sm] = [parseInt(start.slice(0, 4), 10), parseInt(start.slice(4, 6), 10)]
  const [ey, em] = [parseInt(end.slice(0, 4), 10), parseInt(end.slice(4, 6), 10)]
  return (ey - sy) * 12 + (em - sm) + 1
}

export default function ProductionPlanPage() {
  const { user } = useAuth()
  const toast = useToast()
  const defaultMonth = getDefaultMonth()

  const [startMonth, setStartMonth] = useState(defaultMonth)
  const [endMonth, setEndMonth] = useState(defaultMonth)
  const [versions, setVersions] = useState([])
  const [selectedVersion, setSelectedVersion] = useState('')
  const [monthKeys, setMonthKeys] = useState([])
  const [channelOrder, setChannelOrder] = useState([])
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [accessDenied, setAccessDenied] = useState(false)
  const [queryClicked, setQueryClicked] = useState(false)
  const [queryTime, setQueryTime] = useState(null)

  const canView = hasPermission(user, 'production_plan.view')

  const handleQuery = () => {
    const count = monthRangeCount(startMonth, endMonth)
    if (!startMonth || !endMonth) {
      toast.error('請選擇查詢起始月份與查詢結束月份')
      return
    }
    if (count > MAX_MONTH_RANGE) {
      toast.error('查詢區間最多 4 個月')
      return
    }
    setQueryClicked(true)
    setQueryTime(new Date())
    setLoading(true)
    getProductionPlanVersions(startMonth, endMonth)
      .then((list) => {
        const verList = Array.isArray(list) ? list : []
        setVersions(verList)
        const first = verList.length > 0 ? verList[0] : ''
        setSelectedVersion(first)
        if (first) {
          return getProductionPlanByRange(startMonth, endMonth, first)
        }
        return { month_keys: [], rows: [] }
      })
      .then((result) => {
        if (result) {
          setMonthKeys(result.month_keys ?? result.monthKeys ?? [])
          setChannelOrder(Array.isArray(result.channel_order) ? result.channel_order : (result.rows?.[0]?.channel_data ?? []).map((c) => c.channel))
          setData(Array.isArray(result.rows) ? result.rows : [])
        }
      })
      .catch((err) => {
        if (err.response?.status === 403) setAccessDenied(true)
        else toast.error(err.response?.data?.message || '無法載入')
        setData([])
        setVersions([])
      })
      .finally(() => setLoading(false))
  }

  const onVersionChange = (version) => {
    setSelectedVersion(version)
    if (startMonth && endMonth && version) {
      setLoading(true)
      getProductionPlanByRange(startMonth, endMonth, version)
        .then((result) => {
          setMonthKeys(result.month_keys ?? result.monthKeys ?? [])
          setChannelOrder(Array.isArray(result.channel_order) ? result.channel_order : (result.rows?.[0]?.channel_data ?? []).map((c) => c.channel))
          setData(Array.isArray(result.rows) ? result.rows : [])
        })
        .catch(() => setData([]))
        .finally(() => setLoading(false))
    }
  }

  const channels = channelOrder.length > 0 ? channelOrder : (data[0]?.channel_data ?? []).map((c) => c.channel ?? c)

  const exportExcel = () => {
    if (data.length === 0) {
      toast.error('尚無資料可匯出')
      return
    }
    const headers = [
      '庫位',
      '中類名稱',
      '貨品規格',
      '品名',
      '品號',
      ...channels.flatMap((ch) => [...monthKeys.map((m) => `${ch} ${m.slice(0, 4)}/${m.slice(4, 6)}`), `${ch} Total`]),
      ...monthKeys.map((m) => `合計 ${m.slice(0, 4)}/${m.slice(4, 6)}`),
      '合計 Total',
      '原始預估',
      '差異',
      '備註',
    ]
    const rows = data.map((row) => {
      const chData = row.channel_data ?? row.channelData ?? []
      const channelCells = channels.map((_, i) => {
        const cd = chData[i]
        const months = cd?.months ?? cd ?? {}
        const monthVals = monthKeys.map((k) => num(months[k]))
        const total = num(cd?.total)
        return [...monthVals, total]
      })
      const agg = row.aggregate_months ?? row.aggregateMonths ?? {}
      const aggMonthVals = monthKeys.map((k) => num(agg[k]))
      const aggTotal = num(row.aggregate_total ?? row.aggregateTotal)
      return [
        row.warehouse_location ?? row.warehouseLocation ?? '',
        row.category ?? '',
        row.spec ?? '',
        row.product_name ?? row.productName ?? '',
        row.product_code ?? row.productCode ?? '',
        ...channelCells.flat(),
        ...aggMonthVals,
        aggTotal,
        num(row.original_forecast ?? row.originalForecast),
        num(row.difference),
        row.remarks ?? '',
      ]
    })
    const csvContent = [
      headers.join(','),
      ...rows.map((r) =>
        r.map((c) => (typeof c === 'string' && (c.includes(',') || c.includes('"') || c.includes('\n')) ? `"${String(c).replace(/"/g, '""')}"` : c)).join(',')
      ),
    ].join('\n')
    const BOM = '\uFEFF'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `生產表單_${startMonth}_${endMonth}.csv`
    a.click()
    URL.revokeObjectURL(url)
    toast.success('已匯出 CSV')
  }

  if (accessDenied || (!loading && !canView)) {
    return (
      <div className="production-plan-page">
        <h1>生產表單</h1>
        <div className="production-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="production-plan-page">
      <div className="production-page-header">
        <h1>生產表單</h1>
      </div>

      <div className="production-controls">
        <div className="control-group">
          <label htmlFor="start-month">查詢起始月份</label>
          <span className="month-input-wrap">
            <input
              id="start-month"
              type="month"
              className="form-input-month"
              value={toMonthInputValue(startMonth)}
              onChange={(e) => {
                setStartMonth(fromMonthInputValue(e.target.value))
                setQueryClicked(false)
              }}
              disabled={loading}
            />
            <button
              type="button"
              className="month-input-calendar-btn"
              onClick={() => document.getElementById('start-month')?.showPicker?.()}
              disabled={loading}
              aria-label="選擇月份"
              title="選擇月份"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
            </button>
          </span>
          <label htmlFor="end-month">查詢結束月份</label>
          <span className="month-input-wrap">
            <input
              id="end-month"
              type="month"
              className="form-input-month"
              value={toMonthInputValue(endMonth)}
              onChange={(e) => {
                setEndMonth(fromMonthInputValue(e.target.value))
                setQueryClicked(false)
              }}
              disabled={loading}
            />
            <button
              type="button"
              className="month-input-calendar-btn"
              onClick={() => document.getElementById('end-month')?.showPicker?.()}
              disabled={loading}
              aria-label="選擇月份"
              title="選擇月份"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
            </button>
          </span>
          <button
            type="button"
            className="btn btn--primary"
            onClick={handleQuery}
            disabled={loading}
          >
            {loading ? '查詢中...' : '查詢'}
          </button>
        </div>
        {monthRangeCount(startMonth, endMonth) > MAX_MONTH_RANGE && (
          <span className="production-hint production-hint--error">查詢區間最多 4 個月</span>
        )}
      </div>

      {queryClicked && (
        <section className="production-form-block">
          <h2>生產表單</h2>
          {queryTime != null && (
            <p className="production-query-time">
              查詢時間：{`${queryTime.getFullYear()}-${String(queryTime.getMonth() + 1).padStart(2, '0')}-${String(queryTime.getDate()).padStart(2, '0')} ${String(queryTime.getHours()).padStart(2, '0')}:${String(queryTime.getMinutes()).padStart(2, '0')}:${String(queryTime.getSeconds()).padStart(2, '0')}`}
            </p>
          )}
          {data.length > 0 && (
            <div className="production-form-toolbar">
              <div className="control-group">
                <label htmlFor="version-select">版本</label>
                <select
                  id="version-select"
                  className="form-select"
                  value={selectedVersion}
                  onChange={(e) => onVersionChange(e.target.value)}
                  disabled={loading}
                >
                  {(versions || []).map((v) => (
                    <option key={v} value={v}>{v}</option>
                  ))}
                </select>
              </div>
              <button type="button" className="btn btn--outline" onClick={exportExcel}>
                Excel 匯出
              </button>
            </div>
          )}
          {loading && !data.length ? (
            <div className="production-loading" role="status">載入中...</div>
          ) : data.length === 0 ? (
            <div className="production-empty">
              請生管設定結束新增查詢月份的銷售預估量表單
            </div>
          ) : (
            <div className="production-grid-wrap">
              <table className="production-grid production-form-table">
                <thead>
                  <tr>
                    <th rowSpan={2} className="frozen-col frozen-col-1 production-form-th-span">庫位</th>
                    <th rowSpan={2} className="frozen-col frozen-col-2 production-form-th-span">中類名稱</th>
                    <th rowSpan={2} className="frozen-col frozen-col-3 production-form-th-span">貨品規格</th>
                    <th rowSpan={2} className="production-form-th-span">品名</th>
                    <th rowSpan={2} className="production-form-th-span">品號</th>
                    {(channels.length ? channels : (data[0]?.channel_data ?? []).map((c) => c.channel)).map((chName) => (
                      <th key={chName} colSpan={monthKeys.length + 1} className="channel-group numeric-col">
                        {chName}
                      </th>
                    ))}
                    <th colSpan={monthKeys.length + 1} className="aggregate-group numeric-col">合計</th>
                    <th rowSpan={2} className="numeric-col production-form-th-span">原始預估</th>
                    <th rowSpan={2} className="numeric-col production-form-th-span">差異</th>
                    <th rowSpan={2} className="production-form-th-span">備註</th>
                  </tr>
                  <tr>
                    {(channels.length ? channels : (data[0]?.channel_data ?? []).map((c) => c.channel)).map((chName) => (
                      <React.Fragment key={chName}>
                        {monthKeys.map((m) => (
                          <th key={m} className="month-col numeric-col">
                            {m.slice(0, 4)}/{m.slice(4, 6)}
                          </th>
                        ))}
                        <th className="numeric-col">Total</th>
                      </React.Fragment>
                    ))}
                    {monthKeys.map((m) => (
                      <th key={`agg-${m}`} className="month-col numeric-col">
                        {m.slice(0, 4)}/{m.slice(4, 6)}
                      </th>
                    ))}
                    <th className="numeric-col">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((row, idx) => {
                    const chData = row.channel_data ?? row.channelData ?? []
                    const agg = row.aggregate_months ?? row.aggregateMonths ?? {}
                    const aggTotal = num(row.aggregate_total ?? row.aggregateTotal)
                    const orig = num(row.original_forecast ?? row.originalForecast)
                    const diff = num(row.difference)
                    return (
                      <tr key={row.product_code ?? row.productCode ?? idx}>
                        <td className="frozen-col frozen-col-1">{row.warehouse_location ?? row.warehouseLocation ?? '-'}</td>
                        <td className="frozen-col frozen-col-2">{row.category ?? '-'}</td>
                        <td className="frozen-col frozen-col-3">{row.spec ?? '-'}</td>
                        <td>{row.product_name ?? row.productName ?? '-'}</td>
                        <td>{row.product_code ?? row.productCode ?? '-'}</td>
                        {chData.map((cd, i) => {
                          const months = cd.months ?? {}
                          return (
                            <React.Fragment key={i}>
                              {monthKeys.map((m) => (
                                <td key={m} className="numeric-col">{num(months[m])}</td>
                              ))}
                              <td className="numeric-col">{num(cd.total)}</td>
                            </React.Fragment>
                          )
                        })}
                        {monthKeys.map((m) => (
                          <td key={m} className="numeric-col">{num(agg[m])}</td>
                        ))}
                        <td className="numeric-col">{aggTotal}</td>
                        <td className="numeric-col">{orig}</td>
                        <td className={`numeric-col ${diff > 0 ? 'diff-positive' : diff < 0 ? 'diff-negative' : ''}`}>
                          {diff > 0 ? '+' : ''}{diff}
                        </td>
                        <td className="remarks-cell">{row.remarks ?? '-'}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {!queryClicked && (
        <p className="production-hint">請選擇查詢起始月份、查詢結束月份（最多 4 個月）後按「查詢」。</p>
      )}
    </div>
  )
}
