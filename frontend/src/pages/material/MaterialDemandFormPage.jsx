import { useState, useCallback, useMemo, useEffect } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import { getWeeklyScheduleFactories } from '../../api/weeklySchedule'
import {
  getMaterialDemand,
  updateMaterialDemand,
  uploadMaterialDemand,
  downloadMaterialDemandTemplate,
} from '../../api/materialDemand'
import FileDropzone from '../../components/forecast/FileDropzone'
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

function formatNum(v) {
  if (v == null) return '-'
  const n = Number(v)
  if (Number.isNaN(n)) return '-'
  return n.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

export default function MaterialDemandFormPage() {
  const { user } = useAuth()
  const toast = useToast()

  const weekOptions = useMemo(() => getWeekOptions(), [])
  const defaultWeek = weekOptions[0]?.value ?? ''

  const [factories, setFactories] = useState([])
  const [loadingFactories, setLoadingFactories] = useState(true)
  const [weekStart, setWeekStart] = useState(defaultWeek)
  const [factory, setFactory] = useState('')
  const [queryClicked, setQueryClicked] = useState(false)
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)

  const [selectedFile, setSelectedFile] = useState(null)
  const [fileError, setFileError] = useState('')
  const [uploading, setUploading] = useState(false)
  const [downloadingTemplate, setDownloadingTemplate] = useState(false)

  const [editingId, setEditingId] = useState(null)
  const [editValues, setEditValues] = useState({ expectedDelivery: '', demandQuantity: '', estimatedInventory: '' })
  const [saving, setSaving] = useState(false)

  const canView = hasPermission(user, 'material_demand.view')
  const canUpload = hasPermission(user, 'material_demand.upload')
  const canEdit = hasPermission(user, 'material_demand.edit')

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

  const runQuery = useCallback(async () => {
    if (!weekStart || !factory) {
      toast.error('請選擇生產週與廠區')
      return
    }
    setLoading(true)
    setQueryClicked(true)
    try {
      const result = await getMaterialDemand(weekStart, factory)
      setData(Array.isArray(result) ? result : [])
      if (!result?.length) toast.info('查無資料')
    } catch (err) {
      if (err.response?.status === 403) toast.error('您沒有權限檢視物料需求')
      else toast.error('查詢失敗')
      setData([])
    } finally {
      setLoading(false)
    }
  }, [weekStart, factory, toast])

  const handleFileChange = (file) => {
    setFileError('')
    if (!file) {
      setSelectedFile(null)
      return
    }
    if (!file.name.toLowerCase().endsWith('.xlsx')) {
      setFileError('請上傳 .xlsx 檔案')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      setFileError('檔案大小超過 10MB')
      return
    }
    setSelectedFile(file)
  }

  const handleDownloadTemplate = async () => {
    if (!factory) {
      toast.error('請先選擇廠區')
      return
    }
    setDownloadingTemplate(true)
    try {
      await downloadMaterialDemandTemplate(factory)
    } catch (err) {
      toast.error('下載範本失敗')
    } finally {
      setDownloadingTemplate(false)
    }
  }

  const handleUpload = async () => {
    if (!weekStart || !factory || !selectedFile) {
      toast.error('請選擇生產週、廠區及檔案')
      return
    }
    setUploading(true)
    try {
      const res = await uploadMaterialDemand(selectedFile, weekStart, factory)
      const count = res?.count ?? 0
      toast.success(`成功上傳 ${count} 筆資料`)
      setSelectedFile(null)
      setFileError('')
      await runQuery()
    } catch (err) {
      if (err.response?.data?.details && Array.isArray(err.response.data.details)) {
        toast.error(err.response.data.details.join('; '))
      } else {
        toast.error(err.response?.data?.message || '上傳失敗')
      }
    } finally {
      setUploading(false)
    }
  }

  const handleEditStart = (row) => {
    if (!canEdit) return
    setEditingId(row.id)
    setEditValues({
      expectedDelivery: String(row.expectedDelivery ?? ''),
      demandQuantity: String(row.demandQuantity ?? ''),
      estimatedInventory: String(row.estimatedInventory ?? ''),
    })
  }

  const handleEditCancel = () => {
    setEditingId(null)
    setEditValues({ expectedDelivery: '', demandQuantity: '', estimatedInventory: '' })
  }

  const setEdit = (field, value) => {
    setEditValues((prev) => ({ ...prev, [field]: value }))
  }

  const parseDecimal = (v) => {
    if (v === '' || v == null) return null
    const n = parseFloat(String(v).replace(/,/g, ''))
    return Number.isNaN(n) ? null : n
  }

  const handleEditSave = async (rowId) => {
    const expectedDelivery = parseDecimal(editValues.expectedDelivery)
    const demandQuantity = parseDecimal(editValues.demandQuantity)
    const estimatedInventory = parseDecimal(editValues.estimatedInventory)
    if (expectedDelivery != null && expectedDelivery < 0) {
      toast.error('預交量不可為負')
      return
    }
    if (demandQuantity != null && demandQuantity < 0) {
      toast.error('需求量不可為負')
      return
    }
    if (estimatedInventory != null && estimatedInventory < 0) {
      toast.error('預計庫存量不可為負')
      return
    }
    setSaving(true)
    try {
      const payload = {}
      if (expectedDelivery !== null) payload.expectedDelivery = expectedDelivery
      if (demandQuantity !== null) payload.demandQuantity = demandQuantity
      if (estimatedInventory !== null) payload.estimatedInventory = estimatedInventory
      await updateMaterialDemand(rowId, payload)
      setData((prev) =>
        prev.map((r) =>
          r.id === rowId
            ? {
                ...r,
                expectedDelivery: payload.expectedDelivery ?? r.expectedDelivery,
                demandQuantity: payload.demandQuantity ?? r.demandQuantity,
                estimatedInventory: payload.estimatedInventory ?? r.estimatedInventory,
              }
            : r
        )
      )
      toast.success('儲存成功')
      setEditingId(null)
      setEditValues({ expectedDelivery: '', demandQuantity: '', estimatedInventory: '' })
    } catch (err) {
      toast.error(err.response?.data?.message || '儲存失敗')
    } finally {
      setSaving(false)
    }
  }

  const handleExportCsv = () => {
    if (!data.length) {
      toast.info('無資料可匯出')
      return
    }
    const BOM = '\uFEFF'
    const headers = ['品號', '品名', '單位', '上次進貨日', '需求日', '預交量', '需求量', '預計庫存量']
    const rows = data.map((r) => [
      r.materialCode ?? '',
      r.materialName ?? '',
      r.unit ?? '',
      r.lastPurchaseDate ?? '',
      r.demandDate ?? '',
      r.expectedDelivery ?? '',
      r.demandQuantity ?? '',
      r.estimatedInventory ?? '',
    ])
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
              disabled={loading || uploading || loadingFactories}
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
              disabled={loading || uploading || loadingFactories}
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
              onClick={runQuery}
              disabled={!weekStart || !factory || loading}
            >
              {loading ? '查詢中...' : '查詢'}
            </button>
          </div>
        </div>
      </section>

      {canUpload && (
        <section className="material-demand-block material-demand-block--upload">
          <h2>上傳區</h2>
          <p className="material-demand-upload-hint">
            生產週：{weekStart || '-'}，廠區：{factory || '-'}
          </p>
          <div className="upload-area">
            <FileDropzone
              file={selectedFile}
              onFileChange={handleFileChange}
              error={fileError}
              disabled={uploading}
            />
          </div>
          <div className="upload-actions">
            <button
              type="button"
              className="btn btn--outline"
              onClick={handleDownloadTemplate}
              disabled={!factory || downloadingTemplate}
            >
              {downloadingTemplate ? '下載中...' : '範本下載'}
            </button>
            <button
              type="button"
              className="btn btn--primary"
              onClick={handleUpload}
              disabled={!weekStart || !factory || !selectedFile || !!fileError || uploading}
            >
              {uploading ? (
                <>
                  <span className="upload-spinner" />
                  上傳中...
                </>
              ) : (
                '上傳'
              )}
            </button>
          </div>
        </section>
      )}

      {queryClicked && (
        <section className="material-demand-block material-demand-block--result">
          <h2>上傳結果</h2>
          <div className="material-demand-result-toolbar">
            <button type="button" className="btn btn--outline" onClick={handleExportCsv} disabled={!data.length}>
              Excel 匯出 (CSV)
            </button>
          </div>
          {loading ? (
            <div className="material-demand-loading">載入中...</div>
          ) : !data.length ? (
            <div className="material-demand-empty">查無資料</div>
          ) : (
            <div className="material-demand-table-wrapper">
              <table className="material-demand-table">
                <thead>
                  <tr>
                    <th>品號</th>
                    <th>品名</th>
                    <th>單位</th>
                    <th>上次進貨日</th>
                    <th>需求日</th>
                    <th className="numeric-col">預交量</th>
                    <th className="numeric-col">需求量</th>
                    <th className="numeric-col">預計庫存量</th>
                    {canEdit && <th>操作</th>}
                  </tr>
                </thead>
                <tbody>
                  {data.map((row) => (
                    <tr key={row.id}>
                      <td>{row.materialCode}</td>
                      <td>{row.materialName}</td>
                      <td>{row.unit ?? '-'}</td>
                      <td>{row.lastPurchaseDate ?? '-'}</td>
                      <td>{row.demandDate ?? '-'}</td>
                      <td className="numeric-col">
                        {editingId === row.id ? (
                          <input
                            type="text"
                            className="material-demand-edit-input"
                            value={editValues.expectedDelivery}
                            onChange={(e) => setEdit('expectedDelivery', e.target.value)}
                          />
                        ) : (
                          formatNum(row.expectedDelivery)
                        )}
                      </td>
                      <td className="numeric-col">
                        {editingId === row.id ? (
                          <input
                            type="text"
                            className="material-demand-edit-input"
                            value={editValues.demandQuantity}
                            onChange={(e) => setEdit('demandQuantity', e.target.value)}
                          />
                        ) : (
                          formatNum(row.demandQuantity)
                        )}
                      </td>
                      <td className="numeric-col">
                        {editingId === row.id ? (
                          <input
                            type="text"
                            className="material-demand-edit-input"
                            value={editValues.estimatedInventory}
                            onChange={(e) => setEdit('estimatedInventory', e.target.value)}
                          />
                        ) : (
                          formatNum(row.estimatedInventory)
                        )}
                      </td>
                      {canEdit && (
                        <td>
                          {editingId === row.id ? (
                            <>
                              <button
                                type="button"
                                className="btn btn--primary btn-sm"
                                onClick={() => handleEditSave(row.id)}
                                disabled={saving}
                              >
                                儲存
                              </button>
                              <button
                                type="button"
                                className="btn btn--outline btn-sm"
                                onClick={handleEditCancel}
                                disabled={saving}
                              >
                                取消
                              </button>
                            </>
                          ) : (
                            <button type="button" className="btn btn--outline btn-sm" onClick={() => handleEditStart(row)}>
                              編輯
                            </button>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}
    </div>
  )
}
