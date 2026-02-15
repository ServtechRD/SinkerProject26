import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import MainLayout from '../MainLayout'
import { renderWithAuth } from '../../test/helpers'

describe('MainLayout', () => {
  const authValue = {
    isAuthenticated: true,
    user: { username: 'admin', fullName: 'Admin' },
    logout: vi.fn(),
  }

  it('renders sidebar', () => {
    renderWithAuth(
      <Routes>
        <Route element={<MainLayout />}>
          <Route index element={<div>Dashboard</div>} />
        </Route>
      </Routes>,
      { authValue }
    )

    expect(screen.getByTestId('sidebar')).toBeInTheDocument()
  })

  it('renders main content area with outlet', () => {
    renderWithAuth(
      <Routes>
        <Route element={<MainLayout />}>
          <Route index element={<div>Dashboard Content</div>} />
        </Route>
      </Routes>,
      { authValue }
    )

    expect(screen.getByTestId('main-content')).toBeInTheDocument()
    expect(screen.getByText('Dashboard Content')).toBeInTheDocument()
  })
})
