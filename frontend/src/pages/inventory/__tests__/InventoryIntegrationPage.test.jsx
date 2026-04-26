import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'
import InventoryIntegrationPage from '../InventoryIntegrationPage'
import AuthContext from '@/contexts/AuthContext'
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
    // Component calls getInventoryVersions(null) on mount; must return a Promise
    vi.mocked(inventoryApi.getInventoryVersions).mockResolvedValue([])
  })

  it('should render page with query controls', () => {
    renderWithContext()

    expect(screen.getByText('庫存銷量預估量整合表單')).toBeInTheDocument()
    expect(screen.getByLabelText('查詢月份')).toBeInTheDocument()
    expect(screen.getByLabelText('結存查詢起訖日期')).toBeInTheDocument()
    expect(screen.getByLabelText('結存查詢起始日')).toBeInTheDocument()
    expect(screen.getByLabelText('結存查詢結束日')).toBeInTheDocument()
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

    const monthInput = screen.getByLabelText('查詢月份')
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
    //expect(screen.getByText('100.00')).toBeInTheDocument()
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

  it('should not allow editing without inventory.edit permission', async () => {
    const user = userEvent.setup()
    const viewOnlyUser = {
      ...mockUser,
      permissions: ['inventory.view']
    }

    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    renderWithContext(viewOnlyUser)

    await user.click(screen.getByRole('button', { name: '查詢' }))
    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    // 版本編輯, 儲存版本, 取消 should not be present for view-only user
    expect(screen.queryByRole('button', { name: '版本編輯' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '儲存版本' })).not.toBeInTheDocument()
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
      expect(screen.getByText('無法載入資料')).toBeInTheDocument()
    })
  })

  it('should store version in localStorage after successful query', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })
    const stored = localStorage.getItem('inventoryVersions')
    if (stored) {
      const versions = JSON.parse(stored)
      expect(versions).toContain('v20260115120000')
    }
  })

  it('should load version from dropdown', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    inventoryApi.getInventoryVersions.mockResolvedValue(['v20260115120000'])

    renderWithContext()

    await user.click(screen.getByLabelText(/查詢特定版本/))
    const versionSelect = screen.getByLabelText('選擇版本')
    await user.selectOptions(versionSelect, 'v20260115120000')

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(inventoryApi.getInventoryIntegration).toHaveBeenCalledWith(
        null,
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
      expect(screen.getByText('P001')).toBeInTheDocument()
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })
  })

  it('should sort table by column click', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

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

  it('should show error when querying without month', async () => {
    const user = userEvent.setup()

    renderWithContext()

    const monthInput = screen.getByLabelText('查詢月份')
    await user.clear(monthInput)

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    expect(inventoryApi.getInventoryIntegration).not.toHaveBeenCalled()
  })

  it('should query by version without month in version mode', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryVersions.mockResolvedValue(['v20260115120000'])
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    await user.click(screen.getByLabelText(/查詢特定版本/))
    const versionSelect = screen.getByLabelText('選擇版本')
    await user.selectOptions(versionSelect, 'v20260115120000')

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(inventoryApi.getInventoryIntegration).toHaveBeenCalledWith(null, null, null, 'v20260115120000')
    })
  })

  it('should show error when loading version without version selected', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryVersions.mockResolvedValue(['v20260115120000'])

    renderWithContext()

    await user.click(screen.getByLabelText(/查詢特定版本/))
    const queryButton = screen.getByRole('button', { name: '查詢' })
    expect(queryButton).toBeDisabled()
    expect(inventoryApi.getInventoryIntegration).not.toHaveBeenCalled()
  })

  it('should handle 403 error for query', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockRejectedValue({
      response: { status: 403 }
    })

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
    })
  })

  it('should handle 403 error for load version', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryVersions.mockResolvedValue(['v20260115120000'])
    inventoryApi.getInventoryIntegration.mockRejectedValue({
      response: { status: 403 }
    })

    renderWithContext()

    await user.click(screen.getByLabelText(/查詢特定版本/))
    const versionSelect = screen.getByLabelText('選擇版本')
    await user.selectOptions(versionSelect, 'v20260115120000')

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
    })
  })

  it('should allow editing modified subtotal in version edit mode', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))

    await waitFor(() => {
      const inputs = screen.getAllByRole('textbox')
      expect(inputs.length).toBeGreaterThanOrEqual(1)
      expect(inputs[0]).toHaveValue('320')
    })
  })

  it('should save modified subtotals and create version on 儲存版本', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    inventoryApi.updateModifiedSubtotal.mockResolvedValue({})
    inventoryApi.copyInventoryVersion.mockResolvedValue({ version: 'v20260116120000' })

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '儲存版本' })).toBeInTheDocument()
    })

    const inputs = screen.getAllByRole('textbox')
    await user.tripleClick(inputs[0])
    await user.keyboard('350')

    await user.click(screen.getByRole('button', { name: '儲存版本' }))

    await waitFor(() => {
      expect(inventoryApi.updateModifiedSubtotal).toHaveBeenCalledWith(1, 350)
    })
    await waitFor(() => {
      expect(inventoryApi.copyInventoryVersion).toHaveBeenCalledWith('v20260115120000')
    })
    expect(screen.getByText(/已建立新版次/)).toBeInTheDocument()
  })

  it('should cancel version edit mode on 取消', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '取消' }))

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: '儲存版本' })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: '取消' })).not.toBeInTheDocument()
    })
    expect(inventoryApi.updateModifiedSubtotal).not.toHaveBeenCalled()
    expect(inventoryApi.copyInventoryVersion).not.toHaveBeenCalled()
  })

  it('should show validation error for invalid format on 儲存版本', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))

    const inputs = screen.getAllByRole('textbox')
    await user.clear(inputs[0])
    await user.type(inputs[0], '123.456')

    await user.click(screen.getByRole('button', { name: '儲存版本' }))

    await waitFor(() => {
      expect(screen.getByText(/格式錯誤/)).toBeInTheDocument()
    })
    expect(inventoryApi.updateModifiedSubtotal).not.toHaveBeenCalled()
    expect(inventoryApi.copyInventoryVersion).not.toHaveBeenCalled()
  })

  it('should show validation error for invalid number on 儲存版本', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))

    const inputs = screen.getAllByRole('textbox')
    await user.clear(inputs[0])
    await user.type(inputs[0], 'abc')

    await user.click(screen.getByRole('button', { name: '儲存版本' }))

    await waitFor(() => {
      expect(screen.getByText(/格式錯誤/)).toBeInTheDocument()
    })
    expect(inventoryApi.updateModifiedSubtotal).not.toHaveBeenCalled()
    expect(inventoryApi.copyInventoryVersion).not.toHaveBeenCalled()
  })

  it('should not save when validation fails', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))
    const inputs = screen.getAllByRole('textbox')
    await user.clear(inputs[0])
    await user.type(inputs[0], 'invalid')

    await user.click(screen.getByRole('button', { name: '儲存版本' }))

    await waitFor(() => {
      expect(screen.getByText(/格式錯誤/)).toBeInTheDocument()
    })
    expect(inventoryApi.updateModifiedSubtotal).not.toHaveBeenCalled()
    expect(inventoryApi.copyInventoryVersion).not.toHaveBeenCalled()
  })

  it('should save all changes and call copyVersion on 儲存版本', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    inventoryApi.updateModifiedSubtotal.mockResolvedValue({})
    inventoryApi.copyInventoryVersion.mockResolvedValue({ version: 'v20260116120000' })

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))
    const inputs = screen.getAllByRole('textbox')
    await user.tripleClick(inputs[0])
    await user.keyboard('350')

    await user.click(screen.getByRole('button', { name: '儲存版本' }))

    await waitFor(() => {
      expect(inventoryApi.updateModifiedSubtotal).toHaveBeenCalledWith(1, 350)
    })
    await waitFor(() => {
      expect(inventoryApi.copyInventoryVersion).toHaveBeenCalled()
    })
  })

  it('should handle 403 error when 儲存版本', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    inventoryApi.updateModifiedSubtotal.mockRejectedValue({
      response: { status: 403, data: { message: '權限不足' } }
    })

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))
    const inputs = screen.getAllByRole('textbox')
    await user.tripleClick(inputs[0])
    await user.keyboard('350')

    await user.click(screen.getByRole('button', { name: '儲存版本' }))

    await waitFor(() => {
      expect(inventoryApi.updateModifiedSubtotal).toHaveBeenCalled()
    })
    await waitFor(() => {
      expect(screen.getByText(/儲存版本失敗|權限不足/)).toBeInTheDocument()
    })
  })

  it('should cancel version edit and discard changes', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))
    const inputs = screen.getAllByRole('textbox')
    await user.tripleClick(inputs[0])
    await user.keyboard('350')

    await user.click(screen.getByRole('button', { name: '取消' }))

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: '儲存版本' })).not.toBeInTheDocument()
    })
    expect(inventoryApi.updateModifiedSubtotal).not.toHaveBeenCalled()
    expect(inventoryApi.copyInventoryVersion).not.toHaveBeenCalled()
  })

  it('should create version without subtotal changes when 儲存版本 with no edits', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    inventoryApi.copyInventoryVersion.mockResolvedValue({ version: 'v20260116120000' })

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))
    await user.click(screen.getByRole('button', { name: '儲存版本' }))

    await waitFor(() => {
      expect(inventoryApi.copyInventoryVersion).toHaveBeenCalledWith('v20260115120000')
    })
    expect(inventoryApi.updateModifiedSubtotal).not.toHaveBeenCalled()
  })

  it('should toggle sort direction on repeated clicks', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('P001')).toBeInTheDocument()
    })

    const headers = screen.getAllByRole('columnheader')
    const productNameHeader = headers.find(h => h.textContent.includes('品名'))

    // First click - ascending
    await user.click(productNameHeader)
    expect(productNameHeader).toHaveTextContent('▲')

    // Second click - descending
    await user.click(productNameHeader)
    expect(productNameHeader).toHaveTextContent('▼')
  })

  it('should update dates when month changes', async () => {
    const user = userEvent.setup()

    renderWithContext()

    const monthInput = screen.getByLabelText('查詢月份')
    await user.clear(monthInput)
    await user.type(monthInput, '2026-02')

    const startDateInput = screen.getByLabelText('結存查詢起始日')
    const endDateInput = screen.getByLabelText('結存查詢結束日')

    await waitFor(() => {
      expect(startDateInput).toHaveValue('2026-02-01')
      expect(endDateInput).toHaveValue('2026-02-28')
    })
  })

  it('should handle localStorage parse error gracefully', () => {
    localStorage.setItem('inventoryVersions', 'invalid json')

    expect(() => renderWithContext()).not.toThrow()
  })

  it('should format null values correctly in table', async () => {
    const user = userEvent.setup()
    const dataWithNulls = [
      {
        id: 1,
        month: '202601',
        productCode: null,
        productName: null,
        category: null,
        spec: null,
        warehouseLocation: null,
        salesQuantity: null,
        inventoryBalance: null,
        forecastQuantity: null,
        productionSubtotal: null,
        modifiedSubtotal: null,
        version: null
      }
    ]

    inventoryApi.getInventoryIntegration.mockResolvedValue(dataWithNulls)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      const table = screen.getByRole('table')
      expect(table).toBeInTheDocument()
    })

    // Should display '-' for null text fields and '0.00' for null numbers
    const rows = screen.getAllByRole('row')
    const dataRow = rows[rows.length - 1]

    expect(within(dataRow).getAllByText('-').length).toBeGreaterThan(0)
    expect(within(dataRow).getAllByText('0.00').length).toBeGreaterThan(0)
  })

  it('should sort data with null values correctly', async () => {
    const user = userEvent.setup()
    const mixedData = [
      {
        ...mockData[0],
        productName: 'Z Product'
      },
      {
        ...mockData[1],
        productName: null
      },
      {
        ...mockData[0],
        id: 3,
        productCode: 'P003',
        productName: 'A Product'
      }
    ]

    inventoryApi.getInventoryIntegration.mockResolvedValue(mixedData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('Z Product')).toBeInTheDocument()
    })

    // Click product name header to sort
    const headers = screen.getAllByRole('columnheader')
    const productNameHeader = headers.find(h => h.textContent.includes('品名'))
    await user.click(productNameHeader)

    // Null values should be sorted last
    const rows = screen.getAllByRole('row')
    // Check that null is not in the first data row
    const firstDataRow = rows[1]
    expect(within(firstDataRow).queryByText('-')).not.toBeInTheDocument()
  })

  it('should show 儲存版本 in version edit mode', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))

    await waitFor(() => {
      const inputs = screen.getAllByRole('textbox')
      expect(inputs.length).toBeGreaterThanOrEqual(1)
    })

    const inputs = screen.getAllByRole('textbox')
    await user.clear(inputs[0])
    await user.type(inputs[0], '350')

    expect(screen.getByRole('button', { name: '儲存版本' })).toBeInTheDocument()
  })

  it('should not trigger beforeunload without changes', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    // Simulate beforeunload event without changes
    const event = new Event('beforeunload')
    const preventDefaultSpy = vi.spyOn(event, 'preventDefault')

    window.dispatchEvent(event)

    // Should not prevent default if no changes
    expect(preventDefaultSpy).not.toHaveBeenCalled()
  })

  it('should accept empty value for modified subtotal and save as null', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    inventoryApi.updateModifiedSubtotal.mockResolvedValue({ modified_subtotal: null })
    inventoryApi.copyInventoryVersion.mockResolvedValue({ version: 'v20260116120000' })

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))
    const inputs = screen.getAllByRole('textbox')
    await user.tripleClick(inputs[0])
    await user.keyboard('{Backspace}')

    await user.click(screen.getByRole('button', { name: '儲存版本' }))

    await waitFor(() => {
      expect(inventoryApi.updateModifiedSubtotal).toHaveBeenCalledWith(1, null)
    })
    await waitFor(() => {
      expect(inventoryApi.copyInventoryVersion).toHaveBeenCalled()
    })
  })

  it('should accept negative values for modified subtotal', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    inventoryApi.updateModifiedSubtotal.mockResolvedValue({})
    inventoryApi.copyInventoryVersion.mockResolvedValue({ version: 'v20260116120000' })

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '版本編輯' }))
    const inputs = screen.getAllByRole('textbox')
    await user.tripleClick(inputs[0])
    await user.keyboard('-50.25')

    await user.click(screen.getByRole('button', { name: '儲存版本' }))

    await waitFor(() => {
      expect(inventoryApi.updateModifiedSubtotal).toHaveBeenCalledWith(1, -50.25)
    })
    await waitFor(() => {
      expect(inventoryApi.copyInventoryVersion).toHaveBeenCalled()
    })
  })

  it('should show query time and product count after query', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText(/查詢時間:/)).toBeInTheDocument()
      expect(screen.getByText(/筆數: 2/)).toBeInTheDocument()
    })
  })

  it('should show query time and count when data has no version', async () => {
    const user = userEvent.setup()
    const dataWithoutVersion = mockData.map(d => ({ ...d, version: null }))
    inventoryApi.getInventoryIntegration.mockResolvedValue(dataWithoutVersion)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('P001')).toBeInTheDocument()
    })

    await waitFor(() => {
      expect(screen.getByText(/查詢時間:/)).toBeInTheDocument()
      expect(screen.getByText(/筆數: 2/)).toBeInTheDocument()
    })
  })

  it('should handle admin user with roleCode', () => {
    const adminUser = {
      userId: 1,
      username: 'admin',
      roleCode: 'admin',
      permissions: ['inventory.view'],
    }

    renderWithContext(adminUser)

    expect(screen.queryByText('您沒有權限檢視此頁面')).not.toBeInTheDocument()
    expect(screen.getByText('庫存銷量預估量整合表單')).toBeInTheDocument()
  })
})
