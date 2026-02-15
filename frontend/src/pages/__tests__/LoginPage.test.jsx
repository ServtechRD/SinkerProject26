import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import LoginPage from '../LoginPage'
import { renderWithAuth } from '../../test/helpers'

describe('LoginPage', () => {
  let loginMock

  beforeEach(() => {
    loginMock = vi.fn()
  })

  it('renders login form with username, password, and submit button', () => {
    renderWithAuth(<LoginPage />, {
      initialEntries: ['/login'],
      authValue: { login: loginMock },
    })

    expect(screen.getByLabelText('Username')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /login/i })).toBeInTheDocument()
  })

  it('password field has type password', () => {
    renderWithAuth(<LoginPage />, {
      initialEntries: ['/login'],
      authValue: { login: loginMock },
    })

    expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'password')
  })

  it('shows validation error when submitting empty form', async () => {
    const user = userEvent.setup()
    renderWithAuth(<LoginPage />, {
      initialEntries: ['/login'],
      authValue: { login: loginMock },
    })

    await user.click(screen.getByRole('button', { name: /login/i }))

    expect(screen.getByRole('alert')).toHaveTextContent('Username and password are required')
    expect(loginMock).not.toHaveBeenCalled()
  })

  it('calls login with credentials on valid submit', async () => {
    loginMock.mockResolvedValue({ token: 'tok', user: { username: 'admin' } })
    const user = userEvent.setup()
    renderWithAuth(<LoginPage />, {
      initialEntries: ['/login'],
      authValue: { login: loginMock },
    })

    await user.type(screen.getByLabelText('Username'), 'admin')
    await user.type(screen.getByLabelText('Password'), 'admin123')
    await user.click(screen.getByRole('button', { name: /login/i }))

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith('admin', 'admin123')
    })
  })

  it('displays error on 401 response', async () => {
    loginMock.mockRejectedValue({
      response: { status: 401, data: { message: 'Invalid username or password' } },
    })
    const user = userEvent.setup()
    renderWithAuth(<LoginPage />, {
      initialEntries: ['/login'],
      authValue: { login: loginMock },
    })

    await user.type(screen.getByLabelText('Username'), 'admin')
    await user.type(screen.getByLabelText('Password'), 'wrong')
    await user.click(screen.getByRole('button', { name: /login/i }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Invalid username or password')
    })
  })

  it('displays error on 403 locked account', async () => {
    loginMock.mockRejectedValue({
      response: { status: 403, data: { message: 'Account is locked' } },
    })
    const user = userEvent.setup()
    renderWithAuth(<LoginPage />, {
      initialEntries: ['/login'],
      authValue: { login: loginMock },
    })

    await user.type(screen.getByLabelText('Username'), 'locked')
    await user.type(screen.getByLabelText('Password'), 'password')
    await user.click(screen.getByRole('button', { name: /login/i }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Account is locked')
    })
  })

  it('shows loading state during login', async () => {
    let resolveLogin
    loginMock.mockImplementation(
      () => new Promise((resolve) => { resolveLogin = resolve })
    )
    const user = userEvent.setup()
    renderWithAuth(<LoginPage />, {
      initialEntries: ['/login'],
      authValue: { login: loginMock },
    })

    await user.type(screen.getByLabelText('Username'), 'admin')
    await user.type(screen.getByLabelText('Password'), 'admin123')
    await user.click(screen.getByRole('button', { name: /login/i }))

    expect(screen.getByRole('button', { name: /logging in/i })).toBeDisabled()

    resolveLogin({ token: 'tok', user: {} })
  })

  it('displays network error message', async () => {
    loginMock.mockRejectedValue(new Error('Network Error'))
    const user = userEvent.setup()
    renderWithAuth(<LoginPage />, {
      initialEntries: ['/login'],
      authValue: { login: loginMock },
    })

    await user.type(screen.getByLabelText('Username'), 'admin')
    await user.type(screen.getByLabelText('Password'), 'admin123')
    await user.click(screen.getByRole('button', { name: /login/i }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Network error')
    })
  })
})
