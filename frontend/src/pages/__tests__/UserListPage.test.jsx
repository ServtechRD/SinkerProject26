import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import AuthContext from '../../contexts/AuthContext'
import { ToastProvider } from '../../components/Toast'
import UserListPage from '../users/UserListPage'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../../api/users', () => ({
  listUsers: vi.fn(),
  deleteUser: vi.fn(),
  toggleUserActive: vi.fn(),
  listRoles: vi.fn(),
}))

import { listUsers, deleteUser, toggleUserActive, listRoles } from '../../api/users'

const MOCK_USERS = {
  users: [
    { id: 1, username: 'admin', fullName: 'Admin', email: 'admin@test.com', role: { id: 1, code: 'admin', name: 'Administrator' }, isActive: true },
    { id: 2, username: 'sales1', fullName: 'Sales User', email: 'sales@test.com', role: { id: 2, code: 'sales', name: 'Sales' }, isActive: false },
  ],
  totalElements: 2,
  totalPages: 1,
  currentPage: 0,
}

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
          <UserListPage />
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('UserListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listUsers.mockResolvedValue(MOCK_USERS)
    listRoles.mockResolvedValue(MOCK_ROLES)
    deleteUser.mockResolvedValue({})
    toggleUserActive.mockResolvedValue({ ...MOCK_USERS.users[0], isActive: false })
  })

  it('renders user list', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getByText('Sales User')).toBeInTheDocument()
    })
  })

  it('shows loading state', () => {
    listUsers.mockReturnValue(new Promise(() => {}))
    renderPage()
    expect(screen.getByText('載入中...')).toBeInTheDocument()
  })

  it('shows empty state', async () => {
    listUsers.mockResolvedValue({ users: [], totalElements: 0, totalPages: 0, currentPage: 0 })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('查無使用者')).toBeInTheDocument()
    })
  })

  it('shows error state', async () => {
    listUsers.mockRejectedValue(new Error('fail'))
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('無法載入使用者列表')).toBeInTheDocument()
    })
  })

  it('search calls API with keyword', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())

    const input = screen.getByPlaceholderText(/搜尋/)
    await user.type(input, 'test')

    await waitFor(() => {
      const calls = listUsers.mock.calls
      const lastCall = calls[calls.length - 1][0]
      expect(lastCall.keyword).toBe('test')
    })
  })

  it('role filter calls API with roleId', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())

    const select = screen.getByLabelText('角色篩選')
    await user.selectOptions(select, '1')

    await waitFor(() => {
      const calls = listUsers.mock.calls
      const lastCall = calls[calls.length - 1][0]
      expect(lastCall.roleId).toBe('1')
    })
  })

  it('status filter calls API with isActive', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())

    const select = screen.getByLabelText('狀態篩選')
    await user.selectOptions(select, 'true')

    await waitFor(() => {
      const calls = listUsers.mock.calls
      const lastCall = calls[calls.length - 1][0]
      expect(lastCall.isActive).toBe('true')
    })
  })

  it('clicking sort header calls API with sort params', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())

    await user.click(screen.getByText(/^帳號/))
    await waitFor(() => {
      const calls = listUsers.mock.calls
      const lastCall = calls[calls.length - 1][0]
      expect(lastCall.sortBy).toBe('username')
    })
  })

  it('delete button shows confirmation dialog', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())

    const deleteButtons = screen.getAllByText('刪除')
    await user.click(deleteButtons[0])

    expect(screen.getByText('確認刪除')).toBeInTheDocument()
    expect(screen.getByText(/確定要刪除使用者「admin」/)).toBeInTheDocument()
  })

  it('toggle button calls API', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())

    const adminRow = screen.getByText('admin').closest('tr')
    const toggleBtn = within(adminRow).getByText('停用')
    await user.click(toggleBtn)

    await waitFor(() => {
      expect(toggleUserActive).toHaveBeenCalledWith(1)
    })
  })

  it('navigate to create page on button click', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())

    await user.click(screen.getByText('建立使用者'))
    expect(mockNavigate).toHaveBeenCalledWith('/users/create')
  })

  it('navigate to edit page on edit button click', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())

    const editButtons = screen.getAllByText('編輯')
    await user.click(editButtons[0])
    expect(mockNavigate).toHaveBeenCalledWith('/users/1/edit')
  })
})
