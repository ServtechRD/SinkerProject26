import { useState, useCallback } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import {
  uploadWeeklySchedule,
  downloadWeeklyScheduleTemplate,
  getWeeklySchedule,
  updateScheduleEntry,
} from '../../api/weeklySchedule'
import WeekPicker from '../../components/WeekPicker'
import FileDropzone from '../../components/forecast/FileDropzone'
import './WeeklySchedule.css'

const FACTORIES = ['工廠A', '工廠B', '工廠C', '工廠D']

function hasPermission(user, perm) {
  if (user?.permissions && Array.isArray(user.permissions)) {
    return user.permissions.includes(perm)
  }
  if (user?.roleCode === 'admin') return true
  return false
}

export default function WeeklySchedulePage() {
  const { user } = useAuth()
  const toast = useToast()

  const [weekStart, setWeekStart] = useState('')
  const [factory, setFactory] = useState('')
  const [scheduleData, setScheduleData] = useState([])
  const [loading, setLoading] = useState(false)

  const [selectedFile, setSelectedFile] = useState(null)
  const [fileError, setFileError] = useState('')
  const [uploading, setUploading] = useState(false)
  const [downloadingTemplate, setDownloadingTemplate] = useState(false)

  const [editingCell, setEditingCell] = useState(null) // { id, field }
  const [editValue, setEditValue] = useState('')

  const canView = hasPermission(user, 'weekly_schedule.view')
  const canUpload = hasPermission(user, 'weekly_schedule.upload')
  const canEdit = hasPermission(user, 'weekly_schedule.edit')

  const handleQuery = useCallback(async () => {
    if (!weekStart || !factory) {
      toast.error('請選擇週次和工廠')
      return
    }

    setLoading(true)
    try {
      const data = await getWeeklySchedule(weekStart, factory)
      setScheduleData(data)
      if (data.length === 0) {
        toast.info('查無資料')
      }
    } catch (err) {
      if (err.response?.status === 403) {
        toast.error('您沒有權限檢視排程')
      } else {
        toast.error('查詢失敗')
      }
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

    const fileName = file.name.toLowerCase()
    if (!fileName.endsWith('.xlsx')) {
      setFileError('請上傳有效的 Excel 檔案 (.xlsx)')
      return
    }

    const maxSize = 5 * 1024 * 1024 // 5MB
    if (file.size > maxSize) {
      setFileError('檔案大小超過 5MB 上限')
      return
    }

    setSelectedFile(file)
  }

  const handleDownloadTemplate = async () => {
    if (!factory) {
      toast.error('請先選擇工廠')
      return
    }

    setDownloadingTemplate(true)
    try {
      await downloadWeeklyScheduleTemplate(factory)
    } catch (err) {
      toast.error('下載範本失敗')
    } finally {
      setDownloadingTemplate(false)
    }
  }

  const handleUpload = async () => {
    if (!weekStart || !factory || !selectedFile) {
      toast.error('請選擇週次、工廠及檔案')
      return
    }

    setUploading(true)
    try {
      const response = await uploadWeeklySchedule(selectedFile, weekStart, factory)
      toast.success(`成功上傳 ${response.rows_inserted || response.count || 0} 筆資料`)
      setSelectedFile(null)
      setFileError('')
      // Refresh data
      await handleQuery()
    } catch (err) {
      if (err.response?.data?.details && Array.isArray(err.response.data.details)) {
        const errorMessages = err.response.data.details.join('; ')
        toast.error(errorMessages)
      } else if (err.response?.data?.error) {
        toast.error(err.response.data.error)
      } else {
        toast.error('上傳失敗，請重試')
      }
    } finally {
      setUploading(false)
    }
  }

  const handleEditStart = (row, field) => {
    if (!canEdit) return
    setEditingCell({ id: row.id, field })
    setEditValue(field === 'demandDate' ? row.demandDate : String(row.quantity))
  }

  const handleEditCancel = () => {
    setEditingCell(null)
    setEditValue('')
  }

  const handleEditSave = async (row) => {
    const { field } = editingCell

    // Validate
    if (field === 'quantity') {
      const num = parseFloat(editValue)
      if (isNaN(num) || num < 0) {
        toast.error('數量必須為正數')
        return
      }
    }

    if (field === 'demandDate') {
      // Basic date validation YYYY-MM-DD
      if (!/^\d{4}-\d{2}-\d{2}$/.test(editValue)) {
        toast.error('日期格式錯誤 (YYYY-MM-DD)')
        return
      }
    }

    try {
      const updateData = {}
      if (field === 'demandDate') {
        updateData.demandDate = editValue
      } else if (field === 'quantity') {
        updateData.quantity = parseFloat(editValue)
      }

      await updateScheduleEntry(row.id, updateData)
      toast.success('更新成功')

      // Update local data
      setScheduleData((prev) =>
        prev.map((item) =>
          item.id === row.id
            ? {
                ...item,
                [field === 'demandDate' ? 'demandDate' : 'quantity']:
                  field === 'demandDate' ? editValue : parseFloat(editValue),
              }
            : item,
        ),
      )

      handleEditCancel()
    } catch (err) {
      toast.error('更新失敗')
    }
  }

  if (!canView) {
    return (
      <div className="weekly-schedule-page">
        <h1>週生產排程</h1>
        <div className="access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  const canSubmit = weekStart && factory && selectedFile && !fileError && !uploading

  return (
    <div className="weekly-schedule-page">
      <h1>週生產排程</h1>

      <div className="schedule-form">
        <div className="schedule-form-row">
          <div className="schedule-form-field">
            <label>週次</label>
            <WeekPicker value={weekStart} onChange={setWeekStart} disabled={loading || uploading} />
          </div>

          <div className="schedule-form-field">
            <label htmlFor="factory-select">工廠</label>
            <select
              id="factory-select"
              className="form-select"
              value={factory}
              onChange={(e) => setFactory(e.target.value)}
              disabled={loading || uploading}
            >
              <option value="">請選擇工廠</option>
              {FACTORIES.map((f) => (
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
              onClick={handleQuery}
              disabled={!weekStart || !factory || loading}
            >
              {loading ? '查詢中...' : '查詢'}
            </button>
          </div>
        </div>

        {canUpload && (
          <div className="schedule-upload-section">
            <h2>Excel 上傳</h2>
            <div className="upload-form-field">
              <FileDropzone file={selectedFile} onFileChange={handleFileChange} error={fileError} disabled={uploading} />
            </div>

            <div className="upload-form-actions">
              <button
                type="button"
                className="btn btn--outline"
                onClick={handleDownloadTemplate}
                disabled={!factory || downloadingTemplate}
              >
                {downloadingTemplate ? '下載中...' : '下載範本'}
              </button>
              <button type="button" className="btn btn--primary" onClick={handleUpload} disabled={!canSubmit}>
                {uploading ? (
                  <>
                    <span className="upload-spinner"></span>
                    上傳中...
                  </>
                ) : (
                  '上傳'
                )}
              </button>
            </div>
          </div>
        )}
      </div>

      {scheduleData.length > 0 && (
        <div className="schedule-table-container">
          <h2>排程資料</h2>
          <div className="schedule-table-wrapper">
            <table className="schedule-table">
              <thead>
                <tr>
                  <th>需求日期</th>
                  <th>品號</th>
                  <th>品名</th>
                  <th>庫位</th>
                  <th>數量</th>
                </tr>
              </thead>
              <tbody>
                {scheduleData.map((row) => (
                  <tr key={row.id}>
                    <td
                      className={canEdit ? 'editable-cell' : ''}
                      onClick={() => handleEditStart(row, 'demandDate')}
                    >
                      {editingCell?.id === row.id && editingCell?.field === 'demandDate' ? (
                        <input
                          type="text"
                          className="cell-input"
                          value={editValue}
                          onChange={(e) => setEditValue(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') handleEditSave(row)
                            if (e.key === 'Escape') handleEditCancel()
                          }}
                          onBlur={handleEditCancel}
                          autoFocus
                        />
                      ) : (
                        row.demandDate
                      )}
                    </td>
                    <td>{row.productCode}</td>
                    <td>{row.productName}</td>
                    <td>{row.warehouseLocation}</td>
                    <td
                      className={canEdit ? 'editable-cell' : ''}
                      onClick={() => handleEditStart(row, 'quantity')}
                    >
                      {editingCell?.id === row.id && editingCell?.field === 'quantity' ? (
                        <input
                          type="text"
                          className="cell-input"
                          value={editValue}
                          onChange={(e) => setEditValue(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') handleEditSave(row)
                            if (e.key === 'Escape') handleEditCancel()
                          }}
                          onBlur={handleEditCancel}
                          autoFocus
                        />
                      ) : (
                        row.quantity
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
