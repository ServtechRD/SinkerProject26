import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import AuthContext from '../../contexts/AuthContext'
import { ToastProvider } from '../../components/Toast'
import UserEditPage from '../users/UserEditPage'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../../api/users', () => ({
  getUserById: vi.fn(),
  updateUser: vi.fn(),
  listRoles: vi.fn(),
}))

import { getUserById, updateUser, listRoles } from '../../api/users'

const MOCK_USER = {
  id: 1, username: 'admin', email: 'admin@test.com', fullName: 'Admin User',
  role: { id: 1, code: 'admin', name: 'Administrator' },
  department: 'IT', phone: '123', isActive: true, channels: [],
}

const MOCK_ROLES = [
  { id: 1, code: 'admin', name: 'Administrator' },
  { id: 2, code: 'sales', name: 'Sales' },
]

const authValue = { token: 'test', user: { username: 'admin' }, isAuthenticated: true, login: vi.fn(), logout: vi.fn() }

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/users/1/edit']}>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <Routes>
            <Route path="users/:id/edit" element={<UserEditPage />} />
          </Routes>
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('UserEditPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getUserById.mockResolvedValue(MOCK_USER)
    listRoles.mockResolvedValue(MOCK_ROLES)
    updateUser.mockResolvedValue({ ...MOCK_USER, fullName: 'Updated' })
  })

  it('loads and displays user data', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByLabelText(/帳號/).value).toBe('admin')
      expect(screen.getByLabelText(/Email/).value).toBe('admin@test.com')
      expect(screen.getByLabelText(/姓名/).value).toBe('Admin User')
      expect(screen.getByLabelText(/部門/).value).toBe('IT')
    })
  })

  it('password field is optional', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/帳號/).value).toBe('admin'))

    expect(screen.getByLabelText(/密碼/).value).toBe('')
    await user.click(screen.getByText('更新'))

    await waitFor(() => {
      expect(updateUser).toHaveBeenCalled()
      const payload = updateUser.mock.calls[0][1]
      expect(payload.password).toBeUndefined()
    })
  })

  it('updates user and navigates', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/姓名/).value).toBe('Admin User'))

    const nameInput = screen.getByLabelText(/姓名/)
    await user.clear(nameInput)
    await user.type(nameInput, 'Updated Name')
    await user.click(screen.getByText('更新'))

    await waitFor(() => {
      expect(updateUser).toHaveBeenCalledWith('1', expect.objectContaining({
        fullName: 'Updated Name',
      }))
      expect(mockNavigate).toHaveBeenCalledWith('/users')
    })
  })

  it('shows validation errors', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/帳號/).value).toBe('admin'))

    await user.clear(screen.getByLabelText(/帳號/))
    await user.click(screen.getByText('更新'))

    expect(screen.getByText('帳號為必填')).toBeInTheDocument()
  })

  it('shows channels if user has sales role', async () => {
    getUserById.mockResolvedValue({
      ...MOCK_USER,
      role: { id: 2, code: 'sales', name: 'Sales' },
      channels: ['PX/大全聯'],
    })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('PX/大全聯')).toBeInTheDocument()
    })
  })
})
