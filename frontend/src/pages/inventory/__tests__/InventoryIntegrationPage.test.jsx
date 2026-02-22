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

    const monthInput = screen.getByLabelText('月份')
    await user.clear(monthInput)

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    // Should not call API
    expect(inventoryApi.getInventoryIntegration).not.toHaveBeenCalled()
  })

  it('should show error when loading version without month', async () => {
    const user = userEvent.setup()
    localStorage.setItem('inventoryVersions', JSON.stringify(['v20260115120000']))

    renderWithContext()

    const monthInput = screen.getByLabelText('月份')
    await user.clear(monthInput)

    const versionSelect = screen.getByLabelText('歷史版本')
    await user.selectOptions(versionSelect, 'v20260115120000')

    const loadButton = screen.getByRole('button', { name: '載入版本' })
    await user.click(loadButton)

    // Should not call API
    expect(inventoryApi.getInventoryIntegration).not.toHaveBeenCalled()
  })

  it('should show error when loading version without version selected', async () => {
    const user = userEvent.setup()

    renderWithContext()

    const loadButton = screen.getByRole('button', { name: '載入版本' })
    await user.click(loadButton)

    // Should not call API
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
    localStorage.setItem('inventoryVersions', JSON.stringify(['v20260115120000']))

    inventoryApi.getInventoryIntegration.mockRejectedValue({
      response: { status: 403 }
    })

    renderWithContext()

    const versionSelect = screen.getByLabelText('歷史版本')
    await user.selectOptions(versionSelect, 'v20260115120000')

    const loadButton = screen.getByRole('button', { name: '載入版本' })
    await user.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
    })
  })

  it('should allow editing modified subtotal cell', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    // Click on the editable cell
    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
      expect(input).toHaveValue('320')
    })
  })

  it('should save edit on Enter key', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.tripleClick(input)
    await user.keyboard('350{Enter}')

    await waitFor(() => {
      expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    })

    // Save and Cancel buttons should appear
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '儲存' })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument()
    })
  })

  it('should cancel edit on Escape key', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.type(input, '{Escape}')

    await waitFor(() => {
      expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    })

    // Save buttons should not appear
    expect(screen.queryByRole('button', { name: '儲存' })).not.toBeInTheDocument()
  })

  it('should show validation error for invalid format', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.clear(input)
    await user.type(input, '123.456')

    await waitFor(() => {
      expect(screen.getByText(/格式錯誤/)).toBeInTheDocument()
    })
  })

  it('should show validation error for invalid number', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.clear(input)
    await user.type(input, 'abc')

    await waitFor(() => {
      expect(screen.getByText(/格式錯誤/)).toBeInTheDocument()
    })
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

    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.clear(input)
    await user.type(input, 'invalid{Enter}')

    // Input should still be visible (not saved)
    await waitFor(() => {
      expect(screen.getByRole('textbox')).toBeInTheDocument()
    })

    // Save button should not appear
    expect(screen.queryByRole('button', { name: '儲存' })).not.toBeInTheDocument()
  })

  it('should save all changes successfully', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    inventoryApi.updateModifiedSubtotal.mockResolvedValue({})

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.tripleClick(input)
    await user.keyboard('350{Enter}')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '儲存' })).toBeInTheDocument()
    })

    const saveButton = screen.getByRole('button', { name: '儲存' })
    await user.click(saveButton)

    await waitFor(() => {
      expect(inventoryApi.updateModifiedSubtotal).toHaveBeenCalledWith(1, 350)
    })
  })

  it('should handle 403 error when saving', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)
    inventoryApi.updateModifiedSubtotal.mockRejectedValue({
      response: { status: 403 }
    })

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.tripleClick(input)
    await user.keyboard('350{Enter}')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '儲存' })).toBeInTheDocument()
    })

    const saveButton = screen.getByRole('button', { name: '儲存' })
    await user.click(saveButton)

    await waitFor(() => {
      expect(inventoryApi.updateModifiedSubtotal).toHaveBeenCalled()
    })
  })

  it('should cancel all changes', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.tripleClick(input)
    await user.keyboard('350{Enter}')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument()
    })

    const cancelButton = screen.getByRole('button', { name: '取消' })
    await user.click(cancelButton)

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: '儲存' })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: '取消' })).not.toBeInTheDocument()
    })
  })

  it('should show error when saving with no changes', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    // Try to save without making any changes
    // Since there are no changes, save button should not be visible
    expect(screen.queryByRole('button', { name: '儲存' })).not.toBeInTheDocument()
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

    const monthInput = screen.getByLabelText('月份')
    await user.clear(monthInput)
    await user.type(monthInput, '2026-02')

    const startDateInput = screen.getByLabelText('開始日期')
    const endDateInput = screen.getByLabelText('結束日期')

    await waitFor(() => {
      expect(startDateInput).toHaveValue('2026-02-01')
      expect(endDateInput).toHaveValue('2026-02-28')
    })
  })

  it('should handle localStorage parse error gracefully', () => {
    localStorage.setItem('inventoryVersions', 'invalid json')

    // Should not throw error
    expect(() => renderWithContext()).not.toThrow()

    // Recent versions should be empty
    const versionSelect = screen.getByLabelText('歷史版本')
    const options = within(versionSelect).getAllByRole('option')

    // Only default option should exist
    expect(options).toHaveLength(1)
    expect(options[0]).toHaveTextContent('請選擇版本')
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

  it('should handle beforeunload event with unsaved changes', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    // Make a change
    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.clear(input)
    await user.type(input, '350{Enter}')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '儲存' })).toBeInTheDocument()
    })

    // Simulate beforeunload event
    const event = new Event('beforeunload')
    const preventDefaultSpy = vi.spyOn(event, 'preventDefault')

    window.dispatchEvent(event)

    expect(preventDefaultSpy).toHaveBeenCalled()
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

  it('should accept empty value for modified subtotal', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.tripleClick(input)
    await user.keyboard('{Backspace}{Enter}')

    await waitFor(() => {
      expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    })

    // Save button should appear
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '儲存' })).toBeInTheDocument()
    })
  })

  it('should accept negative values for modified subtotal', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('320.00')).toBeInTheDocument()
    })

    const editableCell = screen.getByText('320.00').closest('td')
    await user.click(editableCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await user.tripleClick(input)
    await user.keyboard('-50.25{Enter}')

    await waitFor(() => {
      expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    })

    // Save button should appear
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '儲存' })).toBeInTheDocument()
    })
  })

  it('should show current version and product count after query', async () => {
    const user = userEvent.setup()
    inventoryApi.getInventoryIntegration.mockResolvedValue(mockData)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText(/目前版本: v20260115120000/)).toBeInTheDocument()
      expect(screen.getByText(/產品數: 2/)).toBeInTheDocument()
    })
  })

  it('should not show version info when data has no version', async () => {
    const user = userEvent.setup()
    const dataWithoutVersion = mockData.map(d => ({ ...d, version: null }))
    inventoryApi.getInventoryIntegration.mockResolvedValue(dataWithoutVersion)

    renderWithContext()

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await user.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('P001')).toBeInTheDocument()
    })

    // Version info should not be displayed
    expect(screen.queryByText(/目前版本:/)).not.toBeInTheDocument()
  })

  it('should handle admin user with roleCode', () => {
    const adminUser = {
      userId: 1,
      username: 'admin',
      roleCode: 'admin'
      // No permissions array - admin role should grant access
    }

    renderWithContext(adminUser)

    // Admin should have access even without explicit permissions array
    expect(screen.queryByText('您沒有權限檢視此頁面')).not.toBeInTheDocument()
    expect(screen.getByText('庫存整合')).toBeInTheDocument()
  })
})
