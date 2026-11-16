import { useState } from 'react'
import { getFormSummaryRange } from '../../api/forecast'
import { useToast } from '../../components/Toast'
import RemarkListModal from './RemarkListModal'
import { remarksToText, downloadCsv } from './remarkUtils'

const MAX_MONTHS = 4

/** YYYYMM → YYYY-MM */
function displayMonth(m) {
  return m && m.length === 6 ? `${m.slice(0, 4)}-${m.slice(4)}` : m
}

/** 起訖（含）共幾個月；起 > 迄時 ≤ 0 */
function monthCount(start, end) {
  const toIndex = (m) => Number(m.slice(0, 4)) * 12 + Number(m.slice(4))
  return toIndex(end) - toIndex(start) + 1
}

/** 跨月彙整：起訖月份內各品項每月合計（12 通路加總，含禮品）＋全期合計，唯讀 */
export default function CrossMonthSummary({ configs }) {
  const toast = useToast()
  const [startMonth, setStartMonth] = useState('')
  const [endMonth, setEndMonth] = useState('')
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState(null)
  const [remarkRow, setRemarkRow] = useState(null)

  const monthOptions = configs.map((c) => c.month).sort()
  const count = startMonth && endMonth ? monthCount(startMonth, endMonth) : 0
  const rangeError = !startMonth || !endMonth
    ? ''
    : count <= 0
      ? '起始月份不可晚於結束月份'
      : count > MAX_MONTHS
        ? `查詢區間最多 ${MAX_MONTHS} 個月`
        : ''

  async function handleQuery() {
    setLoading(true)
    try {
      setData(await getFormSummaryRange(startMonth, endMonth))
    } catch (err) {
      toast.error(err.response?.data?.message || '查詢失敗')
    } finally {
      setLoading(false)
    }
  }

  function handleExport() {
    const months = data.months.map((m) => m.month)
    const headers = ['庫位', '中類名稱', '貨品規格', '品名', '品號', ...months.map(displayMonth), '全期合計', '備註']
    const rows = data.rows.map((r) => [
      r.warehouse_location ?? '',
      r.category ?? '',
      r.spec ?? '',
      r.product_name ?? '',
      r.product_code ?? '',
      ...months.map((m) => r.month_quantities?.[m] ?? 0),
      r.total ?? 0,
      remarksToText(r.remarks),
    ])
    downloadCsv(`跨月彙整_${startMonth}-${endMonth}.csv`, [headers, ...rows])
  }

  return (
    <>
      <div className="forecast-filters">
        <div className="filter-field">
          <label htmlFor="range-start-select">起始月份</label>
          <select id="range-start-select" className="form-select" value={startMonth}
            onChange={(e) => { setStartMonth(e.target.value); setData(null) }}>
            <option value="">請選擇月份</option>
            {monthOptions.map((m) => <option key={m} value={m}>{displayMonth(m)}</option>)}
          </select>
        </div>
        <div className="filter-field">
          <label htmlFor="range-end-select">結束月份</label>
          <select id="range-end-select" className="form-select" value={endMonth}
            onChange={(e) => { setEndMonth(e.target.value); setData(null) }}>
            <option value="">請選擇月份</option>
            {monthOptions.map((m) => <option key={m} value={m}>{displayMonth(m)}</option>)}
          </select>
        </div>
        <div className="filter-field filter-field--button">
          <label>&nbsp;</label>
          <button type="button" className="btn btn--primary" onClick={handleQuery}
            disabled={!startMonth || !endMonth || !!rangeError || loading}>
            {loading ? '查詢中...' : '查詢'}
          </button>
        </div>
      </div>

      {rangeError && <p className="forecast-hint forecast-hint--warning">{rangeError}</p>}

      {data && (
        <section className="forecast-result-block">
          <h2>跨月彙整結果</h2>
          <div className="forecast-result-toolbar">
            <button type="button" className="btn btn--outline" onClick={handleExport} disabled={!data.rows.length}>
              Excel 匯出
            </button>
          </div>
          {data.rows.length === 0 ? (
            <div className="forecast-empty">查詢區間尚無預估資料</div>
          ) : (
            <div className="forecast-table-wrap">
              <table className="forecast-table">
                <thead>
                  <tr>
                    <th>庫位</th>
                    <th>中類名稱</th>
                    <th>貨品規格</th>
                    <th>品名</th>
                    <th>品號</th>
                    {data.months.map((m) => (
                      <th key={m.month} className="align-right">
                        {displayMonth(m.month)}
                        {!m.closed && <span className="forecast-month-open-tag">未關帳</span>}
                      </th>
                    ))}
                    <th className="align-right">全期合計</th>
                    <th>備註</th>
                  </tr>
                </thead>
                <tbody>
                  {data.rows.map((r) => (
                    <tr key={r.product_code}>
                      <td>{r.warehouse_location ?? '-'}</td>
                      <td>{r.category ?? '-'}</td>
                      <td>{r.spec ?? '-'}</td>
                      <td>{r.product_name ?? '-'}</td>
                      <td>{r.product_code}</td>
                      {data.months.map((m) => (
                        <td key={m.month} className="align-right">{r.month_quantities?.[m.month] ?? 0}</td>
                      ))}
                      <td className="align-right">{r.total ?? 0}</td>
                      <td className="forecast-remark-cell">
                        {r.remarks?.length ? (
                          <button type="button" className="btn btn--small btn--link forecast-remark-btn" onClick={() => setRemarkRow(r)}>
                            備註
                          </button>
                        ) : '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {remarkRow && (
        <RemarkListModal
          title={`備註：${remarkRow.product_name ?? ''}`}
          remarks={remarkRow.remarks}
          onClose={() => setRemarkRow(null)}
        />
      )}
    </>
  )
}
