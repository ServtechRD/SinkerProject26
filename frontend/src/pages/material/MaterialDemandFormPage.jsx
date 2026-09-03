import { useState, useCallback, useMemo, useEffect, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import { getWeeklyScheduleFactories } from '../../api/weeklySchedule'
import { getVendorList } from '../../api/vendors'
import {
  getMaterialDemand,
  getMaterialDemandLastEditSavedAt,
  updateMaterialDemand,
  confirmSendErp,
  reviewApproveMaterialDemand,
  reviewRejectMaterialDemand,
} from '../../api/materialDemand'
import ConfirmDialog from '../../components/ConfirmDialog'
import { formatMaterialDemandSavedAt } from '../../utils/materialDemandDateTime'
import './MaterialDemand.css'

function getWeekOptions() {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const start = new Date(year, month, 1)
  let d = new Date(start)
  while (d.getDay() !== 1) d.setDate(d.getDate() + 1)
  const end = new Date(year, month + 12, 0)
  const options = []
  while (d <= end) {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    options.push({ value: `${y}-${m}-${day}`, label: `${y}/${m}/${day}` })
    d.setDate(d.getDate() + 7)
  }
  return options
}

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

/** 數值欄位：null/undefined/空字串視為 0；數值 0 顯示 0 不顯示「-」（日期欄位另處理） */
function formatDemandNumeric(v) {
  if (v == null || v === '') {
    return (0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
  }
  const n = Number(v)
  if (Number.isNaN(n)) return '-'
  return n.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

/** 與後端 material_demand_pending_confirm.status 一致：0 待審核、1 審核完成、2 退回 */
const REVIEW_STATUS_PENDING = 0
const REVIEW_STATUS_APPROVED = 1

/** 預計庫存量 = 現有庫存 + 預交量 - 需求量（後端已計算，此處僅供顯示一致） */
function computedEstimatedInventory(row) {
  const cur = Number(row.currentStock)
  const del = Number(row.expectedDelivery)
  const dem = Number(row.demandQuantity)
  if (Number.isNaN(cur) && Number.isNaN(del) && Number.isNaN(dem)) return row.estimatedInventory
  const c = Number.isNaN(cur) ? 0 : cur
  const d = Number.isNaN(del) ? 0 : del
  const q = Number.isNaN(dem) ? 0 : dem
  return c + d - q
}

export default function MaterialDemandFormPage() {
  const { user } = useAuth()
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const paramWeek = searchParams.get('week_start')
  const paramFactory = searchParams.get('factory')

  const weekOptions = useMemo(() => getWeekOptions(), [])
  const defaultWeek = weekOptions[0]?.value ?? ''

  const [factories, setFactories] = useState([])
  const [loadingFactories, setLoadingFactories] = useState(true)
  const [weekStart, setWeekStart] = useState(defaultWeek)
  const [factory, setFactory] = useState('')
  const [queryClicked, setQueryClicked] = useState(false)
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)

  const [editMode, setEditMode] = useState(false)
  const [purchaseQuantityById, setPurchaseQuantityById] = useState({})
  const [saving, setSaving] = useState(false)
  const [confirmingErp, setConfirmingErp] = useState(false)
  const [erpConfirmOpen, setErpConfirmOpen] = useState(false)
  const [vendors, setVendors] = useState([])
  const [vendorCode, setVendorCode] = useState('')
  /** 後端 lastEditSavedAt 原始值（待確認 ERP 時才有，即最後一次編輯儲存觸發時間） */
  const [lastEditSavedRaw, setLastEditSavedRaw] = useState(null)
  /** 0 待審核、1 審核完成、2 退回；無待確認列時為 null */
  const [reviewStatus, setReviewStatus] = useState(null)
  const [reviewApproveOpen, setReviewApproveOpen] = useState(false)
  const [reviewRejectOpen, setReviewRejectOpen] = useState(false)
  const [reviewActionLoading, setReviewActionLoading] = useState(false)

  const canView = hasPermission(user, 'material_demand.view')
  const canEdit = hasPermission(user, 'material_demand.edit')
  const canConfirmSendErp = hasPermission(user, 'confirm_data_send_erp')

  const applyPendingMeta = useCallback((meta) => {
    const raw = meta?.lastEditSavedAt
    setLastEditSavedRaw(raw != null && raw !== '' ? raw : null)
    const st = meta?.reviewStatus
    if (st === null || st === undefined || st === '') {
      setReviewStatus(null)
      return
    }
    const n = Number(st)
    setReviewStatus(Number.isNaN(n) ? null : n)
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoadingFactories(true)
    getWeeklyScheduleFactories()
      .then((list) => {
        if (!cancelled && Array.isArray(list) && list.length > 0) {
          setFactories(list)
          setFactory((prev) => (prev === '' ? list[0] : prev))
        }
      })
      .catch(() => {
        if (!cancelled) toast.error('無法取得廠區列表')
      })
      .finally(() => {
        if (!cancelled) setLoadingFactories(false)
      })
    return () => { cancelled = true }
  }, [toast])

  useEffect(() => {
    if (defaultWeek && !weekStart) setWeekStart(defaultWeek)
  }, [defaultWeek, weekStart])

  useEffect(() => {
    if (!canConfirmSendErp) return
    getVendorList()
      .then((list) => setVendors(Array.isArray(list) ? list : []))
      .catch(() => setVendors([]))
  }, [canConfirmSendErp])

  const hasAutoRun = useRef(false)
  useEffect(() => {
    if (!paramWeek || !paramFactory || factories.length === 0 || hasAutoRun.current) return
    if (!factories.includes(paramFactory)) return
    hasAutoRun.current = true
    setWeekStart(paramWeek)
    setFactory(paramFactory)
    setQueryClicked(true)
    setLoading(true)
    setLastEditSavedRaw(null)
    setReviewStatus(null)
    getMaterialDemand(paramWeek, paramFactory)
      .then(async (result) => {
        setData(Array.isArray(result) ? result : [])
        setPage(1)
        if (!result?.length) toast.info('查無資料')
        try {
          const meta = await getMaterialDemandLastEditSavedAt(paramWeek, paramFactory)
          applyPendingMeta(meta)
        } catch {
          setLastEditSavedRaw(null)
          setReviewStatus(null)
        }
      })
      .catch(() => {
        setData([])
        setLastEditSavedRaw(null)
        setReviewStatus(null)
        toast.error('查詢失敗')
      })
      .finally(() => setLoading(false))
  }, [paramWeek, paramFactory, factories, toast, applyPendingMeta])

  const runQueryRef = useCallback(async () => {
    if (!weekStart || !factory) return
    setLoading(true)
    setQueryClicked(true)
    setEditMode(false)
    setPurchaseQuantityById({})
    setLastEditSavedRaw(null)
    setReviewStatus(null)
    try {
      const result = await getMaterialDemand(weekStart, factory)
      setData(Array.isArray(result) ? result : [])
      setPage(1)
      if (!result?.length) toast.info('查無資料')
      try {
        const meta = await getMaterialDemandLastEditSavedAt(weekStart, factory)
        applyPendingMeta(meta)
      } catch {
        setLastEditSavedRaw(null)
        setReviewStatus(null)
      }
    } catch (err) {
      if (err.response?.status === 403) toast.error('您沒有權限檢視物料需求')
      else toast.error('查詢失敗')
      setData([])
      setLastEditSavedRaw(null)
      setReviewStatus(null)
    } finally {
      setLoading(false)
    }
  }, [weekStart, factory, toast, applyPendingMeta])

  const runQuery = useCallback(async () => {
    if (!weekStart || !factory) {
      toast.error('請選擇生產週與廠區')
      return
    }
    await runQueryRef()
  }, [weekStart, factory, toast, runQueryRef])

  const handleEditStart = () => {
    if (!canEdit || !data.length) return
    const initial = {}
    data.forEach((row) => {
      initial[row.id] = String(row.purchaseQuantity ?? 0)
    })
    setPurchaseQuantityById(initial)
    setEditMode(true)
  }

  const handleEditCancel = () => {
    setEditMode(false)
    setPurchaseQuantityById({})
  }

  const setPurchaseQuantity = (id, value) => {
    setPurchaseQuantityById((prev) => ({ ...prev, [id]: value }))
  }

  const parseDecimal = (v) => {
    if (v === '' || v == null) return null
    const n = parseFloat(String(v).replace(/,/g, ''))
    return Number.isNaN(n) ? null : n
  }

  const handleEditSave = async () => {
    const toUpdate = []
    for (const row of data) {
      const edited = purchaseQuantityById[row.id]
      if (edited === undefined) continue
      const parsed = parseDecimal(edited)
      const current = row.purchaseQuantity != null ? Number(row.purchaseQuantity) : null
      if (parsed !== null && parsed < 0) {
        toast.error('採購量不可為負')
        return
      }
      const same = parsed === null && (current === null || current === 0)
      const sameNum = parsed !== null && current !== null && Math.abs(parsed - current) < 1e-9
      if (!same && !sameNum) toUpdate.push({ row, parsed })
    }
    setSaving(true)
    try {
      for (const { row, parsed } of toUpdate) {
        await updateMaterialDemand(row.id, { purchaseQuantity: parsed })
      }
      if (toUpdate.length > 0) toast.success('儲存成功')
      setEditMode(false)
      setPurchaseQuantityById({})
      await runQuery()
    } catch (err) {
      toast.error(err.response?.data?.message || '儲存失敗')
    } finally {
      setSaving(false)
    }
  }

  const performConfirmSendErp = async () => {
    if (!weekStart || !factory || !vendorCode) return
    setConfirmingErp(true)
    try {
      await confirmSendErp(weekStart, factory, vendorCode)
      toast.success('已送ERP')
      setVendorCode('')
      await runQuery()
    } catch (err) {
      toast.error(err.response?.data?.message || '送出失敗')
    } finally {
      setConfirmingErp(false)
    }
  }

  const handleErpConfirmDialogConfirm = async () => {
    try {
      await performConfirmSendErp()
    } finally {
      setErpConfirmOpen(false)
    }
  }

  const performReviewApprove = async () => {
    if (!weekStart || !factory) return
    setReviewActionLoading(true)
    try {
      await reviewApproveMaterialDemand(weekStart, factory)
      toast.success('已審核完成')
      await runQueryRef()
    } catch (err) {
      toast.error(err.response?.data?.message || '審核失敗')
    } finally {
      setReviewActionLoading(false)
    }
  }

  const performReviewReject = async () => {
    if (!weekStart || !factory) return
    setReviewActionLoading(true)
    try {
      await reviewRejectMaterialDemand(weekStart, factory)
      toast.success('已退回')
      await runQueryRef()
    } catch (err) {
      toast.error(err.response?.data?.message || '退回失敗')
    } finally {
      setReviewActionLoading(false)
    }
  }

  const handleReviewApproveDialogConfirm = async () => {
    try {
      await performReviewApprove()
    } finally {
      setReviewApproveOpen(false)
    }
  }

  const handleReviewRejectDialogConfirm = async () => {
    try {
      await performReviewReject()
    } finally {
      setReviewRejectOpen(false)
    }
  }

  const handleExportCsv = () => {
    if (!data.length) {
      toast.info('無資料可匯出')
      return
    }
    const BOM = '\uFEFF'
    const headers = [
      '品號',
      '品名',
      '單位',
      '上次進貨日',
      '需求日',
      '現有庫存',
      '安全庫存量',
      '標準交期(天)',
      '預計進廠日',
      '預交量',
      '需求量',
      '預計庫存量',
      '採購量',
    ]
    const rows = data.map((r) => {
      const est = computedEstimatedInventory(r)
      return [
        r.materialCode ?? '',
        r.materialName ?? '',
        r.unit ?? '',
        r.lastPurchaseDate ?? '',
        r.demandDate ?? '',
        r.currentStock ?? 0,
        r.safetyStock ?? 0,
        r.standardLeadTime ?? '',
        r.expectedArrivalDate ?? '',
        r.expectedDelivery ?? 0,
        r.demandQuantity ?? 0,
        est == null || est === '' ? 0 : est,
        r.purchaseQuantity ?? 0,
      ]
    })
    const csv = BOM + [headers.join(','), ...rows.map((r) => r.join(','))].join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `物料需求數量_${weekStart}_${factory}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }

  const totalPages = Math.ceil(data.length / pageSize)
  const pageRows = data.slice((page - 1) * pageSize, page * pageSize)

  if (!canView) {
    return (
      <div className="material-demand-page">
        <h1>物料需求數量表單</h1>
        <div className="material-demand-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="material-demand-page">
      <h1>物料需求數量表單</h1>

      <section className="material-demand-block material-demand-block--query">
        <h2>查詢區</h2>
        <div className="material-demand-filters">
          <div className="filter-group">
            <label>生產週</label>
            <select
              className="form-select"
              value={weekStart}
              onChange={(e) => setWeekStart(e.target.value)}
              disabled={loading || loadingFactories}
            >
              {weekOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
          <div className="filter-group">
            <label>廠區</label>
            <select
              className="form-select"
              value={factory}
              onChange={(e) => setFactory(e.target.value)}
              disabled={loading || loadingFactories}
            >
              <option value="">請選擇廠區</option>
              {factories.map((f) => (
                <option key={f} value={f}>
                  {f}
                </option>
              ))}
            </select>
          </div>
          <div className="filter-group">
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => runQuery()}
              disabled={!weekStart || !factory || loading}
            >
              {loading ? '查詢中...' : '查詢'}
            </button>
          </div>
        </div>
      </section>

      {queryClicked && (
        <section className="material-demand-block material-demand-block--result">
          <h2>物料需求數量表單 結果</h2>
          {!loading && data.length > 0 && (
            <p className="material-demand-last-edit-saved">
              最後編輯儲存時間：
              {lastEditSavedRaw ? formatMaterialDemandSavedAt(lastEditSavedRaw) : '—'}
            </p>
          )}
          <div className="material-demand-result-toolbar">
            {canEdit && !editMode && (
              <button
                type="button"
                className="btn btn--outline"
                onClick={handleEditStart}
                disabled={!data.length || loading}
              >
                編輯
              </button>
            )}
            {canEdit && editMode && (
              <>
                <button
                  type="button"
                  className="btn btn--primary"
                  onClick={handleEditSave}
                  disabled={saving}
                >
                  {saving ? '儲存中...' : '儲存'}
                </button>
                <button
                  type="button"
                  className="btn btn--outline"
                  onClick={handleEditCancel}
                  disabled={saving}
                >
                  取消
                </button>
              </>
            )}
            {canConfirmSendErp &&
              !editMode &&
              data.length > 0 &&
              reviewStatus === REVIEW_STATUS_PENDING && (
                <>
                  <button
                    type="button"
                    className="btn btn--primary"
                    onClick={() => setReviewApproveOpen(true)}
                    disabled={reviewActionLoading}
                  >
                    審核
                  </button>
                  <button
                    type="button"
                    className="btn btn--outline"
                    onClick={() => setReviewRejectOpen(true)}
                    disabled={reviewActionLoading}
                  >
                    退回
                  </button>
                </>
              )}
            {canConfirmSendErp &&
              !editMode &&
              data.length > 0 &&
              reviewStatus === REVIEW_STATUS_APPROVED && (
                <button
                  type="button"
                  className="btn btn--primary"
                  onClick={() => {
                    setVendorCode('')
                    setErpConfirmOpen(true)
                  }}
                  disabled={confirmingErp}
                >
                  {confirmingErp ? '送出中...' : '本週資料確認無誤送出至天心ERP'}
                </button>
              )}
            <button
              type="button"
              className="btn btn--outline"
              onClick={handleExportCsv}
              disabled={!data.length}
            >
              Excel 匯出 (CSV)
            </button>
          </div>
          {loading ? (
            <div className="material-demand-loading">載入中...</div>
          ) : !data.length ? (
            <div className="material-demand-empty">查無資料</div>
          ) : (
            <>
              <div className="material-demand-table-wrapper">
                <table className="material-demand-table">
                  <thead>
                    <tr>
                      <th>品號</th>
                      <th>品名</th>
                      <th>單位</th>
                      <th>上次進貨日</th>
                      <th>需求日</th>
                      <th className="numeric-col">現有庫存</th>
                      <th className="numeric-col">安全庫存量</th>
                      <th className="numeric-col">標準交期(天)</th>
                      <th>預計進廠日</th>
                      <th className="numeric-col">預交量</th>
                      <th className="numeric-col">需求量</th>
                      <th className="numeric-col">預計庫存量</th>
                      <th className="numeric-col">採購量</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pageRows.map((row) => (
                      <tr key={row.id}>
                        <td>{row.materialCode}</td>
                        <td>{row.materialName}</td>
                        <td>{row.unit ?? '-'}</td>
                        <td>{row.lastPurchaseDate ?? '-'}</td>
                        <td>{row.demandDate ?? '-'}</td>
                        <td className="numeric-col">{formatDemandNumeric(row.currentStock)}</td>
                        <td className="numeric-col">{formatDemandNumeric(row.safetyStock)}</td>
                        <td className="numeric-col">{row.standardLeadTime ?? '-'}</td>
                        <td>{row.expectedArrivalDate ?? '-'}</td>
                        <td className="numeric-col">{formatDemandNumeric(row.expectedDelivery)}</td>
                        <td className="numeric-col">{formatDemandNumeric(row.demandQuantity)}</td>
                        <td className="numeric-col">{formatDemandNumeric(computedEstimatedInventory(row))}</td>
                        <td className="numeric-col">
                          {editMode ? (
                            <input
                              type="text"
                              className="material-demand-edit-input"
                              value={purchaseQuantityById[row.id] ?? '0'}
                              onChange={(e) => setPurchaseQuantity(row.id, e.target.value)}
                            />
                          ) : (
                            formatDemandNumeric(row.purchaseQuantity)
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="material-demand-pagination">
                <span className="material-demand-pagination-info">共 {data.length} 筆</span>
                <div className="material-demand-pagination-buttons">
                  <button
                    type="button"
                    onClick={() => setPage((p) => Math.max(1, p - 1))}
                    disabled={page === 1}
                  >
                    上一頁
                  </button>
                  <span className="material-demand-pagination-page">
                    第 {page} / {totalPages} 頁
                  </span>
                  <button
                    type="button"
                    onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                    disabled={page === totalPages}
                  >
                    下一頁
                  </button>
                </div>
                <div className="material-demand-pagination-size">
                  每頁
                  <select
                    value={pageSize}
                    onChange={(e) => { setPageSize(Number(e.target.value)); setPage(1) }}
                    className="form-select--sm"
                  >
                    <option value={10}>10</option>
                    <option value={20}>20</option>
                    <option value={50}>50</option>
                  </select>
                  筆
                </div>
              </div>
            </>
          )}
        </section>
      )}

      <ConfirmDialog
        open={reviewApproveOpen}
        title="審核"
        message="確定標記為審核完成？完成後可送出至天心ERP。"
        onConfirm={handleReviewApproveDialogConfirm}
        onCancel={() => {
          if (!reviewActionLoading) setReviewApproveOpen(false)
        }}
        loading={reviewActionLoading}
        confirmText="確認"
        confirmButtonClass="btn--primary"
      />
      <ConfirmDialog
        open={reviewRejectOpen}
        title="退回"
        message="確定退回？採購人員可修改後重新送審。"
        onConfirm={handleReviewRejectDialogConfirm}
        onCancel={() => {
          if (!reviewActionLoading) setReviewRejectOpen(false)
        }}
        loading={reviewActionLoading}
        confirmText="確認退回"
        confirmButtonClass="btn--outline"
      />
      <ConfirmDialog
        open={erpConfirmOpen}
        title="確認送出"
        message="請選擇廠商："
        onConfirm={handleErpConfirmDialogConfirm}
        onCancel={() => {
          if (!confirmingErp) {
            setErpConfirmOpen(false)
            setVendorCode('')
          }
        }}
        loading={confirmingErp}
        confirmDisabled={!vendorCode}
        confirmText="確認"
        confirmButtonClass="btn--primary"
      >
        <select
          className="form-select"
          value={vendorCode}
          onChange={(e) => setVendorCode(e.target.value)}
          disabled={confirmingErp}
        >
          <option value="">請選擇廠商</option>
          {vendors.map((v) => (
            <option key={v.code} value={v.code}>
              {v.name}
            </option>
          ))}
        </select>
      </ConfirmDialog>
    </div>
  )
}
