import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ToastProvider } from '../../../components/Toast'
import CrossMonthSummary from '../CrossMonthSummary'

vi.mock('../../../api/forecast', () => ({
  getFormSummaryRange: vi.fn(),
}))

import { getFormSummaryRange } from '../../../api/forecast'

const configs = [
  { id: 3, month: '202701' },
  { id: 1, month: '202611' },
  { id: 2, month: '202612' },
  { id: 4, month: '202702' },
  { id: 5, month: '202703' },
]

const rangeData = {
  months: [
    { month: '202611', closed: true },
    { month: '202612', closed: false },
  ],
  rows: [
    {
      product_code: 'P001',
      product_name: '年節禮盒',
      month_quantities: { 202611: 300, 202612: 500 },
      total: 800,
      remarks: [
        { channel: 'PX + 大全聯', month: '202611', text: '首批鋪貨' },
        { channel: '7-11', month: '202612', text: '12/20前到貨' },
      ],
    },
    { product_code: 'P002', product_name: '蘇打餅', month_quantities: { 202611: 0, 202612: 7 }, total: 7, remarks: [] },
  ],
}

function renderSummary() {
  return render(
    <ToastProvider>
      <CrossMonthSummary configs={configs} />
    </ToastProvider>
  )
}

async function selectRange(start, end) {
  await userEvent.selectOptions(screen.getByLabelText('起始月份'), start)
  await userEvent.selectOptions(screen.getByLabelText('結束月份'), end)
}

describe('CrossMonthSummary (F127)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('lists month options in chronological order', () => {
    renderSummary()
    const options = within(screen.getByLabelText('起始月份')).getAllByRole('option').map((o) => o.value)
    expect(options).toEqual(['', '202611', '202612', '202701', '202702', '202703'])
  })

  it('queries range and renders month totals, overall total and open-month tag', async () => {
    getFormSummaryRange.mockResolvedValue(rangeData)
    renderSummary()
    await selectRange('202611', '202612')
    await userEvent.click(screen.getByRole('button', { name: '查詢' }))

    expect(getFormSummaryRange).toHaveBeenCalledWith('202611', '202612')
    await waitFor(() => expect(screen.getByText('年節禮盒')).toBeInTheDocument())
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent)
    expect(headers).toContain('2026-11')
    expect(headers).toContain('2026-12未關帳')
    const row = screen.getByText('年節禮盒').closest('tr')
    expect(within(row).getByText('300')).toBeInTheDocument()
    expect(within(row).getByText('500')).toBeInTheDocument()
    expect(within(row).getByText('800')).toBeInTheDocument()
    // 無備註顯示 -
    expect(screen.getByText('蘇打餅').closest('tr').querySelector('.forecast-remark-cell')).toHaveTextContent(/^-$/)
    // 唯讀：沒有編輯／儲存
    expect(screen.queryByRole('button', { name: '編輯' })).not.toBeInTheDocument()
  })

  it('opens remark modal listing remarks in month order', async () => {
    getFormSummaryRange.mockResolvedValue(rangeData)
    renderSummary()
    await selectRange('202611', '202612')
    await userEvent.click(screen.getByRole('button', { name: '查詢' }))
    await waitFor(() => expect(screen.getByText('年節禮盒')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: '備註' }))
    const dialog = screen.getByRole('dialog', { name: '備註' })
    const lines = within(dialog).getAllByRole('listitem').map((li) => li.textContent)
    expect(lines).toEqual(['[PX + 大全聯/11月]首批鋪貨', '[7-11/12月]12/20前到貨'])
    expect(within(dialog).queryByRole('textbox')).not.toBeInTheDocument()
  })

  it('blocks ranges over 4 months or with start after end', async () => {
    renderSummary()
    await selectRange('202611', '202703')
    expect(screen.getByText('查詢區間最多 4 個月')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '查詢' })).toBeDisabled()

    await selectRange('202612', '202611')
    expect(screen.getByText('起始月份不可晚於結束月份')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '查詢' })).toBeDisabled()
    expect(getFormSummaryRange).not.toHaveBeenCalled()
  })
})
