import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import ConfirmDialog from '../ConfirmDialog'

describe('ConfirmDialog', () => {
  it('renders nothing when not open', () => {
    const { container } = render(
      <ConfirmDialog open={false} message="test" onConfirm={vi.fn()} onCancel={vi.fn()} />
    )
    expect(container.innerHTML).toBe('')
  })

  it('renders with title and message', () => {
    render(
      <ConfirmDialog open title="確認刪除" message="確定要刪除嗎？" onConfirm={vi.fn()} onCancel={vi.fn()} />
    )
    expect(screen.getByText('確認刪除')).toBeInTheDocument()
    expect(screen.getByText('確定要刪除嗎？')).toBeInTheDocument()
  })

  it('cancel button calls onCancel', async () => {
    const onCancel = vi.fn()
    const user = userEvent.setup()
    render(
      <ConfirmDialog open message="test" onConfirm={vi.fn()} onCancel={onCancel} />
    )
    await user.click(screen.getByText('取消'))
    expect(onCancel).toHaveBeenCalled()
  })

  it('confirm button calls onConfirm', async () => {
    const onConfirm = vi.fn()
    const user = userEvent.setup()
    render(
      <ConfirmDialog open message="test" onConfirm={onConfirm} onCancel={vi.fn()} />
    )
    await user.click(screen.getByText('刪除'))
    expect(onConfirm).toHaveBeenCalled()
  })

  it('shows loading state on confirm button', () => {
    render(
      <ConfirmDialog open message="test" onConfirm={vi.fn()} onCancel={vi.fn()} loading />
    )
    expect(screen.getByText('處理中...')).toBeInTheDocument()
  })
})
