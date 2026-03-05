import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ProductionPlanPage from '../ProductionPlanPage'
import * as productionPlanApi from '../../../api/productionPlan'
import { renderWithAuth } from '../../../test/helpers'

vi.mock('../../../api/productionPlan', () => ({
  getProductionPlan: vi.fn(),
  updateProductionPlanBuffer: vi.fn(),
  updateProductionPlanItem: vi.fn(),
}))

describe('ProductionPlanPage', () => {
  const channelData = (ch, months, total) => ({ channel: ch, months: months || {}, total: total ?? 0 })
  const mockData = [
    {
      warehouse_location: 'WH1',
      category: 'A',
      spec: 'S1',
      product_name: 'Product 1',
      product_code: 'PROD001',
      channel_data: [
        channelData('PX/大全聯', { '2': 100, '3': 150 }, 250),
        channelData('家樂福', {}, 0),
      ],
      aggregate_months: { '2': 80, '3': 120 },
      buffer_quantity: 50,
      aggregate_total: 250,
      original_forecast: 250,
      difference: 0,
      remarks: 'Test',
      production_form_id: 1,
    },
    {
      warehouse_location: 'WH2',
      category: 'B',
      spec: 'S2',
      product_name: 'Product 2',
      product_code: 'PROD002',
      channel_data: [
        channelData('PX/大全聯', { '2': 50, '3': 75 }, 125),
        channelData('家樂福', {}, 0),
      ],
      aggregate_months: { '2': 40, '3': 60 },
      buffer_quantity: 25,
      aggregate_total: 125,
      original_forecast: 125,
      difference: 0,
      remarks: '',
      production_form_id: 2,
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

    expect(screen.getByText(/生產表單/)).toBeInTheDocument()
   // expect(screen.getByLabelText('年度')).toBeInTheDocument()
   // expect(screen.getByRole('button', { name: '載入' })).toBeInTheDocument()
  })*/

  it('loads data when Query button is clicked', async () => {
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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText(/請選擇年份並按「查詢」取得生產表單資料/)).toBeInTheDocument()
    })
  })

  it('displays grid with table headers', async () => {
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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('庫位')).toBeInTheDocument()
      expect(screen.getByText('中類名稱')).toBeInTheDocument()
      expect(screen.getByText('貨品規格')).toBeInTheDocument()
      expect(screen.getByText('品名')).toBeInTheDocument()
      expect(screen.getByText('品號')).toBeInTheDocument()
      expect(screen.getByText('原始預估')).toBeInTheDocument()
      expect(screen.getByText('差異')).toBeInTheDocument()
      expect(screen.getByText('備註')).toBeInTheDocument()
      expect(screen.getByText('緩衝量')).toBeInTheDocument()
      expect(screen.getByText('合計')).toBeInTheDocument()
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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText(/請選擇年份並按「查詢」取得生產表單資料/)).toBeInTheDocument()
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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

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
    productionPlanApi.updateProductionPlanBuffer.mockResolvedValue(undefined)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('50')).toBeInTheDocument()
    })

    // Edit buffer cell
    const cell = screen.getByText('50')
    await userEvent.click(cell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '200{Enter}')

    await waitFor(() => {
      expect(productionPlanApi.updateProductionPlanBuffer).toHaveBeenCalledWith(
        expect.any(Number),
        'PROD001',
        200
      )
    })

    // Click query again (new UI does not use confirm for query)
    const queryBtn = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryBtn)

    await waitFor(() => {
      expect(productionPlanApi.getProductionPlan).toHaveBeenCalled()
    })
  })

  it('can edit buffer and save', async () => {
    productionPlanApi.getProductionPlan.mockResolvedValue([mockData[0]])
    productionPlanApi.updateProductionPlanBuffer.mockResolvedValue(undefined)

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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('50')).toBeInTheDocument()
    })

    const bufferCell = screen.getByText('50')
    await userEvent.click(bufferCell)

    await waitFor(() => {
      const input = screen.getByRole('textbox')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox')
    await userEvent.clear(input)
    await userEvent.type(input, '99{Enter}')

    await waitFor(() => {
      expect(productionPlanApi.updateProductionPlanBuffer).toHaveBeenCalledWith(
        expect.any(Number),
        'PROD001',
        99
      )
    })
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

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    // Should show loading state
    expect(screen.getByRole('status')).toHaveTextContent('載入中...')
    expect(screen.getByRole('button', { name: '查詢' })).toHaveTextContent('查詢中...')

    // Wait for loading to finish
    await waitFor(() => {
      expect(screen.queryByRole('status')).not.toBeInTheDocument()
    })
  })
})
