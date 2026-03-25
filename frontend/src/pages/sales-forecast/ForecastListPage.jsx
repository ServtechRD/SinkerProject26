import { useState, useEffect, useCallback, useMemo } from 'react'
import { listConfigs } from '../../api/forecastConfig'
import {
  getFormSummary,
  getFormVersions,
  saveFormSummaryVersion,
} from '../../api/forecast'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import ConfirmDialog from '../../components/ConfirmDialog'
import './ForecastList.css'

const PAGE_SIZE_OPTIONS = [10, 20, 50]
const DEFAULT_SORT_KEY = 'product_code'
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

/** 僅接受數字／數字字串作為 runQuery 覆寫版號；避免 onClick={runQuery} 把 MouseEvent 當成 versionOverride */
function normalizeQueryVersionOverride(arg) {
  if (arg == null) return undefined
  if (typeof arg === 'number' && Number.isFinite(arg)) return arg
  if (typeof arg === 'string' && arg.trim() !== '') {
    const n = parseInt(arg.trim(), 10)
    return Number.isNaN(n) ? undefined : n
  }
  return undefined
}

/** 解析出 API 用的正整數 version_no；失敗回傳 null */
function resolveFormVersionNo(raw, formVersions) {
  if (raw == null) return null
  if (typeof raw === 'number' && Number.isNaN(raw)) return null
  const s = String(raw).trim()
  if (s === '' || s === 'undefined' || s === 'null') return null
  const parsed = parseInt(s, 10)
  if (!Number.isNaN(parsed) && parsed >= 1) return parsed
  const f = Number(s)
  if (Number.isFinite(f) && f >= 1) return Math.floor(f)
  if (Array.isArray(formVersions)) {
    const hit = formVersions.find(
      (v) =>
        v.version_no === raw ||
        v.versionNo === raw ||
        String(v.version_no ?? v.versionNo) === s,
    )
    if (hit != null) {
      const vn = hit.version_no ?? hit.versionNo
      const x = Number(vn)
      if (Number.isFinite(x) && x >= 1) return Math.floor(x)
    }
  }
  return null
}

