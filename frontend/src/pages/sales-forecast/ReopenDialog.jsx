import { useState } from 'react'
import { reopenChannels } from '../../api/forecastConfig'
import { useToast } from '../../components/Toast'
import { formatLocalDateTime, toDateTimeLocalValue } from '../../utils/dateTime'
import './ForecastConfig.css'

/** 開放補登：通路可複選、共用一個補登截止時間與原因；上限由後端給（開放後 3 個工作天） */
export default function ReopenDialog({ open, configId, month, channels, maxExpiresAt, onClose, onSuccess }) {
  const toast = useToast()
  const [selected, setSelected] = useState([])
  const [expiresAt, setExpiresAt] = useState('')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)

  if (!open) return null

  const overwrite = channels.filter((c) => selected.includes(c.channel) && c.correctionCount > 0)
  const canSubmit = !loading && selected.length > 0 && expiresAt !== '' && reason.trim() !== ''

  function reset() {
    setSelected([])
    setExpiresAt('')
    setReason('')
  }

  function handleToggle(channel) {
    setSelected((prev) => (prev.includes(channel) ? prev.filter((c) => c !== channel) : [...prev, channel]))
  }

  function handleClose() {
    reset()
    onClose()
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!canSubmit) return
    setLoading(true)
    try {
      // 依畫面通路順序送出
      const ordered = channels.map((c) => c.channel).filter((c) => selected.includes(c))
      await reopenChannels(configId, { channels: ordered, expiresAt, reason: reason.trim() })
      toast.success('已開放補登')
      reset()
      onSuccess()
    } catch (err) {
      toast.error(err.response?.data?.message || '開放補登失敗')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="dialog-overlay" onClick={handleClose}>
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reopen-dialog-title"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '560px' }}
      >
        <h3 id="reopen-dialog-title" className="dialog-title">開放補登 – {month}</h3>
        <form onSubmit={handleSubmit} noValidate>
          <fieldset className="fc-dialog-field">
            <legend>開放通路（可複選）</legend>
            <div className="fc-channel-checks">
              {channels.map((c) => {
                const busy = Boolean(c.activeWindow)
                const id = `reopen-ch-${c.channel}`
                return (
                  <label key={c.channel} htmlFor={id}>
                    <input
                      id={id}
                      type="checkbox"
                      checked={selected.includes(c.channel)}
                      disabled={busy || loading}
                      onChange={() => handleToggle(c.channel)}
                    />
                    {busy ? `${c.channel}（補登中）` : c.channel}
                  </label>
                )
              })}
            </div>
          </fieldset>
          <div className="fc-dialog-field">
            <label htmlFor="reopenExpiresAt">補登截止時間</label>
            <input
              id="reopenExpiresAt"
              className="form-input"
              type="datetime-local"
              value={expiresAt}
              max={toDateTimeLocalValue(maxExpiresAt)}
              onChange={(e) => setExpiresAt(e.target.value)}
              disabled={loading}
            />
            <div className="form-hint">上限：{formatLocalDateTime(maxExpiresAt)}（開放後 3 個工作天）</div>
          </div>
          <div className="fc-dialog-field">
            <label htmlFor="reopenReason">開放原因</label>
            <textarea
              id="reopenReason"
              className="form-input"
              rows={2}
              maxLength={200}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              disabled={loading}
            />
          </div>
          {overwrite.length > 0 && (
            <div className="fc-reopen-warning" role="alert">
              {overwrite.map((c) => `${c.channel}（${c.correctionCount} 筆）`).join('、')}
              截止後已有生管修正。業務補登後將覆蓋此通路數量，補登結束時系統自動產生新版本並保留差異。
            </div>
          )}
          <div className="dialog-actions">
            <button type="button" className="btn btn--secondary" onClick={handleClose} disabled={loading}>
              取消
            </button>
            <button type="submit" className="btn btn--primary" disabled={!canSubmit}>
              {loading ? '開放中...' : '確認開放'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
