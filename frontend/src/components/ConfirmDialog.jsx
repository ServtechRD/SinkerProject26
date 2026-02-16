import './ConfirmDialog.css'

export default function ConfirmDialog({ open, title, message, onConfirm, onCancel, loading }) {
  if (!open) return null

  return (
    <div className="dialog-overlay" onClick={onCancel}>
      <div
        className="dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="dialog-title"
        onClick={(e) => e.stopPropagation()}
      >
        {title && (
          <h3 id="dialog-title" className="dialog-title">
            {title}
          </h3>
        )}
        <p className="dialog-message">{message}</p>
        <div className="dialog-actions">
          <button className="btn btn--secondary" onClick={onCancel} disabled={loading}>
            取消
          </button>
          <button className="btn btn--danger" onClick={onConfirm} disabled={loading}>
            {loading ? '處理中...' : '刪除'}
          </button>
        </div>
      </div>
    </div>
  )
}
