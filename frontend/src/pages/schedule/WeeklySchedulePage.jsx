import { useState, useCallback, useMemo, useEffect } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import {
  getWeeklyScheduleFactories,
  uploadWeeklySchedule,
  getWeeklySchedule,
  updateScheduleEntry,
  downloadWeeklyScheduleTemplate,
} from '../../api/weeklySchedule'
import FileDropzone from '../../components/forecast/FileDropzone'
import './WeeklySchedule.css'

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

const RESULT_PAGE_SIZE_OPTIONS = [10, 20, 50]

export default function WeeklySchedulePage() {
  const { user } = useAuth()
  const toast = useToast()

  const weekOptions = useMemo(() => getWeekOptions(), [])
  const defaultWeek = weekOptions[0]?.value ?? ''

  const [factories, setFactories] = useState([])
  const [loadingFactories, setLoadingFactories] = useState(true)
  const [weekStart, setWeekStart] = useState(defaultWeek)
  const [factory, setFactory] = useState('')
  const [queryClicked, setQueryClicked] = useState(false)
  const [scheduleData, setScheduleData] = useState([])
  const [loading, setLoading] = useState(false)

  const [selectedFile, setSelectedFile] = useState(null)
  const [fileError, setFileError] = useState('')
  const [uploading, setUploading] = useState(false)
  const [downloadingTemplate, setDownloadingTemplate] = useState(false)

  const [editingMode, setEditingMode] = useState(false)
  const [editValues, setEditValues] = useState({})
  const [savingEdit, setSavingEdit] = useState(false)

  const [resultPage, setResultPage] = useState(1)
  const [resultPageSize, setResultPageSize] = useState(20)

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

  const canView = hasPermission(user, 'weekly_schedule.view')
  const canUpload = hasPermission(user, 'weekly_schedule.upload')
  const canEdit = hasPermission(user, 'weekly_schedule.edit')

  const runQuery = useCallback(async () => {
    if (!weekStart || !factory) {
      toast.error('請選擇生產週與廠區')
      return
    }
    setLoading(true)
    setQueryClicked(true)
    try {
      const data = await getWeeklySchedule(weekStart, factory)
      setScheduleData(Array.isArray(data) ? data : [])
      setResultPage(1)
      if (!data?.length) toast.info('查無資料')
    } catch (err) {
      if (err.response?.status === 403) toast.error('您沒有權限檢視排程')
      else toast.error('查詢失敗')
      setScheduleData([])
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
      setFileError('請上傳有效的 Excel 檔案 (.xlsx)')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      setFileError('檔案大小超過 5MB 上限')
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
      await downloadWeeklyScheduleTemplate(factory)
      toast.success('範本下載成功')
    } catch (err) {
      const msg = err.response?.data?.message ?? err.response?.status === 403
        ? '無權限下載範本'
        : '下載範本失敗，請稍後再試'
      toast.error(msg)
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
      const response = await uploadWeeklySchedule(selectedFile, weekStart, factory)
      const count = response?.recordsInserted ?? response?.rows_inserted ?? response?.count ?? 0
      toast.success(`成功上傳 ${count} 筆資料`)
      setSelectedFile(null)
      setFileError('')
      const data = await getWeeklySchedule(weekStart, factory)
      setScheduleData(Array.isArray(data) ? data : [])
      setQueryClicked(true)
    } catch (err) {
      if (err.response?.data?.details && Array.isArray(err.response.data.details)) {
        toast.error(err.response.data.details.join('; '))
      } else if (err.response?.data?.error) {
        toast.error(err.response.data.error)
      } else {
        toast.error('上傳失敗，請重試')
      }
    } finally {
      setUploading(false)
    }
  }

  const handleEditStart = () => {
    if (!canEdit) return
    const initial = {}
    scheduleData.forEach((row) => {
      initial[row.id] = row.quantity != null ? String(row.quantity) : ''
    })
    setEditValues(initial)
    setEditingMode(true)
  }

  const handleEditCancel = () => {
    setEditingMode(false)
    setEditValues({})
  }

  const setEditQuantity = (id, value) => {
    setEditValues((prev) => ({ ...prev, [id]: value }))
  }

  const handleEditSave = async () => {
    const toUpdate = scheduleData.filter((row) => {
      const v = editValues[row.id]
      if (v === undefined) return false
      const num = parseFloat(v)
      return !Number.isNaN(num) && num >= 0 && String(row.quantity) !== v
    })
    if (toUpdate.length === 0) {
      setEditingMode(false)
      setEditValues({})
      return
    }
    setSavingEdit(true)
    try {
      for (const row of toUpdate) {
        await updateScheduleEntry(row.id, { quantity: parseFloat(editValues[row.id]) })
      }
      toast.success('儲存成功')
      const data = await getWeeklySchedule(weekStart, factory)
      setScheduleData(Array.isArray(data) ? data : [])
      setEditingMode(false)
      setEditValues({})
    } catch (err) {
      toast.error('儲存失敗')
    } finally {
      setSavingEdit(false)
    }
  }

  const downloadExcel = () => {
    const rows = scheduleData
    if (!rows.length) {
      toast.info('無資料可匯出')
      return
    }
    const BOM = '\uFEFF'
    const headers = ['需求日期', '品號', '品名', '庫位', '箱數小計']
    const csvRows = [headers.join(',')]
    rows.forEach((row) => {
      const demandDate = row.demandDate ?? ''
      const productCode = row.productCode ?? ''
      const productName = row.productName ?? ''
      const warehouseLocation = row.warehouseLocation ?? ''
      const qty = row.quantity != null ? row.quantity : ''
      csvRows.push([demandDate, productCode, productName, warehouseLocation, qty].join(','))
    })
    const blob = new Blob([BOM + csvRows.join('\r\n')], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `生產週排程_${weekStart}_${factory}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }

  const totalPages = Math.max(1, Math.ceil(scheduleData.length / resultPageSize))
  const pageStart = (resultPage - 1) * resultPageSize
  const pageRows = scheduleData.slice(pageStart, pageStart + resultPageSize)

  if (!canView) {
    return (
      <div className="weekly-schedule-page">
        <h1>生產週排程表單-匯入</h1>
        <div className="access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="weekly-schedule-page">
      <h1>生產週排程表單-匯入</h1>

      <section className="schedule-block schedule-block--query">
        <h2>查詢區</h2>
        <div className="schedule-form-row">
          <div className="schedule-form-field">
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
          <div className="schedule-form-field">
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
          <div className="schedule-form-actions">
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
        <section className="schedule-block schedule-block--upload">
          <h2>上傳區</h2>
          <p className="schedule-upload-hint">
            生產週：{weekStart || '-'}，廠區：{factory || '-'}
          </p>
          <div className="upload-form-field">
            <FileDropzone
              file={selectedFile}
              onFileChange={handleFileChange}
              error={fileError}
              disabled={uploading}
            />
          </div>
          <div className="upload-form-actions">
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
        <section className="schedule-block schedule-block--result">
          <h2>上傳結果</h2>
          {scheduleData.length > 0 ? (
            <>
              <div className="schedule-result-toolbar">
                {canEdit &&
                  (editingMode ? (
                    <>
                      <button
                        type="button"
                        className="btn btn--primary"
                        onClick={handleEditSave}
                        disabled={savingEdit}
                      >
                        {savingEdit ? '儲存中...' : '儲存'}
                      </button>
                      <button
                        type="button"
                        className="btn btn--outline"
                        onClick={handleEditCancel}
                        disabled={savingEdit}
                      >
                        取消
                      </button>
                    </>
                  ) : (
                    <button type="button" className="btn btn--outline" onClick={handleEditStart}>
                      編輯
                    </button>
                  ))}
                <button type="button" className="btn btn--outline" onClick={downloadExcel}>
                  Excel 匯出
                </button>
              </div>
              <div className="schedule-pagination-top">
                <select
                  className="form-select schedule-page-size"
                  value={resultPageSize}
                  onChange={(e) => {
                    setResultPageSize(Number(e.target.value))
                    setResultPage(1)
                  }}
                >
                  {RESULT_PAGE_SIZE_OPTIONS.map((n) => (
                    <option key={n} value={n}>
                      {n} 筆/頁
                    </option>
                  ))}
                </select>
                <span className="schedule-pagination-info">
                  第 {resultPage} / {totalPages} 頁，共 {scheduleData.length} 筆
                </span>
                <button
                  type="button"
                  className="btn btn--outline btn-sm"
                  disabled={resultPage <= 1}
                  onClick={() => setResultPage((p) => p - 1)}
                >
                  上一頁
                </button>
                <button
                  type="button"
                  className="btn btn--outline btn-sm"
                  disabled={resultPage >= totalPages}
                  onClick={() => setResultPage((p) => p + 1)}
                >
                  下一頁
                </button>
              </div>
              <div className="schedule-table-wrapper">
                <table className="schedule-table">
                  <thead>
                    <tr>
                      <th>需求日期</th>
                      <th>品號</th>
                      <th>品名</th>
                      <th>庫位</th>
                      <th>箱數小計</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pageRows.map((row) => (
                      <tr key={row.id}>
                        <td>{row.demandDate ?? '-'}</td>
                        <td>{row.productCode ?? '-'}</td>
                        <td>{row.productName ?? '-'}</td>
                        <td>{row.warehouseLocation ?? '-'}</td>
                        <td className={canEdit ? 'editable-quantity-cell' : ''}>
                          {editingMode ? (
                            <input
                              type="number"
                              min={0}
                              step={1}
                              className="cell-input"
                              value={editValues[row.id] ?? row.quantity ?? ''}
                              onChange={(e) => setEditQuantity(row.id, e.target.value)}
                            />
                          ) : (
                            row.quantity ?? '-'
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {totalPages > 1 && (
                <div className="schedule-pagination-bottom">
                  <button
                    type="button"
                    className="btn btn--outline btn-sm"
                    disabled={resultPage <= 1}
                    onClick={() => setResultPage((p) => p - 1)}
                  >
                    上一頁
                  </button>
                  <span>
                    第 {resultPage} / {totalPages} 頁
                  </span>
                  <button
                    type="button"
                    className="btn btn--outline btn-sm"
                    disabled={resultPage >= totalPages}
                    onClick={() => setResultPage((p) => p + 1)}
                  >
                    下一頁
                  </button>
                </div>
              )}
            </>
          ) : (
            <p className="schedule-no-data">查無資料</p>
          )}
        </section>
      )}
    </div>
  )
}
