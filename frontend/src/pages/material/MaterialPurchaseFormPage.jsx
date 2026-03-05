import { useState, useCallback, useMemo, useEffect } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import { getWeeklyScheduleFactories } from '../../api/weeklySchedule'
import {
  getMaterialPurchase,
  updateMaterialPurchase,
  uploadMaterialPurchase,
  downloadMaterialPurchaseTemplate,
} from '../../api/materialPurchase'
import FileDropzone from '../../components/forecast/FileDropzone'
import './MaterialPurchase.css'

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

export default function MaterialPurchaseFormPage() {
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
  const [editValues, setEditValues] = useState({
    kgPerBox: '',
    basketQuantity: '',
    boxesPerBarrel: '',
    requiredBarrels: '',
  })
  const [saving, setSaving] = useState(false)

  const canView = hasPermission(user, 'material_purchase.view')
  const canUpload = hasPermission(user, 'material_purchase.upload')
  const canEdit = hasPermission(user, 'material_purchase.edit')

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
      const result = await getMaterialPurchase(weekStart, factory)
      setData(Array.isArray(result) ? result : [])
      if (!result?.length) toast.info('查無資料')
    } catch (err) {
      if (err.response?.status === 403) toast.error('您沒有權限檢視物料採購')
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
      await downloadMaterialPurchaseTemplate(factory)
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
      const res = await uploadMaterialPurchase(selectedFile, weekStart, factory)
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
      kgPerBox: String(row.kgPerBox ?? ''),
      basketQuantity: String(row.basketQuantity ?? ''),
      boxesPerBarrel: String(row.boxesPerBarrel ?? ''),
      requiredBarrels: String(row.requiredBarrels ?? ''),
    })
  }

  const handleEditCancel = () => {
    setEditingId(null)
    setEditValues({ kgPerBox: '', basketQuantity: '', boxesPerBarrel: '', requiredBarrels: '' })
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
    const kgPerBox = parseDecimal(editValues.kgPerBox)
    const basketQuantity = parseDecimal(editValues.basketQuantity)
    const boxesPerBarrel = parseDecimal(editValues.boxesPerBarrel)
    const requiredBarrels = parseDecimal(editValues.requiredBarrels)
    if (kgPerBox != null && kgPerBox < 0) { toast.error('公斤/箱不可為負'); return }
    if (basketQuantity != null && basketQuantity < 0) { toast.error('籃數不可為負'); return }
    if (boxesPerBarrel != null && boxesPerBarrel < 0) { toast.error('箱/桶不可為負'); return }
    if (requiredBarrels != null && requiredBarrels < 0) { toast.error('所需桶數不可為負'); return }
    setSaving(true)
    try {
      const payload = {}
      if (kgPerBox !== null) payload.kgPerBox = kgPerBox
      if (basketQuantity !== null) payload.basketQuantity = basketQuantity
      if (boxesPerBarrel !== null) payload.boxesPerBarrel = boxesPerBarrel
      if (requiredBarrels !== null) payload.requiredBarrels = requiredBarrels
      await updateMaterialPurchase(rowId, payload)
      setData((prev) =>
        prev.map((r) =>
          r.id === rowId
            ? {
                ...r,
                kgPerBox: payload.kgPerBox ?? r.kgPerBox,
                basketQuantity: payload.basketQuantity ?? r.basketQuantity,
                boxesPerBarrel: payload.boxesPerBarrel ?? r.boxesPerBarrel,
                requiredBarrels: payload.requiredBarrels ?? r.requiredBarrels,
              }
            : r
        )
      )
      toast.success('儲存成功')
      setEditingId(null)
      setEditValues({ kgPerBox: '', basketQuantity: '', boxesPerBarrel: '', requiredBarrels: '' })
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
    const headers = ['品號', '品名', '箱數小計', '半成品名稱', '半成品編號', '公斤/箱', '籃數', '箱/桶', '所需桶數']
    const rows = data.map((r) => [
      r.productCode ?? '',
      r.productName ?? '',
      r.quantity ?? '',
      r.semiProductName ?? '',
      r.semiProductCode ?? '',
      r.kgPerBox ?? '',
      r.basketQuantity ?? '',
      r.boxesPerBarrel ?? '',
      r.requiredBarrels ?? '',
    ])
    const csv = BOM + [headers.join(','), ...rows.map((r) => r.join(','))].join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `物料採購數量_${weekStart}_${factory}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }

  if (!canView) {
    return (
      <div className="material-purchase-page">
        <h1>物料採購數量表單</h1>
        <div className="material-purchase-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="material-purchase-page">
      <h1>物料採購數量表單</h1>

      <section className="material-purchase-block material-purchase-block--query">
        <h2>查詢區</h2>
        <div className="material-purchase-filters">
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
        <section className="material-purchase-block material-purchase-block--upload">
          <h2>上傳區</h2>
          <p className="material-purchase-upload-hint">
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
              className="btn btn--secondary"
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
        <section className="material-purchase-block material-purchase-block--result">
          <h2>上傳結果</h2>
          <div className="material-purchase-result-toolbar">
            <button type="button" className="btn btn--secondary" onClick={handleExportCsv} disabled={!data.length}>
              Excel 匯出 (CSV)
            </button>
          </div>
          {loading ? (
            <div className="material-purchase-loading">載入中...</div>
          ) : !data.length ? (
            <div className="material-purchase-empty">查無資料</div>
          ) : (
            <div className="material-purchase-table-wrapper">
              <table className="material-purchase-table">
                <thead>
                  <tr>
                    <th>品號</th>
                    <th>品名</th>
                    <th className="numeric-col">箱數小計</th>
                    <th>半成品名稱</th>
                    <th>半成品編號</th>
                    <th className="numeric-col">公斤/箱</th>
                    <th className="numeric-col">籃數</th>
                    <th className="numeric-col">箱/桶</th>
                    <th className="numeric-col">所需桶數</th>
                    {canEdit && <th>操作</th>}
                  </tr>
                </thead>
                <tbody>
                  {data.map((row) => (
                    <tr key={row.id}>
                      <td>{row.productCode}</td>
                      <td>{row.productName}</td>
                      <td className="numeric-col">{formatNum(row.quantity)}</td>
                      <td>{row.semiProductName ?? '-'}</td>
                      <td>{row.semiProductCode ?? '-'}</td>
                      <td className="numeric-col">
                        {editingId === row.id ? (
                          <input
                            type="text"
                            className="material-purchase-edit-input"
                            value={editValues.kgPerBox}
                            onChange={(e) => setEdit('kgPerBox', e.target.value)}
                          />
                        ) : (
                          formatNum(row.kgPerBox)
                        )}
                      </td>
                      <td className="numeric-col">
                        {editingId === row.id ? (
                          <input
                            type="text"
                            className="material-purchase-edit-input"
                            value={editValues.basketQuantity}
                            onChange={(e) => setEdit('basketQuantity', e.target.value)}
                          />
                        ) : (
                          formatNum(row.basketQuantity)
                        )}
                      </td>
                      <td className="numeric-col">
                        {editingId === row.id ? (
                          <input
                            type="text"
                            className="material-purchase-edit-input"
                            value={editValues.boxesPerBarrel}
                            onChange={(e) => setEdit('boxesPerBarrel', e.target.value)}
                          />
                        ) : (
                          formatNum(row.boxesPerBarrel)
                        )}
                      </td>
                      <td className="numeric-col">
                        {editingId === row.id ? (
                          <input
                            type="text"
                            className="material-purchase-edit-input"
                            value={editValues.requiredBarrels}
                            onChange={(e) => setEdit('requiredBarrels', e.target.value)}
                          />
                        ) : (
                          formatNum(row.requiredBarrels)
                        )}
                      </td>
                      {canEdit && (
                        <td>
                          {editingId === row.id ? (
                            <>
                              <button
                                type="button"
                                className="btn btn--primary btn--small"
                                onClick={() => handleEditSave(row.id)}
                                disabled={saving}
                              >
                                儲存
                              </button>
                              <button
                                type="button"
                                className="btn btn--secondary btn--small"
                                onClick={handleEditCancel}
                                disabled={saving}
                              >
                                取消
                              </button>
                            </>
                          ) : (
                            <button type="button" className="btn btn--secondary btn--small" onClick={() => handleEditStart(row)}>
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
