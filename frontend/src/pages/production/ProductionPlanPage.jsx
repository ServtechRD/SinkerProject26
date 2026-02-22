import { useState, useEffect, useCallback, useRef } from 'react'
import { getProductionPlan, updateProductionPlanItem } from '../../api/productionPlan'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import './ProductionPlan.css'

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

const MONTHS = [
  { key: '2', label: 'Feb' },
  { key: '3', label: 'Mar' },
  { key: '4', label: 'Apr' },
  { key: '5', label: 'May' },
  { key: '6', label: 'Jun' },
  { key: '7', label: 'Jul' },
  { key: '8', label: 'Aug' },
  { key: '9', label: 'Sep' },
  { key: '10', label: 'Oct' },
  { key: '11', label: 'Nov' },
  { key: '12', label: 'Dec' },
]

function calculateTotal(row) {
  let sum = 0
  MONTHS.forEach((m) => {
    const val = row.monthlyAllocations?.[m.key] || 0
    sum += Number(val)
  })
  sum += Number(row.buffer || 0)
  return sum
}

function calculateDifference(row) {
  const total = calculateTotal(row)
  const forecast = Number(row.forecast || 0)
  return total - forecast
}

export default function ProductionPlanPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [year, setYear] = useState(new Date().getFullYear())
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [accessDenied, setAccessDenied] = useState(false)
  const [modifiedRows, setModifiedRows] = useState(new Map())
  const [saving, setSaving] = useState(false)

  const [editingCell, setEditingCell] = useState(null)
  const [editingValue, setEditingValue] = useState('')
  const editInputRef = useRef(null)

  const canView = hasPermission(user, 'production_plan.view')
  const canEdit = hasPermission(user, 'production_plan.edit')

  const fetchData = useCallback(async () => {
    if (!year) return

    setLoading(true)
    setAccessDenied(false)
    try {
      const result = await getProductionPlan(year)
      setData(result)
      setModifiedRows(new Map())
    } catch (err) {
      if (err.response?.status === 403) {
        setAccessDenied(true)
      } else {
        toast.error('無法載入生產計畫資料')
      }
      setData([])
    } finally {
      setLoading(false)
    }
  }, [year, toast])

  const handleLoad = () => {
    if (modifiedRows.size > 0) {
      if (!window.confirm('您有未儲存的變更，確定要載入新資料嗎？')) {
        return
      }
    }
    fetchData()
  }

  const startEditing = (rowId, field) => {
    if (!canEdit) return

    const row = data.find((r) => r.id === rowId)
    if (!row) return

    let value = ''
    if (field === 'buffer') {
      value = String(row.buffer || 0)
    } else if (field === 'remarks') {
      value = row.remarks || ''
    } else if (field.startsWith('month_')) {
      const monthKey = field.replace('month_', '')
      value = String(row.monthlyAllocations?.[monthKey] || 0)
    }

    setEditingCell({ rowId, field })
    setEditingValue(value)
    setTimeout(() => editInputRef.current?.focus(), 0)
  }

  const cancelEditing = () => {
    setEditingCell(null)
    setEditingValue('')
  }

  const saveEdit = () => {
    if (!editingCell) return

    const { rowId, field } = editingCell
    const row = data.find((r) => r.id === rowId)
    if (!row) return

    let isValid = true
    let newValue = editingValue

    if (field !== 'remarks') {
      const numValue = Number(editingValue)
      if (isNaN(numValue) || numValue < 0) {
        toast.error('請輸入有效的數值')
        return
      }
      if (editingValue.includes('.')) {
        const parts = editingValue.split('.')
        if (parts[1] && parts[1].length > 2) {
          toast.error('最多只能有兩位小數')
          return
        }
      }
      const totalDigits = editingValue.replace('.', '').length
      if (totalDigits > 10) {
        toast.error('數字太大（最多10位數字）')
        return
      }
      newValue = numValue
    }

    const updatedRow = { ...row }

    if (field === 'buffer') {
      updatedRow.buffer = newValue
    } else if (field === 'remarks') {
      updatedRow.remarks = newValue
    } else if (field.startsWith('month_')) {
      const monthKey = field.replace('month_', '')
      updatedRow.monthlyAllocations = { ...updatedRow.monthlyAllocations, [monthKey]: newValue }
    }

    const newData = data.map((r) => (r.id === rowId ? updatedRow : r))
    setData(newData)

    const newModified = new Map(modifiedRows)
    newModified.set(rowId, updatedRow)
    setModifiedRows(newModified)

    cancelEditing()
  }

  const handleEditKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      saveEdit()
    } else if (e.key === 'Escape') {
      cancelEditing()
    } else if (e.key === 'Tab') {
      e.preventDefault()
      saveEdit()
      // Tab navigation could be enhanced here
    }
  }

  const handleSave = async () => {
    if (modifiedRows.size === 0) return

    setSaving(true)
    const rowsToSave = Array.from(modifiedRows.values())

    let successCount = 0
    let failCount = 0

    for (const row of rowsToSave) {
      try {
        await updateProductionPlanItem(row.id, {
          monthlyAllocations: row.monthlyAllocations,
          buffer: row.buffer,
          remarks: row.remarks,
        })
        successCount++
      } catch (err) {
        failCount++
        console.error('Failed to save row:', row.id, err)
      }
    }

    setSaving(false)

    if (failCount === 0) {
      toast.success(`成功儲存 ${successCount} 筆記錄`)
      setModifiedRows(new Map())
      fetchData()
    } else if (successCount === 0) {
      toast.error('儲存失敗，請重試')
    } else {
      toast.error(`部分儲存失敗：成功 ${successCount} 筆，失敗 ${failCount} 筆`)
      // Refresh to get consistent state
      fetchData()
    }
  }

  const handleCancel = () => {
    if (modifiedRows.size > 5) {
      if (!window.confirm(`確定要取消 ${modifiedRows.size} 筆變更嗎？`)) {
        return
      }
    } else if (modifiedRows.size > 0) {
      if (!window.confirm('確定要取消變更嗎？')) {
        return
      }
    }
    fetchData()
  }

  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (modifiedRows.size > 0) {
        e.preventDefault()
        e.returnValue = `您有 ${modifiedRows.size} 筆未儲存的變更`
        return e.returnValue
      }
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [modifiedRows])

  if (accessDenied || (!loading && !canView)) {
    return (
      <div className="production-plan-page">
        <h1>生產計畫</h1>
        <div className="production-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  const yearOptions = []
  for (let y = 2020; y <= 2030; y++) {
    yearOptions.push(y)
  }

  return (
    <div className="production-plan-page">
      <div className="production-page-header">
        <h1>生產計畫 - {year}</h1>
        {modifiedRows.size > 0 && (
          <div className="production-actions">
            <button
              className="btn btn--primary"
              onClick={handleSave}
              disabled={saving}
            >
              {saving ? '儲存中...' : `儲存 (${modifiedRows.size})`}
            </button>
            <button
              className="btn btn--secondary"
              onClick={handleCancel}
              disabled={saving}
            >
              取消
            </button>
          </div>
        )}
      </div>

      <div className="production-controls">
        <div className="control-group">
          <label htmlFor="year-select">年度</label>
          <select
            id="year-select"
            className="form-select"
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
            disabled={loading}
          >
            {yearOptions.map((y) => (
              <option key={y} value={y}>
                {y}
              </option>
            ))}
          </select>
          <button
            className="btn btn--primary"
            onClick={handleLoad}
            disabled={loading}
          >
            {loading ? '載入中...' : '載入'}
          </button>
        </div>
        {data.length > 0 && (
          <div className="production-info">
            總計: {data.length} 產品/通路組合
          </div>
        )}
      </div>

      {loading ? (
        <div className="production-loading" role="status">
          載入中...
        </div>
      ) : data.length === 0 ? (
        <div className="production-empty">所選年度無生產計畫資料</div>
      ) : (
        <div className="production-grid-wrap">
          <table className="production-grid">
            <thead>
              <tr>
                <th className="frozen-col frozen-col-1">產品代碼</th>
                <th className="frozen-col frozen-col-2">產品名稱</th>
                <th>類別</th>
                <th>規格</th>
                <th>倉儲位置</th>
                <th>通路</th>
                {MONTHS.map((m) => (
                  <th key={m.key} className="month-col">
                    {m.label}
                  </th>
                ))}
                <th className="numeric-col">Buffer</th>
                <th className="numeric-col">Total</th>
                <th className="numeric-col">Forecast</th>
                <th className="numeric-col">Diff</th>
                <th>Remarks</th>
              </tr>
            </thead>
            <tbody>
              {data.map((row) => {
                const isModified = modifiedRows.has(row.id)
                const total = calculateTotal(row)
                const diff = calculateDifference(row)

                return (
                  <tr key={row.id} className={isModified ? 'row-modified' : ''}>
                    <td className="frozen-col frozen-col-1">{row.productCode}</td>
                    <td className="frozen-col frozen-col-2">{row.productName}</td>
                    <td>{row.category || '-'}</td>
                    <td>{row.spec || '-'}</td>
                    <td>{row.warehouseLocation || '-'}</td>
                    <td>{row.channel}</td>
                    {MONTHS.map((m) => {
                      const field = `month_${m.key}`
                      const isEditing =
                        editingCell?.rowId === row.id && editingCell?.field === field
                      const value = row.monthlyAllocations?.[m.key] || 0

                      return (
                        <td key={m.key} className="numeric-col editable-cell">
                          {isEditing ? (
                            <input
                              ref={editInputRef}
                              type="text"
                              className="cell-input"
                              value={editingValue}
                              onChange={(e) => setEditingValue(e.target.value)}
                              onBlur={saveEdit}
                              onKeyDown={handleEditKeyDown}
                            />
                          ) : (
                            <span
                              className={canEdit ? 'editable' : ''}
                              onClick={() => startEditing(row.id, field)}
                              role={canEdit ? 'button' : undefined}
                              tabIndex={canEdit ? 0 : undefined}
                            >
                              {Number(value).toFixed(2)}
                            </span>
                          )}
                        </td>
                      )
                    })}
                    <td className="numeric-col editable-cell">
                      {editingCell?.rowId === row.id && editingCell?.field === 'buffer' ? (
                        <input
                          ref={editInputRef}
                          type="text"
                          className="cell-input"
                          value={editingValue}
                          onChange={(e) => setEditingValue(e.target.value)}
                          onBlur={saveEdit}
                          onKeyDown={handleEditKeyDown}
                        />
                      ) : (
                        <span
                          className={canEdit ? 'editable' : ''}
                          onClick={() => startEditing(row.id, 'buffer')}
                          role={canEdit ? 'button' : undefined}
                          tabIndex={canEdit ? 0 : undefined}
                        >
                          {Number(row.buffer || 0).toFixed(2)}
                        </span>
                      )}
                    </td>
                    <td className="numeric-col">{total.toFixed(2)}</td>
                    <td className="numeric-col">{Number(row.forecast || 0).toFixed(2)}</td>
                    <td
                      className={`numeric-col ${diff > 0 ? 'diff-positive' : diff < 0 ? 'diff-negative' : ''}`}
                    >
                      {diff > 0 ? '+' : ''}
                      {diff.toFixed(2)}
                    </td>
                    <td className="editable-cell remarks-cell">
                      {editingCell?.rowId === row.id && editingCell?.field === 'remarks' ? (
                        <input
                          ref={editInputRef}
                          type="text"
                          className="cell-input"
                          value={editingValue}
                          onChange={(e) => setEditingValue(e.target.value)}
                          onBlur={saveEdit}
                          onKeyDown={handleEditKeyDown}
                        />
                      ) : (
                        <span
                          className={canEdit ? 'editable' : ''}
                          onClick={() => startEditing(row.id, 'remarks')}
                          role={canEdit ? 'button' : undefined}
                          tabIndex={canEdit ? 0 : undefined}
                        >
                          {row.remarks || '-'}
                        </span>
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
