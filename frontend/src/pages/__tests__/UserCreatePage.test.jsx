import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import AuthContext from '../../contexts/AuthContext'
import { ToastProvider } from '../../components/Toast'
import UserCreatePage from '../users/UserCreatePage'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../../api/users', () => ({
  createUser: vi.fn(),
  listRoles: vi.fn(),
}))

import { createUser, listRoles } from '../../api/users'

const MOCK_ROLES = [
  { id: 1, code: 'admin', name: 'Administrator' },
  { id: 2, code: 'sales', name: 'Sales' },
]

const authValue = { token: 'test', user: { username: 'admin' }, isAuthenticated: true, login: vi.fn(), logout: vi.fn() }

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <UserCreatePage />
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('UserCreatePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listRoles.mockResolvedValue(MOCK_ROLES)
    createUser.mockResolvedValue({ id: 3, username: 'newuser' })
  })

  it('renders create form with all fields', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByLabelText(/帳號/)).toBeInTheDocument()
      expect(screen.getByLabelText(/Email/)).toBeInTheDocument()
      expect(screen.getByLabelText(/密碼/)).toBeInTheDocument()
      expect(screen.getByLabelText(/姓名/)).toBeInTheDocument()
      expect(screen.getByLabelText(/角色/)).toBeInTheDocument()
      expect(screen.getByLabelText(/部門/)).toBeInTheDocument()
      expect(screen.getByLabelText(/電話/)).toBeInTheDocument()
    })
  })

  it('shows validation errors on empty submit', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/帳號/)).toBeInTheDocument())

    await user.click(screen.getByText('建立'))

    expect(screen.getByText('帳號為必填')).toBeInTheDocument()
    expect(screen.getByText('Email 為必填')).toBeInTheDocument()
    expect(screen.getByText('密碼為必填')).toBeInTheDocument()
    expect(screen.getByText('姓名為必填')).toBeInTheDocument()
    expect(screen.getByText('角色為必填')).toBeInTheDocument()
  })

  it('submits valid form and navigates', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/帳號/)).toBeInTheDocument())

    await user.type(screen.getByLabelText(/帳號/), 'newuser')
    await user.type(screen.getByLabelText(/Email/), 'new@test.com')
    await user.type(screen.getByLabelText(/密碼/), 'password123')
    await user.type(screen.getByLabelText(/姓名/), 'New User')
    await user.selectOptions(screen.getByLabelText(/角色/), '1')

    await user.click(screen.getByText('建立'))

    await waitFor(() => {
      expect(createUser).toHaveBeenCalledWith(expect.objectContaining({
        username: 'newuser',
        email: 'new@test.com',
        password: 'password123',
        fullName: 'New User',
        roleId: 1,
      }))
      expect(mockNavigate).toHaveBeenCalledWith('/users')
    })
  })

  it('shows channels when sales role selected', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/角色/)).toBeInTheDocument())

    await user.selectOptions(screen.getByLabelText(/角色/), '2')

    expect(screen.getByText('PX/大全聯')).toBeInTheDocument()
    expect(screen.getByText('家樂福')).toBeInTheDocument()
  })

  it('hides channels for non-sales role', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/角色/)).toBeInTheDocument())

    await user.selectOptions(screen.getByLabelText(/角色/), '2')
    expect(screen.getByText('PX/大全聯')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText(/角色/), '1')
    expect(screen.queryByText('PX/大全聯')).not.toBeInTheDocument()
  })

  it('validates channels required for sales role', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/角色/)).toBeInTheDocument())

    await user.type(screen.getByLabelText(/帳號/), 'salesuser')
    await user.type(screen.getByLabelText(/Email/), 's@test.com')
    await user.type(screen.getByLabelText(/密碼/), 'password123')
    await user.type(screen.getByLabelText(/姓名/), 'Sales')
    await user.selectOptions(screen.getByLabelText(/角色/), '2')

    await user.click(screen.getByText('建立'))

    expect(screen.getByText('業務角色需選擇至少一個通路')).toBeInTheDocument()
  })

  it('shows API error on failure', async () => {
    createUser.mockRejectedValue({ response: { data: { message: 'Username already exists' } } })
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/帳號/)).toBeInTheDocument())

    await user.type(screen.getByLabelText(/帳號/), 'admin')
    await user.type(screen.getByLabelText(/Email/), 'a@t.com')
    await user.type(screen.getByLabelText(/密碼/), 'password123')
    await user.type(screen.getByLabelText(/姓名/), 'Test')
    await user.selectOptions(screen.getByLabelText(/角色/), '1')

    await user.click(screen.getByText('建立'))

    await waitFor(() => {
      expect(screen.getByText('Username already exists')).toBeInTheDocument()
    })
  })

  it('cancel navigates back', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByText('取消'))
    expect(mockNavigate).toHaveBeenCalledWith('/users')
  })
})
