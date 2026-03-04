import { useState, useCallback, useRef, Fragment } from 'react'
import {
  getProductionPlan,
  updateProductionPlanBuffer,
} from '../../api/productionPlan'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import './ProductionPlan.css'

const MONTH_KEYS = ['2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12']
const MONTH_LABELS = Object.fromEntries(
  MONTH_KEYS.map((k) => [k, `${parseInt(k, 10)}月`])
)

function hasPermission(user, perm) {
  return Boolean(
    user?.permissions &&
      Array.isArray(user.permissions) &&
      user.permissions.includes(perm)
  )
}

function num(v) {
  if (v == null) return 0
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

export default function ProductionPlanPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [year, setYear] = useState(new Date().getFullYear())
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [accessDenied, setAccessDenied] = useState(false)
  const [saving, setSaving] = useState(false)

  const [editingRowId, setEditingRowId] = useState(null)
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
      setData(Array.isArray(result) ? result : [])
    } catch (err) {
      if (err.response?.status === 403) {
        setAccessDenied(true)
      } else {
        toast.error('無法載入生產表單資料')
      }
      setData([])
    } finally {
      setLoading(false)
    }
  }, [year, toast])

  const handleQuery = () => {
    fetchData()
  }

  const getRowId = (row) => {
    return row.production_form_id ?? row.product_code ?? `${row.warehouse_location}-${row.product_code}`
  }

  const startEditBuffer = (row) => {
    if (!canEdit) return
    const id = getRowId(row)
    const buf = row.buffer_quantity ?? row.bufferQuantity ?? 0
    setEditingRowId(id)
    setEditingValue(String(num(buf)))
    setTimeout(() => editInputRef.current?.focus(), 0)
  }

  const cancelEdit = () => {
    setEditingRowId(null)
    setEditingValue('')
  }

  const saveBuffer = async (row) => {
    const productCode = row.product_code ?? row.productCode
    const newVal = editingValue === '' ? 0 : parseFloat(editingValue)
    if (Number.isNaN(newVal) || newVal < 0) {
      toast.error('請輸入有效的緩衝量（≥0）')
      return
    }
    setSaving(true)
    try {
      await updateProductionPlanBuffer(year, productCode, newVal)
      setData((prev) =>
        prev.map((r) =>
          (r.product_code ?? r.productCode) === productCode
            ? {
                ...r,
                buffer_quantity: newVal,
                bufferQuantity: newVal,
                aggregate_total:
                  num(r.aggregate_total ?? r.aggregateTotal) -
                  num(r.buffer_quantity ?? r.bufferQuantity) +
                  newVal,
                aggregateTotal:
                  num(r.aggregate_total ?? r.aggregateTotal) -
                  num(r.buffer_quantity ?? r.bufferQuantity) +
                  newVal,
                difference:
                  num(r.original_forecast ?? r.originalForecast) -
                  (num(r.aggregate_total ?? r.aggregateTotal) -
                    num(r.buffer_quantity ?? r.bufferQuantity) +
                    newVal),
              }
            : r
        )
      )
      toast.success('已儲存緩衝量')
      cancelEdit()
    } catch (err) {
      toast.error(err.response?.data?.message || '儲存失敗')
    } finally {
      setSaving(false)
    }
  }

  const handleEditKeyDown = (e, row) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      saveBuffer(row)
    } else if (e.key === 'Escape') {
      cancelEdit()
    }
  }

  const exportExcel = () => {
    if (data.length === 0) {
      toast.error('尚無資料可匯出')
      return
    }
    const channelOrder =
      data[0]?.channel_data?.map((c) => c.channel) ?? []
    const headers = [
      '庫位',
      '中類名稱',
      '貨品規格',
      '品名',
      '品號',
      ...channelOrder.flatMap((ch) => [
        ...MONTH_KEYS.map((k) => `${ch}-${MONTH_LABELS[k]}`),
        `${ch}-Total`,
      ]),
      ...MONTH_KEYS.map((k) => `合計-${MONTH_LABELS[k]}`),
      '緩衝量',
      '合計-Total',
      '原始預估',
      '差異',
      '備註',
    ]
    const rows = data.map((row) => {
      const cd = row.channel_data ?? []
      const channelCells = channelOrder.map((ch) => {
        const c = cd.find((x) => x.channel === ch) ?? {}
        const months = c.months ?? {}
        const total = num(c.total)
        return [
          ...MONTH_KEYS.map((k) => num(months[k])),
          total,
        ]
      }).flat()
      const agg = row.aggregate_months ?? row.aggregateMonths ?? {}
      const aggMonths = MONTH_KEYS.map((k) => num(agg[k]))
      const buf = num(row.buffer_quantity ?? row.bufferQuantity)
      const aggTotal = num(row.aggregate_total ?? row.aggregateTotal)
      const orig = num(row.original_forecast ?? row.originalForecast)
      const diff = num(row.difference)
      return [
        row.warehouse_location ?? row.warehouseLocation ?? '',
        row.category ?? '',
        row.spec ?? '',
        row.product_name ?? row.productName ?? '',
        row.product_code ?? row.productCode ?? '',
        ...channelCells,
        ...aggMonths,
        buf,
        aggTotal,
        orig,
        diff,
        row.remarks ?? '',
      ]
    })
    const csvContent = [
      headers.join(','),
      ...rows.map((r) =>
        r.map((c) => (typeof c === 'string' && (c.includes(',') || c.includes('"') || c.includes('\n')) ? `"${String(c).replace(/"/g, '""')}"` : c)).join(',')
      ),
    ].join('\n')
    const BOM = '\uFEFF'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `生產表單_${year}.csv`
    a.click()
    URL.revokeObjectURL(url)
    toast.success('已匯出 CSV')
  }

  if (accessDenied || (!loading && !canView)) {
    return (
      <div className="production-plan-page">
        <h1>生產表單</h1>
        <div className="production-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  const yearOptions = []
  for (let y = 2020; y <= 2030; y++) yearOptions.push(y)

  const channelOrder = data[0]?.channel_data?.map((c) => c.channel) ?? []

  return (
    <div className="production-plan-page">
      <div className="production-page-header">
        <h1>生產表單</h1>
        {data.length > 0 && canView && (
          <button
            type="button"
            className="btn btn--outline"
            onClick={exportExcel}
          >
            Excel 匯出
          </button>
        )}
      </div>

      <div className="production-controls">
        <div className="control-group">
          <label htmlFor="year-select">年份</label>
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
            type="button"
            className="btn btn--primary"
            onClick={handleQuery}
            disabled={loading}
          >
            {loading ? '查詢中...' : '查詢'}
          </button>
        </div>
        {data.length > 0 && (
          <div className="production-info">共 {data.length} 筆</div>
        )}
      </div>

      {loading ? (
        <div className="production-loading" role="status">
          載入中...
        </div>
      ) : data.length === 0 ? (
        <div className="production-empty">
          請選擇年份並按「查詢」取得生產表單資料
        </div>
      ) : (
        <div className="production-grid-wrap">
          <table className="production-grid production-form-table">
            <thead>
              <tr>
                <th className="frozen-col frozen-col-1">庫位</th>
                <th className="frozen-col frozen-col-2">中類名稱</th>
                <th className="frozen-col frozen-col-3">貨品規格</th>
                <th>品名</th>
                <th>品號</th>
                {channelOrder.map((ch) => (
                  <th key={ch} colSpan={MONTH_KEYS.length + 1} className="channel-group">
                    {ch}
                  </th>
                ))}
                <th colSpan={MONTH_KEYS.length + 2} className="aggregate-group">
                  合計
                </th>
                <th className="numeric-col">原始預估</th>
                <th className="numeric-col">差異</th>
                <th>備註</th>
              </tr>
              <tr>
                <th className="frozen-col frozen-col-1" />
                <th className="frozen-col frozen-col-2" />
                <th className="frozen-col frozen-col-3" />
                <th />
                <th />
                {channelOrder.map((ch) => (
                  <Fragment key={ch}>
                    {MONTH_KEYS.map((k) => (
                      <th key={`${ch}-${k}`} className="month-col">
                        {MONTH_LABELS[k]}
                      </th>
                    ))}
                    <th className="numeric-col">Total</th>
                  </Fragment>
                ))}
                {MONTH_KEYS.map((k) => (
                  <th key={`agg-${k}`} className="month-col">
                    {MONTH_LABELS[k]}
                  </th>
                ))}
                <th className="numeric-col">緩衝量{canEdit && <span className="edit-hint">(可點擊編輯)</span>}</th>
                <th className="numeric-col">Total</th>
                <th className="numeric-col" />
                <th className="numeric-col" />
                <th />
              </tr>
            </thead>
            <tbody>
              {data.map((row, idx) => {
                const rowId = getRowId(row)
                const isEditing = editingRowId === rowId
                const cd = row.channel_data ?? []
                const agg = row.aggregate_months ?? row.aggregateMonths ?? {}
                const buf = row.buffer_quantity ?? row.bufferQuantity
                const aggTotal = row.aggregate_total ?? row.aggregateTotal
                const orig = row.original_forecast ?? row.originalForecast
                const diff = num(row.difference)

                return (
                  <tr key={rowId}>
                    <td className="frozen-col frozen-col-1">
                      {row.warehouse_location ?? row.warehouseLocation ?? '-'}
                    </td>
                    <td className="frozen-col frozen-col-2">
                      {row.category ?? '-'}
                    </td>
                    <td className="frozen-col frozen-col-3">{row.spec ?? '-'}</td>
                    <td>{row.product_name ?? row.productName ?? '-'}</td>
                    <td>{row.product_code ?? row.productCode ?? '-'}</td>
                    {channelOrder.map((ch) => {
                      const c = cd.find((x) => x.channel === ch) ?? {}
                      const months = c.months ?? {}
                      return (
                        <Fragment key={ch}>
                          {MONTH_KEYS.map((k) => (
                            <td key={`${ch}-${k}`} className="numeric-col">
                              {num(months[k])}
                            </td>
                          ))}
                          <td className="numeric-col">{num(c.total)}</td>
                        </Fragment>
                      )
                    })}
                    {MONTH_KEYS.map((k) => (
                      <td key={`agg-${k}`} className="numeric-col">
                        {num(agg[k])}
                      </td>
                    ))}
                    <td className="numeric-col editable-cell">
                      {isEditing ? (
                        <input
                          ref={editInputRef}
                          type="text"
                          className="cell-input"
                          value={editingValue}
                          onChange={(e) => setEditingValue(e.target.value)}
                          onBlur={() => saveBuffer(row)}
                          onKeyDown={(e) => handleEditKeyDown(e, row)}
                        />
                      ) : (
                        <span
                          className={canEdit ? 'editable' : ''}
                          onClick={() => startEditBuffer(row)}
                          role={canEdit ? 'button' : undefined}
                          tabIndex={canEdit ? 0 : undefined}
                          title={canEdit ? '點擊編輯緩衝量' : undefined}
                        >
                          {num(buf)}
                        </span>
                      )}
                    </td>
                    <td className="numeric-col">{num(aggTotal)}</td>
                    <td className="numeric-col">{num(orig)}</td>
                    <td
                      className={`numeric-col ${
                        diff > 0 ? 'diff-positive' : diff < 0 ? 'diff-negative' : ''
                      }`}
                    >
                      {diff > 0 ? '+' : ''}
                      {diff}
                    </td>
                    <td className="remarks-cell">{row.remarks ?? '-'}</td>
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
