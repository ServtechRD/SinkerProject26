import { useState, useEffect } from 'react'
import { updateConfig } from '../../api/forecastConfig'
import { useToast } from '../../components/Toast'
import './ForecastConfig.css'

export default function EditConfigDialog({ open, config, onClose, onSuccess }) {
  const toast = useToast()
  const [autoCloseDay, setAutoCloseDay] = useState('')
  const [isClosed, setIsClosed] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (config) {
      setAutoCloseDay(String(config.autoCloseDay ?? ''))
      setIsClosed(!!config.isClosed)
      setError('')
    }
  }, [config])

  if (!open || !config) return null

  const dayNum = parseInt(autoCloseDay, 10)
  const dayValid = autoCloseDay !== '' && !isNaN(dayNum) && dayNum >= 1 && dayNum <= 31
  const dayError = autoCloseDay !== '' && !dayValid ? '自動關帳日需為 1-31' : ''

  const hasChanges =
    (dayValid && dayNum !== config.autoCloseDay) || isClosed !== !!config.isClosed

  const canSubmit = !loading && dayValid && !dayError && hasChanges

  async function handleSubmit(e) {
    e.preventDefault()
    if (!canSubmit) return
    setLoading(true)
    setError('')
    try {
      await updateConfig(config.id, {
        autoCloseDay: dayNum,
        isClosed,
      })
      toast.success('設定已更新')
      onSuccess()
    } catch (err) {
      const status = err.response?.status
      const message = err.response?.data?.message
      if (status === 400) {
        setError(message || '輸入資料無效')
      } else {
        setError(message || '更新失敗')
      }
      toast.error(message || '更新失敗')
    } finally {
      setLoading(false)
    }
  }

  function handleClose() {
    setError('')
    onClose()
  }

  return (
    <div className="dialog-overlay" onClick={handleClose}>
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="edit-dialog-title"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '480px' }}
      >
        <h3 id="edit-dialog-title" className="dialog-title">編輯設定</h3>
        {error && <div className="fc-dialog-api-error" role="alert">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="fc-dialog-field">
            <label htmlFor="editMonth">月份</label>
            <input
              id="editMonth"
              className="form-input"
              type="text"
              value={config.month}
              disabled
              readOnly
            />
          </div>
          <div className="fc-dialog-field">
            <label htmlFor="editAutoCloseDay">自動關帳日</label>
            <input
              id="editAutoCloseDay"
              className={`form-input${dayError ? ' error' : ''}`}
              type="number"
              min={1}
              max={31}
              value={autoCloseDay}
              onChange={(e) => setAutoCloseDay(e.target.value)}
              disabled={loading}
            />
            {dayError && <div className="form-error">{dayError}</div>}
            <div className="form-hint">輸入 1-31 的數字</div>
          </div>
          <div className="fc-switch-row">
            <label htmlFor="editIsClosed">已關帳</label>
            <label className="fc-switch">
              <input
                id="editIsClosed"
                type="checkbox"
                checked={isClosed}
                onChange={(e) => setIsClosed(e.target.checked)}
                disabled={loading}
              />
              <span className="fc-switch-slider" />
            </label>
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
              {loading ? '儲存中...' : '儲存'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
