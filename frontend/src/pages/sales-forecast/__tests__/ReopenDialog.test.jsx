import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ToastProvider } from '../../../components/Toast'
import ReopenDialog from '../ReopenDialog'

vi.mock('../../../api/forecastConfig', () => ({ reopenChannels: vi.fn() }))

import { reopenChannels } from '../../../api/forecastConfig'

const channels = [
  { channel: '家樂福', activeWindow: null, correctionCount: 2 },
  { channel: '愛買', activeWindow: null, correctionCount: 0 },
  { channel: '7-11', activeWindow: { id: 9, expiresAt: '2026-12-11T18:00:00', reason: 'x' }, correctionCount: 0 },
]

function renderDialog() {
  const onSuccess = vi.fn()
  render(
    <ToastProvider>
      <ReopenDialog open configId="7" month="202612" channels={channels}
        maxExpiresAt="2026-12-15T08:00:00" onClose={vi.fn()} onSuccess={onSuccess} />
    </ToastProvider>
  )
  return { onSuccess }
}

describe('ReopenDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('補登中的通路不能選，期限上限顯示在提示', () => {
    renderDialog()
    expect(screen.getByLabelText('7-11（補登中）')).toBeDisabled()
    expect(screen.getByText(/上限：2026\/12\/15 08:00/)).toBeInTheDocument()
    expect(screen.getByLabelText('補登截止時間')).toHaveAttribute('max', '2026-12-15T08:00')
  })

  it('通路、期限、原因都填才能送出', () => {
    renderDialog()
    const submit = screen.getByRole('button', { name: '確認開放' })
    expect(submit).toBeDisabled()
    fireEvent.click(screen.getByLabelText('愛買'))
    fireEvent.change(screen.getByLabelText('補登截止時間'), { target: { value: '2026-12-12T18:00' } })
    expect(submit).toBeDisabled()
    fireEvent.change(screen.getByLabelText('開放原因'), { target: { value: '新品追加' } })
    expect(submit).toBeEnabled()
  })

  it('選到已有生管修正的通路時顯示覆蓋提醒', () => {
    renderDialog()
    expect(screen.queryByText(/將覆蓋此通路數量/)).not.toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('家樂福'))
    expect(screen.getByText(/家樂福（2 筆）/)).toBeInTheDocument()
    expect(screen.getByText(/將覆蓋此通路數量/)).toBeInTheDocument()
  })

  it('送出可複選通路並呼叫 onSuccess', async () => {
    reopenChannels.mockResolvedValue()
    const { onSuccess } = renderDialog()
    fireEvent.click(screen.getByLabelText('家樂福'))
    fireEvent.click(screen.getByLabelText('愛買'))
    fireEvent.change(screen.getByLabelText('補登截止時間'), { target: { value: '2026-12-12T18:00' } })
    fireEvent.change(screen.getByLabelText('開放原因'), { target: { value: ' 新品追加 ' } })
    fireEvent.click(screen.getByRole('button', { name: '確認開放' }))
    await waitFor(() => expect(reopenChannels).toHaveBeenCalledWith('7', {
      channels: ['家樂福', '愛買'], expiresAt: '2026-12-12T18:00', reason: '新品追加',
    }))
    expect(onSuccess).toHaveBeenCalled()
  })

  it('後端拒絕時顯示訊息', async () => {
    reopenChannels.mockRejectedValue({ response: { status: 400, data: { message: '補登期限不可超過 2026/12/15 08:00' } } })
    renderDialog()
    fireEvent.click(screen.getByLabelText('愛買'))
    fireEvent.change(screen.getByLabelText('補登截止時間'), { target: { value: '2026-12-16T18:00' } })
    fireEvent.change(screen.getByLabelText('開放原因'), { target: { value: 'x' } })
    fireEvent.click(screen.getByRole('button', { name: '確認開放' }))
    expect(await screen.findByText('補登期限不可超過 2026/12/15 08:00')).toBeInTheDocument()
  })
})
