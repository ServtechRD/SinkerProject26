import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'
import InventoryIntegrationPage from '../InventoryIntegrationPage'
import { AuthContext } from '../../../contexts/AuthContext'
import { ToastProvider } from '../../../components/Toast'
import * as inventoryApi from '../../../api/inventory'

vi.mock('../../../api/inventory')

const mockUser = {
  userId: 1,
  username: 'testuser',
  roleCode: 'admin',
  permissions: ['inventory.view', 'inventory.edit']
}

const mockData = [
  {
    id: 1,
    month: '202601',
    productCode: 'P001',
    productName: 'Product 1',
    category: 'Category A',
    spec: 'Spec 1',
    warehouseLocation: 'WH01',
    salesQuantity: 100.00,
    inventoryBalance: 250.00,
    forecastQuantity: 150.00,
    productionSubtotal: 300.00,
    modifiedSubtotal: 320.00,
    version: 'v20260115120000'
  },
  {
    id: 2,
    month: '202601',
    productCode: 'P002',
    productName: 'Product 2',
    category: 'Category B',
    spec: 'Spec 2',
    warehouseLocation: 'WH02',
    salesQuantity: 50.00,
    inventoryBalance: 100.00,
    forecastQuantity: 75.00,
    productionSubtotal: 125.00,
    modifiedSubtotal: null,
    version: 'v20260115120000'
  }
]

function renderWithContext(user = mockUser) {
  const authValue = {
    user,
    token: 'test-token',
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn()
  }

  return render(
    <BrowserRouter>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <InventoryIntegrationPage />
        </ToastProvider>
      </AuthContext.Provider>
    </BrowserRouter>
  )
}

describe('InventoryIntegrationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('should render page with query controls', () => {
    renderWithContext()

    expect(screen.getByText('庫存整合')).toBeInTheDocument()
    expect(screen.getByLabelText('月份')).toBeInTheDocument()
    expect(screen.getByLabelText('開始日期')).toBeInTheDocument()
    expect(screen.getByLabelText('結束日期')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '查詢' })).toBeInTheDocument()
  })

  it('should show access denied for user without inventory.view permission', () => {
    const userWithoutPermission = {
      ...mockUser,
      permissions: []
    }

    renderWithContext(userWithoutPermission)

    expect(screen.getByRole('alert')).toHaveTextContent('您沒有權限檢視此頁面')
  })

  it('should query data on button click', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const monthInput = screen.getByLabelText('月份')
    await user.clear(monthInput)
    await user.type(monthInput, '2026-01')

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(inventoryApi.getInventoryIntegration).toHaveBeenCalledWith(
        '2026-01',
        expect.any(String),
        expect.any(String),
        null
      )
    })

    await waitFor(() => {
      expect(screen.getByText('P001')).toBeInTheDocument()
      expect(screen.getByText('Product 1')).toBeInTheDocument()
      expect(screen.getByText('P002')).toBeInTheDocument()
    })
  })

  it('should display data table after successful query', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      const table = screen.getByRole('table')
      expect(table).toBeInTheDocument()
    })

    expect(screen.getByText('P001')).toBeInTheDocument()
    expect(screen.getByText('Product 1')).toBeInTheDocument()
    expect(screen.getByText('100.00')).toBeInTheDocument()
  })

  it('should show loading state during query', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve(mockData), 100))
    )

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    expect(screen.getByRole('status')).toHaveTextContent('載入中...')

    await waitFor(() => {
      expect(screen.queryByRole('status')).not.toBeInTheDocument()
    })
  })

  it('should allow editing modified subtotal with inventory.edit permission', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    // Find and click the modified subtotal cell
    const rows = screen.getAllByRole('row')
    const dataRow = rows.find(row => within(row).queryByText('P001'))
    expect(dataRow).toBeDefined()
  })

  it('should not allow editing without inventory.edit permission', () => {
    const viewOnlyUser = {
      ...mockUser,
      permissions: ['inventory.view']
    }

    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    renderWithContext(viewOnlyUser)

    // Save and Cancel buttons should not be present
    expect(screen.queryByRole('button', { name: '儲存' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '取消' })).not.toBeInTheDocument()
  })

  it('should handle API errors gracefully', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockRejectedValue(
      new Error('Network error')
    )

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('請查詢以顯示資料')).toBeInTheDocument()
    })
  })

  it('should store version in localStorage after successful query', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      const stored = localStorage.getItem('inventoryVersions')
      expect(stored).toBeTruthy()
      const versions = JSON.parse(stored)
      expect(versions).toContain('v20260115120000')
    })
  })

  it('should load version from dropdown', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    // Set up localStorage with a version
    localStorage.setItem('inventoryVersions', JSON.stringify(['v20260115120000']))

    renderWithContext()

    const versionSelect = screen.getByLabelText('歷史版本')
    await user.selectOptions(versionSelect, 'v20260115120000')

    const loadButton = screen.getByRole('button', { name: '載入版本' })
    await user.click(loadButton)

    await waitFor(() => {
      expect(inventoryApi.getInventoryIntegration).toHaveBeenCalledWith(
        expect.any(String),
        null,
        null,
        'v20260115120000'
      )
    })
  })

  it('should highlight rows where modified subtotal differs from production subtotal', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      const rows = screen.getAllByRole('row')
      const modifiedRow = rows.find(row => within(row).queryByText('P001'))
      expect(modifiedRow).toHaveClass('row-modified')
    })
  })

  it('should sort table by column click', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' }await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('P001')).toBeInTheDocument()
    })

    // Click on product name header to sort
    const headers = screen.getAllByRole('columnheader')
    const productNameHeader = headers.find(h => h.textContent.includes('品名'))
    await user.click(productNameHeader)

    // Verify sort indicator appears
    expect(productNameHeader).toHaveTextContent('▲')
  })
})
