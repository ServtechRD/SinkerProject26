import { useState } from 'react'
import { createMonths } from '../../api/forecastConfig'
import { useToast } from '../../components/Toast'
import './ForecastConfig.css'

const MONTH_PATTERN = /^\d{6}$/

function isValidMonth(val) {
  if (!MONTH_PATTERN.test(val)) return false
  const mm = parseInt(val.slice(4), 10)
  return mm >= 1 && mm <= 12
}

export default function CreateMonthsDialog({ open, onClose, onSuccess }) {
  const toast = useToast()
  const [startMonth, setStartMonth] = useState('')
  const [endMonth, setEndMonth] = useState('')
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState({})

  if (!open) return null

  function validate() {
    const errs = {}
    if (!startMonth) errs.startMonth = '必填'
    else if (!isValidMonth(startMonth)) errs.startMonth = '格式無效，請輸入 YYYYMM'
    if (!endMonth) errs.endMonth = '必填'
    else if (!isValidMonth(endMonth)) errs.endMonth = '格式無效，請輸入 YYYYMM'
    if (!errs.startMonth && !errs.endMonth && startMonth > endMonth) {
      errs.endMonth = '結束月份不可早於起始月份'
    }
    return errs
  }

  const validationErrors = validate()
  const canSubmit = !loading && Object.keys(validationErrors).length === 0

  async function handleSubmit(e) {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) {
      setErrors(errs)
      return
    }
    setLoading(true)
    try {
      const result = await createMonths(startMonth, endMonth)
      toast.success(`已成功建立 ${result.createdCount} 個月份`)
      setStartMonth('')
      setEndMonth('')
      setErrors({})
      onSuccess()
    } catch (err) {
      const status = err.response?.status
      const message = err.response?.data?.message
      if (status === 409) {
        toast.error(message || '部分月份已存在')
      } else if (status === 400) {
        toast.error(message || '輸入資料無效')
      } else {
        toast.error('建立月份失敗')
      }
    } finally {
      setLoading(false)
    }
  }

  function handleClose() {
    setStartMonth('')
    setEndMonth('')
    setErrors({})
    onClose()
  }

  return (
    <div className="dialog-overlay" onClick={handleClose}>
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-dialog-title"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '480px' }}
      >
        <h3 id="create-dialog-title" className="dialog-title">建立月份</h3>
        <form onSubmit={handleSubmit}>
          <div className="fc-dialog-field">
            <label htmlFor="startMonth">起始月份</label>
            <input
              id="startMonth"
              className={`form-input${errors.startMonth ? ' error' : ''}`}
              type="text"
              placeholder="YYYYMM (例如 202601)"
              value={startMonth}
              onChange={(e) => {
                setStartMonth(e.target.value)
                setErrors((prev) => ({ ...prev, startMonth: undefined }))
              }}
              maxLength={6}
              disabled={loading}
            />
            {errors.startMonth && <div className="form-error">{errors.startMonth}</div>}
          </div>
          <div className="fc-dialog-field">
            <label htmlFor="endMonth">結束月份</label>
            <input
              id="endMonth"
              className={`form-input${errors.endMonth ? ' error' : ''}`}
              type="text"
              placeholder="YYYYMM (例如 202603)"
              value={endMonth}
              onChange={(e) => {
                setEndMonth(e.target.value)
                setErrors((prev) => ({ ...prev, endMonth: undefined }))
              }}
              maxLength={6}
              disabled={loading}
            />
            {errors.endMonth && <div className="form-error">{errors.endMonth}</div>}
          </div>
          <div className="dialog-actions">
            <button
              type="button"
              className="btn btn--secondary"
              onClick={handleClose}
              disabled={loading}
            >
              取消
            </button>
            <button
              type="submit"
              className="btn btn--primary"
              disabled={!canSubmit}
            >
              {loading ? '建立中...' : '建立'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
