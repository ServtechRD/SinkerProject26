import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthProvider, useAuth } from '../AuthContext'
import { MemoryRouter } from 'react-router-dom'

vi.mock('../../api/auth', () => ({
  loginApi: vi.fn(),
}))

import { loginApi } from '../../api/auth'

function TestConsumer() {
  const { token, user, isAuthenticated, login, logout } = useAuth()
  return (
    <div>
      <span data-testid="auth">{String(isAuthenticated)}</span>
      <span data-testid="token">{token || 'null'}</span>
      <span data-testid="user">{user ? user.username : 'null'}</span>
      <button onClick={() => login('admin', 'admin123')}>Login</button>
      <button onClick={logout}>Logout</button>
    </div>
  )
}

function renderAuthConsumer() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('provides initial unauthenticated state', () => {
    renderAuthConsumer()
    expect(screen.getByTestId('auth')).toHaveTextContent('false')
    expect(screen.getByTestId('token')).toHaveTextContent('null')
    expect(screen.getByTestId('user')).toHaveTextContent('null')
  })

  it('login updates state and localStorage', async () => {
    loginApi.mockResolvedValue({
      token: 'jwt-token',
      user: { id: 1, username: 'admin', email: 'admin@test.com', fullName: 'Admin', roleCode: 'admin' },
    })
    const user = userEvent.setup()
    renderAuthConsumer()

    await user.click(screen.getByRole('button', { name: 'Login' }))

    expect(screen.getByTestId('auth')).toHaveTextContent('true')
    expect(screen.getByTestId('token')).toHaveTextContent('jwt-token')
    expect(screen.getByTestId('user')).toHaveTextContent('admin')
    expect(localStorage.getItem('authToken')).toBe('jwt-token')
    expect(JSON.parse(localStorage.getItem('user')).username).toBe('admin')
  })

  it('logout clears state and localStorage', async () => {
    loginApi.mockResolvedValue({
      token: 'jwt-token',
      user: { id: 1, username: 'admin' },
    })
    const user = userEvent.setup()
    renderAuthConsumer()

    await user.click(screen.getByRole('button', { name: 'Login' }))
    expect(screen.getByTestId('auth')).toHaveTextContent('true')

    await user.click(screen.getByRole('button', { name: 'Logout' }))
    expect(screen.getByTestId('auth')).toHaveTextContent('false')
    expect(screen.getByTestId('token')).toHaveTextContent('null')
    expect(localStorage.getItem('authToken')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('loads auth from localStorage on mount', () => {
    localStorage.setItem('authToken', 'stored-token')
    localStorage.setItem('user', JSON.stringify({ username: 'stored-user' }))

    renderAuthConsumer()

    expect(screen.getByTestId('auth')).toHaveTextContent('true')
    expect(screen.getByTestId('token')).toHaveTextContent('stored-token')
    expect(screen.getByTestId('user')).toHaveTextContent('stored-user')
  })

  it('handles invalid JSON in localStorage user', () => {
    localStorage.setItem('authToken', 'some-token')
    localStorage.setItem('user', 'not-valid-json')

    renderAuthConsumer()

    // Token exists so isAuthenticated is true, but user parse failed
    expect(screen.getByTestId('auth')).toHaveTextContent('true')
    expect(screen.getByTestId('user')).toHaveTextContent('null')
  })
})
