import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import AuthContext from '../../contexts/AuthContext'
import { ToastProvider } from '../../components/Toast'
import RoleListPage from '../roles/RoleListPage'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../../api/roles', () => ({
  listRoles: vi.fn(),
}))

import { listRoles } from '../../api/roles'

const MOCK_ROLES = [
  { id: 1, code: 'admin', name: 'Administrator', description: 'Full system access', isSystem: true, isActive: true },
  { id: 2, code: 'sales', name: 'Sales', description: 'Sales forecast management', isSystem: true, isActive: true },
  { id: 3, code: 'production_planner', name: 'Production Planner', description: null, isSystem: true, isActive: true },
]

const authValue = { token: 'test', user: { username: 'admin' }, isAuthenticated: true, login: vi.fn(), logout: vi.fn() }

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <RoleListPage />
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('RoleListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listRoles.mockResolvedValue(MOCK_ROLES)
  })

  it('renders role list', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument()
      expect(screen.getByText('Sales')).toBeInTheDocument()
      expect(screen.getByText('Production Planner')).toBeInTheDocument()
    })
  })

  it('shows loading state', () => {
    listRoles.mockReturnValue(new Promise(() => {}))
    renderPage()
    expect(screen.getByText('載入中...')).toBeInTheDocument()
  })

  it('shows error state', async () => {
    listRoles.mockRejectedValue(new Error('fail'))
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('無法載入角色列表')).toBeInTheDocument()
    })
  })

  it('shows empty state', async () => {
    listRoles.mockResolvedValue([])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('查無角色')).toBeInTheDocument()
    })
  })

  it('displays table columns', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('代碼')).toBeInTheDocument()
      expect(screen.getByText('名稱')).toBeInTheDocument()
      expect(screen.getByText('描述')).toBeInTheDocument()
      expect(screen.getByText('系統角色')).toBeInTheDocument()
      expect(screen.getByText('操作')).toBeInTheDocument()
    })
  })

  it('shows system role badge', async () => {
    renderPage()
    await waitFor(() => {
      const badges = screen.getAllByText('是')
      expect(badges.length).toBe(3)
    })
  })

  it('edit button navigates to edit page', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('Administrator')).toBeInTheDocument())

    const editButtons = screen.getAllByText('編輯')
    await user.click(editButtons[0])
    expect(mockNavigate).toHaveBeenCalledWith('/roles/1/edit')
  })

  it('displays role code in code element', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getByText('sales')).toBeInTheDocument()
    })
  })
})
