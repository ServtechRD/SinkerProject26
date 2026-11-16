import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ToastProvider } from '../../../components/Toast'
import CreateMonthsDialog from '../CreateMonthsDialog'

vi.mock('../../../api/forecastConfig', () => ({ createMonth: vi.fn() }))

import { createMonth } from '../../../api/forecastConfig'

function renderDialog(open = true) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  render(
    <ToastProvider>
      <CreateMonthsDialog open={open} onClose={onClose} onSuccess={onSuccess} />
    </ToastProvider>
  )
  return { onClose, onSuccess }
}

function fillForm() {
  fireEvent.change(screen.getByLabelText('填寫月份'), { target: { value: '2026-12' } })
  fireEvent.change(screen.getByLabelText('系統自動結束新增設定時間'), { target: { value: '2026-12-10T18:30' } })
}

describe('CreateMonthsDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('open=false 時不渲染', () => {
    renderDialog(false)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('截止欄位為日期加時間，兩欄都填才能建立', () => {
    renderDialog()
    expect(screen.getByLabelText('系統自動結束新增設定時間')).toHaveAttribute('type', 'datetime-local')
    expect(screen.getByRole('button', { name: '建立' })).toBeDisabled()
    fillForm()
    expect(screen.getByRole('button', { name: '建立' })).toBeEnabled()
  })

  it('送出 YYYYMM 與截止時間', async () => {
    createMonth.mockResolvedValue({})
    const { onSuccess } = renderDialog()
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: '建立' }))
    await waitFor(() => expect(createMonth).toHaveBeenCalledWith('202612', '2026-12-10T18:30'))
    expect(onSuccess).toHaveBeenCalled()
  })

  it('409 顯示後端訊息', async () => {
    createMonth.mockRejectedValue({ response: { status: 409, data: { message: '該月份已存在' } } })
    renderDialog()
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: '建立' }))
    expect(await screen.findByText('該月份已存在')).toBeInTheDocument()
  })

  it('取消呼叫 onClose', () => {
    const { onClose } = renderDialog()
    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    expect(onClose).toHaveBeenCalled()
  })
})
