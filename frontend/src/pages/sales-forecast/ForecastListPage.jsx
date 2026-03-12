import { useState, useEffect, useCallback, useMemo } from 'react'
import { listConfigs } from '../../api/forecastConfig'
import {
  getFormSummary,
  getFormVersions,
  saveFormSummaryVersion,
} from '../../api/forecast'
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

function formatVersionOption(v) {
  const at = v.created_at ?? v.createdAt
  if (at) {
    try {
      const d = new Date(at)
      if (!Number.isNaN(d.getTime())) {
        const y = d.getFullYear()
        const m = String(d.getMonth() + 1).padStart(2, '0')
        const day = String(d.getDate()).padStart(2, '0')
        const h = String(d.getHours()).padStart(2, '0')
        const min = String(d.getMinutes()).padStart(2, '0')
        const sec = String(d.getSeconds()).padStart(2, '0')
        return `${y}-${m}-${day} ${h}:${min}:${sec}`
      }
    } catch (_) {}
  }
  return String(v.version_no ?? v.versionNo ?? '')
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

function num(v) {
  if (v == null || v === '') return 0
  const n = Number(v)
  return Number.isNaN(n) ? 0 : n
}

export default function ForecastListPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(true)
  const [accessDenied, setAccessDenied] = useState(false)

  const [selectedMonth, setSelectedMonth] = useState('')
  const [formVersions, setFormVersions] = useState([])
  const [selectedVersionNo, setSelectedVersionNo] = useState(null)
  const [queryClicked, setQueryClicked] = useState(false)
  const [loadingQuery, setLoadingQuery] = useState(false)
  const [channelOrder, setChannelOrder] = useState([])
  const [rows, setRows] = useState([])
  const [versionRemark, setVersionRemark] = useState(null)

  const [editMode, setEditMode] = useState(false)
  const [editChannelValues, setEditChannelValues] = useState({})
  const [saving, setSaving] = useState(false)
  const [reasonModal, setReasonModal] = useState(null)
  const [remarkModal, setRemarkModal] = useState(null)

  const [sortKey, setSortKey] = useState(DEFAULT_SORT_KEY)
  const [sortAsc, setSortAsc] = useState(DEFAULT_SORT_ASC)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)

  const canView = hasPermission(user, 'sales_forecast.update_after_closed')

  const selectedConfig = useMemo(
    () => configs.find((c) => c.month === selectedMonth),
    [configs, selectedMonth]
  )
  const monthClosed = Boolean(selectedConfig?.isClosed ?? selectedConfig?.is_closed)

  const fetchConfigs = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listConfigs()
      const sorted = [...(data || [])].sort((a, b) => (b.month > a.month ? 1 : b.month < a.month ? -1 : 0))
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

  useEffect(() => {
    if (!selectedMonth || !monthClosed) {
      setFormVersions([])
      setSelectedVersionNo(null)
      return
    }
    getFormVersions(selectedMonth)
      .then((list) => {
        setFormVersions(Array.isArray(list) ? list : [])
        if (Array.isArray(list) && list.length > 0 && selectedVersionNo == null) {
          setSelectedVersionNo(list[0].version_no ?? list[0].versionNo)
        }
      })
      .catch(() => setFormVersions([]))
  }, [selectedMonth, monthClosed])

  const runQuery = useCallback(async () => {
    if (!selectedMonth || !monthClosed) return
    if (selectedVersionNo == null) {
      toast.error('請選擇版本')
      return
    }
    setQueryClicked(true)
    setLoadingQuery(true)
    setRows([])
    setPage(1)
    try {
      const data = await getFormSummary(selectedMonth, selectedVersionNo)
      setChannelOrder(data.channel_order ?? data.channelOrder ?? [])
      setRows(data.rows ?? [])
      setVersionRemark(data.version_remark ?? data.versionRemark ?? null)
    } catch (err) {
      if (err.response?.status === 403) toast.error('您沒有權限檢視此資料')
      else toast.error(err.response?.data?.message || '無法載入表單摘要')
      setRows([])
    } finally {
      setLoadingQuery(false)
    }
  }, [selectedMonth, monthClosed, selectedVersionNo, toast])

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

  const handleEditStart = () => {
    const next = {}
    rows.forEach((row, rowIdx) => {
      const cells = row.channel_cells ?? row.channelCells ?? []
      next[rowIdx] = cells.map((c) => String(c.current_qty ?? c.currentQty ?? ''))
    })
    setEditChannelValues(next)
    setEditMode(true)
  }

  const handleEditCancel = () => {
    setEditMode(false)
    setEditChannelValues({})
  }

  const setEditCell = (rowIdx, chIdx, value) => {
    setEditChannelValues((prev) => {
      const arr = prev[rowIdx] ? [...prev[rowIdx]] : []
      arr[chIdx] = value
      return { ...prev, [rowIdx]: arr }
    })
  }

  const getEditValue = (rowIdx, chIdx) => {
    const arr = editChannelValues[rowIdx]
    if (!arr || chIdx >= arr.length) return ''
    return arr[chIdx]
  }

  const handleSaveClick = () => {
    setReasonModal({ open: true })
  }

  const handleReasonSubmit = async (changeReason) => {
    setReasonModal(null)
    if (!changeReason || !changeReason.trim()) {
      toast.error('請輸入修改原因')
      return
    }
    setSaving(true)
    try {
      const channelOrderList = channelOrder.length ? channelOrder : (rows[0]?.channel_cells ?? rows[0]?.channelCells ?? []).map((_, i) => `通路${i + 1}`)
      const payloadRows = sortedRows.map((row, rowIdx) => {
        const cells = row.channel_cells ?? row.channelCells ?? []
        const quantities = editMode ? (editChannelValues[rowIdx] ?? cells.map((c) => c.current_qty ?? c.currentQty)) : cells.map((c) => c.current_qty ?? c.currentQty)
        return {
          warehouse_location: row.warehouse_location ?? row.warehouseLocation,
          category: row.category,
          spec: row.spec,
          product_name: row.product_name ?? row.productName,
          product_code: row.product_code ?? row.productCode,
          channel_quantities: (Array.isArray(quantities) ? quantities : []).map((q) => (q != null && q !== '') ? Number(q) : 0),
        }
      })
      const res = await saveFormSummaryVersion(selectedMonth, {
        change_reason: changeReason.trim(),
        rows: payloadRows,
      })
      const newVersionNo = res?.version_no ?? res?.versionNo
      toast.success('儲存成功')
      setEditMode(false)
      setEditChannelValues({})
      setFormVersions((prev) => [...prev, { versionNo: newVersionNo, version_no: newVersionNo, createdAt: new Date().toISOString(), created_at: new Date().toISOString(), changeReason: changeReason.trim(), change_reason: changeReason.trim() }])
      setSelectedVersionNo(newVersionNo)
      await runQuery()
    } catch (err) {
      toast.error(err.response?.data?.message || '儲存失敗')
    } finally {
      setSaving(false)
    }
  }

  const handleExportExcel = () => {
    if (!rows.length) {
      toast.info('無資料可匯出')
      return
    }
    const BOM = '\uFEFF'
    const chOrder = channelOrder.length ? channelOrder : (rows[0]?.channel_cells ?? rows[0]?.channelCells ?? []).map((_, i) => `通路${i + 1}`)
    const headers = ['庫位', '中類名稱', '貨品規格', '品名', '品號', ...chOrder, '原小計', '更改後小計', '差異', '備註']
    const dataRows = sortedRows.map((row, rowIdx) => {
      const cells = row.channel_cells ?? row.channelCells ?? []
      const prevSum = cells.reduce((s, c) => s + num(c.previous_qty ?? c.previousQty), 0)
      const currVals = editMode && editChannelValues[rowIdx] ? editChannelValues[rowIdx] : cells.map((c) => c.current_qty ?? c.currentQty)
      const currSum = currVals.reduce((s, v) => s + num(v), 0)
      const diff = prevSum - currSum
      const remark = versionRemark ?? '-'
      return [
        row.warehouse_location ?? row.warehouseLocation ?? '',
        row.category ?? '',
        row.spec ?? '',
        row.product_name ?? row.productName ?? '',
        row.product_code ?? row.productCode ?? '',
        ...(editMode && editChannelValues[rowIdx] ? editChannelValues[rowIdx] : cells.map((c) => c.current_qty ?? c.currentQty ?? '')),
        prevSum,
        currSum,
        diff,
        remark,
      ]
    })
    const csv = BOM + [headers.join(','), ...dataRows.map((r) => r.join(','))].join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `總體銷售預估量_${selectedMonth}_v${selectedVersionNo}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }

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
                  setSelectedVersionNo(null)
                }}
              >
                <option value="">請選擇月份</option>
                {configs.map((c) => (
                  <option key={c.id ?? c.month} value={c.month}>
                    {formatMonth(c.month)}
                  </option>
                ))}
              </select>
            </div>
            <div className="filter-field">
              <label htmlFor="version-select">版本</label>
              <select
                id="version-select"
                className="form-select"
                value={selectedVersionNo ?? ''}
                onChange={(e) => {
                  const v = e.target.value
                  setSelectedVersionNo(v === '' ? null : Number(v))
                  setQueryClicked(false)
                }}
                disabled={!monthClosed}
              >
                {!monthClosed ? (
                  <option value="">無版本</option>
                ) : formVersions.length === 0 ? (
                  <option value="">載入版本中...</option>
                ) : (
                  formVersions.map((v) => (
                    <option key={v.version_no ?? v.versionNo} value={v.version_no ?? v.versionNo}>
                      {formatVersionOption(v)}
                    </option>
                  ))
                )}
              </select>
            </div>
            <div className="filter-field filter-field--button">
              <label>&nbsp;</label>
              <button
                type="button"
                className="btn btn--primary"
                onClick={runQuery}
                disabled={!selectedMonth || !monthClosed || selectedVersionNo == null || loadingQuery}
              >
                {loadingQuery ? '查詢中...' : '查詢'}
              </button>
            </div>
          </div>

          {!monthClosed && selectedMonth && (
            <p className="forecast-hint forecast-hint--warning">該月份尚未結束，無法查詢。請選擇已結束的月份。</p>
          )}

          {queryClicked && (
            <>
              {loadingQuery ? (
                <div className="forecast-loading" role="status">載入中...</div>
              ) : (
                <>
                  <section className="forecast-result-block">
                    <h2>總體銷售預估量表單結果</h2>
                    <div className="forecast-result-toolbar">
                      {!editMode ? (
                        <button type="button" className="btn btn--outline" onClick={handleEditStart} disabled={!rows.length}>
                          編輯
                        </button>
                      ) : (
                        <>
                          <button type="button" className="btn btn--primary" onClick={handleSaveClick} disabled={saving}>
                            {saving ? '儲存中...' : '儲存'}
                          </button>
                          <button type="button" className="btn btn--outline" onClick={handleEditCancel} disabled={saving}>
                            取消
                          </button>
                        </>
                      )}
                      <button type="button" className="btn btn--outline" onClick={handleExportExcel} disabled={!rows.length}>
                        Excel 匯出
                      </button>
                    </div>
                    {rows.length === 0 ? (
                      <div className="forecast-empty">該月份尚無預估資料</div>
                    ) : (
                      <>
                        <div className="forecast-table-wrap forecast-table-wrap--summary">
                          <table className="forecast-table forecast-table--summary">
                            <thead>
                              <tr>
                                <th className={sortKey === 'warehouse_location' ? 'sortable sort-active' : 'sortable'} onClick={() => handleSort('warehouse_location')}>
                                  庫位 {sortKey === 'warehouse_location' && (sortAsc ? '↑' : '↓')}
                                </th>
                                <th className={sortKey === 'category' ? 'sortable sort-active' : 'sortable'} onClick={() => handleSort('category')}>中類名稱 {sortKey === 'category' && (sortAsc ? '↑' : '↓')}</th>
                                <th className={sortKey === 'spec' ? 'sortable sort-active' : 'sortable'} onClick={() => handleSort('spec')}>貨品規格 {sortKey === 'spec' && (sortAsc ? '↑' : '↓')}</th>
                                <th className={sortKey === 'product_name' ? 'sortable sort-active' : 'sortable'} onClick={() => handleSort('product_name')}>品名 {sortKey === 'product_name' && (sortAsc ? '↑' : '↓')}</th>
                                <th className={sortKey === 'product_code' ? 'sortable sort-active' : 'sortable'} onClick={() => handleSort('product_code')}>品號 {sortKey === 'product_code' && (sortAsc ? '↑' : '↓')}</th>
                                {(channelOrder || []).map((ch) => (
                                  <th key={ch} className="align-right channel-value-header">{ch}</th>
                                ))}
                                <th className="align-right">原小計</th>
                                <th className="align-right">更改後小計</th>
                                <th className="align-right">差異</th>
                                <th>備註</th>
                              </tr>
                            </thead>
                            <tbody>
                              {pageRows.map((row, idx) => {
                                const globalIdx = sortedRows.indexOf(row)
                                const cells = row.channel_cells ?? row.channelCells ?? []
                                const prevSum = cells.reduce((s, c) => s + num(c.previous_qty ?? c.previousQty), 0)
                                const currSum = editMode && editChannelValues[globalIdx]
                                  ? editChannelValues[globalIdx].reduce((s, v) => s + num(v), 0)
                                  : cells.reduce((s, c) => s + num(c.current_qty ?? c.currentQty), 0)
                                const diff = prevSum - currSum
                                return (
                                  <tr key={globalIdx}>
                                    <td>{row.warehouse_location ?? row.warehouseLocation ?? '-'}</td>
                                    <td>{row.category ?? '-'}</td>
                                    <td>{row.spec ?? '-'}</td>
                                    <td>{row.product_name ?? row.productName ?? '-'}</td>
                                    <td>{row.product_code ?? row.productCode ?? '-'}</td>
                                    {cells.map((cell, i) => (
                                      <td key={i} className="align-right">
                                        {editMode ? (
                                          <input
                                            type="text"
                                            className="quantity-input"
                                            value={getEditValue(globalIdx, i)}
                                            onChange={(e) => setEditCell(globalIdx, i, e.target.value)}
                                          />
                                        ) : (
                                          cell.current_qty ?? cell.currentQty ?? '-'
                                        )}
                                      </td>
                                    ))}
                                    <td className="align-right">{prevSum}</td>
                                    <td className="align-right">{currSum}</td>
                                    <td className="align-right">{diff}</td>
                                    <td className="forecast-remark-cell">
                                      {versionRemark ? (
                                        <button
                                          type="button"
                                          className="btn btn--small btn--outline forecast-remark-btn"
                                          onClick={() => setRemarkModal({ text: versionRemark })}
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
                              <button type="button" className="btn btn--small btn--outline" disabled={page <= 1} onClick={() => setPage((p) => Math.max(1, p - 1))}>上一頁</button>
                              <span className="forecast-pagination-page">第 {page} / {totalPages} 頁</span>
                              <button type="button" className="btn btn--small btn--outline" disabled={page >= totalPages} onClick={() => setPage((p) => Math.min(totalPages, p + 1))}>下一頁</button>
                            </div>
                            <div className="forecast-pagination-size">
                              <label htmlFor="page-size">每頁</label>
                              <select id="page-size" className="form-select form-select--sm" value={pageSize} onChange={(e) => { setPageSize(Number(e.target.value)); setPage(1) }}>
                                {PAGE_SIZE_OPTIONS.map((n) => (<option key={n} value={n}>{n}</option>))}
                              </select>
                              筆
                            </div>
                          </div>
                        )}
                      </>
                    )}
                  </section>
                </>
              )}
            </>
          )}

          {queryClicked && !loadingQuery && !rows.length && selectedMonth && monthClosed && (
            <p className="forecast-hint">該月份尚無預估資料。</p>
          )}

          {reasonModal?.open && (
            <div className="forecast-remark-modal-overlay" onClick={() => setReasonModal(null)} role="presentation">
              <div className="forecast-remark-modal" onClick={(e) => e.stopPropagation()}>
                <div className="forecast-remark-modal-header">
                  <h3>修改原因</h3>
                  <button type="button" className="forecast-remark-modal-close" onClick={() => setReasonModal(null)} aria-label="關閉">×</button>
                </div>
                <ReasonForm onSubmit={handleReasonSubmit} onCancel={() => setReasonModal(null)} />
              </div>
            </div>
          )}

          {remarkModal && (
            <div className="forecast-remark-modal-overlay" onClick={() => setRemarkModal(null)} role="presentation">
              <div className="forecast-remark-modal" onClick={(e) => e.stopPropagation()}>
                <div className="forecast-remark-modal-header">
                  <h3>本版備註（修改原因）</h3>
                  <button type="button" className="forecast-remark-modal-close" onClick={() => setRemarkModal(null)} aria-label="關閉">×</button>
                </div>
                <p className="forecast-remark-modal-list" style={{ margin: 16 }}>{remarkModal.text}</p>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}

function ReasonForm({ onSubmit, onCancel }) {
  const [value, setValue] = useState('')
  return (
    <div style={{ padding: 16 }}>
      <textarea
        className="form-select"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="請輸入修改原因"
        rows={4}
        style={{ width: '100%', resize: 'vertical' }}
      />
      <div style={{ marginTop: 12, display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <button type="button" className="btn btn--outline" onClick={onCancel}>取消</button>
        <button type="button" className="btn btn--primary" onClick={() => onSubmit(value)}>確定</button>
      </div>
    </div>
  )
}
