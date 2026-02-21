import { useState, useEffect, useCallback, useRef } from 'react'
import { getInventoryIntegration, updateModifiedSubtotal } from '../../api/inventory'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import './InventoryIntegration.css'

function hasPermission(user, perm) {
  if (user?.permissions && Array.isArray(user.permissions)) {
    return user.permissions.includes(perm)
  }
  if (user?.roleCode === 'admin') return true
  return false
}

function formatNumber(value) {
  if (value == null || value === '') return '0.00'
  const num = Number(value)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function getCurrentMonth() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

function getFirstDayOfMonth(monthStr) {
  if (!monthStr) return ''
  const [year, month] = monthStr.split('-')
  return `${year}-${month}-01`
}

function getLastDayOfMonth(monthStr) {
  if (!monthStr) return ''
  const [year, month] = monthStr.split('-')
  const lastDay = new Date(year, month, 0).getDate()
  return `${year}-${month}-${String(lastDay).padStart(2, '0')}`
}

export default function InventoryIntegrationPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [month, setMonth] = useState(getCurrentMonth())
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [versionInput, setVersionInput] = useState('')

  const [integrationData, setIntegrationData] = useState([])
  const [currentVersion, setCurrentVersion] = useState('')
  const [loadingData, setLoadingData] = useState(false)
  const [accessDenied, setAccessDenied] = useState(false)

  const [editingCell, setEditingCell] = useState(null)
  const [editValue, setEditValue] = useState('')
  const [editError, setEditError] = useState('')
  const [changedRows, setChangedRows] = useState({})
  const [saving, setSaving] = useState(false)

  const [recentVersions, setRecentVersions] = useState([])
  const [sortConfig, setSortConfig] = useState({ key: 'productCode', direction: 'asc' })

  const inputRef = useRef(null)

  const canView = hasPermission(user, 'inventory.view')
  const canEdit = hasPermission(user, 'inventory.edit')

  useEffect(() => {
    const stored = localStorage.getItem('inventoryVersions')
    if (stored) {
      try {
        setRecentVersions(JSON.parse(stored))
      } catch {
        setRecentVersions([])
      }
    }
  }, [])

  useEffect(() => {
    if (Object.keys(changedRows).length > 0) {
      const handleBeforeUnload = (e) => {
        e.preventDefault()
        e.returnValue = ''
      }
      window.addEventListener('beforeunload', handleBeforeUnload)
      return () => window.removeEventListener('beforeunload', handleBeforeUnload)
    }
  }, [changedRows])

  const handleQuery = useCallback(async () => {
    if (!month) {
      toast.error('請選擇月份')
      return
    }

    setLoadingData(true)
    setAccessDenied(false)
    try {
      const data = await getInventoryIntegration(month, startDate || null, endDate || null, null)
      setIntegrationData(data)

      if (data.length > 0 && data[0].version) {
        const newVersion = data[0].version
        setCurrentVersion(newVersion)

        const updatedVersions = [newVersion, ...recentVersions.filter(v => v !== newVersion)].slice(0, 10)
        setRecentVersions(updatedVersions)
        localStorage.setItem('inventoryVersions', JSON.stringify(updatedVersions))
      }

      setChangedRows({})
      toast.success('查詢成功')
    } catch (err) {
      if (err.response?.status === 403) {
        setAccessDenied(true)
        toast.error('您沒有權限檢視此資料')
      } else {
        toast.error('無法載入資料')
      }
      setIntegrationData([])
    } finally {
      setLoadingData(false)
    }
  }, [month, startDate, endDate, recentVersions, toast])

  const handleLoadVersion = useCallback(async () => {
    if (!month || !versionInput) {
      toast.error('請選擇月份和版本')
      return
    }

    setLoadingData(true)
    setAccessDenied(false)
    try {
      const data = await getInventoryIntegration(month, null, null, versionInput)
      setIntegrationData(data)
      setCurrentVersion(versionInput)
      setChangedRows({})
      toast.success('版本載入成功')
    } catch (err) {
      if (err.response?.status === 403) {
        setAccessDenied(true)
        toast.error('您沒有權限檢視此資料')
      } else {
        toast.error('無法載入版本資料')
      }
      setIntegrationData([])
    } finally {
      setLoadingData(false)
    }
  }, [month, versionInput, toast])

  const handleMonthChange = (e) => {
    const newMonth = e.target.value
    setMonth(newMonth)
    setStartDate(getFirstDayOfMonth(newMonth))
    setEndDate(getLastDayOfMonth(newMonth))
  }

  const validateDecimal = (value) => {
    if (!value) return { valid: true, error: '' }

    const regex = /^-?\d{0,10}(\.\d{0,2})?$/
    if (!regex.test(value)) {
      return { valid: false, error: '格式錯誤：最多 10 位整數，2 位小數' }
    }

    const num = parseFloat(value)
    if (isNaN(num)) {
      return { valid: false, error: '請輸入有效數字' }
    }

    return { valid: true, error: '' }
  }

  const handleCellClick = (rowId) => {
    if (!canEdit) return

    const row = integrationData.find(r => r.id === rowId)
    if (!row) return

    const currentValue = changedRows[rowId]?.modifiedSubtotal ?? row.modifiedSubtotal ?? ''
    setEditingCell(rowId)
    setEditValue(String(currentValue))
    setEditError('')

    setTimeout(() => inputRef.current?.focus(), 0)
  }

  const handleEditChange = (e) => {
    const value = e.target.value
    setEditValue(value)

    const validation = validateDecimal(value)
    setEditError(validation.error)
  }

  const handleEditSave = (rowId) => {
    const validation = validateDecimal(editValue)
    if (!validation.valid) {
      setEditError(validation.error)
      return
    }

    const row = integrationData.find(r => r.id === rowId)
    if (!row) return

    const newValue = editValue === '' ? null : parseFloat(editValue)

    setChangedRows(prev => ({
      ...prev,
      [rowId]: {
        ...row,
        modifiedSubtotal: newValue
      }
    }))

    setEditingCell(null)
    setEditValue('')
    setEditError('')
  }

  const handleEditCancel = () => {
    setEditingCell(null)
    setEditValue('')
    setEditError('')
  }

  const handleKeyDown = (e, rowId) => {
    if (e.key === 'Enter') {
      handleEditSave(rowId)
    } else if (e.key === 'Escape') {
      handleEditCancel()
    }
  }

  const handleSaveAll = async () => {
    const changedIds = Object.keys(changedRows)
    if (changedIds.length === 0) {
      toast.error('沒有變更需要儲存')
      return
    }

    setSaving(true)
    try {
      for (const id of changedIds) {
        const row = changedRows[id]
        await updateModifiedSubtotal(parseInt(id), row.modifiedSubtotal)
      }

      toast.success('儲存成功，重新查詢資料')
      setChangedRows({})
      await handleQuery()
    } catch (err) {
      if (err.response?.status === 403) {
        toast.error('您沒有權限編輯資料')
      } else {
        toast.error('儲存失敗，請重試')
      }
    } finally {
      setSaving(false)
    }
  }

  const handleCancelChanges = () => {
    setChangedRows({})
    setEditingCell(null)
    setEditValue('')
    setEditError('')
    toast.success('已取消變更')
  }

  const handleSort = (key) => {
    setSortConfig(prev => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc'
    }))
  }

  const getSortedData = () => {
    const dataToSort = integrationData.map(row => {
      if (changedRows[row.id]) {
        return { ...row, ...changedRows[row.id] }
      }
      return row
    })

    return [...dataToSort].sort((a, b) => {
      const aVal = a[sortConfig.key]
      const bVal = b[sortConfig.key]

      if (aVal == null) return 1
      if (bVal == null) return -1

      const comparison = aVal > bVal ? 1 : aVal < bVal ? -1 : 0
      return sortConfig.direction === 'asc' ? comparison : -comparison
    })
  }

  const isRowModified = (row) => {
    const displayValue = changedRows[row.id]?.modifiedSubtotal ?? row.modifiedSubtotal
    return displayValue != null && displayValue !== row.productionSubtotal
  }

  if (accessDenied || (!canView)) {
    return (
      <div className="inventory-integration-page">
        <h1>庫存整合</h1>
        <div className="inventory-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  const sortedData = getSortedData()
  const hasChanges = Object.keys(changedRows).length > 0

  return (
    <div className="inventory-integration-page">
      <div className="inventory-page-header">
        <h1>庫存整合</h1>
        {hasChanges && canEdit && (
          <div className="inventory-action-buttons">
            <button
              className="btn btn--secondary"
              onClick={handleCancelChanges}
              disabled={saving}
            >
              取消
            </button>
            <button
              className="btn btn--primary"
              onClick={handleSaveAll}
              disabled={saving}
            >
              {saving ? '儲存中...' : '儲存'}
            </button>
          </div>
        )}
      </div>

      <div className="inventory-filters">
        <div className="filter-field">
          <label htmlFor="month-input">月份</label>
          <input
            id="month-input"
            type="month"
            className="form-input"
            value={month}
            onChange={handleMonthChange}
          />
        </div>

        <div className="filter-field">
          <label htmlFor="start-date-input">開始日期</label>
          <input
            id="start-date-input"
            type="date"
            className="form-input"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
        </div>

        <div className="filter-field">
          <label htmlFor="end-date-input">結束日期</label>
          <input
            id="end-date-input"
            type="date"
            className="form-input"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
        </div>

        <div className="filter-field filter-field--button">
          <label>&nbsp;</label>
          <button
            className="btn btn--primary"
            onClick={handleQuery}
            disabled={!month || loadingData}
          >
            查詢
          </button>
        </div>
      </div>

      <div className="inventory-filters">
        <div className="filter-field">
          <label htmlFor="version-select">歷史版本</label>
          <select
            id="version-select"
            className="form-select"
            value={versionInput}
            onChange={(e) => setVersionInput(e.target.value)}
          >
            <option value="">請選擇版本</option>
            {recentVersions.map((v) => (
              <option key={v} value={v}>
                {v}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-field filter-field--button">
          <label>&nbsp;</label>
          <button
            className="btn btn--secondary"
            onClick={handleLoadVersion}
            disabled={!versionInput || loadingData}
          >
            載入版本
          </button>
        </div>
      </div>

      {currentVersion && (
        <div className="inventory-info">
          目前版本: {currentVersion} | 產品數: {integrationData.length}
        </div>
      )}

      {loadingData ? (
        <div className="inventory-loading" role="status">
          載入中...
        </div>
      ) : integrationData.length === 0 ? (
        <div className="inventory-empty">請查詢以顯示資料</div>
      ) : (
        <div className="inventory-table-wrap">
          <table className="inventory-integration-table">
            <thead>
              <tr>
                <th onClick={() => handleSort('productCode')} className="sortable">
                  品號 {sortConfig.key === 'productCode' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th onClick={() => handleSort('productName')} className="sortable">
                  品名 {sortConfig.key === 'productName' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th onClick={() => handleSort('category')} className="sortable">
                  類別 {sortConfig.key === 'category' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th onClick={() => handleSort('spec')} className="sortable">
                  規格 {sortConfig.key === 'spec' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th onClick={() => handleSort('warehouseLocation')} className="sortable">
                  庫位 {sortConfig.key === 'warehouseLocation' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th className="align-right" onClick={() => handleSort('salesQuantity')} className="sortable align-right">
                  銷售數量 {sortConfig.key === 'salesQuantity' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th className="align-right" onClick={() => handleSort('inventoryBalance')} className="sortable align-right">
                  庫存餘額 {sortConfig.key === 'inventoryBalance' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th className="align-right" onClick={() => handleSort('forecastQuantity')} className="sortable align-right">
                  預測數量 {sortConfig.key === 'forecastQuantity' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th className="align-right" onClick={() => handleSort('productionSubtotal')} className="sortable align-right">
                  生產小計 {sortConfig.key === 'productionSubtotal' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th className="align-right" onClick={() => handleSort('modifiedSubtotal')} className="sortable align-right">
                  修改小計 {sortConfig.key === 'modifiedSubtotal' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th onClick={() => handleSort('version')} className="sortable">
                  版本 {sortConfig.key === 'version' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
              </tr>
            </thead>
            <tbody>
              {sortedData.map((row) => {
                const isEditing = editingCell === row.id
                const displayValue = changedRows[row.id]?.modifiedSubtotal ?? row.modifiedSubtotal
                const isModified = isRowModified(row)

                return (
                  <tr key={row.id} className={isModified ? 'row-modified' : ''}>
                    <td>{row.productCode || '-'}</td>
                    <td>{row.productName || '-'}</td>
                    <td>{row.category || '-'}</td>
                    <td>{row.spec || '-'}</td>
                    <td>{row.warehouseLocation || '-'}</td>
                    <td className="align-right">{formatNumber(row.salesQuantity)}</td>
                    <td className="align-right">{formatNumber(row.inventoryBalance)}</td>
                    <td className="align-right">{formatNumber(row.forecastQuantity)}</td>
                    <td className="align-right">{formatNumber(row.productionSubtotal)}</td>
                    <td
                      className={`align-right ${canEdit ? 'editable-cell' : ''}`}
                      onClick={() => handleCellClick(row.id)}
                    >
                      {isEditing ? (
                        <div className="edit-cell-wrapper">
                          <input
                            ref={inputRef}
                            type="text"
                            className={`edit-input ${editError ? 'edit-input--error' : ''}`}
                            value={editValue}
                            onChange={handleEditChange}
                            onKeyDown={(e) => handleKeyDown(e, row.id)}
                            onBlur={() => handleEditSave(row.id)}
                          />
                          {editError && <div className="edit-error">{editError}</div>}
                        </div>
                      ) : (
                        <>
                          {formatNumber(displayValue)}
                          {canEdit && <span className="edit-icon">✎</span>}
                        </>
                      )}
                    </td>
                    <td>{row.version || '-'}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
