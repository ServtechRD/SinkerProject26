import { useState, useEffect, useCallback } from 'react'
import { listConfigs } from '../../api/forecastConfig'
import { uploadForecast, downloadTemplate } from '../../api/forecast'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import FileDropzone from '../../components/forecast/FileDropzone'
import './ForecastUpload.css'

const VALID_CHANNELS = [
  'PX/大全聯',
  '家樂福',
  '7-11',
  '全家',
  '萊爾富',
  'OK超商',
  '美廉社',
  '愛買',
  '大潤發',
  '好市多',
  '頂好',
  '楓康',
]

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

function formatMonth(monthStr) {
  if (!monthStr || monthStr.length !== 6) return monthStr
  const year = monthStr.substring(0, 4)
  const month = monthStr.substring(4, 6)
  const monthNames = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ]
  const monthIndex = parseInt(month, 10) - 1
  const monthName = monthNames[monthIndex] || ''
  return `${monthStr} (${monthName} ${year})`
}

export default function ForecastUploadPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(true)
  const [accessDenied, setAccessDenied] = useState(false)

  const [selectedMonth, setSelectedMonth] = useState('')
  const [selectedChannel, setSelectedChannel] = useState('')
  const [selectedFile, setSelectedFile] = useState(null)
  const [fileError, setFileError] = useState('')

  const [uploading, setUploading] = useState(false)
  const [downloadingTemplate, setDownloadingTemplate] = useState(false)

  const canUpload = hasPermission(user, 'sales_forecast.upload')

  const userChannels = user?.channels || []
  const canViewAllChannels = hasPermission(user, 'sales_forecast.view')
  const availableChannels = canViewAllChannels ? VALID_CHANNELS : VALID_CHANNELS.filter((ch) => userChannels.includes(ch))

  const fetchConfigs = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listConfigs()
      const openConfigs = data.filter((c) => !c.isClosed)
      const sorted = [...openConfigs].sort((a, b) => (b.month > a.month ? 1 : b.month < a.month ? -1 : 0))
      setConfigs(sorted)
      setAccessDenied(false)
    } catch (err) {
      if (err.response?.status === 403) {
        setAccessDenied(true)
      } else {
        toast.error('無法載入月份設定')
      }
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    fetchConfigs()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleFileChange = (file) => {
    setFileError('')

    if (!file) {
      setSelectedFile(null)
      return
    }

    // Validate file type
    const fileName = file.name.toLowerCase()
    if (!fileName.endsWith('.xlsx')) {
      setFileError('請上傳有效的 Excel 檔案 (.xlsx)')
      return
    }

    // Validate file size (10MB)
    const maxSize = 10 * 1024 * 1024
    if (file.size > maxSize) {
      setFileError('檔案大小超過 10MB 上限')
      return
    }

    setSelectedFile(file)
  }

  const handleDownloadTemplate = async () => {
    if (!selectedChannel) return

    setDownloadingTemplate(true)
    try {
      await downloadTemplate(selectedChannel)
    } catch (err) {
      toast.error('下載範本失敗')
    } finally {
      setDownloadingTemplate(false)
    }
  }

  const handleUpload = async () => {
    if (!selectedMonth || !selectedChannel || !selectedFile) return

    setUploading(true)
    try {
      const response = await uploadForecast(selectedFile, selectedMonth, selectedChannel)
      toast.success(`成功上傳 ${response.rows_processed} 筆資料`)
      // Clear form
      setSelectedMonth('')
      setSelectedChannel('')
      setSelectedFile(null)
      setFileError('')
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

  if (accessDenied || (!loading && !canUpload)) {
    return (
      <div className="forecast-upload-page">
        <h1>預測上傳</h1>
        <div className="forecast-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  const canSubmit = selectedMonth && selectedChannel && selectedFile && !fileError && !uploading

  return (
    <div className="forecast-upload-page">
      <h1>預測上傳</h1>

      {loading ? (
        <div className="forecast-loading" role="status">
          載入中...
        </div>
      ) : (
        <div className="upload-form">
          <div className="upload-form-field">
            <label htmlFor="month-select">月份</label>
            <select
              id="month-select"
              className="form-select"
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(e.target.value)}
              disabled={uploading}
            >
              <option value="">請選擇月份</option>
              {configs.length === 0 && <option disabled>無開放月份</option>}
              {configs.map((c) => (
                <option key={c.id} value={c.month}>
                  {formatMonth(c.month)}
                </option>
              ))}
            </select>
          </div>

          <div className="upload-form-field">
            <label htmlFor="channel-select">通路</label>
            <select
              id="channel-select"
              className="form-select"
              value={selectedChannel}
              onChange={(e) => setSelectedChannel(e.target.value)}
              disabled={uploading}
            >
              <option value="">請選擇通路</option>
              {availableChannels.map((ch) => (
                <option key={ch} value={ch}>
                  {ch}
                </option>
              ))}
            </select>
          </div>

          <div className="upload-form-field">
            <label>檔案</label>
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
              disabled={!selectedChannel || downloadingTemplate}
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
  )
}
