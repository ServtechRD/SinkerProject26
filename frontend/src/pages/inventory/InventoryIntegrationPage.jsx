import { useState, useEffect, useCallback, useRef } from 'react'
import {
  getInventoryIntegration,
  getInventoryVersions,
  copyInventoryVersion,
  updateModifiedSubtotal,
} from '../../api/inventory'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import './InventoryIntegration.css'

const QUERY_MODE_DATE = 'date'
const QUERY_MODE_VERSION = 'version'
const PRODUCTION_SUBTOTAL_TOOLTIP = '預估量-結存-銷貨存量(銷貨+撥出+撥入)'

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
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
  const lastDay = new Date(parseInt(year, 10), parseInt(month, 10), 0).getDate()
  return `${year}-${month}-${String(lastDay).padStart(2, '0')}`
}

/** 計算起始日～結束日涵蓋的月數（含頭尾），超過 4 個月回傳 false */
function isDateRangeWithinFourMonths(startDateStr, endDateStr) {
  if (!startDateStr || !endDateStr) return true
  const start = new Date(startDateStr)
  const end = new Date(endDateStr)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return true
  if (end < start) return false
  const monthsBetween =
    (end.getFullYear() - start.getFullYear()) * 12 +
    (end.getMonth() - start.getMonth()) +
    1
  return monthsBetween <= 4
}

