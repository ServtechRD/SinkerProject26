import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import ProtectedRoute from '../ProtectedRoute'
import { renderWithAuth } from '../../test/helpers'

describe('ProtectedRoute', () => {
  it('redirects to /login when not authenticated', () => {
    renderWithAuth(
      <Routes>
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <div>Protected Content</div>
            </ProtectedRoute>
          }
        />
        <Route path="/login" element={<div>Login Page</div>} />
      </Routes>,
      { authValue: { isAuthenticated: false } }
    )

    expect(screen.getByText('Login Page')).toBeInTheDocument()
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })

  it('renders children when authenticated', () => {
    renderWithAuth(
      <Routes>
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <div>Protected Content</div>
            </ProtectedRoute>
          }
        />
        <Route path="/login" element={<div>Login Page</div>} />
      </Routes>,
      { authValue: { isAuthenticated: true } }
    )

    expect(screen.getByText('Protected Content')).toBeInTheDocument()
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument()
  })

  it('redirects to / when authenticated but missing the route permission', () => {
    renderWithAuth(
      <Routes>
        <Route path="/" element={<div>Dashboard</div>} />
        <Route
          path="/users"
          element={
            <ProtectedRoute>
              <div>Users Page</div>
            </ProtectedRoute>
          }
        />
      </Routes>,
      {
        authValue: { isAuthenticated: true, user: { permissions: [] } },
        initialEntries: ['/users'],
      }
    )

    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.queryByText('Users Page')).not.toBeInTheDocument()
  })

  it('renders children when authenticated and route permission is satisfied', () => {
    renderWithAuth(
      <Routes>
        <Route path="/" element={<div>Dashboard</div>} />
        <Route
          path="/users"
          element={
            <ProtectedRoute>
              <div>Users Page</div>
            </ProtectedRoute>
          }
        />
      </Routes>,
      {
        authValue: { isAuthenticated: true, user: { permissions: ['user.view'] } },
        initialEntries: ['/users'],
      }
    )

    expect(screen.getByText('Users Page')).toBeInTheDocument()
  })
})
