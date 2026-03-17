import { useState, useEffect, useCallback, useRef } from 'react'
import { listConfigs } from '../../api/forecastConfig'
import {
  uploadGiftForecast,
  downloadGiftTemplate,
  getGiftForecastVersions,
  getGiftForecastList,
  updateGiftForecastItem,
  createGiftForecastItem,
  copyGiftVersion,
  saveGiftVersionReason,
  deleteGiftVersion,
  getGiftVersionDiff,
} from '../../api/giftForecast'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import FileDropzone from '../../components/forecast/FileDropzone'
import AddItemDialog from '../../components/forecast/AddItemDialog'
import '../../components/ConfirmDialog.css'
import './ForecastUpload.css'
import './ForecastList.css'

const RESULT_PAGE_SIZE_OPTIONS = [10, 20, 50]
const VALID_CHANNELS = [
  'PX + 大全聯',
  '家樂福',
  '愛買',
  '7-11',
  '全家',
  'Ok+萊爾富',
  '好市多',
  '楓康',
  '美聯社',
  '康是美',
  '電商',
  '市面經銷',
]

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

function escapeCsvCell(val) {
  if (val == null) return ''
  const s = String(val)
  if (s.includes(',') || s.includes('"') || s.includes('\n')) {
    return '"' + s.replace(/"/g, '""') + '"'
  }
  return s
}

function downloadDefaultForecastTemplate(channel) {
  const BOM = '\uFEFF'
  const headers = ['中類名稱', '貨品規格', '品號', '品名', '庫位', '箱數小計']
  const csv = BOM + headers.join(',') + '\r\n'
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `銷售預估量範本_${channel || 'channel'}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

function downloadForecastCsv(data, filename) {
  const headers = ['中類名稱', '貨品規格', '品號', '品名', '庫位', '箱數小計']
  const rows = data.map((item) => [
    item.category ?? '',
    item.spec ?? '',
    (item.product_code ?? item.productCode) ?? '',
    (item.product_name ?? item.productName) ?? '',
    (item.warehouse_location ?? item.warehouseLocation) ?? '',
    item.quantity ?? '',
  ])
  const csvContent = [
    headers.map(escapeCsvCell).join(','),
    ...rows.map((row) => row.map(escapeCsvCell).join(',')),
  ].join('\n')
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename || 'sales_forecast_export.csv'
  link.click()
  URL.revokeObjectURL(url)
}

export default function GiftForecastUploadPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(true)
  const [accessDenied, setAccessDenied] = useState(false)

  // Block 1: 查詢
  const [selectedMonth, setSelectedMonth] = useState('')
  const [selectedChannel, setSelectedChannel] = useState('')
  const [queryClicked, setQueryClicked] = useState(false)
  const [loadingQuery, setLoadingQuery] = useState(false)
  const [versions, setVersions] = useState([])
  const [selectedVersion, setSelectedVersion] = useState('')
  const [forecastData, setForecastData] = useState([])
  const [loadingData, setLoadingData] = useState(false)

  // Block 2: 上傳
  const [selectedFile, setSelectedFile] = useState(null)
  const [fileError, setFileError] = useState('')
  const [uploading, setUploading] = useState(false)
  const [downloadingTemplate, setDownloadingTemplate] = useState(false)

  // Block 3: 編輯/新增/分頁
  const [editingId, setEditingId] = useState(null)
  const [editingValue, setEditingValue] = useState('')
  const [savingEdit, setSavingEdit] = useState(false)
  const [resultPage, setResultPage] = useState(1)
  const [resultPageSize, setResultPageSize] = useState(10)
  const [showAddDialog, setShowAddDialog] = useState(false)
  const [lastAddedIds, setLastAddedIds] = useState(new Set())
  const editInputRef = useRef(null)

  // UpdateAfterClosed: 編輯版次模式
  const [editingNewVersion, setEditingNewVersion] = useState(false)
  const [newVersionVersion, setNewVersionVersion] = useState('')
  const [showReasonDialog, setShowReasonDialog] = useState(false)
  const [reasonInput, setReasonInput] = useState('')
  const [savingVersionReason, setSavingVersionReason] = useState(false)
  const [showDiffDialog, setShowDiffDialog] = useState(false)
  const [diffData, setDiffData] = useState([])
  const [loadingDiff, setLoadingDiff] = useState(false)

  const canUpload = hasPermission(user, 'sales_forecast.upload')
  const canUpdateAfterClosed = hasPermission(user, 'sales_forecast.update_after_closed')
  const canCreate = hasPermission(user, 'sales_forecast.create')
  const canEdit = hasPermission(user, 'sales_forecast.edit')

  const userChannels = user?.channels || []
  const canViewAllChannels = hasPermission(user, 'sales_forecast.view')
  const availableChannels = canViewAllChannels
    ? VALID_CHANNELS
    : VALID_CHANNELS.filter((ch) => userChannels.includes(ch))

  const selectedConfig = configs.find((c) => c.month === selectedMonth)
  const isMonthOpen = selectedConfig && !selectedConfig.isClosed
  const isOwnerOfChannel = canViewAllChannels || userChannels.includes(selectedChannel)

  const canUploadSection = isOwnerOfChannel && selectedMonth && selectedChannel && isMonthOpen
  const canEditResult = (isOwnerOfChannel && isMonthOpen && canEdit) || (editingNewVersion && canEdit)
  const canAddResult = isOwnerOfChannel && ((isMonthOpen && canCreate) || (editingNewVersion && canCreate))

  const selectedVersionIndex = versions.indexOf(selectedVersion)
  const hasPreviousVersion = canUpdateAfterClosed && selectedVersion && selectedVersionIndex >= 0 && selectedVersionIndex < versions.length - 1

  const resultTotalPages = Math.max(1, Math.ceil(forecastData.length / resultPageSize))
  const resultStart = (resultPage - 1) * resultPageSize
  const pageData = forecastData.slice(resultStart, resultStart + resultPageSize)

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

  useEffect(() => {
    if (resultPage > resultTotalPages && resultTotalPages >= 1) {
      setResultPage(resultTotalPages)
    }
  }, [forecastData.length, resultTotalPages, resultPage, resultPageSize])

  const runQuery = useCallback(async (optVersion) => {
    if (!selectedMonth || !selectedChannel) return
    setQueryClicked(true)
    setLastAddedIds(new Set())
    setResultPage(1)
    setEditingNewVersion(false)
    setNewVersionVersion('')
    setLoadingQuery(true)
    setVersions([])
    setSelectedVersion('')
    setForecastData([])
    try {
      const data = await getGiftForecastVersions(selectedMonth, selectedChannel)
      const versionStrings = (Array.isArray(data) ? data : [])
        .map((item) => (typeof item === 'string' ? item : item?.version))
        .filter(Boolean)
      setVersions(versionStrings)
      const versionToUse = optVersion ?? (versionStrings.length > 0 ? versionStrings[0] : '')
      setSelectedVersion(versionToUse)
      setLoadingData(true)
      try {
        const list = await getGiftForecastList(selectedMonth, selectedChannel, versionToUse || undefined)
        setForecastData(Array.isArray(list) ? list : [])
      } catch (e) {
        toast.error('無法載入上傳結果')
        setForecastData([])
      } finally {
        setLoadingData(false)
      }
    } catch (err) {
      toast.error('查詢失敗')
      setVersions([])
      setForecastData([])
    } finally {
      setLoadingQuery(false)
    }
  }, [selectedMonth, selectedChannel, toast])

  const fetchResultList = useCallback(async () => {
    if (!selectedMonth || !selectedChannel || !selectedVersion) return
    setLoadingData(true)
    try {
      const list = await getGiftForecastList(selectedMonth, selectedChannel, selectedVersion)
      setForecastData(Array.isArray(list) ? list : [])
    } catch (err) {
      toast.error('無法載入上傳結果')
      setForecastData([])
    } finally {
      setLoadingData(false)
    }
  }, [selectedMonth, selectedChannel, selectedVersion, toast])

  useEffect(() => {
    if (selectedVersion && queryClicked && selectedMonth && selectedChannel) {
      fetchResultList()
    }
  }, [selectedVersion]) // eslint-disable-line react-hooks/exhaustive-deps

  const handleFileChange = (file) => {
    setFileError('')
    if (!file) {
      setSelectedFile(null)
      return
    }
    const ext = file.name.toLowerCase()
    if (!ext.endsWith('.xlsx') && !ext.endsWith('.csv')) {
      setFileError('請上傳 Excel (.xlsx) 或 CSV 檔案')
      return
    }
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
      await downloadGiftTemplate(selectedChannel)
      toast.success('範本下載成功')
    } catch (err) {
      let msg = err?.message || '下載範本失敗'
      if (err.response?.data instanceof Blob) {
        try {
          const text = await err.response.data.text()
          const body = JSON.parse(text)
          if (Array.isArray(body.details)) msg = body.details.join('; ')
          else if (body.message) msg = body.message
        } catch (_) {}
      }
      toast.error(msg)
    } finally {
      setDownloadingTemplate(false)
    }
  }

  const handleUpload = async () => {
    if (!selectedMonth || !selectedChannel || !selectedFile) return
    if (!isOwnerOfChannel) {
      toast.error('非負責的通路')
      return
    }
    setUploading(true)
    try {
      const response = await uploadGiftForecast(selectedFile, selectedMonth, selectedChannel)
      toast.success(`成功上傳 ${response.rows_processed} 筆資料`)
      setSelectedFile(null)
      setFileError('')
      await runQuery()
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

  const startEditing = (item) => {
    if (!canEditResult) return
    setEditingId(item.id)
    setEditingValue(String(item.quantity))
    setTimeout(() => editInputRef.current?.focus(), 0)
  }

  const cancelEditing = () => {
    setEditingId(null)
    setEditingValue('')
  }

  const saveEdit = async (id) => {
    const numValue = Number(editingValue)
    if (isNaN(numValue) || numValue < 0) {
      toast.error('箱數小計不可為負數')
      return
    }
    setSavingEdit(true)
    try {
      await updateGiftForecastItem(id, { quantity: numValue })
      toast.success('已儲存')
      await fetchResultList()
      cancelEditing()
    } catch (err) {
      toast.error(err.response?.data?.error || '儲存失敗，請重試')
    } finally {
      setSavingEdit(false)
    }
  }

  const handleEditKeyDown = (e, id) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      saveEdit(id)
    } else if (e.key === 'Escape') cancelEditing()
  }

  const handleAddSuccess = async (createdItem) => {
    toast.success('項目已新增')
    setShowAddDialog(false)
    if (createdItem?.id != null) {
      setLastAddedIds((prev) => new Set([...prev, createdItem.id]))
    }
    // 查詢為空時 selectedVersion 可能為 ''，fetchResultList 會直接 return；改跑 runQuery 重新取版本並載入列表
    await runQuery()
  }

  const handleExportExcel = () => {
    const name = `gift_sales_forecast_${selectedMonth}_${(selectedChannel || '').replace(/\//g, '_')}.csv`
    downloadForecastCsv(forecastData, name)
    toast.success('已匯出 CSV')
  }

  const handleStartEditVersion = async () => {
    if (!selectedMonth || !selectedChannel) return
    try {
      const res = await copyGiftVersion(selectedMonth, selectedChannel)
      setNewVersionVersion(res.version)
      setEditingNewVersion(true)
      setSelectedVersion(res.version)
      setVersions((prev) => [res.version, ...prev])
      setLoadingData(true)
      try {
        const list = await getGiftForecastList(selectedMonth, selectedChannel, res.version)
        setForecastData(Array.isArray(list) ? list : [])
      } catch (e) {
        toast.error('無法載入新版本資料')
        setForecastData([])
      } finally {
        setLoadingData(false)
      }
      toast.success('已建立新版本，可編輯或新增後儲存')
    } catch (err) {
      toast.error(err.response?.data?.message || err.response?.data?.error || '建立版本失敗')
    }
  }

  const handleSaveVersionWithReason = () => {
    setShowReasonDialog(true)
  }

  const handleConfirmReason = async () => {
    const reason = (reasonInput || '').trim()
    if (!reason) {
      toast.error('請輸入修改原因')
      return
    }
    if (!selectedMonth || !selectedChannel || !newVersionVersion) return
    setSavingVersionReason(true)
    try {
      await saveGiftVersionReason(selectedMonth, selectedChannel, newVersionVersion, reason)
      toast.success('已儲存版本與修改原因')
      setShowReasonDialog(false)
      setReasonInput('')
      setEditingNewVersion(false)
      setNewVersionVersion('')
      await runQuery()
    } catch (err) {
      toast.error(err.response?.data?.error || '儲存失敗')
    } finally {
      setSavingVersionReason(false)
    }
  }

  const handleCancelEditVersion = async () => {
    if (!selectedMonth || !selectedChannel || !newVersionVersion) return
    try {
      await deleteGiftVersion(selectedMonth, selectedChannel, newVersionVersion)
      toast.success('已取消，新版本資料已刪除')
      setEditingNewVersion(false)
      setNewVersionVersion('')
      await runQuery()
    } catch (err) {
      toast.error(err.response?.data?.error || '取消失敗')
    }
  }

  const handleShowDiff = async () => {
    if (!selectedMonth || !selectedChannel || !selectedVersion) return
    setShowDiffDialog(true)
    setLoadingDiff(true)
    setDiffData([])
    try {
      const list = await getGiftVersionDiff(selectedMonth, selectedChannel, selectedVersion)
      setDiffData(Array.isArray(list) ? list : [])
    } catch (err) {
      toast.error('無法載入差異')
      setDiffData([])
    } finally {
      setLoadingDiff(false)
    }
  }

  if (accessDenied || (!loading && !canUpload && !canUpdateAfterClosed)) {
    return (
      <div className="forecast-upload-page">
        <h1>禮品銷售預估量表單</h1>
        <div className="forecast-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="forecast-upload-page">
      <h1>禮品銷售預估量表單上傳</h1>

      {loading ? (
        <div className="forecast-loading" role="status">載入中...</div>
      ) : (
        <>
          {/* Block 1: 查詢 */}
          <section className="upload-block upload-block--query">
            <h2 className="upload-block-title">查詢</h2>
            <div className="upload-block-filters">
              <div className="filter-field">
                <label htmlFor="query-month">*銷售預估量月份</label>
                <select
                  id="query-month"
                  className="form-select"
                  value={selectedMonth}
                  onChange={(e) => {
                    setSelectedMonth(e.target.value)
                    setSelectedChannel('')
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
              <div className="filter-field">
                <label htmlFor="query-channel">*銷售預估量通路</label>
                <select
                  id="query-channel"
                  className="form-select"
                  value={selectedChannel}
                  onChange={(e) => {
                    setSelectedChannel(e.target.value)
                    setQueryClicked(false)
                  }}
                  disabled={!selectedMonth}
                >
                  <option value="">請選擇通路</option>
                  {availableChannels.map((ch) => (
                    <option key={ch} value={ch}>{ch}</option>
                  ))}
                </select>
              </div>
              {/* 版本選項在此頁隱藏，不顯示 */}
              <div className="filter-field filter-field--button">
                <label>&nbsp;</label>
                <button
                  type="button"
                  className="btn btn--primary"
                  onClick={() => runQuery()}
                  disabled={!selectedMonth || !selectedChannel || loadingQuery}
                >
                  {loadingQuery ? '查詢中...' : '查詢'}
                </button>
              </div>
            </div>
          </section>

          {/* Block 2: 禮品銷售預估量表單上傳 */}
          <section className="upload-block upload-block--upload">
            <h2 className="upload-block-title">禮品銷售預估量表單上傳</h2>
            <div className="upload-block-current">
              銷售預估量月份：{selectedMonth ? formatMonth(selectedMonth) : '—'}　銷售預估量通路：{selectedChannel || '—'}
            </div>
            <div className="upload-form">
              <div className="upload-form-field">
                <label>檔案</label>
                <FileDropzone
                  file={selectedFile}
                  onFileChange={handleFileChange}
                  error={fileError}
                  disabled={uploading || !canUploadSection}
                  accept=".xlsx,.csv"
                  acceptHint="支援 .xlsx、.csv，檔案大小上限 10MB"
                />
              </div>
              <div className="upload-form-actions">
                <button
                  type="button"
                  className="btn btn--outline"
                  onClick={handleDownloadTemplate}
                  disabled={!selectedChannel || downloadingTemplate}
                >
                  {downloadingTemplate ? '下載中...' : '下載 Excel 範本'}
                </button>
                <button
                  type="button"
                  className="btn btn--outline"
                  onClick={() => selectedChannel && downloadDefaultForecastTemplate(selectedChannel)}
                  disabled={!selectedChannel}
                >
                  下載 CSV 範本
                </button>
                <button
                  type="button"
                  className="btn btn--primary"
                  onClick={handleUpload}
                  disabled={!selectedMonth || !selectedChannel || !selectedFile || !!fileError || uploading || !canUploadSection}
                >
                  {uploading ? (
                    <>
                      <span className="upload-spinner"></span>
                      上傳中...
                    </>
                  ) : (
                    '上傳'
                  )}
                </button>
                {/* 此頁隱藏所有需 update_after_closed 權限的功能：編輯/儲存/取消（月份已結束時）不顯示 */}
              </div>
            </div>
            {!isMonthOpen && selectedMonth && (
              <p className="upload-block-hint">該月份已結束新增設定，無法上傳。</p>
            )}
            {selectedChannel && !isOwnerOfChannel && (
              <p className="upload-block-hint">您並非此通路負責人，無法上傳。</p>
            )}
          </section>

          {/* Block 3: 上傳結果 */}
          <section className="upload-block upload-block--result">
            <h2 className="upload-block-title">上傳結果</h2>
            {!queryClicked ? (
              <p className="upload-block-hint">請在上方選擇月份與通路並點擊「查詢」後顯示結果。</p>
            ) : (
              <>
                {loadingData ? (
                  <div className="forecast-loading" role="status">載入中...</div>
                ) : (
                  <>
                    {!isMonthOpen && selectedMonth && (
                      <div className="forecast-banner forecast-banner--readonly" role="alert">
                        該月份已結束新增設定，僅可檢視不可編輯與新增。
                      </div>
                    )}
                    {selectedChannel && !isOwnerOfChannel && (
                      <div className="forecast-banner forecast-banner--readonly" role="alert">
                        您並非此通路負責人，僅可檢視不可編輯與新增。
                      </div>
                    )}
                    {forecastData.length === 0 ? (
                      <>
                        {canAddResult && (
                          <div className="forecast-table-toolbar">
                            <button
                              type="button"
                              className="btn btn--primary btn--small"
                              onClick={() => setShowAddDialog(true)}
                            >
                              新增項目
                            </button>
                          </div>
                        )}
                        <div className="forecast-empty">尚無上傳資料</div>
                      </>
                    ) : (
                      <>
                        <div className="forecast-table-toolbar">
                          {canAddResult && (
                            <button
                              type="button"
                              className="btn btn--primary btn--small"
                              onClick={() => setShowAddDialog(true)}
                            >
                              新增項目
                            </button>
                          )}
                          <div className="forecast-table-toolbar-right">
                            <button
                              type="button"
                              className="btn btn--outline btn--small"
                              onClick={handleExportExcel}
                            >
                              匯出 Excel (CSV)
                            </button>
                          </div>
                        </div>
                        <div className="forecast-table-wrap">
                          <table className="forecast-table">
                            <thead>
                              <tr>
                                <th>中類名稱</th>
                                <th>貨品規格</th>
                                <th>品號</th>
                                <th>品名</th>
                                <th>庫位</th>
                                <th className="align-right">箱數小計</th>
                                {canEditResult && <th className="actions-cell">編輯</th>}
                              </tr>
                            </thead>
                            <tbody>
                              {pageData.map((item) => {
                                const isEditing = editingId === item.id
                                const isNewRow = lastAddedIds.has(item.id)
                                const isQuantityZero = item.quantity === 0 || item.quantity === '0'
                                return (
                                  <tr key={item.id} className={isNewRow ? 'row-added' : ''}>
                                    <td>{item.category || '-'}</td>
                                    <td>{item.spec || '-'}</td>
                                    <td>{(item.product_code ?? item.productCode) || '-'}</td>
                                    <td>{(item.product_name ?? item.productName) || '-'}</td>
                                    <td>{(item.warehouse_location ?? item.warehouseLocation) || '-'}</td>
                                    <td className={`align-right${isQuantityZero ? ' quantity-cell--zero' : ''}`}>
                                      {isEditing ? (
                                        <input
                                          ref={editInputRef}
                                          type="number"
                                          className="quantity-input"
                                          value={editingValue}
                                          onChange={(e) => setEditingValue(e.target.value)}
                                          onKeyDown={(e) => handleEditKeyDown(e, item.id)}
                                          min="0"
                                          disabled={savingEdit}
                                        />
                                      ) : (
                                        <span
                                          className={canEditResult ? 'quantity-editable' : ''}
                                          onClick={() => startEditing(item)}
                                          role={canEditResult ? 'button' : undefined}
                                          tabIndex={canEditResult ? 0 : undefined}
                                        >
                                          {item.quantity}
                                        </span>
                                      )}
                                    </td>
                                    {canEditResult && (
                                      <td className="actions-cell">
                                        {isEditing ? (
                                          <span className="upload-result-edit-actions">
                                            <button
                                              type="button"
                                              className="btn btn--small btn--primary"
                                              onClick={() => saveEdit(item.id)}
                                              disabled={savingEdit}
                                            >
                                              {savingEdit ? '儲存中...' : '儲存'}
                                            </button>
                                            <button
                                              type="button"
                                              className="btn btn--small btn--outline"
                                              onClick={cancelEditing}
                                              disabled={savingEdit}
                                            >
                                              取消
                                            </button>
                                          </span>
                                        ) : (
                                          <button
                                            type="button"
                                            className="btn btn--small btn--outline"
                                            onClick={() => startEditing(item)}
                                          >
                                            編輯
                                          </button>
                                        )}
                                      </td>
                                    )}
                                  </tr>
                                )
                              })}
                            </tbody>
                          </table>
                        </div>
                        <div className="forecast-pagination">
                          <div className="filter-field filter-field--inline">
                            <select
                              id="result-page-size"
                              className="form-select form-select--sm"
                              value={resultPageSize}
                              aria-label="每頁筆數"
                              onChange={(e) => {
                                const val = Number(e.target.value)
                                setResultPageSize(val)
                                setResultPage(1)
                              }}
                            >
                              {RESULT_PAGE_SIZE_OPTIONS.map((n) => (
                                <option key={n} value={n}>{n}</option>
                              ))}
                            </select>
                          </div>
                          <span className="forecast-pagination-info">
                            第 {resultStart + 1}–{Math.min(resultStart + resultPageSize, forecastData.length)} 筆，共 {forecastData.length} 筆
                          </span>
                          <div className="forecast-pagination-buttons">
                            <button
                              type="button"
                              className="btn btn--small btn--outline"
                              disabled={resultPage <= 1}
                              onClick={() => setResultPage((p) => Math.max(1, p - 1))}
                            >
                              上一頁
                            </button>
                            <span className="forecast-pagination-page">
                              第 {resultPage} / {resultTotalPages} 頁
                            </span>
                            <button
                              type="button"
                              className="btn btn--small btn--outline"
                              disabled={resultPage >= resultTotalPages}
                              onClick={() => setResultPage((p) => Math.min(resultTotalPages, p + 1))}
                            >
                              下一頁
                            </button>
                          </div>
                        </div>
                      </>
                    )}
                  </>
                )}
              </>
            )}
          </section>

          <AddItemDialog
            open={showAddDialog}
            month={selectedMonth}
            channel={selectedChannel}
            onClose={() => setShowAddDialog(false)}
            onSuccess={handleAddSuccess}
            createItemApi={createGiftForecastItem}
          />

          {showReasonDialog && (
            <div className="dialog-overlay" role="dialog" aria-modal="true" onClick={() => !savingVersionReason && setShowReasonDialog(false)}>
              <div className="dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 480 }}>
                <h3 className="dialog-title">修改原因</h3>
                <div className="upload-form-field">
                  <label htmlFor="reason-input">請輸入修改原因</label>
                  <textarea
                    id="reason-input"
                    className="form-input"
                    rows={4}
                    value={reasonInput}
                    onChange={(e) => setReasonInput(e.target.value)}
                    placeholder="輸入本次版本修改原因..."
                    disabled={savingVersionReason}
                  />
                </div>
                <div className="dialog-actions">
                  <button
                    type="button"
                    className="btn btn--outline"
                    onClick={() => !savingVersionReason && setShowReasonDialog(false)}
                    disabled={savingVersionReason}
                  >
                    取消
                  </button>
                  <button
                    type="button"
                    className="btn btn--primary"
                    onClick={handleConfirmReason}
                    disabled={savingVersionReason}
                  >
                    {savingVersionReason ? '儲存中...' : '確定'}
                  </button>
                </div>
              </div>
            </div>
          )}

          {showDiffDialog && (
            <div className="dialog-overlay" role="dialog" aria-modal="true" onClick={() => setShowDiffDialog(false)}>
              <div className="dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 860 }}>
                <h3 className="dialog-title">和前版差異</h3>
                {loadingDiff ? (
                  <div className="forecast-loading">載入中...</div>
                ) : diffData.length === 0 ? (
                  <p className="upload-block-hint">目前版次與前一版數量皆相同，無差異。</p>
                ) : (
                  <div className="forecast-table-wrap">
                    <table className="forecast-table">
                      <thead>
                        <tr>
                          <th>中類名稱</th>
                          <th>貨品規格</th>
                          <th>品號</th>
                          <th>品名</th>
                          <th>庫位</th>
                          <th className="align-right">本版箱數</th>
                          <th className="align-right">前版箱數</th>
                        </tr>
                      </thead>
                      <tbody>
                        {diffData.map((row, i) => (
                          <tr key={i}>
                            <td>{row.category ?? '-'}</td>
                            <td>{row.spec ?? '-'}</td>
                            <td>{row.product_code ?? row.productCode ?? '-'}</td>
                            <td>{row.product_name ?? row.productName ?? '-'}</td>
                            <td>{row.warehouse_location ?? row.warehouseLocation ?? '-'}</td>
                            <td className="align-right">{row.current_quantity ?? row.currentQuantity ?? '-'}</td>
                            <td className="align-right">{row.previous_quantity ?? row.previousQuantity ?? '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                <div className="dialog-actions" style={{ marginTop: 16 }}>
                  <button type="button" className="btn btn--primary" onClick={() => setShowDiffDialog(false)}>
                    關閉
                  </button>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
