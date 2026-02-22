import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Sidebar from '../Sidebar'
import { renderWithAuth } from '../../test/helpers'

describe('Sidebar', () => {
  const authValue = {
    isAuthenticated: true,
    user: { username: 'admin', fullName: 'System Administrator' },
    logout: vi.fn(),
  }

  it('renders all navigation links', () => {
    renderWithAuth(<Sidebar />, { authValue })

    expect(screen.getByText('儀表板')).toBeInTheDocument()
    expect(screen.getByText('使用者')).toBeInTheDocument()
    expect(screen.getByText('預測設定')).toBeInTheDocument()
    expect(screen.getByText('預測上傳')).toBeInTheDocument()
  })

  it('renders logout button', () => {
    renderWithAuth(<Sidebar />, { authValue })
    expect(screen.getByRole('button', { name: '登出' })).toBeInTheDocument()
  })

  it('displays user full name', () => {
    renderWithAuth(<Sidebar />, { authValue })
    expect(screen.getByText('System Administrator')).toBeInTheDocument()
  })

  it('calls logout on button click', async () => {
    const logoutMock = vi.fn()
    const user = userEvent.setup()
    renderWithAuth(<Sidebar />, {
      authValue: { ...authValue, logout: logoutMock },
    })

    await user.click(screen.getByRole('button', { name: '登出' }))
    expect(logoutMock).toHaveBeenCalledOnce()
  })

  it('highlights active link for dashboard', () => {
    renderWithAuth(<Sidebar />, {
      authValue,
      initialEntries: ['/'],
    })

    const dashboardLink = screen.getByText('儀表板').closest('a')
    expect(dashboardLink).toHaveClass('sidebar-link--active')
  })

  it('highlights active link for users page', () => {
    renderWithAuth(<Sidebar />, {
      authValue,
      initialEntries: ['/users'],
    })

    const usersLink = screen.getByText('使用者').closest('a')
    expect(usersLink).toHaveClass('sidebar-link--active')
  })
})
