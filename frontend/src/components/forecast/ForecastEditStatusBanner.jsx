import { formatLocalDateTime, remainingText } from '../../utils/dateTime'
import './ForecastEditStatusBanner.css'

/** 業務上傳頁的截止／補登提示（PDF R4 第 19 頁） */
export default function ForecastEditStatusBanner({ status, month }) {
  if (!status || status.status === 'OPEN') return null

  if (status.status === 'REOPENED') {
    return (
      <div className="forecast-edit-status forecast-edit-status--reopen" role="status">
        <strong>補登中</strong>
        生管已開放此通路補登至 {formatLocalDateTime(status.reopenExpiresAt)}，{remainingText(status.reopenExpiresAt)}。
        原因：{status.reopenReason}
      </div>
    )
  }

  return (
    <div className="forecast-edit-status forecast-edit-status--closed" role="alert">
      <strong>已結束新增</strong>
      {month} 表單截止時間 {formatLocalDateTime(status.autoCloseAt)}，上傳與編輯已關閉。如需異動請洽生管（生管可直接修正，或開放補登）。
    </div>
  )
}

/** 「生管最新版數量」欄：與業務數量不同時紅字 */
export function PlannerQtyCell({ item, planner }) {
  const code = item.product_code ?? item.productCode
  const qty = planner?.[code]
  if (qty == null) return <td className="align-right">-</td>
  const changed = Number(qty) !== Number(item.quantity)
  return <td className={`align-right${changed ? ' planner-qty--changed' : ''}`}>{qty}</td>
}
