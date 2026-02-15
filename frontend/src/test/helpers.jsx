import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import AuthContext from '../contexts/AuthContext'

export function renderWithRouter(ui, { initialEntries = ['/'], ...options } = {}) {
  return render(ui, {
    wrapper: ({ children }) => (
      <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
    ),
    ...options,
  })
}

export function renderWithAuth(ui, { authValue = {}, initialEntries = ['/'], ...options } = {}) {
  const defaultAuth = {
    token: null,
    user: null,
    isAuthenticated: false,
    login: vi.fn(),
    logout: vi.fn(),
    ...authValue,
  }
  return render(ui, {
    wrapper: ({ children }) => (
      <MemoryRouter initialEntries={initialEntries}>
        <AuthContext.Provider value={defaultAuth}>{children}</AuthContext.Provider>
      </MemoryRouter>
    ),
    ...options,
  })
}
