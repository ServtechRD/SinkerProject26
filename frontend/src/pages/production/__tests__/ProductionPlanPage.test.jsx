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

  it('renders page with year selector for user with view permission', () => {
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
    expect(screen.getByLabelText('年度')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '載入' })).toBeInTheDocument()
  })

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
})
