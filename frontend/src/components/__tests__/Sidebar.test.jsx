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

    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.getByText('Users')).toBeInTheDocument()
    expect(screen.getByText('Forecast Config')).toBeInTheDocument()
    expect(screen.getByText('Forecast Upload')).toBeInTheDocument()
  })

  it('renders logout button', () => {
    renderWithAuth(<Sidebar />, { authValue })
    expect(screen.getByRole('button', { name: /logout/i })).toBeInTheDocument()
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

    await user.click(screen.getByRole('button', { name: /logout/i }))
    expect(logoutMock).toHaveBeenCalledOnce()
  })

  it('highlights active link for dashboard', () => {
    renderWithAuth(<Sidebar />, {
      authValue,
      initialEntries: ['/'],
    })

    const dashboardLink = screen.getByText('Dashboard').closest('a')
    expect(dashboardLink).toHaveClass('sidebar-link--active')
  })

  it('highlights active link for users page', () => {
    renderWithAuth(<Sidebar />, {
      authValue,
      initialEntries: ['/users'],
    })

    const usersLink = screen.getByText('Users').closest('a')
    expect(usersLink).toHaveClass('sidebar-link--active')
  })
})
