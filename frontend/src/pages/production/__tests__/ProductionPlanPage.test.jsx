import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ProductionPlanPage from '../ProductionPlanPage'
import * as productionPlanApi from '../../../api/productionPlan'
import { renderWithAuth } from '../../../test/helpers'

vi.mock('../../../api/productionPlan', () => ({
  getProductionPlan: vi.fn(),
  updateProductionPlanItem: vi.fn(),
}))

describe('ProductionPlanPage', () => {
  const mockData = [
    {
      id: 1,
      productCode: 'PROD001',
      productName: 'Product 1',
      category: 'A',
      spec: 'S1',
      warehouseLocation: 'WH1',
      channel: 'Direct',
      monthlyAllocations: { '2': 100, '3': 150, '4': 200 },
      buffer: 50,
      forecast: 2300,
      remarks: 'Test',
    },
    {
      id: 2,
      productCode: 'PROD002',
      productName: 'Product 2',
      category: 'B',
      spec: 'S2',
      warehouseLocation: 'WH2',
      channel: 'Retail',
      monthlyAllocations: { '2': 50, '3': 75 },
      buffer: 25,
      forecast: 1200,
      remarks: '',
    },
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows access denied if user lacks production_plan.view permission', () => {
    const authValue = {
      user: { id: 1, username: 'user', roleCode: 'user', permissions: [] },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
  })

  /*it('renders page with year selector for user with view permission', () => {
    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'user',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    expect(screen.getByText(/生產計畫/)).toBeInTheDocument()
   // expect(screen.getByLabelText('年度')).toBeInTheDocument()
   // expect(screen.getByRole('button', { name: '載入' })).toBeInTheDocument()
  })*/

  it('loads data when Load button is clicked', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(productionPlanApi.getProductionPlan).toHaveBeenCalledWith(
        new Date().getFullYear()
      )
    })

    await waitFor(() => {
      expect(screen.getByText('PROD001')).toBeInTheDocument()
      expect(screen.getByText('Product 1')).toBeInTheDocument()
      expect(screen.getByText('PROD002')).toBeInTheDocument()
      expect(screen.getByText('Product 2')).toBeInTheDocument()
    })
  })

  it('shows empty state when no data', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('所選年度無生產計畫資料')).toBeInTheDocument()
    })
  })

  it('displays grid with all columns', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('產品代碼')).toBeInTheDocument()
      expect(screen.getByText('產品名稱')).toBeInTheDocument()
      expect(screen.getByText('類別')).toBeInTheDocument()
      expect(screen.getByText('規格')).toBeInTheDocument()
      expect(screen.getByText('倉儲位置')).toBeInTheDocument()
      expect(screen.getByText('通路')).toBeInTheDocument()
      expect(screen.getByText('Feb')).toBeInTheDocument()
      expect(screen.getByText('Mar')).toBeInTheDocument()
      expect(screen.getByText('Buffer')).toBeInTheDocument()
      expect(screen.getByText('Total')).toBeInTheDocument()
      expect(screen.getByText('Forecast')).toBeInTheDocument()
      expect(screen.getByText('Diff')).toBeInTheDocument()
      expect(screen.getByText('Remarks')).toBeInTheDocument()
    })
  })

  it('calculates total and difference correctly', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      // Total = 100 + 150 + 200 + 50 = 500
      expect(screen.getByText('500.00')).toBeInTheDocument()
      // Diff = 500 - 2300 = -1800
      expect(screen.getByText('-1800.00')).toBeInTheDocument()
    })
  })

  it('allows editing when user has edit permission', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    // Click on a cell to edit
    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    // Should show an input field
    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })
  })

  it('handles API error gracefully', async () => {
    productionPlanApi.getProductionPlan.mockRejectedValue({
      response: { status: 500 },
    })

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('所選年度無生產計畫資料')).toBeInTheDocument()
    })
  })

  it('shows access denied on 403 error', async () => {
    productionPlanApi.getProductionPlan.mockRejectedValue({
      response: { status: 403 },
    })

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
    })
  })

  it('displays count of product/channel combinations', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('總計: 2 產品/通路組合')).toBeInTheDocument()
    })
  })

  it('allows editing buffer field', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('50.00')).toBeInTheDocument()
    })

    // Click buffer cell
    const bufferCell = screen.getByText('50.00')
    await userEvent.click(bufferCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toHaveValue('50')
    })
  })

  it('allows editing remarks field', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('Test')).toBeInTheDocument()
    })

    // Click remarks cell
    const remarksCell = screen.getByText('Test')
    await userEvent.click(remarksCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toHaveValue('Test')
    })
  })

  it('allows editing month field', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    // Click month cell (Feb = 100)
    const monthCell = screen.getByText('100.00')
    await userEvent.click(monthCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toHaveValue('100')
    })
  })

  it('cancels editing on Escape key', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    // Press Escape
    const input = screen.getByRole('textbox')
    await userEvent.type(input, '{Escape}')

    await waitFor(() => {
      expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    })
  })

  it('saves edit on Enter key', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    // Change value and press Enter
    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '200{Enter}')

    await waitFor(() => {
      expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    })

    // Save button should appear
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /儲存/ })).toBeInTheDocument()
    })
  })

  it('saves edit on Tab key', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    // Change value and press Tab
    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '200{Tab}')

    await waitFor(() => {
      expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    })

    // Save button should appear
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /儲存/ })).toBeInTheDocument()
    })
  })

  it('shows error for invalid number input', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    // Enter invalid value
    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, 'abc{Enter}')

    // Should still show input (not saved)
    await waitFor(() => {
      expect(screen.getByRole('textbox')).toBeInTheDocument()
    })
  })

  it('shows error for negative number input', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    // Enter negative value
    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '-10{Enter}')

    // Should still show input (not saved)
    await waitFor(() => {
      expect(screen.getByRole('textbox')).toBeInTheDocument()
    })
  })

  it('shows error for too many decimal places', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    // Enter value with too many decimal places
    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '10.123{Enter}')

    // Should still show input (not saved)
    await waitFor(() => {
      expect(screen.getByRole('textbox')).toBeInTheDocument()
    })
  })

  it('shows error for number with too many digits', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    // Enter value with too many digits
    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '12345678901{Enter}')

    // Should still show input (not saved)
    await waitFor(() => {
      expect(screen.getByRole('textbox')).toBeInTheDocument()
    })
  })

  it('does not allow editing without edit permission', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'user',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    // Should not show input field
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
  })

  it('allows editing remarks without numeric validation', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('Test')).toBeInTheDocument()
    })

    const remarksCell = screen.getByText('Test')
    await userEvent.click(remarksCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    // Enter text (should accept any text for remarks)
    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, 'New remark{Enter}')

    await waitFor(() => {
      expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    })

    // Save button should appear
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /儲存/ })).toBeInTheDocument()
    })
  })

  it('saves all changes successfully', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])
    productionPlanApi.updateProductionPlanItem.mockResolvedValue({})

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    // Edit a cell
    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '200{Enter}')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /儲存/ })).toBeInTheDocument()
    })

    // Click save
    const saveButton = screen.getByRole('button', { name: /儲存/ })
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(productionPlanApi.updateProductionPlanItem).toHaveBeenCalled()
    })
  })

  it('handles save failure for all items', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])
    productionPlanApi.updateProductionPlanItem.mockRejectedValue(new Error('Save failed'))

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    // Edit a cell
    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '200{Enter}')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /儲存/ })).toBeInTheDocument()
    })

    // Click save
    const saveButton = screen.getByRole('button', { name: /儲存/ })
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(productionPlanApi.updateProductionPlanItem).toHaveBeenCalled()
    })
  })

  it('handles partial save failure', async () => {
    const multiData = [mockData[0], mockData[1]]
    productionPlanApi.getProductionPlan.mockResolvedValue(multiData)

    // First call succeeds, second fails
    let callCount = 0
    productionPlanApi.updateProductionPlanItem.mockImplementation(() => {
      callCount++
      if (callCount === 1) {
        return Promise.resolve({})
      } else {
        return Promise.reject(new Error('Save failed'))
      }
    })

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('PROD001')).toBeInTheDocument()
      expect(screen.getByText('PROD002')).toBeInTheDocument()
    })

    // Edit two cells
    const cells = screen.getAllByText('100.00')

    // Edit first cell
    await userEvent.click(cells[0])
    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })
    let input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '200{Enter}')

    // Edit second cell (if exists - need to find buffer cell for second row)
    await waitFor(() => {
      const bufferCells = screen.getAllByText('50.00')
      expect(bufferCells.length).toBeGreaterThan(1)
    })

    const bufferCells = screen.getAllByText('50.00')
    await userEvent.click(bufferCells[1])

    await waitFor(() => {
      input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })
    await userEvent.clear(input)
    await userEvent.type(input, '75{Enter}')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /儲存/ })).toBeInTheDocument()
    })

    // Click save
    const saveButton = screen.getByRole('button', { name: /儲存/ })
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(productionPlanApi.updateProductionPlanItem).toHaveBeenCalled()
    })
  })

  it('shows confirmation when canceling few changes', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])
    window.confirm = vi.fn(() => true)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    // Edit a cell
    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '200{Enter}')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument()
    })

    // Click cancel
    const cancelButton = screen.getByRole('button', { name: '取消' })
    await userEvent.click(cancelButton)

    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalledWith('確定要取消變更嗎？')
    })
  })

  it('shows confirmation when canceling many changes', async () => {
    // Create data with 6 rows to test size > 5 confirmation
    const manyData = Array.from({ length: 6 }, (_, i) => ({
      ...mockData[0],
      id: i + 1,
      productCode: `PROD00${i + 1}`,
    }))

    productionPlanApi.getProductionPlan.mockResolvedValue(manyData)
    window.confirm = vi.fn(() => true)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('PROD001')).toBeInTheDocument()
    })

    // Edit 6 cells (one per row)
    const monthCells = screen.getAllByText('100.00')
    for (let i = 0; i < 6; i++) {
      await userEvent.click(monthCells[i])
      await waitFor(() => {
        const input = screen.getByRole('textbox')
        expect(input).toBeInTheDocument()
      })
      const input = screen.getByRole('textbox')
      await userEvent.clear(input)
      await userEvent.type(input, `${200 + i}{Enter}`)

      await waitFor(() => {
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
      })
    }

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /儲存/ })).toBeInTheDocument()
    })

    // Click cancel
    const cancelButton = screen.getByRole('button', { name: '取消' })
    await userEvent.click(cancelButton)

    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining('6'))
    })
  })

  it('shows confirmation when loading with unsaved changes', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])
    window.confirm = vi.fn(() => true)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    // Edit a cell
    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '200{Enter}')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /儲存/ })).toBeInTheDocument()
    })

    // Click load again
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalledWith('您有未儲存的變更，確定要載入新資料嗎？')
    })
  })

  it('does not load when user cancels confirmation', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])
    window.confirm = vi.fn(() => false)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view', 'production_plan.edit'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    // Edit a cell
    const cell = screen.getByText('100.00')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '200{Enter}')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /儲存/ })).toBeInTheDocument()
    })

    const callCountBefore = productionPlanApi.getProductionPlan.mock.calls.length

    // Click load again
    await userEvent.click(loadButton)

    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalled()
    })

    // Should not call API again
    expect(productionPlanApi.getProductionPlan.mock.calls.length).toBe(callCountBefore)
  })

  it('shows loading state during data fetch', async () => {
    productionPlanApi.getProductionPlan.mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve([mockData[0]]), 100))
    )

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const loadButton = screen.getByRole('button', { name: '載入' })
    await userEvent.click(loadButton)

    // Should show loading state
    expect(screen.getByRole('status')).toHaveTextContent('載入中...')
    expect(loadButton).toHaveTextContent('載入中...')

    // Wait for loading to finish
    await waitFor(() => {
      expect(screen.queryByRole('status')).not.toBeInTheDocument()
    })
  })
})