export default function InventoryIntegrationPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [queryMode, setQueryMode] = useState(QUERY_MODE_DATE)
  const [month, setMonth] = useState(getCurrentMonth())
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [versionList, setVersionList] = useState([])
  const [selectedVersion, setSelectedVersion] = useState('')

  const [integrationData, setIntegrationData] = useState([])
  const [currentVersion, setCurrentVersion] = useState('')
  const [loadingData, setLoadingData] = useState(false)
  const [accessDenied, setAccessDenied] = useState(false)
  const [copyingVersion, setCopyingVersion] = useState(false)

  const [editingRowId, setEditingRowId] = useState(null)
  const [editValue, setEditValue] = useState('')
  const [editError, setEditError] = useState('')
  const [saving, setSaving] = useState(false)

  const [sortConfig, setSortConfig] = useState({ key: 'productCode', direction: 'asc' })
  const inputRef = useRef(null)

  const canView = hasPermission(user, 'inventory.view')
  const canEdit = hasPermission(user, 'inventory.edit')

  useEffect(() => {
    setStartDate(getFirstDayOfMonth(month))
    setEndDate(getLastDayOfMonth(month))
  }, [month])

  useEffect(() => {
    getInventoryVersions(null)
      .then((list) => setVersionList(Array.isArray(list) ? list : []))
      .catch(() => setVersionList([]))
  }, [])

  const runQuery = useCallback(async () => {
    if (queryMode === QUERY_MODE_DATE && !month) {
      toast.error('請選擇月份')
      return
    }
    if (queryMode === QUERY_MODE_DATE && (startDate || endDate)) {
      if (!isDateRangeWithinFourMonths(startDate, endDate)) {
        toast.error('結存查詢起始日期與結存查詢結束日期最多只能查詢 4 個月')
        return
      }
    }
    if (queryMode === QUERY_MODE_VERSION && !selectedVersion) {
      toast.error('請選擇版本')
      return
    }

    setLoadingData(true)
    setAccessDenied(false)
    try {
      const data =
        queryMode === QUERY_MODE_VERSION
          ? await getInventoryIntegration(null, null, null, selectedVersion)
          : await getInventoryIntegration(month, startDate || null, endDate || null, null)
      setIntegrationData(Array.isArray(data) ? data : [])

      if (data.length > 0 && data[0].version) {
        setCurrentVersion(data[0].version)
      } else {
        setCurrentVersion('')
      }
      toast.success('查詢成功')
    } catch (err) {
      if (err.response?.status === 403) {
        setAccessDenied(true)
        toast.error('您沒有權限檢視此資料')
      } else {
        toast.error(err.response?.data?.message || '無法載入資料')
      }
      setIntegrationData([])
      setCurrentVersion('')
    } finally {
      setLoadingData(false)
    }
  }, [month, startDate, endDate, queryMode, selectedVersion, toast])

  const handleCopyVersion = useCallback(async () => {
    if (!currentVersion) {
      toast.error('請先查詢並取得目前版本')
      return
    }
    setCopyingVersion(true)
    try {
      const res = await copyInventoryVersion(currentVersion)
      const newVer = res?.version
      toast.success('已建立新版次：' + newVer)
      setSelectedVersion(newVer)
      setQueryMode(QUERY_MODE_VERSION)
      setVersionList((prev) => (newVer ? [newVer, ...prev.filter((v) => v !== newVer)] : prev))
      await getInventoryIntegration(null, null, null, newVer).then(setIntegrationData)
      setCurrentVersion(newVer)
    } catch (err) {
      toast.error(err.response?.data?.message || '建立新版次失敗')
    } finally {
      setCopyingVersion(false)
    }
  }, [currentVersion, toast])

  const validateDecimal = (value) => {
    if (value === '' || value == null) return { valid: true, error: '' }
    const regex = /^-?\d{0,10}(\.\d{0,2})?$/
    if (!regex.test(String(value))) {
      return { valid: false, error: '格式錯誤：最多 10 位整數，2 位小數' }
    }
    const num = parseFloat(value)
    if (isNaN(num)) return { valid: false, error: '請輸入有效數字' }
    return { valid: true, error: '' }
  }

  const startEdit = (row) => {
    if (!canEdit) return
    setEditingRowId(row.id)
    setEditValue(
      row.modifiedSubtotal != null && row.modifiedSubtotal !== '' ? String(row.modifiedSubtotal) : ''
    )
    setEditError('')
    setTimeout(() => inputRef.current?.focus(), 0)
  }

  const cancelEdit = () => {
    setEditingRowId(null)
    setEditValue('')
    setEditError('')
  }

  const saveEdit = async (rowId) => {
    const validation = validateDecimal(editValue)
    if (!validation.valid) {
      setEditError(validation.error)
      return
    }
    const newVal = editValue === '' ? null : parseFloat(editValue)
    setSaving(true)
    try {
      const updated = await updateModifiedSubtotal(rowId, newVal)
      const savedVal = updated?.modified_subtotal ?? updated?.modifiedSubtotal
      setIntegrationData((prev) =>
        prev.map((r) =>
          r.id === rowId
            ? { ...r, modifiedSubtotal: savedVal, modified_subtotal: savedVal }
            : r
        )
      )
      toast.success('已儲存')
      cancelEdit()
    } catch (err) {
      toast.error(err.response?.data?.message || '儲存失敗')
    } finally {
      setSaving(false)
    }
  }

  const handleSort = (key) => {
    setSortConfig((prev) => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
    }))
  }

  const sortedData = [...integrationData].sort((a, b) => {
    const aVal = a[sortConfig.key]
    const bVal = b[sortConfig.key]
    if (aVal == null) return 1
    if (bVal == null) return -1
    const cmp = aVal > bVal ? 1 : aVal < bVal ? -1 : 0
    return sortConfig.direction === 'asc' ? cmp : -cmp
  })

  if (accessDenied || !canView) {
    return (
      <div className="inventory-integration-page">
        <h1>庫存銷量預估量整合表單</h1>
        <div className="inventory-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="inventory-integration-page">
      <div className="inventory-page-header">
        <h1>庫存銷量預估量整合表單</h1>
      </div>

      <div className="inventory-query-section">
        <div className="inventory-query-modes">
          <label className="inventory-radio-label">
            <input
              type="radio"
              name="queryMode"
              checked={queryMode === QUERY_MODE_DATE}
              onChange={() => setQueryMode(QUERY_MODE_DATE)}
            />
            查詢月份、結存查詢起始日期、結存查詢結束日期
          </label>
          <label className="inventory-radio-label">
            <input
              type="radio"
              name="queryMode"
              checked={queryMode === QUERY_MODE_VERSION}
              onChange={() => setQueryMode(QUERY_MODE_VERSION)}
            />
            查詢特定版本
          </label>
        </div>

        <div className="inventory-filters">
          {queryMode === QUERY_MODE_DATE && (
            <>
              <div className="filter-field">
                <label htmlFor="month-input">查詢月份</label>
                <input
                  id="month-input"
                  type="month"
                  className="form-input"
                  value={month}
                  onChange={(e) => setMonth(e.target.value)}
                />
              </div>
              <div className="filter-field">
                <label htmlFor="start-date-input">結存查詢起始日期</label>
                <input
                  id="start-date-input"
                  type="date"
                  className="form-input"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </div>
              <div className="filter-field">
                <label htmlFor="end-date-input">結存查詢結束日期</label>
                <input
                  id="end-date-input"
                  type="date"
                  className="form-input"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </div>
            </>
          )}
          {queryMode === QUERY_MODE_VERSION && (
            <div className="filter-field">
              <label htmlFor="version-select">選擇版本</label>
              <select
                id="version-select"
                className="form-select"
                value={selectedVersion}
                onChange={(e) => setSelectedVersion(e.target.value)}
              >
                <option value="">請選擇版本</option>
                {versionList.map((v) => (
                  <option key={v} value={v}>
                    {v}
                  </option>
                ))}
              </select>
            </div>
          )}
          <div className="filter-field filter-field--button">
            <label>&nbsp;</label>
            <button
              className="btn btn--primary"
              onClick={runQuery}
              disabled={
                loadingData ||
                (queryMode === QUERY_MODE_DATE && !month) ||
                (queryMode === QUERY_MODE_VERSION && !selectedVersion)
              }
            >
              {loadingData ? '查詢中...' : '查詢'}
            </button>
          </div>
        </div>
      </div>

      {currentVersion && (
        <div className="inventory-info">
          目前版本: {currentVersion} | 筆數: {integrationData.length}
        </div>
      )}

      {integrationData.length > 0 && (
        <div className="inventory-result-toolbar">
          {canEdit && (
            <button
              type="button"
              className="btn btn--outline"
              onClick={handleCopyVersion}
              disabled={copyingVersion}
            >
              {copyingVersion ? '建立中...' : '版本編輯'}
            </button>
          )}
        </div>
      )}

      {loadingData ? (
        <div className="inventory-loading" role="status">
          載入中...
        </div>
      ) : integrationData.length === 0 ? (
        <div className="inventory-empty">請選擇查詢條件後點擊「查詢」顯示結果</div>
      ) : (
        <div className="inventory-table-wrap">
          <table className="inventory-integration-table">
            <thead>
              <tr>
                <th onClick={() => handleSort('warehouseLocation')} className="sortable">
                  庫位 {sortConfig.key === 'warehouseLocation' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th onClick={() => handleSort('category')} className="sortable">
                  中類名稱 {sortConfig.key === 'category' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th onClick={() => handleSort('spec')} className="sortable">
                  貨品規格 {sortConfig.key === 'spec' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th onClick={() => handleSort('productName')} className="sortable">
                  品名 {sortConfig.key === 'productName' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th onClick={() => handleSort('productCode')} className="sortable">
                  品號 {sortConfig.key === 'productCode' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th className="align-right sortable" onClick={() => handleSort('salesQuantity')}>
                  銷貨數量 {sortConfig.key === 'salesQuantity' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th className="align-right sortable" onClick={() => handleSort('inventoryBalance')}>
                  結存 {sortConfig.key === 'inventoryBalance' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th className="align-right sortable" onClick={() => handleSort('forecastQuantity')}>
                  預估量 {sortConfig.key === 'forecastQuantity' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th
                  className="align-right sortable"
                  onClick={() => handleSort('productionSubtotal')}
                  title={PRODUCTION_SUBTOTAL_TOOLTIP}
                >
                  生產小計 {sortConfig.key === 'productionSubtotal' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th className="align-right sortable" onClick={() => handleSort('modifiedSubtotal')}>
                  修改後小計 {sortConfig.key === 'modifiedSubtotal' && (sortConfig.direction === 'asc' ? '▲' : '▼')}
                </th>
                <th>編輯</th>
              </tr>
            </thead>
            <tbody>
              {sortedData.map((row) => {
                const isEditing = editingRowId === row.id
                return (
                  <tr key={row.id}>
                    <td>{row.warehouseLocation ?? row.warehouse_location ?? '-'}</td>
                    <td>{row.category ?? '-'}</td>
                    <td>{row.spec ?? '-'}</td>
                    <td>{row.productName ?? row.product_name ?? '-'}</td>
                    <td>{row.productCode ?? row.product_code ?? '-'}</td>
                    <td className="align-right">{formatNumber(row.salesQuantity ?? row.sales_quantity)}</td>
                    <td className="align-right">{formatNumber(row.inventoryBalance ?? row.inventory_balance)}</td>
                    <td className="align-right">{formatNumber(row.forecastQuantity ?? row.forecast_quantity)}</td>
                    <td className="align-right" title={PRODUCTION_SUBTOTAL_TOOLTIP}>
                      {formatNumber(row.productionSubtotal ?? row.production_subtotal)}
                    </td>
                    <td className="align-right">
                      {isEditing ? (
                        <div className="edit-cell-wrapper">
                          <input
                            ref={inputRef}
                            type="text"
                            className={`edit-input ${editError ? 'edit-input--error' : ''}`}
                            value={editValue}
                            onChange={(e) => {
                              setEditValue(e.target.value)
                              setEditError(validateDecimal(e.target.value).error)
                            }}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter') saveEdit(row.id)
                              if (e.key === 'Escape') cancelEdit()
                            }}
                          />
                          {editError && <div className="edit-error">{editError}</div>}
                          <div className="edit-actions">
                            <button
                              type="button"
                              className="btn btn--small btn--outline"
                              onClick={cancelEdit}
                              disabled={saving}
                            >
                              取消
                            </button>
                            <button
                              type="button"
                              className="btn btn--small btn--primary"
                              onClick={() => saveEdit(row.id)}
                              disabled={saving}
                            >
                              {saving ? '儲存中...' : '儲存'}
                            </button>
                          </div>
                        </div>
                      ) : (
                        formatNumber(row.modifiedSubtotal ?? row.modified_subtotal)
                      )}
                    </td>
                    <td>
                      {!isEditing && canEdit && (
                        <button
                          type="button"
                          className="btn btn--small btn--outline"
                          onClick={() => startEdit(row)}
                        >
                          編輯
                        </button>
                      )}
                    </td>
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