function formatVersionOption(v) {
  // 優先使用後端回傳的台灣時間顯示字串 (created_at_display)
  const display = v.created_at_display ?? v.createdAtDisplay
  if (display) return display
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

/** 通路格：目前值是否與前一版不同（用於紅字） */
function channelValueChangedFromPrev(prevQty, currQty) {
  return Math.abs(num(currQty) - num(prevQty)) > 1e-9
}

/** 編輯列：依各格贈品量（current − sales）不變，加總編輯後銷售，得到與目前版 current_qty 同口徑的列總合 */
function editedRowCurrentQtyTotal(cells, rowIdxInRows, getEditVal) {
  return cells.reduce((s, c, i) => {
    const baseCurr = num(c.current_qty ?? c.currentQty)
    const baseSales = num(
      c.current_sales_qty ?? c.currentSalesQty ?? c.current_qty ?? c.currentQty,
    )
    const giftPart = baseCurr - baseSales
    const raw = getEditVal(rowIdxInRows, i)
    const editedSales = raw === '' || raw == null ? 0 : num(raw)
    return s + editedSales + giftPart
  }, 0)
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
  /** 最近一次成功載入的表單版本號（與 API 一致，用於第 1 版不顯示「變動」紅字） */
  const [summaryVersionNo, setSummaryVersionNo] = useState(null)

  const [editMode, setEditMode] = useState(false)
  const [editChannelValues, setEditChannelValues] = useState({})
  const [editRowRemarks, setEditRowRemarks] = useState({})
  const [saving, setSaving] = useState(false)
  const [saveConfirmOpen, setSaveConfirmOpen] = useState(false)
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
      setSummaryVersionNo(null)
      return
    }
    getFormVersions(selectedMonth)
      .then((list) => {
        const arr = Array.isArray(list) ? list : []
        setFormVersions(arr)
        if (arr.length === 0) return
        // 用函數式更新，避免 .then 閉包讀到過期的 selectedVersionNo，導致永遠不寫入預設版號
        setSelectedVersionNo((prev) => {
          if (prev != null) return prev
          const vn = resolveFormVersionNo(arr[0].version_no ?? arr[0].versionNo, arr)
          return vn != null ? vn : prev
        })
      })
      .catch(() => setFormVersions([]))
  }, [selectedMonth, monthClosed])

  const runQuery = useCallback(async (versionOverride) => {
    if (!selectedMonth || !monthClosed) return
    const explicitVer = normalizeQueryVersionOverride(versionOverride)
    // 版本清單為非同步載入：使用者可能在 setSelectedVersionNo 生效前就按查詢，此時改以清單第一筆 fallback
    let raw = explicitVer ?? selectedVersionNo
    let usedListFallback = false
    if (raw == null && formVersions.length > 0) {
      raw = formVersions[0].version_no ?? formVersions[0].versionNo
      usedListFallback = true
    }
    const versionNum = resolveFormVersionNo(raw, formVersions)
    if (versionNum == null) {
      toast.error('請選擇版本')
      return
    }
    if (usedListFallback) {
      setSelectedVersionNo(versionNum)
    }
    setQueryClicked(true)
    setLoadingQuery(true)
    setRows([])
    setPage(1)
    try {
      const data = await getFormSummary(selectedMonth, versionNum)
      setChannelOrder(data.channel_order ?? data.channelOrder ?? [])
      setRows(data.rows ?? [])
      setVersionRemark(data.version_remark ?? data.versionRemark ?? null)
      setSummaryVersionNo(data.version_no ?? data.versionNo ?? versionNum)
    } catch (err) {
      if (err.response?.status === 403) toast.error('您沒有權限檢視此資料')
      else toast.error(err.response?.data?.message || '無法載入表單摘要')
      setRows([])
      setSummaryVersionNo(null)
    } finally {
      setLoadingQuery(false)
    }
  }, [selectedMonth, monthClosed, selectedVersionNo, formVersions, toast])

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

  /** 第 1 版無「與前一版差異」，不顯示紅字（依最後一次成功載入的 API version_no） */
  const isFormVersion1 = useMemo(
    () => summaryVersionNo != null && Number(summaryVersionNo) === 1,
    [summaryVersionNo],
  )

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
      // 編輯時使用銷售數量（current_sales_qty），儲存僅會更新銷售預估上傳
      next[rowIdx] = cells.map((c) => String(c.current_sales_qty ?? c.currentSalesQty ?? c.current_qty ?? c.currentQty ?? ''))
    })
    setEditChannelValues(next)
    const remarks = {}
    rows.forEach((row, rowIdx) => {
      remarks[rowIdx] = row.remark ?? ''
    })
    setEditRowRemarks(remarks)
    setEditMode(true)
  }

  const handleEditCancel = () => {
    setEditMode(false)
    setEditChannelValues({})
    setEditRowRemarks({})
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

  const performSave = async (changeReason = '') => {
    setSaving(true)
    try {
      const payloadRows = sortedRows.map((row) => {
        const rowIdxInRows = rows.indexOf(row)
        const cells = row.channel_cells ?? row.channelCells ?? []
        const quantities = editMode ? (editChannelValues[rowIdxInRows] ?? cells.map((c) => c.current_sales_qty ?? c.currentSalesQty ?? c.current_qty ?? c.currentQty)) : cells.map((c) => c.current_qty ?? c.currentQty)
        return {
          warehouse_location: row.warehouse_location ?? row.warehouseLocation,
          category: row.category,
          spec: row.spec,
          product_name: row.product_name ?? row.productName,
          product_code: row.product_code ?? row.productCode,
          remark: editMode ? (editRowRemarks[rowIdxInRows] ?? row.remark ?? '') : (row.remark ?? ''),
          channel_quantities: (Array.isArray(quantities) ? quantities : []).map((q) => (q != null && q !== '') ? Number(q) : 0),
        }
      })
      const res = await saveFormSummaryVersion(selectedMonth, {
        change_reason: (changeReason && changeReason.trim()) ? changeReason.trim() : '',
        rows: payloadRows,
      })
      const newVersionNo = res?.version_no ?? res?.versionNo
      toast.success('儲存成功')
      setEditMode(false)
      setEditChannelValues({})
      setEditRowRemarks({})
      const [versList, summaryData] = await Promise.all([
        getFormVersions(selectedMonth),
        getFormSummary(selectedMonth, newVersionNo),
      ])
      setFormVersions(Array.isArray(versList) ? versList : [])
      setSelectedVersionNo(newVersionNo)
      setChannelOrder(summaryData.channel_order ?? summaryData.channelOrder ?? [])
      setRows(summaryData.rows ?? [])
      setVersionRemark(summaryData.version_remark ?? summaryData.versionRemark ?? null)
      setSummaryVersionNo(summaryData.version_no ?? summaryData.versionNo ?? newVersionNo)
      setPage(1)
    } catch (err) {
      toast.error(err.response?.data?.message || '儲存失敗')
    } finally {
      setSaving(false)
    }
  }

  const handleSaveClick = () => {
    setSaveConfirmOpen(true)
  }

  const handleSaveConfirm = () => {
    setSaveConfirmOpen(false)
    performSave('')
  }

  const handleExportExcel = () => {
    if (!rows.length) {
      toast.info('無資料可匯出')
      return
    }
    const BOM = '\uFEFF'
    const chOrder = channelOrder.length ? channelOrder : (rows[0]?.channel_cells ?? rows[0]?.channelCells ?? []).map((_, i) => `通路${i + 1}`)
    const headers = ['庫位', '中類名稱', '貨品規格', '品名', '品號', ...chOrder, '原小計', '更改後小計', '差異', '備註']
    const dataRows = sortedRows.map((row) => {
      const rowIdxInRows = rows.indexOf(row)
      const cells = row.channel_cells ?? row.channelCells ?? []
      /** 非編輯：原小計欄 = 前一版各通路數量加總；編輯時匯出「原小計」改為本版總合（與畫面編輯模式一致） */
      const previousVersionSum = cells.reduce((s, c) => s + num(c.previous_qty ?? c.previousQty), 0)
      const currentVersionSum = cells.reduce((s, c) => s + num(c.current_qty ?? c.currentQty), 0)
      const afterTotal = editMode && editChannelValues[rowIdxInRows]
        ? editedRowCurrentQtyTotal(cells, rowIdxInRows, getEditValue)
        : currentVersionSum
      const exportOriginal = editMode ? currentVersionSum : previousVersionSum
      const diff = exportOriginal - afterTotal
      /** 第 1 版無「與前一版比較」：未編輯時更改後小計、差異固定顯示 0 */
      const exportAfterSum = isFormVersion1 && !editMode ? 0 : afterTotal
      const exportDiff = isFormVersion1 && !editMode ? 0 : diff
      const remark = editMode ? (editRowRemarks[rowIdxInRows] ?? row.remark ?? '-') : (row.remark ?? '-')
      return [
        row.warehouse_location ?? row.warehouseLocation ?? '',
        row.category ?? '',
        row.spec ?? '',
        row.product_name ?? row.productName ?? '',
        row.product_code ?? row.productCode ?? '',
        ...(editMode && editChannelValues[rowIdxInRows] ? editChannelValues[rowIdxInRows] : cells.map((c) => c.current_qty ?? c.currentQty ?? '')),
        exportOriginal,
        exportAfterSum,
        exportDiff,
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
                  setSummaryVersionNo(null)
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
                value={selectedVersionNo == null ? '' : String(selectedVersionNo)}
                onChange={(e) => {
                  const v = e.target.value
                  if (v === '') {
                    setSelectedVersionNo(null)
                  } else {
                    const n = parseInt(v, 10)
                    setSelectedVersionNo(Number.isNaN(n) ? null : n)
                  }
                  setQueryClicked(false)
                  setSummaryVersionNo(null)
                }}
                disabled={!monthClosed}
              >
                {!monthClosed ? (
                  <option value="">無版本</option>
                ) : formVersions.length === 0 ? (
                  <option value="">載入版本中...</option>
                ) : (
                  formVersions
                    .filter((v) => v.version_no != null || v.versionNo != null)
                    .map((v) => {
                      const vn = v.version_no ?? v.versionNo
                      return (
                        <option key={vn} value={String(vn)}>
                          {formatVersionOption(v)}
                        </option>
                      )
                    })
                )}
              </select>
            </div>
            <div className="filter-field filter-field--button">
              <label>&nbsp;</label>
              <button
                type="button"
                className="btn btn--primary"
                onClick={() => runQuery()}
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
                      <button type="button" className="btn btn--outline" onClick={handleExportExcel} disabled={!rows.length || editMode}>
                        Excel 匯出
                      </button>
                    </div>
                    {rows.length === 0 ? (
                      <div className="forecast-empty">該月份尚無預估資料</div>
                    ) : (
                      <>
                        {versionRemark != null && versionRemark !== '' && (
                          <p className="forecast-version-reason">
                            本版修改原因：
                            <button type="button" className="btn btn--small btn--link" onClick={() => setRemarkModal({ text: versionRemark })}>
                              檢視
                            </button>
                          </p>
                        )}
                        <div className="forecast-table-wrap forecast-table-wrap--summary">
                          <table className={`forecast-table forecast-table--summary${editMode ? ' forecast-table--edit' : ''}`}>
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
                                const rowIdxInRows = rows.indexOf(row)
                                const cells = row.channel_cells ?? row.channelCells ?? []
                                /** 查詢且非編輯：原小計 = 前一版各通路 previous_qty 加總 */
                                const previousVersionSum = cells.reduce((s, c) => s + num(c.previous_qty ?? c.previousQty), 0)
                                const currentVersionSum = cells.reduce((s, c) => s + num(c.current_qty ?? c.currentQty), 0)
                                const editedRowTotal =
                                  editMode && editChannelValues[rowIdxInRows]
                                    ? editedRowCurrentQtyTotal(cells, rowIdxInRows, getEditValue)
                                    : currentVersionSum
                                const displayOriginalSubtotal = editMode ? currentVersionSum : previousVersionSum
                                const diff = displayOriginalSubtotal - editedRowTotal
                                /** 第 1 版：未編輯模式下更改後小計、差異固定為 0 */
                                const showAfterSum = isFormVersion1 && !editMode ? 0 : editedRowTotal
                                const showDiff = isFormVersion1 && !editMode ? 0 : diff
                                const highlightAfterDiff =
                                  editMode
                                    ? Math.abs(currentVersionSum - editedRowTotal) > 1e-9
                                    : !isFormVersion1 && Math.abs(previousVersionSum - currentVersionSum) > 1e-9
                                return (
                                  <tr key={globalIdx}>
                                    <td>{row.warehouse_location ?? row.warehouseLocation ?? '-'}</td>
                                    <td>{row.category ?? '-'}</td>
                                    <td>{row.spec ?? '-'}</td>
                                    <td>{row.product_name ?? row.productName ?? '-'}</td>
                                    <td>{row.product_code ?? row.productCode ?? '-'}</td>
                                    {cells.map((cell, i) => {
                                      const prevQ = cell.previous_qty ?? cell.previousQty
                                      const currRaw = editMode
                                        ? getEditValue(rowIdxInRows, i)
                                        : (cell.current_qty ?? cell.currentQty)
                                      const changed = !isFormVersion1 && channelValueChangedFromPrev(prevQ, currRaw)
                                      return (
                                        <td key={i} className="align-right forecast-channel-cell">
                                          {editMode ? (
                                            <input
                                              type="text"
                                              className={`quantity-input${changed ? ' quantity-input--channel-changed' : ''}`}
                                              value={getEditValue(rowIdxInRows, i)}
                                              onChange={(e) => setEditCell(rowIdxInRows, i, e.target.value)}
                                              title={changed ? '與前一版不同' : undefined}
                                            />
                                          ) : (
                                            <span
                                              className={changed ? 'forecast-channel-value--changed' : undefined}
                                              title={changed ? '與前一版不同' : undefined}
                                            >
                                              {cell.current_qty ?? cell.currentQty ?? '-'}
                                            </span>
                                          )}
                                        </td>
                                      )
                                    })}
                                    <td className="align-right">{displayOriginalSubtotal}</td>
                                    <td className={`align-right${highlightAfterDiff ? ' forecast-channel-value--changed' : ''}`}>{showAfterSum}</td>
                                    <td className={`align-right${highlightAfterDiff ? ' forecast-channel-value--changed' : ''}`}>{showDiff}</td>
                                    <td className="forecast-remark-cell">
                                      {editMode ? (
                                        <input
                                          type="text"
                                          className="form-select forecast-remark-input"
                                          value={editRowRemarks[rowIdxInRows] ?? row.remark ?? ''}
                                          onChange={(e) => setEditRowRemarks((prev) => ({ ...prev, [rowIdxInRows]: e.target.value }))}
                                          placeholder="列備註"
                                        />
                                      ) : (row.remark ? (
                                        <button
                                          type="button"
                                          className="btn btn--small btn--outline forecast-remark-btn"
                                          onClick={() => setRemarkModal({ text: row.remark, title: '列備註' })}
                                        >
                                          備註
                                        </button>
                                      ) : (
                                        '-'
                                      ))}
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

          <ConfirmDialog
            open={saveConfirmOpen}
            title="確認儲存"
            message="是否確認修改？"
            onConfirm={handleSaveConfirm}
            onCancel={() => setSaveConfirmOpen(false)}
            confirmText="是"
            confirmButtonClass="btn--primary"
          />

          {remarkModal && (
            <div className="forecast-remark-modal-overlay" onClick={() => setRemarkModal(null)} role="presentation">
              <div className="forecast-remark-modal" onClick={(e) => e.stopPropagation()}>
                <div className="forecast-remark-modal-header">
                  <h3>{remarkModal.title ?? '本版修改原因'}</h3>
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
