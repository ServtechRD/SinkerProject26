import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import AuthContext from '../../contexts/AuthContext'
import { ToastProvider } from '../../components/Toast'
import RoleEditPage from '../roles/RoleEditPage'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../../api/roles', () => ({
  getRoleById: vi.fn(),
  updateRole: vi.fn(),
}))

import { getRoleById, updateRole } from '../../api/roles'

const MOCK_ROLE = {
  id: 1,
  code: 'admin',
  name: 'Administrator',
  description: 'Full system access',
  isSystem: true,
  isActive: true,
  permissions: [
    { id: 1, code: 'user.view', name: 'View Users', module: 'user' },
    { id: 2, code: 'user.create', name: 'Create Users', module: 'user' },
    { id: 3, code: 'user.edit', name: 'Edit Users', module: 'user' },
    { id: 4, code: 'user.delete', name: 'Delete Users', module: 'user' },
    { id: 5, code: 'role.view', name: 'View Roles', module: 'role' },
    { id: 6, code: 'role.edit', name: 'Edit Roles', module: 'role' },
  ],
  permissionsByModule: {
    user: [
      { id: 1, code: 'user.view', name: 'View Users', module: 'user' },
      { id: 2, code: 'user.create', name: 'Create Users', module: 'user' },
      { id: 3, code: 'user.edit', name: 'Edit Users', module: 'user' },
      { id: 4, code: 'user.delete', name: 'Delete Users', module: 'user' },
    ],
    role: [
      { id: 5, code: 'role.view', name: 'View Roles', module: 'role' },
      { id: 6, code: 'role.edit', name: 'Edit Roles', module: 'role' },
    ],
  },
}

const authValue = { token: 'test', user: { username: 'admin' }, isAuthenticated: true, login: vi.fn(), logout: vi.fn() }

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/roles/1/edit']}>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <Routes>
            <Route path="roles/:id/edit" element={<RoleEditPage />} />
          </Routes>
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('RoleEditPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getRoleById.mockResolvedValue(MOCK_ROLE)
    updateRole.mockResolvedValue(MOCK_ROLE)
  })

  it('loads and displays role data', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText(/編輯角色：Administrator/)).toBeInTheDocument()
      expect(screen.getByDisplayValue('Administrator')).toBeInTheDocument()
      expect(screen.getByDisplayValue('admin')).toBeInTheDocument()
    })
  })

  it('code field is read-only', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByDisplayValue('admin')).toBeInTheDocument())
    expect(screen.getByDisplayValue('admin')).toBeDisabled()
  })

  it('name field is editable', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByDisplayValue('Administrator')).toBeInTheDocument())

    const nameInput = screen.getByLabelText(/名稱/)
    await user.clear(nameInput)
    await user.type(nameInput, 'New Name')
    expect(nameInput.value).toBe('New Name')
  })

  it('displays permissions grouped by module', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('使用者管理')).toBeInTheDocument()
      expect(screen.getByText('角色管理')).toBeInTheDocument()
    })
  })

  it('permissions are pre-selected on load', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByText('使用者管理')).toBeInTheDocument())

    const checkboxes = screen.getAllByRole('checkbox')
    // 2 module select-all + 6 permissions = 8 checkboxes
    const permissionCheckboxes = checkboxes.filter((cb) => !cb.getAttribute('aria-label')?.startsWith('全選'))
    permissionCheckboxes.forEach((cb) => {
      expect(cb).toBeChecked()
    })
  })

  it('clicking permission checkbox toggles', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('View Users')).toBeInTheDocument())

    const viewUsersLabel = screen.getByText('View Users').closest('label')
    const checkbox = viewUsersLabel.querySelector('input[type="checkbox"]')
    expect(checkbox).toBeChecked()

    await user.click(checkbox)
    expect(checkbox).not.toBeChecked()

    await user.click(checkbox)
    expect(checkbox).toBeChecked()
  })

  it('module select-all selects all in module', async () => {
    // Start with a role that has no permissions selected
    getRoleById.mockResolvedValue({
      ...MOCK_ROLE,
      permissions: [],
    })
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('使用者管理')).toBeInTheDocument())

    const selectAll = screen.getByLabelText('全選 使用者管理')
    await user.click(selectAll)

    // All 4 user permissions should now be checked
    const userModule = screen.getByText('使用者管理').closest('.permission-module')
    const checkboxes = userModule.querySelectorAll('.permission-item input[type="checkbox"]')
    checkboxes.forEach((cb) => {
      expect(cb.checked).toBe(true)
    })
  })

  it('module select-all deselects all when all selected', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('使用者管理')).toBeInTheDocument())

    const selectAll = screen.getByLabelText('全選 使用者管理')
    await user.click(selectAll) // all are selected, so this deselects all

    const userModule = screen.getByText('使用者管理').closest('.permission-module')
    const checkboxes = userModule.querySelectorAll('.permission-item input[type="checkbox"]')
    checkboxes.forEach((cb) => {
      expect(cb.checked).toBe(false)
    })
  })

  it('save button submits with permission IDs', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByDisplayValue('Administrator')).toBeInTheDocument())

    await user.click(screen.getByText('儲存'))

    await waitFor(() => {
      expect(updateRole).toHaveBeenCalledWith('1', expect.objectContaining({
        name: 'Administrator',
        permissionIds: expect.arrayContaining([1, 2, 3, 4, 5, 6]),
      }))
      expect(mockNavigate).toHaveBeenCalledWith('/roles')
    })
  })

  it('shows validation error when name is empty', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByDisplayValue('Administrator')).toBeInTheDocument())

    await user.clear(screen.getByLabelText(/名稱/))
    await user.click(screen.getByText('儲存'))

    expect(screen.getByText('名稱為必填')).toBeInTheDocument()
    expect(updateRole).not.toHaveBeenCalled()
  })

  it('shows error on API failure', async () => {
    updateRole.mockRejectedValue({ response: { data: { message: 'Validation failed' } } })
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByDisplayValue('Administrator')).toBeInTheDocument())

    await user.click(screen.getByText('儲存'))

    await waitFor(() => {
      expect(screen.getByText('Validation failed')).toBeInTheDocument()
    })
  })

  it('cancel navigates back', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByDisplayValue('Administrator')).toBeInTheDocument())

    await user.click(screen.getByText('取消'))
    expect(mockNavigate).toHaveBeenCalledWith('/roles')
  })

  it('shows loading state on page load', () => {
    getRoleById.mockReturnValue(new Promise(() => {}))
    renderPage()
    expect(screen.getByText('載入中...')).toBeInTheDocument()
  })

  it('shows error when role not found', async () => {
    getRoleById.mockRejectedValue(new Error('Not found'))
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('無法載入角色資料')).toBeInTheDocument()
    })
  })
})
