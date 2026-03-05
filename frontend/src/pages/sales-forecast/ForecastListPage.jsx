import { useState, useEffect, useCallback, useMemo } from 'react'
import { listConfigs } from '../../api/forecastConfig'
import { getFormSummary } from '../../api/forecast'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import './ForecastList.css'

const PAGE_SIZE_OPTIONS = [10, 20, 50]
const DEFAULT_SORT_KEY = 'category'
const DEFAULT_SORT_ASC = true

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

function formatMonth(monthStr) {
  if (!monthStr || monthStr.length !== 6) return monthStr
  const year = monthStr.substring(0, 4)
  const month = monthStr.substring(4, 6)
  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December',
  ]
  const monthIndex = parseInt(month, 10) - 1
  const monthName = monthNames[monthIndex] || ''
  return `${monthStr} (${monthName} ${year})`
}

function getRowValue(row, key) {
  switch (key) {
    case 'warehouse_location':
      return row.warehouse_location ?? row.warehouseLocation ?? ''
    case 'category':
      return row.category ?? ''
    case 'spec':
      return row.spec ?? ''
    case 'product_name':
      return row.product_name ?? row.productName ?? ''
    case 'product_code':
      return row.product_code ?? row.productCode ?? ''
    default:
      return ''
  }
}

export default function ForecastListPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(true)
  const [accessDenied, setAccessDenied] = useState(false)

  const [selectedMonth, setSelectedMonth] = useState('')
  const [queryClicked, setQueryClicked] = useState(false)
  const [loadingQuery, setLoadingQuery] = useState(false)
  const [channelVersions, setChannelVersions] = useState([])
  const [channelOrder, setChannelOrder] = useState([])
  const [rows, setRows] = useState([])

  const [sortKey, setSortKey] = useState(DEFAULT_SORT_KEY)
  const [sortAsc, setSortAsc] = useState(DEFAULT_SORT_ASC)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [remarkModal, setRemarkModal] = useState(null) // { parts: string[] }

  const canView = hasPermission(user, 'sales_forecast.update_after_closed')

  const fetchConfigs = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listConfigs()
      const sorted = [...data].sort((a, b) => (b.month > a.month ? 1 : b.month < a.month ? -1 : 0))
      setConfigs(sorted)
      setAccessDenied(false)
    } catch (err) {
      if (err.response?.status === 403) setAccessDenied(true)
      else toast.error('無法載入月份設定')
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    fetchConfigs()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const runQuery = useCallback(async () => {
    if (!selectedMonth) return
    setQueryClicked(true)
    setLoadingQuery(true)
    setChannelVersions([])
    setChannelOrder([])
    setRows([])
    setPage(1)
    try {
      const data = await getFormSummary(selectedMonth)
      setChannelVersions(data.channel_versions ?? data.channelVersions ?? [])
      setChannelOrder(data.channel_order ?? data.channelOrder ?? [])
      setRows(data.rows ?? [])
    } catch (err) {
      if (err.response?.status === 403) toast.error('您沒有權限檢視此資料')
      else toast.error('無法載入表單摘要')
      setRows([])
    } finally {
      setLoadingQuery(false)
    }
  }, [selectedMonth, toast])

  const sortedRows = useMemo(() => {
    const key = sortKey
    const asc = sortAsc
    return [...rows].sort((a, b) => {
      const va = getRowValue(a, key)
      const vb = getRowValue(b, key)
      const cmp = String(va).localeCompare(String(vb), 'zh-TW')
      return asc ? cmp : -cmp
    })
  }, [rows, sortKey, sortAsc])

  const totalPages = Math.max(1, Math.ceil(sortedRows.length / pageSize))
  const pageRows = useMemo(() => {
    const start = (page - 1) * pageSize
    return sortedRows.slice(start, start + pageSize)
  }, [sortedRows, page, pageSize])

  const handleSort = (key) => {
    if (sortKey === key) setSortAsc((a) => !a)
    else {
      setSortKey(key)
      setSortAsc(true)
    }
    setPage(1)
  }

  useEffect(() => {
    if (page > totalPages && totalPages >= 1) setPage(totalPages)
  }, [page, totalPages])

  if (accessDenied || (!loading && !canView)) {
    return (
      <div className="forecast-list-page">
        <h1>銷售預估量表單</h1>
        <div className="forecast-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="forecast-list-page">
      <div className="forecast-page-header">
        <h1>銷售預估量表單</h1>
      </div>

      {loading ? (
        <div className="forecast-loading" role="status">載入中...</div>
      ) : (
        <>
          <div className="forecast-filters">
            <div className="filter-field">
              <label htmlFor="month-select">月份</label>
              <select
                id="month-select"
                className="form-select"
                value={selectedMonth}
                onChange={(e) => {
                  setSelectedMonth(e.target.value)
                  setQueryClicked(false)
                }}
              >
                <option value="">請選擇月份</option>
                {configs.map((c) => (
                  <option key={c.id} value={c.month}>
                    {formatMonth(c.month)}
                  </option>
                ))}
              </select>
            </div>
            <div className="filter-field filter-field--button">
              <label>&nbsp;</label>
              <button
                type="button"
                className="btn btn--primary"
                onClick={runQuery}
                disabled={!selectedMonth || loadingQuery}
              >
                {loadingQuery ? '查詢中...' : '查詢'}
              </button>
            </div>
          </div>

          {queryClicked && (
            <>
              {loadingQuery ? (
                <div className="forecast-loading" role="status">載入中...</div>
              ) : (
                <>
                  {channelVersions.length > 0 && (
                    <div className="forecast-form-summary-versions">
                      <strong>各通路最新版號：</strong>
                      {channelVersions.map((cv) => (
                        <span key={cv.channel} className="forecast-version-tag">
                          {cv.channel}: {cv.latest_version ?? cv.latestVersion ?? '-'}
                        </span>
                      ))}
                    </div>
                  )}

                  {rows.length === 0 ? (
                    <div className="forecast-empty">該月份尚無預估資料</div>
                  ) : (
                    <>
                      <div className="forecast-table-wrap forecast-table-wrap--summary">
                        <table className="forecast-table forecast-table--summary">
                          <thead>
                            <tr>
                              <th
                                className={sortKey === 'warehouse_location' ? 'sortable sort-active' : 'sortable'}
                                onClick={() => handleSort('warehouse_location')}
                              >
                                庫位 {sortKey === 'warehouse_location' && (sortAsc ? '↑' : '↓')}
                              </th>
                              <th
                                className={sortKey === 'category' ? 'sortable sort-active' : 'sortable'}
                                onClick={() => handleSort('category')}
                              >
                                中類名稱 {sortKey === 'category' && (sortAsc ? '↑' : '↓')}
                              </th>
                              <th
                                className={sortKey === 'spec' ? 'sortable sort-active' : 'sortable'}
                                onClick={() => handleSort('spec')}
                              >
                                貨品規格 {sortKey === 'spec' && (sortAsc ? '↑' : '↓')}
                              </th>
                              <th
                                className={sortKey === 'product_name' ? 'sortable sort-active' : 'sortable'}
                                onClick={() => handleSort('product_name')}
                              >
                                品名 {sortKey === 'product_name' && (sortAsc ? '↑' : '↓')}
                              </th>
                              <th
                                className={sortKey === 'product_code' ? 'sortable sort-active' : 'sortable'}
                                onClick={() => handleSort('product_code')}
                              >
                                品號 {sortKey === 'product_code' && (sortAsc ? '↑' : '↓')}
                              </th>
                              {channelOrder.map((ch) => (
                                <th key={ch} className="align-right channel-value-header">
                                  {ch}
                                </th>
                              ))}
                              <th className="align-right">原小計</th>
                              <th className="align-right">小計</th>
                              <th className="align-right">差異數</th>
                              <th>備註</th>
                            </tr>
                          </thead>
                          <tbody>
                            {pageRows.map((row, idx) => {
                              const cells = row.channel_cells ?? row.channelCells ?? []
                              const prevSum = cells.reduce((s, c) => s + (Number(c.previous_qty ?? c.previousQty) || 0), 0)
                              const currSum = cells.reduce((s, c) => s + (Number(c.current_qty ?? c.currentQty) || 0), 0)
                              const remarkParts = (channelOrder || []).map((ch, i) => {
                                const cell = cells[i]
                                const r = cell?.remark ?? ''
                                if (r == null || String(r).trim() === '') return null
                                return `${ch}: ${String(r).trim()}`
                              }).filter(Boolean)
                              return (
                                <tr key={idx}>
                                  <td>{row.warehouse_location ?? row.warehouseLocation ?? '-'}</td>
                                  <td>{row.category ?? '-'}</td>
                                  <td>{row.spec ?? '-'}</td>
                                  <td>{row.product_name ?? row.productName ?? '-'}</td>
                                  <td>{row.product_code ?? row.productCode ?? '-'}</td>
                                  {cells.map((cell, i) => (
                                    <td key={i} className="align-right">
                                      {cell.current_qty ?? cell.currentQty ?? '-'}
                                    </td>
                                  ))}
                                  <td className="align-right">{prevSum}</td>
                                  <td className="align-right">{currSum}</td>
                                  <td className="align-right">{prevSum - currSum}</td>
                                  <td className="forecast-remark-cell">
                                    {remarkParts.length > 0 ? (
                                      <button
                                        type="button"
                                        className="btn btn--small btn--outline forecast-remark-btn"
                                        onClick={() => setRemarkModal({ parts: remarkParts })}
                                      >
                                        備註
                                      </button>
                                    ) : (
                                      '-'
                                    )}
                                  </td>
                                </tr>
                              )
                            })}
                          </tbody>
                        </table>
                      </div>

                      {sortedRows.length > pageSize && (
                        <div className="forecast-pagination">
                          <span className="forecast-pagination-info">
                            第 {(page - 1) * pageSize + 1}–{Math.min(page * pageSize, sortedRows.length)} 筆，共 {sortedRows.length} 筆
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
                          <div className="forecast-pagination-size">
                            <label htmlFor="page-size">每頁</label>
                            <select
                              id="page-size"
                              className="form-select form-select--sm"
                              value={pageSize}
                              onChange={(e) => {
                                setPageSize(Number(e.target.value))
                                setPage(1)
                              }}
                            >
                              {PAGE_SIZE_OPTIONS.map((n) => (
                                <option key={n} value={n}>{n}</option>
                              ))}
                            </select>
                            筆
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </>
              )}
            </>
          )}

          {!queryClicked && selectedMonth && (
            <p className="forecast-hint">請選擇月份後點擊「查詢」顯示表單摘要。</p>
          )}

          {remarkModal && (
            <div className="forecast-remark-modal-overlay" onClick={() => setRemarkModal(null)} role="presentation">
              <div className="forecast-remark-modal" onClick={(e) => e.stopPropagation()}>
                <div className="forecast-remark-modal-header">
                  <h3>各通路修改原因</h3>
                  <button type="button" className="forecast-remark-modal-close" onClick={() => setRemarkModal(null)} aria-label="關閉">
                    ×
                  </button>
                </div>
                <ul className="forecast-remark-modal-list">
                  {remarkModal.parts.map((line, i) => (
                    <li key={i}>{line}</li>
                  ))}
                </ul>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
