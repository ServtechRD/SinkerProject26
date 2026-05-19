import { useState } from 'react'
import { createMonth } from '../../api/forecastConfig'
import { useToast } from '../../components/Toast'
import './ForecastConfig.css'

function getTodayString() {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

export default function CreateMonthsDialog({ open, onClose, onSuccess }) {
  const toast = useToast()
  const [month, setMonth] = useState('')
  const [autoCloseDate, setAutoCloseDate] = useState('')
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState({})

  if (!open) return null

  const today = getTodayString()

  function validate() {
    const errs = {}
    if (!month) errs.month = '必填'
    if (!autoCloseDate) errs.autoCloseDate = '必填'
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
      // type="month" 回傳 YYYY-MM，後端要求 YYYYMM
      const monthValue = month.replace('-', '')
      await createMonth(monthValue, autoCloseDate)
      toast.success('月份建立成功')
      setMonth('')
      setAutoCloseDate('')
      setErrors({})
      onSuccess()
    } catch (err) {
      const status = err.response?.status
      const message = err.response?.data?.message
      if (status === 409) {
        toast.error(message || '該月份已存在')
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
    setMonth('')
    setAutoCloseDate('')
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
        <h3 id="create-dialog-title" className="dialog-title">新增填寫月份</h3>
        <form onSubmit={handleSubmit}>
          <div className="fc-dialog-field">
            <label htmlFor="month">填寫月份</label>
            <input
              id="month"
              className={`form-input${errors.month ? ' error' : ''}`}
              type="month"
              value={month}
              onChange={(e) => {
                setMonth(e.target.value)
                setErrors((prev) => ({ ...prev, month: undefined }))
              }}
              disabled={loading}
            />
            {errors.month && <div className="form-error">{errors.month}</div>}
          </div>
          <div className="fc-dialog-field">
            <label htmlFor="autoCloseDate">系統自動結束新增設定日期</label>
            <input
              id="autoCloseDate"
              className={`form-input${errors.autoCloseDate ? ' error' : ''}`}
              type="date"
              value={autoCloseDate}
              min={today}
              onChange={(e) => {
                setAutoCloseDate(e.target.value)
                setErrors((prev) => ({ ...prev, autoCloseDate: undefined }))
              }}
              disabled={loading}
            />
            {errors.autoCloseDate && <div className="form-error">{errors.autoCloseDate}</div>}
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
