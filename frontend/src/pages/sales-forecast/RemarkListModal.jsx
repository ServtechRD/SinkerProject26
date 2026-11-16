import { remarkLabel } from './remarkUtils'

/**
 * 列備註彈窗：逐行列出 [通路/M月] 內容；傳入 onChangeText 時每則為可編輯輸入框
 */
export default function RemarkListModal({ title, remarks, onChangeText, onClose }) {
  return (
    <div className="forecast-remark-modal-overlay" onClick={onClose} role="presentation">
      <div className="forecast-remark-modal" role="dialog" aria-label="備註" onClick={(e) => e.stopPropagation()}>
        <div className="forecast-remark-modal-header">
          <h3>{title}</h3>
          <button type="button" className="forecast-remark-modal-close" onClick={onClose} aria-label="關閉">×</button>
        </div>
        <ul className="forecast-remark-modal-list">
          {remarks.map((r, ri) => (
            <li key={`${r.month}-${r.channel}`} className="forecast-remark-line">
              <span className="forecast-remark-label">{remarkLabel(r)}</span>
              {onChangeText ? (
                <input
                  type="text"
                  className="form-select forecast-remark-input"
                  value={r.text ?? ''}
                  onChange={(e) => onChangeText(ri, e.target.value)}
                  aria-label={`${remarkLabel(r)} 備註`}
                />
              ) : (
                <span>{r.text}</span>
              )}
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
