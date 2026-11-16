import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ToastProvider } from '../../../components/Toast'
import EditConfigDialog from '../EditConfigDialog'

vi.mock('../../../api/forecastConfig', () => ({ updateConfig: vi.fn() }))

import { updateConfig } from '../../../api/forecastConfig'

const openConfig = { id: 1, month: '202612', autoCloseAt: '2026-12-10T00:00:00', isClosed: false, closedAt: null }
const closedConfig = { ...openConfig, isClosed: true, closedAt: '2026-12-09T16:00:00' }

function renderDialog(config) {
  const onSuccess = vi.fn()
  const utils = render(
    <ToastProvider>
      <EditConfigDialog open config={config} onClose={vi.fn()} onSuccess={onSuccess} />
    </ToastProvider>
  )
  return { ...utils, onSuccess }
}

describe('EditConfigDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('帶入既有截止時間（日期加時間）', () => {
    renderDialog(openConfig)
    const input = screen.getByLabelText('自動結束新增設定時間')
    expect(input).toHaveAttribute('type', 'datetime-local')
    expect(input).toHaveValue('2026-12-10T00:00')
  })

  it('未變更時不能儲存', () => {
    renderDialog(openConfig)
    expect(screen.getByRole('button', { name: '儲存' })).toBeDisabled()
  })

  it('修改截止時間後送出', async () => {
    updateConfig.mockResolvedValue({})
    const { onSuccess } = renderDialog(openConfig)
    fireEvent.change(screen.getByLabelText('自動結束新增設定時間'), { target: { value: '2026-12-11T18:00' } })
    fireEvent.click(screen.getByRole('button', { name: '儲存' }))
    await waitFor(() => expect(updateConfig).toHaveBeenCalledWith(1, { autoCloseAt: '2026-12-11T18:00', isClosed: false }))
    expect(onSuccess).toHaveBeenCalled()
  })

  it('已結束的月份不能改回開放', () => {
    const { container } = renderDialog(closedConfig)
    expect(container.querySelector('#editIsClosed')).toBeDisabled()
    expect(screen.getByText(/已結束新增設定，無法再開放/)).toBeInTheDocument()
  })

  it('400 顯示後端訊息', async () => {
    updateConfig.mockRejectedValue({ response: { status: 400, data: { message: 'autoCloseAt format must be YYYY-MM-DDTHH:mm' } } })
    renderDialog(openConfig)
    fireEvent.change(screen.getByLabelText('自動結束新增設定時間'), { target: { value: '2026-12-11T18:00' } })
    fireEvent.click(screen.getByRole('button', { name: '儲存' }))
    expect((await screen.findAllByRole('alert'))[0]).toHaveTextContent('autoCloseAt format')
  })
})
