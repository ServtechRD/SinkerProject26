import { useState, useEffect } from 'react'
import { updateConfig } from '../../api/forecastConfig'
import { useToast } from '../../components/Toast'
import ConfirmDialog from '../../components/ConfirmDialog'
import './ForecastConfig.css'

/** 設定檔 YYYYMM → 顯示用 YYYY-MM */
function formatOpenMonthLabel(monthStr) {
  const s = String(monthStr ?? '').trim()
  if (s.length === 6 && /^\d{6}$/.test(s)) {
    return `${s.slice(0, 4)}-${s.slice(4, 6)}`
  }
  return s || '—'
}

export default function EditConfigDialog({ open, config, onClose, onSuccess }) {
  const toast = useToast()
  const [autoCloseDay, setAutoCloseDay] = useState('')
  const [isClosed, setIsClosed] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [closeConfirmOpen, setCloseConfirmOpen] = useState(false)

  useEffect(() => {
    if (config) {
      setAutoCloseDay(String(config.autoCloseDay ?? ''))
      setIsClosed(!!config.isClosed)
      setError('')
      setCloseConfirmOpen(false)
    }
  }, [config])

  if (!open || !config) return null

  const openMonthLabel = formatOpenMonthLabel(config.month)
  const closeConfirmMessage = `確定要結束新增設定 ${openMonthLabel} 表單嗎？關閉表單後業務同仁就無法再編輯。`

  const dayNum = parseInt(autoCloseDay, 10)
  const dayValid = autoCloseDay !== '' && !isNaN(dayNum) && dayNum >= 1 && dayNum <= 31
  const dayError = autoCloseDay !== '' && !dayValid ? '自動結束新增設定日期需為 1-31' : ''

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
    setCloseConfirmOpen(false)
    onClose()
  }

  function handleIsClosedChange(wantClosed) {
    if (!config || config.isClosed) return
    if (wantClosed) {
      setCloseConfirmOpen(true)
    } else {
      setIsClosed(false)
    }
  }

  function handleCloseConfirmOk() {
    setIsClosed(true)
    setCloseConfirmOpen(false)
  }

  return (
    <>
    <div
      className="dialog-overlay"
      onClick={() => {
        if (!closeConfirmOpen) handleClose()
      }}
    >
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="edit-dialog-title"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '480px' }}
      >
        <h3 id="edit-dialog-title" className="dialog-title">編輯填寫月份</h3>
        {error && <div className="fc-dialog-api-error" role="alert">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="fc-dialog-field">
            <label htmlFor="editMonth">填寫月份</label>
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
            <label htmlFor="editAutoCloseDay">自動結束新增設定日期</label>
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
            <label htmlFor="editIsClosed">結束新增設定</label>
            <label className="fc-switch">
              <input
                id="editIsClosed"
                type="checkbox"
                checked={isClosed}
                onChange={(e) => handleIsClosedChange(e.target.checked)}
                disabled={loading || !!config.isClosed}
              />
              <span className="fc-switch-slider" />
            </label>
          </div>
          {config.isClosed && (
            <div className="form-hint">已結束新增設定，無法再開放；仍可修改自動結束新增設定日期。</div>
          )}
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

    <ConfirmDialog
      open={closeConfirmOpen}
      title="確認結束新增設定"
      message={closeConfirmMessage}
      onConfirm={handleCloseConfirmOk}
      onCancel={() => setCloseConfirmOpen(false)}
      confirmText="確定"
      confirmButtonClass="btn--primary"
    />
    </>
  )
}
