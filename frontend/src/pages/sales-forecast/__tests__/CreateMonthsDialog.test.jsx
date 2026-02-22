import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ToastProvider } from '../../../components/Toast'
import CreateMonthsDialog from '../CreateMonthsDialog'

vi.mock('../../../api/forecastConfig', () => ({
  createMonths: vi.fn(),
}))

import { createMonths } from '../../../api/forecastConfig'

function renderDialog(props = {}) {
  const defaultProps = {
    open: true,
    onClose: vi.fn(),
    onSuccess: vi.fn(),
    ...props,
  }
  return {
    ...render(
      <ToastProvider>
        <CreateMonthsDialog {...defaultProps} />
      </ToastProvider>
    ),
    props: defaultProps,
  }
}

describe('CreateMonthsDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders dialog when open', () => {
    renderDialog()
    expect(screen.getByText('建立月份')).toBeInTheDocument()
    expect(screen.getByLabelText('起始月份')).toBeInTheDocument()
    expect(screen.getByLabelText('結束月份')).toBeInTheDocument()
  })

  it('does not render when not open', () => {
    renderDialog({ open: false })
    expect(screen.queryByText('建立月份')).not.toBeInTheDocument()
  })

  it('calls onClose when cancel clicked', async () => {
    const user = userEvent.setup()
    const { props } = renderDialog()

    await user.click(screen.getByRole('button', { name: '取消' }))
    expect(props.onClose).toHaveBeenCalled()
  })

  it('create button disabled with empty fields', () => {
    renderDialog()
    expect(screen.getByRole('button', { name: '建立' })).toBeDisabled()
  })

  it('create button disabled when start > end', async () => {
    const user = userEvent.setup()
    renderDialog()

    await user.type(screen.getByLabelText('起始月份'), '202605')
    await user.type(screen.getByLabelText('結束月份'), '202601')

    expect(screen.getByRole('button', { name: '建立' })).toBeDisabled()
  })

  it('create button enabled with valid input', async () => {
    const user = userEvent.setup()
    renderDialog()

    await user.type(screen.getByLabelText('起始月份'), '202601')
    await user.type(screen.getByLabelText('結束月份'), '202603')

    expect(screen.getByRole('button', { name: '建立' })).toBeEnabled()
  })

  it('calls API and onSuccess on successful creation', async () => {
    createMonths.mockResolvedValue({ createdCount: 3, months: ['202601', '202602', '202603'] })
    const user = userEvent.setup()
    const { props } = renderDialog()

    await user.type(screen.getByLabelText('起始月份'), '202601')
    await user.type(screen.getByLabelText('結束月份'), '202603')
    await user.click(screen.getByRole('button', { name: '建立' }))

    await waitFor(() => {
      expect(createMonths).toHaveBeenCalledWith('202601', '202603')
    })
    await waitFor(() => {
      expect(props.onSuccess).toHaveBeenCalled()
    })
  })

  it('shows error toast on 409 conflict', async () => {
    createMonths.mockRejectedValue({
      response: { status: 409, data: { message: 'Some months already exist' } },
    })
    const user = userEvent.setup()
    renderDialog()

    await user.type(screen.getByLabelText('起始月份'), '202601')
    await user.type(screen.getByLabelText('結束月份'), '202603')
    await user.click(screen.getByRole('button', { name: '建立' }))

    await waitFor(() => {
      expect(createMonths).toHaveBeenCalled()
    })
  })

  it('shows validation error for invalid month format', async () => {
    const user = userEvent.setup()
    renderDialog()

    await user.type(screen.getByLabelText('起始月份'), '2026')
    await user.type(screen.getByLabelText('結束月份'), '202603')

    expect(screen.getByRole('button', { name: '建立' })).toBeDisabled()
  })
})
