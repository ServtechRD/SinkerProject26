import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ToastProvider } from '../../../components/Toast'
import EditConfigDialog from '../EditConfigDialog'

vi.mock('../../../api/forecastConfig', () => ({
  updateConfig: vi.fn(),
}))

import { updateConfig } from '../../../api/forecastConfig'

const sampleConfig = {
  id: 1,
  month: '202601',
  autoCloseDay: 10,
  isClosed: false,
  closedAt: null,
}

function renderDialog(props = {}) {
  const defaultProps = {
    open: true,
    config: sampleConfig,
    onClose: vi.fn(),
    onSuccess: vi.fn(),
    ...props,
  }
  return {
    ...render(
      <ToastProvider>
        <EditConfigDialog {...defaultProps} />
      </ToastProvider>
    ),
    props: defaultProps,
  }
}

describe('EditConfigDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('displays current config values', () => {
    renderDialog()

    expect(screen.getByLabelText('月份')).toHaveValue('202601')
    expect(screen.getByLabelText('自動關帳日')).toHaveValue(10)
    expect(screen.getByLabelText('已關帳')).not.toBeChecked()
  })

  it('month field is disabled (read-only)', () => {
    renderDialog()
    expect(screen.getByLabelText('月份')).toBeDisabled()
  })

  it('shows validation error for auto_close_day > 31', async () => {
    const user = userEvent.setup()
    renderDialog()

    const input = screen.getByLabelText('自動關帳日')
    await user.clear(input)
    await user.type(input, '32')

    expect(screen.getByText('自動關帳日需為 1-31')).toBeInTheDocument()
  })

  it('shows validation error for auto_close_day < 1', async () => {
    const user = userEvent.setup()
    renderDialog()

    const input = screen.getByLabelText('自動關帳日')
    await user.clear(input)
    await user.type(input, '0')

    expect(screen.getByText('自動關帳日需為 1-31')).toBeInTheDocument()
  })

  it('no validation error for valid auto_close_day', async () => {
    const user = userEvent.setup()
    renderDialog()

    const input = screen.getByLabelText('自動關帳日')
    await user.clear(input)
    await user.type(input, '15')

    expect(screen.queryByText('自動關帳日需為 1-31')).not.toBeInTheDocument()
  })

  it('toggle changes is_closed state', async () => {
    const user = userEvent.setup()
    renderDialog()

    const toggle = screen.getByLabelText('已關帳')
    expect(toggle).not.toBeChecked()

    await user.click(toggle)
    expect(toggle).toBeChecked()
  })

  it('save button disabled when no changes', () => {
    renderDialog()
    expect(screen.getByRole('button', { name: '儲存' })).toBeDisabled()
  })

  it('save button enabled when auto_close_day changes', async () => {
    const user = userEvent.setup()
    renderDialog()

    const input = screen.getByLabelText('自動關帳日')
    await user.clear(input)
    await user.type(input, '20')

    expect(screen.getByRole('button', { name: '儲存' })).toBeEnabled()
  })

  it('calls API and onSuccess on successful update', async () => {
    updateConfig.mockResolvedValue({ ...sampleConfig, autoCloseDay: 20 })
    const user = userEvent.setup()
    const { props } = renderDialog()

    const input = screen.getByLabelText('自動關帳日')
    await user.clear(input)
    await user.type(input, '20')
    await user.click(screen.getByRole('button', { name: '儲存' }))

    await waitFor(() => {
      expect(updateConfig).toHaveBeenCalledWith(1, { autoCloseDay: 20, isClosed: false })
    })
    await waitFor(() => {
      expect(props.onSuccess).toHaveBeenCalled()
    })
  })

  it('shows error on 400 bad request', async () => {
    updateConfig.mockRejectedValue({
      response: { status: 400, data: { message: 'Invalid input' } },
    })
    const user = userEvent.setup()
    renderDialog()

    const input = screen.getByLabelText('自動關帳日')
    await user.clear(input)
    await user.type(input, '20')
    await user.click(screen.getByRole('button', { name: '儲存' }))

    await waitFor(() => {
      const alerts = screen.getAllByRole('alert')
      const hasError = alerts.some((el) => el.textContent.includes('Invalid input'))
      expect(hasError).toBe(true)
    })
  })

  it('does not render when not open', () => {
    renderDialog({ open: false })
    expect(screen.queryByText('編輯設定')).not.toBeInTheDocument()
  })

  it('calls onClose when cancel clicked', async () => {
    const user = userEvent.setup()
    const { props } = renderDialog()

    await user.click(screen.getByRole('button', { name: '取消' }))
    expect(props.onClose).toHaveBeenCalled()
  })
})
