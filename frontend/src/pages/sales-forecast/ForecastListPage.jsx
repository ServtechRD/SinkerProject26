import { useState, useEffect, useCallback, useRef } from 'react'
import { listConfigs } from '../../api/forecastConfig'
import {
  getForecastVersions,
  getForecastList,
  updateForecastItem,
  deleteForecastItem,
} from '../../api/forecast'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import AddItemDialog from '../../components/forecast/AddItemDialog'
import ConfirmDialog from '../../components/ConfirmDialog'
import './ForecastList.css'

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

export default function ForecastListPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(true)
  const [accessDenied, setAccessDenied] = useState(false)

  const [selectedMonth, setSelectedMonth] = useState('')
  const [selectedChannel, setSelectedChannel] = useState('')
  const [selectedVersion, setSelectedVersion] = useState('')

  const [versions, setVersions] = useState([])
  const [loadingVersions, setLoadingVersions] = useState(false)

  const [forecastData, setForecastData] = useState([])
  const [loadingData, setLoadingData] = useState(false)

  const [editingId, setEditingId] = useState(null)
  const [editingValue, setEditingValue] = useState('')

  const [showAddDialog, setShowAddDialog] = useState(false)

  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  const editInputRef = useRef(null)

  const canView = hasPermission(user, 'sales_forecast.view') || hasPermission(user, 'sales_forecast.view_own')
  const canCreate = hasPermission(user, 'sales_forecast.create')
  const canEdit = hasPermission(user, 'sales_forecast.edit')
  const canDelete = hasPermission(user, 'sales_forecast.delete')

  const userChannels = user?.channels || []
  const canViewAllChannels = hasPermission(user, 'sales_forecast.view')
  const availableChannels = canViewAllChannels
    ? VALID_CHANNELS
    : VALID_CHANNELS.filter((ch) => userChannels.includes(ch))

  const selectedConfig = configs.find((c) => c.month === selectedMonth)
  const isMonthClosed = selectedConfig?.isClosed || false
  const canEditWhenClosed = hasPermission(user, 'sales_forecast.edit_closed')

  const isReadOnly = isMonthClosed && !canEditWhenClosed

  const canPerformActions = {
    create: canCreate && !isReadOnly,
    edit: canEdit && !isReadOnly,
    delete: canDelete && !isReadOnly,
  }

  const fetchConfigs = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listConfigs()
      const sorted = [...data].sort((a, b) => (b.month > a.month ? 1 : b.month < a.month ? -1 : 0))
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

  const fetchVersions = useCallback(
    async (month, channel) => {
      if (!month || !channel) return

      setLoadingVersions(true)
      try {
        const data = await getForecastVersions(month, channel)
        setVersions(data)

        if (data.length > 0) {
          setSelectedVersion(data[0])
        } else {
          setSelectedVersion('')
        }
      } catch (err) {
        toast.error('無法載入版本資料')
        setVersions([])
        setSelectedVersion('')
      } finally {
        setLoadingVersions(false)
      }
    },
    [toast]
  )

  useEffect(() => {
    if (selectedMonth && selectedChannel) {
      fetchVersions(selectedMonth, selectedChannel)
    } else {
      setVersions([])
      setSelectedVersion('')
    }
  }, [selectedMonth, selectedChannel]) // eslint-disable-line react-hooks/exhaustive-deps

  const fetchData = useCallback(
    async (month, channel, version) => {
      if (!month || !channel || !version) return

      setLoadingData(true)
      try {
        const data = await getForecastList(month, channel, version)
        setForecastData(data)
      } catch (err) {
        if (err.response?.status === 403) {
          toast.error('您沒有權限檢視此資料')
        } else {
          toast.error('無法載入預測資料')
        }
        setForecastData([])
      } finally {
        setLoadingData(false)
      }
    },
    [toast]
  )

  useEffect(() => {
    if (selectedMonth && selectedChannel && selectedVersion) {
      fetchData(selectedMonth, selectedChannel, selectedVersion)
    } else {
      setForecastData([])
    }
  }, [selectedMonth, selectedChannel, selectedVersion]) // eslint-disable-line react-hooks/exhaustive-deps

  const handleMonthChange = (month) => {
    setSelectedMonth(month)
    setSelectedChannel('')
    setSelectedVersion('')
    setVersions([])
    setForecastData([])
  }

  const handleChannelChange = (channel) => {
    setSelectedChannel(channel)
    setSelectedVersion('')
    setVersions([])
    setForecastData([])
  }

  const handleVersionChange = (version) => {
    setSelectedVersion(version)
  }

  const startEditing = (item) => {
    if (!canPerformActions.edit) return
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
      toast.error('數量必須為正數')
      return
    }

    try {
      await updateForecastItem(id, { quantity: numValue })
      toast.success('數量已更新')
      await fetchData(selectedMonth, selectedChannel, selectedVersion)
      cancelEditing()
    } catch (err) {
      if (err.response?.data?.error) {
        toast.error(err.response.data.error)
      } else {
        toast.error('更新失敗，請重試')
      }
    }
  }

  const handleEditKeyDown = (e, id) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      saveEdit(id)
    } else if (e.key === 'Escape') {
      cancelEditing()
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return

    setDeleting(true)
    try {
      await deleteForecastItem(deleteTarget.id)
      toast.success('項目已刪除')
      setDeleteTarget(null)
      await fetchData(selectedMonth, selectedChannel, selectedVersion)
    } catch (err) {
      if (err.response?.data?.error) {
        toast.error(err.response.data.error)
      } else {
        toast.error('刪除失敗，請重試')
      }
    } finally {
      setDeleting(false)
    }
  }

  const handleAddSuccess = async () => {
    toast.success('項目已新增')
    setShowAddDialog(false)
    await fetchData(selectedMonth, selectedChannel, selectedVersion)
  }

  if (accessDenied || (!loading && !canView)) {
    return (
      <div className="forecast-list-page">
        <h1>銷售預測</h1>
        <div className="forecast-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  const latestVersion = versions.length > 0 ? versions[0] : null

  return (
    <div className="forecast-list-page">
      <div className="forecast-page-header">
        <h1>銷售預測</h1>
        {canPerformActions.create && selectedMonth && selectedChannel && (
          <button className="btn btn--primary" onClick={() => setShowAddDialog(true)}>
            新增項目
          </button>
        )}
      </div>

      {isReadOnly && (
        <div className="forecast-banner forecast-banner--readonly" role="alert">
          月份已關帳，資料為唯讀
        </div>
      )}

      {loading ? (
        <div className="forecast-loading" role="status">
          載入中...
        </div>
      ) : (
        <>
          <div className="forecast-filters">
            <div className="filter-field">
              <label htmlFor="month-select">月份</label>
              <select
                id="month-select"
                className="form-select"
                value={selectedMonth}
                onChange={(e) => handleMonthChange(e.target.value)}
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
              <label htmlFor="channel-select">通路</label>
              <select
                id="channel-select"
                className="form-select"
                value={selectedChannel}
                onChange={(e) => handleChannelChange(e.target.value)}
                disabled={!selectedMonth}
              >
                <option value="">請選擇通路</option>
                {availableChannels.map((ch) => (
                  <option key={ch} value={ch}>
                    {ch}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-field">
              <label htmlFor="version-select">版本</label>
              <select
                id="version-select"
                className="form-select"
                value={selectedVersion}
                onChange={(e) => handleVersionChange(e.target.value)}
                disabled={!selectedMonth || !selectedChannel || loadingVersions}
              >
                <option value="">請選擇版本</option>
                {versions.map((v) => (
                  <option key={v} value={v}>
                    {v} {v === latestVersion ? '(Latest)' : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {selectedVersion && (
            <div className="forecast-info">
              版本: {selectedVersion} | 項目數: {forecastData.length}
            </div>
          )}

          {loadingData ? (
            <div className="forecast-loading" role="status">
              載入中...
            </div>
          ) : selectedMonth && selectedChannel && selectedVersion ? (
            forecastData.length === 0 ? (
              <div className="forecast-empty">所選月份與通路無預測資料</div>
            ) : (
              <div className="forecast-table-wrap">
                <table className="forecast-table">
                  <thead>
                    <tr>
                      <th>類別</th>
                      <th>規格</th>
                      <th>產品代碼</th>
                      <th>產品名稱</th>
                      <th>倉儲位置</th>
                      <th className="align-right">數量</th>
                      {(canPerformActions.edit || canPerformActions.delete) && (
                        <th className="actions-cell">操作</th>
                      )}
                    </tr>
                  </thead>
                  <tbody>
                    {forecastData.map((item) => {
                      const isEditing = editingId === item.id
                      const isModified = item.isModified === true

                      return (
                        <tr key={item.id} className={isModified ? 'row-modified' : ''}>
                          <td>{item.category || '-'}</td>
                          <td>{item.spec || '-'}</td>
                          <td>{item.productCode}</td>
                          <td>{item.productName}</td>
                          <td>{item.warehouseLocation || '-'}</td>
                          <td className="align-right">
                            {isEditing ? (
                              <input
                                ref={editInputRef}
                                type="number"
                                className="quantity-input"
                                value={editingValue}
                                onChange={(e) => setEditingValue(e.target.value)}
                                onBlur={() => saveEdit(item.id)}
                                onKeyDown={(e) => handleEditKeyDown(e, item.id)}
                                min="0"
                              />
                            ) : (
                              <span
                                className={canPerformActions.edit ? 'quantity-editable' : ''}
                                onClick={() => startEditing(item)}
                                role={canPerformActions.edit ? 'button' : undefined}
                                tabIndex={canPerformActions.edit ? 0 : undefined}
                              >
                                {item.quantity}
                              </span>
                            )}
                          </td>
                          {(canPerformActions.edit || canPerformActions.delete) && (
                            <td className="actions-cell">
                              {canPerformActions.delete && (
                                <button
                                  className="btn btn--small btn--danger"
                                  onClick={() => setDeleteTarget(item)}
                                >
                                  刪除
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
            )
          ) : (
            <div className="forecast-empty">請選擇月份、通路與版本以檢視資料</div>
          )}
        </>
      )}

      <AddItemDialog
        open={showAddDialog}
        month={selectedMonth}
        channel={selectedChannel}
        onClose={() => setShowAddDialog(false)}
        onSuccess={handleAddSuccess}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="刪除確認"
        message={`確定要刪除 ${deleteTarget?.productName} 嗎？`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  )
}
